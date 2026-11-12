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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PinCushion;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
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
	protected void onThrow(int cell) {
		if (flight != null) {
			int end = flight.path.indexOf(cell);
			for (int i = 1; i <= end; i++) {
				int pathCell = flight.path.get(i);
				Heap heap = Dungeon.level.heaps.get(pathCell);
				if (heap != null) heap.burn();
				if (Dungeon.level.water[pathCell]) Blob.seed(pathCell, 0, Fire.class);
			}
		}

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
	protected void rangedHit(Char enemy, int cell) {
		super.rangedHit(enemy, cell);
		PinCushion stuck = enemy.buff(PinCushion.class);
		if (stuck != null && stuck.hasStuckItem(SunfireJavelin.class)) {
			Buff.affect(enemy, Burning.class).reignite(enemy);
		}
	}
}
