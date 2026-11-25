package com.shatteredpixel.shatteredpixeldungeon.levels.builders;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.CavesLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.RoomProtection;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.connection.CavesPathRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.BlacksmithRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.ShopRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.SuperSecretRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.CaveRoom;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;

/** 按房间实际尺寸分配支路；普通层使用短连接，悬空层使用较长桥面。 */
public class CavesBuilder extends Builder {
    public boolean chasm;
    private final int branchCount;
    private final CavesLevel.Orientation orientation = CavesLevel.orientation();
    private final ArrayList<int[]> branchGaps = new ArrayList<>();

    public CavesBuilder(boolean large) {
        branchCount = (Dungeon.depth == 11 ? 2 : 2 + Random.chances(new float[]{1, 3})) + (large ? 1 : 0);
    }

    public int branchCount() { return branchCount; }

    @Override
    public ArrayList<Room> build(ArrayList<Room> rooms) {
        Room entrance = null, exit = null, shop = null;
        ArrayList<Room> ordinary = new ArrayList<>(), special = new ArrayList<>(), leaves = new ArrayList<>();
        for (Room room : rooms) {
            room.clearConnections();
            room.setEmpty();
            if (room.isEntrance()) entrance = room;
            else if (room.isExit()) exit = room;
            else if (room instanceof ShopRoom) shop = room;
            else if (room instanceof SpecialRoom) special.add(room);
            else if (room instanceof BlacksmithRoom || room.maxConnections(Room.ALL) == 1) leaves.add(room);
            else ordinary.add(room);
        }
        if (entrance == null || exit == null || ordinary.size() < 2) return null;
        int stairWidth = branchCount == 4 ? 13 : 11;
        if (!entrance.forceSize(vertical() ? stairWidth : 11, vertical() ? 11 : stairWidth)
                || !exit.forceSize(vertical() ? stairWidth : 11, vertical() ? 11 : stairWidth)) return null;
        for (Room room : ordinary) {
            if (!room.setSize()) return null;
            int width = oddSize(room.width(), room.minWidth(), room.maxWidth());
            int height = oddSize(room.height(), room.minHeight(), room.maxHeight());
            // 外侧房需要与两端楼梯房共墙，七格边长能留出有效入口墙段。
            if (vertical()) height = Math.max(height, Math.min(7, room.maxHeight()));
            else width = Math.max(width, Math.min(7, room.maxWidth()));
            if (!room.forceSize(width, height)) return null;
        }
        ordinary.sort((a, b) -> Integer.compare(b.square(), a.square()));

        ArrayList<ArrayList<Room>> branches = new ArrayList<>();
        for (int i = 0; i < branchCount; i++) branches.add(new ArrayList<>());
        int coreCount = Math.max(2, ordinary.size() - (leaves.isEmpty() ? 1 : 0));
        branches.get(0).add(ordinary.get(0));
        branches.get(branchCount - 1).add(ordinary.get(1));
        // 两侧容纳大房间；内侧只分配横向尺寸能落在楼梯入口墙段内的房间。
        int innerWidth = 5;
        for (int i = 2; i < coreCount; i++) {
            Room room = ordinary.get(i);
            int best = 0;
            int load = span(branches.get(0));
            for (int branch = 1; branch < branchCount; branch++) {
                boolean inner = branch < branchCount - 1;
                if (inner && across(room) > innerWidth) continue;
                int candidate = span(branches.get(branch));
                if (candidate < load) { best = branch; load = candidate; }
            }
            branches.get(best).add(room);
        }
        for (int i = coreCount; i < ordinary.size(); i++) leaves.add(ordinary.get(i));

        // 用沿途房间补齐较短支路，不再拆掉长支路的尾房来压缩楼梯间距。
        int distance = -1;
        for (int pass = 0; pass < ordinary.size() + branchCount; pass++) {
            distance = staircaseDistance(branches, entrance);
            if (distance >= 0) break;
            int shortest = 0;
            int shortestLength = Integer.MAX_VALUE;
            for (int branch = 0; branch < branchCount; branch++) {
                int length = span(branches.get(branch));
                if (length < shortestLength) { shortest = branch; shortestLength = length; }
            }
            CaveRoom room = new CaveRoom();
            if (!room.setSizeCat(0, 0)) return null;
            int width = shortest > 0 && shortest < branchCount - 1 ? innerWidth : 9;
            if (!room.forceSize(vertical() ? width : 9, vertical() ? 9 : width)) return null;
            branches.get(shortest).add(room);
        }
        if (distance < 0) return null;

        // 距离来自上述尺寸分配，方向仍由整局种子决定；不再设置额外的最小间距。
        position(entrance, -across(entrance) / 2, -along(entrance) / 2);
        position(exit, -across(exit) / 2, distance - along(exit) / 2);
        ArrayList<Room> placed = new ArrayList<>();
        placed.add(entrance);
        placed.add(exit);
        // 商店独立于普通旁室，先为它保留入口背面的共墙连接。
        if (shop != null) {
            if (placeRoom(placed, entrance, shop, 90f * orientation.ordinal()) == -1) return null;
            placed.add(shop);
        }
        for (int branch = 0; branch < branchCount; branch++) {
            ArrayList<Room> chain = branches.get(branch);
            int[] gaps = branchGaps.get(branch);
            // 全部支路从入口房前沿出发，到出口房后沿结束，不能在房间侧面提前接回楼梯。
            int top = along(entrance) / 2;
            Room previous = entrance;
            for (int i = 0; i < chain.size(); i++) {
                Room room = chain.get(i);
                top += gaps[i];
                int left;
                if (branch == 0) left = -2 * (branchCount - 2) - across(room) + 1;
                else if (branch == branchCount - 1) left = 2 * (branchCount - 2);
                else left = (branchCount == 4 ? (branch == 1 ? -2 : 2) : 0) - across(room) / 2;
                position(room, left, top);
                if (overlaps(room, placed)) return null;
                placed.add(room);
                if (!connectShort(previous, room, branch, placed)) return null;
                previous = room;
                top += along(room) - 1;
            }
            if (!connectShort(previous, exit, branch, placed)) return null;
        }

        // 所有已放置且还能连接的房间、架构都进入候选池，不限定为普通房间。
        ArrayList<Room> anchors = new ArrayList<>(placed);
        leaves.sort((a, b) -> Integer.compare(b.minWidth() * b.minHeight(), a.minWidth() * a.minHeight()));
        for (Room room : leaves) {
            if (!attachCompact(room, anchors, placed)) return null;
            placed.add(room);
            anchors.add(room);
        }
        for (Room room : special) {
            ArrayList<Room> candidates = anchors;
            if (room instanceof SuperSecretRoom) {
                candidates = new ArrayList<>();
                for (Room anchor : placed) {
                    if (anchor instanceof StandardRoom && !(anchor instanceof BlacksmithRoom)
                            && !anchor.isEntrance() && !anchor.isExit()) candidates.add(anchor);
                }
            }
            if (!attachCompact(room, candidates, placed)) return null;
            placed.add(room);
            anchors.add(room);
        }
        return placed;
    }

    private int staircaseDistance(ArrayList<ArrayList<Room>> branches, Room entrance) {
        int base = along(entrance) - 1;
        int minimumGap = chasm ? 3 : 0;
        int maximumGap = chasm ? 5 : 3;
        int longest = 0;
        for (ArrayList<Room> chain : branches) longest = Math.max(longest, span(chain) + minimumGap * (chain.size() + 1));
        int extra = maximumGap - minimumGap;
        for (ArrayList<Room> chain : branches) {
            if (chain.isEmpty()) return -1;
            // 普通层沿用短连接；悬空层留出三至五格深渊，再用一段桥面跨过。
            int capacity = maximumGap * (chain.size() + 1) - (longest - span(chain));
            if (capacity < 0) return -1;
            extra = Math.min(extra, capacity);
        }
        int padding = Random.IntRange(0, extra);
        branchGaps.clear();
        for (ArrayList<Room> chain : branches) {
            int[] gaps = new int[chain.size() + 1];
            java.util.Arrays.fill(gaps, minimumGap);
            int remaining = longest - span(chain) + padding - minimumGap * gaps.length;
            while (remaining > 0) {
                int index = Random.Int(gaps.length);
                if (gaps[index] == maximumGap) continue;
                gaps[index]++;
                remaining--;
            }
            branchGaps.add(gaps);
        }
        return base + longest + padding;
    }

    /** 每个间隔用一段连接跨过；悬空层允许更长的桥面。 */
    private boolean connectShort(Room a, Room b, int branch, ArrayList<Room> placed) {
        if (a.connect(b)) return true;
        Room first = a, second = b;
        boolean horizontal = false;
        int maximumGap = chasm ? 5 : 3;
        if (b.bottom < a.top && a.top - b.bottom <= maximumGap || b.right < a.left && a.left - b.right <= maximumGap) {
            first = b; second = a;
        }
        int gap = second.top - first.bottom;
        if (gap < 1 || gap > maximumGap) {
            gap = second.left - first.right;
            if (gap < 1 || gap > maximumGap) return false;
            horizontal = true;
        }
        int low = horizontal ? Math.max(first.top, second.top) + 1 : Math.max(first.left, second.left) + 1;
        int high = horizontal ? Math.min(first.bottom, second.bottom) - 1 : Math.min(first.right, second.right) - 1;
        // 较长的连接可以错开两端入口，但横向仍限制在两端房间的共同墙段。
        int maxWidth = gap == 1 ? 3 : 5;
        for (int width = Math.min(maxWidth, high - low + 3); width >= 3; width--) {
            int positions = high - low + 4 - width;
            int offset = Random.Int(positions);
            for (int tries = 0; tries < positions; tries++) {
                int side = low - 1 + (offset + tries) % positions;
                CavesPathRoom path = new CavesPathRoom();
                path.branch = branch;
                path.forceSize(horizontal ? gap + 1 : width, horizontal ? width : gap + 1);
                path.setPos(horizontal ? first.right : side, horizontal ? side : first.bottom);
                if (!overlaps(path, placed) && first.connect(path) && path.connect(second)) {
                    placed.add(path);
                    return true;
                }
                path.clearConnections();
            }
        }
        return false;
    }

    /** 复用 placeRoom 探索共墙位置，优先填补不同支路之间的薄隔墙位置。 */
    private boolean attachCompact(Room room, ArrayList<Room> anchors, ArrayList<Room> placed) {
        Room best = null;
        int left = 0, top = 0, width = 0, height = 0, bestArea = Integer.MAX_VALUE;
        for (int attempt = 0; attempt < 64; attempt++) {
            Room anchor = anchors.get(attempt % anchors.size());
            if (placeRoom(placed, anchor, room, 90f * Random.Int(4) + Random.Float(-20f, 20f)) == -1) {
                room.clearConnections();
                continue;
            }
            int minX = room.left, minY = room.top, maxX = room.right, maxY = room.bottom;
            for (Room other : placed) {
                minX = Math.min(minX, other.left); minY = Math.min(minY, other.top);
                maxX = Math.max(maxX, other.right); maxY = Math.max(maxY, other.bottom);
            }
            int area = (maxX - minX + 1) * (maxY - minY + 1);
            if (!RoomProtection.isProtected(room)) for (Room other : placed) {
                if (other == anchor || other.isEntrance() || other.isExit() || RoomProtection.isProtected(other)) continue;
                int xGap = Math.max(room.left, other.left) - Math.min(room.right, other.right);
                int yGap = Math.max(room.top, other.top) - Math.min(room.bottom, other.bottom);
                if (xGap >= 0 && xGap <= 1) area -= 12 * Math.max(0, Math.min(room.bottom, other.bottom) - Math.max(room.top, other.top) - 1);
                if (yGap >= 0 && yGap <= 1) area -= 12 * Math.max(0, Math.min(room.right, other.right) - Math.max(room.left, other.left) - 1);
            }
            if (area < bestArea) {
                best = anchor; bestArea = area;
                left = room.left; top = room.top; width = room.width(); height = room.height();
            }
            room.clearConnections();
        }
        if (best == null || !room.forceSize(width, height)) return false;
        room.setPos(left, top);
        return room.connect(best);
    }

    private static boolean overlaps(Room room, ArrayList<Room> placed) {
        for (Room other : placed) {
            if (room == other) continue;
            if (Math.max(room.left, other.left) < Math.min(room.right, other.right)
                    && Math.max(room.top, other.top) < Math.min(room.bottom, other.bottom)) return true;
        }
        return false;
    }

    private int span(ArrayList<Room> chain) {
        int length = 0;
        for (Room room : chain) length += along(room) - 1;
        return length;
    }

    private static int oddSize(int size, int minimum, int maximum) {
        if ((size & 1) == 1) return size;
        return size + 1 <= maximum ? size + 1 : Math.max(minimum, size - 1);
    }

    private boolean vertical() {
        return orientation == CavesLevel.Orientation.NORTH || orientation == CavesLevel.Orientation.SOUTH;
    }
    private int along(Room room) { return vertical() ? room.height() : room.width(); }
    private int across(Room room) { return vertical() ? room.width() : room.height(); }

    private void position(Room room, int sideways, int forward) {
        Point first = world(sideways, forward);
        Point last = world(sideways + across(room) - 1, forward + along(room) - 1);
        room.setPos(Math.min(first.x, last.x), Math.min(first.y, last.y));
    }

    private Point world(int sideways, int forward) {
        switch (orientation) {
            case NORTH: return new Point(sideways, forward);
            case EAST: return new Point(-forward, sideways);
            case SOUTH: return new Point(-sideways, -forward);
            default: return new Point(forward, -sideways);
        }
    }
}
