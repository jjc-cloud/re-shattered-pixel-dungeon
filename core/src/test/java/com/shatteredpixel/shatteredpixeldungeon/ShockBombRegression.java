package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.ShockBomb;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.PrisonLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/** Standalone checks for the shock bomb only destroying creatures that can see it. */
public class ShockBombRegression {

	private static final int WIDTH = 11;
	private static final int BOMB = cell(5, 5);

	private static class Target extends Char {
		int damageTaken;

		@Override
		public void damage(int dmg, Object src) {
			damageTaken += dmg;
		}
	}

	private static int cell(int x, int y) {
		return x + y * WIDTH;
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}

	private static void prepareLevel() {
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
		level.buildFlagMaps();
		level.cleanWalls();

		Hero hero = new Hero();
		hero.heroClass = HeroClass.WARRIOR;
		Talent.initClassTalents(hero);
		hero.HP = hero.HT = 20;
		hero.pos = cell(1, 1);
		Dungeon.hero = hero;
		Dungeon.level = level;
	}

	//the hero's FOV is deliberately left uncalculated, which also keeps the explosion's visuals out of the test
	private static Target target(int x, int y) {
		Target t = new Target();
		t.pos = cell(x, y);
		t.HP = t.HT = 20;
		t.alignment = Char.Alignment.ENEMY;
		t.fieldOfView = new boolean[Dungeon.level.length()];
		Dungeon.level.updateFieldOfView(t, t.fieldOfView);
		return t;
	}

	private static void explode(Target t) {
		Actor.add(t);
		new ShockBomb().explode(BOMB);
		Actor.remove(t);
	}

	public static void main(String[] args) {
		WarriorTalentsRegression.setupHeadless();

		//看得见炸弹的单位照旧受伤
		prepareLevel();
		Target sighted = target(8, 5);
		check(sighted.fieldOfView[BOMB], "a target in the open can see the bomb cell");
		explode(sighted);
		check(sighted.damageTaken > 0, "a target that can see the bomb is damaged");

		//失明只是"视野变小"的一个来源：失明者贴身仍有感知，所以紧挨炸弹照旧受伤
		prepareLevel();
		Target blindAdjacent = target(6, 5);
		Buff.affect(blindAdjacent, Blindness.class);
		Dungeon.level.updateFieldOfView(blindAdjacent, blindAdjacent.fieldOfView);
		check(blindAdjacent.fieldOfView[BOMB], "blindness still lets a creature sense the adjacent bomb cell");
		explode(blindAdjacent);
		check(blindAdjacent.damageTaken > 0, "a blind creature right next to the bomb is still damaged");

		//只有失明真的把炸弹挡在视野之外时，才免伤
		prepareLevel();
		Target blindAway = target(7, 5);
		Buff.affect(blindAway, Blindness.class);
		Dungeon.level.updateFieldOfView(blindAway, blindAway.fieldOfView);
		check(!blindAway.fieldOfView[BOMB], "a blind creature two tiles away cannot sense the bomb cell");
		explode(blindAway);
		check(blindAway.damageTaken == 0, "a blind creature that cannot see the bomb takes no damage");

		//视野够不到炸弹，就不该受伤
		prepareLevel();
		Target shortSighted = target(8, 5);
		shortSighted.viewDistance = 2;
		Dungeon.level.updateFieldOfView(shortSighted, shortSighted.fieldOfView);
		check(!shortSighted.fieldOfView[BOMB], "a short-sighted target cannot see the bomb cell");
		explode(shortSighted);
		check(shortSighted.damageTaken == 0, "a target that cannot see the bomb takes no damage");

		//视线被墙挡住，就不该受伤
		prepareLevel();
		Dungeon.level.map[cell(7, 5)] = Terrain.WALL;
		Dungeon.level.buildFlagMaps();
		Dungeon.level.cleanWalls();
		Target aroundCorner = target(9, 5);
		check(!aroundCorner.fieldOfView[BOMB], "a wall blocks the target's view of the bomb");
		explode(aroundCorner);
		check(aroundCorner.damageTaken == 0, "a target whose view is blocked takes no damage");

		System.out.println("PASS: shock bomb only hits creatures that can see it");
	}
}
