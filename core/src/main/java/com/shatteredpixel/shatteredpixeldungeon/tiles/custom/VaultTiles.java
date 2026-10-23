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
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.vault.VaultLayout;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.watabou.noosa.Tilemap;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.CityLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.Arrays;

/** Vault floor, gold, carpet, decorations and doors from the quest atlas. */
public final class VaultTiles {
	private VaultTiles() {}

	/** Plain floor and the supplied edge variants share complete dotted intersections. */
	public static class PlainFloor extends CustomTilemap {
		public PlainFloor() {
			texture = Assets.Environment.CITY_QUEST;
			setRect(3, 13, 35, 32);
		}

		private boolean plain(int x, int y) {
			if (x < tileX || x >= tileX + tileW || y < tileY || y >= tileY + tileH) return false;
			char tile = VaultLayout.tile(x, y);
			int terrain = Dungeon.level.terrainAt(x + y * Dungeon.level.width());
			return (tile == '.' && (terrain == Terrain.EMPTY_SP || terrain == Terrain.EMPTY || terrain == Terrain.EMPTY_DECO))
					|| (tile == 'D' && (terrain == Terrain.CUSTOM_DECO || terrain == Terrain.EMPTY
					|| terrain == Terrain.EMPTY_DECO || terrain == Terrain.DOOR || terrain == Terrain.LOCKED_DOOR
					|| terrain == Terrain.OPEN_DOOR));
		}

		private int transition(int x, int y) {
			boolean west = plain(x - 1, y), east = plain(x + 1, y), south = plain(x, y + 1);
			boolean southwest = west || south || plain(x - 1, y + 1);
			boolean southeast = east || south || plain(x + 1, y + 1);
			int mask = (west ? 1 : 0) | (east ? 2 : 0) | (south ? 4 : 0)
					| (southwest ? 8 : 0) | (southeast ? 16 : 0);
			// The fixed approach widens downwards, so it needs south, west and east edges.
			// Diagonal contacts must also remove the quarter-circle at the shared corner.
			switch (mask) {
				case 28: return 12; // south edge
				case 30: return 13; // south and east edges
				case 16: return 14; // southeast corner only
				case 18: return 15; // east edge
				case 29: return 29; // south and west edges
				case 8:  return 30; // southwest corner only
				case 9:  return 31; // west edge
				default: return 11; // 带圆点的地板使用任务图集，不再显示都市底图。
			}
		}

		public int[] tileData() {
			int[] data = new int[tileW * tileH];
			Arrays.fill(data, -1);
			for (int y = 0; y < tileH; y++) {
				for (int x = 0; x < tileW; x++) {
					int gx = tileX + x, gy = tileY + y;
					char tile = VaultLayout.tile(gx, gy);
					int terrain = Dungeon.level.terrainAt(gx + gy * Dungeon.level.width());
					if (plain(gx, gy)) {
						data[x + y * tileW] = 16 + 12;
					} else if (tile == ':' && terrain == Terrain.EMPTY_SP) {
						data[x + y * tileW] = transition(gx, gy);
					}
				}
			}
			return data;
		}

		@Override
		public Tilemap create() {
			Tilemap result = super.create();
			result.map(tileData(), tileW);
			return result;
		}
	}

	public static class GoldFloor extends CustomTilemap {
		private int[] tiles;

		public GoldFloor() {
			texture = Assets.Environment.CITY_QUEST;
			setRect(15, 1, 11, 11);
		}

		private boolean wall(int x, int y) {
			return VaultLayout.tile(x, y) == '#';
		}

		public int[] tileData() {
			if (tiles == null) {
				tiles = new int[tileW * tileH];
				Arrays.fill(tiles, -1);
				for (int y = 0; y < tileH; y++) {
					for (int x = 0; x < tileW; x++) {
						int gx = tileX + x, gy = tileY + y;
						char tile = VaultLayout.tile(gx, gy);
						int visual;
						if (tile == 'p') visual = 7 * 16 + 3;
						else if (tile != 'g') continue;
						else if (wall(gx, gy - 1)) {
							visual = 6 * 16 + 2;
							if (wall(gx + 1, gy)) visual++;
							else if (wall(gx - 1, gy)) visual += 2;
						} else if (wall(gx + 1, gy)) visual = 6 * 16;
						else if (wall(gx - 1, gy)) visual = 6 * 16 + 1;
						else visual = 7 * 16 + Random.Int(3);
						tiles[x + y * tileW] = visual;
					}
				}
			}
			return tiles.clone();
		}

		@Override
		public Tilemap create() {
			Tilemap result = super.create();
			result.map(tileData(), tileW);
			return result;
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("gold_tiles", tileData());
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			tiles = bundle.getIntArray("gold_tiles");
		}
	}

	/** 直走廊地毯：靠入口一端接入口地毯，靠大门一端用转角收口。 */
	public static class EntranceCarpet extends Carpet {
		public EntranceCarpet() {
			setRect(19, 45, 3, 23);
			restorePattern();
		}

		private void restorePattern() {
			for (int y = 0; y < tileH; y++) {
				for (int x = 0; x < tileW; x++) {
					if (y == 0){
						//大门一侧原本是敞口，换成左上角、上边、右上角把口收住
						overrideTile(x, y, 48 + (x == 0 ? 9 : x == tileW - 1 ? 3 : 1));
					} else {
						overrideTile(x, y, 48 + (x == 0 ? 8 : x == tileW - 1 ? 2 : 0));
					}
				}
			}
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			restorePattern();
		}

	}

	/** Literal atlas cells for the supplied platform and surrounding entrance rug. */
	public static class EntranceRug extends CustomTilemap {
		@Override
		public Tilemap create() {
			Tilemap v = super.create();
			v.map(tileData(), tileW);
			return v;
		}

		public EntranceRug() {
			texture = Assets.Environment.CITY_QUEST;
			setRect(18, 68, 5, 4);
		}

		public int[] tileData() {
			return new int[]{
					44, 24, 25, 26, 45,
					46, 40, 41, 42, 47,
					60, 56, 57, 58, 61,
					-1, 60, 62, 61, -1
			};
		}

		@Override
		public String name(int x, int y) {
			return x >= 1 && x <= 3 && y < 3 ? Messages.get(this, "name") : Messages.get(Carpet.class, "name");
		}

		@Override
		public String desc(int x, int y) {
			return x >= 1 && x <= 3 && y < 3 ? Messages.get(this, "desc") : Messages.get(Carpet.class, "desc");
		}
	}

	public static class EntranceDecorations extends CustomTilemap {
		public EntranceDecorations() {
			texture = Assets.Environment.CITY_QUEST;
			setRect(9, 45, 23, 28);
		}

		public int[] tileData() {
			int[] data = new int[tileW * tileH];
			Arrays.fill(data, -1);
			for (int y = 0; y < tileH; y++) {
				for (int x = 0; x < tileW; x++) {
					int gx = tileX + x, gy = tileY + y;
					int terrain = Dungeon.level.terrainAt(gx + gy * Dungeon.level.width());
					if (terrain == Terrain.STATUE) {
						if (VaultLayout.tile(gx, gy) == 's') data[x + y * tileW] = 10 * 16 + (gx == 24 ? 4 : 3);
					}
					if (y + 1 < tileH && VaultLayout.tile(gx, gy + 1) == 's'
							&& (Dungeon.level.hasTerrain(gx + (gy + 1) * Dungeon.level.width(), Terrain.STATUE))) {
						data[x + y * tileW] = 9 * 16 + (gx == 24 ? 4 : 3);
					}
				}
			}
			return data;
		}

		@Override
		public Tilemap create() {
			Tilemap result = super.create();
			result.map(tileData(), tileW);
			return result;
		}
		@Override
		public String name(int x, int y) {
			if (!(Dungeon.level instanceof VaultLevel) || !((VaultLevel) Dungeon.level)
					.customStatueCell(tileX + x + (tileY + y) * Dungeon.level.width())) return null;
			return Messages.get(Level.class, "statue_name");
		}

		@Override
		public String desc(int x, int y) {
			if (!(Dungeon.level instanceof VaultLevel) || !((VaultLevel) Dungeon.level)
					.customStatueCell(tileX + x + (tileY + y) * Dungeon.level.width())) return null;
			return Messages.get(CityLevel.class, "statue_desc");
		}

	}

	/** Supplied single-cell artwork for the treasure chamber door. */
	public static class Gate extends CustomTilemap {
		public Gate() {
			texture = Assets.Environment.CITY_QUEST;
		}

		public int[] tileData() {
			int[] data = new int[tileW * tileH];
			Arrays.fill(data, -1);
			if (tileW != 1) return data;
			int cell = tileX + (tileY + tileH - 1) * Dungeon.level.width();
			int terrain = Dungeon.level.terrainAt(cell);
			if (terrain != Terrain.DOOR && terrain != Terrain.LOCKED_DOOR && terrain != Terrain.OPEN_DOOR) return data;
			for (int y = 0; y < tileH; y++) {
				//任务图集第 0 列是开门状态，上下两格都使用对应贴图。
				data[y] = (terrain == Terrain.OPEN_DOOR ? 0 : terrain == Terrain.LOCKED_DOOR ? 2 : 1) + (11 + y) * 16;
			}
			return data;
		}

		@Override
		public Tilemap create() {
			Tilemap result = super.create();
			result.map(tileData(), tileW);
			return result;
		}
	}
}
