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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.levels.CellContent;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.TenguDartTrap;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

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
		Ballistica incoming = flight;
		flight = null;
		Char center = Actor.findChar(cell);
		super.onThrow(cell);
		WandOfBlastWave.BlastWave.blast(cell);
		Sample.INSTANCE.play(Assets.Sounds.BLAST);

		ArrayList<Integer> affectedCells = new ArrayList<>(PathFinder.NEIGHBOURS9.length);
		for (int offset : PathFinder.NEIGHBOURS9) {
			int affected = cell + offset;
			if (Dungeon.level.insideMap(affected)) affectedCells.add(affected);
		}
		if (Dungeon.level.interactions().affectCells(affectedCells,
				CellContent.BREAKABLE | CellContent.SHOCKWAVE_BREAKABLE)) {
			Dungeon.observe();
		}

		for (int affected : affectedCells) {
			if (!(Dungeon.level.traps.get(affected) instanceof TenguDartTrap)) {
				Dungeon.level.pressCell(affected);
			}
		}

		for (int offset : PathFinder.NEIGHBOURS8) {
			int neighbor = cell + offset;
			if (!Dungeon.level.insideMap(neighbor)) continue;
			Char target = Actor.findChar(neighbor);
			if (target != null && (target.isAlive() || target.flying || !Dungeon.level.pit[neighbor])) {
				Ballistica push = new Ballistica(neighbor, neighbor + offset, Ballistica.MAGIC_BOLT);
				WandOfBlastWave.throwChar(target, push, 2, false, true, this);
			}
		}

		if (center != null && center.pos == cell && Actor.findChar(cell) == center
				&& (center.isAlive() || center.flying || !Dungeon.level.pit[cell]) && incoming != null) {
			int impactIndex = incoming.path.indexOf(cell);
			if (impactIndex >= 0 && impactIndex + 1 < incoming.path.size()) {
				Ballistica push = new Ballistica(cell, incoming.path.get(impactIndex + 1), Ballistica.MAGIC_BOLT);
				WandOfBlastWave.throwChar(center, push, 2, false, true, this);
			}
		}
	}

	@Override
	protected void rangedMiss(int cell) {
		rangedHit(null, cell);
	}
}
