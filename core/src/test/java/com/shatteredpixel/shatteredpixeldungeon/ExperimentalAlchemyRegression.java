package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Recipe;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Firebomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.FrostBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.CausticBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.GooBlob;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.PhaseShift;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.TrinketCatalyst;
import com.shatteredpixel.shatteredpixeldungeon.plants.Firebloom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/** Standalone regression runner for experimental alchemy recipe matching. */
public class ExperimentalAlchemyRegression {

	public static void main(String[] args) {
		check(Recipe.experimentalOutputs(Recipe.EXPERIMENTAL_BOMBS).size() == 10,
				"all enhanced bombs are selectable");
		check(Recipe.experimentalOutputs(Recipe.EXPERIMENTAL_BREWS).size() == 14,
				"all brews and elixirs are selectable");
		check(Recipe.experimentalOutputs(Recipe.EXPERIMENTAL_SPELLS).size() == 11,
				"all spell crystals are selectable");
		for (int category = 0; category < 3; category++) {
			ArrayList<Item> outputs = Recipe.experimentalOutputs(category);
			HashSet<Class<?>> outputTypes = new HashSet<>();
			for (Item output : outputs) {
				check(output != null, "selectable output is present");
				outputTypes.add(output.getClass());
			}
			check(outputTypes.size() == outputs.size(), "selectable outputs are unique");
		}

		PotionOfFrost unknownFrost = item(PotionOfFrost.class);
		ArrayList<Item> bombIngredients = ingredients(item(Bomb.class), unknownFrost);
		check(!unknownFrost.isIdentified(), "bomb ingredient starts unidentified");
		check(Recipe.findExperimentalRecipe(bombIngredients, item(FrostBomb.class), 0,
				Recipe.EXPERIMENTAL_BOMBS) != null, "correct bomb guess");
		check(!unknownFrost.isIdentified(), "matching does not identify bomb ingredient");
		check(Recipe.findExperimentalRecipe(bombIngredients, item(FrostBomb.class), 1,
				Recipe.EXPERIMENTAL_BOMBS) == null, "wrong bomb energy");
		check(Recipe.findExperimentalRecipe(bombIngredients, item(Firebomb.class), 0,
				Recipe.EXPERIMENTAL_BOMBS) == null, "wrong bomb output");
		bombIngredients.add(item(GooBlob.class));
		check(Recipe.findExperimentalRecipe(bombIngredients, item(FrostBomb.class), 0,
				Recipe.EXPERIMENTAL_BOMBS) == null, "extra bomb ingredient fails");

		PotionOfToxicGas unknownToxic = item(PotionOfToxicGas.class);
		ArrayList<Item> brewIngredients = ingredients(unknownToxic, item(GooBlob.class));
		check(!unknownToxic.isIdentified(), "brew ingredient starts unidentified");
		check(Recipe.findExperimentalRecipe(brewIngredients, item(CausticBrew.class), 1,
				Recipe.EXPERIMENTAL_BREWS) != null, "correct brew guess");
		check(!unknownToxic.isIdentified(), "matching does not identify brew ingredient");

		ScrollOfTeleportation unknownTeleport = item(ScrollOfTeleportation.class);
		ArrayList<Item> spellIngredients = ingredients(unknownTeleport);
		check(!unknownTeleport.isIdentified(), "spell ingredient starts unidentified");
		check(Recipe.findExperimentalRecipe(spellIngredients, item(PhaseShift.class), 10,
				Recipe.EXPERIMENTAL_SPELLS) != null, "correct spell guess");
		check(!unknownTeleport.isIdentified(), "matching does not identify spell ingredient");

		check(Recipe.findExperimentalRecipe(ingredients(item(Firebloom.Seed.class), item(Firebloom.Seed.class),
				item(Firebloom.Seed.class)), item(CausticBrew.class), 0, Recipe.EXPERIMENTAL_BREWS) == null,
				"seed potion recipe is excluded");
		check(Recipe.findExperimentalRecipe(ingredients(item(TrinketCatalyst.class)), item(PhaseShift.class), 6,
				Recipe.EXPERIMENTAL_SPELLS) == null, "random trinket recipe is excluded");

		System.out.println("PASS: experimental alchemy matching");
	}

	private static ArrayList<Item> ingredients(Item... items) {
		return new ArrayList<>(Arrays.asList(items));
	}

	@SuppressWarnings("unchecked")
	private static <T> T item(Class<T> type) {
		try {
			java.lang.reflect.Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			T result = (T)((sun.misc.Unsafe)field.get(null)).allocateInstance(type);
			if (result instanceof Item) ((Item)result).quantity(1);
			return result;
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
