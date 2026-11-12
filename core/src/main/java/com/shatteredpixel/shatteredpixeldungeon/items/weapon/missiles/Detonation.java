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
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.PathFinder;

public class Detonation extends MagicMissileWeapon {

	{
		image = ItemSpriteSheet.DETONATION;
		tier = 2;
		sticky = false;
	}

	@Override
	public int max(int lvl) {
		return 6 + tier*lvl;
	}

	@Override
	protected void onThrow(int cell) {
		super.onThrow(cell);
		for (int offset : PathFinder.NEIGHBOURS8) {
			int neighbor = cell + offset;
			if (!Dungeon.level.insideMap(neighbor)) continue;
			Char target = Actor.findChar(neighbor);
			if (target != null) {
				Ballistica push = new Ballistica(neighbor, neighbor + offset, Ballistica.MAGIC_BOLT);
				WandOfBlastWave.throwChar(target, push, 2, false, false, this);
			}
		}
	}

	@Override
	protected void rangedMiss(int cell) {
		rangedHit(null, cell);
	}
}
