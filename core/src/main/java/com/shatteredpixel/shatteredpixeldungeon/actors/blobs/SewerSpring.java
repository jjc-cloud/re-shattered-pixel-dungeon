package com.shatteredpixel.shatteredpixeldungeon.actors.blobs;

import com.badlogic.gdx.utils.IntArray;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.SparseArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

/**
 * 下水道出水口：生成期把每处出水口相连的水体登记下来，水被清掉后先等几回合，
 * 再从离缺口最近的那一圈开始每回合回填一层。
 * 同一片水由多个出水口供水时只算一个水体，回填进度不按出水口数量叠加。
 * 不参与渲染，也不占用 Blob 的 cur/off 数组。
 */
public class SewerSpring extends Blob {

	// 水被清掉后先等几回合，之后每回合回填一层
	private static final int WAIT_TURNS = 3;

	private static final class Outlet {
		int cell;        // 出水口所在的墙格
		int[] dist;      // 与所属水体的 cells 一一对应：到本出水口的环距离
		boolean dead;    // 出水口被破坏后不再供水
		Body body;
	}

	private static final class Body {
		final int[] cells;                 // 原水体格子
		final ArrayList<Outlet> outlets = new ArrayList<>();
		int wait;                          // 剩余等待回合
		boolean missing;                   // 上一回合是否处于缺水状态
		int lastMissing;                   // 上一回合的缺水格数，用来判断是否又被清过
		boolean pending;
		boolean restarted;
		int[] nearest;

		Body(int[] cells) {
			this.cells = cells;
		}
	}

	private final ArrayList<Body> bodies = new ArrayList<>();

	// 水体格子 → 水体下标，只用来在 cellChanged 里做一次 O(1) 过滤
	private int[] bodyAt;
	private final SparseArray<ArrayList<Outlet>> outletsAt = new SparseArray<>();
	private final BitSet wetCells = new BitSet();
	// 缺水或水源失效时置位；空闲时每回合只查这一个布尔值。
	private boolean dirty;

	/** 生成期调用：先区分四向连通的水体，再登记每个水体接触的出水口。 */
	public static void create(Level level) {
		SewerSpring blob = new SewerSpring();
		int length = level.length();
		int[] distMap = new int[length];
		int[] bodyOf = new int[length];
		int[] position = new int[length];
		Arrays.fill(distMap, -1);
		Arrays.fill(bodyOf, -1);
		ArrayList<Integer> seeds = new ArrayList<>();

		// 水体身份只由水格的连通性决定，同一个出水口可以分别供应多个水体。
		for (int cell = 0; cell < length; cell++) {
			if (!level.insideMap(cell) || bodyOf[cell] >= 0 || !level.hasTerrain(cell, Terrain.WATER)) continue;
			seeds.clear();
			seeds.add(cell);
			IntArray cells = new IntArray();
			waterBfs(level, seeds, distMap, cells, new IntArray());
			Body body = new Body(cells.toArray());
			int id = blob.bodies.size();
			blob.bodies.add(body);
			for (int i = 0; i < body.cells.length; i++) {
				bodyOf[body.cells[i]] = id;
				position[body.cells[i]] = i;
			}
		}

		// 同一出水口只在其实际接触的连通水体内计算距离，不合并分离的水池。
		for (int outlet = 0; outlet < length; outlet++) {
			if (!level.hasTerrain(outlet, Terrain.WALL_DECO)) continue;

			IntArray adjacentBodies = new IntArray();
			for (int offset : PathFinder.NEIGHBOURS8) {
				int next = outlet + offset;
				if (!level.insideMap(next) || !level.adjacent(outlet, next)) continue;
				int id = bodyOf[next];
				if (id >= 0 && !adjacentBodies.contains(id)) adjacentBodies.add(id);
			}
			for (int i = 0; i < adjacentBodies.size; i++) {
				int id = adjacentBodies.get(i);
				Body body = blob.bodies.get(id);
				seeds.clear();
				for (int offset : PathFinder.NEIGHBOURS8) {
					int next = outlet + offset;
					if (level.insideMap(next) && level.adjacent(outlet, next) && bodyOf[next] == id) seeds.add(next);
				}
				IntArray cells = new IntArray();
				IntArray dists = new IntArray();
				waterBfs(level, seeds, distMap, cells, dists);
				Outlet entry = new Outlet();
				entry.cell = outlet;
				entry.dist = align(body.cells, cells.toArray(), dists.toArray(), position);
				body.outlets.add(entry);
			}
		}
		for (int i = blob.bodies.size() - 1; i >= 0; i--) {
			if (blob.bodies.get(i).outlets.isEmpty()) blob.bodies.remove(i);
		}

		if (!blob.bodies.isEmpty()) {
			blob.ensureIndex(level);
			level.blobs.put(SewerSpring.class, blob);
			level.sewerSpring = blob;
		}
	}

	/** 从种子出发沿四向水格 BFS，按距离升序追加格子与距离。 */
	private static void waterBfs(Level level, ArrayList<Integer> seeds, int[] distMap, IntArray cells, IntArray dists) {
		for (int seed : seeds) {
			if (distMap[seed] >= 0) continue;
			distMap[seed] = 0;
			cells.add(seed);
		}
		for (int head = 0; head < cells.size; head++) {
			int cell = cells.get(head);
			dists.add(distMap[cell]);
			for (int offset : PathFinder.NEIGHBOURS4) {
				int next = cell + offset;
				if (!level.insideMap(next) || distMap[next] >= 0) continue;
				if (!level.hasTerrain(next, Terrain.WATER)) continue;
				distMap[next] = distMap[cell] + 1;
				cells.add(next);
			}
		}
		// 只清理实际访问的水格，下一次 BFS 不必清空整张地图，也不装箱队列元素。
		for (int i = 0; i < cells.size; i++) distMap[cells.get(i)] = -1;
	}

	/** 把另一处出水口算出的距离按水体的格子顺序重排。 */
	private static int[] align(int[] canonical, int[] cells, int[] dists, int[] position) {
		int[] result = new int[canonical.length];
		Arrays.fill(result, -1);
		for (int i = 0; i < cells.length; i++) result[position[cells[i]]] = dists[i];
		return result;
	}

	/** 由 Level.cellChanged 调用：只唤醒缺水或出水口失效所影响的水体。 */
	public void cellChanged(Level level, int cell) {
		if (cell < 0 || cell >= level.length()) return;
		ensureIndex(level);
		ArrayList<Outlet> outlets = outletsAt.get(cell);
		if (outlets != null && !level.hasTerrain(cell, Terrain.WALL_DECO)) {
			for (Outlet outlet : outlets) {
				if (outlet.dead) continue;
				outlet.dead = true;
				outlet.body.nearest = null;
				outlet.body.pending = true;
				dirty = true;
			}
		}
		if (bodyAt[cell] < 0) return;
		if (level.hasTerrain(cell, Terrain.WATER)) {
			wetCells.set(cell);
		} else {
			Body body = bodies.get(bodyAt[cell]);
			if (wetCells.get(cell)) body.restarted = true;
			wetCells.clear(cell);
			body.pending = true;
			dirty = true;
		}
	}

	public void ensureIndex(Level level) {
		if (bodyAt != null && bodyAt.length == level.length()) return;
		bodyAt = new int[level.length()];
		Arrays.fill(bodyAt, -1);
		outletsAt.clear();
		wetCells.clear();
		dirty = false;
		for (int i = 0; i < bodies.size(); i++) {
			Body body = bodies.get(i);
			for (int cell : body.cells) {
				bodyAt[cell] = i;
				if (level.hasTerrain(cell, Terrain.WATER)) wetCells.set(cell);
			}
			for (Outlet outlet : body.outlets) {
				outlet.body = body;
				ArrayList<Outlet> entries = outletsAt.get(outlet.cell);
				if (entries == null) {
					entries = new ArrayList<>();
					outletsAt.put(outlet.cell, entries);
				}
				entries.add(outlet);
			}
			body.pending |= body.missing;
			if (body.pending) dirty = true;
		}
	}

	@Override
	public boolean act() {
		spend(TICK);

		// 水体完整时每回合只查这一个布尔值，不做任何扫描
		if (!dirty) return true;

		Level level = Dungeon.level;
		if (level == null || level.blobs.get(getClass()) != this) return true;
		ensureIndex(level);

		boolean changed = false;
		boolean awaiting = false;
		for (Body body : bodies) {
			if (!body.pending) continue;
			changed |= update(level, body);
			body.pending = body.missing;
			if (body.pending) awaiting = true;
		}
		if (changed) Dungeon.observe();

		// 全部补满后回到空闲状态
		dirty = awaiting;

		return true;
	}

	private boolean update(Level level, Body body) {
		// 出水口被挖掉后不再供水，已经恢复的水保留
		boolean supply = false;
		for (Outlet outlet : body.outlets) {
			if (!outlet.dead && !level.hasTerrain(outlet.cell, Terrain.WALL_DECO)) {
				outlet.dead = true;
				body.nearest = null;
			}
			if (!outlet.dead) supply = true;
		}
		if (!supply) {
			body.missing = false;
			body.wait = 0;
			body.restarted = false;
			body.lastMissing = 0;
			return false;
		}

		int missing = 0;
		int nearest = Integer.MAX_VALUE;
		for (int i = 0; i < body.cells.length; i++) {
			int cell = body.cells[i];
			if (!canEverHoldWater(level, cell) || level.hasTerrain(cell, Terrain.WATER)) continue;
			int layer = distance(body, i);
			if (layer == Integer.MAX_VALUE) continue;
			missing++;
			if (level.traps.get(cell) == null && level.plants.get(cell) == null && layer < nearest) nearest = layer;
		}

		// 水体完整
		if (missing == 0) {
			body.wait = 0;
			body.missing = false;
			body.lastMissing = 0;
			body.restarted = false;
			return false;
		}

		// 初次缺水或回填期间再次失水时重新计时，其他地形刷新不会重置等待。
		if (!body.missing || body.restarted || missing > body.lastMissing) {
			body.missing = true;
			body.restarted = false;
			body.wait = WAIT_TURNS;
			body.lastMissing = missing;
			return false;
		}

		if (body.wait > 0) {
			body.wait--;
			if (body.wait > 0) {
				body.lastMissing = missing;
				return false;
			}
		}
		if (nearest == Integer.MAX_VALUE) {
			body.lastMissing = missing;
			return false;
		}

		// 每回合只推进一层：同层里所有缺口一起补，多个出水口不会让进度叠加
		boolean filled = false;
		int filledCount = 0;
		for (int i = 0; i < body.cells.length; i++) {
			if (distance(body, i) != nearest) continue;
			int cell = body.cells[i];
			if (!canEverHoldWater(level, cell) || level.hasTerrain(cell, Terrain.WATER)) continue;
			// 陷阱、植物占住的位置先跳过，等条件恢复再填
			if (level.traps.get(cell) != null || level.plants.get(cell) != null) continue;
			level.cellAt(cell).place(Terrain.WATER);
			filled = true;
			filledCount++;
		}

		body.lastMissing = missing - filledCount;
		body.missing = body.lastMissing > 0;

		return filled;
	}

	/** 该格到最近一个仍在供水的出水口的环距离；没有供水就用 MAX_VALUE。 */
	private static int distance(Body body, int index) {
		if (body.nearest == null) {
			body.nearest = new int[body.cells.length];
			Arrays.fill(body.nearest, Integer.MAX_VALUE);
			for (Outlet outlet : body.outlets) {
				if (outlet.dead) continue;
				for (int i = 0; i < body.nearest.length; i++) {
					if (outlet.dist[i] >= 0 && outlet.dist[i] < body.nearest[i]) body.nearest[i] = outlet.dist[i];
				}
			}
		}
		return body.nearest[index];
	}

	/** 墙体、或地面已经铺不了水：这格水永远回不来了，跳过且不算缺口。 */
	private static boolean canEverHoldWater(Level level, int cell) {
		if ((Terrain.flags[level.cellAt(cell).ground()] & Terrain.FLOODABLE) == 0) return false;
		return level.passable[cell] || level.avoid[cell];
	}

	private static final String COUNT = "count";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(COUNT, bodies.size());
		for (int i = 0; i < bodies.size(); i++) {
			Body body = bodies.get(i);
			bundle.put("cells_" + i, body.cells);
			bundle.put("wait_" + i, body.wait);
			bundle.put("missing_" + i, body.missing);
			bundle.put("last_missing_" + i, body.lastMissing);
			bundle.put("pending_" + i, body.pending);
			bundle.put("restarted_" + i, body.restarted);
			bundle.put("outlets_" + i, body.outlets.size());
			for (int j = 0; j < body.outlets.size(); j++) {
				Outlet outlet = body.outlets.get(j);
				bundle.put("outlet_" + i + "_" + j, outlet.cell);
				bundle.put("dist_" + i + "_" + j, outlet.dist);
				bundle.put("dead_" + i + "_" + j, outlet.dead);
			}
		}
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		bodies.clear();
		bodyAt = null;
		dirty = false;
		for (int i = 0; i < bundle.getInt(COUNT); i++) {
			Body body = new Body(bundle.getIntArray("cells_" + i));
			body.wait = bundle.getInt("wait_" + i);
			body.missing = bundle.getBoolean("missing_" + i);
			body.lastMissing = bundle.getInt("last_missing_" + i);
			body.pending = bundle.getBoolean("pending_" + i);
			body.restarted = bundle.getBoolean("restarted_" + i);
			for (int j = 0; j < bundle.getInt("outlets_" + i); j++) {
				Outlet outlet = new Outlet();
				outlet.cell = bundle.getInt("outlet_" + i + "_" + j);
				outlet.dist = bundle.getIntArray("dist_" + i + "_" + j);
				outlet.dead = bundle.getBoolean("dead_" + i + "_" + j);
				body.outlets.add(outlet);
			}
			bodies.add(body);
			// 读档时如果正在回填，直接接着算，不必等下一次缺水事件
			if (body.pending || body.missing) dirty = true;
		}
	}
}
