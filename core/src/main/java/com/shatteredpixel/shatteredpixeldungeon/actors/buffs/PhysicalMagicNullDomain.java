package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

public class PhysicalMagicNullDomain extends DamageDomain {

	{
		blocksPhysical = true;
		blocksMagic = true;
	}

	@Override
	public int icon() {
		return BuffIndicator.PHYSICAL_MAGIC_NULL_DOMAIN;
	}
}
