/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;

import java.util.function.Supplier;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.TengusMask;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ScaleArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTransmutation;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfMetamorphosis;
/**
 * Compile-time test start configuration.
 * <p>
 * When {@link #ENABLED} is set to {@code true}, new games begin at depth 16
 * with a level 25 hero, 18 strength, a Greatsword and Scale Armor equipped,
 * a Tengu's Mask in the backpack, and 15 Scrolls of Upgrade.
 * </p>
 */
public class TestStart {

	/**
	 * Set to true to enable the test start point. Remember to set this back
	 * to false before building a release package.
	 */
	public static final boolean ENABLED = false;

	/**
	 * Starting depth when test start is enabled.
	 */
	public static final int START_DEPTH = 16;

	/**
	 * Starting hero level when test start is enabled.
	 */
	public static final int START_LEVEL = 25;

	/**
	 * Starting hero strength when test start is enabled.
	 */
	public static final int START_STRENGTH = 18;

	public static void apply() {
		if (!ENABLED) return;
		Hero hero = Dungeon.hero;
		if (hero == null) {
			return;
		}

		Dungeon.depth = START_DEPTH;

		// Level and core stats
		hero.lvl = START_LEVEL;
		hero.exp = 0;
		hero.STR = START_STRENGTH;

		// HP/HT and combat skills must match a hero that leveled from 1 to 25
		hero.updateHT( true );
		hero.setAttackSkill( 10 + (START_LEVEL - 1) );
		hero.setDefenseSkill( 5 + (START_LEVEL - 1) );

		// Replace starting weapon and armor
		Greatsword gs = new Greatsword();
		gs.identify();
		hero.belongings.weapon = gs;

		ScaleArmor armor = new ScaleArmor();
		armor.identify();
		hero.belongings.armor = armor;
		armor.activate( hero );

		// Subclass transfer item
		new TengusMask().collect();

		// Upgrade scrolls
		ScrollOfUpgrade sou = new ScrollOfUpgrade();
		sou.quantity( 15 );
		sou.collect();

		// Transmutation scrolls
		ScrollOfTransmutation sot = new ScrollOfTransmutation();
		sot.quantity( 5 );
		sot.collect();

		// Metamorphosis scrolls
		ScrollOfMetamorphosis som = new ScrollOfMetamorphosis();
		som.quantity( 20 );
		som.collect();


		Item.updateQuickslot();
	}

	/**
	 * Edit this method to add test mobs and traps. Runs only for newly generated
	 * main-branch floors, including floors generated before START_DEPTH.
	 * Use constructor references (and import the corresponding classes), e.g.:
	 * <pre>
	 * spawnMobs(level, START_DEPTH, Rat::new, 3);
	 * spawnTraps(level, START_DEPTH, WornDartTrap::new, 2, false);
	 * </pre>
	 * The final trap argument selects hidden (true) or visible (false) traps.
	 * Saved floors are loaded normally, so revisiting does not add duplicates.
	 */
	public static void applyToLevel(Level level) {
		if (!ENABLED || Dungeon.branch != 0) return;

		// Add spawnMobs(...) and spawnTraps(...) calls here.
	}

	/** Returns the number placed; stops when no suitable empty floor remains. */
	public static int spawnMobs(Level level, int depth, Supplier<? extends Mob> factory, int count) {
		if (!ENABLED || Dungeon.branch != 0 || Dungeon.depth != depth) return 0;
		int placed = 0;
		for (; placed < count; placed++) {
			Mob mob = factory.get();
			int cell = emptyCell(level, Char.hasProp(mob, Char.Property.LARGE));
			if (cell == -1) break;
			mob.pos = cell;
			level.mobs.add(mob);
		}
		return placed;
	}

	/** Returns the number placed; traps that cannot be hidden remain visible. */
	public static int spawnTraps(Level level, int depth, Supplier<? extends Trap> factory,
	                             int count, boolean hidden) {
		if (!ENABLED || Dungeon.branch != 0 || Dungeon.depth != depth) return 0;
		int placed = 0;
		for (; placed < count; placed++) {
			int cell = emptyCell(level, false);
			if (cell == -1) break;
			Trap trap = factory.get();
			trap.visible = !hidden || !trap.canBeHidden;
			Level.set(cell, trap.visible ? Terrain.TRAP : Terrain.SECRET_TRAP, level);
			// Generation has no active Dungeon.level or GameScene yet.
			level.traps.put(cell, trap.set(cell));
		}
		return placed;
	}

	// Prefer entrance-adjacent ordinary floor without consuming the game's RNG.
	private static int emptyCell(Level level, boolean large) {
		int best = -1;
		int nearest = Integer.MAX_VALUE;
		for (int cell = 0; cell < level.length(); cell++) {
			if (!level.insideMap(cell) || level.map[cell] != Terrain.EMPTY
					|| !level.passable[cell] || (large && !level.openSpace[cell])
					|| cell == level.entrance() || cell == level.exit()
					|| level.getTransition(cell) != null || level.findMob(cell) != null
					|| level.heaps.get(cell) != null || level.plants.get(cell) != null
					|| level.traps.get(cell) != null) continue;
			int distance = level.distance(level.entrance(), cell);
			if (distance < nearest) {
				best = cell;
				nearest = distance;
			}
		}
		return best;
	}
}
