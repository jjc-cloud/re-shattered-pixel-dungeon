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

package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.ChargeItem;

/**
 * Base class for artifacts which use a discrete current/max charge pool.
 */
public abstract class ChargedArtifact extends Artifact implements ChargeItem {

	@Override
	public int currentCharge() {
		return charge;
	}

	@Override
	public int maxCharge() {
		return chargeCap;
	}

	@Override
	public float partialCharge() {
		return partialCharge;
	}

	protected void gainChargeProgress(Char owner, float rawGain) {
		Hero hero = owner instanceof Hero ? (Hero) owner : null;
		partialCharge += adjustRechargeGain(hero, rawGain);
	}

	@Override
	public void spendCharge(float cost) {
		partialCharge -= cost;
		while (partialCharge < 0) {
			charge--;
			partialCharge++;
		}
	}
}
