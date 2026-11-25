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
import com.shatteredpixel.shatteredpixeldungeon.levels.CellContent;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.Door;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.connection.ConnectionRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.SecretRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.SuperSecretRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.BlacksmithRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet;
import com.watabou.utils.Random;
import com.watabou.utils.Point;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

public class CavesPainter extends RegularPainter {

	@Override
	protected boolean mergeRooms(Level level, Room room, Room neighbour, Point start, CellPlacement terrain) {
		// 支路数量由生成器确定，不能通过后续拆墙将相邻支路或旁室连成捷径。
		return false;
	}
	
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
			paintMetalConnections(level, rooms);
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

	private void paintMetalConnections(Level level, ArrayList<Room> rooms) {
		int width = level.width();
		for (Room room : rooms) {
			if (room instanceof SuperSecretRoom) continue;
			if (room instanceof ConnectionRoom) {
				// 只替换现有通路，保留桥下深渊和相邻房间的墙体。
				for (int y = room.top + 1; y < room.bottom; y++) {
					for (int x = room.left + 1; x < room.right; x++) {
						int cell = x + y * width;
						int terrain = level.terrainAt(cell);
						if (terrain == Terrain.EMPTY || terrain == Terrain.EMPTY_SP || terrain == Terrain.EMPTY_DECO) {
							level.cellAt(cell).place(Terrain.REGION_DECO);
						}
					}
				}
			}
			for (Room neighbour : room.connected.keySet()) {
				if (neighbour instanceof SuperSecretRoom) continue;
				Room.Door door = room.connected.get(neighbour);
				if (door == null) continue;
				int doorway = door.x + door.y * width;
				if (door.type == Room.Door.Type.CRYSTAL || Door.is(level, doorway, Door.State.CRYSTAL)) continue;
				if (room instanceof SecretRoom || neighbour instanceof SecretRoom) {
					// 普通隐藏房入口外观仍是墙，没有 SECRET 属性，不会被搜索揭开。
					level.cellAt(doorway).reset(Terrain.EMPTY, Terrain.WALL);
					level.interactions().propertiesAt(doorway, Terrain.WALL,
							CellContent.BREAKABLE | CellContent.CLICK_BREAKABLE, 0);
					door.type = Room.Door.Type.WALL;
					continue;
				}
				// 矿洞入口改为架构，包括商店和锁门；保留水晶门与解谜用木路障。
				switch (door.type) {
					case EMPTY: case TUNNEL: case REGULAR: case UNLOCKED:
					case HIDDEN: case LOCKED: case WATER:
						level.cellAt(doorway).reset(Terrain.EMPTY, Terrain.REGION_DECO);
						door.type = Room.Door.Type.EMPTY;
						// 在门口两侧的岩墙内侧补一层墙，增加墙体厚度，保留中间通路。
						int dx = door.x == room.left ? 1 : door.x == room.right ? -1 : 0;
						int dy = door.y == room.top ? 1 : door.y == room.bottom ? -1 : 0;
						boolean thickened = false;
						for (int side : new int[]{-1, 1}) {
							int wallX = door.x - dy * side;
							int wallY = door.y + dx * side;
							int wall = wallX + wallY * width;
							if (!level.hasTerrain(wall, Terrain.WALL) && !level.hasTerrain(wall, Terrain.WALL_DECO)) continue;
							Point inner = new Point(wallX + dx, wallY + dy);
							if (!room.inside(inner)) continue;
							int cell = level.pointToCell(inner);
							if (level.hasTerrain(cell, Terrain.WALL) || level.hasTerrain(cell, Terrain.WALL_DECO)) {
								thickened = true;
									continue;
							}
							if (level.getTransition(cell) != null || level.heaps.get(cell) != null
									|| level.findMob(cell) != null || level.traps.get(cell) != null
									|| level.plants.get(cell) != null) continue;
							if ((level.cellAt(cell).flags() & (Terrain.SOLID | Terrain.PIT)) != 0
									|| level.hasTerrain(cell, Terrain.REGION_DECO)
									|| level.hasTerrain(cell, Terrain.REGION_DECO_ALT)) continue;
							// 洞口内侧缺少可走的纵深时，不能用装饰岩壁堵住横向通路。
							Point behind = new Point(inner.x + dx, inner.y + dy);
							if (!room.inside(behind) || (level.cellAt(level.pointToCell(behind)).flags() & Terrain.PASSABLE) == 0) continue;
							level.cellAt(cell).place(Terrain.WALL);
							thickened = true;
						}
						// 金属架构铺过加厚后的洞口，中间不补墙，深渊仍统一补地板。
						Point opening = new Point(door.x + dx, door.y + dy);
						if (thickened && room.inside(opening)) {
							int cell = level.pointToCell(opening);
							if (level.getTransition(cell) == null && (level.cellAt(cell).flags() & Terrain.SOLID) == 0) {
								level.cellAt(cell).place(Terrain.REGION_DECO);
							}
						}
						if ((room instanceof StandardRoom || room instanceof SpecialRoom) && !(room instanceof BlacksmithRoom)) {
							paintMetalExtension(level, room, door);
						}
						break;
					default:
						break;
				}
			}
		}
		for (int cell = 0; cell < level.length(); cell++) {
			Door door = Door.at(level, cell);
			if (door != null && door.state() != Door.State.CRYSTAL) {
				level.cellAt(cell).reset(Terrain.EMPTY, Terrain.REGION_DECO);
			}
		}
		for (int cell = 0; cell < level.length(); cell++) {
			if (Door.is(level, cell, Door.State.CRYSTAL)) {
				// 所有架构绘制结束后清理门口，防止其他入口的延伸贴到水晶门旁。
				for (int offset : PathFinder.NEIGHBOURS9) {
					int next = cell + offset;
					if (level.insideMap(next) && level.distance(cell, next) <= 1) {
						level.cellAt(next).removeTerrain(Terrain.REGION_DECO);
						level.cellAt(next).removeTerrain(Terrain.REGION_DECO_ALT);
					}
				}
			}
		}
		// 架构不能悬在深渊上；只补基础地板，保留同格的其他内容。
		for (int cell = 0; cell < level.length(); cell++) {
			if ((level.hasTerrain(cell, Terrain.REGION_DECO) || level.hasTerrain(cell, Terrain.REGION_DECO_ALT))
					&& level.cellAt(cell).ground() == Terrain.CHASM) {
				level.cellAt(cell).setGround(Terrain.EMPTY);
			}
		}
	}

	private void paintMetalExtension(Level level, Room room, Room.Door door) {
		int dx = door.x == room.left ? 1 : door.x == room.right ? -1 : 0;
		int dy = door.y == room.top ? 1 : door.y == room.bottom ? -1 : 0;
		int extension = Random.IntRange(0, 3);
		if (extension == 0) return;
		int turnAt = extension == 1 ? 2 : Random.IntRange(2, extension);
		int turn = Random.Int(2) == 0 ? -1 : 1;
		Point point = new Point(door);
		for (int step = 1; step <= extension; step++) {
			int nextX = dx, nextY = dy;
			if (step == turnAt) {
				nextX = -dy * turn;
				nextY = dx * turn;
			}
			// 转弯朝向不可用时试另一侧，最后才沿原方向延伸。
			boolean placed = false;
			for (int attempt = 0; attempt < (step == turnAt ? 3 : 1); attempt++) {
				int moveX = attempt == 0 ? nextX : attempt == 1 ? -nextX : dx;
				int moveY = attempt == 0 ? nextY : attempt == 1 ? -nextY : dy;
				Point next = new Point(point.x + moveX, point.y + moveY);
				if (!room.inside(next)) continue;
				int cell = level.pointToCell(next);
				if (level.getTransition(cell) != null || level.heaps.get(cell) != null
						|| level.findMob(cell) != null || level.traps.get(cell) != null
						|| level.plants.get(cell) != null) continue;
				if ((level.cellAt(cell).flags() & Terrain.SOLID) != 0) continue;
				// 保留原地面、水、草及其他覆盖物；深渊由连接绘制结束后统一补地板。
				level.cellAt(cell).place(Terrain.REGION_DECO);
				point = next;
				dx = moveX;
				dy = moveY;
				placed = true;
				break;
			}
			if (!placed) break;
		}
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
