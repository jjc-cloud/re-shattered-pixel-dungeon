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

import static com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfSpellpower.scaleEffect;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ShieldBuff;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Random;

public class Blocking extends Weapon.Enchantment {
	
	private static ItemSprite.Glowing BLUE = new ItemSprite.Glowing( 0x0000FF );
	
	@Override
	public int proc(Weapon weapon, Char attacker, Char defender, int damage) {
		
		int level = Math.max( 0, weapon.buffedLvl() );

		// lvl 0 - 10%
		// lvl 1 ~ 12%
		// lvl 2 ~ 14%
		float procChance = (level+4f)/(level+40f) * procChanceMultiplier(attacker);
		if (Random.Float() < procChance){
			float powerMulti = Math.max(1f, procChance);

			int shield = Math.round(scaleEffect(attacker, powerMulti * (2 + weapon.buffedLvl())));
			if (grantShield(attacker, shield) && attacker.sprite != null) {
				attacker.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 5);
			}
		}
		
		return damage;
	}

	public static boolean grantShield(Char target, int amount) {
		BlockBuff shield = target.buff(BlockBuff.class);
		int current = shield == null ? 0 : shield.shielding();
		int gained = Math.min(amount, maxShielding(target) - current);
		if (gained <= 0) return false;

		if (shield == null) shield = Buff.affect(target, BlockBuff.class);
		shield.incShield(gained);
		if (target.sprite != null) {
			target.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(gained), FloatingText.SHIELDING);
		}
		return true;
	}

	private static int maxShielding(Char target) {
		return (int) (target.HT * 3L / 5);
	}
	
	@Override
	public ItemSprite.Glowing glowing() {
		return BLUE;
	}

	public static class BlockBuff extends ShieldBuff {

		{
			type = buffType.POSITIVE;

			shieldUsePriority = -1;
		}

		public void trimToMaxHealth() {
			int excess = shielding() - maxShielding(target);
			if (excess > 0) absorbDamage(excess);
		}

		@Override
		public void fx(boolean on) {
			if (on) {
				target.sprite.add(CharSprite.State.SHIELDED);
			} else if (target.buff(Barrier.class) == null) {
				target.sprite.remove(CharSprite.State.SHIELDED);
			}
		}

		@Override
		public int icon() {
			return BuffIndicator.ARMOR;
		}
		
		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.5f, 1f, 2f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", shielding());
		}
	}
}
