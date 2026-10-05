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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.levels.rooms;

import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.connection.MazeConnectionRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.SecretRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.CrystalPathRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.MagicalFireRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.PoolRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SacrificeRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SentryRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.ToxicGasRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.TrapsRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.WeakFloorRoom;

import java.util.Arrays;
import java.util.List;

/** 需要保留隐藏入口、解谜布局或入口限制的房间边界保护规则。 */
public final class RoomProtection {

	// 新增需要保护边界的房间类型时，在此添加；所列类型的子类自动继承保护。
	private static final List<Class<? extends Room>> PROTECTED_TYPES = Arrays.asList(
			SecretRoom.class,
			MazeConnectionRoom.class,
			PoolRoom.class,
			SentryRoom.class,
			TrapsRoom.class,
			ToxicGasRoom.class,
			MagicalFireRoom.class,
			CrystalPathRoom.class,
			WeakFloorRoom.class,
			SacrificeRoom.class
	);

	private RoomProtection() {}

	public static boolean isProtected(Room room) {
		for (Class<? extends Room> type : PROTECTED_TYPES) {
			if (type.isInstance(room)) return true;
		}

		// 连接门由两间房共用，仅检查特殊房间自身的入口，避免保护另一侧普通房间。
		Room.Door entrance = room instanceof SpecialRoom ? ((SpecialRoom) room).entrance() : null;
		return entrance != null && (entrance.type == Room.Door.Type.LOCKED
				|| entrance.type == Room.Door.Type.CRYSTAL || entrance.type == Room.Door.Type.BARRICADE);
	}
}
