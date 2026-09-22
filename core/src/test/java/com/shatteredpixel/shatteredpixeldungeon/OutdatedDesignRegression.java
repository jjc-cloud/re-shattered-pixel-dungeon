package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Foresight;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.TrapMechanism;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.RegularPainter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;

import java.util.ArrayList;
import java.util.Arrays;

/** Standalone checks of the 隐蔽设计 (OUTDATED_DESIGN) challenge after implementation:
 *  passive trap discovery is disabled, and the hidden traps are doubled while the traps which are
 *  already visible when the level is generated stay untouched. */
public class OutdatedDesignRegression {

	private static final int CHALLENGE = Challenges.OUTDATED_DESIGN;

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}

	private static class FoundSecret extends RuntimeException { }

	/**
	 * Returns whether a hidden secret on the neighbouring cell was discovered. The level throws as
	 * soon as it is discovered, which also skips the sprite calls that need a live scene.
	 */
	private static boolean search(boolean intentional, boolean foresight, HeroClass type, int lvl,
	                              int terrain, float roll, boolean challenged) {
		Level previousLevel = Dungeon.level;
		Hero previousHero = Dungeon.hero;
		int previousChallenges = Dungeon.challenges;
		try {
			Dungeon.challenges = challenged ? CHALLENGE : 0;
			Dungeon.level = new SewerLevel() {
				@Override public void discover(int cell) { throw new FoundSecret(); }
			};
			Dungeon.level.setSize(9, 9);
			Dungeon.level.traps = new SparseArray<>();
			Dungeon.level.locked = false;
			Dungeon.depth = 1;
			Dungeon.branch = 0;

			Hero hero = new Hero();
			Dungeon.hero = hero;
			hero.heroClass = type;
			hero.subClass = HeroSubClass.NONE;
			hero.HP = hero.HT = 1000;
			hero.lvl = lvl;
			hero.pos = 20;
			hero.fieldOfView = new boolean[81];
			hero.fieldOfView[21] = true;
			Talent.initClassTalents(hero);
			Talent.initSubclassTalents(hero);

			if (foresight) {
				//the buff searches on attach, so the secret must not be on the map yet
				Buff.affect(hero, Foresight.class);
			}
			Dungeon.level.map[21] = terrain;
			Dungeon.level.secret[21] = true;

			long seed = 0;
			while (true) {
				Random.pushGenerator(seed);
				float candidate = Random.Float();
				Random.popGenerator();
				if (candidate > roll && candidate < roll + .01f) break;
				seed++;
			}
			Random.pushGenerator(seed);
			try {
				hero.search(intentional);
				return false;
			} catch (FoundSecret found) {
				return true;
			} finally {
				Random.popGenerator();
			}
		} finally {
			Dungeon.level = previousLevel;
			Dungeon.hero = previousHero;
			Dungeon.challenges = previousChallenges;
		}
	}

	private static void passiveDiscovery() {
		int trap = Terrain.SECRET_TRAP;
		int door = Terrain.SECRET_DOOR;

		//baseline: the passive paths still work without the challenge
		check(search(false, false, HeroClass.WARRIOR, 1, trap, .30f, false),
				"passive trap detection unchanged without the challenge");
		check(search(false, false, HeroClass.ROGUE, 1, trap, .42f, false),
				"rogue awareness bonus unchanged without the challenge");
		check(search(false, false, HeroClass.WARRIOR, 1, door, .15f, false),
				"passive door detection unchanged without the challenge");

		//challenge: hidden traps are never noticed passively, the rogue bonus included
		check(!search(false, false, HeroClass.WARRIOR, 1, trap, .30f, true),
				"the challenge blocks passive trap detection");
		check(!search(false, false, HeroClass.ROGUE, 1, trap, .42f, true),
				"the challenge overrides the rogue awareness bonus");

		//hidden doors are unaffected
		check(search(false, false, HeroClass.WARRIOR, 1, door, .15f, true),
				"the challenge does not block passive door detection");

		//the ways a player can still find traps on purpose
		check(search(true, false, HeroClass.WARRIOR, 1, trap, .99f, true),
				"intentional search still reveals traps");
		check(search(false, true, HeroClass.WARRIOR, 1, trap, .99f, true),
				"foresight still reveals traps passively");
	}

	/** Paints traps on a synthetic room, using only traps that can actually be hidden.
	 *  Must be public, as painters instantiate their trap classes reflectively. */
	public static class TestTrap extends Trap {
		@Override public void activate() { }
	}

	public static class TrapPainter extends RegularPainter {
		@Override protected void decorate(Level level, ArrayList<Room> rooms) { }
		void paint(Level level) { paintTraps(level, new ArrayList<Room>()); }
	}

	private static final long TRAP_SEED = 1234567L;

	/** @return {visible traps, hidden traps, outdated traps among the hidden ones} */
	private static int[] paintedTraps(int size, boolean challenged, boolean trapsFeeling,
	                                  int trinketLevel, int nTraps) {
		int previousChallenges = Dungeon.challenges;
		Hero previousHero = Dungeon.hero;
		try {
			Dungeon.challenges = challenged ? CHALLENGE : 0;
			Dungeon.depth = 1;
			Dungeon.branch = 0;
			Dungeon.hero = null;
			if (trinketLevel >= 0) {
				Hero hero = new Hero();
				Dungeon.hero = hero;
				hero.heroClass = HeroClass.WARRIOR;
				TrapMechanism trinket = new TrapMechanism();
				if (trinketLevel > 0) trinket.upgrade(trinketLevel);
				hero.belongings.backpack.items.add(trinket);
				check(Math.abs(TrapMechanism.revealHiddenTrapChance() - (0.1f * (trinketLevel + 1))) < 0.0001f,
						"test trinket level " + trinketLevel + " reveals " + TrapMechanism.revealHiddenTrapChance());
			}

			Level level = new SewerLevel();
			level.setSize(size, size);
			level.traps = new SparseArray<>();
			Arrays.fill(level.map, Terrain.WALL);
			for (int y = 1; y < size - 1; y++) {
				for (int x = 1; x < size - 1; x++) {
					level.map[x + y * size] = Terrain.EMPTY;
				}
			}
			level.feeling = trapsFeeling ? Level.Feeling.TRAPS : Level.Feeling.NONE;

			TrapPainter painter = new TrapPainter();
			painter.setTraps(nTraps, new Class<?>[]{TestTrap.class}, new float[]{1f});

			Random.pushGenerator(TRAP_SEED);
			try {
				painter.paint(level);
			} finally {
				Random.popGenerator();
			}

			int visible = 0, hidden = 0, outdated = 0;
			for (int i = 0; i < level.length(); i++) {
				if (level.map[i] == Terrain.TRAP) {
					visible++;
				} else if (level.map[i] == Terrain.SECRET_TRAP) {
					hidden++;
					Trap trap = level.traps.get(i);
					if (trap != null && trap.outdated) outdated++;
				}
			}
			return new int[]{visible, hidden, outdated};
		} finally {
			Dungeon.challenges = previousChallenges;
			Dungeon.hero = previousHero;
		}
	}

	/** Interior cells of a size x size level. */
	private static int interior(int size) {
		return (size - 2) * (size - 2);
	}

	private static void trapCounts() {
		//plain level: every trap is hidden when there is no trinket to reveal it
		int[] plain = paintedTraps(24, false, false, -1, 9);
		int[] challenged = paintedTraps(24, true, false, -1, 9);
		check(plain[0] == 0 && plain[1] == 9 && challenged[0] == 0,
				"baseline without trinket hides all nine traps: " + Arrays.toString(plain) + " " + Arrays.toString(challenged));
		check(challenged[0] == plain[0], "the challenge does not change the visible trap count");
		check(challenged[1] == 2 * plain[1], "the challenge doubles the hidden trap count");
		check(challenged[2] == challenged[1] / 2, "half of the hidden traps are outdated");

		//traps feeling: the four extra sets of traps are visible on generation, and stay that way
		plain = paintedTraps(24, false, true, -1, 9);
		challenged = paintedTraps(24, true, true, -1, 9);
		check(plain[0] == 36 && plain[1] == 9,
				"baseline traps feeling shows 4x traps: " + Arrays.toString(plain));
		check(challenged[0] == plain[0], "the challenge leaves the visible traps of a traps feeling untouched");
		check(challenged[1] == 2 * plain[1], "the challenge doubles the hidden traps of a traps feeling");
		check(challenged[2] == challenged[1] / 2, "half of the hidden traps are outdated on a traps feeling");

		//trap mechanism: a level four trinket turns half of the rolled traps visible at generation
		plain = paintedTraps(24, false, true, 4, 9);
		challenged = paintedTraps(24, true, true, 4, 9);
		check(plain[0] == 40 && plain[1] == 5, "trinket reveals four of the nine rolled traps: " + Arrays.toString(plain));
		check(challenged[0] == plain[0], "the challenge keeps the traps the trinket revealed visible");
		check(challenged[1] == 2 * plain[1], "the challenge only doubles the traps the trinket left hidden");
		check(challenged[2] < challenged[1], "a trinket reduces the outdated traps there are to learn");

		//a level so small that the base traps fill it: the doubling is capped instead of crashing
		plain = paintedTraps(8, false, true, -1, 999);
		challenged = paintedTraps(8, true, true, -1, 999);
		check(plain[1] == 7 && plain[0] == 28 && plain[0] + plain[1] == interior(8) - 1,
				"a tiny level places one trap per five cells: " + Arrays.toString(plain));
		check(challenged[0] == plain[0], "the challenge still leaves the visible traps untouched on a tiny level");
		check(challenged[1] == plain[1] + 1 && challenged[1] + challenged[0] == interior(8),
				"the doubling is capped by the cells that are left: " + Arrays.toString(challenged));

		//a level with fewer than five trap cells gets no traps at all, challenge or not
		plain = paintedTraps(4, false, true, -1, 999);
		challenged = paintedTraps(4, true, true, -1, 999);
		check(plain[0] == 0 && plain[1] == 0 && challenged[0] == 0 && challenged[1] == 0,
				"a level with fewer than five trap cells gets no traps: "
						+ Arrays.toString(plain) + " " + Arrays.toString(challenged));
	}

	public static void main(String[] args) throws Exception {
		com.badlogic.gdx.utils.GdxNativesLoader.load();
		new com.watabou.noosa.Game(com.watabou.noosa.Scene.class, null);
		WarriorTalentsRegression.setupHeadless();
		com.watabou.utils.GameSettings.set((com.badlogic.gdx.Preferences) java.lang.reflect.Proxy.newProxyInstance(
				OutdatedDesignRegression.class.getClassLoader(), new Class[]{com.badlogic.gdx.Preferences.class}, (proxy, method, values) -> {
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

		passiveDiscovery();
		trapCounts();
		System.out.println("PASS: 隐蔽设计 passive trap discovery blocked, hidden traps doubled, visible traps unchanged");
	}
}
