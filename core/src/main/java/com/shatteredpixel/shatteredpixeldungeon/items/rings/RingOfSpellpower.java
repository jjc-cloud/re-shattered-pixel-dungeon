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

package com.shatteredpixel.shatteredpixeldungeon.items.rings;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

public class RingOfSpellpower extends Ring {

	private static final double POWER_PER_LEVEL = 1.15;

	{
		icon = ItemSpriteSheet.Icons.RING_SPELLPOWER;
		buffClass = Spellpower.class;
	}

	@Override
	public String statsInfo() {
		if (isIdentified()){
			String info = Messages.get(this, "stats",
					Messages.decimalFormat("#.##", 100f * (Math.pow(POWER_PER_LEVEL, soloBuffedBonus()) - 1f)));
			if (isEquipped(Dungeon.hero) && soloBuffedBonus() != combinedBuffedBonus(Dungeon.hero)){
				info += "\n\n" + Messages.get(this, "combined_stats",
						Messages.decimalFormat("#.##", 100f * (Math.pow(POWER_PER_LEVEL, combinedBuffedBonus(Dungeon.hero)) - 1f)));
			}
			return info;
		} else {
			return Messages.get(this, "typical_stats", Messages.decimalFormat("#.##", 100f * (POWER_PER_LEVEL - 1)));
		}
	}

	@Override
	public String upgradeStat1(int level){
		int bonusLevel = effectiveBonus(level, visiblyCursed());
		return Messages.decimalFormat("#.##", 100f * (Math.pow(POWER_PER_LEVEL, bonusLevel)-1f)) + "%";
	}

	@Override
	protected RingBuff buff( ) {
		return new Spellpower();
	}

	public static double powerMultiplier(Char target){
		return target == null ? 1 : Math.pow(POWER_PER_LEVEL, getBuffedBonus(target, Spellpower.class));
	}

	public class Spellpower extends RingBuff {
	}
}
