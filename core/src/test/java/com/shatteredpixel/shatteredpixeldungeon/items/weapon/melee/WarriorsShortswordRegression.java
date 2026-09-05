package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.WarriorTalentsRegression;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;

/** Standalone checks for the warrior's unique starting sword. */
public class WarriorsShortswordRegression {

	public static void main(String[] args) {
		WarriorTalentsRegression.main(args);

		Hero hero = new WarriorTalentsRegression.TestHero();
		Dungeon.hero = hero;
		WarriorsShortsword sword = new WarriorsShortsword();
		sword.identify();
		hero.belongings.weapon = sword;

		check(sword.isUpgradable(), "starting sword accepts its carried upgrade");
		check(!sword.actions(hero).contains(WarriorsShortsword.AC_REFORGE), "unupgraded sword cannot reforge");
		sword.upgrade();
		check(sword.trueLevel() == 1 && !sword.isUpgradable(), "starting sword carries only one upgrade");
		check(sword.actions(hero).contains(WarriorsShortsword.AC_REFORGE), "upgraded sword offers reforge");

		WornShortsword target = new WornShortsword();
		check(sword.canReforge(target), "ordinary melee weapon is selectable");
		check(!sword.canReforge(sword), "starting sword cannot reforge itself");
		check(sword.reforge(hero, target), "equipped starting sword reforges target");
		check(hero.belongings.weapon == null, "reforged starting sword disappears when equipped");
		check(target.trueLevel() == 1, "target gains one upgrade");

		WarriorsShortsword packedSword = new WarriorsShortsword();
		packedSword.upgrade();
		check(packedSword.collect(hero.belongings.backpack), "second sword fits in backpack fixture");
		WornShortsword secondTarget = new WornShortsword();
		check(packedSword.reforge(hero, secondTarget), "backpack starting sword reforges target");
		check(!hero.belongings.contains(packedSword), "reforged starting sword disappears from backpack");
		check(secondTarget.trueLevel() == 1, "second target gains one upgrade");

		boolean generated = false;
		boolean wornStillGenerated = false;
		for (int i = 0; i < Generator.Category.WEP_T1.classes.length; i++) {
			if (Generator.Category.WEP_T1.classes[i] == WarriorsShortsword.class
					&& Generator.Category.WEP_T1.defaultProbs[i] > 0) {
				generated = true;
			}
			if (Generator.Category.WEP_T1.classes[i] == WornShortsword.class
					&& Generator.Category.WEP_T1.defaultProbs[i] > 0) {
				wornStillGenerated = true;
			}
		}
		check(!generated, "starting sword is excluded from random generation and transmutation");
		check(wornStillGenerated, "ordinary worn shortsword remains in the tier-one pool");

		System.out.println("PASS: warrior starting sword upgrade, reforge, consumption and transmutation exclusion");
	}

	private static void check(boolean value, String message) {
		if (!value) throw new AssertionError(message);
	}
}
