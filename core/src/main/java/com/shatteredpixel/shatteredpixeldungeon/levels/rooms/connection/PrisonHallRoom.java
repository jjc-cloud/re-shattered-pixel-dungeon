package com.shatteredpixel.shatteredpixeldungeon.levels.rooms.connection;

import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.CellPlacement;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.watabou.utils.Point;
import com.watabou.utils.Bundle;

/** 主廊以共墙的 5×5 段组成，内部及段间开口均为 3 格宽。 */
public class PrisonHallRoom extends ConnectionRoom {

	// 四条支线分别作为一个物品生成房间，交点本身不额外计权重。
	public int arm = -1;

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("hall_arm", arm);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		arm = bundle.getInt("hall_arm");
	}

	@Override
	public void paint(Level level) {
		// 深渊主廊保持悬空，贴过来的房间墙壁由各自房间绘制并保留。
		if (level.feeling != Level.Feeling.CHASM) {
			Painter.fill(level, this, CellPlacement.ground(Terrain.EMPTY, Terrain.WALL));
		}
		Painter.fill(level, this, 1, CellPlacement.reset(Terrain.EMPTY));
		for (Room other : connected.keySet()) {
			Door door = connected.get(other);
			if (other instanceof PrisonHallRoom) {
				if (other.left == right || other.right == left) {
					int x = other.left == right ? right : left;
					Painter.fill(level, x, top + 1, 1, 3, CellPlacement.reset(Terrain.EMPTY));
				} else {
					int y = other.top == bottom ? bottom : top;
					Painter.fill(level, left + 1, y, 3, 1, CellPlacement.reset(Terrain.EMPTY));
				}
				// 让连接图与实际通路一致，且不在廊段之间生成单格门。
				door.set(Door.Type.EMPTY);
			} else {
				door.set(Door.Type.REGULAR);
			}
		}
	}

	@Override public int minConnections(int direction) { return direction == ALL ? 1 : 0; }
	@Override public boolean canPlaceWater(Point p) { return true; }
	@Override public boolean canPlaceGrass(Point p) { return true; }
	@Override public boolean canPlaceTrap(Point p) { return true; }
}
