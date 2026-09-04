package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.WornDartTrap;
import com.watabou.utils.SparseArray;

import java.util.ArrayList;
import java.util.HashSet;

/** Standalone regression runner; requires no graphics context or test library. */
public class TestStartRegression {

	public static void main(String[] args) {
		com.watabou.noosa.Game.version = "regression-test";
		Dungeon.level = null;
		Dungeon.depth = TestStart.START_DEPTH;
		Dungeon.branch = 0;
		Level level = new SewerLevel();
		level.setSize(9, 9);
		level.mobs = new HashSet<>();
		level.blobs = new java.util.HashMap<>();
		level.heaps = new SparseArray<>();
		level.plants = new SparseArray<>();
		level.traps = new SparseArray<>();
		level.transitions = new ArrayList<>();
		LevelTransition entrance = new LevelTransition(level, 20, LevelTransition.Type.REGULAR_ENTRANCE);
		entrance.set(1, 1, 3, 3);
		level.transitions.add(entrance);
		for (int y = 1; y < 8; y++) {
			for (int x = 1; x < 8; x++) level.map[x + y * 9] = Terrain.EMPTY;
		}
		level.buildFlagMaps();

		check(level.getTransition(30) == entrance, "inclusive transition bounds with no active level");
		check(level.getTransition(40) == null, "ordinary floor outside transition");
		// Queries must also use the receiver when a different-sized level is active.
		Dungeon.level = new SewerLevel();
		Dungeon.level.setSize(5, 5);
		check(level.getTransition(30) == entrance, "receiver coordinates, not active level coordinates");
		Dungeon.level = null;
		com.watabou.utils.PathFinder.setMapSize(9, 9);

		if (TestStart.ENABLED) {
			check(TestStart.spawnMobs(level, Dungeon.depth, Rat::new, 3) == 3, "three mobs");
			check(TestStart.spawnTraps(level, Dungeon.depth, WornDartTrap::new, 2, false) == 2, "two visible traps");
			check(TestStart.spawnTraps(level, Dungeon.depth, WornDartTrap::new, 1, true) == 1, "hidden trap");
			for (int cell : level.traps.keyArray()) {
				check(level.traps.get(cell) != null, "trap get/put round trip");
				check(level.map[cell] == (level.traps.get(cell).visible ? Terrain.TRAP : Terrain.SECRET_TRAP), "trap terrain");
				check(level.getTransition(cell) == null && level.findMob(cell) == null, "no overlaps");
			}
			level.mobs.forEach(mob -> check(level.getTransition(mob.pos) == null, "mob outside entrance"));
			check(TestStart.spawnMobs(level, Dungeon.depth + 1, Rat::new, 1) == 0, "depth filter");
			Dungeon.branch = 1;
			check(TestStart.spawnTraps(level, Dungeon.depth, WornDartTrap::new, 1, false) == 0, "branch filter");
			Dungeon.branch = 0;
			check(TestStart.spawnTraps(level, Dungeon.depth, WornDartTrap::new, 100, false) < 100, "full floor terminates");
		} else {
			check(TestStart.spawnMobs(level, Dungeon.depth, Rat::new, 3) == 0, "disabled mobs");
			check(TestStart.spawnTraps(level, Dungeon.depth, WornDartTrap::new, 2, false) == 0, "disabled traps");
		}
		check(Dungeon.level == null, "generation does not install an active level");
		System.out.println("PASS: transition queries and test spawns; ENABLED=" + TestStart.ENABLED);
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
