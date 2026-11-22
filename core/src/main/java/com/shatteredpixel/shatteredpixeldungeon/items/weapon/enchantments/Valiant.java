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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.Bundle;

public class Valiant extends Weapon.Enchantment {

	private static final ItemSprite.Glowing GOLD = new ItemSprite.Glowing(0xFFB340);

	public static int minDamageBonus(Char owner, int baseMin, int level, boolean consumeBoosts) {
		if (owner.HT <= 0 || owner.HP >= owner.HT || baseMin <= 0) return 0;

		float missingHealth = Math.min(1f, (owner.HT - owner.HP) / (float) owner.HT);
		float power = genericProcChanceMultiplier(owner, consumeBoosts);
		StrikePowerTracker strike = owner.buff(StrikePowerTracker.class);
		if (strike != null) power += strike.bonus;
		float bonus = missingHealth * (0.5f + 0.2f * level) * power;
		float cap = (0.4f + 0.02f * level) * (1f + (power - 1f) / 2f);
		return Math.round(baseMin * Math.max(0f, Math.min(bonus, cap)));
	}

	@Override
	public int proc(Weapon weapon, Char attacker, Char defender, int damage) {
		return damage;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return GOLD;
	}

	public static class StrikePowerTracker extends Buff implements Buff.AttackSnapshot {
		public float bonus;

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("bonus", bonus);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			bonus = bundle.getFloat("bonus");
		}
	}
}
