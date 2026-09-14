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
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.blobs;

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.HashMap;

public class WaterVapor extends Blob implements Hero.Doom {

	private HashMap<Integer, Float> entryTimes = new HashMap<>();

	private HashMap<Integer, Float> entryTimes() {
		if (entryTimes == null) entryTimes = new HashMap<>();
		return entryTimes;
	}

	public void affectOnEntry( Char ch ) {
		entryTimes().put(ch.id(), Actor.now());
		damage(ch);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		//Entry timestamps are turn-local and must never survive a save/load boundary.
		entryTimes().clear();
	}

	private void damage( Char ch ) {
		if (ch.isAlive() && !ch.isImmune(getClass())) {
			//Steam deals twice a normal burning roll, but does not amplify other fire damage.
			ch.damage(2 * Random.NormalIntRange(1, 3 + Dungeon.scalingDepth()/4), this);
		}
	}

	@Override
	protected void evolve() {
		//The first blob tick may happen during the same turn this vapor is created.
		//Count down instead of clearing immediately so the particles remain visible
		//through the following turn.
		for (int y = area.top; y < area.bottom; y++) {
			for (int x = area.left; x < area.right; x++) {
				int cell = x + y * Dungeon.level.width();
				Char ch = Actor.findChar(cell);
				if (cur[cell] == 1 && ch != null
						&& !entryTimes().getOrDefault(ch.id(), Float.NEGATIVE_INFINITY).equals(Actor.now())) {
					damage(ch);
				}
				int duration = Math.max(0, cur[cell] - 1);
				volume += (off[cell] = duration);
			}
		}
		entryTimes().clear();
		Dungeon.observe();
	}

	@Override
	public void use( BlobEmitter emitter ) {
		super.use(emitter);
		emitter.pour(Speck.factory(Speck.STEAM), 0.03f);
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}

	@Override
	public void onDeath() {
		Badges.validateDeathFromFire();
		Dungeon.fail(this);
		GLog.n(Messages.get(this, "ondeath"));
	}
}
