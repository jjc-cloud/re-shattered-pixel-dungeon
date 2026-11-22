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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

public class Dragonslayer extends MeleeWeapon {

	{
		image = ItemSpriteSheet.DRAGONSLAYER;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1f;

		tier = 5;
	}

	@Override
	protected int baseChargeUse(Hero hero, Char target){
		if (hero.buff(Sword.CleaveTracker.class) != null){
			return 0;
		} else {
			return 1;
		}
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	protected void duelistAbility(Hero hero, Integer target) {
		//+(7+lvl) damage, roughly +40% base dmg, +30% scaling
		int dmgBoost = augment.damageFactor(7 + buffedLvl());
		Sword.cleaveAbility(hero, target, 1, dmgBoost, this);
	}

	@Override
	protected int intrinsicMinDamageBonus(Char owner, int baseMin, int baseMax) {
		if (owner == null || owner.HT <= 0) return 0;
		float missingHealth = Math.max(0f, Math.min(0.8f, (owner.HT - owner.HP) / (float) owner.HT));
		return Math.round((baseMax - baseMin) * missingHealth);
	}

	@Override
	public String abilityInfo() {
		int level = levelKnown ? buffedLvl() : 0;
		int minDamage = damageRollMin(Dungeon.hero, level, false);
		int maxDamage = max(level);
		Augment previewAugment = levelKnown ? augment : Augment.NONE;
		int dmgBoost = previewAugment.damageFactor(7 + level);
		return Messages.get(this, levelKnown ? "ability_desc" : "typical_ability_desc",
				previewAugment.damageFactor(Math.min(minDamage, maxDamage)) + dmgBoost,
				previewAugment.damageFactor(Math.max(minDamage, maxDamage)) + dmgBoost);
	}

	@Override
	public String upgradeAbilityStat(int level){
		int dmgBoost = augment.damageFactor(7 + level);
		int minDamage = damageRollMin(Dungeon.hero, level, false);
		int maxDamage = max(level);
		return (augment.damageFactor(Math.min(minDamage, maxDamage)) + dmgBoost) + "-"
				+ (augment.damageFactor(Math.max(minDamage, maxDamage)) + dmgBoost);
	}

}
