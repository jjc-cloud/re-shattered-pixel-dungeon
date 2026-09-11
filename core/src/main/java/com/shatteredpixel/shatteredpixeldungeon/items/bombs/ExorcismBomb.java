/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.shatteredpixel.shatteredpixeldungeon.items.bombs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;

public class ExorcismBomb extends Bomb {

	private static final int RANGE = 2;
	private static final float DURATION = 30f;

	{
		image = ItemSpriteSheet.EXORCISM_BOMB;
	}

	@Override
	protected int explosionRange() {
		return RANGE;
	}

	@Override
	public void explode(int cell) {
		super.explode(cell);
		PathFinder.buildDistanceMap(cell, BArray.not(Dungeon.level.solid, null), RANGE);
		for (int i = 0; i < PathFinder.distance.length; i++) {
			Char ch = Actor.findChar(i);
			if (PathFinder.distance[i] != Integer.MAX_VALUE && ch != null && ch.isAlive()) {
				Buff.prolong(ch, MagicImmune.class, DURATION);
			}
		}
	}
}
