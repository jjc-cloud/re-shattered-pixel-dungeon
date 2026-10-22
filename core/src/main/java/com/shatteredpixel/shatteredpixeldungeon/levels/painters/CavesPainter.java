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

package com.shatteredpixel.shatteredpixeldungeon.levels.painters;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.CaveCollapse;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.BlacksmithRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class CavesPainter extends RegularPainter {
	
	@Override
	protected void decorate(Level level, ArrayList<Room> rooms) {
		
		int w = level.width();
		int l = level.length();
		int[] map = level.terrainVisuals();

		for (Room r : rooms) {
			for (Room n : r.neigbours) {
				if (!r.connected.containsKey( n )) {
					mergeRooms(level, r, n, null, Random.Int(3) == 0 ? CellPlacement.reset(Terrain.EMPTY, Terrain.REGION_DECO) : CellPlacement.reset(Terrain.CHASM));
				}
			}
		}

		for (Room room : rooms) {
			if (!(room instanceof StandardRoom) || room instanceof BlacksmithRoom) {
				continue;
			}
			
			if (room.width() <= 4 || room.height() <= 4) {
				continue;
			}
			
			int s = room.square();

			//for each corner, we have a chance to fill based on room size
			//but not if filling that corner replaces solid terrain, blocks a connection, or places a visible trap next to a wall
			if (Random.Int( s ) > 8) {
				int corner = (room.left + 1) + (room.top + 1) * w;
				if ((level.cellAt(corner).flags() & Terrain.SOLID) == 0
						&& level.hasTerrain(corner-1, Terrain.WALL) && !room.connected.containsValue(level.cellToPoint(corner-1))
						&& level.hasTerrain(corner-w, Terrain.WALL) && !room.connected.containsValue(level.cellToPoint(corner-w))
						&& !level.hasTerrain(corner+1, Terrain.TRAP) && !level.hasTerrain(corner+w, Terrain.TRAP)) {
					level.cellAt(corner).place(Terrain.WALL);
					level.traps.remove(corner);
				}
			}
			
			if (Random.Int( s ) > 8) {
				int corner = (room.right - 1) + (room.top + 1) * w;
				if ((level.cellAt(corner).flags() & Terrain.SOLID) == 0
						&& level.hasTerrain(corner+1, Terrain.WALL) && !room.connected.containsValue(level.cellToPoint(corner+1))
						&& level.hasTerrain(corner-w, Terrain.WALL) && !room.connected.containsValue(level.cellToPoint(corner-w))
						&& !level.hasTerrain(corner-1, Terrain.TRAP) && !level.hasTerrain(corner+w, Terrain.TRAP)) {
					level.cellAt(corner).place(Terrain.WALL);
					level.traps.remove(corner);
				}
			}
			
			if (Random.Int( s ) > 8) {
				int corner = (room.left + 1) + (room.bottom - 1) * w;
				if ((level.cellAt(corner).flags() & Terrain.SOLID) == 0
						&& level.hasTerrain(corner-1, Terrain.WALL) && !room.connected.containsValue(level.cellToPoint(corner-1))
						&& level.hasTerrain(corner+w, Terrain.WALL) && !room.connected.containsValue(level.cellToPoint(corner+w))
						&& !level.hasTerrain(corner+1, Terrain.TRAP) && !level.hasTerrain(corner-w, Terrain.TRAP)) {
					level.cellAt(corner).place(Terrain.WALL);
					level.traps.remove(corner);
				}
			}
			
			if (Random.Int( s ) > 8) {
				int corner = (room.right - 1) + (room.bottom - 1) * w;
				if ((level.cellAt(corner).flags() & Terrain.SOLID) == 0
						&& level.hasTerrain(corner+1, Terrain.WALL) && !room.connected.containsValue(level.cellToPoint(corner+1))
						&& level.hasTerrain(corner+w, Terrain.WALL) && !room.connected.containsValue(level.cellToPoint(corner+w))
						&& !level.hasTerrain(corner-1, Terrain.TRAP) && !level.hasTerrain(corner-w, Terrain.TRAP)) {
					level.cellAt(corner).place(Terrain.WALL);
					level.traps.remove(corner);
				}
			}

		}

		if (Dungeon.branch == 0 && Dungeon.depth >= 11 && Dungeon.depth <= 14) {
			level.caveCollapse = new CaveCollapse(level);
			level.caveCollapse.generateWalls(rooms);
		}

		for (int i=w + 1; i < l - w; i++) {
			if (level.terrainAt(i) == Terrain.EMPTY) {
				int n = 0;
				if (level.hasTerrain(i+1, Terrain.WALL) || level.hasTerrain(i+1, Terrain.COLLAPSE_WALL)) {
					n++;
				}
				if (level.hasTerrain(i-1, Terrain.WALL) || level.hasTerrain(i-1, Terrain.COLLAPSE_WALL)) {
					n++;
				}
				if (level.hasTerrain(i+w, Terrain.WALL) || level.hasTerrain(i+w, Terrain.COLLAPSE_WALL)) {
					n++;
				}
				if (level.hasTerrain(i-w, Terrain.WALL) || level.hasTerrain(i-w, Terrain.COLLAPSE_WALL)) {
					n++;
				}
				if (Random.Int( 6 ) <= n) {
					level.cellAt(i).setGround(Terrain.EMPTY_DECO);
				}
			}
		}
		
		generateGold(level, rooms);

	}

	protected void generateGold(Level level, ArrayList<Room> rooms){
		int w = level.width();
		int l = level.length();
		int[] map = level.terrainVisuals();

		for (int i=0; i < l - w; i++) {
			if ((level.hasTerrain(i, Terrain.WALL) || level.hasTerrain(i, Terrain.COLLAPSE_WALL)) &&
					DungeonTileSheet.floorTile(level.terrainAt(i + w))
					&& Random.Int( 4 ) == 0) {
				if (level.hasTerrain(i, Terrain.COLLAPSE_WALL)) level.caveCollapse.goldVeins.add(i);
				else level.cellAt(i).place(Terrain.WALL_DECO);
			}
		}
	}
}
