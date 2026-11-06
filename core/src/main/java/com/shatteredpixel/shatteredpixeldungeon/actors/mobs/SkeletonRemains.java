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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.SkeletonRemainsSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Collection;

//伪装成普通遗骸的骷髅。受到伤害或手动打开时苏醒，并掉出原遗骸中的物品。
public class SkeletonRemains extends Skeleton {

	{
		spriteClass = SkeletonRemainsSprite.class;

		//潜伏时保持中立，不会主动行动
		alignment = Alignment.NEUTRAL;
		state = PASSIVE;

		//不受等级限制，经验与掉落始终生效（同 VaultSkeleton）
		maxLvl = Hero.MAX_LEVEL;
	}

	public ArrayList<Item> items;

	private boolean ambush = false;

	private static final String ITEMS = "items";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		if (items != null) bundle.put( ITEMS, items );
	}

	@SuppressWarnings("unchecked")
	@Override
	public void restoreFromBundle( Bundle bundle ) {
		if (bundle.contains( ITEMS )) {
			items = new ArrayList<>((Collection<Item>) ((Collection<?>) bundle.getCollection(ITEMS)));
		}
		super.restoreFromBundle( bundle );
		//Mob 不保存 alignment，读档由构造器回到 NEUTRAL；已苏醒的必须恢复为敌对
		if (state != PASSIVE && alignment == Alignment.NEUTRAL) {
			alignment = Alignment.ENEMY;
		}
	}

	@Override
	public boolean add( Buff buff ) {
		if (super.add( buff )) {
			if (buff.type == Buff.buffType.NEGATIVE && alignment == Alignment.NEUTRAL) {
				reveal();
			}
			return true;
		}
		return false;
	}

	@Override
	public String name() {
		if (alignment == Alignment.NEUTRAL) {
			return Messages.get( Heap.class, "skeleton" );
		}
		return super.name();
	}

	@Override
	public String description() {
		if (alignment == Alignment.NEUTRAL) {
			return Messages.get( Heap.class, "skeleton_desc" );
		}
		return super.description();
	}

	@Override
	public CharSprite sprite() {
		SkeletonRemainsSprite sprite = (SkeletonRemainsSprite) super.sprite();
		if (alignment == Alignment.NEUTRAL) sprite.showRemains();
		return sprite;
	}

	@Override
	protected boolean act() {
		if (alignment == Alignment.NEUTRAL && state != PASSIVE) {
			reveal();
		}
		return super.act();
	}

	@Override
	public void beckon( int cell ) {
		//潜伏时不会被声响惊动
		if (alignment != Alignment.NEUTRAL) {
			super.beckon( cell );
		}
	}

	@Override
	public boolean interact( Char c ) {
		if (alignment != Alignment.NEUTRAL || c != Dungeon.hero) {
			return super.interact( c );
		}

		//手动打开遗骸：骷髅先手攻击一次
		reveal();
		Dungeon.hero.busy();
		Dungeon.hero.sprite.operate( pos );
		ambush = true;
		boolean finishedImmediately = doAttack( Dungeon.hero );
		if (finishedImmediately) {
			ambush = false;
			if (Dungeon.hero.isAlive()) {
				Dungeon.hero.spendAndNext( 1f );
			}
		}
		return finishedImmediately;
	}

	@Override
	public void onAttackComplete() {
		super.onAttackComplete();
		if (ambush) {
			ambush = false;
			if (Dungeon.hero.isAlive()) {
				Dungeon.hero.spendAndNext( 1f );
			}
			enemySeen = true;
		}
	}

	@Override
	public void damage( int dmg, Object src ) {
		reveal();
		super.damage( dmg, src );
	}

	@Override
	public void die( Object cause ) {
		reveal();
		super.die( cause );
	}

	private void reveal() {
		if (alignment != Alignment.NEUTRAL) return;

		alignment = Alignment.ENEMY;
		state = HUNTING;
		if (Dungeon.hero != null) {
			enemy = Dungeon.hero;
			target = Dungeon.hero.pos;
		}
		enemySeen = true;

		if (sprite instanceof SkeletonRemainsSprite) {
			((SkeletonRemainsSprite) sprite).showSkeleton();
		}

		dropItems();

		if (Actor.chars().contains( this ) && Dungeon.level.heroFOV[pos]) {
			GLog.w( Messages.get( this, "reveal" ) );
			CellEmitter.get( pos ).burst( Speck.factory( Speck.BONE ), 10 );
			Sample.INSTANCE.play( Assets.Sounds.BONES );
		}
	}

	private void dropItems() {
		if (items == null) return;

		for (Item item : items) {
			Dungeon.level.drop( item, pos ).sprite.drop();
		}
		items = null;
	}

	@Override
	public void rollToDropLoot() {
		dropItems();
		super.rollToDropLoot();
	}
}
