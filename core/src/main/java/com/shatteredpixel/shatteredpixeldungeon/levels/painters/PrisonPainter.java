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
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.connection.PrisonHallRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.ChasmBridgeRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.FissureRoom;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;

public class PrisonPainter extends RegularPainter {
	
	@Override
	protected void decorate(Level level, ArrayList<Room> rooms) {
		
		int w = level.width();
		int l = level.length();
		int[] map = level.terrainVisuals();
		if (level.feeling == Level.Feeling.CHASM) {
			// 悬空连接不留孤立门，房间墙体支撑的门继续保留。
			for (Room r : rooms) for (Room.Door door : r.connected.values()) {
				if (door == null) continue;
				int cell = door.x + door.y * w;
				if (isDoor(map[cell]) && !isWall(map[cell - 1]) && !isWall(map[cell + 1])
						&& !isWall(map[cell - w]) && !isWall(map[cell + w])) {
					level.cellAt(cell).removeTerrain(map[cell]);
					door.type = Room.Door.Type.EMPTY;
				}
			}
		}

		// 按实际廊段重建中心线，装饰间距沿弯曲主廊计算，不按直线坐标取模。
		boolean[] hallFloor = new boolean[l];
		boolean[] centerLine = new boolean[l];
		int origin = -1;
		for (Room r : rooms) if (r instanceof PrisonHallRoom) {
			int cx = (r.left + r.right) / 2, cy = (r.top + r.bottom) / 2;
			centerLine[cx + cy * w] = true;
			for (int y = r.top + 1; y < r.bottom; y++) {
				for (int x = r.left + 1; x < r.right; x++) hallFloor[x + y * w] = true;
			}
			int arms = 0, endOffset = 0;
			for (Room n : r.connected.keySet()) {
				if (!(n instanceof PrisonHallRoom)) {
					// 门只覆盖内容，脚下仍是主廊特殊地砖。
					Room.Door door = r.connected.get(n);
					if (door != null) level.cellAt(door.x + door.y * w).setGround(Terrain.EMPTY_SP);
					continue;
				}
				arms++;
				int nx = (n.left + n.right) / 2, ny = (n.top + n.bottom) / 2;
				endOffset = Integer.signum(cx - nx) + Integer.signum(cy - ny) * w;
				for (int y = Math.min(cy, ny); y <= Math.max(cy, ny); y++) {
					for (int x = Math.min(cx, nx); x <= Math.max(cx, nx); x++) {
						centerLine[x + y * w] = true;
						for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
							hallFloor[x + dx + (y + dy) * w] = true;
						}
					}
				}
			}
			if (arms == 1) {
				int end = cx + cy * w + endOffset;
				centerLine[end] = true;
				// 尽头中间一格也铺特殊地砖，与两侧边带连成完整收边。
				level.cellAt(end).setGround(Terrain.EMPTY_SP);
			}
			if (((PrisonHallRoom) r).arm == -1) origin = cx + cy * w;
		}
		// 按完整廊道铺边带，包含转弯外侧凸角，也不会因开门缺少邻墙而断砖。
		for (int cell = w + 1; cell < l - w - 1; cell++) if (hallFloor[cell] && !centerLine[cell]) {
			level.cellAt(cell).setGround(Terrain.EMPTY_SP);
		}
		if (origin != -1) {
			int[] distance = new int[l];
			Arrays.fill(distance, -1);
			int[] queue = new int[l];
			int[] neighbours = {-w, 1, w, -1};
			int head = 0, tail = 0;
			queue[tail++] = origin;
			distance[origin] = 0;
			while (head < tail) {
				int cell = queue[head++];
				for (int offset : neighbours) {
					int next = cell + offset;
					if (centerLine[next] && distance[next] == -1) {
						distance[next] = distance[cell] + 1;
						queue[tail++] = next;
					}
				}
				if (distance[cell] % 5 != 0) continue;
				// 铁笼沿主廊中线摆放，底座为特殊地板，两侧仍可绕行。
				level.cellAt(cell).setGround(Terrain.EMPTY_SP);
				level.cellAt(cell).place(Terrain.REGION_DECO);
			}
		}
		
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
							//悬吊牢笼挂在深渊上，不能把底下铺成地板
							level.cellAt(cell).reset(Terrain.CHASM, Terrain.REGION_DECO_ALT);
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
					!isAdjacentToTorch(level, i, w) &&
					Random.Int( 6 ) == 0) {
				
				level.cellAt(i).place(Terrain.WALL_DECO);
			}
		}
		
		for (int y=1; y < level.height()-1; y++) {
			for (int x=1; x < w-1; x++) {
				int cell = x + y*w;
				if (isTorchWallCandidate(undecoratedMap, cell, w) &&
						!isAdjacentToTorch(level, cell, w) && Random.Int( 6 ) == 0) {
					level.cellAt(cell).place(Terrain.WALL_DECO);
				}
			}
		}
	}

	private static boolean isAdjacentToTorch( Level level, int cell, int width ) {
		return cell >= width && level.hasTerrain(cell - width, Terrain.WALL_DECO)
				|| cell + width < level.length() && level.hasTerrain(cell + width, Terrain.WALL_DECO)
				|| cell % width > 0 && level.hasTerrain(cell - 1, Terrain.WALL_DECO)
				|| cell % width < width - 1 && level.hasTerrain(cell + 1, Terrain.WALL_DECO);
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
