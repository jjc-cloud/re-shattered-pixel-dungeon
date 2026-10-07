package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

public class MagicOnlyDomain extends DamageDomain {

	{
		blocksPhysical = true;
	}

	@Override
	public int icon() {
		return BuffIndicator.MAGIC_ONLY_DOMAIN;
	}
}
