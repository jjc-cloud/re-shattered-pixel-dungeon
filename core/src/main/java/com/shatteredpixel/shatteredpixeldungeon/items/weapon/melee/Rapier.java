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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.FrozenAttack;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

public class Rapier extends MeleeWeapon {

	{
		image = ItemSpriteSheet.RAPIER;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1.3f;

		tier = 1;

		bones = false;
	}

	@Override
	public int max(int lvl) {
		return  4*(tier+1) +    //8 base, down from 10
				lvl*(tier+1);   //scaling unchanged
	}

	@Override
	public int defenseFactor( Char owner ) {
		return 1;	//1 extra defence
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	protected void duelistAbility(Hero hero, Integer target) {
		//+(5+1.5*lvl) damage, roughly +111% base damage, +100% scaling
		int dmgBoost =  augment.damageFactor(5 + Math.round(1.5f*buffedLvl()));
		swordWaveAbility(hero, target, 1, dmgBoost, this);
	}

	@Override
	public String abilityInfo() {
		int dmgBoost = levelKnown ? 5 + Math.round(1.5f*buffedLvl()) : 5;
		if (levelKnown){
			return Messages.get(this, "ability_desc", augment.damageFactor(min()+dmgBoost), augment.damageFactor(max()+dmgBoost));
		} else {
			return Messages.get(this, "typical_ability_desc", min(0)+dmgBoost, max(0)+dmgBoost);
		}
	}

	public String upgradeAbilityStat(int level){
		int dmgBoost = 5 + Math.round(1.5f*level);
		return augment.damageFactor(min(level)+dmgBoost) + "-" + augment.damageFactor(max(level)+dmgBoost);
	}

	public static void swordWaveAbility(Hero hero, Integer target, float dmgMulti, int dmgBoost, MeleeWeapon wep){
		if (target == null){
			return;
		}

		Char selected = Actor.findChar(target);
		if (target == hero.pos
				|| (selected != null && (!selected.isAlive() || hero.isCharmedBy(selected)))) {
			GLog.w(Messages.get(wep, "ability_no_target"));
			return;
		}

		boolean ranged = selected == null || !Dungeon.level.adjacent(hero.pos, target);
		Ballistica shot = ranged ? Ballistica.projectile(hero, hero.pos, target,
				Ballistica.PROJECTILE | Ballistica.STOP_PROJECTILE_COVER) : null;
		int cell = shot == null ? target : shot.collisionPos;

		hero.busy();
		Callback attack = () -> {
			Char enemy = Actor.findChar(cell);
			wep.beforeAbilityUsed(hero, enemy);
			if (enemy != null && enemy != hero) AttackIndicator.target(enemy);
			boolean delayed = ranged && FrozenAttack.swordWave(hero, cell, dmgMulti, dmgBoost, shot.projectile);
			if (!delayed && enemy != null && enemy != hero && enemy.isAlive()
					&& FrozenAttack.attack(hero, enemy, ranged ? dmgMulti : 1f, ranged ? dmgBoost : 0,
							Char.INFINITE_ACCURACY, 0)) {
				Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
				if (!enemy.isAlive()) wep.onAbilityKill(hero, enemy);
			}
			Invisibility.dispel(hero);
			hero.spendAndNext(hero.attackDelay());
			wep.afterAbilityUsed(hero);
		};
		if (ranged) {
			hero.sprite.zap(target);
			if (FrozenAttack.isFrozen(hero)) {
				attack.call();
			} else {
				Item wave = new Item();
				wave.image = ItemSpriteSheet.SWORD_WAVE;
				((MissileSprite) hero.sprite.parent.recycle(MissileSprite.class)).reset(hero.sprite, cell, wave, attack);
			}
		} else {
			hero.sprite.attack(target, attack);
		}
	}
}
