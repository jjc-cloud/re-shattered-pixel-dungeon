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

package com.shatteredpixel.shatteredpixeldungeon.mechanics;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Daze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Preparation;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.WondrousResin;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Flail;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RunicBlade;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sickle;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Comparator;

/** 时停只保存攻击意图和可见特效；恢复后才进行命中、伤害与死亡判定。 */
public class FrozenAttack implements Bundlable {

	private static final int MELEE = 0, THROW = 1, WAND = 2, BOW = 3;
	public static final int CLEAVE = 1, CRUSH = 2, SPIKE = 3;
	private int kind, from, aim, targetID;
	private Item item;
	private KindOfWeapon secondWeapon;
	private int weaponIndex = -1, secondWeaponIndex = -1;
	private float damageMultiplier = 1, damageBonus, accuracyMultiplier = 1;
	private boolean normalAttack, bowSpecial;
	private boolean abilityAttack, invisible;
	private int meleeEffect;
	private ArrayList<Buff> attackBuffs = new ArrayList<>();
	private float bowBonus;
	private int wandLevel;
	private Wand originalWand;
	private int wandIndex = -1;
	private boolean forcedCurse;
	private Class<? extends CursedWand.CursedEffect> curseClass;
	private boolean positiveCurse;
	private CursedWand.CursedEffect curse;
	private Ballistica shot;
	private Ballistica.Projectile projectile;
	private Effects effects;
	private boolean arrived;
	private float arrivalTime;

	// 仅在构造特效或执行一次命中时设置，不能把玩家移回原发射格。
	private static Visual effectSource;
	private static FrozenAttack resolving;

	public static boolean isFrozen(Char user) {
		return user.buff(TimekeepersHourglass.timeFreeze.class) != null
				|| user.buff(Swiftthistle.TimeBubble.class) != null;
	}

	private static void addAttack(Hero hero, FrozenAttack attack) {
		Buff.affect(hero, Queue.class).attacks.add(attack);
		attack.show();
	}

	public static class Queue extends Buff {
		private ArrayList<FrozenAttack> attacks = new ArrayList<>();

		public void restoreAttackVisuals() {
			for (FrozenAttack attack : attacks) attack.show();
		}

		@Override
		public void detach() {
			if (!attacks.isEmpty() && target instanceof Hero) ((Hero) target).busy();
			super.detach();
			FrozenAttack.release(attacks);
			attacks = new ArrayList<>();
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("attacks", attacks);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			for (Bundlable attack : bundle.getCollection("attacks")) attacks.add((FrozenAttack) attack);
		}
	}

	public static Visual visualSource(Char user) {
		return effectSource == null ? user.sprite : effectSource;
	}

	public static boolean resolving() {
		return resolving != null;
	}

	public static int origin(Char user) {
		return resolving != null && user == Dungeon.hero ? resolving.from : user.pos;
	}

	public static boolean wasInvisible(Char user) {
		return resolving != null && user == Dungeon.hero ? resolving.invisible : user.invisible > 0;
	}

	public static boolean overridesBuff(Char user, Class<? extends Buff> type) {
		return resolving != null && user == Dungeon.hero && (type == Preparation.class
				|| type == RunicBlade.RunicSlashTracker.class || type == Sickle.HarvestBleedTracker.class
				|| type == Flail.SpinAbilityTracker.class || type == Talent.PreciseAssaultTracker.class);
	}

	public static <T extends Buff> T attackBuff(Class<T> type) {
		for (Buff buff : resolving.attackBuffs) if (buff.getClass() == type) return type.cast(buff);
		return null;
	}

	public static boolean detachBuff(Buff buff) {
		return resolving != null && resolving.attackBuffs.remove(buff);
	}

	private void captureAttackBuffs(Hero hero) {
		invisible = hero.invisible > 0;
		for (Buff buff : hero.buffs()) {
			Class<? extends Buff> type = buff.getClass();
			if (type == Preparation.class || type == RunicBlade.RunicSlashTracker.class
					|| type == Sickle.HarvestBleedTracker.class || type == Flail.SpinAbilityTracker.class
					|| type == Talent.PreciseAssaultTracker.class) {
				attackBuffs.add(buff);
				buff.detach();
			}
		}
	}

	public static boolean melee(Hero hero, Char target, float damageMultiplier,
	                            float damageBonus, float accuracyMultiplier, boolean normal) {
		return melee(hero, target, damageMultiplier, damageBonus, accuracyMultiplier, normal, 0);
	}

	private static boolean melee(Hero hero, Char target, float damageMultiplier,
	                             float damageBonus, float accuracyMultiplier, boolean normal, int effect) {
		if (!isFrozen(hero) || resolving() || target == null) return false;
		FrozenAttack attack = new FrozenAttack();
		attack.kind = MELEE;
		attack.from = hero.pos;
		attack.targetID = target.id();
		attack.item = hero.belongings.attackingWeapon();
		attack.secondWeapon = hero.belongings.secondWep();
		attack.damageMultiplier = damageMultiplier;
		attack.damageBonus = damageBonus;
		attack.accuracyMultiplier = accuracyMultiplier;
		attack.normalAttack = normal;
		attack.abilityAttack = hero.belongings.abilityWeapon != null;
		attack.meleeEffect = effect;
		attack.captureAttackBuffs(hero);
		addAttack(hero, attack);
		return true;
	}

	public static boolean attack(Hero hero, Char target, float damageMultiplier,
	                             float damageBonus, float accuracyMultiplier, int effect) {
		if (melee(hero, target, damageMultiplier, damageBonus, accuracyMultiplier, false, effect)) return false;
		return hero.attack(target, damageMultiplier, damageBonus, accuracyMultiplier);
	}

	public static void missile(Hero hero, Item missile, int aim) {
		FrozenAttack attack = new FrozenAttack();
		attack.projectile = Ballistica.currentProjectile();
		attack.kind = THROW;
		attack.item = missile;
		attack.from = hero.pos;
		attack.aim = aim;
		attack.captureAttackBuffs(hero);
		addAttack(hero, attack);
	}

	public static void bow(Hero hero, SpiritBow bow, int aim, int count) {
		for (int i = 0; i < count; i++) {
			FrozenAttack attack = new FrozenAttack();
			attack.kind = BOW;
			attack.item = bow.duplicate();
			attack.bowSpecial = bow.sniperSpecial;
			attack.bowBonus = bow.sniperSpecialBonusDamage;
			attack.from = hero.pos;
			attack.aim = aim;
			attack.projectile = i == 0 ? Ballistica.currentProjectile() : new Ballistica.Projectile(hero);
			Ballistica.Projectile previous = Ballistica.setProjectile(attack.projectile);
			try {
				((SpiritBow) attack.item).knockArrow().throwPos(hero, attack.from, aim);
			} finally {
				attack.projectile.finish();
				Ballistica.setProjectile(previous);
			}
			if (i == 0) attack.captureAttackBuffs(hero);
			addAttack(hero, attack);
		}
	}

	public static boolean wand(Hero hero, Wand wand, int aim, Ballistica shot) {
		if (!isFrozen(hero)) return false;
		FrozenAttack attack = new FrozenAttack();
		attack.projectile = shot.projectile;
		attack.kind = WAND;
		attack.item = wand.duplicate();
		attack.originalWand = wand;
		attack.wandLevel = wand.buffedLvl();
		attack.from = hero.pos;
		attack.aim = aim;
		addAttack(hero, attack);
		if (!wand.cursed && Random.Float() < WondrousResin.extraCurseEffectChance()) {
			FrozenAttack extra = new FrozenAttack();
			extra.kind = WAND;
			extra.projectile = Ballistica.projectile(hero, hero.pos, aim,
					Ballistica.MAGIC_BOLT | Ballistica.STOP_PROJECTILE_COVER).projectile;
			extra.item = wand.duplicate();
			extra.wandLevel = attack.wandLevel;
			extra.from = hero.pos;
			extra.aim = aim;
			extra.forcedCurse = extra.positiveCurse = true;
			addAttack(hero, extra);
		}
		return true;
	}

	private static ArrayList<Wand> heldWands() {
		ArrayList<Wand> wands = Dungeon.hero.belongings.getAllItems(Wand.class);
		for (Buff buff : Dungeon.hero.buffs()) {
			if (buff instanceof Wand.Charger) {
				Wand wand = ((Wand.Charger) buff).wand();
				if (!wands.contains(wand)) wands.add(wand);
			}
		}
		return wands;
	}

	/** 场景重建时也从保存的攻击记录恢复真实特效。 */
	public void show() {
		if (kind == MELEE) return;
		Game.runOnRenderThread(() -> {
			if (Dungeon.hero.sprite != null && isFrozen(Dungeon.hero)) {
				buildEffects();
				Dungeon.hero.sprite.parent.add(effects);
			}
		});
	}

	private void buildEffects() {
		if (effects != null) effects.killAndErase();
		effects = new Effects();
		arrived = kind == MELEE;
		if (kind == MELEE) return;
		Hero hero = Dungeon.hero;
		Item previousItem = Item.currentItem();
		item.setCurrent(hero);
		Image source = new Image();
		PointF center = DungeonTilemap.raisedTileCenterToWorld(from);
		source.width = source.height = 1;
		source.point(new PointF(center.x - 0.5f, center.y - 0.5f));
		source.parent = effects;
		Callback arrival = () -> arrived = true;
		effectSource = source;
		Ballistica.Projectile previousProjectile = Ballistica.setProjectile(projectile);
		try {
			if (kind == WAND) {
				Wand wand = (Wand) item;
				wand.frozenLevel = wandLevel;
				wand.tryToZap(hero, aim);
				if (originalWand == null && wandIndex >= 0) {
					ArrayList<Wand> wands = heldWands();
					if (wandIndex < wands.size() && wands.get(wandIndex).getClass() == wand.getClass()) originalWand = wands.get(wandIndex);
				}
				shot = wand.prepareZap(from, aim);
				if (wand.cursed || forcedCurse) {
					shot = new Ballistica(from, aim, Ballistica.MAGIC_BOLT | Ballistica.STOP_PROJECTILE_COVER
							| Ballistica.IRON_DOOR_CHANCE);
					if (curse == null) {
						if (curseClass == null) {
							positiveCurse = forcedCurse || Random.Float() < WondrousResin.positiveCurseEffectChance();
							curse = CursedWand.randomValidEffect(wand, hero, shot, positiveCurse);
							curseClass = curse.getClass();
						} else {
							curse = Reflection.newInstance(curseClass);
						}
					}
					curse.FX(wand, hero, shot, arrival);
				} else {
					wand.fx(shot, arrival);
				}
			} else {
				Item projectile = item;
				if (kind == BOW) {
					SpiritBow bow = (SpiritBow) item;
					bow.sniperSpecial = bowSpecial;
					bow.sniperSpecialBonusDamage = bowBonus;
					projectile = bow.knockArrow();
				}
				int cell = projectile.throwPos(hero, from, aim);
				shot = new Ballistica(from, aim, Ballistica.WONT_STOP);
				shot.collisionPos = cell;
				shot.dist = shot.path.indexOf(cell);
				MissileSprite missile = (MissileSprite) effects.recycle(MissileSprite.class);
				missile.reset(source, shot.collisionPos, projectile, arrival);
			}
			arrivalTime = effects.travelTime();
			// 飞弹粒子需要一次极短的更新才能成形，随后连同位移、旋转一起冻结。
			if (!arrived) {
				float elapsed = Game.elapsed;
				try {
					Game.elapsed = Math.min(0.02f, arrivalTime / 4);
					effects.advance();
					arrivalTime -= Game.elapsed;
				} finally {
					Game.elapsed = elapsed;
				}
			}
		} finally {
			if (projectile != null) projectile.finish();
			Ballistica.setProjectile(previousProjectile);
			effectSource = null;
			if (previousItem != null) previousItem.setCurrent(hero);
		}
	}

	private void resolve() {
		Hero hero = Dungeon.hero;
		resolving = this;
		for (Buff buff : attackBuffs) buff.target = hero;
		try {
			if (kind == MELEE) {
				Char target = (Char) Actor.findById(targetID);
				if (target == null || !target.isAlive()) return;
				ArrayList<KindOfWeapon> weapons = hero.belongings.getAllItems(KindOfWeapon.class);
				if (weaponIndex >= 0 && weaponIndex < weapons.size() && item != null
						&& weapons.get(weaponIndex).getClass() == item.getClass()) item = weapons.get(weaponIndex);
				if (secondWeaponIndex >= 0 && secondWeaponIndex < weapons.size() && secondWeapon != null
						&& weapons.get(secondWeaponIndex).getClass() == secondWeapon.getClass()) secondWeapon = weapons.get(secondWeaponIndex);
				KindOfWeapon weapon = hero.belongings.weapon, second = hero.belongings.secondWep;
				KindOfWeapon ability = hero.belongings.abilityWeapon;
				try {
					hero.belongings.weapon = (KindOfWeapon) item;
					hero.belongings.secondWep = secondWeapon;
					hero.belongings.abilityWeapon = abilityAttack ? (KindOfWeapon) item : null;
					if (normalAttack) hero.resolveMeleeAttack(target);
					else {
						int oldPos = target.pos;
						boolean hit = hero.attack(target, damageMultiplier, damageBonus, accuracyMultiplier);
						if (hit && abilityAttack) {
							Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
							if (!target.isAlive()) {
								MeleeWeapon.onAbilityKill(hero, target);
								if (meleeEffect == CLEAVE) {
									if (hero.buff(Sword.CleaveTracker.class) != null) hero.buff(Sword.CleaveTracker.class).detach();
									else Buff.prolong(hero, Sword.CleaveTracker.class, 4f);
								}
							} else if (meleeEffect == CRUSH) {
								Buff.affect(target, Daze.class, Daze.DURATION);
							} else if (meleeEffect == SPIKE && target.pos == oldPos && !Pushing.pushingExistsForChar(target)) {
								Ballistica direction = new Ballistica(from, target.pos, Ballistica.STOP_TARGET);
								Ballistica push = new Ballistica(target.pos, direction.path.get(direction.path.size() - 1), Ballistica.PROJECTILE);
								WandOfBlastWave.throwChar(target, push, 1, true, false, hero);
							}
						}
					}
				} finally {
					hero.belongings.weapon = weapon;
					hero.belongings.secondWep = second;
					hero.belongings.abilityWeapon = ability;
				}
			} else {
				item.setCurrent(hero);
				if (kind == WAND) {
					if (curse != null) curse.effect(item, hero, shot, positiveCurse);
					else {
						Wand wand = (Wand) item;
						if (wand instanceof WandOfWarding && !wand.tryToZap(hero, shot.collisionPos)) return;
						if (originalWand != null) wand.copyZapState(originalWand);
						Char target = Actor.findChar(shot.collisionPos);
						if (target != null && target != hero && target.isAlive() && target.alignment == Char.Alignment.ENEMY) {
							Talent.onExecutionStack(hero, Talent.EXEC_COND_WAND);
						}
						wand.resolveZap(shot);
						if (originalWand != null) originalWand.copyZapState(wand);
					}
				} else if (kind == BOW) {
					((SpiritBow) item).resolveFrozenArrow(hero, shot.collisionPos);
				} else {
					// 闪现符石等物品仍复用自己的路径准备逻辑，不能沿用另一发的静态缓存。
					Ballistica.Projectile previous = Ballistica.setProjectile(projectile);
					try {
						item.throwPos(hero, from, aim);
					} finally {
						Ballistica.setProjectile(previous);
					}
					item.resolveFrozenThrow(hero, shot.collisionPos);
				}
			}
		} finally {
			resolving = null;
		}
	}

	/** 用一个 Actor 挡住正常回合推进，但允许所有特效在渲染线程同时前进。 */
	public static void release(ArrayList<FrozenAttack> attacks) {
		if (attacks.isEmpty()) return;
		Actor.add(new Actor() {
			{ actPriority = VFX_PRIO + 1; }
			@Override
			protected boolean act() {
				Dungeon.hero.busy();
				Actor gate = this;
				Game.runOnRenderThread(() -> {
					for (FrozenAttack attack : attacks) attack.buildEffects();
					// 稳定排序：相同到达时间沿用发起顺序，不依赖特效在 Group 中的位置。
					attacks.sort(Comparator.comparingDouble(a -> a.arrivalTime));
					Group release = new Group() {
						int nextAttack;
						@Override
						public void update() {
							for (FrozenAttack attack : attacks) attack.effects.advance();
							while (nextAttack < attacks.size() && attacks.get(nextAttack).arrived) {
								attacks.get(nextAttack++).resolve();
							}
							if (nextAttack == attacks.size()) {
								// 让尚未消散的射线、锥形边缘和尾迹自行播完。
								for (FrozenAttack attack : attacks) {
									attack.effects.running = true;
									Dungeon.hero.sprite.parent.add(attack.effects);
								}
								killAndErase();
								Actor.remove(gate);
								gate.next();
							}
						}
					};
					for (FrozenAttack attack : attacks) release.add(attack.effects);
					Dungeon.hero.sprite.parent.add(release);
				});
				return false;
			}
		});
	}

	private static class Effects extends Group {
		boolean running;
		@Override
		public void update() {
			if (running) {
				super.update();
				if (countLiving() == 0) killAndErase();
			}
		}
		void advance() { super.update(); }
		float travelTime() {
			float time = 0;
			for (Gizmo member : members) {
				if (member instanceof MagicMissile) time = Math.max(time, ((MagicMissile) member).travelTime());
				if (member instanceof MissileSprite) time = Math.max(time, ((MissileSprite) member).travelTime());
			}
			return time;
		}
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put("kind", kind);
		bundle.put("from", from);
		bundle.put("aim", aim);
		bundle.put("target", targetID);
		bundle.put("item", item);
		bundle.put("projectile", projectile);
		bundle.put("second_weapon", secondWeapon);
		ArrayList<KindOfWeapon> weapons = Dungeon.hero.belongings.getAllItems(KindOfWeapon.class);
		bundle.put("weapon_index", weapons.indexOf(item));
		bundle.put("second_weapon_index", weapons.indexOf(secondWeapon));
		bundle.put("damage_multiplier", damageMultiplier);
		bundle.put("damage_bonus", damageBonus);
		bundle.put("accuracy_multiplier", accuracyMultiplier);
		bundle.put("normal_attack", normalAttack);
		bundle.put("ability_attack", abilityAttack);
		bundle.put("melee_effect", meleeEffect);
		bundle.put("invisible", invisible);
		bundle.put("attack_buffs", attackBuffs);
		for (Buff buff : attackBuffs) {
			if (buff instanceof RunicBlade.RunicSlashTracker) bundle.put("runic_boost", ((RunicBlade.RunicSlashTracker) buff).boost);
		}
		bundle.put("bow_special", bowSpecial);
		bundle.put("bow_bonus", bowBonus);
		bundle.put("wand_level", wandLevel);
		bundle.put("wand_index", originalWand == null ? -1 : heldWands().indexOf(originalWand));
		bundle.put("forced_curse", forcedCurse);
		if (curseClass != null) bundle.put("curse", curseClass);
		bundle.put("positive_curse", positiveCurse);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void restoreFromBundle(Bundle bundle) {
		kind = bundle.getInt("kind");
		from = bundle.getInt("from");
		aim = bundle.getInt("aim");
		targetID = bundle.getInt("target");
		item = (Item) bundle.get("item");
		projectile = (Ballistica.Projectile) bundle.get("projectile");
		secondWeapon = (KindOfWeapon) bundle.get("second_weapon");
		weaponIndex = bundle.getInt("weapon_index");
		secondWeaponIndex = bundle.getInt("second_weapon_index");
		damageMultiplier = bundle.getFloat("damage_multiplier");
		damageBonus = bundle.getFloat("damage_bonus");
		accuracyMultiplier = bundle.getFloat("accuracy_multiplier");
		normalAttack = bundle.getBoolean("normal_attack");
		abilityAttack = bundle.getBoolean("ability_attack");
		meleeEffect = bundle.getInt("melee_effect");
		invisible = bundle.getBoolean("invisible");
		for (Bundlable buff : bundle.getCollection("attack_buffs")) attackBuffs.add((Buff) buff);
		for (Buff buff : attackBuffs) {
			if (buff instanceof RunicBlade.RunicSlashTracker) ((RunicBlade.RunicSlashTracker) buff).boost = bundle.getFloat("runic_boost");
		}
		bowSpecial = bundle.getBoolean("bow_special");
		bowBonus = bundle.getFloat("bow_bonus");
		wandLevel = bundle.getInt("wand_level");
		wandIndex = bundle.getInt("wand_index");
		forcedCurse = bundle.getBoolean("forced_curse");
		if (bundle.contains("curse")) curseClass = bundle.getClass("curse");
		positiveCurse = bundle.getBoolean("positive_curse");
	}
}
