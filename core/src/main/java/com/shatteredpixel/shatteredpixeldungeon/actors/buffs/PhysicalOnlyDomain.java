package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

public class PhysicalOnlyDomain extends DamageDomain {

	{
		blocksMagic = true;
	}

	@Override
	public int icon() {
		return BuffIndicator.PHYSICAL_ONLY_DOMAIN;
	}
}
