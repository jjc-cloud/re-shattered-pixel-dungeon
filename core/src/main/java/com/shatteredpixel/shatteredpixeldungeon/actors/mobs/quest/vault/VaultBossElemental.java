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

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.TerrainPropagation;
import com.shatteredpixel.shatteredpixeldungeon.levels.TerrainInteractions;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp;
import com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Electricity;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Dread;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Feint;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicSealDomain;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PinCushion;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.AntiMagic;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Shocking;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.VaultBossElementalSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameMath;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class VaultBossElemental extends Mob {

	{
		HP = HT = 200;
		spriteClass = VaultBossElementalSprite.class;

		EXP = 30;
		defenseSkill = 20;

		properties.add(Property.BOSS);
	}

	public VaultBossElemental(){
		super();
		form = ElementalForm.values()[Random.Int(3)];
	}

	@Override
	public int damageRoll() {
		//frost form does less melee damage, as you're meant to fight it up-close
		if (form == ElementalForm.FROST){
			return Random.NormalIntRange( 15, 20 );
		} else {
			return Random.NormalIntRange( 20, 25 );
		}
	}

	@Override
	public int attackSkill( Char target ) {
		return 28;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 10);
	}

	@Override
	public int defenseSkill(Char enemy) {
		if (form == ElementalForm.FIRE
				&& enemy instanceof Hero
				&& ((Hero) enemy).belongings.attackingWeapon() instanceof MissileWeapon
				&& !Dungeon.level.adjacent(pos, enemy.pos)){
			//always gets hit by a thrown weapon attack when not adjacent
			return 0;
		} else if (form == ElementalForm.FROST
				&& enemy instanceof Hero
				&& ((Hero) enemy).belongings.attackingWeapon() instanceof MeleeWeapon) {
			//halved evasion in frost form vs. melee attacks
			return super.defenseSkill(enemy)/2;
		} else {
			return super.defenseSkill(enemy);
		}
	}

	public enum ElementalForm {
		FIRE,
		FROST,
		SHOCK,
		UNSTABLE //currently unused
	}
	private ElementalForm form = null; //only initially

	protected int envAttackCooldown = Random.NormalIntRange( 10, 15 );
	protected int spAttackCooldown = Random.NormalIntRange( 6, 10 );
	protected int spTargetCell = -1;

	private boolean coordinating;
	private int orbitDirection = Random.Int(2) == 0 ? 1 : -1;
	private float orbitRadius = -1;

	protected int lastEnemyPos = -1; //used for tracking targeting on some attacks

	public ElementalForm curForm(){
		return form;
	}

	public void setElementalForm( ElementalForm form ){
		if (sprite == null) {
			this.form = form;
			return;
		}
		// Initialize the fixed element and its particle effects.
		Buff.affect(this, PinCushionRemover.class).preferGrouping = this.form == ElementalForm.FIRE;

		this.form = form;
		boolean wasTurned = sprite.flipHorizontal;

		((VaultBossElementalSprite)sprite).updateForm();
		AttackIndicator.target(this);
		Emitter e = sprite.emitter();
		//centered a bit, but not totally
		e.fillTarget = false;
		e.pos(sprite, 4, 4, 24, 24);
		if (form == ElementalForm.FIRE){
			e.burst(FlameParticle.FACTORY, 50);
			Sample.INSTANCE.play(Assets.Sounds.BURNING, 2f);

			for (Buff b : buffs()){
				if (b instanceof Chill || b instanceof Frost){
					b.detach();
				}
			}
		} else if (form == ElementalForm.FROST){
			e.burst(MagicMissile.MagicParticle.FACTORY, 50);
			Sample.INSTANCE.play(Assets.Sounds.SHATTER, 2f);

			for (Buff b : buffs()){
				if (b instanceof Burning){
					b.detach();
				}
			}
		} else if (form == ElementalForm.SHOCK){
			e.burst(SparkParticle.FACTORY, 50);
			Sample.INSTANCE.play(Assets.Sounds.LIGHTNING, 2f);
		}

		//don't want to follow through now that form changed, so force a new sp attack instead
		if (spTargetCell != -1){
			spTargetCell = -1;
			spAttackCooldown = 0;
		}

		//significantly reduce environment attack cooldown
		envAttackCooldown /= 2;

		sprite.flipHorizontal = wasTurned;
		BossHealthBar.assignBoss(this, true);
		BossHealthBar.bleed(this, HP < HT/2);
	}

	@Override
	public void aggro(Char ch) {
		super.aggro(ch);
		enemySeen = true; //to prevent opening surprise attack
	}

	@Override
	protected boolean act() {
		BossHealthBar.assignBoss(this);
		BossHealthBar.bleed(this, HP < HT/2);
		if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()){
			fieldOfView = new boolean[Dungeon.level.length()];
		}
		Dungeon.level.updateFieldOfView( this, fieldOfView );

		ArrayList<VaultBossElemental> partners = new ArrayList<>();
		for (Mob mob : Dungeon.level.mobs) {
			if (mob != this && mob instanceof VaultBossElemental && mob.isAlive()) {
				partners.add((VaultBossElemental) mob);
			}
		}
		boolean coordinate = !partners.isEmpty() && alignment == Alignment.ENEMY;
		if (coordinating && !coordinate) {
			// Discard global tracking when the last elemental returns to normal perception.
			enemy = null;
			enemySeen = false;
			target = -1;
			path = null;
			state = WANDERING;
			orbitRadius = -1;
		}
		coordinating = coordinate;
		if (paralysed > 0 || buff(Terror.class) != null || buff(Dread.class) != null
				|| buff(Feint.AfterImage.FeintConfusion.class) != null) {
			return super.act();
		}
		if (coordinate && Dungeon.hero.isAlive() && Dungeon.hero.invisible <= 0
				&& !isCharmedBy(Dungeon.hero) && state != FLEEING) {
			// Like DM300's beckoning, position tracking does not grant line of sight.
			enemy = Dungeon.hero;
			target = enemy.pos;
			state = HUNTING;
		} else {
			enemy = chooseEnemy();
		}
		boolean enemyVisible = enemy != null && enemy.isAlive() && fieldOfView[enemy.pos]
				&& enemy.invisible <= 0 && !isCharmedBy(enemy);
		enemySeen = enemyVisible;
		// Greater elementals always detect visible targets, including the last surviving elemental.
		if (enemyVisible && (state == WANDERING || state == SLEEPING || state == INVESTIGATING)) {
			aggro(enemy);
			target = enemy.pos;
			notice();
		}
		if (coordinate && enemyVisible && (orbitRadius <= 0 || lastEnemyPos != enemy.pos)) {
			// Circle immediately at the observed range, rather than first closing to four tiles.
			orbitRadius = Math.max(4, Dungeon.level.trueDistance(pos, enemy.pos));
		}

		// Once telegraphed, the attack remains committed to its original warning cells.
		if (spTargetCell != -1 && paralysed == 0){
			lastEnemyPos = enemyVisible ? enemy.pos : -1;
			if (sprite != null && (sprite.visible
					|| (enemy != null && enemy.sprite != null && enemy.sprite.visible))) {
				sprite.zap(spTargetCell);
				return false;
			} else {
				zap();
				return true;
			}
		}

		spAttackCooldown--;
		envAttackCooldown--;
		if (state == HUNTING && paralysed == 0 && enemyVisible){
			if (spAttackCooldown <= 0){
				spend(GameMath.gate(attackDelay(), (int)Math.ceil(Dungeon.hero.cooldown()), 3*attackDelay()));
				if (form == ElementalForm.FIRE){
					setupFireBall(enemy);
				} else if (form == ElementalForm.FROST){
					setupFrostCone(enemy);
				} else if (form == ElementalForm.SHOCK){
					setupLightningBolt(enemy);
				}

				Dungeon.hero.interrupt();
				lastEnemyPos = enemy.pos;
				return true;
			} else if (envAttackCooldown <= 0){
				spend(TICK);
				if (form == ElementalForm.FIRE){
					setupFireWall();
				} else if (form == ElementalForm.FROST){
					setupFrostVortex();
				} else if (form == ElementalForm.SHOCK){
					setupLightningChase();
				}
				//not an actual attack, do nothing
				sprite.operate(enemy.pos);
				envAttackCooldown = Random.NormalIntRange( 10, 15 );
				//shock form gets faster abilities
				if (form == ElementalForm.SHOCK){
					envAttackCooldown = (int) (envAttackCooldown*0.67f);
				}

				Dungeon.hero.interrupt();
				lastEnemyPos = enemy.pos;
				return true;
			}
		}

		// Ready skills above always take precedence over spacing movement.
		if (coordinate && state == HUNTING && enemy == Dungeon.hero
				&& Dungeon.hero.isAlive() && Dungeon.hero.invisible <= 0 && paralysed == 0
				&& !isCharmedBy(enemy)) {
			boolean[] spacingPassable = Dungeon.level.passable.clone();
			int heroDistance = Dungeon.level.distance(pos, enemy.pos);
			for (int cell = 0; cell < spacingPassable.length; cell++) {
				if (!spacingPassable[cell]) continue;
				// Do not tighten an existing gap; allow gradual escape if already too close.
				if (Dungeon.level.distance(cell, enemy.pos) < Math.min(4, heroDistance)) {
					spacingPassable[cell] = false;
					continue;
				}
				for (VaultBossElemental partner : partners) {
					if (Dungeon.level.distance(cell, partner.pos)
							< Math.min(3, Dungeon.level.distance(pos, partner.pos))) {
						spacingPassable[cell] = false;
						break;
					}
				}
			}
			for (Char ch : Actor.chars()) spacingPassable[ch.pos] = false;
			PathFinder.buildDistanceMap(pos, spacingPassable);
			int destination = pos;
			boolean orbiting = enemyVisible && heroDistance >= 4;
			if (orbiting) {
				Point heroPos = Dungeon.level.cellToPoint(enemy.pos);
				float currentAngle = PointF.angle(heroPos, Dungeon.level.cellToPoint(pos));
				float advance = 1.5f / orbitRadius;
				// If terrain or a partner blocks this direction, try the other direction.
				for (int attempt = 0; attempt < 2 && destination == pos; attempt++) {
					float bestScore = Float.POSITIVE_INFINITY;
					for (int cell = 0; cell < spacingPassable.length; cell++) {
						if (!spacingPassable[cell] || PathFinder.distance[cell] == Integer.MAX_VALUE) continue;
						float turn = PointF.angle(heroPos, Dungeon.level.cellToPoint(cell)) - currentAngle;
						if (turn > PointF.PI) turn -= PointF.PI2;
						if (turn < -PointF.PI) turn += PointF.PI2;
						turn *= orbitDirection;
						if (turn <= 0 || turn > PointF.PI / 2) continue;
						float score = Math.abs(turn - advance) * orbitRadius
								+ 2 * Math.abs(Dungeon.level.trueDistance(cell, enemy.pos) - orbitRadius)
								+ 0.25f * PathFinder.distance[cell];
						for (VaultBossElemental partner : partners) {
							score += 10 * Math.max(0, 3 - Dungeon.level.distance(cell, partner.pos));
						}
						if (score < bestScore) {
							destination = cell;
							bestScore = score;
						}
					}
					if (destination == pos) orbitDirection = -orbitDirection;
				}
			} else {
				// Too close: retreat first. Unseen: follow the known position until sight returns.
				int bestPenalty = Math.abs(heroDistance - 4);
				for (VaultBossElemental partner : partners) {
					bestPenalty += Math.max(0, 3 - Dungeon.level.distance(pos, partner.pos));
				}
				int bestTravel = 0;
				for (int cell = 0; cell < spacingPassable.length; cell++) {
					if (!spacingPassable[cell] || PathFinder.distance[cell] == Integer.MAX_VALUE) continue;
					int penalty = Math.abs(Dungeon.level.distance(cell, enemy.pos) - 4);
					for (VaultBossElemental partner : partners) {
						penalty += Math.max(0, 3 - Dungeon.level.distance(cell, partner.pos));
					}
					if (penalty < bestPenalty || (penalty == bestPenalty && PathFinder.distance[cell] < bestTravel)) {
						destination = cell;
						bestPenalty = penalty;
						bestTravel = PathFinder.distance[cell];
					}
				}
			}

			lastEnemyPos = enemyVisible ? enemy.pos : -1;
			if (!rooted && destination != pos) {
				int step = Dungeon.findStep(this, destination, spacingPassable, fieldOfView, true);
				if (step != -1 && Actor.findChar(step) == null) {
					int oldPos = pos;
					move(step);
					spend(1 / speed());
					return moveSprite(oldPos, pos);
				}
			}
			spend(TICK);
			return true;
		}


		AiState lastState = state;
		boolean result = super.act();

		//if state changed from wandering to hunting, we haven't acted yet, don't update.
		if (!(lastState == WANDERING && state == HUNTING)) {
			if (enemy != null) {
				lastEnemyPos = enemy.pos;
			} else {
				lastEnemyPos = Dungeon.hero.pos;
			}
		}

		return result;
	}

	protected void zap() {
		spend( Actor.TICK );

		Invisibility.dispel(this);
		if (form == ElementalForm.FIRE){
			doFireBall(spTargetCell);
		} else if (form == ElementalForm.FROST){
			doFrostCone(spTargetCell);
		} else if (form == ElementalForm.SHOCK){
			doLightningBolt(spTargetCell);
		}

		spAttackCooldown = Random.NormalIntRange( 6, 10 );
		//shock form gets faster abilities
		if (form == ElementalForm.SHOCK){
			spAttackCooldown = (int) (spAttackCooldown*0.67f);
		}
		spTargetCell = -1;
	}

	public void onZapComplete() {
		zap();
		next();
	}

	@Override
	public int defenseProc(Char enemy, int damage) {
		if (form == ElementalForm.SHOCK && enemy == Dungeon.hero && !(Dungeon.hero.belongings.attackingWeapon() instanceof MissileWeapon)){
			enemy.sprite.parent.addToFront( new Lightning( sprite.center(), enemy.sprite.center(), null ) );
			TerrainPropagation.point(Dungeon.level, TerrainInteractions.Source.ELECTRIC, enemy.pos,
					enemy, this, target -> {
				if (target instanceof VaultBossElemental) return;
				if (target == Dungeon.hero) {
					if (Dungeon.hero.vaultElementalControlled && Dungeon.hero.vaultElementalControlHits >= 3) return;
					Dungeon.hero.vaultElementalDamage = true;
				}
				target.damage( Random.IntRange(5, 10), new Shocking() , VaultBossElemental.this);
				Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
				PixelScene.shake( 2, 0.3f );
				target.sprite.centerEmitter().burst(SparkParticle.FACTORY, 3);
				target.sprite.flash();
				if (!target.isAlive()){
					Badges.validateDeathFromEnemyMagic();
					Dungeon.fail(VaultBossElemental.this);
				} else {
					GLog.w(Messages.get(VaultBossElemental.this, "shock_resist"));
				}
			});
		}
		return super.defenseProc(enemy, damage);
	}

	private boolean weakAnnounced = false;

	@Override
	public void damage(int dmg, Object src) {
		//fire form is resistant to magic and weak to thrown weapons
		if (form == ElementalForm.FIRE){
			if (AntiMagic.RESISTS.contains(damageSourceClass(src))){
				dmg /= 4;
				//prompts faster attacks, only do this if it's from the hero
				if (src instanceof Wand || src instanceof ClericSpell){
					GLog.w(Messages.get(this, "fire_resist"));
					spAttackCooldown -= 3;
					envAttackCooldown -= 5;
				}
			} else if (src == Dungeon.hero && Dungeon.hero.belongings.attackingWeapon() instanceof MissileWeapon){
				if (!weakAnnounced){
					GLog.p(Messages.get(this, "fire_weak"));
					weakAnnounced = true;
				}
				Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
				dmg += 5;
			}
		//frost form is resistant to thrown weapons and weak to melee (only from the hero though!)
		} else if ( form == ElementalForm.FROST ){
			if (src == Dungeon.hero && Dungeon.hero.belongings.attackingWeapon() instanceof MissileWeapon){
				GLog.w(Messages.get(this, "frost_resist"));
				//penalty is that the weapon sticks
				dmg /= 4;
			} else if (src == Dungeon.hero && !(Dungeon.hero.belongings.attackingWeapon() instanceof MissileWeapon)){
				if (!weakAnnounced){
					GLog.p(Messages.get(this, "frost_weak"));
					weakAnnounced = true;
				}
				Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
				dmg += 5;
			}
		//shock form is resistant to melee and weak to magic
		} else if ( form == ElementalForm.SHOCK ){
			if (AntiMagic.RESISTS.contains(damageSourceClass(src))){
				if (!weakAnnounced){
					GLog.p(Messages.get(this, "shock_weak"));
					weakAnnounced = true;
				}
				//all wands get a little charge
				if (src instanceof Wand || src instanceof ClericSpell){
					Dungeon.hero.belongings.charge(0.2f);
				}
				Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
				dmg += 5;
			} else if (src instanceof Char && !(src == Dungeon.hero && Dungeon.hero.belongings.attackingWeapon() instanceof MissileWeapon)){
				//resisted text already in defenseproc, as well as shock penalty (only for the hero)
				dmg /= 4;
			}
		}

		int preHP = HP;
		super.damage(dmg, src);
		BossHealthBar.bleed(this, HP < HT/2);
		int dmgTaken = preHP - HP;
		if (dmgTaken > 0) {
			envAttackCooldown -= dmgTaken/24f;
			spAttackCooldown -= dmgTaken/12f;
		}
	}

	@Override
	public void die(Object cause) {
		super.die(cause);
		for (Mob mob : Dungeon.level.mobs) {
			if (mob instanceof VaultBossElemental && mob.isAlive()) {
				BossHealthBar.assignBoss(mob);
				return;
			}
		}
		boolean remindStatue = false;
		if (Dungeon.level instanceof VaultLevel && ((VaultLevel) Dungeon.level).isRegularQuest()) {
			Buff.detach(Dungeon.hero, MagicSealDomain.class);
		}
		if (Dungeon.level instanceof VaultLevel && ((VaultLevel) Dungeon.level).isRegularQuest()
				&& !Imp.Quest.vaultBossesDefeated) {
			remindStatue = true;
			Imp.Quest.vaultBossesDefeated = true;
			Statistics.questScores[3] += 2000;
		}
		Dungeon.level.unseal();
		GameScene.bossSlain();
		if (remindStatue && Dungeon.hero.isAlive()) {
			Dungeon.hero.interrupt();
			GLog.n(Messages.get(Imp.class, "vault_bosses_defeated"));
		}
	}

	@Override
	public CharSprite sprite() {
		if (form == null){
			form = ElementalForm.FIRE;
		}
		CharSprite sprite = super.sprite();
		if (form != null) {
			((VaultBossElementalSprite)sprite).setForm(form);
		}
		return sprite;
	}

	@Override
	public boolean add(Buff buff) {
		if (buff instanceof PinCushion && form != ElementalForm.FROST){
			Buff.affect(this, PinCushionRemover.class).preferGrouping = this.form == ElementalForm.FIRE;
		}

		boolean harmful = false;
		if (form == ElementalForm.FIRE){
			harmful = buff instanceof Frost || buff instanceof Chill;
		} else if (form == ElementalForm.FROST){
			harmful = buff instanceof Burning;
		}

		//damaged by these, but much less so than regular elementals
		if (harmful){
			damage( Random.NormalIntRange( 5, 10 ), buff );
			return false;
		}

		return super.add(buff);
	}

	@Override
	public HashSet<Property> properties() {
		HashSet<Property> props = new HashSet<>(properties);
		if (form == ElementalForm.FIRE){
			props.add(Property.FIERY);
		} else if (form == ElementalForm.FROST){
			props.add(Property.ICY);
		} else if (form == ElementalForm.SHOCK){
			props.add(Property.ELECTRIC);
		}
		return props;
	}

	@Override
	public float resist(Class effect) {
		if (form == ElementalForm.SHOCK && effect == WandOfLightning.class){
			return 1; //shock form doesn't resist wand of lightning as it's a wand, and wands are effective vs. shock form
		}
		return super.resist(effect);
	}

	@Override
	public String description() {
		String desc = super.description();
		if (form != null){
			switch (form) {
				default:
				case FIRE:
					return desc + "\n\n" + Messages.get(this, "desc_fire");
				case FROST:
					return desc + "\n\n" + Messages.get(this, "desc_frost");
				case SHOCK:
					return desc + "\n\n" + Messages.get(this, "desc_shock");
			}
		} else {
			return desc;
		}
	}

	private static final String FORM = "elemental_form";

	private static final String ENV_ATK_COOLDOWN = "env_atk_cooldown";
	private static final String SP_ATK_COOLDOWN = "sp_atk_cooldown";
	private static final String SP_TARGET_CELL = "sp_target_cell";

	private static final String LAST_ENEMY_POS = "last_enemy_pos";
	private static final String LIGHTNING_OFS = "lightning_ofs";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(FORM, form);
		bundle.put("coordinating", coordinating);
		bundle.put("orbit_direction", orbitDirection);
		bundle.put("orbit_radius", orbitRadius);

		bundle.put(ENV_ATK_COOLDOWN, envAttackCooldown);
		bundle.put(SP_ATK_COOLDOWN, spAttackCooldown);
		bundle.put(SP_TARGET_CELL, spTargetCell);

		bundle.put(LAST_ENEMY_POS, lastEnemyPos);
		bundle.put(LIGHTNING_OFS, lightningOfs);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		form = bundle.getEnum(FORM, ElementalForm.class);
		coordinating = bundle.getBoolean("coordinating");
		if (bundle.contains("orbit_direction")) orbitDirection = bundle.getInt("orbit_direction");
		orbitRadius = bundle.getFloat("orbit_radius");

		envAttackCooldown = bundle.getInt(ENV_ATK_COOLDOWN);
		spAttackCooldown = bundle.getInt(SP_ATK_COOLDOWN);
		spTargetCell = bundle.getInt(SP_TARGET_CELL);

		lastEnemyPos = bundle.getInt(LAST_ENEMY_POS);
		lightningOfs = bundle.getInt(LIGHTNING_OFS);

		BossHealthBar.assignBoss(this);
		BossHealthBar.bleed(this, HP < HT/2);
	}

	//used to forcefully remove pincushion after its applied
	public static class PinCushionRemover extends Buff{

		{
			actPriority = VFX_PRIO;
		}

		//triggered when elemental is currently in or leaving fire form, making collecting easier
		public boolean preferGrouping = false;

		@Override
		public boolean act() {

			if (target.buff(PinCushion.class) == null){
				detach();
				return true;
			}

			PathFinder.buildDistanceMap(target.pos, Dungeon.level.passable, 2);
			ArrayList<Integer> candidates = new ArrayList<>();
			int furthestDist = 1;
			int closestDist = 100;
			for (int i = 0; i < Dungeon.level.length(); i++){
				if (PathFinder.distance[i] == 2){
					int dist = Dungeon.level.distance(i, Dungeon.hero.pos);
					if (dist > 1) {
						if (dist < closestDist){
							closestDist = dist;
						} if (dist > furthestDist){
							furthestDist = dist;
						}
						candidates.add(i);
					}
				}
			}

			//prevent overlap if hero is close
			if (Math.abs(furthestDist - closestDist) <= 1){
				furthestDist++;
				closestDist--;
			}

			ArrayList<Integer> existingStacks = new ArrayList<>();

			for (int i : candidates.toArray(new Integer[0])){
				int dist = Dungeon.level.distance(i, Dungeon.hero.pos);
				if (dist <= closestDist || dist >= furthestDist){
					candidates.remove((Integer)i);
				} else {
					if (Dungeon.level.heaps.get(i) != null){
						existingStacks.add(i);
					}
				}
			}

			//always place thrown weapons onto each other if possible, but only in fire form
			if (preferGrouping && !existingStacks.isEmpty()) {
				candidates = existingStacks;
			}

			while (target.buff(PinCushion.class) != null) {
				Item item = target.buff(PinCushion.class).grabOne();

				Dungeon.level.drop(item, Random.element(candidates)).sprite.drop(target.pos);
			}
			detach();
			return true;
		}
	}

	/***************************
	 *** Fire Form Abilities ***
	 **************************/

	public void setupFireBall( Char enemy ){
		//aim at direction enemy is moving
		if (Dungeon.level.adjacent(enemy.pos, lastEnemyPos)){
			spTargetCell = enemy.pos + (enemy.pos - lastEnemyPos);
		} else {
			//random otherwise
			spTargetCell = enemy.pos + PathFinder.NEIGHBOURS8[Random.Int(8)];
		}

		//if there's a firewall, fiddle with fireball position so that it cannot be inside a firewall cell
		FireWall wall = buff(FireWall.class);
		if (wall != null){
			int ofs = 0;
			boolean valid;
			do {
				valid = true;
				for (int i : wall.cells){
					if (i == spTargetCell+ofs){
						do {
							ofs = PathFinder.NEIGHBOURS8[Random.Int(4)];
						} while (Actor.findChar(ofs) != null);
						valid = false;
						break;
					}
				}
			} while (!valid);
			spTargetCell += ofs;
		}

		for (int i : PathFinder.NEIGHBOURS9){
			if (!Dungeon.level.solid[spTargetCell+i]) {
				GameScene.targetedCell(spTargetCell + i, cooldown());
			}
		}
	}

	// Reuse Fire's evolution and persistence, with a distinct source for friendly fire.
	public static class ElementalFire extends Fire {}

	public void doFireBall( int cell ){

		Sample.INSTANCE.play(Assets.Sounds.BURNING);
		for (int i : PathFinder.NEIGHBOURS9){
			if (Dungeon.level.solid[cell+i]) {
				continue;
			}

			CellEmitter.get(cell+i).burst(FlameParticle.FACTORY, 10);
			GameScene.add(Blob.seed(cell+i, 2, ElementalFire.class));

			Char ch = Actor.findChar(cell+i);
			if (ch != null && !(ch instanceof VaultBossElemental)){
				//at this depth fire deals ~32 damage, already loads
				Buff.affect(ch, Burning.class).reignite(ch);
				if (ch == Dungeon.hero){
					Statistics.questScores[3] -= 100;
				}
			}
		}

	}

	public void setupFireWall(){
		FireWall wall = Buff.append(this, FireWall.class);
		Room r = ((RegularLevel)Dungeon.level).room(pos);
		int minX = r.left + 1, maxX = r.right - 1;
		int minY = r.top + 1, maxY = r.bottom - 1;
		int i = 0;

		ArrayList<Integer> wallDistances = new ArrayList<>();

		//0,1,2,3 for left,top,right,bottom
		Point heroPos = Dungeon.level.cellToPoint(Dungeon.hero.pos);
		wallDistances.add(0, heroPos.x - minX);
		wallDistances.add(1, heroPos.y - minY);
		wallDistances.add(2, maxX - heroPos.x);
		wallDistances.add(3, maxY - heroPos.y);

		ArrayList<Integer> sortedDistances = (ArrayList<Integer>) wallDistances.clone();
		Collections.shuffle(sortedDistances);
		Collections.sort(sortedDistances);

		int wallFrom = 0;
		int minSkipDist, maxSkipDist;

		// Keep the original directions and gaps, sized for the entire fixed arena.
		if (HP >= HT/2){
			minSkipDist = 1;
			maxSkipDist = 3;
			do {
				wallFrom = Random.Int(4);
			} while (wallDistances.get(wallFrom) < sortedDistances.get(1));
		//otherwise always pick between 2nd and 3rd furthest
		} else {
			minSkipDist = 4;
			maxSkipDist = 6;
			//in the specific cases of 2x2 walls being equidistant (or all 4 walls equidistant) just pick a random wall
			if (sortedDistances.get(0).equals(sortedDistances.get(1))
					&& sortedDistances.get(2).equals(sortedDistances.get(3))){
				wallFrom = Random.Int(4);
			} else{
				do {
					wallFrom = Random.Int(4);
				} while (wallDistances.get(wallFrom) < sortedDistances.get(1)
						|| wallDistances.get(wallFrom) > sortedDistances.get(2));
			}
		}

		if (wallFrom == 1 || wallFrom == 3){
			wall.cells = new int[maxX - minX];
			wall.left = maxY - minY - 1;
			int y;
			if (wallFrom == 1){
				y = minY;
				wall.direction = Dungeon.level.width();
			} else {
				y = maxY;
				wall.direction = -Dungeon.level.width();
			}
			int skip;
			do {
				skip = Random.IntRange(minX, maxX);
			} while (Math.abs(skip - heroPos.x) > maxSkipDist || Math.abs(skip - heroPos.x) < minSkipDist);
			for (int x = minX; x <= maxX; x++){
				if (x == skip) continue;
				wall.cells[i] = x + (y*Dungeon.level.width());
				i++;
			}
		} else {
			wall.cells = new int[maxY - minY];
			wall.left = maxX - minX - 1;
			int x;
			if (wallFrom == 0){
				x = minX;
				wall.direction = 1;
			} else {
				x = maxX;
				wall.direction = -1;
			}
			int skip;
			do {
				skip = Random.IntRange(minY, maxY);
			} while (Math.abs(skip - heroPos.y) > maxSkipDist || Math.abs(skip - heroPos.y) < minSkipDist);
			for (int y = minY; y <= maxY; y++){
				if (y == skip) continue;
				wall.cells[i] = x + (y*Dungeon.level.width());
				i++;
			}
		}
	}

	public static class FireWall extends Buff {

		private int[] cells = new int[0];
		private int direction;
		private boolean[] entered = new boolean[0];

		private int left; //travel distance set from the arena bounds

		private ArrayList<Emitter> emitters = new ArrayList<>();

		@Override
		public boolean act() {
			if (entered.length != cells.length) entered = new boolean[cells.length];
			boolean active = false;
			for (int i = 0; i < cells.length; i++){
				if (cells[i] == -1) continue;
				for (int j = 0; j < 2; j++) {
					int cell = cells[i] + j * direction;
					if (!Dungeon.level.insideMap(cell)) {
						cells[i] = -1;
						break;
					}
					if (Dungeon.level.solid[cell]) {
						// The bounding rectangle starts outside the curved arena floor.
						// Once a lane enters the floor, every solid tile stops it permanently.
						if (entered[i]) {
							cells[i] = -1;
							break;
						}
						continue;
					}
					entered[i] = true;
					CellEmitter.get(cell).burst(FlameParticle.FACTORY, 10);
					Char ch = Actor.findChar(cell);
					if (ch != null && !(ch instanceof VaultBossElemental)){
						Buff.affect(ch, Burning.class).reignite(ch, 5); //~20 effective damage
						if (ch == Dungeon.hero){
							Sample.INSTANCE.play(Assets.Sounds.BURNING);
							Statistics.questScores[3] -= 100;
						}
					}
				}
				if (cells[i] != -1) {
					cells[i] += direction;
					active = true;
				}
			}

			Sample.INSTANCE.play(Assets.Sounds.BURNING, 0.5f);
			if (left-- <= 0 || !active){
				detach();
			} else {
				updateFX();
			}
			spend(TICK);
			return true;
		}

		private void updateFX(){
			for (Emitter e : emitters) e.on = false;
			emitters.clear();
			if (entered.length != cells.length) entered = new boolean[cells.length];
			for (int i = 0; i < cells.length; i++) {
				int cell = cells[i];
				if (cell == -1) continue;
				boolean oneOpen = false;
				for (int j = 0; j < 2; j++) {
					int next = cell + j * direction;
					if (!Dungeon.level.insideMap(next)) break;
					if (Dungeon.level.solid[next]) {
						if (entered[i] || oneOpen) break;
						continue;
					}
					Emitter pour = CellEmitter.get(next);
					pour.pour(FlameParticle.FACTORY, 0.1f);
					emitters.add(pour);
					oneOpen = true;
				}
				if (!oneOpen && !entered[i]) {
					// Show the first floor tile where this lane will enter the arena.
					for (int j = 1; j <= left; j++) {
						int next = cell + j * direction;
						if (!Dungeon.level.insideMap(next)) break;
						if (!Dungeon.level.solid[next]) {
							Emitter pour = CellEmitter.center(next);
							pour.pour(FlameParticle.FACTORY, 0.5f);
							emitters.add(pour);
							break;
						}
					}
				}
			}
		}

		@Override
		public void fx(boolean on) {
			if (on) {
				updateFX();
			} else {
				for (Emitter e : emitters){
					e.on = false;
				}
				emitters.clear();
			}
		}

		private static String CELLS = "cells";
		private static String DIRECTION = "direction";
		private static String LEFT = "left";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(CELLS, cells);
			bundle.put("entered", entered);
			bundle.put(DIRECTION, direction);
			bundle.put(LEFT, left);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			cells = bundle.getIntArray( CELLS );
			entered = bundle.contains("entered") ? bundle.getBooleanArray("entered") : new boolean[cells.length];
			direction = bundle.getInt( DIRECTION );
			left = bundle.getInt(LEFT);
		}
	}

	/****************************
	 *** Frost Form Abilities ***
	 ***************************/

	public void setupFrostCone( Char enemy ){

		spTargetCell = enemy.pos;
		Ballistica core = new Ballistica(pos, enemy.pos, Ballistica.WONT_STOP);

		Room room = ((RegularLevel)Dungeon.level).room(pos);
		float range = (float)Math.hypot(room.width() - 2, room.height() - 2);
		ConeAOE cone = new ConeAOE(core, range, 50, Ballistica.STOP_SOLID);
		// A grazing cone ray must not include cells sheltered from the cast origin.
		Iterator<Integer> cellIterator = cone.cells.iterator();
		while (cellIterator.hasNext()) {
			int candidate = cellIterator.next();
			if (Dungeon.level.solid[candidate]
					|| new Ballistica(pos, candidate, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID).collisionPos != candidate) {
				cellIterator.remove();
			}
		}
		for (int cell : cone.cells){
			if (Dungeon.level.trueDistance(cell, pos) <= 2){
				GameScene.targetedCell(cell, cooldown());
			}
		}

	}

	public void doFrostCone( int cell ){

		FrostCone cone = Buff.append(this, FrostCone.class);

		Ballistica core = new Ballistica(pos, cell, Ballistica.WONT_STOP);
		Room room = ((RegularLevel)Dungeon.level).room(pos);
		float range = (float)Math.hypot(room.width() - 2, room.height() - 2);
		cone.cells = new ConeAOE(core, range, 50, Ballistica.STOP_SOLID).cells;
		cone.startPos = pos;
		Iterator<Integer> cellIterator = cone.cells.iterator();
		while (cellIterator.hasNext()) {
			int candidate = cellIterator.next();
			if (Dungeon.level.solid[candidate]
					|| new Ballistica(cone.startPos, candidate, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID).collisionPos != candidate) {
				cellIterator.remove();
			}
		}

	}

	//tracker buff to ensure that hero gets a chance to act after freezing
	public static class FrostResist extends Buff{

		{
			actPriority = Actor.BUFF_PRIO-1; //after other buffs
		}

		@Override
		public boolean act() {
			if (target.buff(Frost.class) != null){
				spend(target.cooldown());
				return true;
			} else {
				detach();
				return true;
			}
		}
	}

	public static class FrostCone extends Buff {

		private int startPos;
		private HashSet<Integer> cells = new HashSet<>();
		private int distance = 2;

		private HashSet<Emitter> emitters = new HashSet<>();

		@Override
		public boolean act() {

			// Recheck cover before both visual effects and damage as the cold front advances.
			updateFX();
			for (int cell : cells.toArray(new Integer[0])){
				if (Dungeon.level.trueDistance(cell, startPos) <= distance){
					CellEmitter.get(cell).burst(MagicMissile.WhiteParticle.FACTORY, 10);
					Char ch = Actor.findChar(cell);
					if (ch != null && !(ch instanceof VaultBossElemental) && ch.buff(FrostResist.class) == null
							&& (ch != Dungeon.hero || !Dungeon.hero.vaultElementalControlled
							|| Dungeon.hero.vaultElementalControlHits < 3)){
						int healthBefore = ch.HP;
						int shieldBefore = ch.shielding();
						if (ch == Dungeon.hero) Dungeon.hero.vaultElementalDamage = true;
						ch.damage(Random.NormalIntRange(10, 15), new Frost());
						if (ch != Dungeon.hero || !Dungeon.hero.vaultElementalControlled
								|| Dungeon.hero.vaultElementalControlHits < 3) {
							Buff.affect(ch, Frost.class, 5f);
							Buff.affect(ch, FrostResist.class);
							if (ch == Dungeon.hero && ch.buff(Frost.class) != null
									&& !Dungeon.hero.vaultElementalControlled) {
								Dungeon.hero.vaultElementalControlled = true;
								Dungeon.hero.vaultElementalControlHits = (healthBefore > ch.HP || shieldBefore > ch.shielding()) ? 1 : 0;
							}
						}
						if (ch == Dungeon.hero){
							Statistics.questScores[3] -= 100;
							Sample.INSTANCE.play(Assets.Sounds.SHATTER);
							if (!ch.isAlive()){
								Badges.validateDeathFromEnemyMagic();
								Dungeon.fail(target);
							}
						}
					}
					if (Dungeon.level.trueDistance(cell, startPos) <= distance-4){
						cells.remove(cell);
					}
				}
			}

			distance += 2;

			if (cells.isEmpty()){
				detach();
			} else {
				spend(TICK);
				for (int cell : cells){
					if (Dungeon.level.trueDistance(cell, startPos) <= distance
						&& Dungeon.level.trueDistance(cell, startPos) > distance-2){
						GameScene.targetedCell(cell, cooldown());
					}
				}
			}

			return true;
		}

		private void updateFX(){
			for (Emitter e : emitters){
				e.on = false;
			}
			emitters.clear();

			Iterator<Integer> cellIterator = cells.iterator();
			while (cellIterator.hasNext()) {
				int candidate = cellIterator.next();
				if (Dungeon.level.solid[candidate]
						|| new Ballistica(startPos, candidate, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID).collisionPos != candidate) {
					cellIterator.remove();
				}
			}
			for (int cell : cells) {
				if (Dungeon.level.trueDistance(cell, startPos) <= distance){
						Emitter e = CellEmitter.get(cell);
						e.pour(SnowParticle.FACTORY, 0.1f);
						emitters.add(e);
					}
				}
			}

		@Override
		public void fx(boolean on) {
			if (on) {
				updateFX();
			} else {
				for (Emitter e : emitters){
					e.on = false;
				}
				emitters.clear();
			}
		}

		public static final String START_POS = "start_pos";
		public static final String CELLS = "cells";
		public static final String DISTANCE = "distance";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(START_POS, startPos);

			int[] bundleCells = new int[cells.size()];
			int i = 0;
			for (int cell : cells){
				bundleCells[i] = cell;
				i++;
			}
			bundle.put(CELLS, bundleCells);
			bundle.put(DISTANCE, distance);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			startPos = bundle.getInt(START_POS);
			for (int cell : bundle.getIntArray(CELLS)){
				cells.add(cell);
			}
			distance = bundle.getInt(DISTANCE);
		}

	}

	public void setupFrostVortex(){
		FrostVortex vortex = Buff.append(this, FrostVortex.class);
		vortex.targetCell = Dungeon.hero.pos;
	}

	public static class FrostVortex extends Buff {

		private int targetCell = -1;
		private int distance = -1000;
		private HashMap<Integer, Integer> shadowDirections = new HashMap<>();
		private static final int[] DIR_X = {-1, -1, 1, 1};
		private static final int[] DIR_Y = {-1, 1, 1, -1};

		private boolean fullVortex = false;
		private boolean altDirection = false;

		private ArrayList<Emitter> emitters = new ArrayList<>();

		@Override
		public boolean act() {
			if (distance == -1000){
				fullVortex = target.HP < target.HT/2;
				altDirection = Random.Int(2) == 0;
				Room room = ((RegularLevel)Dungeon.level).room(targetCell);
				Point center = Dungeon.level.cellToPoint(targetCell);
				distance = 0;
				for (int y = room.top + 1; y < room.bottom; y++) {
					for (int x = room.left + 1; x < room.right; x++) {
						int cell = x + y * Dungeon.level.width();
						if (Dungeon.level.solid[cell]) continue;
						for (int i = 0; i < DIR_X.length; i++) {
							boolean enabled = fullVortex || (altDirection ? i % 2 == 0 : i % 2 != 0);
							if (enabled) distance = Math.max(distance, (x - center.x) * DIR_X[i] + (y - center.y) * DIR_Y[i]);
						}
					}
				}
				for (int y = room.top + 1; y < room.bottom; y++) {
					for (int x = room.left + 1; x < room.right; x++) {
						int cell = x + y * Dungeon.level.width();
						if (!Dungeon.level.solid[cell]) continue;
						int width = Dungeon.level.width();
						boolean interiorObstacle = !Dungeon.level.solid[cell - 1] && !Dungeon.level.solid[cell + 1]
								|| !Dungeon.level.solid[cell - width] && !Dungeon.level.solid[cell + width];
						if (!interiorObstacle) continue;
						int directions = 0;
						for (int i = 0; i < DIR_X.length; i++) {
							boolean enabled = fullVortex || (altDirection ? i % 2 == 0 : i % 2 != 0);
							int frontDistance = (x - center.x) * DIR_X[i] + (y - center.y) * DIR_Y[i];
							if (enabled && frontDistance > 0 && frontDistance <= distance) directions |= 1 << i;
						}
						if (directions != 0) shadowDirections.put(cell, directions);
					}
				}
			}

			HashSet<Integer> cells = new HashSet<>();
			if (fullVortex) {
				cells.addAll(getCells(targetCell, distance, -1, -1));
				cells.addAll(getCells(targetCell, distance, -1, 1));
				cells.addAll(getCells(targetCell, distance, 1, 1));
				cells.addAll(getCells(targetCell, distance, 1, -1));
			} else {
				if (altDirection) {
					cells.addAll(getCells(targetCell, distance, -1, -1));
					cells.addAll(getCells(targetCell, distance, 1, 1));
				} else {
					cells.addAll(getCells(targetCell, distance, -1, 1));
					cells.addAll(getCells(targetCell, distance, 1, -1));
				}
			}

			boolean frontExists = !cells.isEmpty();
			for (Iterator<Integer> iterator = cells.iterator(); iterator.hasNext();) {
				int cell = iterator.next();
				boolean sheltered = false;
				for (Map.Entry<Integer, Integer> shadow : shadowDirections.entrySet()) {
					int dx = cell % Dungeon.level.width() - shadow.getKey() % Dungeon.level.width();
					int dy = cell / Dungeon.level.width() - shadow.getKey() / Dungeon.level.width();
					for (int i = 0; i < DIR_X.length; i++) {
						if ((shadow.getValue() & (1 << i)) == 0) continue;
						// A widening wedge behind the actual obstacle, along the incoming front's direction.
						int forward = -dx * DIR_X[i] - dy * DIR_Y[i];
						int sideways = dx * DIR_Y[i] - dy * DIR_X[i];
						if (forward > 0 && Math.abs(sideways) <= forward) {
							sheltered = true;
							break;
						}
					}
					if (sheltered) break;
				}
				if (sheltered) {
					iterator.remove();
				}
			}

			// The wave keeps advancing, while obstacle shadows remain protected.
			distance--;
			updateFX();

			if (!frontExists){
				detach();
				return true;
			} else {
				for (Integer cell : cells){
					CellEmitter.get(cell).burst(MagicMissile.WhiteParticle.FACTORY, 10);
					Char ch = Actor.findChar(cell);
					if (ch != null && !(ch instanceof VaultBossElemental) && ch.buff(FrostResist.class) == null
							&& (ch != Dungeon.hero || !Dungeon.hero.vaultElementalControlled
							|| Dungeon.hero.vaultElementalControlHits < 3)){
						Buff.affect(ch, Frost.class, 5f);
						Buff.affect(ch, FrostResist.class);
						if (ch == Dungeon.hero && ch.buff(Frost.class) != null
								&& !Dungeon.hero.vaultElementalControlled) {
							Dungeon.hero.vaultElementalControlled = true;
							Dungeon.hero.vaultElementalControlHits = 0;
						}
						if (ch == Dungeon.hero){
							Sample.INSTANCE.play(Assets.Sounds.SHATTER);
							Statistics.questScores[3] -= 100;
						}
					}
				}
				Sample.INSTANCE.play(Assets.Sounds.GAS, 0.25f);
				spend(TICK);
				return true;
			}
		}

		private void updateFX(){
			for (Emitter e : emitters){
				e.on = false;
			}
			emitters.clear();

			if (targetCell != -1) {
				HashSet<Integer> cells = new HashSet<>();
				if (fullVortex) {
					cells.addAll(getCells(targetCell, distance, -1, -1));
					cells.addAll(getCells(targetCell, distance, -1, 1));
					cells.addAll(getCells(targetCell, distance, 1, 1));
					cells.addAll(getCells(targetCell, distance, 1, -1));
				} else {
					if (altDirection) {
						cells.addAll(getCells(targetCell, distance, -1, -1));
						cells.addAll(getCells(targetCell, distance, 1, 1));
					} else {
						cells.addAll(getCells(targetCell, distance, -1, 1));
						cells.addAll(getCells(targetCell, distance, 1, -1));
					}
				}

				for (Iterator<Integer> iterator = cells.iterator(); iterator.hasNext();) {
					int cell = iterator.next();
					boolean sheltered = false;
					for (Map.Entry<Integer, Integer> shadow : shadowDirections.entrySet()) {
						int dx = cell % Dungeon.level.width() - shadow.getKey() % Dungeon.level.width();
						int dy = cell / Dungeon.level.width() - shadow.getKey() / Dungeon.level.width();
						for (int i = 0; i < DIR_X.length; i++) {
							if ((shadow.getValue() & (1 << i)) == 0) continue;
							int forward = -dx * DIR_X[i] - dy * DIR_Y[i];
							int sideways = dx * DIR_Y[i] - dy * DIR_X[i];
							if (forward > 0 && Math.abs(sideways) <= forward) {
								sheltered = true;
								break;
							}
						}
						if (sheltered) break;
					}
					if (sheltered) iterator.remove();
				}

				for (Integer cell : cells) {
					Emitter pour = CellEmitter.get(cell);
					pour.pour(SnowParticle.FACTORY, 0.1f);
					emitters.add(pour);
				}
			}
		}

		@Override
		public void fx(boolean on) {
			if (on) {
				updateFX();
			} else {
				for (Emitter e : emitters){
					e.on = false;
				}
				emitters.clear();
			}
		}

		private HashSet<Integer> getCells(int start, int dist, int dirX, int dirY){
			dist = Math.abs(dist);
			HashSet<Integer> cells = new HashSet<>();
			Room room = ((RegularLevel)Dungeon.level).room(start);
			Point center = Dungeon.level.cellToPoint(start);
			// Both flanks continue past a column; its forward shadow remains blocked.
			for (int y = room.top + 1; y < room.bottom; y++) {
				for (int x = room.left + 1; x < room.right; x++) {
					if ((x - center.x) * dirX + (y - center.y) * dirY != dist) continue;
					int cell = x + y * Dungeon.level.width();
					if (Dungeon.level.insideMap(cell) && !Dungeon.level.solid[cell]) cells.add(cell);
				}
			}
			return cells;
		}

		private static String TARGET_CELL = "target_cell";
		private static String DISTANCE = "distance";

		private static String FULL_VORTEX = "full_vortex";
		private static String ALT_DIR = "alt_direction";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(TARGET_CELL, targetCell);
			bundle.put(DISTANCE, distance);
			int[] obstacles = new int[shadowDirections.size()];
			int[] directions = new int[shadowDirections.size()];
			int shadowIndex = 0;
			for (Map.Entry<Integer, Integer> shadow : shadowDirections.entrySet()) {
				obstacles[shadowIndex] = shadow.getKey();
				directions[shadowIndex++] = shadow.getValue();
			}
			bundle.put("shadow_obstacles", obstacles);
			bundle.put("shadow_directions", directions);
			bundle.put(FULL_VORTEX, fullVortex);
			bundle.put(ALT_DIR, altDirection);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			targetCell = bundle.getInt(TARGET_CELL);
			distance = bundle.getInt(DISTANCE);
			if (bundle.contains("shadow_obstacles")) {
				int[] obstacles = bundle.getIntArray("shadow_obstacles");
				int[] directions = bundle.getIntArray("shadow_directions");
				for (int i = 0; i < obstacles.length; i++) shadowDirections.put(obstacles[i], directions[i]);
			}
			fullVortex = bundle.getBoolean(FULL_VORTEX);
			altDirection = bundle.getBoolean(ALT_DIR);
		}

	}

	/****************************
	 *** Shock Form Abilities ***
	 ***************************/

	private int lightningOfs;

	public void setupLightningBolt(Char enemy){

		spTargetCell = enemy.pos;
		lightningOfs = Random.Int(2);

		Ballistica bolt = new Ballistica(pos, spTargetCell, Ballistica.STOP_SOLID);
		for (int cell : bolt.subPath(0, bolt.dist)){
			GameScene.targetedCell(cell, cooldown());
		}

		for (int i = lightningOfs % 2; i < PathFinder.CIRCLE8.length; i+=2){
			GameScene.targetedCell(spTargetCell + PathFinder.CIRCLE8[i], cooldown());
		}

	}

	public void doLightningBolt(int cell){

		HashSet<Integer> affectedCells = new HashSet<>();

		Ballistica bolt = new Ballistica(pos, cell, Ballistica.STOP_SOLID);
		for (int c : bolt.subPath(0, bolt.dist)){
			CellEmitter.get(c).burst(SparkParticle.FACTORY, 5);
			affectedCells.add(c);
		}

		for (int i = lightningOfs % 2; i < PathFinder.CIRCLE8.length; i+=2){
			CellEmitter.get(cell + PathFinder.CIRCLE8[i]).burst(SparkParticle.FACTORY, 5);
			affectedCells.add(cell + PathFinder.CIRCLE8[i]);
		}

		TerrainPropagation propagation = TerrainPropagation.event(Dungeon.level, TerrainInteractions.Source.ELECTRIC);
		if (propagation != null) propagation.extendCells(affectedCells);

		for (int c : affectedCells){
			Char ch = Actor.findChar(c);
			if (ch != null && !(ch instanceof VaultBossElemental) && ch.buff(ShockResist.class) == null
					&& (ch != Dungeon.hero || !Dungeon.hero.vaultElementalControlled
					|| Dungeon.hero.vaultElementalControlHits < 3)){
				int healthBefore = ch.HP;
				int shieldBefore = ch.shielding();
				if (ch == Dungeon.hero) Dungeon.hero.vaultElementalDamage = true;
				ch.damage(Random.NormalIntRange(20, 30), new Electricity());
				if (ch != Dungeon.hero || !Dungeon.hero.vaultElementalControlled
						|| Dungeon.hero.vaultElementalControlHits < 3) {
					Buff.prolong(ch, Paralysis.class, 1f);
					Buff.affect(ch, ShockResist.class);
					if (ch == Dungeon.hero && ch.buff(Paralysis.class) != null
							&& !Dungeon.hero.vaultElementalControlled) {
						Dungeon.hero.vaultElementalControlled = true;
						Dungeon.hero.vaultElementalControlHits = (healthBefore > ch.HP || shieldBefore > ch.shielding()) ? 1 : 0;
					}
				}
				ch.sprite.centerEmitter().burst(SparkParticle.FACTORY, 3);
				ch.sprite.flash();
				if (ch == Dungeon.hero){
					Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
					PixelScene.shake( 2, 0.3f );
					Statistics.questScores[3] -= 100;
					if (!ch.isAlive()){
						Badges.validateDeathFromEnemyMagic();
						Dungeon.fail(this);
					}
				}
			}
		}

		sprite.parent.add(new Lightning(cell + PathFinder.CIRCLE8[lightningOfs],
				cell + PathFinder.CIRCLE8[lightningOfs+4], null));
		sprite.parent.add(new Lightning(cell + PathFinder.CIRCLE8[lightningOfs+2],
				cell + PathFinder.CIRCLE8[lightningOfs+6], null));

	}

	//tracker buff to ensure that hero gets a chance to act after freezing
	public static class ShockResist extends Buff {

		{
			actPriority = Actor.BUFF_PRIO-1; //after other buffs
		}

		@Override
		public boolean act() {
			if (target.buff(Paralysis.class) != null){
				spend(target.cooldown());
				return true;
			} else {
				detach();
				return true;
			}
		}
	}

	//TODO these aren't working great atm, perhaps it's better to use more but always go straight?
	public void setupLightningChase(){
		Room room = ((RegularLevel)Dungeon.level).room(pos);
		Point center = room.center();
		int width = Dungeon.level.width();
		int north = -1, south = -1, west = -1, east = -1;
		for (int y = room.top + 1; y < room.bottom; y++) {
			int cell = center.x + y * width;
			if (!Dungeon.level.solid[cell] && Dungeon.level.passable[cell]) {
				if (north == -1) north = cell;
				south = cell;
			}
		}
		for (int x = room.left + 1; x < room.right; x++) {
			int cell = x + center.y * width;
			if (!Dungeon.level.solid[cell] && Dungeon.level.passable[cell]) {
				if (west == -1) west = cell;
				east = cell;
			}
		}
		boolean alt = Random.Int(2) == 0;
		if (alt || HP < HT/2) {
			if (north != -1) Buff.append(this, LightningChase.class).curCell = north;
			if (south != -1) Buff.append(this, LightningChase.class).curCell = south;
		}
		if (!alt || HP < HT/2) {
			if (west != -1) Buff.append(this, LightningChase.class).curCell = west;
			if (east != -1) Buff.append(this, LightningChase.class).curCell = east;
		}
	}

	public static class LightningChase extends Buff {

		private static float lastSFXTime = -1;

		float direction = -1;

		int curCell = -1;
		int midCell = -1;
		int endCell = -1;

		ArrayList<Emitter> emitters = new ArrayList<>();

		@Override
		public boolean act() {

			PointF curPos;

			if (direction == -1){
				curPos = new PointF(Dungeon.level.cellToPoint(curCell));
				curPos.x += 0.5f;
				curPos.y += 0.5f;

				direction = PointF.angle(curPos, new PointF(Dungeon.level.cellToPoint(Dungeon.hero.pos)));

				//just started, so do initial bolt visuals

				int curCell = Dungeon.level.pointToCell(curPos.floor());
				CellEmitter.get(curCell).burst(SparkParticle.FACTORY, 10);

			} else {

				CellEmitter.get(curCell).burst(SparkParticle.FACTORY, 10);
				Char ch = Actor.findChar(curCell);
				if (ch != null && !(ch instanceof VaultBossElemental) && ch.buff(ShockResist.class) == null){
					shockChar(ch);
				}
				if ( Dungeon.level.insideMap(midCell) && !Dungeon.level.solid[midCell]) {
					CellEmitter.get(midCell).burst(SparkParticle.FACTORY, 10);
					ch = Actor.findChar(midCell);
					if (ch != null && !(ch instanceof VaultBossElemental) && ch.buff(ShockResist.class) == null){
						shockChar(ch);
					}
					if (Dungeon.level.insideMap(endCell) && !Dungeon.level.solid[endCell]) {
						CellEmitter.get(endCell).burst(SparkParticle.FACTORY, 10);
						ch = Actor.findChar(endCell);
						if (ch != null && !(ch instanceof VaultBossElemental) && ch.buff(ShockResist.class) == null){
							shockChar(ch);
						}
					} else {
						detach();
						return true;
					}
				} else {
					detach();
					return true;
				}

				curPos = new PointF(Dungeon.level.cellToPoint(endCell));
				curPos.x += 0.5f;
				curPos.y += 0.5f;

				float targetAngle = PointF.angle(curPos, new PointF(Dungeon.level.cellToPoint(Dungeon.hero.pos)));

				if (Math.abs(direction - targetAngle) > PointF.PI){
					if (direction > targetAngle){
						targetAngle += PointF.PI2;
					} else {
						targetAngle -= PointF.PI2;
					}
				}

				float maxMove = Random.Float(PointF.PI/8, PointF.PI/6);
				if (direction > targetAngle){
					direction -= Math.min(direction - targetAngle, maxMove);
				} else {
					direction += Math.min(targetAngle - direction, maxMove);
				}

				if (direction > PointF.PI) {
					direction -= PointF.PI2;
				} else if (direction < -PointF.PI){
					direction += PointF.PI2;
				}

			}

			PointF endPos = new PointF(curPos);
			endPos.offset(new PointF(curPos).polar(direction, 2));
			//always snap to the middle of the cell
			endPos.x = Math.round(2 * endPos.x) / 2f;
			endPos.y = Math.round(2 * endPos.y) / 2f;

			PointF midPos = PointF.inter(curPos, endPos, 0.5f);

			endCell = Dungeon.level.pointToCell(endPos.floor());
			midCell = Dungeon.level.pointToCell(midPos.floor());
			curCell = Dungeon.level.pointToCell(curPos.floor());

			//prevents many instances from all making their sfx at once
			if (Actor.now() > lastSFXTime) {
				Sample.INSTANCE.play(Assets.Sounds.LIGHTNING, 0.5f);
				lastSFXTime = Actor.now();
			}

			updateFX();

			spend(TICK);
			return true;
		}

		private void shockChar(Char ch){
			if (ch == Dungeon.hero && Dungeon.hero.vaultElementalControlled
					&& Dungeon.hero.vaultElementalControlHits >= 3) return;
			int healthBefore = ch.HP;
			int shieldBefore = ch.shielding();
			if (ch == Dungeon.hero) Dungeon.hero.vaultElementalDamage = true;
			ch.damage(Random.NormalIntRange(10, 15), new Electricity());
			if (ch != Dungeon.hero || !Dungeon.hero.vaultElementalControlled
					|| Dungeon.hero.vaultElementalControlHits < 3) {
				Buff.prolong(ch, Paralysis.class, 1f);
				Buff.affect(ch, ShockResist.class);
				if (ch == Dungeon.hero && ch.buff(Paralysis.class) != null
						&& !Dungeon.hero.vaultElementalControlled) {
					Dungeon.hero.vaultElementalControlled = true;
					Dungeon.hero.vaultElementalControlHits = (healthBefore > ch.HP || shieldBefore > ch.shielding()) ? 1 : 0;
				}
			}
			ch.sprite.centerEmitter().burst(SparkParticle.FACTORY, 3);
			ch.sprite.flash();
			if (ch == Dungeon.hero){
				Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
				PixelScene.shake( 2, 0.3f );
				Statistics.questScores[3] -= 100;
				if (!ch.isAlive()){
					Badges.validateDeathFromEnemyMagic();
					Dungeon.fail(target);
				}
			}
		}

		private void updateFX(){
			for (Emitter e : emitters){
				e.on = false;
			}
			emitters.clear();

			if (endCell != -1 && Dungeon.level.insideMap(endCell) && !Dungeon.level.solid[endCell]){
				Emitter e = CellEmitter.get(endCell);
				e.pour(SparkParticle.STATIC, 0.1f);
				emitters.add(e);
			}

			if (midCell != -1 && Dungeon.level.insideMap(midCell) && !Dungeon.level.solid[midCell]){
				Emitter e = CellEmitter.get(midCell);
				e.pour(SparkParticle.STATIC, 0.1f);
				emitters.add(e);
			}

			if (curCell != -1 && Dungeon.level.insideMap(curCell) && !Dungeon.level.solid[curCell]){
				Emitter e = CellEmitter.get(curCell);
				e.pour(SparkParticle.STATIC, 0.1f);
				emitters.add(e);
			}

		}

		@Override
		public void fx(boolean on) {
			if (on){
				updateFX();
			} else {
				for (Emitter e : emitters){
					e.on = false;
				}
				emitters.clear();
			}
		}

		private static String DIRECTION = "direction";
		private static String CUR_CELL = "cur_cell";
		private static String MID_CELL = "mid_cell";
		private static String END_CELL = "end_cell";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(DIRECTION, direction);
			bundle.put(CUR_CELL, curCell);
			bundle.put(MID_CELL, midCell);
			bundle.put(END_CELL, endCell);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			direction = bundle.getInt(DIRECTION);
			curCell = bundle.getInt(CUR_CELL);
			midCell = bundle.getInt(MID_CELL);
			endCell = bundle.getInt(END_CELL);
			lastSFXTime = -1;
		}
	}

}
