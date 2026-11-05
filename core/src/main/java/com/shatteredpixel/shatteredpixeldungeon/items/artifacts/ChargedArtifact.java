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
