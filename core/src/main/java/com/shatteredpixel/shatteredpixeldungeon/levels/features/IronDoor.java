package com.shatteredpixel.shatteredpixeldungeon.levels.features;

import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

/** 铁门沿用门的全部交互与外观状态，仅定义材质差异。 */
public class IronDoor extends Door {

	public IronDoor() {
		closedFlags = Terrain.PASSABLE | Terrain.SOLID;
		openFlags = Terrain.PASSABLE;
		excludedFlags = Terrain.LOS_BLOCKING;
		opensOnImpact = false;
		// 已发现的铁门上锁后仍保留材质性质；隐藏状态由 Door 按墙处理。
		unlockedOnlyProperties = 0;
		materialProperties = OCCUPIES_SPACE | CONDUCTIVE | PASSES_NON_PROJECTILES | EXPLOSION_BREAKABLE
				| DISINTEGRATION_BREAKABLE | LEAVES_EMBERS;
	}

	public IronDoor(int terrain) {
		this();
		if (state(terrain) == null || terrain == Terrain.CRYSTAL_DOOR)
			throw new IllegalArgumentException("铁门不支持此状态");
		type = terrain;
	}

	@Override public String name() {
		if (unlocked()) return Messages.get(this, state() == State.OPEN ? "open_name" : "name");
		return super.name();
	}

	/** 关闭的铁门对投射物等同墙体：投射物停在门外，不能压在门上。 */
	@Override public int properties() {
		int properties = super.properties();
		return state() == State.OPEN
				? properties & ~BLOCKS_PROJECTILES
				: properties | BLOCKS_PROJECTILES;
	}
}
