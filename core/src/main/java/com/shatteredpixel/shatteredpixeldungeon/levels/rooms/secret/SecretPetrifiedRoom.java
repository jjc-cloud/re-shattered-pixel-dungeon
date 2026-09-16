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

package com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.MobSpawner;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.PetrifiedStatue;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfPetrification;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet;
import com.watabou.noosa.Tilemap;
import com.watabou.utils.Bundle;
import com.watabou.utils.Point;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

public class SecretPetrifiedRoom extends SecretRoom {

	private static final int ROOM_SIZE = 15;
	private static final int CENTER = ROOM_SIZE / 2;

	//Template faces right. F is floor, D is decorated floor, S is a statue, P is the pedestal.
	private static final String[] TEMPLATE = new String[]{
			"...............",
			".......F.......",
			"....DF...F.F...",
			"..F.F.FDF.F....",
			"...DF.SFSFD....",
			"..F.FFFFFFFF...",
			".D.FSFDDDFSFFFF",
			".F.DFFDPDFFFFFF",
			"..F.SFDDDFSFFFF",
			"..DFFFFFFFFF...",
			"....DFSFSFD....",
			"...F.FFDF..F...",
			".....D...D.....",
			".......F.......",
			"..............."
	};

	@Override
	public int minWidth() {
		return ROOM_SIZE;
	}

	@Override
	public int maxWidth() {
		return ROOM_SIZE;
	}

	@Override
	public int minHeight() {
		return ROOM_SIZE;
	}

	@Override
	public int maxHeight() {
		return ROOM_SIZE;
	}

	@Override
	public boolean canConnect(Point p) {
		if (!super.canConnect(p)) return false;
		return (p.x == left || p.x == right) && p.y == top + CENTER
				|| (p.y == top || p.y == bottom) && p.x == left + CENTER;
	}

	@Override
	public void paint(Level level) {
		Painter.fill(level, this, Terrain.WALL);
		Painter.fill(level, this, 1, Terrain.EMPTY);

		int direction = entranceDirection();
		for (int templateY = 0; templateY < ROOM_SIZE; templateY++) {
			for (int templateX = 0; templateX < ROOM_SIZE; templateX++) {
				char tile = TEMPLATE[templateY].charAt(templateX);
				if (tile == '.') continue;
				Point p = templateToRoom(templateX, templateY, direction);
				if (inside(p) || p.equals(entrance())) {
					Painter.set(level, p, tile == 'P' ? Terrain.PEDESTAL : Terrain.EMPTY);
				}
			}
		}
		for (Point p : getPoints()) {
			if (!inside(p) || isFixedPoint(p)) continue;
			int roll = Random.Int(100);
			if (roll < 20) {
				Painter.set(level, p, Terrain.CHASM);
			} else if (roll < 35) {
				if (Dungeon.depth >= 6 && Dungeon.depth <= 10) {
					int prisonDecoration = Random.Int(3);
					Painter.set(level, p, prisonDecoration == 0 ? Terrain.REGION_DECO
							: prisonDecoration == 1 ? Terrain.REGION_DECO_ALT : Terrain.STATUE);
				} else {
					Painter.set(level, p, Random.Int(2) == 0
							? Terrain.REGION_DECO : Terrain.STATUE);
				}
			}
		}

		HallsFloor visuals = new HallsFloor();
		visuals.setRect(left, top, ROOM_SIZE, ROOM_SIZE);
		visuals.direction = direction;
		level.customTiles.add(visuals);

		ArrayList<Class<? extends Mob>> rotation = new ArrayList<>();
		for (int depth = 1; depth <= 24; depth++) {
			for (Class<? extends Mob> type : MobSpawner.getMobRotation(depth)) {
				if (!rotation.contains(type)) rotation.add(type);
			}
		}
		for (int templateY = 0; templateY < ROOM_SIZE; templateY++) {
			for (int templateX = 0; templateX < ROOM_SIZE; templateX++) {
				if (TEMPLATE[templateY].charAt(templateX) == 'S') {
					Mob subject;
					do {
						subject = Reflection.newInstance(Random.element(rotation));
					} while (Char.hasProp(subject, Char.Property.BOSS)
							|| Char.hasProp(subject, Char.Property.MINIBOSS)
							|| Char.hasProp(subject, Char.Property.BOSS_MINION));
					PetrifiedStatue statue = PetrifiedStatue.from(
							subject, Random.Int(2) == 0, Random.Int(2) == 0);
					statue.pos = level.pointToCell(templateToRoom(templateX, templateY, direction));
					level.mobs.add(statue);
				}
			}
		}

		Wand prize = Random.Int(2) == 0
				? (Wand)new WandOfPetrification().random()
				: (Wand)Generator.random(Generator.Category.WAND);
		prize.level(Random.chances(new float[]{3, 2, 1}) + 1);
		prize.curCharges = prize.maxCharges;
		prize.cursed = false;
		prize.cursedKnown = true;
		level.drop(prize, level.pointToCell(new Point(left + CENTER, top + CENTER)));

		entrance().set(Door.Type.HIDDEN);
	}

	@Override
	public boolean canPlaceWater(Point p) {
		return !isFixedPoint(p);
	}

	@Override
	public boolean canPlaceGrass(Point p) {
		return !isFixedPoint(p);
	}

	@Override
	public boolean canPlaceTrap(Point p) {
		return !isFixedPoint(p);
	}

	private int entranceDirection() {
		Door door = entrance();
		if (door.x == left) return LEFT;
		if (door.y == top) return TOP;
		if (door.y == bottom) return BOTTOM;
		return RIGHT;
	}

	private Point templateToRoom(int x, int y, int direction) {
		switch (direction) {
			case LEFT:
				return new Point(left + ROOM_SIZE - 1 - x, top + ROOM_SIZE - 1 - y);
			case TOP:
				return new Point(left + y, top + ROOM_SIZE - 1 - x);
			case BOTTOM:
				return new Point(left + ROOM_SIZE - 1 - y, top + x);
			case RIGHT:
			default:
				return new Point(left + x, top + y);
		}
	}

	private boolean isFixedPoint(Point p) {
		int x = p.x - left;
		int y = p.y - top;
		if (x < 0 || y < 0 || x >= ROOM_SIZE || y >= ROOM_SIZE) return false;
		switch (entranceDirection()) {
			case LEFT:
				x = ROOM_SIZE - 1 - x;
				y = ROOM_SIZE - 1 - y;
				break;
			case TOP: {
				int oldX = x;
				x = ROOM_SIZE - 1 - y;
				y = oldX;
				break;
			}
			case BOTTOM: {
				int oldX = x;
				x = y;
				y = ROOM_SIZE - 1 - oldX;
				break;
			}
		}
		return TEMPLATE[y].charAt(x) != '.';
	}

	public static class HallsFloor extends CustomTilemap {

		private int direction = RIGHT;

		{
			texture = Assets.Environment.TILES_HALLS;
		}

		@Override
		public Tilemap create() {
			Tilemap visual = super.create();
			int[] data = new int[tileW * tileH];
			for (int y = 0; y < tileH; y++) {
				for (int x = 0; x < tileW; x++) {
					int templateX = x;
					int templateY = y;
					switch (direction) {
						case LEFT:
							templateX = ROOM_SIZE - 1 - x;
							templateY = ROOM_SIZE - 1 - y;
							break;
						case TOP:
							templateX = ROOM_SIZE - 1 - y;
							templateY = x;
							break;
						case BOTTOM:
							templateX = y;
							templateY = ROOM_SIZE - 1 - x;
							break;
					}

					char tile = TEMPLATE[templateY].charAt(templateX);
					int index = x + y * tileW;
					int cell = tileX + x + (tileY + y) * Dungeon.level.width();
					if (tile == '.' || templateX == ROOM_SIZE - 1 && templateY == CENTER
							|| (x == 0 || y == 0 || x == ROOM_SIZE - 1 || y == ROOM_SIZE - 1)
								&& Dungeon.level.map[cell] != Terrain.EMPTY) {
						data[index] = DungeonTileSheet.NULL_TILE;
					} else if (tile == 'D' || tile == 'S') {
						data[index] = DungeonTileSheet.getVisualWithAlts(DungeonTileSheet.FLOOR_DECO, cell);
					} else if (tile == 'P') {
						data[index] = DungeonTileSheet.PEDESTAL;
					} else {
						data[index] = DungeonTileSheet.getVisualWithAlts(DungeonTileSheet.FLOOR, cell);
					}
				}
			}
			visual.map(data, tileW);
			return visual;
		}

		private static final String DIRECTION = "direction";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(DIRECTION, direction);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			direction = bundle.getInt(DIRECTION);
		}
	}
}
