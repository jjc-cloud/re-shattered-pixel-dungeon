package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;

/**
 * A discrete charge pool displayed and spent as current/max charges.
 */
public interface ChargeItem {

	int currentCharge();

	int maxCharge();

	float partialCharge();

	void spendCharge(float cost);

	default boolean canSpendCharge(Hero hero, float cost) {
		return currentCharge() >= cost
				|| (cost > 0
				&& currentCharge() >= 0
				&& hero != null
				&& hero.hasTalent(Talent.MANA_OVERLOAD));
	}

	default int rechargeReferenceCharge() {
		return Math.max(0, currentCharge());
	}

	default float adjustRechargeGain(Hero hero, float rawGain) {
		if (rawGain <= 0 || currentCharge() >= 0) return rawGain;

		float gain = rawGain;
		if (hero != null && hero.pointsInTalent(Talent.MANA_OVERLOAD) == 2) {
			gain *= 2f;
		}

		//Debt recovery ends exactly at zero, so its acceleration never spills into 0 -> 1.
		return Math.min(gain, Math.max(0f, -currentCharge() - partialCharge()));
	}
}
