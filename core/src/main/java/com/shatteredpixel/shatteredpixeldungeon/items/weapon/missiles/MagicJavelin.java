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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;

public class MagicJavelin extends MissileWeapon {

	{
		image = ItemSpriteSheet.MAGIC_JAVELIN;
		hitSound = Assets.Sounds.HIT_MAGIC;
		tier = 4;
		sticky = false;
	}

	@Override
	public int max(int lvl) {
		return 16 + tier*lvl;
	}

	@Override
	public int STRReq(int lvl) {
		return 0;
	}

	@Override
	protected float adjacentAccFactor(Char owner, Char target) {
		return owner instanceof Hero && ((Hero) owner).heroClass == HeroClass.HUNTRESS ? 1.5f : 1f;
	}

	@Override
	public float durabilityPerUse(int level) {
		return 0;
	}

	@Override
	protected void rangedHit(Char enemy, int cell) {
		parent = null;
		extraThrownLeft = false;
		if (!spawnedForEffect) {
			if (collect(curUser.belongings.backpack)) {
				GameScene.pickUp(this, cell);
				Sample.INSTANCE.play(Assets.Sounds.ITEM);
			} else {
				Dungeon.level.drop(this, curUser.pos).sprite.drop();
			}
		}
	}

	@Override
	protected void rangedMiss(int cell) {
		parent = null;
	}
}
