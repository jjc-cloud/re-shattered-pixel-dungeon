package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Berserk;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Warlock;
import com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;

/** Standalone checks for the berserker rage controller. */
public class BerserkerRegression {

	private static final float EPSILON = 0.0001f;

	private static class FixedDamageHero extends WarriorTalentsRegression.TestHero {
		@Override public int damageRoll() { return 10; }
		@Override public int attackSkill(com.shatteredpixel.shatteredpixeldungeon.actors.Char target) {
			return INFINITE_ACCURACY;
		}
	}

	private static Hero hero(int endlessRage, int catalyst) {
		return hero(new WarriorTalentsRegression.TestHero(), endlessRage, catalyst);
	}

	private static Hero hero(Hero hero, int endlessRage, int catalyst) {
		hero.heroClass = HeroClass.WARRIOR;
		hero.subClass = HeroSubClass.BERSERKER;
		hero.HT = hero.HP = 100;
		hero.pos = 20;
		Talent.initClassTalents(hero);
		Talent.initSubclassTalents(hero);
		hero.talents.get(2).put(Talent.ENDLESS_RAGE, endlessRage);
		hero.talents.get(2).put(Talent.ENRAGED_CATALYST, catalyst);
		Dungeon.hero = hero;
		return hero;
	}

	private static Berserk rage(float power) {
		Hero hero = hero(3, 0);
		Berserk rage = Buff.affect(hero, Berserk.class);
		rage.gainRage(power);
		return rage;
	}

	private static void checkFactors(float power, float damage, float incoming,
			float accuracy, float evasion, float speed) {
		Berserk rage = rage(power);
		checkClose(rage.damageMultiplier(), damage, "damage at " + power);
		checkClose(rage.incomingDamageMultiplier(), incoming, "incoming damage at " + power);
		checkClose(rage.accuracyMultiplier(), accuracy, "accuracy at " + power);
		checkClose(rage.evasionMultiplier(), evasion, "evasion at " + power);
		checkClose(rage.speedMultiplier(), speed, "speed at " + power);
	}

	public static void main(String[] args) {
		WarriorTalentsRegression.main(args);

		checkFactors(0f,   1f,    1f,     1f,    1f,   1f);
		checkFactors(0.5f, 1.25f, 1.125f, 1f,    1f,   1f);
		checkFactors(1f,   1.5f,  1.25f,  1f,    0.8f, 1f);
		checkFactors(1.5f, 2f,    1.375f, 1.25f, 0.8f, 1.125f);
		checkFactors(2f,   2.5f,  1.5f,   1.5f,  1f,   1.25f);
		checkFactors(2.5f, 1.75f, 1.25f,  2f,    1f,   1.375f);
		checkFactors(3f,   1f,    1f,     2.5f,  0.2f, 1.5f);
		checkFactors(3.5f, 2.5f,  1.5f,   3.75f, 0.2f, 2f);
		checkFactors(4f,   4f,    2f,     5f,    0.2f, 2.5f);

		for (int rank = 0; rank <= 3; rank++) {
			Hero hero = hero(rank, 0);
			Berserk rage = Buff.affect(hero, Berserk.class);
			rage.gainRage(4f);
			checkClose(rage.power(), rank + 1f, "natural rage cap rank " + rank);
			rage.forceRage(4f);
			checkClose(rage.power(), 4f, "forced rage ignores natural cap rank " + rank);
		}

		float[] enchantCaps = {1f, 1.5f, 2.25f, 3f};
		for (int rank = 1; rank <= 3; rank++) {
			Hero hero = hero(3, rank);
			Berserk rage = Buff.affect(hero, Berserk.class);
			rage.gainRage(1f);
			checkClose(rage.enchantFactor(1f), 1f + (enchantCaps[rank] - 1f) / 2f,
					"catalyst halfway rank " + rank);
			rage.gainRage(3f);
			checkClose(rage.enchantFactor(1f), enchantCaps[rank], "catalyst cap rank " + rank);
		}

		Berserk normalDecay = rage(1f);
		normalDecay.powerLossBuffer = 0;
		normalDecay.act();
		checkClose(normalDecay.power(), 0.95f, "normal rage decay");
		Berserk sanityDecay = rage(2.5f);
		sanityDecay.powerLossBuffer = 0;
		sanityDecay.act();
		checkClose(sanityDecay.power(), 2.4f, "sanity rage decays twice as fast");
		Berserk frenzyDecay = rage(3.5f);
		frenzyDecay.powerLossBuffer = 0;
		frenzyDecay.act();
		checkClose(frenzyDecay.power(), 3.5f, "frenzy rage does not decay");

		Berserk manual = rage(3.5f);
		float before = Dungeon.hero.cooldown();
		manual.doAction();
		checkClose(manual.power(), 2.5f, "manual action removes one hundred percent rage");
		checkClose(Dungeon.hero.cooldown() - before, 1f, "manual action spends one turn");

		Hero attacker = hero(new FixedDamageHero(), 3, 0);
		Berserk attackRage = Buff.affect(attacker, Berserk.class);
		attackRage.forceRage(1f);
		checkClose(attackRage.damageFactor(10f), 15f, "attack uses pre-attack stance damage");
		attackRage.onAttackResolved(100, 85);
		checkClose(attackRage.power(), 1.075f, "attack rage uses actual HP loss");

		Hero armored = hero(3, 0);
		armored.belongings.armor = new ClothArmor();
		armored.belongings.armor.upgrade(10);
		for (int i = 0; i < 20; i++) {
			if (armored.drRoll() != 0) throw new AssertionError("berserker armor contributes no random DR");
		}

		Hero sealed = hero(3, 0);
		sealed.belongings.armor = new ClothArmor();
		sealed.belongings.armor.upgrade(5);
		sealed.belongings.armor.affixSeal(new BrokenSeal());
		BrokenSeal.WarriorShield shield = sealed.buff(BrokenSeal.WarriorShield.class);
		int original = sealed.belongings.armor.checkSeal().maxShield(
				sealed.belongings.armor.tier, sealed.belongings.armor.level());
		int armorMaximum = sealed.belongings.armor.DRMax();
		Buff.affect(sealed, Berserk.class).forceRage(2f);
		checkClose(shield.maxShield(), original + 2 * armorMaximum, "seal adds lost armor DR at trigger rage");
		shield.activate();
		int triggeredShield = shield.shielding();
		Buff.affect(sealed, Berserk.class).forceRage(4f);
		checkClose(shield.shielding(), triggeredShield, "existing shield is a trigger-time snapshot");

		Hero defender = hero(3, 0);
		Berserk defenseRage = Buff.affect(defender, Berserk.class);
		com.shatteredpixel.shatteredpixeldungeon.actors.Char enemy =
				new com.shatteredpixel.shatteredpixeldungeon.actors.Char() { };
		enemy.alignment = com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY;
		checkClose(defenseRage.modifyIncomingDamage(20, enemy), 20, "zero-rage enemy damage multiplier");
		checkClose(defenseRage.power(), 0.2f, "physical damage grants rage from raw damage");
		defenseRage.forceRage(1f);
		checkClose(defenseRage.modifyIncomingDamage(10, new Warlock.DarkBolt()), 12.5f,
				"enemy magic receives posture multiplier");
		checkClose(defenseRage.power(), 1f, "enemy magic does not grant rage");

		System.out.println("PASS: berserker rage caps, stance factors, decay and catalyst scaling");
	}

	private static void checkClose(float actual, float expected, String message) {
		if (Math.abs(actual - expected) > EPSILON) {
			throw new AssertionError(message + ": expected " + expected + ", got " + actual);
		}
	}
}
