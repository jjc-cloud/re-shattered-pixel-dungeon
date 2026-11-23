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

package com.shatteredpixel.shatteredpixeldungeon.items.trinkets;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

public class TrapMechanism extends Trinket {

	{
		image = ItemSpriteSheet.TRAP_MECHANISM;
	}

	@Override
	public String statsDesc() {
		if (isIdentified()){
			return Messages.get(this, "stats_desc", (int)(100*overrideNormalLevelChance(buffedLvl())), (int)(100*revealHiddenTrapChance(buffedLvl())));
		} else {
			return Messages.get(this, "typical_stats_desc", (int)(100*overrideNormalLevelChance(0)), (int)(100*revealHiddenTrapChance(0)));
		}
	}

	public static float overrideNormalLevelChance(){
		return overrideNormalLevelChance(trinketLevel(TrapMechanism.class));
	}

	public static float overrideNormalLevelChance( int level ){
		if (level == -1){
			return 0f;
		} else {
			return 0.25f + 0.25f*level;
		}
	}

	public static float revealHiddenTrapChance(){
		return revealHiddenTrapChance(trinketLevel(TrapMechanism.class));
	}

	public static float revealHiddenTrapChance( int level ){
		if (level == -1){
			return 0f;
		} else {
			return 0.1f + 0.1f*level;
		}
	}

	public static Level.Feeling getNextFeeling(){
		if (Dungeon.hero.belongings.getItem(TrapMechanism.class) == null) {
			return Level.Feeling.NONE;
		}
		if (Dungeon.branch == 0 && Dungeon.depth >= 11 && Dungeon.depth <= 14) {
			return Level.Feeling.TRAPS;
		}
		return Random.Int(2) == 0 ? Level.Feeling.TRAPS : Level.Feeling.CHASM;
	}

}
