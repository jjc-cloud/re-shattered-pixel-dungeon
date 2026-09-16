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

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.ExorcismBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.ShockBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.StunBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.SuperBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Blandfruit;
import com.shatteredpixel.shatteredpixeldungeon.items.food.MeatPie;
import com.shatteredpixel.shatteredpixeldungeon.items.food.StewedMeat;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.AquaBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.BlizzardBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.Brew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.CausticBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.InfernalBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.ShockingBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.SteamBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.UnstableBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfArcaneArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfDragonsBlood;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfFeatherFall;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfHoneyedHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfIcyTouch;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfKineticEnergy;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfToxicEssence;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfStormClouds;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRetribution;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ExoticScroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfAntiMagic;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfMetamorphosis;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLevitation;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfStamina;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.Alchemize;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.BeaconOfReturning;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.CurseInfusion;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.MagicalInfusion;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.PhaseShift;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.ReclaimTrap;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.Recycle;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.SummonElemental;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.TelekineticGrab;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.UnstableSpell;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.WildEnergy;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.Trinket;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.TrinketCatalyst;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.journal.Journal;
import com.watabou.utils.Reflection;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public abstract class Recipe {
	
	public abstract boolean testIngredients(ArrayList<Item> ingredients);

	/**
	 * Ingredient check used by experimental alchemy. Unlike ordinary alchemy,
	 * this must not require the hero to already know an ingredient's identity.
	 */
	public boolean testIngredientsExperimental(ArrayList<Item> ingredients) {
		return testIngredients(ingredients);
	}
	
	public abstract int cost(ArrayList<Item> ingredients);
	
	public abstract Item brew(ArrayList<Item> ingredients);
	
	public abstract Item sampleOutput(ArrayList<Item> ingredients);
	
	//subclass for the common situation of a recipe with static inputs and outputs
	public static abstract class SimpleRecipe extends Recipe {
		
		//*** These elements must be filled in by subclasses
		protected Class<?extends Item>[] inputs; //each class should be unique
		protected int[] inQuantity;
		
		protected int cost;
		
		protected Class<?extends Item> output;
		protected int outQuantity;
		//***
		
		//gets a simple list of items based on inputs
		public ArrayList<Item> getIngredients() {
			ArrayList<Item> result = new ArrayList<>();
			for (int i = 0; i < inputs.length; i++) {
				Item ingredient = Reflection.newInstance(inputs[i]);
				ingredient.quantity(inQuantity[i]);
				result.add(ingredient);
			}
			return result;
		}
		
		@Override
		public boolean testIngredients(ArrayList<Item> ingredients) {
			return testIngredients(ingredients, true);
		}

		@Override
		public boolean testIngredientsExperimental(ArrayList<Item> ingredients) {
			return testIngredients(ingredients, false);
		}

		private boolean testIngredients(ArrayList<Item> ingredients, boolean requireIdentified) {
			
			int[] needed = inQuantity.clone();
			
			for (Item ingredient : ingredients){
				if (requireIdentified && !ingredient.isIdentified()) return false;
				for (int i = 0; i < inputs.length; i++){
					if (ingredient.getClass() == inputs[i]){
						needed[i] -= ingredient.quantity();
						break;
					}
				}
			}
			
			for (int i : needed){
				if (i > 0){
					return false;
				}
			}
			
			return true;
		}
		
		public int cost(ArrayList<Item> ingredients){
			return cost;
		}
		
		@Override
		public Item brew(ArrayList<Item> ingredients) {
			if (!testIngredients(ingredients)) return null;
			
			int[] needed = inQuantity.clone();
			
			for (Item ingredient : ingredients){
				for (int i = 0; i < inputs.length; i++) {
					if (ingredient.getClass() == inputs[i] && needed[i] > 0) {
						if (needed[i] <= ingredient.quantity()) {
							ingredient.quantity(ingredient.quantity() - needed[i]);
							needed[i] = 0;
						} else {
							needed[i] -= ingredient.quantity();
							ingredient.quantity(0);
						}
					}
				}
			}
			
			//sample output and real output are identical in this case.
			return sampleOutput(null);
		}
		
		//ingredients are ignored, as output doesn't vary
		public Item sampleOutput(ArrayList<Item> ingredients){
			try {
				Item result = Reflection.newInstance(output);
				result.quantity(outQuantity);
				return result;
			} catch (Exception e) {
				ShatteredPixelDungeon.reportException( e );
				return null;
			}
		}
	}
	
	
	//*******
	// Static members
	//*******

	private static Recipe[] variableRecipes = new Recipe[]{
			//none for now
	};
	
	private static Recipe[] oneIngredientRecipes = new Recipe[]{
		new Scroll.ScrollToStone(),
		new ExoticPotion.PotionToExotic(),
		new ExoticScroll.ScrollToExotic(),
		new ArcaneResin.Recipe(),
		new LiquidMetal.Recipe(),
		new BlizzardBrew.Recipe(),
		new InfernalBrew.Recipe(),
		new AquaBrew.Recipe(),
		new ShockingBrew.Recipe(),
		new ElixirOfDragonsBlood.Recipe(),
		new ElixirOfIcyTouch.Recipe(),
		new ElixirOfToxicEssence.Recipe(),
		new ElixirOfMight.Recipe(),
		new ElixirOfFeatherFall.Recipe(),
		new MagicalInfusion.Recipe(),
		new BeaconOfReturning.Recipe(),
		new PhaseShift.Recipe(),
		new Recycle.Recipe(),
		new TelekineticGrab.Recipe(),
		new SummonElemental.Recipe(),
		new StewedMeat.oneMeat(),
		new TrinketCatalyst.Recipe(),
		new Trinket.UpgradeTrinket()
	};
	
	private static Recipe[] twoIngredientRecipes = new Recipe[]{
		new Blandfruit.CookFruit(),
		new Bomb.EnhanceBomb(),
		new UnstableBrew.Recipe(),
		new CausticBrew.Recipe(),
		new ElixirOfArcaneArmor.Recipe(),
		new ElixirOfAquaticRejuvenation.Recipe(),
		new ElixirOfHoneyedHealing.Recipe(),
		new UnstableSpell.Recipe(),
		new Alchemize.Recipe(),
		new CurseInfusion.Recipe(),
		new ReclaimTrap.Recipe(),
		new WildEnergy.Recipe(),
		new StewedMeat.twoMeat()
	};
	
	private static Recipe[] threeIngredientRecipes = new Recipe[]{
		new Potion.SeedToPotion(),
		new StewedMeat.threeMeat(),
		new MeatPie.Recipe()
	};

	public static final int EXPERIMENTAL_BOMBS = 0;
	public static final int EXPERIMENTAL_BREWS = 1;
	public static final int EXPERIMENTAL_SPELLS = 2;

	// 特殊配方在炼金图鉴里的栏位。魔药与秘药页分上下两栏，其余两页各只有一栏。
	public static final int GUIDE_BOMB = 0;
	public static final int GUIDE_BREW = 1;
	public static final int GUIDE_ELIXIR = 2;
	public static final int GUIDE_SPELL = 3;

	public static class SpecialRecipe extends SimpleRecipe {

		private final int category;

		@SafeVarargs
		public SpecialRecipe(int category, int cost, Class<? extends Item> output,
				Class<? extends Item>... inputs) {
			this.category = category;
			this.cost = cost;
			this.output = output;
			this.inputs = inputs;
			this.inQuantity = new int[inputs.length];
			for (int i = 0; i < inQuantity.length; i++) inQuantity[i] = 1;
			this.outQuantity = 1;
		}

		public int category() {
			return category;
		}

		/**
		 * 本配方在炼金图鉴中应归入的栏位。分类由 category 决定页，
		 * 魔药与秘药页再用产物类型区分上下栏：魔药走上栏，其余（秘药）走下列，
		 * 这样新增特殊配方只要 category 正确就会自动进对栏，也不会漏掉条目。
		 */
		public int guideSection() {
			if (category == EXPERIMENTAL_BREWS) {
				return output != null && Brew.class.isAssignableFrom(output)
						? GUIDE_BREW : GUIDE_ELIXIR;
			} else if (category == EXPERIMENTAL_BOMBS) {
				return GUIDE_BOMB;
			} else {
				return GUIDE_SPELL;
			}
		}

		@Override
		public boolean testIngredients(ArrayList<Item> ingredients) {
			return exactIngredients(ingredients);
		}

		@Override
		public boolean testIngredientsExperimental(ArrayList<Item> ingredients) {
			return exactIngredients(ingredients);
		}

		private boolean exactIngredients(ArrayList<Item> ingredients) {
			if (ingredients.size() != inputs.length) return false;
			int[] found = new int[inputs.length];
			for (Item ingredient : ingredients) {
				boolean matched = false;
				for (int i = 0; i < inputs.length; i++) {
					if (ingredient.getClass() == inputs[i]) {
						found[i] += ingredient.quantity();
						matched = true;
						break;
					}
				}
				if (!matched) return false;
			}
			for (int i = 0; i < found.length; i++) {
				if (found[i] != inQuantity[i]) return false;
			}
			return true;
		}
	}

	private static final SpecialRecipe[] specialRecipes = new SpecialRecipe[]{
			new SpecialRecipe(EXPERIMENTAL_BOMBS, 4, SuperBomb.class,
					Bomb.class, PotionOfStrength.class),
			new SpecialRecipe(EXPERIMENTAL_BOMBS, 2, StunBomb.class,
					Bomb.class, PotionOfLevitation.class),
			new SpecialRecipe(EXPERIMENTAL_BOMBS, 6, ShockBomb.class,
					Bomb.class, ScrollOfRetribution.class),
			new SpecialRecipe(EXPERIMENTAL_BOMBS, 2, ExorcismBomb.class,
					Bomb.class, ScrollOfAntiMagic.class),
			new SpecialRecipe(EXPERIMENTAL_BREWS, 8, ElixirOfKineticEnergy.class,
					PotionOfStamina.class),
			new SpecialRecipe(EXPERIMENTAL_SPELLS, 4, ExperimentalTengusMask.class,
					TengusMask.class, ScrollOfMetamorphosis.class),
			new SpecialRecipe(EXPERIMENTAL_SPELLS, 6, ExperimentalKingsCrown.class,
					KingsCrown.class, ScrollOfMetamorphosis.class),
			new SpecialRecipe(EXPERIMENTAL_BREWS, 4, SteamBrew.class,
					InfernalBrew.class, PotionOfStormClouds.class)
	};

	private static final LinkedHashMap<String, Integer> specialRecipeCosts = new LinkedHashMap<>();

	private static final Recipe[][] experimentalRecipes = new Recipe[][]{
			{
					new Bomb.EnhanceBomb(), specialRecipes[0], specialRecipes[1],
					specialRecipes[2], specialRecipes[3]
			},
			{
					new UnstableBrew.Recipe(), new CausticBrew.Recipe(),
					new BlizzardBrew.Recipe(), new ShockingBrew.Recipe(),
					new InfernalBrew.Recipe(), specialRecipes[7], new AquaBrew.Recipe(),
					new ElixirOfHoneyedHealing.Recipe(), new ElixirOfAquaticRejuvenation.Recipe(),
					new ElixirOfArcaneArmor.Recipe(), new ElixirOfIcyTouch.Recipe(),
					new ElixirOfToxicEssence.Recipe(), new ElixirOfDragonsBlood.Recipe(),
					new ElixirOfFeatherFall.Recipe(), new ElixirOfMight.Recipe(), specialRecipes[4]
			},
			{
					new UnstableSpell.Recipe(), new WildEnergy.Recipe(),
					new TelekineticGrab.Recipe(), new PhaseShift.Recipe(),
					new Alchemize.Recipe(), new CurseInfusion.Recipe(),
					new MagicalInfusion.Recipe(), new Recycle.Recipe(),
					new ReclaimTrap.Recipe(), new SummonElemental.Recipe(),
					new BeaconOfReturning.Recipe(), specialRecipes[5], specialRecipes[6]
			}
	};

	public static ArrayList<Item> experimentalOutputs(int category) {
		ArrayList<Item> result = new ArrayList<>();
		if (category < 0 || category >= experimentalRecipes.length) return result;

		if (category == EXPERIMENTAL_BOMBS) {
			for (Class<? extends Bomb> output : Bomb.EnhanceBomb.validIngredients.values()) {
				result.add(Reflection.newInstance(output));
			}
			for (SpecialRecipe recipe : specialRecipes) {
				if (recipe.category == category) result.add(recipe.sampleOutput(null));
			}
		} else {
			for (Recipe recipe : experimentalRecipes[category]) {
				result.add(recipe.sampleOutput(null));
			}
		}
		return result;
	}

	public static Recipe findExperimentalRecipe(ArrayList<Item> ingredients, Item output,
			int energy, int category) {
		if (output == null || category < 0 || category >= experimentalRecipes.length) return null;
		ingredients = RealityWarp.alchemyIngredients(ingredients);

		for (Recipe recipe : experimentalRecipes[category]) {
			boolean enoughEnergy = recipe instanceof SpecialRecipe
					? energy >= recipe.cost(ingredients) : energy == recipe.cost(ingredients);
			if (ingredients.size() == experimentalIngredientSlots(recipe, category)
					&& recipe.testIngredientsExperimental(ingredients) && enoughEnergy) {
				Item expected = recipe.sampleOutput(ingredients);
				if (expected != null && expected.getClass() == output.getClass()) return recipe;
			}
		}
		return null;
	}

	private static int experimentalIngredientSlots(Recipe recipe, int category) {
		if (recipe instanceof SimpleRecipe) return ((SimpleRecipe) recipe).inputs.length;
		if (category == EXPERIMENTAL_BOMBS || recipe instanceof UnstableBrew.Recipe
				|| recipe instanceof UnstableSpell.Recipe || recipe instanceof Alchemize.Recipe) {
			return 2;
		}
		return 1;
	}

	public static boolean isSpecialOutput(Item output) {
		if (output == null) return false;
		for (SpecialRecipe recipe : specialRecipes) {
			if (recipe.output == output.getClass()) return true;
		}
		return false;
	}

	public static boolean isSpecialDiscovered(Item output) {
		return output != null && specialRecipeCosts.containsKey(output.getClass().getName());
	}

	public static void recordSpecialRecipe(Recipe recipe, int energy) {
		if (!(recipe instanceof SpecialRecipe)) return;
		String key = ((SpecialRecipe)recipe).output.getName();
		Integer previous = specialRecipeCosts.get(key);
		if (previous == null || energy < previous) {
			specialRecipeCosts.put(key, energy);
			Journal.markDirty();
		}
	}

	public static int discoveredSpecialCost(SpecialRecipe recipe) {
		Integer result = specialRecipeCosts.get(recipe.output.getName());
		return result == null ? -1 : result;
	}

	public static ArrayList<SpecialRecipe> discoveredSpecialRecipes(int guidePage) {
		ArrayList<SpecialRecipe> result = new ArrayList<>();
		int category = guidePage == 5 ? EXPERIMENTAL_BOMBS
				: guidePage == 7 ? EXPERIMENTAL_BREWS
				: guidePage == 8 ? EXPERIMENTAL_SPELLS : -1;
		for (SpecialRecipe recipe : specialRecipes) {
			if (recipe.category == category && discoveredSpecialCost(recipe) >= 0) result.add(recipe);
		}
		return result;
	}

	private static final String SPECIAL_RECIPE_KEYS = "special_recipe_keys";
	private static final String SPECIAL_RECIPE_COSTS = "special_recipe_costs";

	public static void resetSpecialRecipes() {
		specialRecipeCosts.clear();
	}

	public static void storeSpecialRecipes(Bundle bundle) {
		String[] keys = specialRecipeCosts.keySet().toArray(new String[0]);
		int[] costs = new int[keys.length];
		for (int i = 0; i < keys.length; i++) costs[i] = specialRecipeCosts.get(keys[i]);
		bundle.put(SPECIAL_RECIPE_KEYS, keys);
		bundle.put(SPECIAL_RECIPE_COSTS, costs);
	}

	public static boolean restoreSpecialRecipes(Bundle bundle) {
		boolean changed = false;
		String[] keys = bundle.getStringArray(SPECIAL_RECIPE_KEYS);
		int[] costs = bundle.getIntArray(SPECIAL_RECIPE_COSTS);
		if (keys != null && costs != null) {
			for (int i = 0; i < Math.min(keys.length, costs.length); i++) {
				Integer previous = specialRecipeCosts.get(keys[i]);
				if (previous == null || costs[i] < previous) {
					specialRecipeCosts.put(keys[i], costs[i]);
					changed = true;
				}
			}
		}
		return changed;
	}
	
	public static ArrayList<Recipe> findRecipes(ArrayList<Item> ingredients){

		ArrayList<Recipe> result = new ArrayList<>();
		ingredients = RealityWarp.alchemyIngredients(ingredients);

		for (Recipe recipe : variableRecipes){
			if (recipe.testIngredients(ingredients)){
				result.add(recipe);
			}
		}

		if (ingredients.size() == 1){
			for (Recipe recipe : oneIngredientRecipes){
				if (recipe.testIngredients(ingredients)){
					result.add(recipe);
				}
			}
			
		} else if (ingredients.size() == 2){
			for (Recipe recipe : twoIngredientRecipes){
				if (recipe.testIngredients(ingredients)){
					result.add(recipe);
				}
			}
			
		} else if (ingredients.size() == 3){
			for (Recipe recipe : threeIngredientRecipes){
				if (recipe.testIngredients(ingredients)){
					result.add(recipe);
				}
			}
		}

		for (SpecialRecipe recipe : specialRecipes) {
			if (discoveredSpecialCost(recipe) >= 0 && recipe.testIngredients(ingredients)) {
				result.add(recipe);
			}
		}
		
		return result;
	}

	public static int cost(Recipe recipe, ArrayList<Item> ingredients) {
		if (recipe instanceof SpecialRecipe) {
			int discoveredCost = discoveredSpecialCost((SpecialRecipe)recipe);
			if (discoveredCost >= 0) return discoveredCost;
		}
		return recipe.cost(RealityWarp.alchemyIngredients(ingredients));
	}

	public static Item sampleOutput(Recipe recipe, ArrayList<Item> ingredients) {
		return recipe.sampleOutput(RealityWarp.alchemyIngredients(ingredients));
	}

	public static Item brew(Recipe recipe, ArrayList<Item> ingredients) {
		return recipe.brew(RealityWarp.alchemyIngredients(ingredients));
	}
	
	public static boolean usableInRecipe(Item item){
		//only upgradeable thrown weapons and wands allowed among equipment items
		if (item instanceof EquipableItem){
			return item.cursedKnown && !item.cursed &&
					item instanceof MissileWeapon && item.isUpgradable();
		} else if (item instanceof Wand) {
			return item.cursedKnown && !item.cursed;
		} else {
			//other items can be unidentified, but not cursed
			return !item.cursed;
		}
	}
}


