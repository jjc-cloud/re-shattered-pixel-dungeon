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

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.sprites.TorturerSprite;

/**
 * A rare prison guard which never runs out of chains. Its chains ignore terrain and
 * anything standing in the way, so it can drag in anyone it can see, but pulled
 * enemies are not crippled.
 *
 * Its stats and armor drop are identical to a regular guard. It is guaranteed to
 * drop the ethereal chains, as long as they haven't dropped anywhere in this run yet.
 */
public class Torturer extends Guard {

	{
		spriteClass = TorturerSprite.class;
	}

	//拷问官的锁链可以无限使用
	@Override
	protected boolean chainsAvailable(){
		return true;
	}

	//拷问官不看弹道，链子直接朝敌人的方向甩出，不受墙壁与挡路怪物影响
	@Override
	protected Ballistica chainsBallistica( int target ){
		return new Ballistica(pos, target, Ballistica.WONT_STOP);
	}

	@Override
	protected boolean chainsReachTarget( Ballistica chain, int target ){
		return true;
	}

	//只要看得见就能拉到身前
	@Override
	protected boolean chainInRange( int cell ){
		return fieldOfView[cell];
	}

	//拷问官不会让被拉过来的敌人残废
	@Override
	protected boolean cripplesTarget(){
		return false;
	}

	//锁链无限次，必须每回合最多拉一次，否则同一回合内会反复触发拉拽
	@Override
	protected void onChainUsed(){
		spend( TICK );
	}

	//拷问官必定掉落虚空锁链，与普通守卫共用"一局一次"的限制
	@Override
	protected void rollForChainsDrop(){
		MasterThievesArmband.StolenTracker stolen = buff(MasterThievesArmband.StolenTracker.class);
		if (Dungeon.hero.lvl <= maxLvl + 2
				&& !Dungeon.LimitedDrops.GUARD_CHAINS.dropped()
				&& (stolen == null || !stolen.itemWasStolen())) {
			Dungeon.LimitedDrops.GUARD_CHAINS.drop();
			dropChains();
		}
	}
}
