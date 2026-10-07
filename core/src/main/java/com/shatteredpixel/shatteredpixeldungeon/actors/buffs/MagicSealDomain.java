package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

public class MagicSealDomain extends Buff {

	{
		type = buffType.NEUTRAL;
		announced = true;
	}

	@Override
	public int icon() {
		return BuffIndicator.MAGIC_SEAL_DOMAIN;
	}
}
