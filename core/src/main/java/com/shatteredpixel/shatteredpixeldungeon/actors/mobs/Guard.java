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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.effects.Chains;
import com.shatteredpixel.shatteredpixeldungeon.effects.Effects;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.EtherealChains;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GuardSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

public class Guard extends Mob {

	//they can only use their chains once
	private boolean chainsUsed = false;

	{
		spriteClass = GuardSprite.class;

		HP = HT = 40;
		defenseSkill = 10;

		EXP = 7;
		maxLvl = 14;

		loot = Generator.Category.ARMOR;
		lootChance = 0.2f; //by default, see lootChance()

		properties.add(Property.UNDEAD);
		
		HUNTING = new Hunting();
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(4, 12);
	}

	//whether this guard still has chains left to throw
	protected boolean chainsAvailable(){
		return !chainsUsed;
	}

	//how the chains travel toward their target. ordinary guards need a clear line of fire,
	//the rare variant ignores terrain on the way and only needs to see its target.
	protected Ballistica chainsBallistica( int target ){
		return new Ballistica(pos, target, Ballistica.PROJECTILE);
	}

	//whether the thrown chains actually land on their target
	protected boolean chainsReachTarget( Ballistica chain, int target ){
		return chain.collisionPos == target;
	}

	//how close a target has to be for the chains to reach it
	protected boolean chainInRange( int cell ){
		return Dungeon.level.distance( pos, cell ) < 5;
	}

	//whether targets pulled in by the chains are also crippled
	protected boolean cripplesTarget(){
		return true;
	}

	//called once the guard commits to pulling a target in with its chains
	protected void onChainUsed(){
		//ordinary guards can only use their chains once, so no extra time is spent here
	}

	protected boolean chain(int target){
		if (!chainsAvailable() || enemy.properties().contains(Property.IMMOVABLE))
			return false;

		Ballistica chain = chainsBallistica(target);

		if (!chainsReachTarget(chain, target)
				|| chain.path.size() < 2
				|| Dungeon.level.pit[chain.path.get(1)])
			return false;
		else {
			int newPos = -1;
			for (int i : chain.subPath(1, chain.dist)){
				//find the closest position to the guard that's open for the target
				if (!Dungeon.level.solid[i] && Actor.findChar(i) == null
						&& (Dungeon.level.openSpace[i] || !Char.hasProp(enemy, Property.LARGE))){
					newPos = i;
					break;
				}
			}

			if (newPos == -1){
				return false;
			} else {
				final int newPosFinal = newPos;
				this.target = newPos;

				if (sprite.visible || enemy.sprite.visible) {
					yell(Messages.get(this, "scorpion"));
					new Item().throwSound();
					Sample.INSTANCE.play(Assets.Sounds.CHAINS);
					sprite.parent.add(new Chains(sprite.center(),
							enemy.sprite.destinationCenter(),
							Effects.Type.CHAIN,
							new Callback() {
						public void call() {
							Actor.add(new Pushing(enemy, enemy.pos, newPosFinal, new Callback() {
								public void call() {
									pullEnemy(enemy, newPosFinal);
								}
							}));
							next();
						}
					}));
				} else {
					pullEnemy(enemy, newPos);
				}
			}
		}
		onChainUsed();
		chainsUsed = true;
		return true;
	}

	protected void pullEnemy( Char enemy, int pullPos ){
		enemy.pos = pullPos;
		enemy.sprite.place(pullPos);
		Dungeon.level.occupyCell(enemy);
		if (cripplesTarget()) {
			Cripple.prolong(enemy, Cripple.class, 4f);
		}
		if (enemy == Dungeon.hero) {
			Dungeon.hero.interrupt();
			Dungeon.observe();
			GameScene.updateFog();
		} else {
			enemy.sprite.visible = Dungeon.level.heroFOV[pullPos];
		}
	}

	@Override
	public int attackSkill( Char target ) {
		return 12;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 7);
	}

	@Override
	public float lootChance() {
		//each drop makes future drops 1/3 as likely
		// so loot chance looks like: 1/5, 1/15, 1/45, 1/135, etc.
		return super.lootChance() * (float)Math.pow(1/3f, Dungeon.LimitedDrops.GUARD_ARM.count);
	}

	@Override
	public void rollToDropLoot() {
		rollForChainsDrop();

		super.rollToDropLoot();
	}

	//chance for the one-time ethereal chains drop, this replaces the armor drop for that kill
	protected void rollForChainsDrop(){
		MasterThievesArmband.StolenTracker stolen = buff(MasterThievesArmband.StolenTracker.class);
		if (Dungeon.hero.lvl <= maxLvl + 2
				&& !Dungeon.LimitedDrops.GUARD_CHAINS.dropped()
				&& (stolen == null || !stolen.itemWasStolen())) {
			float armorChance = lootChance;
			lootChance = 0.03f;
			if (Random.Float() < super.lootChance()) {
				Dungeon.LimitedDrops.GUARD_CHAINS.drop();
				dropChains();
			}
			lootChance = armorChance;
		}
	}

	//places the dropped chains on the floor
	protected void dropChains(){
		Dungeon.level.drop(Generator.random(EtherealChains.class), pos).sprite.drop();
	}

	@Override
	public Item createLoot() {
		Dungeon.LimitedDrops.GUARD_ARM.count++;
		return super.createLoot();
	}

	private final String CHAINSUSED = "chainsused";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(CHAINSUSED, chainsUsed);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		chainsUsed = bundle.getBoolean(CHAINSUSED);
	}
	
	protected class Hunting extends Mob.Hunting{
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			enemySeen = enemyInFOV;
			
			if (chainsAvailable()
					&& enemyInFOV
					&& !isCharmedBy( enemy )
					&& !canAttack( enemy )
					&& chainInRange( enemy.pos )
					&& chain(enemy.pos)){
				return !(sprite.visible || enemy.sprite.visible);
			} else {
				return super.act( enemyInFOV, justAlerted );
			}
			
		}
	}
}
