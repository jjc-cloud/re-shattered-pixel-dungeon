package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntSet;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.List;

/** 按事件查询的地形规则；未配置的层不创建此组件，不参与 Actor 调度。 */
public final class TerrainInteractions {

	/** 作用类别，不是物品类别。不同来源沿同一作用路径报告同一类别。 */
	public enum Source { CLICK, EXPLOSION, DISINTEGRATION, ELECTRIC, FIRE, SHOCKWAVE }

	public enum Action { LEGACY, IGNORE, REPLACE, DESTROY, CONDUCT }

	/** 响应是数据：可忽略、换成指定地形、复用区域破坏方法或参与传播。 */
	public static final class Response {
		public static final Response LEGACY = new Response(Action.LEGACY, -1);
		public static final Response IGNORE = new Response(Action.IGNORE, -1);
		public static final Response DESTROY = new Response(Action.DESTROY, -1);
		public static final Response CONDUCT = new Response(Action.CONDUCT, -1);
		public final Action action;
		public final int terrain;
		private Response(Action action, int terrain) { this.action = action; this.terrain = terrain; }
		public static Response replaceWith(int terrain) { return new Response(Action.REPLACE, terrain); }
	}

	/** 不可变、可组合的类别响应表；生产系统不认识木桶、铁笼或易碎墙这些样例。 */
	public static final class Rule {
		public static final Rule LEGACY = new Rule(Response.LEGACY);
		public static final Rule NONE = new Rule(Response.IGNORE);
		private final Response[] responses;
		public Rule(Response fallback) {
			responses = new Response[Source.values().length];
			Arrays.fill(responses, fallback);
		}
		private Rule(Response[] responses) { this.responses = responses; }
		public Rule on(Source source, Response response) {
			Response[] copy = responses.clone();
			copy[source.ordinal()] = response;
			return new Rule(copy);
		}
		public Response response(Source source) { return responses[source.ordinal()]; }
		public boolean allows(Source source) {
			Action action = response(source).action;
			return action != Action.LEGACY && action != Action.IGNORE;
		}
		public boolean conducts(Source source) { return response(source).action == Action.CONDUCT; }
	}

	public enum Result { UNHANDLED, DENIED, UNCHANGED, CHANGED }
	private final ArrayList<Rule> rules = new ArrayList<>();
	private ArrayDeque<TerrainPropagation> propagationPool;
	private final int[] defaults = new int[Terrain.flags.length];
	// -1 继承类型默认值；0 显式恢复旧行为；1 显式拒绝已接入的新交互。
	private int[] overrides;
	private final Level level;
	private final int[] conductorRules = new int[Source.values().length];
	private final int[] effectRules = new int[Source.values().length];

	public TerrainInteractions(Level level) {
		this.level = level;
		rules.add(Rule.LEGACY);
		rules.add(Rule.NONE);
	}

	private int register(Rule rule) {
		int id = rules.indexOf(rule);
		if (id < 0) { id = rules.size(); rules.add(rule); }
		return id;
	}

	public void setDefault(int terrain, Rule rule) {
		countConductors(rules.get(defaults[terrain]), -1);
		defaults[terrain] = register(rule);
		countConductors(rule, 1);
	}

	public void setOverride(int cell, Rule rule) {
		if (!level.insideMap(cell)) throw new IllegalArgumentException("地图边界不可配置交互");
		if (overrides == null) {
			overrides = new int[level.length()];
			Arrays.fill(overrides, -1);
		}
		clearOverride(cell);
		overrides[cell] = register(rule);
		countConductors(rule, 1);
	}

	public void clearOverride(int cell) {
		if (overrides != null) {
			if (overrides[cell] >= 0) countConductors(rules.get(overrides[cell]), -1);
			overrides[cell] = -1;
		}
	}

	public Rule ruleAt(int cell) {
		if (!level.insideMap(cell)) return Rule.NONE;
		int id = overrides == null ? -1 : overrides[cell];
		return rules.get(id < 0 ? defaults[level.map[cell]] : id);
	}

	public boolean hasConductors(Source source) { return conductorRules[source.ordinal()] > 0; }
	public boolean hasEffects(Source source) { return effectRules[source.ordinal()] > 0; }

	TerrainPropagation acquirePropagation(Source source) {
		if (propagationPool == null) propagationPool = new ArrayDeque<>();
		TerrainPropagation event = propagationPool.pollFirst();
		if (event == null) event = new TerrainPropagation();
		event.begin(level, source);
		event.poolOwner = this;
		return event;
	}

	void releasePropagation(TerrainPropagation event) { propagationPool.addFirst(event); }

	private void countConductors(Rule rule, int delta) {
		for (int i = 0; i < conductorRules.length; i++) {
			Action action = rule.responses[i].action;
			if (action == Action.CONDUCT) conductorRules[i] += delta;
			if (action == Action.CONDUCT || action == Action.REPLACE || action == Action.DESTROY) effectRules[i] += delta;
		}
	}

	/** 返回值区分“未接管”和“拒绝”，调用方只对前者执行旧破坏逻辑。视野由调用方批量更新。 */
	public Result interact(int cell, Source source) {
		Response response = ruleAt(cell).response(source);
		if (response.action == Action.LEGACY) return Result.UNHANDLED;
		if (response.action == Action.IGNORE) return Result.DENIED;
		int before = level.map[cell];
		if (response.action == Action.REPLACE) {
			Level.set(cell, response.terrain, level);
		} else if (response.action == Action.DESTROY) {
			level.destroy(cell);
		}
		if (level.map[cell] == before) return Result.UNCHANGED;
		// 开墙后邻格可能由不可发现变为可发现；沿用矿洞的局部更新方式。
		for (int offset : PathFinder.NEIGHBOURS9) {
			int next = cell + offset;
			if (level.discoverable != null) level.discoverable[next] = true;
			GameScene.updateMap(next);
		}
		return Result.CHANGED;
	}

	/** 使用爆炸前距离图，只收集接触墙面，不把实体墙加入爆炸传播通道。 */
	public IntArray blastSurfaces(List<Integer> affected, int[] distance, int range) {
		IntArray surfaces = new IntArray();
		IntSet seen = new IntSet();
		for (int cell : affected) {
			if (distance[cell] >= range) continue;
			for (int offset : PathFinder.NEIGHBOURS8) {
				int next = cell + offset;
				if (level.insideMap(next) && level.adjacent(cell, next)
						&& distance[next] == Integer.MAX_VALUE && level.solid[next]
						&& ruleAt(next).allows(Source.EXPLOSION) && seen.add(next)) {
					surfaces.add(next);
				}
			}
		}
		return surfaces;
	}

	public void storeInBundle(Bundle bundle) {
		bundle.put("defaults", defaults);
		if (overrides != null) bundle.put("overrides", overrides);
		bundle.put("rule_count", rules.size());
		Source[] sources = Source.values();
		for (int i = 0; i < rules.size(); i++) {
			Bundle entry = new Bundle();
			for (Source source : sources) {
				Response response = rules.get(i).response(source);
				entry.put(source.name(), response.action);
				entry.put(source.name() + "_terrain", response.terrain);
			}
			bundle.put("rule_" + i, entry);
		}
	}

	public void restoreFromBundle(Bundle bundle) {
		rules.clear();
		for (int i = 0; i < bundle.getInt("rule_count"); i++) {
			Bundle entry = bundle.getBundle("rule_" + i);
			Rule rule = Rule.NONE;
			for (Source source : Source.values()) {
				if (entry.contains(source.name())) rule = rule.on(source,
						new Response(entry.getEnum(source.name(), Action.class), entry.getInt(source.name() + "_terrain")));
			}
			rules.add(rule);
		}
		int[] saved = bundle.getIntArray("defaults");
		System.arraycopy(saved, 0, defaults, 0, defaults.length);
		overrides = bundle.contains("overrides") ? bundle.getIntArray("overrides") : null;
		Arrays.fill(conductorRules, 0);
		Arrays.fill(effectRules, 0);
		for (int id : defaults) countConductors(rules.get(id), 1);
		if (overrides != null) {
			if (overrides.length != level.length()) throw new IllegalArgumentException("地形规则尺寸不符");
			for (int id : overrides) if (id >= 0) countConductors(rules.get(id), 1);
		}
	}
}
