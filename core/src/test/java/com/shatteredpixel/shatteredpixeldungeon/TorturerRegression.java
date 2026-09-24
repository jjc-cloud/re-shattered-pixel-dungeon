package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Guard;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.MobSpawner;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Torturer;
import com.shatteredpixel.shatteredpixeldungeon.effects.Effects;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.journal.Bestiary;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GuardSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.TorturerSprite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/** Headless checks for the torturer: guard stats, endless sight-based chains, a pull-then-strike turn, and the guaranteed chains drop. */
public class TorturerRegression {

	//the test level is a bare room; every cell below is interior, as buildFlagMaps() forces the border solid
	private static final int W = 13;

	private static final int MOB = 6 * W + 2;    //(2,6)  where both variants stand
	private static final int NEAR = 6 * W + 6;   //(6,6)  four tiles away, within a guard's reach
	private static final int FAR = 11 * W + 6;   //(6,11) five tiles away, only visible to a torturer
	private static final int FINAL = 2 * W + 5;  //(5,2)  stays out of the field of view

	private static final int FIRST = 6 * W + 3;  //(3,6)  first tile the chains can land on
	private static final int SECOND = 6 * W + 4; //(4,6)  the tile after it
	private static final int PAST = 6 * W + 11;  //(11,6) last interior tile of that row, where a wandering trajectory ends

	public static class Victim extends Char {
		Victim(int cell) {
			HP = HT = 100;
			pos = cell;
			sprite = new StubSprite();
		}
		@Override public void die(Object cause) { HP = 0; }
	}

	/** Pulling and striking only need a sprite to exist, it never needs to be drawn. */
	public static class StubSprite extends MobSprite {
		StubSprite() { visible = false; }
		@Override public void place(int cell) { }
		//there is no parent scene to hang emotion icons off of when running headless
		@Override public void showAlert() { }
		@Override public void hideAlert() { }
		@Override public void showLost() { }
		@Override public void hideLost() { }
		@Override public void showInvestigate() { }
		@Override public void hideInvestigate() { }
	}

	public static class TestGuard extends Guard {
		boolean chainsDropped = false;
		TestGuard() {
			fieldOfView = new boolean[Dungeon.level.length()];
			sprite = new StubSprite();
		}
		boolean available() { return chainsAvailable(); }
		boolean inRange(int cell) { return chainInRange(cell); }
		boolean reaches(Ballistica chain, int target) { return chainsReachTarget(chain, target); }
		Ballistica aim(int target) { return chainsBallistica(target); }
		Effects.Type effect() { return chainEffect(); }
		float chainCost() { float before = cooldown(); onChainUsed(); return cooldown() - before; }
		boolean tryChain(int target) { return chain(target); }
		void target(Char ch) { enemy = ch; }
		void roll() { rollForChainsDrop(); }
		@Override protected void dropChains() { chainsDropped = true; }
	}

	public static class TestTorturer extends Torturer {
		boolean chainsDropped = false;
		int chainsThrown = 0;
		int strikes = 0;
		private Char forcedEnemy = null;

		TestTorturer() {
			fieldOfView = new boolean[Dungeon.level.length()];
			sprite = new StubSprite();
		}
		boolean available() { return chainsAvailable(); }
		boolean inRange(int cell) { return chainInRange(cell); }
		boolean cripples() { return cripplesTarget(); }
		boolean reaches(Ballistica chain, int target) { return chainsReachTarget(chain, target); }
		Ballistica aim(int target) { return chainsBallistica(target); }
		Effects.Type effect() { return chainEffect(); }
		boolean tryChain(int target) { return chain(target); }
		void target(Char ch) { enemy = ch; }
		float chainCost() { float before = cooldown(); onChainUsed(); return cooldown() - before; }
		/**pretends enough time has gone by for the torturer to have taken another turn*/
		void startNextTurn() { spend(1f); }
		boolean actOnce() { return act(); }
		void forceEnemy(Char ch) { forcedEnemy = ch; }
		void roll() { rollForChainsDrop(); }

		@Override protected Char chooseEnemy() { return forcedEnemy != null ? forcedEnemy : super.chooseEnemy(); }
		@Override protected void onChainUsed() { chainsThrown++; super.onChainUsed(); }
		/**the damage roll is not what is under test here, only whether the torturer gets to strike at all*/
		@Override public boolean attack(Char enemy, float dmgMulti, float dmgBonus, float accMulti) {
			strikes++;
			return true;
		}
		@Override protected void dropChains() { chainsDropped = true; }
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}

	private static void near(float actual, float expected, String message) {
		check(Math.abs(actual - expected) < 0.0001f, message + ": " + actual);
	}

	public static void main(String[] args) {
		com.badlogic.gdx.utils.GdxNativesLoader.load();
		WarriorTalentsRegression.setupHeadless();

		//setupHeadless leaves behind a bare 9x9 room; the chain checks need a few more interior tiles
		Dungeon.hero = new Hero();
		Level level = Dungeon.level;
		level.setSize(W, W);
		level.mobs = new HashSet<>();
		level.heaps = new com.watabou.utils.SparseArray<>();
		level.blobs = new HashMap<>();
		level.plants = new com.watabou.utils.SparseArray<>();
		level.traps = new com.watabou.utils.SparseArray<>();
		level.customTiles = new ArrayList<>();
		Arrays.fill(level.map, Terrain.EMPTY);
		level.buildFlagMaps();

		check(MobSpawner.RARE_ALTS.get(Guard.class) == Torturer.class, "torturers are registered as the guard's rare variant");
		check(Bestiary.RARE.entities().contains(Torturer.class), "torturers are listed as a rare bestiary entry");

		Messages.setup(Languages.ENGLISH);
		check(!Messages.get(Torturer.class, "name").equals(Messages.NO_TEXT_FOUND)
						&& !Messages.get(Torturer.class, "desc").equals(Messages.NO_TEXT_FOUND),
				"torturers are named and described in english too");
		Messages.setup(Languages.CHI_SMPL);
		check(Messages.get(Torturer.class, "name").equals("拷问官"), "torturers are named 拷问官 in chinese");
		check(!Messages.get(Torturer.class, "desc").equals(Messages.NO_TEXT_FOUND), "torturers have a chinese description");

		Dungeon.LimitedDrops.GUARD_ARM.count = 0;
		Dungeon.LimitedDrops.GUARD_CHAINS.count = 0;
		TestTorturer stats = new TestTorturer();
		check(stats.HP == 40 && stats.HT == 40, "torturers keep the guard's health");
		check(stats.EXP == 7 && stats.maxLvl == 14, "torturers keep the guard's exp and level cap");
		check(stats.defenseSkill == 10 && stats.attackSkill(null) == 12, "torturers keep the guard's combat skills");
		check(Char.hasProp(stats, Char.Property.UNDEAD), "torturers keep the guard's undead property");
		int lowestDamage = Integer.MAX_VALUE;
		int highestDamage = Integer.MIN_VALUE;
		for (int i = 0; i < 200; i++) {
			int roll = stats.damageRoll();
			lowestDamage = Math.min(lowestDamage, roll);
			highestDamage = Math.max(highestDamage, roll);
		}
		check(lowestDamage >= 4 && highestDamage <= 12, "torturers keep the guard's damage range");
		check(stats.lootChance() == 0.2f, "torturers keep the guard's armor drop chance");
		check(stats.createLoot() instanceof Armor, "torturers keep the guard's armor loot");
		check(Dungeon.LimitedDrops.GUARD_ARM.count == 1, "torturer armor drops share the guard's limited-drop counter");
		check(!Dungeon.LimitedDrops.GUARD_CHAINS.dropped(), "armor drops do not consume the chains drop");

		TestGuard guard = new TestGuard();
		TestTorturer torturer = new TestTorturer();
		guard.pos = MOB;
		torturer.pos = MOB;
		guard.fieldOfView[NEAR] = true;
		torturer.fieldOfView[NEAR] = true;
		torturer.fieldOfView[FAR] = true;
		check(guard.available() && torturer.available(), "both variants start with chains ready");
		check(guard.inRange(NEAR), "guards still reach targets four tiles away");
		check(!guard.inRange(FAR), "guards cannot reach targets five tiles away");
		check(torturer.inRange(NEAR) && torturer.inRange(FAR), "torturers reach any target in their field of view");
		check(!torturer.inRange(FINAL), "torturers cannot reach targets they cannot see");
		check(!torturer.cripples(), "torturers do not cripple pulled targets");
		check(guard.effect() == Effects.Type.CHAIN, "guards draw their pull with the plain iron chain");
		check(torturer.effect() == Effects.Type.ETHEREAL_CHAIN, "torturers draw their pull with the ethereal chains");

		Ballistica guardAim = guard.aim(NEAR);
		Ballistica torturerAim = torturer.aim(NEAR);
		check(guardAim.collisionProperties == Ballistica.PROJECTILE, "guards still aim with a projectile trajectory");
		check(guardAim.collisionPos == NEAR, "a guard's trajectory stops on its target");
		check(guard.reaches(guardAim, NEAR), "guards need their trajectory to hit");
		check(torturerAim.collisionProperties == Ballistica.WONT_STOP, "torturers aim without a projectile trajectory");
		check(torturerAim.collisionPos == PAST, "a torturer's trajectory runs on past its target");
		check(torturer.reaches(torturerAim, NEAR), "torturers pull regardless of their trajectory");

		Victim victim = new Victim(NEAR);
		torturer.target(victim);
		check(torturer.tryChain(NEAR), "torturers pull targets in from a distance");
		check(victim.pos == FIRST, "torturers pull their target to the first open tile in the direction of the target");
		check(victim.buff(Cripple.class) == null, "torturers leave pulled targets able to move");
		check(!torturer.available(), "a torturer throws at most one chain per turn");
		check(!torturer.tryChain(NEAR), "a second pull in the same turn is refused");
		torturer.startNextTurn();
		check(torturer.available(), "torturers have their chains back once their turn is over");

		near(torturer.chainCost(), 0f, "pulling costs the torturer no time, so it can strike right afterwards");
		torturer.startNextTurn();

		victim.pos = NEAR;
		Dungeon.level.map[FIRST] = Terrain.CHASM;
		Dungeon.level.buildFlagMaps();
		check(!torturer.tryChain(NEAR), "torturers refuse to pull targets onto a chasm");
		check(victim.pos == NEAR, "a refused pull leaves the target where it was");
		check(torturer.available(), "a refused pull does not use up the turn's chain");

		Dungeon.level.map[FIRST] = Terrain.WALL;
		Dungeon.level.buildFlagMaps();
		check(torturer.tryChain(NEAR), "torturers pull past solid tiles");
		check(victim.pos == SECOND, "torturers skip solid tiles and use the first open tile instead");
		torturer.startNextTurn();

		Dungeon.level.map[FIRST] = Terrain.EMPTY;
		Dungeon.level.buildFlagMaps();

		Victim guardVictim = new Victim(NEAR);
		guard.target(guardVictim);
		check(guard.tryChain(NEAR), "guards pull targets in");
		check(guardVictim.pos == FIRST, "guards pull their target to the same landing tile");
		check(guardVictim.buff(Cripple.class) != null, "guards still cripple pulled targets");
		check(!guard.available(), "ordinary guards still run out of chains");
		near(guard.chainCost(), 0f, "guards spend no extra time on their single chain");

		//the behaviour that matters: drag the target in and then strike it, all within the guard's one turn
		TestTorturer puller = new TestTorturer();
		puller.pos = MOB;
		puller.state = puller.HUNTING;
		Victim prey = new Victim(NEAR);
		puller.forceEnemy(prey);

		check(puller.actOnce(), "a torturer keeps acting after dragging a target in, so it can strike the same turn");
		check(prey.pos == FIRST, "the pull lands the target right in front of the torturer");
		near(puller.cooldown(), 0f, "dragging a target in does not end the torturer's turn");
		check(puller.chainsThrown == 1, "the torturer throws its chain");
		check(puller.strikes == 0, "the torturer does not strike before its chain has landed");

		check(puller.actOnce(), "the follow up act resolves the strike");
		check(puller.strikes == 1, "the torturer strikes the target it just pulled in, on the same turn");
		check(puller.chainsThrown == 1, "the torturer throws only one chain per turn");

		Dungeon.LimitedDrops.GUARD_CHAINS.count = 0;
		TestTorturer firstTorturer = new TestTorturer();
		firstTorturer.roll();
		check(firstTorturer.chainsDropped, "the first torturer guarantees the ethereal chains drop");
		check(Dungeon.LimitedDrops.GUARD_CHAINS.dropped(), "the guaranteed drop counts as this run's chains drop");

		TestTorturer laterTorturer = new TestTorturer();
		laterTorturer.roll();
		check(!laterTorturer.chainsDropped, "later torturers do not drop chains once this run already dropped them");

		int guardDrops = 0;
		for (int i = 0; i < 4000; i++) {
			Dungeon.LimitedDrops.GUARD_CHAINS.count = 0;
			TestGuard rollingGuard = new TestGuard();
			rollingGuard.roll();
			if (rollingGuard.chainsDropped) guardDrops++;
		}
		check(guardDrops > 0 && guardDrops < 1000, "guards still roll a chance for the chains instead of guaranteeing them");

		GuardSprite guardSprite = new GuardSprite();
		TorturerSprite torturerSprite = new TorturerSprite();
		near(guardSprite.frame().top, 0f, "guards use the first texture row");
		near(torturerSprite.frame().top, 16f / torturerSprite.texture.height, "torturers use the second texture row");
		check(torturerSprite.texture == guardSprite.texture, "torturers reuse the guard's placeholder sheet");

		System.out.println("TorturerRegression passed");
	}
}
