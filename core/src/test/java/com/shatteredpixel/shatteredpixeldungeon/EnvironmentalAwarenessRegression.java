package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.VaultFlameTraps;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.levels.CavesBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.CavesLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.CityLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.PrisonLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.PrisonPainter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.MagicalFireRoom;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/** Standalone checks for terrain light ranges and line-of-sight clipping. */
public class EnvironmentalAwarenessRegression {
	private static class RippleTrackingSprite extends CharSprite {
		private boolean rippleShown;

		private boolean canShowRipple( int cell ) {
			return canShowWaterRipple(cell);
		}

		@Override
		public void showWaterRipple( int cell ) {
			rippleShown = true;
		}

		@Override
		public void turnTo( int from, int to ) {
		}

		@Override
		public void place( int cell ) {
		}
	}

	private static class TestRat extends Rat {
		private boolean moveSpriteForTest( int from, int to ) {
			return moveSprite(from, to);
		}
	}

	private static class TestPrisonPainter extends PrisonPainter {
		private static boolean isCandidate( int[] map, int cell, int width ) {
			return isTorchWallCandidate(map, cell, width);
		}
	}

	private static final int WIDTH = 21;

	private static int cell( int x, int y ) {
		return x + y * WIDTH;
	}

	private static void check( boolean condition, String message ) {
		if (!condition) throw new AssertionError(message);
	}

	private static Hero prepare( Level level ) {
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
		hero.pos = cell(2, 7);
		hero.viewDistance = 2;
		Dungeon.hero = hero;
		Dungeon.level = level;
		return hero;
	}

	private static void prisonLight() {
		PrisonLevel level = new PrisonLevel();
		prepare(level);
		level.map[cell(11, 7)] = Terrain.WALL_DECO;
		level.buildFlagMaps();
		Dungeon.observe(3);

		check(level.heroFOV[cell(11, 6)], "prison torch expands vision beyond eight tiles");
		check(level.environmentalViewDistance() > 8, "environmental view bounds include distant light");
		check(level.visited[cell(11, 6)], "expanded environmental vision updates explored tiles");
		check(!level.heroFOV[cell(12, 7)], "prison torch does not reveal behind its wall");
		check(!level.heroFOV[cell(10, 5)], "prison torch does not light beyond 3x3");
	}

	private static void prisonTorchDirections() {
		int center = cell(10, 10);
		for (int backingOffset : new int[]{-WIDTH, WIDTH, -1, 1}) {
			PrisonLevel level = new PrisonLevel();
			prepare(level);
			level.map[center] = Terrain.WALL;
			level.map[center + backingOffset] = Terrain.WALL;
			check(TestPrisonPainter.isCandidate(level.map, center, WIDTH),
					"prison torch supports every wall-facing direction");
		}

		PrisonLevel level = new PrisonLevel();
		prepare(level);
		level.map[center] = Terrain.WALL;
		check(!TestPrisonPainter.isCandidate(level.map, center, WIDTH),
				"isolated walls are not torch candidates");

		level.map[center - WIDTH] = Terrain.WALL;
		level.map[center - 1] = Terrain.DOOR;
		check(!TestPrisonPainter.isCandidate(level.map, center, WIDTH),
				"walls beside doors are not torch candidates");
	}

	private static void cityLight() {
		CityLevel level = new CityLevel();
		Hero hero = prepare(level);
		level.map[cell(11, 7)] = Terrain.REGION_DECO;
		level.buildFlagMaps();
		level.updateFieldOfView(hero, level.heroFOV);

		check(level.heroFOV[cell(11, 5)], "eternal pedestal expands vision with its 5x5 light");
		check(!level.heroFOV[cell(11, 4)], "eternal pedestal does not light beyond 5x5");
	}

	private static void activeGroundLights() {
		PrisonLevel level = new PrisonLevel();
		Hero hero = prepare(level);
		level.buildFlagMaps();

		Blob.seed(cell(12, 5), 2, Fire.class, level);
		Blob.seed(cell(12, 7), 1, MagicalFireRoom.EternalFire.class, level);
		Blob.seed(cell(12, 9), 2, VaultFlameTraps.class, level);
		level.updateFieldOfView(hero, level.heroFOV);

		check(level.heroFOV[cell(12, 5)], "active ground fire reveals its own tile");
		check(level.heroFOV[cell(12, 7)], "eternal fire reveals its own tile");
		check(level.heroFOV[cell(12, 9)], "active periodic flame reveals its own tile");
		check(!level.heroFOV[cell(12, 6)], "point light does not reveal neighboring tiles");
	}

	private static void caveLights() {
		CavesLevel level = new CavesLevel();
		Hero hero = prepare(level);
		level.map[cell(12, 5)] = Terrain.GRASS;
		level.map[cell(8, 7)] = Terrain.HIGH_GRASS;
		level.map[cell(9, 7)] = Terrain.HIGH_GRASS;
		level.buildFlagMaps();
		level.updateFieldOfView(hero, level.heroFOV);

		check(level.heroFOV[cell(12, 5)], "fluorescent moss reveals its own tile");
		check(level.heroFOV[cell(8, 7)], "outer fluorescent mushrooms are visible");
		check(!level.heroFOV[cell(9, 7)], "fluorescent mushrooms still block the interior");
	}

	private static void pylonEnergyLight() {
		CavesBossLevel level = new CavesBossLevel();
		Hero hero = prepare(level);
		level.buildFlagMaps();
		Blob.seed(cell(12, 7), 1, CavesBossLevel.PylonEnergy.class, level);
		level.updateFieldOfView(hero, level.heroFOV);

		check(level.heroFOV[cell(12, 7)], "active pylon energy reveals its own tile");
		check(!level.heroFOV[cell(12, 6)], "pylon energy does not reveal neighboring tiles");
	}

	private static void mappedWaterRipple() {
		PrisonLevel level = new PrisonLevel();
		prepare(level);
		int from = cell(6, 7);
		int to = cell(7, 7);
		level.map[from] = Terrain.WATER;
		level.map[to] = Terrain.WATER;
		level.buildFlagMaps();
		level.mapped[from] = true;

		TestRat rat = new TestRat();
		rat.pos = from;
		RippleTrackingSprite sprite = new RippleTrackingSprite();
		sprite.visible = false;
		sprite.ch = rat;
		rat.sprite = sprite;

		check(sprite.canShowRipple(from), "mapped water supports environmental ripple awareness");
		rat.moveSpriteForTest(from, to);
		check(sprite.rippleShown, "hidden movement still requests a ripple on known water");
	}

	public static void main( String[] args ) {
		com.watabou.noosa.Game.version = "regression-test";
		prisonTorchDirections();
		prisonLight();
		cityLight();
		activeGroundLights();
		caveLights();
		pylonEnergyLight();
		mappedWaterRipple();
		System.out.println("PASS: environmental lighting ranges, sources, and occlusion");
	}
}
