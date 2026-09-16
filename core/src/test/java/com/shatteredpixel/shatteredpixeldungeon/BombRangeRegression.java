package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.StunBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.SuperBomb;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.PrisonLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/** Standalone checks for the blast range of the stun bomb and the super bomb. */
public class BombRangeRegression {

	private static final int WIDTH = 11;
	private static final int BOMB = cell(5, 5);

	private static class Target extends Char {
		int damageTaken;

		@Override
		public void damage(int dmg, Object src) {
			damageTaken += dmg;
		}
	}

	//英雄挨打会走 Hero.damage 里的中断逻辑，那需要游戏场景；
	//这里只关心眩晕，所以和 Target 一样把伤害拦下来。
	private static class TestHero extends Hero {
		@Override
		public void damage(int dmg, Object src) {
			//吞掉伤害
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

		Hero hero = new TestHero();
		hero.heroClass = HeroClass.WARRIOR;
		Talent.initClassTalents(hero);
		hero.HP = hero.HT = 200;
		hero.pos = cell(1, 1);
		Dungeon.hero = hero;
		Dungeon.level = level;
	}

	//the hero's FOV is deliberately left uncalculated, which also keeps the explosion's visuals out of the test
	private static Target enemy(int x, int y) {
		Target t = new Target();
		t.pos = cell(x, y);
		t.HP = t.HT = 100;
		t.alignment = Char.Alignment.ENEMY;
		t.fieldOfView = new boolean[Dungeon.level.length()];
		return t;
	}

	private static void explode(Target t, Bomb bomb) {
		Actor.add(t);
		bomb.explode(BOMB);
	}

	//buff 挂在角色身上，所以必须等断言都跑完再把它移出演员表：
	//Actor.remove 会一并卸掉角色的全部 buff，相当于角色离场。
	private static void release(Target t) {
		Actor.remove(t);
	}

	public static void main(String[] args) {
		WarriorTalentsRegression.setupHeadless();

		//眩晕炸弹：两格外的敌人要被炸到，并且吃满 20 回合眩晕
		prepareLevel();
		Target stunned = enemy(7, 5);
		explode(stunned, new StunBomb());
		check(stunned.damageTaken > 0, "the stun bomb reaches an enemy two tiles away");
		Vertigo vertigo = stunned.buff(Vertigo.class);
		check(vertigo != null, "an enemy two tiles away is stunned");
		check(vertigo.cooldown() == 20f,
				"the stun lasts exactly 20 turns, was " + (vertigo == null ? -1f : vertigo.cooldown()));
		release(stunned);

		//对角线上的两格同样在范围之内
		prepareLevel();
		Target diagonal = enemy(7, 7);
		explode(diagonal, new StunBomb());
		check(diagonal.buff(Vertigo.class) != null, "the stun reaches diagonally");
		release(diagonal);

		//三格之外既不吃伤害也不吃眩晕
		prepareLevel();
		Target outside = enemy(8, 5);
		explode(outside, new StunBomb());
		check(outside.damageTaken == 0, "the stun bomb does not reach three tiles");
		check(outside.buff(Vertigo.class) == null, "an enemy three tiles away is not stunned");
		release(outside);

		//范围内所有单位都吃眩晕，不分敌我
		prepareLevel();
		Target ally = enemy(7, 5);
		ally.alignment = Char.Alignment.ALLY;
		explode(ally, new StunBomb());
		check(ally.damageTaken > 0, "the stun bomb still damages everything in its blast");
		check(ally.buff(Vertigo.class) != null, "an ally in the blast is stunned as well");
		release(ally);

		//英雄也在"所有单位"之内
		prepareLevel();
		Hero hero = Dungeon.hero;
		hero.pos = cell(7, 5);
		Actor.add(hero);
		new StunBomb().explode(BOMB);
		check(hero.buff(Vertigo.class) != null, "the hero's own stun bomb stuns the hero");
		Actor.remove(hero);

		//超级炸弹：同样范围 2
		prepareLevel();
		Target near = enemy(7, 5);
		explode(near, new SuperBomb());
		check(near.damageTaken > 0, "the super bomb reaches two tiles");
		release(near);

		prepareLevel();
		Target far = enemy(8, 5);
		explode(far, new SuperBomb());
		check(far.damageTaken == 0, "the super bomb does not reach three tiles");
		release(far);

		//对照：普通炸弹仍然是范围 1
		prepareLevel();
		Target plain = enemy(7, 5);
		explode(plain, new Bomb());
		check(plain.damageTaken == 0, "a plain bomb still only reaches one tile");
		release(plain);

		System.out.println("PASS: stun and super bombs use the standard alchemy bomb range");
	}
}
