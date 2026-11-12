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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.FrozenAttack;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

public class Skyfall extends MagicMissileWeapon {

	{
		image = ItemSpriteSheet.SKYFALL;
		tier = 4;
		sticky = false;
	}

	@Override
	public int max(int lvl) {
		return 16 + tier*lvl;
	}

	@Override
	protected void onThrow(int cell) {
		int origin = FrozenAttack.origin(curUser);
		ArrayList<Lightning.Arc> arcs = new ArrayList<>();
		if (cell != origin) {
			arcs.add(new Lightning.Arc(DungeonTilemap.raisedTileCenterToWorld(origin),
					DungeonTilemap.raisedTileCenterToWorld(cell)));
		}

		ArrayList<Char> directHits = new ArrayList<>();
		boolean hitWater = false;
		if (flight != null) {
			int end = flight.path.indexOf(cell);
			for (int i = 1; i <= end; i++) {
				Char target = Actor.findChar(flight.path.get(i));
				if (target != null && target != curUser) {
					directHits.add(target);
					hitWater |= Dungeon.level.water[target.pos];
				}
			}
		}

		ArrayList<Char> affected = new ArrayList<>(directHits);
		if (!directHits.isEmpty()) {
			WandOfLightning.ArcChain chain = new WandOfLightning.ArcChain(curUser, affected, arcs);
			for (Char target : directHits) chain.arc(target);
			chain.finish();
			affected.removeAll(directHits);
			for (Char target : affected.toArray(new Char[0])) {
				if (!target.isAlive() || target.alignment == curUser.alignment
						|| target.buff(WandOfLightning.LightningCharge.class) != null) {
					affected.remove(target);
				}
			}
		}

		if (!arcs.isEmpty()) {
			curUser.sprite.parent.addToFront(new Lightning(arcs, null));
			Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
		}
		for (Char target : directHits) {
			if (target.isAlive()) target.damage(damageRoll(curUser), this, curUser);
		}
		if (!affected.isEmpty()) {
			float multiplier = WandOfLightning.ArcChain.damageMultiplier(directHits.size() + affected.size(), hitWater);
			for (Char target : affected) {
				if (target.isAlive()) target.damage(Math.round(damageRoll(curUser) * multiplier), this, curUser);
			}
		}
		Invisibility.dispel();
		rangedHit(null, cell);
	}
}
