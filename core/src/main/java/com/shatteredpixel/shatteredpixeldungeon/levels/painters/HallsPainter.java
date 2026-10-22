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

import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class HallsPainter extends RegularPainter {
	
	@Override
	protected void decorate(Level level, ArrayList<Room> rooms) {
		
		int[] map = level.terrainVisuals();
		int w = level.width();
		int l = level.length();
		
		for (int i=w + 1; i < l - w - 1; i++) {
			if (level.terrainAt(i) == Terrain.EMPTY) {
				
				int count = 0;
				for (int j = 0; j < PathFinder.NEIGHBOURS8.length; j++) {
					if ((level.cellAt(i + PathFinder.NEIGHBOURS8[j]).flags() & Terrain.PASSABLE) > 0) {
						count++;
					}
				}
				
				if (Random.Int( 80 ) < count) {
					level.cellAt(i).setGround(Terrain.EMPTY_DECO);
				}
				
			} else
			if (level.hasTerrain(i, Terrain.WALL) &&
					!level.hasTerrain(i-1, Terrain.WALL_DECO) && !level.hasTerrain(i-w, Terrain.WALL_DECO) &&
					Random.Int( 20 ) == 0) {
				
				level.cellAt(i).place(Terrain.WALL_DECO);
				
			} else if (level.hasTerrain(i, Terrain.REGION_DECO) && Random.Int(2) == 0){
				level.cellAt(i).place(Terrain.REGION_DECO_ALT);
			}
		}

		for (Room r : rooms) {
			for (Room n : r.neigbours) {
				if (!r.connected.containsKey( n )) {
					mergeRooms(level, r, n, null, Random.Int(3) == 0 ? CellPlacement.reset(Terrain.EMPTY, Terrain.REGION_DECO) : CellPlacement.reset(Terrain.CHASM));
				}
			}
		}

	}
}
