package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Bandit;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Guard;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Thief;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.EtherealChains;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;

public class MobArtifactDropsRegression {
	private static class TestThief extends Thief {
		Object lootType() {
			return loot;
		}
	}

	private static void check(boolean value, String message) {
		if (!value) throw new AssertionError(message);
	}

	private static float artifactWeight(Class<?> artifact) {
		for (int i = 0; i < Generator.Category.ARTIFACT.classes.length; i++) {
			if (Generator.Category.ARTIFACT.classes[i] == artifact) {
				return Generator.Category.ARTIFACT.defaultProbs[i];
			}
		}
		throw new AssertionError("artifact is missing from the generator registry");
	}

	public static void main(String[] args) {
		Dungeon.hero = new Hero();

		check(artifactWeight(EtherealChains.class) == 0,
				"ethereal chains are excluded from ordinary artifact generation");
		check(artifactWeight(MasterThievesArmband.class) == 0,
				"master thieves' armband is excluded from ordinary artifact generation");

		Dungeon.LimitedDrops.GUARD_ARM.count = 0;
		Guard guard = new Guard();
		check(guard.lootChance() == 0.2f, "guards retain their ordinary armor drop chance");
		check(guard.createLoot() instanceof Armor, "guards retain their ordinary armor loot");
		check(Dungeon.LimitedDrops.GUARD_ARM.count == 1,
				"ordinary armor drops retain their independent limited-drop counter");
		check(!Dungeon.LimitedDrops.GUARD_CHAINS.dropped(),
				"ordinary armor drops do not consume the ethereal chains drop");

		Dungeon.LimitedDrops.THEIF_MISC.count = 0;
		TestThief thief = new TestThief();
		check(thief.lootChance() == 0.03f, "thieves use the standard three-percent drop chance");
		check(thief.createLoot() instanceof MasterThievesArmband,
				"the first thief loot is the master thieves' armband");
		check(thief.lootChance() == 0.03f,
				"the first ring or artifact keeps the standard drop chance after the armband");
		check(thief.lootType() == Generator.Category.RING
				|| thief.lootType() == Generator.Category.ARTIFACT,
				"thieves only open their ordinary ring and artifact pool after the armband");

		Dungeon.LimitedDrops.THEIF_MISC.count = 0;
		Bandit bandit = new Bandit();
		check(bandit.lootChance() == 1f, "bandits guarantee the armband drop");
		check(bandit.createLoot() instanceof MasterThievesArmband,
				"the bandit's guaranteed first loot is the master thieves' armband");
		check(bandit.lootChance() == 1f,
				"the bandit's first later ring or artifact remains guaranteed");

		System.out.println("MobArtifactDropsRegression passed");
	}
}
