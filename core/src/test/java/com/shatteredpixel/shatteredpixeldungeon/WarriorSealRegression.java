package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Warlock;
import com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.watabou.utils.Bundle;

/** Standalone mechanics checks, run from the repository root. */
public class WarriorSealRegression {
	private static Hero hero(HeroSubClass subclass) {
		Hero hero = new WarriorTalentsRegression.TestHero();
		Dungeon.hero = hero;
		Dungeon.depth = 1;
		Dungeon.branch = 0;
		hero.heroClass = HeroClass.WARRIOR;
		hero.subClass = subclass;
		Talent.initClassTalents(hero);
		hero.HP = hero.HT = 200;
		hero.pos = 20;
		hero.damageInterrupt = false; // no input controller in this headless fixture
		hero.belongings.armor = new ClothArmor();
		hero.belongings.armor.affixSeal(new BrokenSeal());
		return hero;
	}

	private static Bundle state(BrokenSeal.WarriorShield shield) {
		Bundle bundle = new Bundle();
		shield.storeInBundle(bundle);
		return bundle;
	}

	public static void main(String[] args) {
		WarriorTalentsRegression.main(args); // headless resources and level fixture
		Char enemy = new Char() { };
		enemy.alignment = Char.Alignment.ENEMY;
		enemy.pos = 22;

		Hero hero = hero(HeroSubClass.NONE);
		BrokenSeal seal = hero.belongings.armor.checkSeal();
		BrokenSeal.WarriorShield shield = hero.buff(BrokenSeal.WarriorShield.class);
		check(!BrokenSeal.isComplete() && seal.name().equals("破损纹章"), "broken form");
		check(shield.maxShield() == 5, "original base shield");
		hero.damage(1, enemy);
		check(hero.HP == 199 && shield.shielding() == 0, "original half-health condition");
		hero.HP = 100;
		hero.damage(1, enemy);
		check(hero.HP == 100 && shield.shielding() == 4, "broken shield before hit");
		check(state(shield).getInt("cooldown") == 150, "original cooldown");
		hero.subClass = HeroSubClass.GLADIATOR;
		shield.updateForm();
		check(state(shield).getInt("cooldown") == 100 && BrokenSeal.isComplete(), "subclass transition updates same seal");

		hero = hero(HeroSubClass.GLADIATOR);
		seal = hero.belongings.armor.checkSeal();
		shield = hero.buff(BrokenSeal.WarriorShield.class);
		check(BrokenSeal.isComplete() && seal.name().equals("完整纹章"), "complete form with original item class");
		check(shield.maxShield() == 5, "no innate max HP bonus on complete seal");
		hero.damage(1, enemy);
		check(hero.HP == 200 && shield.shielding() == 4, "complete shield at full health");
		check(state(shield).getInt("cooldown") == 100, "complete cooldown");
		check(state(shield).getInt("gladiator_damage") == 0, "absorbed damage does not count");
		shield.decShield(shield.shielding());
		for (int i = 0; i < 24; i++) { hero.HP = 200; hero.damage(1, enemy); }
		check(shield.coolingDown() && state(shield).getInt("gladiator_damage") == 24, "24 HP not enough");
		hero.damage(1, enemy);
		check(!shield.coolingDown(), "25 HP resets cooldown");
		hero.damage(1, enemy);
		check(shield.shielding() == 4 && shield.coolingDown(), "next hit triggers refreshed shield");

		hero = hero(HeroSubClass.BERSERKER);
		shield = hero.buff(BrokenSeal.WarriorShield.class);
		hero.damage(1, enemy);
		shield.decShield(shield.shielding());
		for (int i = 0; i < 11; i++) { hero.HP = 200; hero.damage(10, enemy); }
		check(!state(shield).getBoolean("guard_ready"), "55 percent not enough");
		hero.HP = 200;
		hero.damage(10, enemy);
		check(state(shield).getBoolean("guard_ready"), "60 percent arms guard");
		Bundle saved = state(shield);
		shield.detach();
		shield = new BrokenSeal.WarriorShield();
		shield.restoreFromBundle(saved);
		shield.attachTo(hero);
		shield.setArmor(hero.belongings.armor);
		check(state(shield).getBoolean("guard_ready"), "guard survives save and restore");
		int hp = hero.HP;
		hero.damage(1, new Object());
		check(hero.HP == hp - 1 && state(shield).getBoolean("guard_ready"), "environment does not consume guard");
		hp = hero.HP;
		hero.damage(999, enemy);
		check(hero.HP == hp && shield.shielding() == 5, "guard negates lethal attack and grants full shield");
		check(!state(shield).getBoolean("guard_ready") && state(shield).getInt("cooldown") == 100, "guard consumed once");
		hero.damage(1, enemy);
		check(shield.shielding() == 4, "next attack consumes shield normally");
		shield.onHealthLost(120);
		check(shield.blockEnemyAttack(5, new Warlock.DarkBolt()), "enemy magic can consume guard");
		shield.onHealthLost(120);
		shield.setArmor(null);
		check(!shield.blockEnemyAttack(5, enemy), "unequipped seal cannot guard");
		shield.setArmor(hero.belongings.armor);
		shield.decShield(shield.shielding());
		for (int i = 0; i < 99; i++) shield.act();
		check(shield.coolingDown(), "100-turn cooldown not finished at 99");
		shield.act();
		check(!shield.coolingDown(), "100-turn cooldown expires");
		check(!seal.desc().contains("碎裂"), "updated text");
		hero = hero(HeroSubClass.NONE);
		com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor startingArmor = hero.belongings.armor;
		seal = startingArmor.checkSeal();
		hero.belongings.armor = new com.shatteredpixel.shatteredpixeldungeon.items.armor.ScaleArmor();
		hero.belongings.armor.activate(hero);
		hero.belongings.armor.affixSeal(startingArmor.detachSeal());
		check(startingArmor.checkSeal() == null && hero.belongings.armor.checkSeal() == seal, "same seal transferred off discarded starting armor");
		shield = hero.buff(BrokenSeal.WarriorShield.class);
		check(shield.maxShield() == 11, "shield rebound to test starting scale armor");
		hero.HP = 100;
		hero.damage(1, enemy);
		check(hero.HP == 100 && shield.shielding() == 10, "transferred seal actually protects the hero");
		for (HeroSubClass subclass : new HeroSubClass[]{HeroSubClass.NONE, HeroSubClass.GLADIATOR, HeroSubClass.BERSERKER}) {
			hero = hero(subclass);
			shield = hero.buff(BrokenSeal.WarriorShield.class);
			for (int rank = 0; rank <= 2; rank++) {
				hero.talents.get(0).put(Talent.IRON_WILL, rank);
				check(shield.maxShield() == 5 + 20 * rank, "Iron Will adds 0/10/20 percent in both forms");
			}
			hero.HT = 150;
			check(shield.maxShield() == 35, "Iron Will follows current max HP");
		}
		hero.heroClass = HeroClass.MAGE;
		hero.belongings.armor = null;
		hero.talents.get(0).put(Talent.IRON_WILL, 1);
		check(shield.maxShield() == 15, "metamorphed Iron Will rank one");
		hero.talents.get(0).put(Talent.IRON_WILL, 2);
		check(shield.maxShield() == 30, "metamorphed Iron Will rank two");
		System.out.println("PASS: seal forms, pre-hit shields, cooldowns, actual HP thresholds and saved guard");
	}

	private static void check(boolean value, String message) {
		if (!value) throw new AssertionError(message);
	}
}
