package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.badlogic.gdx.utils.IntArray;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.watabou.utils.PathFinder;
import com.watabou.utils.SparseArray;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collection;

/** 一次施法的导体遍历。独立于 PathFinder 的共享距离图，无递归和逐次全图清空。 */
public final class TerrainPropagation {
	public interface TargetEffect { void apply(Char target); }

	/** 单点作用的类别入口。沿用调用方的原效果回调，不在地形系统中决定伤害。 */
	public static void point(Level level, TerrainInteractions.Source source, int cell,
	                         Char primary, Char excluded, TargetEffect effect) {
		TerrainPropagation event = event(level, source);
		if (event == null) {
			if (primary != null) effect.apply(primary);
			return;
		}
		ArrayList<Char> targets = new ArrayList<>();
		if (primary != null) targets.add(primary);
		event.touch(cell);
		event.extendTargets(targets, excluded);
		for (Char target : targets) effect.apply(target);
	}

	/** 事件入口：没有此类响应时返回 null，不分配事件或逐格缓存。 */
	public static TerrainPropagation event(Level level, TerrainInteractions.Source source) {
		if (level.terrainInteractions == null || !level.terrainInteractions.hasEffects(source)) return null;
		return level.terrainInteractions.acquirePropagation(source);
	}

	/** 点命中和范围脉冲共用的目标扩展；伤害、免疫及持续时间由来源结算。 */
	public void extendTargets(ArrayList<Char> targets, Char excluded) {
		if (!hasNext()) { end(); return; }
		SparseArray<Char> occupants = new SparseArray<>();
		for (Char ch : Actor.chars()) occupants.put(ch.pos, ch);
		HashSet<Char> seen = new HashSet<>(targets);
		while (hasNext()) {
			int cell = next();
			for (int offset : PathFinder.NEIGHBOURS9) {
				Char ch = occupants.get(cell + offset);
				if (ch != null && ch != excluded && seen.add(ch)) targets.add(ch);
			}
		}
		end();
	}

	/** 范围作用共用入口：仅加入导体及其邻格，由来源继续按原规则逐格处理。 */
	public void extendCells(Collection<Integer> affected) {
		for (int cell : affected) touch(cell);
		if (!hasNext()) { end(); return; }
		HashSet<Integer> seen = new HashSet<>(affected);
		while (hasNext()) {
			int cell = next();
			for (int offset : PathFinder.NEIGHBOURS9) {
				int next = cell + offset;
				if (level.insideMap(next) && (next == cell || !level.solid[next]) && seen.add(next)) affected.add(next);
			}
		}
		end();
	}

	private Level level;
	TerrainInteractions poolOwner;
	private TerrainInteractions.Source source;
	private int[] visited;
	private int[] responded;
	private boolean terrainChanged;
	private int generation;
	private final IntArray cells = new IntArray();
	private final IntArray parents = new IntArray();
	private int head;

	public void begin(Level level, TerrainInteractions.Source source) {
		this.source = source;
		this.level = level;
		terrainChanged = false;
		cells.clear();
		parents.clear();
		head = 0;
		if (++generation == 0) {
			if (visited != null) Arrays.fill(visited, 0);
			if (responded != null) Arrays.fill(responded, 0);
			generation = 1;
		}
	}

	public void seed(int from, int cell) {
		if (level.terrainInteractions == null
				|| !level.terrainInteractions.ruleAt(cell).conducts(source)) return;
		if (visited == null || visited.length != level.length()) visited = new int[level.length()];
		if (visited[cell] == generation) return;
		visited[cell] = generation;
		cells.add(cell);
		parents.add(from);
	}

	/** 生物与导体直接接触；水面和充能不增加此接触距离。 */
	public void touch(int cell) {
		if (!level.insideMap(cell)) return;
		respond(cell);
		seed(cell, cell);
		for (int offset : PathFinder.NEIGHBOURS8) {
			int next = cell + offset;
			if (level.insideMap(next) && level.adjacent(cell, next)) seed(cell, next);
		}
	}

	public boolean hasNext() { return head < cells.size; }
	public int size() { return cells.size; }
	public int cell(int index) { return cells.get(index); }

	public int next() {
		int cell = cells.get(head++);
		for (int offset : PathFinder.NEIGHBOURS9) respond(cell + offset);
		for (int offset : PathFinder.NEIGHBOURS8) {
			int next = cell + offset;
			if (level.insideMap(next) && level.adjacent(cell, next)) seed(cell, next);
		}
		return cell;
	}

	public int parent() { return parents.get(head - 1); }
	private void respond(int cell) {
		if (!level.insideMap(cell) || level.terrainInteractions == null) return;
		TerrainInteractions.Action action = level.terrainInteractions.ruleAt(cell).response(source).action;
		if (action != TerrainInteractions.Action.REPLACE && action != TerrainInteractions.Action.DESTROY) return;
		if (responded == null || responded.length != level.length()) responded = new int[level.length()];
		if (responded[cell] == generation) return;
		responded[cell] = generation;
		terrainChanged |= level.affectTerrain(cell, source);
	}

	public void end() {
		Level completedLevel = level;
		level = null;
		if (poolOwner != null) {
			TerrainInteractions owner = poolOwner;
			poolOwner = null;
			owner.releasePropagation(this);
		}
		if (terrainChanged && completedLevel == Dungeon.level) {
			Dungeon.observe();
		}
	}

	/** Ballistica 对不可通行装饰停在前一格。仅修正本类别能够响应的实体表面。 */
	public static int impactCell(Level level, Ballistica bolt, TerrainInteractions.Source source) {
		int cell = bolt.collisionPos;
		if (level.terrainInteractions != null && level.terrainInteractions.hasEffects(source)
				&& (bolt.collisionProperties & Ballistica.STOP_SOLID) != 0 && !level.solid[cell]
				&& Actor.findChar(cell) == null && bolt.dist + 1 < bolt.path.size()) {
			int next = bolt.path.get(bolt.dist + 1);
			if (level.solid[next] && !level.passable[next] && !level.avoid[next]
					&& level.terrainInteractions.ruleAt(next).allows(source)) return next;
		}
		return cell;
	}
}
