package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

/** 独立于普通失明，魔能透视不会清除或免疫此状态。 */
public class ForcedBlindness extends FlavourBuff {

	public static final float DURATION = 3f;

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	public static void apply(Char target) {
		ForcedBlindness blindness = Buff.affect(target, ForcedBlindness.class);
		if (blindness.target != null) blindness.postpone(DURATION);
	}

	@Override
	public boolean attachTo(Char target) {
		if (!super.attachTo(target)) return false;
		if (Dungeon.level != null) {
			if (target == Dungeon.hero) Dungeon.observe();
			else if (target.fieldOfView != null) Dungeon.level.updateFieldOfView(target, target.fieldOfView);
		}
		return true;
	}

	@Override
	public void detach() {
		super.detach();
		if (Dungeon.level != null) {
			if (target == Dungeon.hero) Dungeon.observe();
			else if (target.fieldOfView != null) Dungeon.level.updateFieldOfView(target, target.fieldOfView);
		}
	}

	@Override
	public int icon() {
		return BuffIndicator.BLINDNESS;
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (DURATION - visualcooldown()) / DURATION);
	}
}
