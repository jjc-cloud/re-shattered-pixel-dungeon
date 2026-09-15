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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

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
