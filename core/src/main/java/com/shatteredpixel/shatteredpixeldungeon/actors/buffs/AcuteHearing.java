package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

/** 持续感知生物位置并揭示其脚下地形，不扩展视野。 */
public class AcuteHearing extends FlavourBuff {

	public static final float DURATION = 200f;
	public static final int DISTANCE = 12;

	{
		type = buffType.POSITIVE;
		announced = true;
	}

	@Override
	public void detach() {
		boolean wasAttached = target != null && target.buff(AcuteHearing.class) == this;
		super.detach();
		if (wasAttached && target.isAlive()) Buff.prolong(target, HearingLoss.class, HearingLoss.DURATION);
	}

	@Override
	public int icon() {
		return BuffIndicator.ACUTE_HEARING;
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (DURATION - visualcooldown()) / DURATION);
	}
}
