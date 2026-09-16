package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blizzard;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Freezing;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SteamCarrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.WaterVapor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.BlobImmunity;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.SteamBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.PrisonLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Bundle;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/** Standalone checks for ghost equipment cleansing and fire/water vapor interactions. */
public class DriedRoseSteamRegression {

	private static final int WIDTH = 11;
	private static class DamageCounter extends Char {
		int damageEvents;

		@Override
		public void damage(int dmg, Object src) {
			damageEvents++;
		}
	}

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
		level.customTiles = new ArrayList<>();
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

	private static void steamBrewUsesCarrierGas() {
		prepareLevel();
		int center = cell(5, 5);
		Dungeon.level.buildFlagMaps();

		new SteamBrew().shatter(center);
		check(Blob.volumeAt(center, WaterVapor.class) == 0,
				"steam brew does not directly seed water vapor");
		SteamCarrier carrier = (SteamCarrier)Dungeon.level.blobs.get(SteamCarrier.class);
		check(carrier != null && carrier.volume == 9 * 120,
				"steam brew carrier matches infernal brew gas volume");

		carrier.act();
		check(Blob.volumeAt(center, WaterVapor.class) > 0,
				"steam carrier releases short-lived water vapor while evolving");
	}

	private static void waterVaporSettlesOncePerTick() {
		prepareLevel();
		Dungeon.level.buildFlagMaps();
		Dungeon.hero.viewDistance = 4;
		DamageCounter target = new DamageCounter();
		target.HT = target.HP = 100;
		target.pos = cell(4, 5);
		target.viewDistance = 4;
		target.sprite = new CharSprite();
		int vaporCell = cell(5, 5);
		WaterVapor vapor = Blob.seed(vaporCell, 2, WaterVapor.class, Dungeon.level);
		Actor.add(target);

		target.move(vaporCell);
		check(target.damageEvents == 0, "entering water vapor does not deal immediate damage");

		vapor.act();
		check(target.damageEvents == 1, "water vapor deals damage during its blob tick");
		target.move(cell(4, 5));
		target.move(vaporCell);
		check(target.damageEvents == 1, "moving through water vapor cannot add a second damage event");

		vapor.act();
		check(target.damageEvents == 2, "water vapor deals one new damage event on the next blob tick");
		Actor.remove(target);
	}

	private static void steamAndBlizzardCondenseIntoWater() {
		prepareLevel();
		int center = cell(5, 5);
		Dungeon.level.buildFlagMaps();
		SteamCarrier carrier = Blob.seed(center, 120, SteamCarrier.class, Dungeon.level);
		Blob.seed(center, 120, Blizzard.class, Dungeon.level);

		carrier.act();
		check(Blob.volumeAt(center, SteamCarrier.class) == 0
				&& Blob.volumeAt(center, Blizzard.class) == 0,
				"steam acting first cancels both carrier gasses");
		check(Dungeon.level.map[center] == Terrain.WATER,
				"steam and blizzard condense into water when steam acts first");
		check(Blob.volumeAt(center, WaterVapor.class) == 0,
				"cancelled steam carrier releases no water vapor");

		prepareLevel();
		Dungeon.level.buildFlagMaps();
		Blob.seed(center, 120, SteamCarrier.class, Dungeon.level);
		Blizzard blizzard = Blob.seed(center, 120, Blizzard.class, Dungeon.level);
		blizzard.act();
		check(Blob.volumeAt(center, SteamCarrier.class) == 0
				&& Blob.volumeAt(center, Blizzard.class) == 0,
				"blizzard acting first cancels both carrier gasses");
		check(Dungeon.level.map[center] == Terrain.WATER,
				"steam and blizzard condense into water when blizzard acts first");
	}

	private static void steamAndFrostCondenseIntoWater() {
		int center = cell(5, 5);

		//冰霜药剂每格只给 10 点，气浪每格 120 点，但抵消是整格的，不看数值大小
		prepareLevel();
		Dungeon.level.buildFlagMaps();
		SteamCarrier carrier = Blob.seed(center, 120, SteamCarrier.class, Dungeon.level);
		Blob.seed(center, 10, Freezing.class, Dungeon.level);

		carrier.act();
		check(Blob.volumeAt(center, SteamCarrier.class) == 0
				&& Blob.volumeAt(center, Freezing.class) == 0,
				"steam acting first cancels frost");
		check(Dungeon.level.map[center] == Terrain.WATER,
				"steam and frost condense into water when steam acts first");
		check(Blob.volumeAt(center, WaterVapor.class) == 0,
				"frost-cancelled steam releases no water vapor");

		prepareLevel();
		Dungeon.level.buildFlagMaps();
		Blob.seed(center, 120, SteamCarrier.class, Dungeon.level);
		Freezing freezing = Blob.seed(center, 10, Freezing.class, Dungeon.level);

		freezing.act();
		check(Blob.volumeAt(center, SteamCarrier.class) == 0
				&& Blob.volumeAt(center, Freezing.class) == 0,
				"ten points of frost cancel a hundred and twenty points of steam");
		check(Dungeon.level.map[center] == Terrain.WATER,
				"steam and frost condense into water when frost acts first");
	}

	private static void snapFreezeCancelsSteam() {
		prepareLevel();
		int center = cell(5, 5);
		Dungeon.level.buildFlagMaps();
		Blob.seed(center, 120, SteamCarrier.class, Dungeon.level);

		//极速冰冻合剂走的是 Freezing.affect 这条老路径，不产生冰霜气体
		Freezing.affect(center);
		check(Blob.volumeAt(center, SteamCarrier.class) == 0,
				"snap freeze clears the steam on its cell");
		check(Dungeon.level.map[center] == Terrain.WATER,
				"snap freeze condenses steam into water");
	}

	private static void purificationBlocksSteam() {
		check(new BlobImmunity().immunities().contains(SteamCarrier.class)
				&& new BlobImmunity().immunities().contains(WaterVapor.class),
				"purification covers the steam gas and the damage it deals");

		prepareLevel();
		Dungeon.level.buildFlagMaps();
		DamageCounter target = new DamageCounter();
		target.HT = target.HP = 100;
		target.pos = cell(5, 5);
		target.viewDistance = 4;
		target.sprite = new CharSprite();
		Actor.add(target);
		Buff.affect(target, BlobImmunity.class);

		WaterVapor vapor = Blob.seed(cell(5, 5), 2, WaterVapor.class, Dungeon.level);
		vapor.act();
		check(target.damageEvents == 0, "purification blocks steam damage");
		Actor.remove(target);
	}

	public static void main( String[] args ) {
		WarriorTalentsRegression.setupHeadless();
		fireEvaporatesWater();
		steamBrewUsesCarrierGas();
		waterVaporSettlesOncePerTick();
		steamAndBlizzardCondenseIntoWater();
		steamAndFrostCondenseIntoWater();
		snapFreezeCancelsSteam();
		purificationBlocksSteam();
		roseCleansesGhostEquipment();
		System.out.println("PASS: dried rose curse cleansing and one-turn water vapor");
	}
}
