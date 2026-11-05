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
