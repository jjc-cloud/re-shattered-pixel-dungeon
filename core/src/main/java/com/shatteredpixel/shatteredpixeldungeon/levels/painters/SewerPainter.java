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
import com.watabou.utils.Random;

import java.util.ArrayList;

public class SewerPainter extends RegularPainter {
	
	@Override
	protected void decorate(Level level, ArrayList<Room> rooms) {
		
		int[] map = level.terrainVisuals();
		int w = level.width();
		int l = level.length();
		
		for (int i=0; i < w; i++) {
			if (level.hasTerrain(i, Terrain.WALL) &&
					level.hasTerrain(i + w, Terrain.WATER) &&
					Random.Int( 4 ) == 0) {
				
				level.cellAt(i).place(Terrain.WALL_DECO);
			}
		}
		
		for (int i=w; i < l - w; i++) {
			if (level.hasTerrain(i, Terrain.WALL) &&
					level.hasTerrain(i - w, Terrain.WALL) &&
					level.hasTerrain(i + w, Terrain.WATER) &&
					Random.Int( 2 ) == 0) {
				
				level.cellAt(i).place(Terrain.WALL_DECO);
			}
		}
		
		for (int i=w + 1; i < l - w - 1; i++) {
			if (level.terrainAt(i) == Terrain.EMPTY) {
				
				int count =
						(level.hasTerrain(i + 1, Terrain.WALL) ? 1 : 0) +
								(level.hasTerrain(i - 1, Terrain.WALL) ? 1 : 0) +
								(level.hasTerrain(i + w, Terrain.WALL) ? 1 : 0) +
								(level.hasTerrain(i - w, Terrain.WALL) ? 1 : 0);
				
				if (Random.Int( 16 ) < count * count) {
					level.cellAt(i).setGround(Terrain.EMPTY_DECO);
				}
			}
		}
	}
}
