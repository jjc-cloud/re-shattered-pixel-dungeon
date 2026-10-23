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

import com.badlogic.gdx.graphics.Pixmap;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.CellContent;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.AmbitiousImpRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.SparseArray;

public class Carpet extends CustomTilemap {

	{
		texture = Assets.Environment.CARPET;
	}

	private boolean contentsRegistered;
	protected SparseArray<Integer> tileOverrides = new SparseArray<>();

	public static final int SKIP = -1;
	public static final int SEAM_PIECES = 8;
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
	 * 地毯基础层。没有地毯的格子留空，破损边缘由破损缝图层现场拼接。
	 * 生成期明确跳过的格子（如下楼口）不作为破口。
	 */
	public synchronized int[] createLayers() {
		registerContents(Dungeon.level);
		int[] base = new int[tileW * tileH];
		for (int y = 0; y < tileH; y++) {
			for (int x = 0; x < tileW; x++) {
				base[x + tileW * y] = carpetAt(tileX + x, tileY + y);
			}
		}
		return base;
	}

	/**
	 * 破损缝：0=上 1=右 2=下 3=左，4～7=左上、右上、右下、左下角。
	 * 返回贴图索引，-1 表示不画；直边按边长选端头，斜邻地毯按接点补角。
	 */
	public static int seamPiece(int cell, int side) {
		Level level = Dungeon.level;
		if (level == null || cell < 0 || cell >= level.length()) return -1;
		if (level.cellAt(cell).hasTerrain(CellContent.CARPET)) return -1;
		int seamBase = seamBase();
		if (seamBase < 0 || !insideCarpet(cell)) return -1;

		int w = level.width();
		int x = cell % w, y = cell / w;
		switch (side) {
			case 0: // 上
				y--;
				break;
			case 1: // 右
				x++;
				break;
			case 2: // 下
				y++;
				break;
			case 3: // 左
				x--;
				break;
			case 4: // 左上
				x--;
				y--;
				break;
			case 5: // 右上
				x++;
				y--;
				break;
			case 6: // 右下
				x++;
				y++;
				break;
			case 7: // 左下
				x--;
				y++;
				break;
			default:
				return -1;
		}
		if (x < 0 || y < 0 || x >= w || y >= level.height()) return -1;
		// 入口地毯可能垫在雕像地毯下面；上层透明的边仍可能被下层填满。
		Pixmap atlas = TextureCache.get(Assets.Environment.CARPET).bitmap;
		int edgePixels = 0;
		for (CustomTilemap custom : level.customTiles) {
			Pixmap edgeAtlas = atlas;
			int visual;
			if (custom instanceof Carpet) {
				visual = ((Carpet) custom).carpetAt(x, y);
			} else if ((custom instanceof AmbitiousImpRoom.QuestEntrance || custom instanceof VaultTiles.EntranceRug)
					&& level.cellAt(x + y * w).hasTerrain(CellContent.CARPET)) {
				// 固定特殊毯虽然不能破坏，仍按实际图块的边长参与相邻破口的拼接。
				int tx = x - custom.tileX, ty = y - custom.tileY;
				if (tx < 0 || ty < 0 || tx >= custom.tileW || ty >= custom.tileH) continue;
				visual = custom instanceof VaultTiles.EntranceRug
						? ((VaultTiles.EntranceRug) custom).tileData()[tx + ty * custom.tileW]
						: tx + ty * 16;
				edgeAtlas = TextureCache.get(Assets.Environment.CITY_QUEST).bitmap;
			} else {
				continue;
			}
			if (visual < 0) continue;
			int tileX = visual % 16 * SIZE, tileY = visual / 16 * SIZE;
			for (int p = 0; p < (side >= 4 ? 1 : SIZE); p++) {
				int px = side >= 4 ? (side == 4 || side == 7 ? SIZE - 1 : 0)
						: side == 1 ? 0 : side == 3 ? SIZE - 1 : p;
				int py = side >= 4 ? (side == 4 || side == 5 ? SIZE - 1 : 0)
						: side == 0 ? SIZE - 1 : side == 2 ? 0 : p;
				if ((edgeAtlas.getPixel(tileX + px, tileY + py) & 0xFF) != 0) edgePixels |= 1 << p;
			}
		}
		if (edgePixels == 0) return -1;

		if (side >= 4) {
			int corner = side - 4;
			int px = corner == 0 || corner == 3 ? 0 : SIZE - 1;
			int py = corner < 2 ? 0 : SIZE - 1;
			int horizontal = seamPiece(cell, corner < 2 ? 0 : 2);
			int vertical = seamPiece(cell, corner == 0 || corner == 3 ? 3 : 1);
			// 接点已有直边覆盖时保持原样，其余情况用第二行的四像素角过渡。
			if (horizontal >= 0 && (atlas.getPixel(horizontal % 16 * SIZE + px,
					horizontal / 16 * SIZE + py) & 0xFF) != 0) return -1;
			if (vertical >= 0 && (atlas.getPixel(vertical % 16 * SIZE + px,
					vertical / 16 * SIZE + py) & 0xFF) != 0) return -1;
			return seamBase + 16 + corner;
		}
		boolean cutStart = (edgePixels & 1) == 0, cutEnd = (edgePixels & (1 << (SIZE - 1))) == 0;
		return seamBase + seamVariant(side,
				side == 0 ? cutEnd : cutStart, side == 0 ? cutStart : cutEnd);
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

	// 与基础层使用同一套图块选择规则，返回当前仍在绘制的地毯图块。
	private int carpetAt(int x, int y) {
		Level level = Dungeon.level;
		if (level == null) return SKIP;
		if (x < 0 || y < 0 || x >= level.width() || y >= level.height()) return SKIP;
		if (!level.cellAt(x + y * level.width()).hasTerrain(CellContent.CARPET)) return SKIP;
		int tx = x - tileX, ty = y - tileY;
		if (tx < 0 || ty < 0 || tx >= tileW || ty >= tileH) return SKIP;
		int index = tx + ty * tileW;
		if (tileOverrides.containsKey(index)) {
			// 雕像与阴影由独立物件层绘制，地毯不再夹带雕像，破坏任一方互不影响。
			switch (tileOverrides.get(index)) {
				case CITY_STATUE: return 48;
				case CITY_STATUE_TR: return 48 + 3;
				case CITY_STATUE_BR: return 48 + 6;
				case CITY_STATUE_TL: return 48 + 9;
				case CITY_STATUE_BL: return 48 + 12;
				default: return tileOverrides.get(index);
			}
		}
		int visual = 16 * ((Dungeon.depth - 1) / 5);
		if (ty == 0) visual += 1;
		if (tx == tileW - 1) visual += 2;
		if (ty == tileH - 1) visual += 4;
		if (tx == 0) visual += 8;
		return visual;
	}

	// 缝只画在原本铺有地毯的范围内，跳过生成期预留的下楼口等空位。
	private static boolean insideCarpet(int cell) {
		Level level = Dungeon.level;
		int w = level.width();
		int x = cell % w, y = cell / w;
		for (CustomTilemap tile : level.customTiles) {
			if (!(tile instanceof Carpet)) continue;
			if (x >= tile.tileX && y >= tile.tileY
					&& x < tile.tileX + tile.tileW && y < tile.tileY + tile.tileH) {
				int index = x - tile.tileX + (y - tile.tileY) * tile.tileW;
				if (((Carpet) tile).tileOverrides.get(index, 0) != SKIP) return true;
			}
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
