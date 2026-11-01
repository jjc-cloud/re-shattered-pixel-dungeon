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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfSharpshooting;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

public class Tomahawk extends MissileWeapon {

	{
		image = ItemSpriteSheet.TOMAHAWK;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 0.9f;

		tier = 4;
		baseUses = 5;
	}

	@Override
	public int min(int lvl) {
		return  Math.round(1.5f * tier) +   //6 base, down from 8
				lvl;                        //scaling unchanged
	}
	
	@Override
	public int max(int lvl) {
		return  Math.round(4f * tier) +     //16 base, down from 20
				(tier-1)*lvl;               //3 scaling, down from 4
	}

	public float minBleed(){
		return minBleed(buffedLvl() + RingOfSharpshooting.levelDamageBonus(Dungeon.hero));
	}

	public float minBleed(int lvl){
		return 6 + lvl;
	}

	public float maxBleed(){
		return maxBleed(buffedLvl() + RingOfSharpshooting.levelDamageBonus(Dungeon.hero));
	}

	public float maxBleed(int lvl){
		return 9 + 3*lvl;
	}
	
	@Override
	public int proc( Char attacker, Char defender, int damage ) {
		Buff.affect( defender, Bleeding.class ).set( augment.damageFactor(Random.NormalFloat(minBleed(), maxBleed())) );
		return super.proc( attacker, defender, damage );
	}

	public String statsInfo(){
		if (isIdentified()){
			return Messages.get(this, "stats_desc",
					Math.round(augment.damageFactor(minBleed())),
					Math.round(augment.damageFactor(maxBleed())));
		} else {
			return Messages.get(this, "typical_stats_desc",
					Math.round(augment.damageFactor(minBleed(0))),
					Math.round(augment.damageFactor(maxBleed(0))));
		}
	}

}
