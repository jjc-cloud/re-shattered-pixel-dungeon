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
import com.shatteredpixel.shatteredpixeldungeon.levels.Patch;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.connection.ConnectionRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.connection.PrisonHallRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.connection.TunnelRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.ChasmBridgeRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.FissureRoom;
import com.watabou.utils.PathFinder;
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
		boolean[] hallEntrances = new boolean[l];
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
					if (door != null) {
						int doorCell = level.pointToCell(door);
						level.cellAt(doorCell).setGround(Terrain.EMPTY_SP);
						int inward = door.x == r.left ? 1 : door.x == r.right ? -1 : door.y == r.top ? w : -w;
						hallEntrances[doorCell] = hallEntrances[doorCell + inward] = true;
					}
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
		if (level.feeling == Level.Feeling.CHASM) {
			// 走廊四面围住的一格深渊改为有地面的支柱。
			for (Room r : rooms) if (r instanceof ConnectionRoom) {
				for (int y = r.top; y <= r.bottom; y++) {
					for (int x = r.left; x <= r.right; x++) {
						int cell = x + y * w;
						if (map[cell] == Terrain.CHASM && isOpenFloor(map[cell - 1]) && isOpenFloor(map[cell + 1])
								&& isOpenFloor(map[cell - w]) && isOpenFloor(map[cell + w])) {
							level.cellAt(cell).reset(Terrain.EMPTY_SP, Terrain.WALL);
						}
					}
				}
			}
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
			if (level.feeling != Level.Feeling.CHASM) {
				paintHallChasms(level, hallFloor, hallEntrances);
			}
		}
		if (level.feeling != Level.Feeling.CHASM) {
			paintTunnelChasm(level, rooms);
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

	private static void paintHallChasms(Level level, boolean[] hallFloor, boolean[] hallEntrances) {
		int[] map = level.terrainVisuals();
		boolean[] passable = new boolean[map.length];
		ArrayList<Integer> floorCells = new ArrayList<>();
		int anchor = -1;
		for (int cell = 0; cell < map.length; cell++) {
			if (hallFloor[cell] && isOpenFloor(map[cell])) {
				passable[cell] = true;
				floorCells.add(cell);
				if (anchor == -1 && hallEntrances[cell]) anchor = cell;
			}
		}
		if (floorCells.isEmpty()) return;
		if (anchor == -1) anchor = floorCells.get(0);

		PathFinder.buildDistanceMap(anchor, passable);
		for (int cell : floorCells) {
			if (PathFinder.distance[cell] == Integer.MAX_VALUE) passable[cell] = false;
		}

		boolean[] patch = Patch.generate(level.width(), level.height(), 0.15f, 1, true);
		Random.shuffle(floorCells);
		for (int cell : floorCells) {
			if (!patch[cell] || !passable[cell] || hallEntrances[cell] || cell == anchor) continue;

			passable[cell] = false;
			PathFinder.buildDistanceMap(anchor, passable);
			boolean connected = true;
			for (int other : floorCells) {
				if (passable[other] && PathFinder.distance[other] == Integer.MAX_VALUE) {
					connected = false;
					break;
				}
			}
			if (connected) level.cellAt(cell).setGround(Terrain.CHASM);
			else passable[cell] = true;
		}
	}

	private static void paintTunnelChasm(Level level, ArrayList<Room> rooms) {
		int w = level.width();
		int[] map = level.terrainVisuals();
		boolean[] tunnelFloor = new boolean[map.length];
		boolean[] reserved = new boolean[map.length];
		for (Room room : rooms) {
			boolean tunnel = room instanceof TunnelRoom;
			for (int y = room.top; y <= room.bottom; y++) {
				for (int x = room.left; x <= room.right; x++) {
					int cell = x + y * w;
					if (!tunnel) reserved[cell] = true;
					else if (isOpenFloor(map[cell])) tunnelFloor[cell] = true;
				}
			}
		}

		boolean[] rock = new boolean[map.length];
		ArrayList<Integer> corners = new ArrayList<>();
		ArrayList<Integer> edges = new ArrayList<>();
		for (int cell = w + 1; cell < map.length - w - 1; cell++) {
			if (!level.insideMap(cell) || reserved[cell] || !isWall(map[cell])) continue;
			rock[cell] = true;
			boolean horizontal = tunnelFloor[cell - 1] || tunnelFloor[cell + 1];
			boolean vertical = tunnelFloor[cell - w] || tunnelFloor[cell + w];
			if (horizontal && vertical) corners.add(cell);
			else if (horizontal || vertical) edges.add(cell);
		}

		Random.shuffle(corners);
		Random.shuffle(edges);
		corners.addAll(edges);
		int[] visited = new int[map.length];
		for (int seed : corners) {
			int target = Random.IntRange(10, 22);
			int visit = seed + 1;
			ArrayList<Integer> cells = new ArrayList<>();
			ArrayList<Integer> frontier = new ArrayList<>();
			frontier.add(seed);
			visited[seed] = visit;
			while (!frontier.isEmpty() && cells.size() < target) {
				int cell = frontier.remove(Random.Int(frontier.size()));
				cells.add(cell);
				for (int offset : PathFinder.NEIGHBOURS4) {
					int next = cell + offset;
					if (rock[next] && visited[next] != visit && level.distance(seed, next) <= 4) {
						visited[next] = visit;
						frontier.add(next);
					}
				}
			}
			if (cells.size() < 6) continue;
			for (int cell : cells) level.cellAt(cell).reset(Terrain.CHASM);
			return;
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
