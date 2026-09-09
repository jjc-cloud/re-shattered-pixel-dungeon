package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Petrification;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invulnerability;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Eye;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.MedusaEye;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.MobSpawner;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfElements;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing;
import com.watabou.utils.Bundle;

/** Headless checks for lethal progress, resistance, action timing, and targeting. */
public class MedusaEyeRegression {
	public static class Victim extends Char {
		Object deathCause;
		Victim() { HP = HT = 100; pos = 2; }
		@Override public void die(Object cause) { deathCause = cause; HP = 0; }
		void boss() { properties.add(Property.BOSS); }
		void resistStone() { resistances.add(Petrification.class); }
		float actionTime() {
			float before = cooldown();
			spend(1f);
			return cooldown() - before;
		}
	}
	public static class TestEye extends MedusaEye {
		TestEye() { fieldOfView = new boolean[10]; }
		boolean sees(Char victim) { return canAttack(victim); }
		void see(Char victim, boolean visible) { fieldOfView[victim.pos] = visible; }
		void attackTarget(Char victim) { doAttack(victim); }
	}
	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
	private static void near(float actual, float expected, String message) {
		check(Math.abs(actual - expected) < 0.0001f, message + ": " + actual);
	}
	public static void main(String[] args) {
		com.badlogic.gdx.utils.GdxNativesLoader.load();
		com.badlogic.gdx.Gdx.files = (com.badlogic.gdx.Files) java.lang.reflect.Proxy.newProxyInstance(
				MedusaEyeRegression.class.getClassLoader(), new Class[]{com.badlogic.gdx.Files.class},
				(proxy, method, values) -> {
					if (method.getName().equals("internal"))
						return new com.badlogic.gdx.files.FileHandle("core/src/main/assets/" + values[0]);
					throw new UnsupportedOperationException(method.getName());
				});
		Victim victim = new Victim();
		Petrification.apply(victim);
		near(victim.actionTime(), 1.25f, "twenty percent slows all ordinary actions");
		Petrification.apply(victim);
		check(victim.isAlive(), "two applications do not kill");
		near(victim.actionTime(), (1f / 0.6f), "forty percent slows all ordinary actions");
		Petrification.apply(victim);
		Petrification.apply(victim);
		check(victim.isAlive(), "four applications do not kill");
		Petrification.apply(victim);
		check(!victim.isAlive() && victim.deathCause instanceof Petrification, "fifth application kills with stone cause");

		Victim resistant = new Victim();
		resistant.resistStone();
		for (int i = 0; i < 3; i++) Petrification.apply(resistant);
		near(resistant.buff(Petrification.class).progress(), 0.3f, "generic resistance halves buildup");

		Victim ringBearer = new Victim();
		Dungeon.hero = new com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero();
		RingOfElements ring = new RingOfElements();
		ring.new Resistance().attachTo(ringBearer);
		Petrification.apply(ringBearer);
		near(ringBearer.buff(Petrification.class).progress(), 0.825f * 0.2f, "elements ring reduces buildup");
		Buff.affect(ringBearer, MagicImmune.class);
		check(ringBearer.buff(Petrification.class) == null, "magic immunity cleanses existing progress");
		Petrification.apply(ringBearer);
		check(ringBearer.buff(Petrification.class) == null, "magic immunity prevents new progress");

		Victim boss = new Victim();
		boss.boss();
		Petrification.apply(boss);
		check(boss.buff(Petrification.class) == null, "boss property grants immunity");
		Victim invulnerable = new Victim();
		Buff.affect(invulnerable, Invulnerability.class);
		Petrification.apply(invulnerable);
		check(invulnerable.buff(Petrification.class) == null, "invulnerability prevents progress");
		Victim cleansed = new Victim();
		Petrification.apply(cleansed);
		PotionOfCleansing.cleanse(cleansed);
		check(cleansed.buff(Petrification.class) == null, "cleansing removes existing stone progress");
		near(cleansed.actionTime(), 1f, "cleansing restores action speed");
		Petrification.apply(cleansed);
		check(cleansed.buff(Petrification.class) == null, "cleansing rejects the negative buff");

		Bundle saved = new Bundle();
		resistant.buff(Petrification.class).storeInBundle(saved);
		Petrification restored = new Petrification();
		restored.restoreFromBundle(saved);
		near(restored.progress(), 0.3f, "progress survives save and restore");
		restored.attachTo(new Victim());
		restored.act();
		near(restored.progress(), 0.3f, "progress does not accumulate without an eye");

		TestEye eye = new TestEye();
		Victim target = new Victim();
		eye.see(target, true);
		check(eye.sees(target), "vision is sufficient without any level or ballistic path");
		eye.attackTarget(target);
		check(eye.beamCharged && target.buff(Petrification.class) == null, "first action charges");
		eye.attackTarget(target);
		near(target.buff(Petrification.class).progress(), 0.2f, "charged eye adds twenty percent");
		eye.see(target, false);
		check(!eye.sees(target), "out of sight cannot be attacked");
		eye.see(target, true);
		target.invisible = 1;
		check(!eye.sees(target), "invisibility prevents gaze");
		check(MobSpawner.RARE_ALTS.get(Eye.class) == MedusaEye.class, "rare spawn mapping registered");
		target.invisible = 0;
		Bundle eyeSave = new Bundle();
		eye.storeInBundle(eyeSave);
		TestEye restoredEye = new TestEye();
		restoredEye.restoreFromBundle(eyeSave);
		restoredEye.attackTarget(target);
		near(target.buff(Petrification.class).progress(), 0.4f, "saved channel resumes against same target");
		Victim differentTarget = new Victim();
		restoredEye.attackTarget(differentTarget);
		check(differentTarget.buff(Petrification.class) == null, "changing targets requires a fresh charge");
		com.shatteredpixel.shatteredpixeldungeon.sprites.EyeSprite normalSprite =
				new com.shatteredpixel.shatteredpixeldungeon.sprites.EyeSprite();
		com.shatteredpixel.shatteredpixeldungeon.sprites.MedusaEyeSprite rareSprite =
				new com.shatteredpixel.shatteredpixeldungeon.sprites.MedusaEyeSprite();
		near(normalSprite.frame().top, 0f, "normal eye uses first texture row");
		near(rareSprite.frame().top, 18f / rareSprite.texture.height, "rare eye uses second texture row");
		WarriorTalentsRegression.setupHeadless();
		com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero berserker = new com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero();
		berserker.heroClass = com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.WARRIOR;
		berserker.subClass = com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.BERSERKER;
		com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.initClassTalents(berserker);
		com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.initSubclassTalents(berserker);
		berserker.HT = berserker.HP = 100;
		Dungeon.hero = berserker;
		berserker.belongings.armor = new com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor();
		berserker.belongings.armor.affixSeal(new com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal());
		com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Berserk rage = Buff.affect(berserker,
				com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Berserk.class);
		for (int i = 0; i < 5; i++) Petrification.apply(berserker);
		check(berserker.isAlive() && berserker.HP == 50, "stone death triggers death defiance");
		near(rage.power(), 4f, "stone death forces berserk rage");
		check(!rage.deathDefianceAvailable(), "stone death consumes defiance");
		check(berserker.buff(Petrification.class) == null, "defiance removes all stone progress");
		for (boolean large : new boolean[]{false, true}) {
			com.shatteredpixel.shatteredpixeldungeon.ui.BuffIcon gray = new com.shatteredpixel.shatteredpixeldungeon.ui.BuffIcon(new Petrification(), large);
			gray.refresh(new Petrification());
			for (int y = 0; y < gray.texture.height; y++) for (int x = 0; x < gray.texture.width; x++) {
				int pixel = gray.texture.bitmap.getPixel(x, y);
				check(((pixel >>> 24) == ((pixel >>> 8) & 255)), "icon pixels are gray, including after refresh");
			}
		}
		com.shatteredpixel.shatteredpixeldungeon.effects.IceBlock stone = com.shatteredpixel.shatteredpixeldungeon.effects.IceBlock.petrify(rareSprite);
		com.watabou.noosa.Game.elapsed = 2f;
		stone.update(); stone.update();
		check(rareSprite.paused && rareSprite.alive && stone.alive, "statue stays frozen without fading after several seconds");
		System.out.println("MedusaEyeRegression passed");
	}
}

