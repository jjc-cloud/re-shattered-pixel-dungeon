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
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.ChasmBridgeRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.FissureRoom;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class PrisonPainter extends RegularPainter {
	
	@Override
	protected void decorate(Level level, ArrayList<Room> rooms) {
		
		int w = level.width();
		int l = level.length();
		int[] map = level.terrainVisuals();
		
		for (int i=w + 1; i < l - w - 1; i++) {
			if (level.terrainAt(i) == Terrain.EMPTY) {
				
				float c = 0.05f;
				if (level.hasTerrain(i + 1, Terrain.WALL) && level.hasTerrain(i + w, Terrain.WALL)) {
					c += 0.2f;
				}
				if (level.hasTerrain(i - 1, Terrain.WALL) && level.hasTerrain(i + w, Terrain.WALL)) {
					c += 0.2f;
				}
				if (level.hasTerrain(i + 1, Terrain.WALL) && level.hasTerrain(i - w, Terrain.WALL)) {
					c += 0.2f;
				}
				if (level.hasTerrain(i - 1, Terrain.WALL) && level.hasTerrain(i - w, Terrain.WALL)) {
					c += 0.2f;
				}
				
				if (Random.Float() < c) {
					level.cellAt(i).setGround(Terrain.EMPTY_DECO);
				}
			}
		}

		for (Room r : rooms){
			if (r instanceof SpecialRoom){
				continue;
			}
			int chance = 15; //1/15 by default, some rooms can be more common though
			if (r instanceof FissureRoom){
				chance = 3;
			} else if (r instanceof ChasmBridgeRoom){
				chance = 5;
			}

			int cell;
			for (int y = r.bottom-1; y > r.top; y--){
				cell = r.left+1 + level.width()*y;
				for (int x = r.left+1; x < r.right; x++){
					if (level.terrainAt(cell) == Terrain.CHASM && level.terrainAt(cell-level.width()) == Terrain.CHASM){
						if (Random.Int(chance) == 0){
							level.cellAt(cell).reset(Terrain.EMPTY, Terrain.REGION_DECO_ALT);
						}
					}
					cell++;
				}
			}
		}
		
		int[] undecoratedMap = map.clone();
		for (int i=0; i < w; i++) {
			if (level.hasTerrain(i, Terrain.WALL) &&
					isOpenFloor(undecoratedMap[i + w]) &&
					!isAdjacentToDoor(undecoratedMap, i, w) &&
					Random.Int( 6 ) == 0) {
				
				level.cellAt(i).place(Terrain.WALL_DECO);
			}
		}
		
		for (int y=1; y < level.height()-1; y++) {
			for (int x=1; x < w-1; x++) {
				int cell = x + y*w;
				if (isTorchWallCandidate(undecoratedMap, cell, w) && Random.Int( 3 ) == 0) {
					level.cellAt(cell).place(Terrain.WALL_DECO);
				}
			}
		}
	}

	protected static boolean isTorchWallCandidate( int[] map, int cell, int width ) {
		if (map[cell] != Terrain.WALL || isAdjacentToDoor(map, cell, width)) return false;
		return isWall(map[cell - width]) && isOpenFloor(map[cell + width])
				|| isWall(map[cell + width]) && isOpenFloor(map[cell - width])
				|| isWall(map[cell - 1]) && isOpenFloor(map[cell + 1])
				|| isWall(map[cell + 1]) && isOpenFloor(map[cell - 1]);
	}

	private static boolean isWall( int terrain ) {
		return terrain == Terrain.WALL || terrain == Terrain.WALL_DECO;
	}

	private static boolean isOpenFloor( int terrain ) {
		return terrain == Terrain.EMPTY || terrain == Terrain.EMPTY_SP;
	}

	private static boolean isAdjacentToDoor( int[] map, int cell, int width ) {
		return cell >= width && isDoor(map[cell - width])
				|| cell + width < map.length && isDoor(map[cell + width])
				|| cell % width > 0 && isDoor(map[cell - 1])
				|| cell % width < width - 1 && isDoor(map[cell + 1]);
	}

	private static boolean isDoor( int terrain ) {
		return com.shatteredpixel.shatteredpixeldungeon.levels.features.Door.state(terrain) != null;
	}
}
