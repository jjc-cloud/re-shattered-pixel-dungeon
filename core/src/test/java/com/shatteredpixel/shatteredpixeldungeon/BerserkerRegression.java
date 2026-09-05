package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Berserk;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Warlock;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.watabou.utils.Bundle;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;

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

		checkFactors(0f,   1f,    1f,     1f,     1f,    1f);
		checkFactors(0.5f, 1.25f, 1.125f, 1.125f, 0.95f, 1f);
		checkFactors(1f,   1.5f,  1.25f,  1.25f,  0.9f,  1f);
		checkFactors(1.5f, 1.75f, 1.375f, 1.375f, 0.85f, 1f);
		checkFactors(2f,   2f,    1f,     1.5f,   0.8f,  1f);
		checkFactors(2.5f, 1.75f, 1f,     1.75f,  0.9f,  1.125f);
		checkFactors(3f,   1.5f,  1.5f,   2f,     0.2f,  1.25f);
		checkFactors(3.5f, 2f,    1.75f,  3.5f,   0.2f,  1.375f);
		checkFactors(4f,   2.5f,  2f,     5f,     0.2f,  1.5f);
		if (!rage(0f).name().equals("愤怒姿态")) throw new AssertionError("zero rage displays angry stance");

		for (int rank = 0; rank <= 3; rank++) {
			Hero hero = hero(rank, 0);
			Berserk rage = Buff.affect(hero, Berserk.class);
			rage.gainRage(4f);
			checkClose(rage.power(), rank + 1f, "natural rage cap rank " + rank);
			rage.forceRage(4f);
			checkClose(rage.power(), 4f, "forced rage ignores natural cap rank " + rank);
			rage.gainRage(0.1f);
			checkClose(rage.power(), 4f, "gaining rage cannot clamp forced rage to natural cap rank " + rank);
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
		Hero lateTalentHero = hero(3, 0);
		Berserk lateTalent = Buff.affect(lateTalentHero, Berserk.class);
		lateTalent.consumeDeathDefiance();
		lateTalentHero.talents.get(2).put(Talent.DEATHLESS_FURY, 1);
		if (lateTalent.levelsUntilDeathDefiance() != 3) {
			throw new AssertionError("late deathless fury investment starts a three-level cooldown");
		}
		lateTalent.onHeroLevelUp();
		lateTalent.onHeroLevelUp();
		if (lateTalent.deathDefianceAvailable()) throw new AssertionError("late talent recharge is not early");
		lateTalent.onHeroLevelUp();
		if (!lateTalent.deathDefianceAvailable()) throw new AssertionError("late talent recharges after three levels");

		Hero catalystHero = hero(3, 2);
		Berserk catalyst = Buff.affect(catalystHero, Berserk.class);
		catalyst.forceRage(2f);
		checkClose(Armor.Glyph.genericProcChanceMultiplier(catalystHero), 2.25f,
				"catalyst applies to armor glyphs and curses");
		checkClose(catalyst.normalWandShotDelay(), 0f, "rank two catalyst makes normal wand shots instant");
		Sword cursedWeapon = new Sword();
		cursedWeapon.cursed = true;
		catalystHero.belongings.weapon = cursedWeapon;
		if (cursedWeapon.doUnequip(catalystHero, false, false)) {
			throw new AssertionError("rank two catalyst cannot remove cursed weapons");
		}
		catalystHero.talents.get(2).put(Talent.ENRAGED_CATALYST, 3);
		if (!cursedWeapon.doUnequip(catalystHero, false, false)) {
			throw new AssertionError("rank three catalyst removes cursed weapons");
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
		ActionIndicator.clearAction();
		manual.fx(true);
		if (ActionIndicator.action != manual) {
			throw new AssertionError("berserker rage control uses the shared action indicator");
		}
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
		attackRage.onAttackResolved(85, 85);
		checkClose(attackRage.power(), 1.075f, "target shielding grants no attack rage");

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
		Berserk sealRage = Buff.affect(sealed, Berserk.class);
		for (float shieldRage : new float[]{0f, 1f, 2f, 4f}) {
			sealRage.forceRage(shieldRage);
			checkClose(shield.maxShield(), original + Math.round(shieldRage * armorMaximum),
					"seal adds lost armor DR at trigger rage " + shieldRage);
		}
		sealRage.forceRage(2f);
		shield.activate();
		int triggeredShield = shield.shielding();
		Buff.affect(sealed, Berserk.class).forceRage(4f);
		checkClose(shield.shielding(), triggeredShield, "existing shield is a trigger-time snapshot");

		Hero defender = hero(3, 0);
		Berserk defenseRage = Buff.affect(defender, Berserk.class);
		com.shatteredpixel.shatteredpixeldungeon.actors.Char enemy =
				new com.shatteredpixel.shatteredpixeldungeon.actors.Char() { };
		enemy.alignment = com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY;
		checkClose(defenseRage.modifyPhysicalIncomingDamage(20, enemy), 20, "zero-rage enemy damage multiplier");
		checkClose(defenseRage.power(), 0.2f, "physical damage grants rage from raw damage");
		defenseRage.forceRage(1f);
		checkClose(defenseRage.modifyIncomingDamage(10, new Warlock.DarkBolt()), 12.5f,
				"enemy magic receives posture multiplier");
		checkClose(defenseRage.power(), 1f, "enemy magic does not grant rage");
		defenseRage.forceRage(0f);
		checkClose(defenseRage.modifyIncomingDamage(5, new Hunger()), 5f, "hunger is not posture-amplified");
		checkClose(defenseRage.power(), 0.05f, "hunger grants one percent rage per raw damage");

		Hero dying = hero(3, 0);
		dying.belongings.armor = new ClothArmor();
		dying.belongings.armor.upgrade(5);
		dying.belongings.armor.affixSeal(new BrokenSeal());
		Berserk deathRage = Buff.affect(dying, Berserk.class);
		BrokenSeal.WarriorShield deathShield = dying.buff(BrokenSeal.WarriorShield.class);
		dying.HP = 0;
		if (!dying.isAlive()) throw new AssertionError("equipped seal permits death defiance");
		checkClose(dying.HP, 50, "death defiance restores half maximum health");
		checkClose(deathRage.power(), 4f, "death defiance forces four hundred percent rage");
		if (!deathShield.isDeathShield()) throw new AssertionError("death refresh marks its shield");
		checkClose(deathRage.damageMultiplierWithDeathShield(), 3.75f,
				"death shield independently multiplies posture damage");
		if (deathRage.deathDefianceAvailable()) throw new AssertionError("death defiance is consumed");
		Bundle deathShieldState = new Bundle();
		deathShield.storeInBundle(deathShieldState);
		if (!deathShieldState.getBoolean("death_shield")) throw new AssertionError("death shield marker is saved");
		deathShield.decShield(deathShield.shielding());
		if (deathShield.isDeathShield()) throw new AssertionError("depleted death shield clears marker");
		deathShield.activateDeathShield();
		deathShield.activate();
		if (deathShield.isDeathShield()) throw new AssertionError("normal seal activation replaces death marker");
		deathShield.activateDeathShield();
		deathShield.setArmor(null);
		if (deathShield.isDeathShield()) throw new AssertionError("removing seal armor clears death marker");

		Hero unsealed = hero(3, 0);
		Berserk unsealedRage = Buff.affect(unsealed, Berserk.class);
		unsealed.HP = 0;
		if (unsealed.isAlive()) throw new AssertionError("death defiance requires an equipped seal");
		if (!unsealedRage.deathDefianceAvailable()) throw new AssertionError("failed defiance is not consumed");

		Hero saneDeath = hero(3, 0);
		saneDeath.belongings.armor = new ClothArmor();
		saneDeath.belongings.armor.affixSeal(new BrokenSeal());
		Berserk saneDeathRage = Buff.affect(saneDeath, Berserk.class);
		saneDeathRage.forceRage(2.5f);
		saneDeath.HP = 0;
		if (saneDeath.isAlive()) throw new AssertionError("sane stance dies without death defiance");
		if (!saneDeathRage.deathDefianceAvailable()) throw new AssertionError("sane death does not consume defiance");

		for (int rank = 1; rank <= 3; rank++) {
			Hero rechargeHero = hero(3, 0);
			rechargeHero.talents.get(2).put(Talent.DEATHLESS_FURY, rank);
			Berserk recharge = Buff.affect(rechargeHero, Berserk.class);
			recharge.consumeDeathDefiance();
			int interval = 4 - rank;
			for (int i = 1; i < interval; i++) recharge.onHeroLevelUp();
			if (recharge.deathDefianceAvailable()) throw new AssertionError("deathless fury recharges too early rank " + rank);
			recharge.onHeroLevelUp();
			if (!recharge.deathDefianceAvailable()) throw new AssertionError("deathless fury recharge rank " + rank);
		}

		System.out.println("PASS: berserker rage caps, stance factors, decay and catalyst scaling");
	}

	private static void checkClose(float actual, float expected, String message) {
		if (Math.abs(actual - expected) > EPSILON) {
			throw new AssertionError(message + ": expected " + expected + ", got " + actual);
		}
	}
}
