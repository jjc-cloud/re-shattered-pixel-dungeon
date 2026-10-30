package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.levels.features.Door;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** 基础地面与同格内容的权威状态；map 图案和通行数组只是派生结果。 */
public class CellState implements Bundlable {

	private volatile int ground = Terrain.EMPTY;
	// 渲染读取快照，不在持有图层锁时等待逻辑线程的格子变更。
	private final CopyOnWriteArrayList<CellContent> contents = new CopyOnWriteArrayList<>();
	private Level level;
	private int cell;
	private int mutations;
	private boolean dirty;
	public CellState() { }
	public CellState(Level level, int cell) { bind(level, cell); }
	public synchronized void bind(Level level, int cell) { this.level = level; this.cell = cell; }
	public int ground() { return ground; }
	public List<CellContent> contents() { return Collections.unmodifiableList(new ArrayList<>(contents)); }

	/** 复用楼层索引里的实体实例，格子查询不会复制或另存一份陷阱、植物和物品。 */
	public List<Object> entities() {
		ArrayList<Object> result = new ArrayList<>();
		Char character = level == Dungeon.level ? Actor.findChar(cell) : level.findMob(cell);
		if (character != null) result.add(character);
		if (level.heaps != null && level.heaps.get(cell) != null) result.add(level.heaps.get(cell));
		if (level.plants != null && level.plants.get(cell) != null) result.add(level.plants.get(cell));
		if (level.traps != null && level.traps.get(cell) != null) result.add(level.traps.get(cell));
		return Collections.unmodifiableList(result);
	}

	public static boolean isGround(int type) {
		switch (type) {
			case Terrain.CHASM: case Terrain.EMPTY: case Terrain.EMPTY_SP:
			case Terrain.EMPTY_DECO: case Terrain.EMPTY_WELL: case Terrain.CUSTOM_DECO_EMPTY:
			case Terrain.ENTRANCE: case Terrain.ENTRANCE_SP: case Terrain.EXIT:
			case Terrain.LOCKED_EXIT: case Terrain.UNLOCKED_EXIT: case Terrain.PEDESTAL: case Terrain.WELL:
				return true;
			default: return false;
		}
	}

	public CellContent content(int type) {
		for (CellContent content : contents) if (content.type() == type) return content;
		return null;
	}

	public boolean hasTerrain(int type) {
		if (isGround(type)) return ground == type;
		if (CellContent.family(type) == Terrain.TRAP && level != null && level.traps != null) {
			Trap trap = level.traps.get(cell);
			if (trap != null) return !buried() && type == (!trap.active ? Terrain.INACTIVE_TRAP : trap.visible ? Terrain.TRAP : Terrain.SECRET_TRAP);
		}
		return content(type) != null;
	}

	public boolean buried() { return covering() != null; }
	public boolean exposed(CellContent content) {
		CellContent cover = covering();
		return cover == null || cover == content;
	}

	private CellContent covering() {
		CellContent cover = null;
		for (CellContent content : contents) if (content.has(CellContent.COVERS_CONTENTS)
				&& (cover == null || content.layer() > cover.layer())) cover = content;
		return cover;
	}

	public CellContent firstWith(int property) {
		CellContent result = null;
		for (CellContent content : contents) if (exposed(content) && content.has(property)
				&& (result == null || content.layer() > result.layer())) result = content;
		return result;
	}

	public boolean has(int property) {
		for (CellContent content : contents) if (exposed(content) && content.has(property)) return true;
		return false;
	}

	public boolean supports(int property) {
		for (CellContent content : contents) if ((content.flags() & Terrain.LIQUID) != 0) return false;
		int flags = Terrain.flags[ground];
		if (property == CellContent.SUPPORTS_COVER) return (flags & Terrain.SUPPORTS_COVER) != 0;
		if (property == CellContent.GROWABLE) return (flags & Terrain.GROWABLE) != 0;
		if (property == CellContent.FLOODABLE) return (flags & Terrain.FLOODABLE) != 0;
		return false;
	}

	/** 随机布置检查承载和占用；不通过最上层的贴图编号判断空地。 */
	public boolean canPlace(CellContent incoming) {
		if ((Terrain.flags[ground] & (Terrain.RESERVED | Terrain.SCATTERABLE)) != Terrain.SCATTERABLE
				|| buried() || !supports(incoming.requiredSupport())) return false;
		if ((level.traps != null && level.traps.get(cell) != null)
				|| (level.plants != null && level.plants.get(cell) != null)) return false;
		for (CellContent existing : contents) {
			if (CellContent.family(existing.type()) == CellContent.family(incoming.type())) return false;
			if (existing.has(CellContent.OCCUPIES_SPACE)) return false;
			if (existing.has(CellContent.SURFACE_COVER) && incoming.has(CellContent.SURFACE_COVER | CellContent.OCCUPIES_SPACE)) return false;
		}
		return true;
	}

	public synchronized void setGround(int terrain) {
		if (!isGround(terrain)) throw new IllegalArgumentException("基础地形不能包含物件或覆盖物");
		ground = terrain;
		if (!supports(CellContent.SUPPORTS_COVER)) removeTerrain(Terrain.EMBERS);
		changed();
	}

	public synchronized boolean addContent(CellContent content) {
		mutations++;
		try {
			if (isGround(content.type())) throw new IllegalArgumentException("地面应通过 setGround 设置");
			if (content.type() == Terrain.EMBERS) {
				if (!supports(CellContent.SUPPORTS_COVER)) return false;
				// 灰烬不铺在水面和楼梯口上
				if (hasTerrain(Terrain.WATER)) return false;
				if (ground == Terrain.ENTRANCE || ground == Terrain.ENTRANCE_SP
						|| ground == Terrain.EXIT || ground == Terrain.LOCKED_EXIT
						|| ground == Terrain.UNLOCKED_EXIT) return false;
			}
			CellContent existing = content(content.type());
			if (existing != null) return false;
			if (content.type() == Terrain.WATER) {
				removeTerrain(Terrain.EMBERS);
				removeTerrain(Terrain.GRASS);
				removeTerrain(Terrain.HIGH_GRASS);
				removeTerrain(Terrain.FURROWED_GRASS);
			}
			if (content.type() == Terrain.COLLAPSE_WALL) {
				// 掩埋永久填平水与陷阱、压低高草，其他内容继续留在岩壁下。
				boolean filled = removeTerrain(Terrain.WATER);
				filled |= removeTerrain(Terrain.TRAP);
				filled |= removeTerrain(Terrain.SECRET_TRAP);
				filled |= removeTerrain(Terrain.INACTIVE_TRAP);
				if (level != null && level.traps != null) filled |= level.traps.remove(cell) != null;
				if (filled) setGround(Terrain.EMPTY);
				boolean flattened = removeTerrain(Terrain.HIGH_GRASS);
				flattened |= removeTerrain(Terrain.FURROWED_GRASS);
				if (flattened) addContent(new CellContent(Terrain.GRASS, level == null ? -1 : level.region()));
			}
			contents.add(content);
			changed();
			return true;

		} finally {
			if (--mutations == 0 && dirty) { dirty = false; changed(); }
		}
	}

	public synchronized boolean removeContent(CellContent content) {
		if (!contents.remove(content)) return false;
		changed();
		return true;
	}

	public synchronized boolean removeTerrain(int type) {
		CellContent content = content(type);
		return content != null && removeContent(content);
	}

	/** 修改内容状态时仅替换同一类别；基础地面和其他类别继续存在。 */
	public synchronized void place(int terrain) {
		mutations++;
		try {
			if (isGround(terrain)) { setGround(terrain); return; }
			if (content(terrain) != null) return;
			if (CellContent.family(terrain) == Terrain.GRASS) removeTerrain(Terrain.EMBERS);
			if (CellContent.family(terrain) == Terrain.TRAP && level.traps != null && level.traps.get(cell) != null) {
				Trap trap = level.traps.get(cell);
				trap.visible = terrain != Terrain.SECRET_TRAP;
				trap.active = terrain != Terrain.INACTIVE_TRAP;
				changed();
				return;
			}
			Door.State doorState = Door.state(terrain);
			if (doorState != null && doorState != Door.State.CRYSTAL) {
				for (CellContent current : contents) {
					if (current instanceof Door && ((Door) current).state() != Door.State.CRYSTAL) {
						((Door) current).changeState(level, cell, doorState);
						return;
					}
				}
			}
			CellContent replacement = doorState == null
					? new CellContent(terrain, level.region()) : level.createDoor(terrain);
			for (CellContent current : new ArrayList<>(contents)) {
				if (CellContent.family(terrain) == Terrain.WALL && CellContent.family(current.type()) == Terrain.WALL) {
					replacement.properties(current.properties(), 0);
				}
				if (CellContent.family(current.type()) == CellContent.family(terrain)) removeContent(current);
			}
			addContent(replacement);

		} finally {
			if (--mutations == 0 && dirty) { dirty = false; changed(); }
		}
	}

	/** 明确重建环境；地面、内容和环境实体一并重置，角色与物品由楼层管理。 */
	public synchronized void reset(int ground, int... types) {
		if (!isGround(ground)) throw new IllegalArgumentException("重建格子必须指定基础地面");
		for (int type : types) if (isGround(type)) throw new IllegalArgumentException("内容不能包含基础地形");
		mutations++;
		try {
			contents.clear();
			if (level.traps != null) level.traps.remove(cell);
			if (level.plants != null) level.plants.remove(cell);
			this.ground = ground;
			for (int type : types) place(type);
			changed();
		} finally {
			if (--mutations == 0 && dirty) { dirty = false; changed(); }
		}
	}

	/** 此编号仅用于旧素材的拼接和具体状态选择，不能写回或用于交互资格。 */
	public int terrain() {
		int result = ground;
		int layer = -1;
		Trap trap = level.traps == null ? null : level.traps.get(cell);
		if (trap != null && !buried()) {
			result = !trap.active ? Terrain.INACTIVE_TRAP : trap.visible ? Terrain.TRAP : Terrain.SECRET_TRAP;
			layer = 30;
		}
		for (CellContent content : contents) {
			if (content.type() < CellContent.CARPET && content.type() != Terrain.EMBERS
					&& content.layer() > layer) { result = content.type(); layer = content.layer(); }
		}
		return result;
	}

	public int flags() {
		int flags = Terrain.flags[ground];
		for (CellContent content : contents) {
			if (!exposed(content)) continue;
			int next = content.flags();
			if ((next & Terrain.SOLID) != 0 && (next & Terrain.PASSABLE) == 0) flags &= ~Terrain.PASSABLE;
			flags |= next & ~Terrain.PASSABLE;
		}
		Trap trap = level.traps == null ? null : level.traps.get(cell);
		if (!buried() && trap != null && trap.active) {
			if (trap.visible) { flags |= Terrain.AVOID; flags &= ~Terrain.PASSABLE; }
			else flags |= Terrain.SECRET;
		}
		return flags;
	}

	/** 属性是参与资格；不存在逐对象、逐动作的响应分配。 */
	public synchronized boolean affect(int property, boolean burned) {
		return affect(property, burned, targets(property));
	}

	public List<CellContent> targets(int property) {
		ArrayList<CellContent> targets = new ArrayList<>();
		for (CellContent content : contents) if (exposed(content) && content.has(property)
				&& ((property & CellContent.DISINTEGRATION_BREAKABLE) == 0
				|| !content.has(CellContent.DISINTEGRATION_PROOF))) targets.add(content);
		return targets;
	}

	/** 范围作用只处理开始时捕获的对象，不把同次作用新生成的内容再次当作目标。 */
	public synchronized boolean affect(int property, boolean burned, List<CellContent> targets) {
		ArrayList<CellContent> affected = new ArrayList<>();
		for (CellContent content : targets) if (contents.contains(content) && content.has(property)
				&& ((property & CellContent.DISINTEGRATION_BREAKABLE) == 0
				|| !content.has(CellContent.DISINTEGRATION_PROOF))) affected.add(content);
		if (affected.isEmpty()) return false;
		contents.removeAll(affected);
		// 先公布移除后的通行状态，再结算产物，掉落才会落在物件原来所在的格子。
		changed();
		for (CellContent content : affected) content.destroyed(level, cell, burned);
		return true;
	}

	private void changed() {
		if (mutations > 0) { dirty = true; return; }
		if (level != null) level.cellChanged(cell);
	}

	@Override public synchronized void storeInBundle(Bundle bundle) {
		bundle.put("ground", ground);
		bundle.put("contents", contents);
	}

	@Override public synchronized void restoreFromBundle(Bundle bundle) {
		ground = bundle.getInt("ground");
		contents.clear();
		for (Bundlable content : bundle.getCollection("contents")) contents.add((CellContent)content);
	}
}
