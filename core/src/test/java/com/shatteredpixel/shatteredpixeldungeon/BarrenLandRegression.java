package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Regrowth;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.RegrowthBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE;
import com.shatteredpixel.shatteredpixeldungeon.plants.BlandfruitBush;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;
import com.watabou.utils.SparseArray;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/** Standalone checks of the 荒芜之地 (NO_HERBALISM) challenge after implementation: only the plants
 *  which have a seed of their own are removed, the seedless specials (无味果/结露草/种子荚) still
 *  grow, and the wand of regrowth only grows grass (its 黄金莲 lotus included) instead of spending
 *  cells on plants which the challenge would remove again. */
public class BarrenLandRegression {

	private static final int CHALLENGE = Challenges.NO_HERBALISM;

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}

	/** A level whose interior is plain empty terrain, ready to be planted on. */
	private static Level emptyLevel(int size) {
		Level level = new SewerLevel();
		level.setSize(size, size);
		level.mobs = new HashSet<>();
		level.heaps = new SparseArray<>();
		level.blobs = new HashMap<>();
		level.plants = new SparseArray<>();
		level.traps = new SparseArray<>();
		Dungeon.depth = 1;
		Dungeon.branch = 0;
		for (int y = 1; y < size - 1; y++) {
			for (int x = 1; x < size - 1; x++) {
				level.map[x + y * size] = Terrain.EMPTY;
			}
		}
		//marks the outer ring solid, which the cell flag updates expect
		level.buildFlagMaps();
		return level;
	}

	/** Plants a seed with the challenge on or off, restoring the previous state afterwards. */
	private static Plant plant(Level level, Plant.Seed seed, int pos, boolean challenged) {
		Level previousLevel = Dungeon.level;
		int previousChallenges = Dungeon.challenges;
		try {
			Dungeon.level = level;
			Dungeon.challenges = challenged ? CHALLENGE : 0;
			return level.plant(seed, pos);
		} finally {
			Dungeon.level = previousLevel;
			Dungeon.challenges = previousChallenges;
		}
	}

	/** The special plants which never drop a seed, and so have no seed to be removed. */
	private static Class<? extends Plant.Seed>[] seedlessSeeds() {
		return new Class[]{
				BlandfruitBush.Seed.class,
				WandOfRegrowth.Dewcatcher.Seed.class,
				WandOfRegrowth.Seedpod.Seed.class};
	}

	private static void plantRule() {
		Level level = emptyLevel(9);
		int pos = 4 + 4 * 9;

		//every plant which has a seed of its own is removed
		for (Class<?> cls : Generator.Category.SEED.classes) {
			Level.set(pos, Terrain.EMPTY, level);
			check(plant(level, (Plant.Seed) Reflection.newInstance(cls), pos, true) == null,
					cls.getSimpleName() + " is not planted under the challenge");
			check(level.plants.get(pos) == null,
					cls.getSimpleName() + " leaves no plant behind under the challenge");
			check(level.map[pos] == Terrain.GRASS,
					cls.getSimpleName() + " still turns its cell into grass, as levelgen relies on it");
		}

		//the rule keys off the seed pool, so the seedless specials have to stay out of it
		ArrayList<Class<?>> pool = new ArrayList<>(Arrays.asList(Generator.Category.SEED.classes));

		//the seedless specials are still generated
		for (Class<? extends Plant.Seed> cls : seedlessSeeds()) {
			check(!pool.contains(cls), cls.getSimpleName() + " stays out of the seed pool");
			Level.set(pos, Terrain.EMPTY, level);
			Plant plant = plant(level, Reflection.newInstance(cls), pos, true);
			check(plant != null, cls.getSimpleName() + " is still planted under the challenge");
			check(level.plants.get(pos) == plant,
					cls.getSimpleName() + " is registered as a plant under the challenge");
		}
	}

	private static void withoutChallenge() {
		Level level = emptyLevel(9);
		int pos = 4 + 4 * 9;

		ArrayList<Class<? extends Plant.Seed>> seeds = new ArrayList<>(Arrays.asList(seedlessSeeds()));
		seeds.add(Sungrass.Seed.class);

		for (Class<? extends Plant.Seed> cls : seeds) {
			Level.set(pos, Terrain.EMPTY, level);
			Plant plant = plant(level, Reflection.newInstance(cls), pos, false);
			check(plant != null && level.plants.get(pos) == plant,
					cls.getSimpleName() + " is planted without the challenge");
		}
	}

	private static final long WAND_SEED = 9876543L;

	/** @return {plants, lotus, grass cells, high grass cells, cells of the cone} after the wand zap */
	private static int[] zap(boolean challenged, int distance) throws Exception {
		Level previousLevel = Dungeon.level;
		Hero previousHero = Dungeon.hero;
		int previousChallenges = Dungeon.challenges;
		try {
			Dungeon.challenges = challenged ? CHALLENGE : 0;
			Level level = emptyLevel(12);
			Dungeon.level = level;

			Hero hero = new Hero();
			Dungeon.hero = hero;
			hero.heroClass = HeroClass.WARRIOR;
			hero.HP = hero.HT = 100;
			hero.lvl = 1;
			hero.pos = 2 + 6 * 12;
			Talent.initClassTalents(hero);
			Talent.initSubclassTalents(hero);

			//a short bolt, so the cone holds fewer cells than the grass the wand wants to place
			int target = hero.pos + distance;
			Ballistica bolt = new Ballistica(hero.pos, target, Ballistica.STOP_SOLID | Ballistica.STOP_TARGET);
			ConeAOE cone = new ConeAOE(bolt, 2 + 2 * 3, 20 + 10 * 3, Ballistica.STOP_SOLID | Ballistica.STOP_TARGET);

			WandOfRegrowth wand = new WandOfRegrowth();
			wand.curCharges = 10; //chargesPerCast() == 3, so the wand spawns its lotus
			setCone(wand, cone);

			Random.pushGenerator(WAND_SEED);
			try {
				wand.onZap(bolt);
			} finally {
				Random.popGenerator();
			}

			int plants = level.plants.valueList().size();
			int lotus = 0;
			for (Mob mob : level.mobs) {
				if (mob instanceof WandOfRegrowth.Lotus) lotus++;
			}
			int grass = 0, highGrass = 0;
			for (int i = 0; i < level.length(); i++) {
				if (level.map[i] == Terrain.HIGH_GRASS) {
					grass++;
					highGrass++;
				} else if (level.map[i] == Terrain.GRASS || level.map[i] == Terrain.FURROWED_GRASS) {
					grass++;
				}
			}
			System.out.println("barren land " + (challenged ? "on " : "off") + " (bolt " + distance + "): plants="
					+ plants + " lotus=" + lotus + " grass=" + grass + "/" + cone.cells.size() + " high=" + highGrass);
			return new int[]{plants, lotus, grass, highGrass, cone.cells.size()};
		} finally {
			Dungeon.level = previousLevel;
			Dungeon.hero = previousHero;
			Dungeon.challenges = previousChallenges;
		}
	}

	/** onZap reads the cone which fx() normally builds, but the sprite effects in fx() need a live
	 *  scene, so the cone is injected directly here. */
	private static void setCone(WandOfRegrowth wand, ConeAOE cone) throws Exception {
		Field field = WandOfRegrowth.class.getDeclaredField("cone");
		field.setAccessible(true);
		field.set(wand, cone);
	}

	private static void wandOnlyGrowsGrass() throws Exception {
		int[] on = zap(true, 2);
		int[] off = zap(false, 2);

		check(off[0] >= 1, "the wand grows plants without the challenge: " + Arrays.toString(off));
		check(on[0] == 0, "the wand grows no plants under the challenge: " + Arrays.toString(on));
		check(on[1] == 1 && off[1] == 1,
				"the wand still grows its lotus: " + Arrays.toString(on) + " " + Arrays.toString(off));
		check(on[2] == on[4], "every cell of the cone grows grass under the challenge: " + Arrays.toString(on));
		check(on[3] > off[3],
				"the cells no longer spent on plants go to the grass: " + Arrays.toString(on) + " " + Arrays.toString(off));
	}

	private static final long BOMB_SEED = 24681357L;

	/** @return {plants, seedless specials, regrowth volume} after the 再生炸弹 explodes */
	private static int[] bomb(boolean challenged) {
		Level previousLevel = Dungeon.level;
		int previousChallenges = Dungeon.challenges;
		try {
			Dungeon.challenges = challenged ? CHALLENGE : 0;
			Level level = emptyLevel(12);
			Dungeon.level = level;

			Random.pushGenerator(BOMB_SEED);
			try {
				new RegrowthBomb().explode(6 + 6 * 12);
			} finally {
				Random.popGenerator();
			}

			int plants = 0, seedless = 0;
			for (Plant plant : level.plants.valueList()) {
				plants++;
				if (plant instanceof BlandfruitBush
						|| plant instanceof WandOfRegrowth.Dewcatcher
						|| plant instanceof WandOfRegrowth.Seedpod) {
					seedless++;
				}
			}
			Blob regrowth = level.blobs.get(Regrowth.class);
			int volume = regrowth == null ? 0 : regrowth.volume;

			System.out.println("regrowth bomb " + (challenged ? "on " : "off") + ": plants=" + plants
					+ " seedless=" + seedless + " regrowth=" + volume);
			return new int[]{plants, seedless, volume};
		} finally {
			Dungeon.level = previousLevel;
			Dungeon.challenges = previousChallenges;
		}
	}

	private static void bombOnlyGrowsGrass() {
		int[] on = bomb(true);
		int[] off = bomb(false);

		check(off[0] >= 1, "the bomb grows plants without the challenge: " + Arrays.toString(off));
		check(off[1] >= 1, "the bomb grows its seedless special without the challenge: " + Arrays.toString(off));
		check(on[0] == 0, "the bomb grows no plants under the challenge: " + Arrays.toString(on));
		check(on[2] > 0 && on[2] == off[2], "the bomb still spreads its regrowth under the challenge: "
				+ Arrays.toString(on) + " " + Arrays.toString(off));
	}

	public static void main(String[] args) throws Exception {
		com.badlogic.gdx.utils.GdxNativesLoader.load();
		new com.watabou.noosa.Game(com.watabou.noosa.Scene.class, null);
		WarriorTalentsRegression.setupHeadless();
		com.watabou.utils.GameSettings.set((com.badlogic.gdx.Preferences) java.lang.reflect.Proxy.newProxyInstance(
				BarrenLandRegression.class.getClassLoader(), new Class[]{com.badlogic.gdx.Preferences.class}, (proxy, method, values) -> {
					if (method.getName().equals("getString")) return "zh";
					if (method.getName().equals("getBoolean")) return false;
					if (method.getName().equals("getInteger")) return values.length > 1 ? values[1] : 0;
					if (method.getName().startsWith("put")) return proxy;
					if (method.getName().equals("flush")) return null;
					throw new UnsupportedOperationException(method.getName());
				}));
		com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring.initGems();
		com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion.initColors();
		com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll.initLabels();

		plantRule();
		withoutChallenge();
		wandOnlyGrowsGrass();
		bombOnlyGrowsGrass();
		System.out.println("PASS: 荒芜之地 removes seed plants, keeps the seedless specials, and the wand of regrowth and regrowth bomb only grow grass");
	}
}
