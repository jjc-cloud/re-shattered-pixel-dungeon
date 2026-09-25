package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.HeavyBoomerang;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone;

import java.lang.reflect.Field;

/**
 * MissileWeapon used to hand out a flat 1.5x accuracy factor whenever the target was not
 * adjacent, which made thrown weapons strictly more accurate at range. That bonus is removed:
 * without a talent, thrown weapons are 0.5x in melee and 1x at range. The huntress's point blank
 * talent is now a pure multiplier on thrown accuracy (+33%/+67%/+100%) applied at every distance,
 * so a maxed talent turns melee into 1x and range into 2x. The boomerang's circling-back bonus is
 * a separate mechanic to keep.
 */
public class MissileAccuracyRegression {

	private static Hero hero(int pointBlankPoints) {
		Hero hero = new Hero();
		hero.heroClass = HeroClass.HUNTRESS;
		hero.pos = 0;
		hero.STR = 30; //well above every thrown weapon's strength requirement, so encumbrance is zero
		hero.HP = hero.HT = 100;
		Talent.initClassTalents(hero);
		if (pointBlankPoints > 0) {
			hero.talents.get(0).put(Talent.POINT_BLANK, pointBlankPoints);
		}
		Dungeon.hero = hero;
		return hero;
	}

	private static Char targetAt(int pos) {
		Char target = new Char() { };
		target.alignment = Char.Alignment.ENEMY;
		target.pos = pos;
		target.HP = target.HT = 10;
		return target;
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}

	//range must not change thrown accuracy in either direction
	private static void rangedAccuracyIsFlat() {
		Hero hero = hero(0);
		MissileWeapon stone = new ThrowingStone();
		Char adjacent = targetAt(1);
		Char midRange = targetAt(20);
		Char farRange = targetAt(80);

		check(stone.accuracyFactor(hero, farRange) == 1f,
				"a thrown weapon no longer gains accuracy at range, actual=" + stone.accuracyFactor(hero, farRange));
		check(stone.accuracyFactor(hero, midRange) == stone.accuracyFactor(hero, farRange),
				"thrown accuracy is identical at every non-adjacent distance");
		check(stone.accuracyFactor(hero, adjacent) == 0.5f,
				"point-blank thrown weapons still take the -50% penalty, actual=" + stone.accuracyFactor(hero, adjacent));
	}

	//point blank is a pure multiplier on thrown accuracy, applied at every distance
	private static void pointBlankMultipliesAllDistances() {
		Char adjacent = targetAt(1);
		Char farRange = targetAt(80);
		//+33%/+67%/+100%, stacked on top of the melee penalty
		float[] melee = {0.5f * 4/3f, 0.5f * 5/3f, 0.5f * 2f};
		float[] ranged = {4/3f, 5/3f, 2f};

		//without the talent thrown weapons are 0.5x in melee and 1x at range
		Hero untalented = hero(0);
		MissileWeapon plain = new ThrowingStone();
		check(plain.accuracyFactor(untalented, adjacent) == 0.5f,
				"without the talent melee thrown weapons stay at -50%, actual="
						+ plain.accuracyFactor(untalented, adjacent));
		check(plain.accuracyFactor(untalented, farRange) == 1f,
				"without the talent ranged thrown weapons get no bonus, actual="
						+ plain.accuracyFactor(untalented, farRange));

		for (int points = 1; points <= 3; points++) {
			Hero hero = hero(points);
			MissileWeapon stone = new ThrowingStone();
			check(Math.abs(stone.accuracyFactor(hero, adjacent) - melee[points - 1]) < 1e-4f,
					"point blank rank " + points + " multiplies melee accuracy, actual="
							+ stone.accuracyFactor(hero, adjacent));
			check(Math.abs(stone.accuracyFactor(hero, farRange) - ranged[points - 1]) < 1e-4f,
					"point blank rank " + points + " multiplies ranged accuracy, actual="
							+ stone.accuracyFactor(hero, farRange));
		}
	}

	//a boomerang flying back is still more accurate; that bonus is about circling, not distance
	private static void boomerangCirclingBonusIsKept() throws Exception {
		Hero hero = hero(0);
		MissileWeapon boomerang = new HeavyBoomerang();
		Char farRange = targetAt(80);

		check(boomerang.accuracyFactor(hero, farRange) == 1f,
				"a boomerang in flight gets no ranged bonus, actual=" + boomerang.accuracyFactor(hero, farRange));

		Field circling = HeavyBoomerang.class.getDeclaredField("circlingBack");
		circling.setAccessible(true);
		circling.setBoolean(boomerang, true);

		check(boomerang.accuracyFactor(hero, farRange) == 1.5f,
				"a boomerang circling back still gains +50% accuracy, actual=" + boomerang.accuracyFactor(hero, farRange));
	}

	public static void main(String[] args) throws Exception {
		com.badlogic.gdx.utils.GdxNativesLoader.load();
		WarriorTalentsRegression.setupHeadless();

		rangedAccuracyIsFlat();
		pointBlankMultipliesAllDistances();
		boomerangCirclingBonusIsKept();

		System.out.println("PASS: thrown weapons are 0.5x melee and 1x ranged without a talent, point blank "
				+ "multiplies both (+33%/+67%/+100%), boomerang circling bonus kept");
	}
}
