/*
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.shatteredpixel.shatteredpixeldungeon.levels.builders;

import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.connection.PrisonHallRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.connection.TunnelRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.SecretRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.SuperSecretRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.ShopRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.watabou.utils.Random;
import com.watabou.utils.Rect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;

/** 先划区、撒房和规划互联，再将跨区连接的端点移到主廊墙边。 */
public class PrisonBuilder extends Builder {

	// 仅控制保证连通之后的额外补门；概率为 0 时关闭对应补门。
	public static final float EXTRA_HALL_CONNECTION_CHANCE = 0.18f;
	public static final float EXTRA_ROOM_CONNECTION_CHANCE = 0.25f;
	// 额外门与已有门候选墙段之间至少隔四格，避免连续挨着开门。
	public static final int EXTRA_DOOR_MIN_DISTANCE = 4;
	public static final int MAX_EXTRA_HALL_CONNECTIONS_PER_ROOM = 1;
	public static final float ROOM_AREA_SCALE = 1.20f;
	public static final int MAX_HALL_STRAIGHT_SEGMENTS = 3;
	public static final int MAX_TUNNEL_STRAIGHT_STEPS = 3;
	// 大多数普通房间直接贴主廊；其余留在区域内部由普通走廊接入。
	public static final float HALL_ADJACENT_RATIO = 0.75f;
	// 已贴主廊的房间之间仍保留少量短走廊，避免整层只剩主廊一条通道。
	public static final int MAX_ATTACHED_LINK_DISTANCE = 20;
	public static final float ATTACHED_LINK_CHANCE = 0.5f;

	private static final int[] DX = {0, 1, 0, -1};
	private static final int[] DY = {-1, 0, 1, 0};
	private int size;
	private int attempts;
	// 每层只抽取一次字形和朝向，布局重试不能改变四种字形各 1/4 的概率。
	private final int hallShape = Random.Int(4);
	private final int hallRotation = Random.Int(4);
	private int regionCount;
	private int[] regions;
	private final ArrayList<Room> halls = new ArrayList<>();
	private final HashMap<Room, Integer> region = new HashMap<>();
	private final ArrayList<Room[]> links = new ArrayList<>();
	private final HashSet<Room> attached = new HashSet<>();
	private final HashSet<Room> anchors = new HashSet<>();
	private final HashMap<StandardRoom, StandardRoom.SizeCategory> baseSizes = new HashMap<>();
	private int[] regionLoad;
	private int[] regionAreas;
	private int smallestRegion;

	@Override
	public ArrayList<Room> build(ArrayList<Room> rooms) {
		halls.clear();
		region.clear();
		links.clear();
		attached.clear();
		anchors.clear();
		Room entrance = null, exit = null;
		ArrayList<Room> core = new ArrayList<>();
		ArrayList<Room> leaves = new ArrayList<>();
		int area = 0;
		for (Room r : rooms) {
			r.clearConnections();
			if (r instanceof StandardRoom) {
				StandardRoom s = (StandardRoom) r;
				baseSizes.putIfAbsent(s, s.sizeCat);
				s.sizeCat = baseSizes.get(s);
			}
			// 在房型允许的尺寸内偏向紧凑，保留大房间与任务房的最小尺寸。
			int allowance = r instanceof SuperSecretRoom ? 2 : 1;
			if (!r.setSizeWithLimit(r.minWidth() + allowance, r.minHeight() + allowance)) return null;
			if (!(r instanceof SuperSecretRoom)) {
				// 奇数边长让紧凑房间能共用偶数坐标的墙，减少无用的一格缝隙。
				r.forceSize(Math.min(r.maxWidth(), r.minWidth() | 1),
						Math.min(r.maxHeight(), r.minHeight() | 1));
				int baseWidth = r.width(), baseHeight = r.height();
				float targetArea = (baseWidth - 2) * (baseHeight - 2) * ROOM_AREA_SCALE;
				float best = Float.MAX_VALUE;
				int width = baseWidth, height = baseHeight;
				// 按可用面积放大约 20%，固定尺寸房间仍遵守房型上限。
				for (int w = baseWidth; w <= r.maxWidth(); w++) for (int h = baseHeight; h <= r.maxHeight(); h++) {
					float score = Math.abs((w - 2) * (h - 2) - targetArea)
							+ Math.abs(w * baseHeight - h * baseWidth) * 0.01f;
					if (score < best || score == best && Random.Int(2) == 0) {
						best = score;
						width = w;
						height = h;
					}
				}
				r.forceSize(width, height);
			}
			area += r.width() * r.height();
			if (r.isEntrance()) entrance = r;
			if (r.isExit()) exit = r;
			if (!(r instanceof SecretRoom) && r.maxConnections(Room.ALL) > 1) core.add(r);
			else leaves.add(r);
		}
		if (entrance == null || exit == null || core.size() < 3) return null;
		// 按房间面积紧凑分配，避免四条主廊把小型楼层也撑到五十多格。
		int cells = Math.max(8, (int) Math.ceil(Math.sqrt(area * 1.4f + 100) / 4));
		// 放不下时逐级补容量；不能在固定上限内无限重试同一组必需房间。
		cells += attempts++ / 32;
		size = cells * 4 + 1;
		if (!buildHalls(cells) || !markRegions()) return null;
		int[] areas = new int[regionCount + 1];
		int largest = 1, totalArea = 0;
		for (int id : regions) if (id > 0) { areas[id]++; totalArea++; }
		for (int id = 2; id <= regionCount; id++) if (areas[id] > areas[largest]) largest = id;
		regionAreas = areas;
		smallestRegion = 1;
		for (int id = 2; id <= regionCount; id++) if (areas[id] < areas[smallestRegion]) smallestRegion = id;
		regionLoad = new int[regionCount + 1];
		Room expanded = null;
		boolean hasLarge = false;
		for (Room r : core) if (r instanceof StandardRoom
				&& ((StandardRoom) r).sizeCat != StandardRoom.SizeCategory.NORMAL) hasLarge = true;
		if (!hasLarge && areas[largest] * regionCount > totalArea * 1.35f) {
			// 偏心交点产生的大区域可选大房型，利用已有空地而不再扩大地图。
			for (Room r : core) if (!r.isEntrance() && !r.isExit() && r instanceof StandardRoom) {
				StandardRoom s = (StandardRoom) r;
				if (s.sizeCat != StandardRoom.SizeCategory.NORMAL || s.sizeCatProbs()[1] == 0) continue;
				if (s.setSizeCat(1, 1) && s.setSizeWithLimit(s.minWidth() + 1, s.minHeight() + 1)) {
					expanded = s;
					break;
				}
			}
		}

		ArrayList<Room> placed = new ArrayList<>(halls);
		int entryRegion = Random.Int(regionCount) + 1;
		int exitRegion = (entryRegion + Random.Int(regionCount - 1)) % regionCount + 1;
		ArrayList<Room> order = new ArrayList<>(core);
		order.sort((a, b) -> Integer.compare(b.width() * b.height(), a.width() * a.height()));
		int attachQuota = Math.round(core.size() * HALL_ADJACENT_RATIO);
		for (Room r : order) {
			int wanted = r == entrance ? entryRegion : r == exit ? exitRegion : r == expanded ? largest : 0;
			boolean tryAttach = attached.size() < attachQuota;
			if (wanted == 0 && r instanceof StandardRoom
					&& ((StandardRoom) r).sizeCat != StandardRoom.SizeCategory.NORMAL) {
				if (!placeInRegion(r, largest, placed, tryAttach)
						&& !placeBalanced(r, placed, tryAttach)) return null;
			} else if (wanted == 0) {
				if (!placeBalanced(r, placed, tryAttach)) return null;
			} else if (!placeInRegion(r, wanted, placed, tryAttach)) {
				return null;
			}
			placed.add(r);
		}
		// 核心房先按邻近关系保留一部分区域内走廊；贴廊房之间只保留短连接。
		planLinks(core);
		if (!attachAnchors(placed)) return null;
		// 特殊房与秘密房在核心位置和贴廊端点确定后落位，入口不会再隔很远。
		if (!placeLeaves(leaves, core, entrance, placed)) return null;
		// 只允许一个空区，且必须是最小的区域；其余房间已按负载分到其他区域。
		if (!checkEmptyRegions()) return null;

		HashMap<Integer, Room> tunnels = new HashMap<>();
		for (Room[] edge : links) {
			Room a = edge[0], b = edge[1];
			if (!region.get(a).equals(region.get(b))) continue;
			if (!route(a, b, placed, tunnels)) return null;
		}
		// 只有一个普通房间的区域，若仍是游离房，尝试从另一侧接回较远的主廊。
		for (int id = 1; id <= regionCount; id++) {
			Room single = null;
			int count = 0;
			for (Room r : core) if (region.get(r) == id) { single = r; count++; }
			if (count != 1 || attached.contains(single)) continue;
			Room entry = null;
			for (Room h : single.connected.keySet()) if (h instanceof PrisonHallRoom) entry = h;
			if (entry == null) continue;
			Room room = single;
			ArrayList<Room> returns = new ArrayList<>(halls);
			returns.sort((a, b) -> Integer.compare(distance(room, a), distance(room, b)));
			for (Room h : returns) {
				if (distance(h, entry) < 16) continue;
				if (route(room, h, placed, tunnels)) break;
			}
		}
		// 紧凑布局允许房间共墙；为相邻的普通房间、支路和主廊补门。
		findNeighbours(placed);
		ArrayList<Rect> doorWalls = new ArrayList<>();
		HashSet<Room> processed = new HashSet<>();
		for (Room r : placed) {
			processed.add(r);
			for (Room n : r.connected.keySet()) {
				if (processed.contains(n) || r instanceof PrisonHallRoom && n instanceof PrisonHallRoom
						|| r instanceof TunnelRoom && n instanceof TunnelRoom) continue;
				doorWalls.add(r.intersect(n));
			}
		}
		processed.clear();
		HashMap<Room, Integer> extraHallConnections = new HashMap<>();
		for (Room r : placed) {
			processed.add(r);
			if (r instanceof SecretRoom || r.maxConnections(Room.ALL) == 1) continue;
			for (Room n : r.neigbours) {
				if (processed.contains(n) || n instanceof SecretRoom || n.maxConnections(Room.ALL) == 1
						|| r.connected.containsKey(n)) continue;
				if (r instanceof PrisonHallRoom && n instanceof PrisonHallRoom) continue;
				if (!(r instanceof PrisonHallRoom) && !(n instanceof PrisonHallRoom)
						&& !region.getOrDefault(r, 0).equals(region.getOrDefault(n, 0))
						&& !(r instanceof TunnelRoom) && !(n instanceof TunnelRoom)) continue;
				boolean hall = r instanceof PrisonHallRoom || n instanceof PrisonHallRoom;
				Room other = r instanceof PrisonHallRoom ? n : r;
				int extras = extraHallConnections.getOrDefault(other, 0);
				if (hall && extras >= MAX_EXTRA_HALL_CONNECTIONS_PER_ROOM) continue;
				Rect wall = r.intersect(n);
				boolean spaced = true;
				for (Rect existing : doorWalls) {
					int dx = Math.max(0, Math.max(wall.left - existing.right, existing.left - wall.right));
					int dy = Math.max(0, Math.max(wall.top - existing.bottom, existing.top - wall.bottom));
					if (dx + dy < EXTRA_DOOR_MIN_DISTANCE) { spaced = false; break; }
				}
				if (!spaced || Random.Float() >= (hall ? EXTRA_HALL_CONNECTION_CHANCE : EXTRA_ROOM_CONNECTION_CHANCE)) continue;
				if (r.connect(n)) {
					doorWalls.add(wall);
					if (hall) extraHallConnections.put(other, extras + 1);
				}
			}
		}
		return placed;
	}

	private boolean buildHalls(int cells) {
		PrisonHallRoom[][] grid = new PrisonHallRoom[cells][cells];
		int mask = new int[]{15, 7, 3, 5}[hallShape];
		mask = ((mask << hallRotation) | (mask >> (4 - hallRotation))) & 15;
		int cx = Random.IntRange(cells / 3, 2 * cells / 3);
		int cy = Random.IntRange(cells / 3, 2 * cells / 3);
		grid[cx][cy] = hall(cx, cy);
		for (int arm = 0; arm < 4; arm++) {
			// 缺失廊臂为零长度，空出来的区域交给 markRegions 重新合并。
			if ((mask & (1 << arm)) == 0) continue;
			int x = cx, y = cy, steps = 0, sideSteps = 0, lastDir = arm, straightRun = 0;
			int primaryDistance = arm == 0 ? cy : arm == 1 ? cells - 1 - cx : arm == 2 ? cells - 1 - cy : cx;
			// 主廊扩容后仍需足够横移次数，才能遵守连续直走段数的上限。
			int sideBudget = Math.max(1, (primaryDistance + MAX_HALL_STRAIGHT_SEGMENTS - 1)
					/ MAX_HALL_STRAIGHT_SEGMENTS);
			while (x > 0 && y > 0 && x < cells - 1 && y < cells - 1) {
				ArrayList<Integer> dirs = new ArrayList<>();
				dirs.add(arm);
				// 按廊臂长度限制绕行次数，并主动打断过长的直线段。
				if (steps > 1 && sideSteps < sideBudget) {
					dirs.add((arm + 1) % 4);
					dirs.add((arm + 3) % 4);
				}
				if (straightRun >= MAX_HALL_STRAIGHT_SEGMENTS || Random.Float() < 0.35f) {
					Random.shuffle(dirs);
					final int previousDir = lastDir;
					dirs.sort((a, b) -> Integer.compare(a == previousDir ? 1 : 0, b == previousDir ? 1 : 0));
				}
				boolean advanced = false;
				for (int dir : dirs) {
					if (dir == lastDir && straightRun >= MAX_HALL_STRAIGHT_SEGMENTS) continue;
					int nx = x + DX[dir], ny = y + DY[dir];
					if (nx < 0 || ny < 0 || nx >= cells || ny >= cells || grid[nx][ny] != null) continue;
					int forward = (nx - cx) * DX[arm] + (ny - cy) * DY[arm];
					int sideways = Math.abs((nx - cx) * DY[arm] - (ny - cy) * DX[arm]);
					if (sideways > forward / 2) continue;
					int neighbours = 0;
					for (int d = 0; d < 4; d++) {
						int ax = nx + DX[d], ay = ny + DY[d];
						if (ax >= 0 && ay >= 0 && ax < cells && ay < cells && grid[ax][ay] != null) neighbours++;
					}
					if (neighbours != 1) continue;
					grid[nx][ny] = hall(nx, ny);
					grid[nx][ny].arm = arm;
					if (!grid[nx][ny].connect(grid[x][y])) return false;
					x = nx;
					y = ny;
					if (dir != arm) sideSteps++;
					straightRun = dir == lastDir ? straightRun + 1 : 1;
					lastDir = dir;
					steps++;
					advanced = true;
					break;
				}
				if (!advanced) return false;
			}
		}
		return true;
	}

	private PrisonHallRoom hall(int x, int y) {
		PrisonHallRoom r = new PrisonHallRoom();
		r.resize(4, 4);
		r.setPos(x * 4, y * 4);
		halls.add(r);
		return r;
	}

	private boolean markRegions() {
		regions = new int[size * size];
		for (Room hall : halls) {
			for (int y = hall.top; y <= hall.bottom; y++) {
				for (int x = hall.left; x <= hall.right; x++) regions[x + y * size] = -1;
			}
		}
		int count = 0;
		int[] queue = new int[regions.length];
		for (int y = 1; y < size - 1; y++) for (int x = 1; x < size - 1; x++) {
			int start = x + y * size;
			if (regions[start] != 0) continue;
			if (++count > 4) return false;
			int head = 0, tail = 0;
			queue[tail++] = start;
			regions[start] = count;
			while (head < tail) {
				int cell = queue[head++];
				for (int d = 0; d < 4; d++) {
					int nx = cell % size + DX[d], ny = cell / size + DY[d];
					if (nx <= 0 || ny <= 0 || nx >= size - 1 || ny >= size - 1) continue;
					int next = nx + ny * size;
					if (regions[next] == 0) {
						regions[next] = count;
						queue[tail++] = next;
					}
				}
			}
		}
		regionCount = count;
		return count >= 2;
	}

	private boolean scatter(Room r, int wanted, ArrayList<Room> placed) {
		// 支路共墙在偶数坐标。固定入口朝向的房间也必须把允许开门的那面墙对齐。
		int parityX = !r.canConnect(Room.LEFT) && r.canConnect(Room.RIGHT) ? (r.width() - 1) % 2 : 0;
		int parityY = !r.canConnect(Room.TOP) && r.canConnect(Room.BOTTOM) ? (r.height() - 1) % 2 : 0;
		for (int tries = 0; tries < 600; tries++) {
			r.setPos(2 * Random.IntRange(1, (size - r.width() - 1 - parityX) / 2) + parityX,
					2 * Random.IntRange(1, (size - r.height() - 1 - parityY) / 2) + parityY);
			int id = regions[(r.left + 1) + (r.top + 1) * size];
			if (id > 0 && (wanted == 0 || wanted == id) && fits(r, id, placed)) {
				region.put(r, id);
				return true;
			}
		}
		return false;
	}

	private boolean fits(Room r, int id, ArrayList<Room> placed) {
		if (r.left < 0 || r.top < 0 || r.right >= size || r.bottom >= size) return false;
		for (int y = r.top + 1; y < r.bottom; y++) for (int x = r.left + 1; x < r.right; x++) {
			if (regions[x + y * size] != id) return false;
		}
		for (Room other : placed) {
			if (r == other) continue;
			if (Math.max(r.left, other.left) < Math.min(r.right, other.right)
					&& Math.max(r.top, other.top) < Math.min(r.bottom, other.bottom)) return false;
		}
		return true;
	}

	private ArrayList<Integer> regionsByLoad(Room r) {
		ArrayList<Integer> ids = new ArrayList<>();
		for (int id = 1; id <= regionCount; id++) ids.add(id);
		final float area = (float) r.width() * r.height();
		ids.sort((a, b) -> Float.compare(
				(regionLoad[a] + area) / regionAreas[a],
				(regionLoad[b] + area) / regionAreas[b]));
		return ids;
	}

	private boolean placeInRegion(Room r, int id, ArrayList<Room> placed, boolean tryAttach) {
		if (!scatter(r, id, placed)) return false;
		regionLoad[id] += r.width() * r.height();
		if (tryAttach && attachToHall(r, placed, false)) attached.add(r);
		return true;
	}

	private boolean placeBalanced(Room r, ArrayList<Room> placed, boolean tryAttach) {
		for (int id : regionsByLoad(r)) if (placeInRegion(r, id, placed, tryAttach)) return true;
		return false;
	}

	private void planLinks(ArrayList<Room> core) {
		anchors.clear();
		for (int id = 1; id <= regionCount; id++) {
			ArrayList<Room> remaining = new ArrayList<>();
			for (Room r : core) if (region.get(r) == id) remaining.add(r);
			if (remaining.isEmpty()) continue;
			ArrayList<Room> path = new ArrayList<>();
			path.add(remaining.remove(Random.Int(remaining.size())));
			// 普通房间按邻近顺序串成通路，保证游离房之间仍有走廊可走。
			while (!remaining.isEmpty()) {
				Room from = path.get(path.size() - 1), to = null;
				float best = Float.MAX_VALUE;
				for (Room r : remaining) {
					float score = distance(from, r) * Random.Float(0.8f, 1.2f);
					if (score < best) { best = score; to = r; }
				}
				path.add(to);
				remaining.remove(to);
			}
			boolean hasAttached = false;
			for (Room r : path) if (attached.contains(r)) { hasAttached = true; break; }
			for (int i = 0; i + 1 < path.size(); i++) {
				Room a = path.get(i), b = path.get(i + 1);
				// 贴廊房之间仍保留少量近距离走廊，避免整个区域只剩主廊一条通道。
				if (attached.contains(a) && attached.contains(b)
						&& (distance(a, b) > MAX_ATTACHED_LINK_DISTANCE
						|| Random.Float() >= ATTACHED_LINK_CHANCE)) {
					continue;
				}
				links.add(new Room[]{a, b});
			}
			if (!hasAttached) {
				// 没有贴廊房的区域：链首尾仍按原方式接回主廊。
				anchors.add(path.get(0));
				if (path.size() > 1) anchors.add(path.get(path.size() - 1));
			}
		}
	}

	private boolean attachAnchors(ArrayList<Room> placed) {
		ArrayList<Room> order = new ArrayList<>(anchors);
		order.sort((a, b) -> Integer.compare(b.width() * b.height(), a.width() * a.height()));
		placed.removeAll(order);
		for (Room r : order) {
			if (!attachToHall(r, placed, true)) return false;
			attached.add(r);
			placed.add(r);
		}
		return true;
	}

	private boolean placeLeaves(ArrayList<Room> leaves, ArrayList<Room> core, Room entrance,
			ArrayList<Room> placed) {
		ArrayList<Room> order = new ArrayList<>(leaves);
		order.sort((a, b) -> Integer.compare(b.width() * b.height(), a.width() * a.height()));
		for (Room r : order) {
			if (r instanceof ShopRoom) {
				int id = region.get(entrance);
				region.put(r, id);
				// 商店继续与上楼梯房共墙，不能只经一条远端支路连回入口。
				if (!attachSecret(entrance, r, placed)) return false;
				regionLoad[id] += r.width() * r.height();
				placed.add(r);
				continue;
			}
			boolean done = false;
			for (int id : regionsByLoad(r)) {
				if (!scatter(r, id, placed)) continue;
				// 秘密房也可以直接贴主廊；门型由房间自身的 HIDDEN 决定。
				if (attachToHall(r, placed, false)) {
					attached.add(r);
					done = true;
				} else {
					// 特殊房只走“贴主廊或贴父房共墙”，不再用长走廊接入。
					Room parent = findParent(core, id, r);
					if (parent != null && attachSecret(parent, r, placed)) done = true;
				}
				if (done) {
					regionLoad[id] += r.width() * r.height();
					placed.add(r);
					break;
				}
			}
			if (!done) return false;
		}
		return true;
	}

	private Room findParent(ArrayList<Room> core, int id, Room leaf) {
		Room best = null;
		int bestScore = Integer.MAX_VALUE;
		for (Room r : core) {
			if (region.get(r) != id || r.remConnections(Room.ALL) <= 0) continue;
			// 优先贴主廊的父房，让特殊房入口尽量靠近主廊。
			int score = distance(r, leaf) - (attached.contains(r) ? 8 : 0);
			if (score < bestScore) {
				bestScore = score;
				best = r;
			}
		}
		return best;
	}

	private boolean checkEmptyRegions() {
		int[] occupied = new int[regionCount + 1];
		for (Room r : region.keySet()) {
			int id = region.get(r);
			if (id >= 1 && id <= regionCount) occupied[id]++;
		}
		int empty = 0, emptyId = 0;
		for (int id = 1; id <= regionCount; id++) {
			if (occupied[id] == 0) {
				empty++;
				emptyId = id;
			}
		}
		// 只允许一个空区，且必须是最小的区域；其余房间已按负载分到其他区域。
		return empty == 0 || empty == 1 && regionAreas[emptyId] == regionAreas[smallestRegion];
	}

	private int distance(Room a, Room b) {
		return Math.abs(a.left + a.right - b.left - b.right) + Math.abs(a.top + a.bottom - b.top - b.bottom);
	}

	private boolean attachToHall(Room r, ArrayList<Room> placed, boolean spread) {
		int oldLeft = r.left, oldTop = r.top;
		ArrayList<Room> candidates = new ArrayList<>(halls);
		Random.shuffle(candidates);
		ArrayList<Room> entries = new ArrayList<>();
		for (Room other : placed) {
			if (!region.get(r).equals(region.get(other))) continue;
			for (Room h : other.connected.keySet()) if (h instanceof PrisonHallRoom) entries.add(h);
		}
		HashMap<Room, Integer> scores = new HashMap<>();
		for (Room h : candidates) {
			int separation = size * 4;
			for (Room entry : entries) separation = Math.min(separation, distance(h, entry));
			scores.put(h, distance(r, h) + (spread ? -separation * 2 : 0));
		}
		candidates.sort((a, b) -> Integer.compare(scores.get(a), scores.get(b)));
		for (Room h : candidates) {
			if (spread) {
				boolean separate = true;
				for (Room entry : entries) if (distance(h, entry) < 16) separate = false;
				if (!separate) continue;
			}
			for (int d = 0; d < 4; d++) for (int offset = 1; offset < 4; offset++) {
				if (d == 0) r.setPos(h.left + offset - r.width() / 2, h.top - r.height() + 1);
				if (d == 1) r.setPos(h.right, h.top + offset - r.height() / 2);
				if (d == 2) r.setPos(h.left + offset - r.width() / 2, h.bottom);
				if (d == 3) r.setPos(h.left - r.width() + 1, h.top + offset - r.height() / 2);
				// 支路中心在奇数格、共墙在偶数格，贴廊后也保留可接支路的墙边。
				if (r.width() % 2 == 1 && r.left % 2 != 0
						|| r.height() % 2 == 1 && r.top % 2 != 0) continue;
				if (fits(r, region.get(r), placed) && r.connect(h)) return true;
			}
		}
		r.setPos(oldLeft, oldTop);
		return false;
	}

	private boolean attachSecret(Room parent, Room r, ArrayList<Room> placed) {
		int oldLeft = r.left, oldTop = r.top;
		for (int d = 0; d < 4; d++) {
			for (int offset = -Math.max(r.width(), r.height()); offset <= Math.max(parent.width(), parent.height()); offset++) {
				if (d == 0) r.setPos(parent.left + offset, parent.top - r.height() + 1);
				if (d == 1) r.setPos(parent.right, parent.top + offset);
				if (d == 2) r.setPos(parent.left + offset, parent.bottom);
				if (d == 3) r.setPos(parent.left - r.width() + 1, parent.top + offset);
				if (fits(r, region.get(r), placed) && r.connect(parent)) return true;
			}
		}
		r.setPos(oldLeft, oldTop);
		return false;
	}

	private boolean route(Room from, Room to, ArrayList<Room> placed, HashMap<Integer, Room> tunnels) {
		if (from.connected.containsKey(to)) return true;
		// 最终位置已经确定，相邻房间直接共墙开门，不再强行绕一条折线走廊。
		Rect shared = from.intersect(to);
		if ((shared.width() == 0 && shared.height() >= 2)
				|| (shared.height() == 0 && shared.width() >= 2)) {
			if (from.connect(to)) return true;
		}
		int id = region.get(from);
		boolean[] open = new boolean[size * size];
		TunnelRoom probe = new TunnelRoom();
		probe.resize(2, 2);
		for (int y = 1; y < size - 1; y += 2) for (int x = 1; x < size - 1; x += 2) {
			int cell = x + y * size;
			probe.setPos(x - 1, y - 1);
			open[cell] = regions[cell] == id && (tunnels.containsKey(cell) || fits(probe, id, placed));
		}
		ArrayList<Integer> starts = doorCells(from, open);
		if (to instanceof PrisonHallRoom) {
			// 返回主廊的支路从房间其他侧出发，避免在原入口旁再开一个口。
			for (Room h : from.connected.keySet()) if (h instanceof PrisonHallRoom) {
				for (int i = starts.size() - 1; i >= 0; i--) {
					int x = starts.get(i) % size, y = starts.get(i) / size;
					if (h.right == from.left && x < from.left
							|| h.left == from.right && x > from.right
							|| h.bottom == from.top && y < from.top
							|| h.top == from.bottom && y > from.bottom) starts.remove(i);
				}
			}
		}
		HashSet<Integer> goals = new HashSet<>(doorCells(to, open));
		goals.removeAll(starts);
		if (starts.isEmpty() || goals.isEmpty()) return false;
		int[] remaining = new int[open.length];
		Arrays.fill(remaining, -1);
		int[] queue = new int[open.length];
		int head = 0, tail = 0;
		for (int cell : goals) { remaining[cell] = 0; queue[tail++] = cell; }
		while (head < tail) {
			int cell = queue[head++];
			for (int d = 0; d < 4; d++) {
				int nx = cell % size + 2 * DX[d], ny = cell / size + 2 * DY[d];
				if (nx <= 0 || ny <= 0 || nx >= size - 1 || ny >= size - 1) continue;
				int next = nx + ny * size;
				if (open[next] && remaining[next] == -1) {
					remaining[next] = remaining[cell] + 1;
					queue[tail++] = next;
				}
			}
		}
		int shortest = Integer.MAX_VALUE;
		for (int cell : starts) if (remaining[cell] >= 0) shortest = Math.min(shortest, remaining[cell]);
		if (shortest == Integer.MAX_VALUE) return false;
		// 长支路必须多次转折，绕行余量随距离增长，避免最短路可达却永远无合法折线路径。
		int maxNodes = Math.max(4, shortest + Math.max(5, shortest / 2 + 2));
		int stride = 4 * MAX_TUNNEL_STRAIGHT_STEPS * 2;
		int[] previous = new int[open.length * stride];
		int[] cost = new int[previous.length], length = new int[previous.length];
		Arrays.fill(previous, -1);
		Arrays.fill(cost, Integer.MAX_VALUE);
		int[] variation = new int[open.length];
		for (int cell = 0; cell < open.length; cell++) if (open[cell]) variation[cell] = Random.Int(3);
		PriorityQueue<int[]> frontier = new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0]));
		for (int cell : starts) if (remaining[cell] >= 0) {
			int x = cell % size, y = cell / size;
			int dir = x < from.left ? 3 : x > from.right ? 1 : y < from.top ? 0 : 2;
			int state = cell * stride + dir * MAX_TUNNEL_STRAIGHT_STEPS * 2;
			cost[state] = 0;
			length[state] = 1;
			frontier.add(new int[]{remaining[cell] * 10, state, 0});
		}
		int end = -1;
		while (!frontier.isEmpty()) {
			int[] entry = frontier.remove();
			int state = entry[1];
			if (entry[2] != cost[state]) continue;
			int cell = state / stride, local = state % stride;
			int dir = local / (MAX_TUNNEL_STRAIGHT_STEPS * 2);
			int run = local / 2 % MAX_TUNNEL_STRAIGHT_STEPS + 1;
			boolean bent = (state & 1) != 0;
			if (goals.contains(cell) && bent && length[state] >= 3) { end = state; break; }
			if (length[state] >= maxNodes) continue;
			for (int d = 0; d < 4; d++) {
				if (d == (dir + 2) % 4) continue;
				int nextRun = d == dir ? run + 1 : 1;
				if (nextRun > MAX_TUNNEL_STRAIGHT_STEPS) continue;
				int nx = cell % size + 2 * DX[d], ny = cell / size + 2 * DY[d];
				if (nx <= 0 || ny <= 0 || nx >= size - 1 || ny >= size - 1) continue;
				int next = nx + ny * size;
				if (!open[next] || remaining[next] < 0 || length[state] + remaining[next] >= maxNodes) continue;
				boolean repeated = false;
				for (int ancestor = state; ancestor != -1; ancestor = previous[ancestor]) {
					if (ancestor / stride == next) { repeated = true; break; }
				}
				if (repeated) continue;
				int nextState = next * stride + d * MAX_TUNNEL_STRAIGHT_STEPS * 2
						+ (nextRun - 1) * 2 + (bent || d != dir ? 1 : 0);
				int nextCost = cost[state] + 10 + variation[next] + (d == dir ? 0 : 3);
				if (nextCost >= cost[nextState]) continue;
				cost[nextState] = nextCost;
				length[nextState] = length[state] + 1;
				previous[nextState] = state;
				frontier.add(new int[]{nextCost + remaining[next] * 10, nextState, nextCost});
			}
		}
		if (end == -1) return false;
		ArrayList<Integer> path = new ArrayList<>();
		for (int state = end; state != -1; state = previous[state]) {
			path.add(state / stride);
		}
		Room prev = to;
		for (int cell : path) {
			Room next = tunnels.get(cell);
			if (next == null) {
				next = new TunnelRoom();
				next.resize(2, 2);
				next.setPos(cell % size - 1, cell / size - 1);
				tunnels.put(cell, next);
				placed.add(next);
			}
			if (!next.connected.containsKey(prev) && !next.connect(prev)) return false;
			prev = next;
		}
		return prev.connected.containsKey(from) || prev.connect(from);
	}

	private ArrayList<Integer> doorCells(Room room, boolean[] open) {
		ArrayList<Integer> result = new ArrayList<>();
		TunnelRoom probe = new TunnelRoom();
		probe.resize(2, 2);
		for (int y = Math.max(1, room.top - 1); y <= Math.min(size - 2, room.bottom + 1); y++) {
			for (int x = Math.max(1, room.left - 1); x <= Math.min(size - 2, room.right + 1); x++) {
				int cell = x + y * size;
				if (!open[cell]) continue;
				probe.setPos(x - 1, y - 1);
				if (probe.canConnect(room) && room.canConnect(probe)) result.add(cell);
			}
		}
		return result;
	}
}
