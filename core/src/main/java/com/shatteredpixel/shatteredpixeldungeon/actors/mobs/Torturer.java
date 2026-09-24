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
import com.shatteredpixel.shatteredpixeldungeon.effects.Effects;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.sprites.TorturerSprite;

/**
 * A rare prison guard which never runs out of chains. Its chains ignore terrain and
 * anything standing in the way, so it can drag in anyone it can see, but pulled
 * enemies are not crippled.
 *
 * Like an ordinary guard it pulls a target in front of itself and then immediately
 * strikes it: pulling costs no time of its own, so the guard's attack still lands on
 * the same turn, and the chains are only bound by "once per turn" rather than once
 * per guard. Fleeing targets therefore keep getting dragged back.
 *
 * Its stats and armor drop are identical to a regular guard. It is guaranteed to
 * drop the ethereal chains, as long as they haven't dropped anywhere in this run yet.
 */
public class Torturer extends Guard {

	//每回合最多甩一次锁链
	private boolean chainedThisTurn = false;

	{
		spriteClass = TorturerSprite.class;
	}

	//拷问官的锁链可以无限使用，只是同一个回合内不会连甩两次
	@Override
	protected boolean chainsAvailable(){
		return !chainedThisTurn;
	}

	//普通守卫靠 chainsUsed 永久锁住"第二次甩链"，拷问官没有那个标记。
	//甩完锁链不花时间（和守卫一样，这样拉过来之后能立刻接上攻击），
	//所以这里用"时间是否被推进"当作回合结束的判定，下个回合锁链重新可用。
	@Override
	protected void spend( float time ){
		super.spend( time );
		chainedThisTurn = false;
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

	//锁链本身不额外花时间，避免抢走"拉至身前后立刻攻击"的那次出手
	@Override
	protected void onChainUsed(){
		chainedThisTurn = true;
	}

	//拷问官用的是虚空锁链，拉拽沿用神器那套特效
	@Override
	protected Effects.Type chainEffect(){
		return Effects.Type.ETHEREAL_CHAIN;
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
