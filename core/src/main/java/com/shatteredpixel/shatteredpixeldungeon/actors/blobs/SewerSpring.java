package com.shatteredpixel.shatteredpixeldungeon.actors.blobs;

import com.badlogic.gdx.utils.IntArray;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * 下水道出水口：生成期把与每处出水口相连的水体登记下来，
 * 水被清掉后先等几回合，再按离缺口的远近每回合回填一圈。
 * 不参与渲染，也不占用 Blob 的 cur/off 数组。
 */
public class SewerSpring extends Blob {

	// 水被清掉后先等几回合，之后每回合回填一圈
	private static final int WAIT_TURNS = 3;

	private static final class Spring {
		int outlet;        // 出水口所在的墙格
		int[] cells;       // 原水体格子，BFS 顺序（dist 非递减）
		int[] dist;        // 与 cells 对应：到出水口的环距离
		int wait;          // 剩余等待回合
		boolean missing;   // 上一回合是否处于缺水状态
		int lastMissing;   // 上一回合的缺水格数，用来判断是否又被清过
		boolean dead;      // 出水口被破坏后不再供水
	}

	private final ArrayList<Spring> springs = new ArrayList<>();

	// 水体格子 → 泉眼下标，只用来在 cellChanged 里做一次 O(1) 过滤
	private int[] springAt;
	// 只有水域真的缺了一块才会置位；完整时每回合只查这一个布尔值
	private boolean dirty;

	/** 生成期调用：每处出水口单独登记它挨着的那片水体。 */
	public static void create(Level level) {
		SewerSpring blob = new SewerSpring();
		int length = level.length();
		int[] distMap = new int[length];

		for (int outlet = 0; outlet < length; outlet++) {
			if (!level.hasTerrain(outlet, Terrain.WALL_DECO)) continue;

			ArrayList<Integer> seeds = new ArrayList<>();
			for (int offset : PathFinder.NEIGHBOURS8) {
				int next = outlet + offset;
				if (level.insideMap(next) && level.hasTerrain(next, Terrain.WATER)) seeds.add(next);
			}
			if (seeds.isEmpty()) continue;

			Arrays.fill(distMap, -1);
			ArrayDeque<Integer> queue = new ArrayDeque<>();
			for (int seed : seeds) {
				distMap[seed] = 0;
				queue.add(seed);
			}

			IntArray cells = new IntArray();
			IntArray dists = new IntArray();
			while (!queue.isEmpty()) {
				int cell = queue.poll();
				cells.add(cell);
				dists.add(distMap[cell]);
				for (int offset : PathFinder.NEIGHBOURS4) {
					int next = cell + offset;
					if (!level.insideMap(next) || distMap[next] >= 0) continue;
					if (!level.hasTerrain(next, Terrain.WATER)) continue;
					distMap[next] = distMap[cell] + 1;
					queue.add(next);
				}
			}

			Spring spring = new Spring();
			spring.outlet = outlet;
			spring.cells = cells.toArray();
			spring.dist = dists.toArray();
			blob.springs.add(spring);
		}

		if (!blob.springs.isEmpty()) {
			blob.ensureIndex(level);
			level.blobs.put(SewerSpring.class, blob);
			level.sewerSpring = blob;
		}
	}

	/** 由 Level.cellChanged 调用：只有登记过的水体格缺水时才唤醒逐回合结算。 */
	public void cellChanged(Level level, int cell) {
		if (cell < 0 || cell >= level.length()) return;
		ensureIndex(level);
		if (springAt[cell] < 0) return;
		if (!level.hasTerrain(cell, Terrain.WATER)) dirty = true;
	}

	private void ensureIndex(Level level) {
		if (springAt != null && springAt.length == level.length()) return;
		springAt = new int[level.length()];
		Arrays.fill(springAt, -1);
		dirty = false;
		for (int i = 0; i < springs.size(); i++) {
			Spring spring = springs.get(i);
			for (int cell : spring.cells) if (springAt[cell] < 0) springAt[cell] = i;
			if (spring.missing) dirty = true;
		}
	}

	@Override
	public boolean act() {
		spend(TICK);

		// 水体完整时每回合只查这一个布尔值，不做任何扫描
		if (!dirty) return true;

		Level level = Dungeon.level;
		if (level == null || level.blobs.get(getClass()) != this) return true;

		boolean changed = false;
		boolean awaiting = false;
		for (Spring spring : springs) {
			changed |= update(level, spring);
			if (spring.missing && !spring.dead) awaiting = true;
		}
		if (changed) Dungeon.observe();

		// 全部补满后回到空闲状态
		dirty = awaiting;

		return true;
	}

	private boolean update(Level level, Spring spring) {
		if (spring.dead) return false;

		// 出水口被挖掉后不再供水，已经恢复的水保留
		if (!level.hasTerrain(spring.outlet, Terrain.WALL_DECO)) {
			spring.dead = true;
			spring.missing = false;
			spring.wait = 0;
			return false;
		}

		int missing = 0;
		int ring = Integer.MAX_VALUE;
		for (int i = 0; i < spring.cells.length; i++) {
			int cell = spring.cells[i];
			if (!canEverHoldWater(level, cell) || level.hasTerrain(cell, Terrain.WATER)) continue;
			missing++;
			if (spring.dist[i] < ring) ring = spring.dist[i];
		}

		// 水体完整
		if (missing == 0) {
			spring.wait = 0;
			spring.missing = false;
			spring.lastMissing = 0;
			return false;
		}

		// 刚被清掉，本回合只开始计时
		if (!spring.missing) {
			spring.missing = true;
			spring.wait = WAIT_TURNS;
			spring.lastMissing = missing;
			return false;
		}

		// 回填途中又被清掉，重新计时（本回合只计时，和刚被清空时一致）
		if (missing > spring.lastMissing) {
			spring.wait = WAIT_TURNS;
			spring.lastMissing = missing;
			return false;
		}
		if (spring.wait > 0) {
			spring.wait--;
			if (spring.wait > 0) {
				spring.lastMissing = missing;
				return false;
			}
		}

		// 从离出水口最近的那圈缺口开始填，不去碰已经有水的地方
		boolean filled = false;
		for (int i = 0; i < spring.cells.length; i++) {
			if (spring.dist[i] != ring) continue;
			int cell = spring.cells[i];
			if (!canEverHoldWater(level, cell) || level.hasTerrain(cell, Terrain.WATER)) continue;
			// 陷阱、植物占住的位置先跳过，等条件恢复再填
			if (level.traps.get(cell) != null || level.plants.get(cell) != null) continue;
			level.cellAt(cell).place(Terrain.WATER);
			filled = true;
		}

		int remaining = 0;
		for (int cell : spring.cells) {
			if (canEverHoldWater(level, cell) && !level.hasTerrain(cell, Terrain.WATER)) remaining++;
		}
		spring.lastMissing = remaining;

		return filled;
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
		bundle.put(COUNT, springs.size());
		for (int i = 0; i < springs.size(); i++) {
			Spring spring = springs.get(i);
			bundle.put("outlet_" + i, spring.outlet);
			bundle.put("cells_" + i, spring.cells);
			bundle.put("dist_" + i, spring.dist);
			bundle.put("wait_" + i, spring.wait);
			bundle.put("missing_" + i, spring.missing);
			bundle.put("last_missing_" + i, spring.lastMissing);
			bundle.put("dead_" + i, spring.dead);
		}
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		springs.clear();
		for (int i = 0; i < bundle.getInt(COUNT); i++) {
			Spring spring = new Spring();
			spring.outlet = bundle.getInt("outlet_" + i);
			spring.cells = bundle.getIntArray("cells_" + i);
			spring.dist = bundle.getIntArray("dist_" + i);
			spring.wait = bundle.getInt("wait_" + i);
			spring.missing = bundle.getBoolean("missing_" + i);
			spring.lastMissing = bundle.getInt("last_missing_" + i);
			spring.dead = bundle.getBoolean("dead_" + i);
			springs.add(spring);
			// 读档时如果正在回填，直接接着算，不必等下一次缺水事件
			if (spring.missing) dirty = true;
		}
	}
}
