package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfDungeonBlueprint;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMap;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;

/** Standalone checks for regional and permanent dungeon map knowledge. */
public class DungeonMapKnowledgeRegression {

	private static void check( boolean condition, String message ) {
		if (!condition) throw new AssertionError(message);
	}

	public static void main( String[] args ) {
		Dungeon.branch = 0;
		Dungeon.depth = 7;
		Dungeon.generatedLevels.clear();
		Dungeon.revealCurrentRegion();

		check(!Dungeon.hasMapKnowledge(5), "regional map excludes the previous region");
		for (int depth = 6; depth <= 10; depth++) {
			check(Dungeon.hasMapKnowledge(depth), "regional map includes depth " + depth);
		}
		check(!Dungeon.hasMapKnowledge(11), "regional map excludes the next region");

		Dungeon.generatedLevels.add(6);
		Dungeon.generatedLevels.add(7);
		Dungeon.generatedLevels.add(8);
		Dungeon.depth = 8;
		Dungeon.clearTemporaryMapKnowledgeOnDeath();

		check(Dungeon.hasMapKnowledge(6), "visited depth 6 remains mapped");
		check(Dungeon.hasMapKnowledge(7), "visited depth 7 remains mapped");
		check(!Dungeon.hasMapKnowledge(8), "death depth is reset");
		check(!Dungeon.hasMapKnowledge(9), "unvisited depth 9 is forgotten");
		check(!Dungeon.hasMapKnowledge(10), "unvisited depth 10 is forgotten");

		Dungeon.revealEntireDungeon();
		Dungeon.clearTemporaryMapKnowledgeOnDeath();
		for (int depth = 1; depth <= 25; depth++) {
			check(Dungeon.hasMapKnowledge(depth), "blueprint preserves depth " + depth);
		}
		check(Catalog.SCROLLS.items().contains(ScrollOfMagicMap.class), "magic map is in scroll catalog");
		check(Catalog.SCROLLS.items().contains(ScrollOfDungeonBlueprint.class), "blueprint is in scroll catalog");

		System.out.println("PASS: regional and permanent dungeon map knowledge, including catalog entries");
	}
}
