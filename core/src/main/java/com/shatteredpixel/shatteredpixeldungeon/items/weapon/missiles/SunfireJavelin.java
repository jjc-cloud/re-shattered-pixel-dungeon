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
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PinCushion;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.FrozenAttack;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.watabou.utils.PathFinder;

public class SunfireJavelin extends MagicMissileWeapon {

	{
		image = ItemSpriteSheet.SUNFIRE_JAVELIN;
		tier = 5;
	}

	@Override
	public int max(int lvl) {
		return 20 + tier*lvl;
	}

	@Override
	public Class<?> damageType() {
		return Fire.class;
	}

	@Override
	protected MissileSprite.FlightProgress flightProgress(int landingCell) {
		Ballistica path = flight;
		int end = path == null ? -1 : path.path.indexOf(landingCell);
		return new MissileSprite.FlightProgress() {
			private int next = 1;

			@Override
			public void update(float progress) {
				int reached = (int) (end * progress);
				while (next <= reached) {
					int cell = path.path.get(next++);
					Heap heap = Dungeon.level.heaps.get(cell);
					if (heap != null) heap.burn();
					if (Dungeon.level.water[cell]) Blob.seed(cell, 0, Fire.class);
				}
				if (progress >= 1f && flight == path) flight = null;
			}
		};
	}

	@Override
	protected void onThrow(int cell) {
		Char target = Actor.findChar(cell);
		if ((target == null || target == curUser && !FrozenAttack.resolving())
				&& !Dungeon.level.pit[cell]) {
			decrementDurability();
		}

		if (flight != null) flightProgress(cell).update(1f);

		super.onThrow(cell);
		for (int offset : PathFinder.NEIGHBOURS9) {
			int fireCell = cell + offset;
			if (Dungeon.level.insideMap(fireCell) && !Dungeon.level.solid[fireCell]) {
				if (Dungeon.level.water[fireCell]) Blob.seed(fireCell, 0, Fire.class);
				GameScene.add(Blob.seed(fireCell, 3, Fire.class));
			}
		}
	}

	@Override
	protected void rangedMiss(int cell) {
		decrementDurability();
		super.rangedMiss(cell);
	}

	@Override
	protected void rangedHit(Char enemy, int cell) {
		boolean returning = rangedHit(enemy, cell, true);
		PinCushion stuck = enemy.buff(PinCushion.class);
		if ((returning && enemy.isActive() && enemy.alignment != Char.Alignment.ALLY)
				|| (stuck != null && stuck.hasStuckItem(SunfireJavelin.class))) {
			Buff.affect(enemy, Burning.class).reignite(enemy);
		}
	}
}
