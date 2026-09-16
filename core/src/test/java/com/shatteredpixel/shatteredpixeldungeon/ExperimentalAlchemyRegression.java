package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Recipe;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Firebomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.FrostBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.SuperBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.CausticBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.InfernalBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.SteamBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfKineticEnergy;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfStamina;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfStormClouds;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.GooBlob;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.PhaseShift;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.TrinketCatalyst;
import com.shatteredpixel.shatteredpixeldungeon.plants.Firebloom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import com.watabou.utils.Bundle;

/** Standalone regression runner for experimental alchemy recipe matching. */
public class ExperimentalAlchemyRegression {

	public static void main(String[] args) {
		check(Recipe.experimentalOutputs(Recipe.EXPERIMENTAL_BOMBS).size() == 14,
				"all enhanced bombs are selectable");
		check(Recipe.experimentalOutputs(Recipe.EXPERIMENTAL_BREWS).size() == 16,
				"all brews and elixirs are selectable");
		check(Recipe.experimentalOutputs(Recipe.EXPERIMENTAL_SPELLS).size() == 13,
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

		ArrayList<Item> specialIngredients = ingredients(item(Bomb.class), item(PotionOfStrength.class));
		Recipe special = Recipe.findExperimentalRecipe(specialIngredients, item(SuperBomb.class), 4,
				Recipe.EXPERIMENTAL_BOMBS);
		check(special != null, "special recipe accepts its minimum energy");
		check(Recipe.findExperimentalRecipe(specialIngredients, item(SuperBomb.class), 9,
				Recipe.EXPERIMENTAL_BOMBS) == special, "special recipe accepts excess energy");
		check(Recipe.findExperimentalRecipe(specialIngredients, item(SuperBomb.class), 3,
				Recipe.EXPERIMENTAL_BOMBS) == null, "special recipe rejects insufficient energy");
		check(Recipe.findRecipes(specialIngredients).isEmpty(), "special recipe is experimental-only");
		ArrayList<Item> wrongQuantity = ingredients(item(Bomb.class), item(PotionOfStrength.class).quantity(2));
		check(Recipe.findExperimentalRecipe(wrongQuantity, item(SuperBomb.class), 4,
				Recipe.EXPERIMENTAL_BOMBS) == null, "special recipe requires exact quantities");

		ArrayList<Item> steamIngredients = ingredients(item(InfernalBrew.class),
				item(PotionOfStormClouds.class));
		Recipe steamRecipe = Recipe.findExperimentalRecipe(steamIngredients, item(SteamBrew.class), 4,
				Recipe.EXPERIMENTAL_BREWS);
		check(steamRecipe != null, "steam brew accepts its minimum energy");
		check(Recipe.findExperimentalRecipe(steamIngredients, item(SteamBrew.class), 8,
				Recipe.EXPERIMENTAL_BREWS) == steamRecipe, "steam brew accepts excess energy");
		check(Recipe.findExperimentalRecipe(steamIngredients, item(SteamBrew.class), 3,
				Recipe.EXPERIMENTAL_BREWS) == null, "steam brew rejects insufficient energy");
		check(Recipe.findRecipes(steamIngredients).isEmpty(), "steam brew is experimental-only");

		Recipe.resetSpecialRecipes();
		Recipe.recordSpecialRecipe(special, 9);
		Recipe.recordSpecialRecipe(special, 6);
		specialIngredients.get(0).identify(false);
		((com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion)specialIngredients.get(1)).anonymize();
		ArrayList<Recipe> learnedRecipes = Recipe.findRecipes(specialIngredients);
		check(learnedRecipes.contains(special), "learned experimental recipe matches in normal alchemy");
		check(Recipe.cost(special, specialIngredients) == 6,
				"normal alchemy uses the lowest discovered special recipe cost");
		check(Recipe.sampleOutput(special, specialIngredients) instanceof SuperBomb,
				"learned normal recipe matches its recorded output");
		Recipe.SpecialRecipe recorded = Recipe.discoveredSpecialRecipes(5).get(0);
		check(Recipe.discoveredSpecialCost(recorded) == 6, "guide records lowest attempted success");
		Bundle saved = new Bundle();
		Recipe.storeSpecialRecipes(saved);
		Recipe.resetSpecialRecipes();
		Recipe.restoreSpecialRecipes(saved);
		check(Recipe.discoveredSpecialCost(recorded) == 6, "special recipe discovery survives saves");
		Bundle otherSave = new Bundle();
		Recipe.resetSpecialRecipes();
		Recipe.recordSpecialRecipe(special, 8);
		Recipe.storeSpecialRecipes(otherSave);
		Recipe.resetSpecialRecipes();
		Recipe.restoreSpecialRecipes(saved);
		check(!Recipe.restoreSpecialRecipes(otherSave), "a worse cost from another save changes nothing");
		check(Recipe.discoveredSpecialCost(recorded) == 6, "global discovery keeps the lower cost");
		Recipe.resetSpecialRecipes();
		Recipe.recordSpecialRecipe(special, 4);
		Recipe.storeSpecialRecipes(otherSave);
		Recipe.resetSpecialRecipes();
		Recipe.restoreSpecialRecipes(saved);
		check(Recipe.restoreSpecialRecipes(otherSave), "a better cost from another save is merged");
		check(Recipe.discoveredSpecialCost(recorded) == 4, "global discovery adopts the new minimum");

		// 图鉴栏位：魔药与秘药页分上下两栏，特殊配方按产物类型自动归栏
		Recipe.resetSpecialRecipes();
		check(((Recipe.SpecialRecipe)special).guideSection() == Recipe.GUIDE_BOMB,
				"special bomb is filed in the bomb section");
		check(((Recipe.SpecialRecipe)steamRecipe).guideSection() == Recipe.GUIDE_BREW,
				"steam brew is filed in the brew section");

		ArrayList<Item> kineticIngredients = ingredients(item(PotionOfStamina.class));
		Recipe kinetic = Recipe.findExperimentalRecipe(kineticIngredients, item(ElixirOfKineticEnergy.class), 8,
				Recipe.EXPERIMENTAL_BREWS);
		check(kinetic instanceof Recipe.SpecialRecipe, "kinetic elixir is a special recipe");
		check(((Recipe.SpecialRecipe)kinetic).guideSection() == Recipe.GUIDE_ELIXIR,
				"kinetic elixir is filed in the elixir section");

		// 已发现的特殊配方必须落在本页存在的栏位里，否则会从图鉴中消失
		Recipe.recordSpecialRecipe(special, 4);
		Recipe.recordSpecialRecipe(steamRecipe, 4);
		Recipe.recordSpecialRecipe(kinetic, 8);
		int[][] pageSections = {{Recipe.GUIDE_BOMB}, {Recipe.GUIDE_BREW, Recipe.GUIDE_ELIXIR}, {Recipe.GUIDE_SPELL}};
		int[] guidePages = {5, 7, 8};
		int filedSpecials = 0;
		for (int p = 0; p < guidePages.length; p++) {
			for (Recipe.SpecialRecipe r : Recipe.discoveredSpecialRecipes(guidePages[p])) {
				boolean filed = false;
				for (int section : pageSections[p]) filed |= r.guideSection() == section;
				check(filed, "every discovered special recipe lands in a section of its guide page");
				filedSpecials++;
			}
		}
		check(filedSpecials == 3, "all recorded special recipes are checked");
		check(Recipe.discoveredSpecialRecipes(7).size() == 2,
				"brews and elixirs share the brews/elixirs guide page");

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
