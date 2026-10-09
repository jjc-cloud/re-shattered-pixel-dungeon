/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SmokeScreen;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.WaterVapor;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ShadowCaster;

import java.util.Arrays;
import java.util.BitSet;

/** 楼层共享照明；角色只读取结果，光源和地形变化按块更新。 */
public final class LevelLighting {

	public static final int MAX_SIGHT_DISTANCE = 16;
	private static final int BLOCK_SIZE = 8;

	private final Level level;
	private final int width, height, columns, blocks;
	private final int environment;
	private final int[] sourceRadius;
	private final boolean[] staticLight, illuminated, litBlocks;
	private final boolean[][] blocking = new boolean[4][];
	private final BitSet staticDirty = new BitSet();
	private final BitSet dirty = new BitSet();
	private boolean initialized;
	private int maxRadius;

	LevelLighting(Level level) {
		this.level = level;
		width = level.width();
		height = level.height();
		columns = (width + BLOCK_SIZE - 1) / BLOCK_SIZE;
		blocks = columns * ((height + BLOCK_SIZE - 1) / BLOCK_SIZE);
		String texture = level.tilesTex();
		environment = Assets.Environment.TILES_PRISON.equals(texture) ? 1
				: Assets.Environment.TILES_CAVES.equals(texture) ? 2
				: Assets.Environment.TILES_CITY.equals(texture) ? 3 : 0;
		sourceRadius = new int[level.length()];
		staticLight = new boolean[level.length()];
		illuminated = new boolean[level.length()];
		litBlocks = new boolean[blocks];
	}

	int terrainRadius(int terrain) {
		if (environment == 1 && terrain == Terrain.WALL_DECO) return 1;
		if (environment == 2 && (terrain == Terrain.GRASS || terrain == Terrain.HIGH_GRASS
				|| terrain == Terrain.FURROWED_GRASS)) return 0;
		if (environment == 3 && (terrain == Terrain.REGION_DECO || terrain == Terrain.REGION_DECO_ALT)) return 2;
		return -1;
	}

	void terrainChanged(int cell) {
		if (!initialized) return;
		int radius = level.staticEnvironmentalLightRadius(cell);
		maxRadius = Math.max(maxRadius, radius);
		sourceRadius[cell] = radius;
		int x = cell % width, y = cell / width;
		// 遮挡变化也会改变附近光源的投射，不只更新光源所在格。
		invalidate(x - maxRadius, y - maxRadius, x + maxRadius + 1, y + maxRadius + 1, true);
	}

	public void blobChanged(Blob blob) {
		if (!initialized || (blob.lightRadius() < 0
				&& !(blob instanceof SmokeScreen) && !(blob instanceof WaterVapor))) return;
		int radius = Math.max(0, blob.lightRadius());
		if (blob.area.isEmpty()) {
			// 读档后尚未建立包围框时不能漏掉既存来源。
			dirty.set(0, blocks);
		} else {
			invalidate(blob.area.left - radius - 1, blob.area.top - radius - 1,
					blob.area.right + radius + 1, blob.area.bottom + radius + 1, false);
		}
	}

	public void blobChanged(Blob blob, int cell) {
		if (!initialized || (blob.lightRadius() < 0
				&& !(blob instanceof SmokeScreen) && !(blob instanceof WaterVapor))) return;
		int radius = Math.max(0, blob.lightRadius());
		int x = cell % width, y = cell / width;
		invalidate(x - radius, y - radius, x + radius + 1, y + radius + 1, false);
	}

	private void invalidate(int left, int top, int right, int bottom, boolean terrain) {
		left = Math.max(0, left) / BLOCK_SIZE;
		top = Math.max(0, top) / BLOCK_SIZE;
		right = (Math.min(width, right) - 1) / BLOCK_SIZE;
		bottom = (Math.min(height, bottom) - 1) / BLOCK_SIZE;
		for (int y = top; y <= bottom; y++) {
			int start = y * columns + left, end = y * columns + right + 1;
			dirty.set(start, end);
			if (terrain) staticDirty.set(start, end);
		}
	}

	private void update() {
		if (!initialized) {
			for (int cell = 0; cell < sourceRadius.length; cell++) {
				sourceRadius[cell] = level.staticEnvironmentalLightRadius(cell);
				maxRadius = Math.max(maxRadius, sourceRadius[cell]);
			}
			staticDirty.set(0, blocks);
			dirty.set(0, blocks);
			initialized = true;
		}
		if (dirty.isEmpty()) return;
		Blob smoke = level.blobs.get(SmokeScreen.class);
		Blob vapor = level.blobs.get(WaterVapor.class);
		for (int block = dirty.nextSetBit(0); block >= 0; block = dirty.nextSetBit(block + 1)) {
			int left = block % columns * BLOCK_SIZE, top = block / columns * BLOCK_SIZE;
			int right = Math.min(width, left + BLOCK_SIZE), bottom = Math.min(height, top + BLOCK_SIZE);
			if (staticDirty.get(block)) {
				for (int y = top; y < bottom; y++) Arrays.fill(staticLight, left + y * width, right + y * width, false);
				for (int y = Math.max(0, top - maxRadius); y < Math.min(height, bottom + maxRadius); y++) {
					for (int x = Math.max(0, left - maxRadius); x < Math.min(width, right + maxRadius); x++) {
						int cell = x + y * width;
						if (sourceRadius[cell] >= 0) {
							ShadowCaster.castLight(x, y, width, staticLight, level.losBlocking,
									sourceRadius[cell], left, top, right, bottom);
						}
					}
				}
			}
			for (int y = top; y < bottom; y++) {
				System.arraycopy(staticLight, left + y * width, illuminated, left + y * width, right - left);
			}
			for (Blob blob : level.blobs.values()) {
				int radius = blob.lightRadius();
				if (radius < 0 || blob.cur == null) continue;
				for (int y = Math.max(0, top - radius); y < Math.min(height, bottom + radius); y++) {
					for (int x = Math.max(0, left - radius); x < Math.min(width, right + radius); x++) {
						int cell = x + y * width;
						if (blob.cur[cell] <= 0) continue;
						if (radius == 0) illuminated[cell] = true;
						else ShadowCaster.castLight(x, y, width, illuminated, level.losBlocking,
								radius, left, top, right, bottom);
					}
				}
			}
			boolean lit = false;
			for (int y = top; y < bottom; y++) {
				for (int x = left; x < right; x++) {
					int cell = x + y * width;
					lit |= illuminated[cell];
					for (int policy = 0; policy < blocking.length; policy++) {
						if (blocking[policy] != null) blocking[policy][cell] = blocksSight(cell, policy, smoke, vapor);
					}
				}
			}
			litBlocks[block] = lit;
		}
		dirty.clear();
		staticDirty.clear();
	}

	private boolean blocksSight(int cell, int policy, Blob smoke, Blob vapor) {
		boolean grass = level.map[cell] == Terrain.HIGH_GRASS || level.map[cell] == Terrain.FURROWED_GRASS;
		return (level.losBlocking[cell] && ((policy & 1) == 0 || !grass))
				|| ((policy & 2) == 0 && smoke != null && smoke.cur != null && smoke.cur[cell] > 0)
				|| (vapor != null && vapor.cur != null && vapor.cur[cell] > 0);
	}

	public boolean[] blocking(boolean seeGrass, boolean seeSmoke) {
		update();
		int policy = (seeGrass ? 1 : 0) | (seeSmoke ? 2 : 0);
		if (blocking[policy] == null) {
			blocking[policy] = new boolean[level.length()];
			Blob smoke = level.blobs.get(SmokeScreen.class), vapor = level.blobs.get(WaterVapor.class);
			for (int cell = 0; cell < level.length(); cell++) blocking[policy][cell] = blocksSight(cell, policy, smoke, vapor);
		}
		return blocking[policy];
	}

	public boolean[] visibleLights(int cell) {
		update();
		int x = cell % width, y = cell / width;
		int left = Math.max(0, x - MAX_SIGHT_DISTANCE) / BLOCK_SIZE;
		int right = Math.min(width - 1, x + MAX_SIGHT_DISTANCE) / BLOCK_SIZE;
		int top = Math.max(0, y - MAX_SIGHT_DISTANCE) / BLOCK_SIZE;
		int bottom = Math.min(height - 1, y + MAX_SIGHT_DISTANCE) / BLOCK_SIZE;
		for (int by = top; by <= bottom; by++) {
			for (int bx = left; bx <= right; bx++) {
				if (litBlocks[bx + by * columns]) return illuminated;
			}
		}
		return null;
	}
}
