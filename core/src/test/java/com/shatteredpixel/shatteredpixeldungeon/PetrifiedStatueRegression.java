package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Petrification;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfSirensSong;
import com.shatteredpixel.shatteredpixeldungeon.sprites.RatSprite;
import com.watabou.utils.Bundle;
import java.util.Arrays;

public class PetrifiedStatueRegression {
	public static class Victim extends Char {
		Victim() { HP = HT = 100; pos = 20; }
		@Override public String name() { return "受试者"; }
		void boss() { properties.add(Property.BOSS); }
	}
	public static class TestEye extends MedusaEye {
		int melees;
		TestEye() { pos = 40; fieldOfView = new boolean[81]; Arrays.fill(fieldOfView, true); }
		boolean canHit(Char target) { return canAttack(target); }
		void strike(Char target) { doAttack(target); }
		@Override protected boolean doMeleeAttack(Char target) { melees++; return true; }
	}
	public static class TestRat extends Rat {
		TestRat() { fieldOfView = new boolean[81]; Arrays.fill(fieldOfView, true); }
		Char choose() { return chooseEnemy(); }
	}
	public static class MeleeHero extends Hero {
		@Override public int attackSkill(Char target) { return INFINITE_ACCURACY; }
		@Override public int damageRoll() { return 10; }
	}
	private static void check(boolean value, String message) {
		if (!value) throw new AssertionError(message);
	}
	public static void main(String[] args) {
		com.badlogic.gdx.utils.GdxNativesLoader.load();
		WarriorTalentsRegression.setupHeadless();
		com.badlogic.gdx.Gdx.app = (com.badlogic.gdx.Application)java.lang.reflect.Proxy.newProxyInstance(
				PetrifiedStatueRegression.class.getClassLoader(), new Class[]{com.badlogic.gdx.Application.class},
				(proxy, method, values) -> {
					if (method.getName().equals("log") || method.getName().equals("postRunnable")) return null;
					throw new UnsupportedOperationException(method.getName());
				});
		Dungeon.hero = new Hero(); Dungeon.hero.pos = 60;
		Dungeon.level.mobs = new java.util.HashSet<>();
		Arrays.fill(Dungeon.level.passable, true);
		Arrays.fill(Dungeon.level.heroFOV, true);
		Victim victim = new Victim();
		victim.sprite = new RatSprite();
		Actor.add(victim);
		for (int i = 0; i < 5; i++) Petrification.apply(victim);
		PetrifiedStatue statue = (PetrifiedStatue)Dungeon.level.findMob(20);
		check(statue != null && !victim.isAlive(), "death leaves an actual statue occupant");
		Actor.add(statue); // GameScene registers actors when a render scene exists.
		check(Actor.findChar(20) == statue, "statue owns the collision cell");
		check(!Dungeon.findPassable(Dungeon.hero, Dungeon.level.passable, Dungeon.level.heroFOV, true)[20], "statue blocks paths");
		check(statue.name().equals("受试者的雕像"), "title contains victim name");
		check(statue.info().equals("你能感受到这尊雕像残留的余热。"), "fixed inspection description");
		check(Char.hasProp(statue, Char.Property.IMMOVABLE), "statue cannot be displaced");
		check(!statue.interact(Dungeon.hero) && !statue.heroShouldInteract(), "click attacks instead of swapping positions");
		check(!Petrification.canAffect(statue), "statues cannot be petrified again");
		Bundle saved = new Bundle(); saved.put("statue", statue);
		PetrifiedStatue restored = (PetrifiedStatue)saved.get("statue");
		check(restored.name().equals(statue.name()) && Arrays.equals(restored.pixels, statue.pixels), "name and appearance survive save");
		check(restored.sprite().paused, "restored statue is frozen");
		// The actual melee target UI must use the snapshot sprite factory, not spriteClass.
		com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.defaultZoom = 1;
		new com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator();
		com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator indicator = new com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator();
		com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator.target(statue);
		check(com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator.instance.target() == statue,
				"melee target UI accepts a statue");
		check(statue.isActive() && restored.isActive(), "living statues pass the examineObject inspection gate, including after loading");
		TestRat rat = new TestRat(); rat.pos = 40;
		Dungeon.level.mobs.add(rat); Actor.add(rat);
		check(rat.choose() == Dungeon.hero, "monster ignores closer statue");
		statue.damage(1, new Object());
		check(Actor.findChar(20) == null && Dungeon.level.findMob(20) == null, "any damage destroys statue and frees its cell");
		check(!statue.isActive(), "destroyed statue no longer passes inspection gate");
		// Run the real hero attack completion, including AttackIndicator, weapon procs and death.
		MeleeHero attacker = new MeleeHero();
		attacker.pos = 21;
		attacker.sprite = new RatSprite(); attacker.sprite.ch = attacker;
		attacker.fieldOfView = Dungeon.level.heroFOV;
		attacker.belongings.weapon = new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword();
		Dungeon.hero = attacker;
		restored.sprite = restored.sprite(); restored.sprite.ch = restored;
		Dungeon.level.mobs.add(restored); Actor.add(restored);
		try {
			java.lang.reflect.Field targetField = Hero.class.getDeclaredField("attackTarget");
			targetField.setAccessible(true);
			targetField.set(attacker, restored);
			attacker.onAttackComplete();
		} catch (ReflectiveOperationException e) { throw new AssertionError(e); }
		check(!restored.isAlive() && Actor.findChar(20) == null, "full hero melee breaks saved statue without crashing");
		check(!restored.sprite.alive && attacker.attackTarget() == null, "melee cleans up the statue sprite and attack state");
		indicator.destroy();
		Victim airborne = new Victim(); airborne.flying = true;
		Dungeon.level.pit[20] = true;
		for (int i = 0; i < 5; i++) Petrification.apply(airborne);
		check(!airborne.isAlive() && Dungeon.level.findMob(20) == null, "pit petrification leaves no statue");
		Dungeon.level.pit[20] = false;
		TestEye eye = new TestEye();
		Victim boss = new Victim(); boss.boss();
		check(!eye.canHit(boss), "immune target outside melee range cannot be attacked remotely");
		boss.pos = 41;
		check(eye.canHit(boss), "immune target in melee range can be attacked");
		eye.strike(boss);
		check(eye.melees == 1 && !eye.beamCharged, "boss takes melee instead of charge");
		Victim protectedTarget = new Victim(); protectedTarget.pos = 41;
		eye.strike(protectedTarget);
		Buff.affect(protectedTarget, MagicImmune.class);
		eye.strike(protectedTarget);
		check(eye.melees == 2 && !eye.beamCharged, "immunity acquired during charge switches to melee");
		Buff.detach(protectedTarget, MagicImmune.class);
		eye.strike(protectedTarget);
		check(eye.beamCharged, "gaze resumes after immunity expires");
		PotionOfCleansing.cleanse(protectedTarget);
		eye.strike(protectedTarget);
		check(eye.melees == 3 && !eye.beamCharged, "cleansing uses melee");
		check(eye.createLoot() instanceof ScrollOfSirensSong && eye.createLoot().quantity() == 1, "fixed single siren scroll loot");
		System.out.println("PetrifiedStatueRegression passed");
	}
}
