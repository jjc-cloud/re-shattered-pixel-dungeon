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

package com.shatteredpixel.shatteredpixeldungeon.tiles.custom;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Tilemap;
import com.watabou.utils.Bundle;
import com.watabou.utils.SparseArray;

//TODO currently carpets only have implemented visuals for the dwarven city,
// and also only support being rectangular in shape
public class Carpet extends CustomTilemap {

	{
		texture = Assets.Environment.CARPET;
	}

	protected SparseArray<Integer> tileOverrides = new SparseArray<>();
	private volatile boolean updateQueued;

	public static final int SKIP = -1;
	public static final int DESTROYED = -2;
	private static final int CITY_DAMAGE_BASE = 96;    // 地毯贴图第 7–9 行：都市（含王座厅与秘库支线）
	private static final int PRISON_DAMAGE_BASE = 144; // 地毯贴图第 10–12 行：监狱与矿洞（两区域同色，共用一块）
	private static final int SEWER_DAMAGE_BASE = 192;  // 地毯贴图第 13–15 行：下水道（实际只有王鼠房铺地毯）
	// 上、右、下、左四向连接的位值依次为 1、2、4、8。
	private static final int[] INNER_DAMAGE = {
			SKIP, 4, 5, 2, 6, 14, 3, 11,
			7, 1, 13, 10, 0, 9, 8, 12
	};
	// 三向形状（只缺一边）里，缺口那一侧正好落在地毯外缘时的内圈图；
	// 索引顺序与 INNER_DAMAGE 相同，用不到的 connected 留 SKIP。
	// 7 = 上+右+下缺左，11 = 上+右+左缺下，13 = 上+下+左缺右，14 = 右+下+左缺上。
	private static final int[] INNER_EDGE = {
			SKIP, SKIP, SKIP, SKIP, SKIP, SKIP, SKIP, 45,
			SKIP, SKIP, SKIP, 44, SKIP, 43, 42, SKIP
	};
	// 通道型（只连对边）的垂直两侧贴着地毯外缘时的图；列依次是都不靠外缘、第一条、第二条、两条都靠外缘。
	// 行依次是竖毯（只连上下：左、右外缘）、横毯（只连左右：上、下外缘）。
	// 竖毯贴右缘那一张画在装饰行（80–95）里，不在区域块内，用 OUT_OF_BLOCK 占位、按区域另取。
	private static final int OUT_OF_BLOCK = -100;
	private static final int CITY_STRAIGHT_EDGE = 90;
	private static final int PRISON_STRAIGHT_EDGE = 92;
	private static final int SEWER_STRAIGHT_EDGE = 94;
	private static final int[][] STRAIGHT_DAMAGE = {
			{14, 47, OUT_OF_BLOCK, 41},
			{13, 15, 46, 40}
	};
	// 单向连接；列依次是不在外缘、第一条垂直外缘、第二条垂直外缘、两条垂直外缘。
	private static final int[][] SINGLE_DAMAGE = {
			{4, 33, 32, 28}, // 上：左、右外缘
			{5, 34, 35, 29}, // 右：上、下外缘
			{6, 36, 37, 30}, // 下：左、右外缘
			{7, 38, 39, 31}  // 左：上、下外缘
	};
	// 相邻双向连接；行依次为上右、右下、上左、下左，列为缺失方向上的外缘组合。
	private static final int[][] PAIR_DAMAGE = {
			{2, 23, 26, 18},
			{3, 21, 27, 19},
			{1, 24, 22, 17},
			{0, 20, 25, 16}
	};
	//first 80 tiles are regular carpet and stiching, so we start customs at 80
	public static final int CITY_STATUE = 80;
	public static final int CITY_PEDESTAL = 81;
	public static final int CITY_ENTRANCE = 82;
	public static final int CITY_STATUE_TR = 83;
	public static final int CITY_STATUE_BR = 84;
	public static final int CITY_STATUE_TL = 85;
	public static final int CITY_STATUE_BL = 86;
	public static final int CITY_PEDESTAL_TR = 87;
	public static final int CITY_PEDESTAL_TL = 88;

	//specify a tile (in carpet coordinates) to override. 0,0 is top-left
	public void overrideTile(int x, int y, int override){
		tileOverrides.put(x + tileW*y, override);
	}

	//specify a tile (int level coords) to override
	public void overrideTile(Level level, int x, int y, int override){
		x -= tileX;
		y -= tileY;
		tileOverrides.put(x + tileW*y, override);
	}

	//specify a tile (in level map index) to override
	public synchronized boolean overrideTile(int tile, Level level, int override){
		int x = tile % level.width() - tileX;
		int y = tile / level.width() - tileY;
		if (x < 0 || y < 0 || x >= tileW || y >= tileH) return false;
		int index = x + tileW*y;
		if (override == DESTROYED && tileOverrides.containsKey(index)
				&& tileOverrides.get(index) == SKIP) return false;
		if (tileOverrides.containsKey(index) && tileOverrides.get(index) == override) return false;
		tileOverrides.put(index, override);
		if (vis != null && vis.alive && vis.parent != null && !updateQueued) {
			updateQueued = true;
			Game.runOnRenderThread(() -> {
				updateQueued = false;
				if (vis != null && vis.alive && vis.parent != null) create();
			});
		}
		return true;
	}

	@Override
	public synchronized Tilemap create() {
		// 场景销毁会释放缓冲区并清空 parent，但不会清除 alive；返回游戏时必须重建。
		Tilemap v = vis != null && vis.alive && vis.parent != null ? vis : super.create();
		int[] data = new int[tileW*tileH];
		boolean[] intact = new boolean[data.length];
		int regionOfs = 16 * (int)((Dungeon.depth-1)/5);
		//按区域选破坏过渡图的基址；三块的配色分别对应都市红、监狱/矿洞蓝灰、下水道绿
		int damageBase = regionOfs == 48 ? CITY_DAMAGE_BASE
				: regionOfs == 0 ? SEWER_DAMAGE_BASE
				: regionOfs == 16 || regionOfs == 32 ? PRISON_DAMAGE_BASE
				: SKIP;
		for (int y = 0; y < tileH; y++){
			for (int x = 0; x < tileW; x++){
				int i = x + tileW*y;
				if (tileOverrides.containsKey(i)){
					int override = tileOverrides.get(i);
					data[i] = override == DESTROYED ? SKIP : override;
				} else {
					data[i] = regionOfs;
					if (y == 0) data[i] += 1;
					if (x == tileW - 1) data[i] += 2;
					if (y == tileH - 1) data[i] += 4;
					if (x == 0) data[i] += 8;
				}
				intact[i] = data[i] >= 0;
			}
		}
		if (damageBase != SKIP) {
			for (int y = 0; y < tileH; y++){
				for (int x = 0; x < tileW; x++){
					int i = x + tileW*y;
					if (!tileOverrides.containsKey(i) || tileOverrides.get(i) != DESTROYED) continue;
					int cell = tileX + x + (tileY + y) * Dungeon.level.width();
					int ground = Dungeon.level.map[cell];
					if (ground != Terrain.EMBERS && ground != Terrain.EMBERS_SP
							&& ground != Terrain.STATUE_EMBERS && ground != Terrain.STATUE_SP_EMBERS
							&& ground != Terrain.CUSTOM_DECO_EMBERS_SP) continue;
					int connected = 0;
					if (y > 0 && intact[i-tileW]) connected |= 1;
					if (x < tileW-1 && intact[i+1]) connected |= 2;
					if (y < tileH-1 && intact[i+tileW]) connected |= 4;
					if (x > 0 && intact[i-1]) connected |= 8;
					if (connected == 0) continue;
					int border = (y == 0 ? 1 : 0) | (x == tileW-1 ? 2 : 0)
							| (y == tileH-1 ? 4 : 0) | (x == 0 ? 8 : 0);
					int visual = INNER_DAMAGE[connected];
					if (Integer.bitCount(connected) == 1) {
						int direction = Integer.numberOfTrailingZeros(connected);
						int ends = (direction & 1) == 0
								? ((border & 8) != 0 ? 1 : 0) | ((border & 2) != 0 ? 2 : 0)
								: ((border & 1) != 0 ? 1 : 0) | ((border & 4) != 0 ? 2 : 0);
						visual = SINGLE_DAMAGE[direction][ends];
					} else if (connected == 5 || connected == 10) {
						int direction = connected == 5 ? 0 : 1;
						int ends = direction == 0
								? ((border & 8) != 0 ? 1 : 0) | ((border & 2) != 0 ? 2 : 0)
								: ((border & 1) != 0 ? 1 : 0) | ((border & 4) != 0 ? 2 : 0);
						visual = STRAIGHT_DAMAGE[direction][ends];
						if (visual == OUT_OF_BLOCK){
							//不在区域块里，按区域取绝对索引再换算成相对 damageBase 的偏移
							int slot = damageBase == CITY_DAMAGE_BASE ? CITY_STRAIGHT_EDGE
									: damageBase == PRISON_DAMAGE_BASE ? PRISON_STRAIGHT_EDGE
									: SEWER_STRAIGHT_EDGE;
							visual = slot - damageBase;
						}
					} else if (connected == 3 || connected == 6 || connected == 9 || connected == 12) {
						int missing = (~connected) & 15;
						int first = Integer.lowestOneBit(missing);
						int ends = ((border & first) != 0 ? 1 : 0)
								| ((border & (missing ^ first)) != 0 ? 2 : 0);
						int pair = connected == 3 ? 0 : connected == 6 ? 1 : connected == 9 ? 2 : 3;
						visual = PAIR_DAMAGE[pair][ends];
					} else if (INNER_EDGE[connected] != SKIP
							&& (border & ((~connected) & 15)) != 0) {
						//缺口那一侧就是地毯外缘，不能沿用"缺口只是另一格余烬"的图
						visual = INNER_EDGE[connected];
					}
					data[i] = damageBase + visual;
				}
			}
		}
		v.map( data, tileW );
		return v;
	}

	//for now we assume that overrides should give the text/desc of the base tile

	@Override
	public String name(int tileX, int tileY) {
		int cell = tileX + tileW*tileY;
		if (tileOverrides.containsKey(cell)){
			return null;
		}
		return Messages.get(this, "name");
	}

	@Override
	public String desc(int tileX, int tileY) {
		int cell = tileX + tileW*tileY;
		if (tileOverrides.containsKey(cell)){
			return null;
		}
		return Messages.get(this, "desc");
	}

	public static final String OVERRIDES_KEYS = "overrides_keys";
	public static final String OVERRIDES_VALUES = "overrides_values";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		int[] keys = tileOverrides.keyArray();
		int[] values = new int[keys.length];
		for (int i = 0; i < keys.length; i++){
			values[i] = tileOverrides.get(keys[i]);
		}
		bundle.put(OVERRIDES_KEYS, keys);
		bundle.put(OVERRIDES_VALUES, values);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		tileOverrides.clear();
		int[] keys = bundle.getIntArray(OVERRIDES_KEYS);
		int[] values = bundle.getIntArray(OVERRIDES_VALUES);
		for (int i = 0; i < keys.length; i++){
			tileOverrides.put(keys[i], values[i]);
		}
	}
}
