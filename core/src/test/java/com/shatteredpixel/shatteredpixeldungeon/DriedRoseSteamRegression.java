package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.WaterVapor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.PrisonLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.Bundle;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/** Standalone checks for ghost equipment cleansing and fire/water vapor interactions. */
public class DriedRoseSteamRegression {

	private static final int WIDTH = 11;

	private static int cell( int x, int y ) {
		return x + y * WIDTH;
	}

	private static void check( boolean condition, String message ) {
		if (!condition) throw new AssertionError(message);
	}

	private static Hero prepareLevel() {
		Level level = new PrisonLevel();
		level.setSize(WIDTH, WIDTH);
		level.mobs = new HashSet<>();
		level.heaps = new com.watabou.utils.SparseArray<>();
		level.blobs = new HashMap<>();
		level.plants = new com.watabou.utils.SparseArray<>();
		level.traps = new com.watabou.utils.SparseArray<>();
		Arrays.fill(level.map, Terrain.EMPTY);
		for (int i = 0; i < WIDTH; i++) {
			level.map[cell(i, 0)] = level.map[cell(i, WIDTH - 1)] = Terrain.WALL;
			level.map[cell(0, i)] = level.map[cell(WIDTH - 1, i)] = Terrain.WALL;
		}

		Hero hero = new Hero();
		hero.heroClass = HeroClass.WARRIOR;
		Talent.initClassTalents(hero);
		hero.HP = hero.HT = 20;
		hero.pos = cell(2, 5);
		hero.viewDistance = 8;
		Dungeon.hero = hero;
		Dungeon.level = level;
		return hero;
	}

	private static void fireEvaporatesWater() {
		Hero hero = prepareLevel();
		int water = cell(5, 5);
		Dungeon.level.map[water] = Terrain.WATER;
		Dungeon.level.buildFlagMaps();

		Blob.seed(water, 4, Fire.class, Dungeon.level);

		check(Dungeon.level.map[water] == Terrain.EMPTY, "fire evaporates water terrain");
		check(Blob.volumeAt(water, Fire.class) == 0, "fire is not created on water");
		check(Blob.volumeAt(water, WaterVapor.class) == 2, "evaporation creates timed water vapor");

		Dungeon.level.updateFieldOfView(hero, Dungeon.level.heroFOV);
		check(Dungeon.level.heroFOV[water], "the near side of water vapor is visible");
		check(!Dungeon.level.heroFOV[cell(6, 5)], "water vapor blocks sight behind it");

		WaterVapor vapor = (WaterVapor)Dungeon.level.blobs.get(WaterVapor.class);
		vapor.act();
		check(Blob.volumeAt(water, WaterVapor.class) == 1,
				"water vapor survives its same-turn setup tick");
		vapor.act();
		check(Blob.volumeAt(water, WaterVapor.class) == 0, "water vapor expires after one turn");
	}

	private static void roseCleansesGhostEquipment() {
		Sword weapon = new Sword();
		weapon.cursed = true;
		weapon.cursedKnown = true;
		LeatherArmor armor = new LeatherArmor();
		armor.cursed = true;
		armor.cursedKnown = true;

		Bundle bundle = new Bundle();
		bundle.put("weapon", weapon);
		bundle.put("armor", armor);
		DriedRose rose = new DriedRose();
		rose.restoreFromBundle(bundle);

		check(ScrollOfRemoveCurse.uncursable(rose), "rose is selectable when ghost gear is cursed");
		check(ScrollOfRemoveCurse.uncurse(null, rose), "remove curse cleanses ghost equipment");
		check(!rose.ghostWeapon().cursed && !rose.ghostArmor().cursed,
				"all ghost equipment curses are removed");
		check(!rose.cursed, "ghost equipment curses do not curse the rose");
	}

	public static void main( String[] args ) {
		WarriorTalentsRegression.setupHeadless();
		fireEvaporatesWater();
		roseCleansesGhostEquipment();
		System.out.println("PASS: dried rose curse cleansing and one-turn water vapor");
	}
}
