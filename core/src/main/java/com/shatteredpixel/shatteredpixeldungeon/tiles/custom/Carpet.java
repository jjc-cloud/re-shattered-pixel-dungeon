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
import com.shatteredpixel.shatteredpixeldungeon.levels.CellContent;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.SparseArray;

//TODO currently carpets only have implemented visuals for the dwarven city,
// and also only support being rectangular in shape
public class Carpet extends CustomTilemap {

	{
		texture = Assets.Environment.CARPET;
	}

	private boolean contentsRegistered;
	protected SparseArray<Integer> tileOverrides = new SparseArray<>();

	public static final int SKIP = -1;
	// 破损缝按边取图，每个区域块间隔 32 格（贴图第 7/9/11 行）。
	private static final int CITY_SEAM = 96;    // 都市（含王座厅与秘库支线）
	private static final int PRISON_SEAM = 128; // 监狱与矿洞（两区域同色，共用一块）
	private static final int SEWER_SEAM = 160;  // 下水道（实际只有王鼠房铺地毯）
	// 每条边 4 个端头变体：完整、切 A 端、切 B 端、两端都切。
	// 0=上(切右/切左)，1=右(切上/切下)，2=下(切左/切右)，3=左(切上/切下)。
	private static final int[] SEAM_FULL = {0, 1, 2, 3};
	private static final int[] SEAM_CUT_A = {4, 6, 8, 10};
	private static final int[] SEAM_CUT_B = {5, 7, 9, 11};
	private static final int[] SEAM_CUT_BOTH = {12, 13, 14, 15};

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

	public void registerContents(Level level) {
		if (contentsRegistered) return;
		contentsRegistered = true;
		for (int y = 0; y < tileH; y++) for (int x = 0; x < tileW; x++) {
			int index = x + y * tileW;
			int override = tileOverrides.get(index, 0);
			int cell = tileX + x + (tileY + y) * level.width();
			if (override != SKIP && cell >= 0 && cell < level.length())
				level.cellAt(cell).addContent(new CellContent(CellContent.CARPET, level.region()));
		}
	}

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
		if (tileOverrides.containsKey(index) && tileOverrides.get(index) == override) return false;
		tileOverrides.put(index, override);
		return true;
	}

	/**
	 * 地毯基础层。没有地毯（含生成期预留的洞口）的格子留空，
	 * 洞口边缘由破损缝图层按周围现存的地毯现场拼接。
	 */
	public synchronized int[] createLayers() {
		registerContents(Dungeon.level);
		int[] base = new int[tileW * tileH];
		int regionOfs = 16 * ((Dungeon.depth - 1) / 5);
		for (int y = 0; y < tileH; y++) {
			for (int x = 0; x < tileW; x++) {
				int i = x + tileW * y;
				int cell = tileX + x + (tileY + y) * Dungeon.level.width();
				boolean carpet = tileOverrides.get(i, 0) != SKIP
						&& Dungeon.level.cellAt(cell).hasTerrain(CellContent.CARPET);
				if (!carpet) {
					base[i] = SKIP;
					continue;
				}
				if (tileOverrides.containsKey(i)) {
					base[i] = tileOverrides.get(i);
				} else {
					base[i] = regionOfs;
					if (y == 0) base[i] += 1;
					if (x == tileW - 1) base[i] += 2;
					if (y == tileH - 1) base[i] += 4;
					if (x == 0) base[i] += 8;
				}
			}
		}
		return base;
	}

	/**
	 * 破损缝：0=上 1=右 2=下 3=左，返回贴图索引，-1 表示该边不画。
	 * 只管“这条边对面还有没有地毯”和“端头顺着缝能不能接下去”，不查任何组合表。
	 */
	public static int seamPiece(int cell, int side) {
		Level level = Dungeon.level;
		if (level == null || cell < 0 || cell >= level.length()) return -1;
		if (level.cellAt(cell).hasTerrain(CellContent.CARPET)) return -1;
		int seamBase = seamBase();
		if (seamBase < 0 || !insideCarpet(cell)) return -1;

		int w = level.width();
		int x = cell % w, y = cell / w;
		int piece;
		switch (side) {
			case 0: // 上
				if (!carpetAt(x, y - 1)) return -1;
				piece = seamVariant(side,
						!(carpetAt(x + 1, y) || carpetAt(x + 1, y - 1)),
						!(carpetAt(x - 1, y) || carpetAt(x - 1, y - 1)));
				break;
			case 1: // 右
				if (!carpetAt(x + 1, y)) return -1;
				piece = seamVariant(side,
						!(carpetAt(x, y - 1) || carpetAt(x + 1, y - 1)),
						!(carpetAt(x, y + 1) || carpetAt(x + 1, y + 1)));
				break;
			case 2: // 下
				if (!carpetAt(x, y + 1)) return -1;
				piece = seamVariant(side,
						!(carpetAt(x - 1, y) || carpetAt(x - 1, y + 1)),
						!(carpetAt(x + 1, y) || carpetAt(x + 1, y + 1)));
				break;
			case 3: // 左
				if (!carpetAt(x - 1, y)) return -1;
				piece = seamVariant(side,
						!(carpetAt(x, y - 1) || carpetAt(x - 1, y - 1)),
						!(carpetAt(x, y + 1) || carpetAt(x - 1, y + 1)));
				break;
			default:
				return -1;
		}
		return seamBase + piece;
	}

	//cutA/cutB 对应贴图里该边两个端头的切法，顺序见 SEAM_CUT_A / SEAM_CUT_B 的注释
	private static int seamVariant(int side, boolean cutA, boolean cutB) {
		if (cutA) return cutB ? SEAM_CUT_BOTH[side] : SEAM_CUT_A[side];
		return cutB ? SEAM_CUT_B[side] : SEAM_FULL[side];
	}

	private static int seamBase() {
		if (Dungeon.depth < 1) return -1;
		int stage = (Dungeon.depth - 1) / 5;
		if (stage == 0) return SEWER_SEAM;
		if (stage == 1 || stage == 2) return PRISON_SEAM;
		if (stage == 3) return CITY_SEAM;
		return -1;
	}

	private static boolean carpetAt(int x, int y) {
		Level level = Dungeon.level;
		if (level == null) return false;
		if (x < 0 || y < 0 || x >= level.width() || y >= level.height()) return false;
		return level.cellAt(x + y * level.width()).hasTerrain(CellContent.CARPET);
	}

	// 缝只画在地毯矩形内部的洞上，矩形外贴着地毯的地板不画
	private static boolean insideCarpet(int cell) {
		Level level = Dungeon.level;
		int w = level.width();
		int x = cell % w, y = cell / w;
		for (CustomTilemap tile : level.customTiles) {
			if (!(tile instanceof Carpet)) continue;
			if (x >= tile.tileX && y >= tile.tileY
					&& x < tile.tileX + tile.tileW && y < tile.tileY + tile.tileH) return true;
		}
		return false;
	}

	@Override
	public synchronized Image image(int tileX, int tileY) {
		return super.image(tileX, tileY);
	}

	@Override
	public boolean allowWater(int tileX, int tileY) {
		int cell = this.tileX + tileX + (this.tileY + tileY) * Dungeon.level.width();
		return !Dungeon.level.cellAt(cell).hasTerrain(CellContent.CARPET);
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
		bundle.put("contents_registered", contentsRegistered);
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
		contentsRegistered = bundle.getBoolean("contents_registered");
		tileOverrides.clear();
		int[] keys = bundle.getIntArray(OVERRIDES_KEYS);
		int[] values = bundle.getIntArray(OVERRIDES_VALUES);
		for (int i = 0; i < keys.length; i++){
			tileOverrides.put(keys[i], values[i]);
		}
	}
}
