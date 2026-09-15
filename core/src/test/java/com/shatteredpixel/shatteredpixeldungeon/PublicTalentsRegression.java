package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DirectableAlly;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.MirrorImage;
import com.shatteredpixel.shatteredpixeldungeon.items.MirrorLink;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Random;

import java.util.HashSet;
import java.util.Arrays;

/** Standalone checks for public metamorph talents; requires no graphics context. */
public class PublicTalentsRegression {

	private static Hero hero(Talent talent, int points){
		Hero hero = new Hero();
		hero.heroClass = HeroClass.WARRIOR;
		hero.pos = 312;
		hero.STR = 10;
		hero.HP = hero.HT = 100;
		Talent.initClassTalents(hero);
		hero.talents.get(0).put(talent, points);
		Dungeon.hero = hero;
		return hero;
	}

	private static Char target(int pos){
		Char target = new Char() { };
		target.alignment = Char.Alignment.ENEMY;
		target.pos = pos;
		target.HP = target.HT = 10;
		Actor.add(target);
		return target;
	}

	private static void check(boolean condition, String message){
		if (!condition) throw new AssertionError(message);
	}

	private static void indexingAndPools(){
		check(Talent.UPLIFTING_MEAL.icon() == 224, "uplifting meal icon");
		check(Talent.MULTIPLE_EXISTENCE.icon() == 225, "multiple existence icon");
		check(Talent.ESCAPE_PLAN.icon() == 226, "escape plan icon");
		check(Talent.VOID_WALKER.icon() == 227, "void walker icon");
		check(Talent.FIREPOWER_BARRAGE.icon() == 228 && Talent.FIREPOWER_BARRAGE.isRare(), "rare barrage icon");
		check(Talent.MANA_OVERLOAD.icon() == 230 && !Talent.MANA_OVERLOAD.isRare(), "mana overload icon");
		check(Talent.SAVAGE_PARASITE.icon() == 231 && !Talent.SAVAGE_PARASITE.isRare(), "savage parasite icon");
		check(Talent.PERFECT_EXECUTION.icon() == 232 && Talent.PERFECT_EXECUTION.isRare(), "hidden skill is rare");
		check(Talent.REALITY_WARP.icon() == 229 && Talent.REALITY_WARP.isRare(), "rare reality warp icon");
		check(Talent.CROSSFIRE.icon() == 233 && !Talent.CROSSFIRE.isRare(), "crossfire is common");
		check(Talent.publicMetamorphTalentsAvailable(1) && Talent.publicMetamorphTalentsAvailable(2), "public tier one and two");
		check(!Talent.publicMetamorphTalentsAvailable(3) && !Talent.publicMetamorphTalentsAvailable(4), "no public tier three or four");
		Talent rareRoll = Talent.randomPublicMetamorphTalent(true, new HashSet<>());
		check(rareRoll == Talent.FIREPOWER_BARRAGE || rareRoll == Talent.REALITY_WARP || rareRoll == Talent.PERFECT_EXECUTION,
				"rare pool contains three talents");
		HashSet<Talent> commonExclusions = new HashSet<>();
		commonExclusions.add(Talent.UPLIFTING_MEAL);
		commonExclusions.add(Talent.MULTIPLE_EXISTENCE);
		commonExclusions.add(Talent.ESCAPE_PLAN);
		commonExclusions.add(Talent.VOID_WALKER);
		commonExclusions.add(Talent.MANA_OVERLOAD);
		commonExclusions.add(Talent.SAVAGE_PARASITE);
		check(Talent.randomPublicMetamorphTalent(false, commonExclusions) == Talent.CROSSFIRE,
				"crossfire is selected from the common pool");
	}

	private static void mealAndMirrorLink(){
		Hero hero = hero(Talent.UPLIFTING_MEAL, 1);
		Talent.onFoodEaten(hero, 0, null);
		check(hero.buff(Swiftthistle.TimeBubble.class) != null, "meal starts time freeze");
		check("3".equals(hero.buff(Swiftthistle.TimeBubble.class).iconTextDisplay()), "rank one grants three turns");

		hero = hero(Talent.MULTIPLE_EXISTENCE, 1);
		Talent.onTalentUpgraded(hero, Talent.MULTIPLE_EXISTENCE);
		check(hero.belongings.getItem(MirrorLink.class) != null, "multiple existence grants mirror link");
		MirrorImage image = new MirrorImage();
		image.pos = 100;
		image.duplicate(hero);
		check(image instanceof DirectableAlly, "mirror uses directable ally logic");
		check(!image.canInteract(hero), "rank one cannot swap with a distant mirror");
		hero.talents.get(0).put(Talent.MULTIPLE_EXISTENCE, 2);
		check(image.canInteract(hero), "rank two can swap with any mirror");
	}

	private static void escapePlan(){
		Hero hero = hero(Talent.ESCAPE_PLAN, 1);
		Char first = target(313);
		Char second = target(311);
		Char distant = target(362);
		check(hero.escapePlanInRange(first), "rank one reaches adjacent target");
		check(!hero.escapePlanInRange(distant), "rank one does not reach two cells");
		check(hero.selectEscapePlanTarget(first) && hero.selectEscapePlanTarget(first), "same target can repeat");
		check(!hero.selectEscapePlanTarget(second), "different target is rejected in same turn");
		hero.talents.get(0).put(Talent.ESCAPE_PLAN, 2);
		check(hero.escapePlanInRange(distant), "rank two reaches two cells");
	}

	private static void mirrorVision(){
		Hero hero = hero(Talent.MULTIPLE_EXISTENCE, 2);
		MirrorImage image = new MirrorImage();
		image.pos = 546;
		image.duplicate(hero);
		Dungeon.level.mobs.clear();
		Dungeon.level.mobs.add(image);
		boolean[] vision = new boolean[Dungeon.level.length()];
		Dungeon.level.updateFieldOfView(hero, vision);
		check(vision[image.pos] && vision[image.pos + 1], "mirror shares its three-by-three area");
		check(!vision[image.pos + 2], "mirror does not share its full sight range");
	}

	private static void firepower(){
		Hero hero = hero(Talent.FIREPOWER_BARRAGE, 1);
		ThrowingStone stone = new ThrowingStone();
		check(stone.castDelay(hero, 313) == 0, "rank one throws instantly");

		WandOfMagicMissile wand = new WandOfMagicMissile();
		hero.talents.get(0).put(Talent.FIREPOWER_BARRAGE, 0);
		Random.pushGenerator(1234);
		int baseWandDamage = wand.damageRoll();
		Random.popGenerator();
		hero.talents.get(0).put(Talent.FIREPOWER_BARRAGE, 2);
		Random.pushGenerator(1234);
		int boostedWandDamage = wand.damageRoll();
		Random.popGenerator();
		check(boostedWandDamage == Math.round(baseWandDamage * 1.5f), "rank two boosts wand damage");

		hero.talents.get(0).put(Talent.FIREPOWER_BARRAGE, 0);
		Random.pushGenerator(5678);
		int baseThrownDamage = stone.damageRoll(hero);
		Random.popGenerator();
		hero.talents.get(0).put(Talent.FIREPOWER_BARRAGE, 2);
		Random.pushGenerator(5678);
		int boostedThrownDamage = stone.damageRoll(hero);
		Random.popGenerator();
		check(boostedThrownDamage == Math.round(baseThrownDamage * 1.5f), "rank two boosts thrown damage");
	}

	//无图标的替身类，headless环境下无法实例化真实药水/卷轴
	private static boolean warpPotionAEffect;
	private static boolean warpPotionBEffect;
	private static boolean warpScrollAEffect;
	private static boolean warpScrollBEffect;
	public static class WarpPotionA extends com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion {
		@Override
		public void apply(Hero hero) { warpPotionAEffect = true; }
	}
	public static class WarpPotionB extends com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion {
		@Override
		public void apply(Hero hero) { warpPotionBEffect = true; }
	}
	public static class WarpScrollA extends com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll {
		@Override
		public void doRead() { warpScrollAEffect = true; }
	}
	public static class WarpScrollB extends com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll {
		@Override
		public void doRead() { warpScrollBEffect = true; }
	}
	public static class WarpRecipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe.SimpleRecipe {
		{
			inputs = new Class[]{WarpPotionB.class};
			inQuantity = new int[]{1};
			cost = 2;
			output = WarpPotionB.class;
			outQuantity = 1;
		}
	}

	private static void realityWarp(){
		com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.setPotionSwap(WarpPotionA.class, WarpPotionB.class);
		check(com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.potionSwapped(), "potion swap recorded");
		check(com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.potionPartner(WarpPotionA.class)
				== WarpPotionB.class, "partner maps forward");
		check(com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.potionPartner(WarpPotionB.class)
				== WarpPotionA.class, "partner maps backward");
		check(com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.potionPartner(
						com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength.class) == null,
				"unswapped potion has no partner");
		check(com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.potionEffect(new WarpPotionA())
				instanceof WarpPotionB, "swapped instance resolved");
		check(com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.potionEffect(new WarpPotionB())
				instanceof WarpPotionA, "swapped instance resolves both ways");
		com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.applyPotionEffect(new WarpPotionA(), Dungeon.hero);
		check(!warpPotionAEffect && warpPotionBEffect, "potion delegates only the swapped effect");

		WarpPotionA physicalA = (WarpPotionA)new WarpPotionA().quantity(2);
		WarpPotionB physicalB = (WarpPotionB)new WarpPotionB().quantity(2);
		physicalA.anonymize();
		physicalB.anonymize();
		java.util.ArrayList<com.shatteredpixel.shatteredpixeldungeon.items.Item> aIngredients =
				new java.util.ArrayList<>(java.util.Arrays.asList(physicalA));
		java.util.ArrayList<com.shatteredpixel.shatteredpixeldungeon.items.Item> bIngredients =
				new java.util.ArrayList<>(java.util.Arrays.asList(physicalB));
		WarpRecipe warpRecipe = new WarpRecipe();
		check(warpRecipe.testIngredients(com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.alchemyIngredients(aIngredients)),
				"physical A is judged as recipe ingredient B");
		check(!warpRecipe.testIngredients(com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.alchemyIngredients(bIngredients)),
				"physical B is no longer judged as recipe ingredient B");
		com.shatteredpixel.shatteredpixeldungeon.items.Item brewed =
				com.shatteredpixel.shatteredpixeldungeon.items.Recipe.brew(warpRecipe, aIngredients);
		check(physicalA.quantity() == 1, "alchemy consumes the physical ingredient");
		check(brewed instanceof WarpPotionB, "alchemy output is not warped");
		check(com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.recipeIngredient(new WarpPotionB())
				instanceof WarpPotionA, "recipe guide displays the physical ingredient");
		check(!com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.scrollSwapped(), "scroll swap unset");

		com.watabou.utils.Bundle bundle = new com.watabou.utils.Bundle();
		com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.storeInBundle(bundle);
		com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.setPotionSwap(null, null);
		check(!com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.potionSwapped(), "swap cleared");
		com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.restoreFromBundle(bundle);
		check(com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.potionPartner(WarpPotionA.class)
				== WarpPotionB.class, "swap survives save and load");
		com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.restoreFromBundle(new com.watabou.utils.Bundle());
		check(!com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.potionSwapped()
				&& !com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.scrollSwapped(),
				"loading a save without swaps clears prior static state");

		Hero warpHero = hero(Talent.REALITY_WARP, 1);
		check(Talent.realityWarpSelectionPending(warpHero), "potion selection pending at rank one");
		com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.setScrollSwap(WarpScrollA.class, WarpScrollB.class);
		com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.scrollEffect(new WarpScrollA()).doRead();
		check(!warpScrollAEffect && warpScrollBEffect, "scroll delegates only the swapped effect");
		warpHero.talents.get(0).put(Talent.REALITY_WARP, 2);
		check(Talent.realityWarpSelectionPending(warpHero), "scroll selection pending at rank two");
		check(!Talent.realityWarpSelectionPending(hero(Talent.REALITY_WARP, 0)), "no selection pending at rank zero");
		com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.setPotionSwap(WarpPotionA.class, WarpPotionB.class);
		check(!Talent.realityWarpSelectionPending(warpHero), "selection complete");

		//互换只属于被选中的基础药水和卷轴，炼金产物不会继续互换
		com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.setPotionSwap(
				com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfFrost.class,
				com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing.class);
		check(com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.resolvePotion(
						com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfSnapFreeze.class)
				== com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfSnapFreeze.class,
				"exotic potion output keeps its normal identity");
		check(com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.resolvePotion(
						com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShielding.class)
				== com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShielding.class,
				"other exotic potion output also stays normal");
		check(com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.resolvePotion(
						com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfFrost.class)
				== com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing.class,
				"regular potion resolves directly");
		check(com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.resolvePotion(
						com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame.class)
				== com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame.class,
				"unswapped potion resolves to itself");
		com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.setScrollSwap(
				com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify.class,
				com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging.class);
		check(com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.resolveScroll(
						com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfDivination.class)
				== com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfDivination.class,
				"exotic scroll output keeps its normal identity");
		check(com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp.resolveScroll(
						com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping.class)
				== com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping.class,
				"unswapped scroll resolves to itself");
	}

	private static void manaOverload(){
		Hero hero = hero(Talent.MANA_OVERLOAD, 0);
		com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile wand =
				new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile();
		wand.curCharges = 0;
		check(!wand.tryToZap(hero, 313), "empty wand fizzles without talent");
		hero.talents.get(0).put(Talent.MANA_OVERLOAD, 1);
		check(wand.tryToZap(hero, 313), "empty wand casts with talent");
		wand.spendCharge(1);
		check(wand.curCharges == -1, "empty wand spends into debt");
		check(!wand.tryToZap(hero, 313), "negative charge wand cannot recast");
		check(wand.rechargeReferenceCharge() == 0, "wand debt recharges from the zero-charge rate");
		check(wand.adjustRechargeGain(hero, 0.25f) == 0.25f, "rank one recovers at normal pace");
		hero.talents.get(0).put(Talent.MANA_OVERLOAD, 2);
		check(wand.adjustRechargeGain(hero, 0.25f) == 0.5f, "rank two recovers at double pace");

		TestChargedArtifact artifact = new TestChargedArtifact();
		check(artifact instanceof com.shatteredpixel.shatteredpixeldungeon.items.ChargeItem,
				"standard artifact automatically implements charge rules");
		artifact.setCharge(0, 10, 0f);
		check(artifact.canSpendCharge(hero, 3), "standard artifact can spend from zero with talent");
		artifact.spendCharge(3);
		check(artifact.currentCharge() == -3 && !artifact.canSpendCharge(hero, 1),
				"standard artifact records debt and blocks repeated spending");
		check(artifact.rechargeReferenceCharge() == 0, "artifact debt uses its zero-charge rate");
		artifact.setCharge(-1, 10, 0.75f);
		artifact.gain(hero, 1f);
		check(artifact.currentCharge() == 0 && artifact.partialCharge() == 0f,
				"debt recovery stops exactly at zero without accelerated overflow");
		artifact.gain(hero, 0.25f);
		check(artifact.currentCharge() == 0 && artifact.partialCharge() == 0.25f,
				"positive recharge resumes at normal speed");

		TestPlainArtifact plain = new TestPlainArtifact();
		check(!(plain instanceof com.shatteredpixel.shatteredpixeldungeon.items.ChargeItem),
				"plain artifact does not opt into mana overload");
		check(new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows()
				instanceof com.shatteredpixel.shatteredpixeldungeon.items.ChargeItem, "cloak uses standard charges");
		check(new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome()
				instanceof com.shatteredpixel.shatteredpixeldungeon.items.ChargeItem, "tome uses standard charges");
		check(new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty()
				instanceof com.shatteredpixel.shatteredpixeldungeon.items.ChargeItem, "horn uses standard charges");
		check(new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.LloydsBeacon()
				instanceof com.shatteredpixel.shatteredpixeldungeon.items.ChargeItem, "beacon uses standard charges");
		check(new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband()
				instanceof com.shatteredpixel.shatteredpixeldungeon.items.ChargeItem, "armband uses standard charges");
		check(new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SkeletonKey()
				instanceof com.shatteredpixel.shatteredpixeldungeon.items.ChargeItem, "key uses standard charges");
		check(new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass()
				instanceof com.shatteredpixel.shatteredpixeldungeon.items.ChargeItem, "hourglass uses standard charges");
		check(new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.UnstableSpellbook()
				instanceof com.shatteredpixel.shatteredpixeldungeon.items.ChargeItem, "spellbook uses standard charges");
		check(!(new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose()
				instanceof com.shatteredpixel.shatteredpixeldungeon.items.ChargeItem), "rose is excluded");
		check(!(new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood()
				instanceof com.shatteredpixel.shatteredpixeldungeon.items.ChargeItem), "chalice is excluded");
		check(!(new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.EtherealChains()
				instanceof com.shatteredpixel.shatteredpixeldungeon.items.ChargeItem), "chains are excluded");
		check(!(new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SandalsOfNature()
				instanceof com.shatteredpixel.shatteredpixeldungeon.items.ChargeItem), "sandals are excluded");
		check(!(new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight()
				instanceof com.shatteredpixel.shatteredpixeldungeon.items.ChargeItem), "talisman is excluded");

		Hero actionHero = hero(Talent.MANA_OVERLOAD, 2);
		TestHorn horn = new TestHorn();
		actionHero.belongings.artifact = horn;
		horn.setCharge(0);
		check(horn.actions(actionHero).contains(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty.AC_EAT),
				"standard artifact action is available for a zero-charge overload");
		horn.setCharge(-1);
		check(!horn.actions(actionHero).contains(com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty.AC_EAT),
				"standard artifact action is hidden while in debt");

		TestArmband armband = new TestArmband();
		armband.setCharge(2);
		com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband.Thievery thievery =
				armband.new Thievery();
		int shopCost = thievery.chargesToUse(new ThrowingStone());
		hero.talents.get(0).put(Talent.MANA_OVERLOAD, 0);
		check(thievery.chargesToUse(new ThrowingStone()) == shopCost,
				"shop theft charge calculation ignores mana overload");
	}

	public static class TestChargedArtifact extends
			com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChargedArtifact {
		public void setCharge(int charge, int cap, float partial) {
			this.charge = charge;
			this.chargeCap = cap;
			this.partialCharge = partial;
		}
		public void gain(Hero hero, float amount) {
			gainChargeProgress(hero, amount);
			while (partialCharge >= 1f && charge < chargeCap) {
				charge++;
				partialCharge--;
			}
		}
	}

	public static class TestPlainArtifact extends
			com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact {
	}

	public static class TestArmband extends
			com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband {
		public void setCharge(int charge) {
			this.charge = charge;
		}
	}

	public static class TestHorn extends
			com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty {
		public void setCharge(int charge) {
			this.charge = charge;
		}
	}

	private static void savageParasite(){
		Hero hero = hero(Talent.SAVAGE_PARASITE, 1);
		Char living = target(313);
		check(Talent.canParasitize(hero, living), "living enemy can be parasitized");

		Char inorganic = new Char(){
			{
				properties.add(Char.Property.INORGANIC);
			}
			@Override
			public boolean isImmune(Class effect){ return false; }
		};
		inorganic.alignment = Char.Alignment.ENEMY;
		inorganic.pos = 314;
		inorganic.HP = inorganic.HT = 10;
		Actor.add(inorganic);
		check(!Talent.canParasitize(hero, inorganic), "inorganic enemy resists at rank one");

		Char undead = new Char(){
			{
				properties.add(Char.Property.UNDEAD);
			}
			@Override
			public boolean isImmune(Class effect){ return false; }
		};
		undead.alignment = Char.Alignment.ENEMY;
		undead.pos = 362;
		undead.HP = undead.HT = 10;
		Actor.add(undead);
		check(!Talent.canParasitize(hero, undead), "undead enemy resists at rank one");

		hero.talents.get(0).put(Talent.SAVAGE_PARASITE, 2);
		check(Talent.canParasitize(hero, inorganic) && Talent.canParasitize(hero, undead),
				"rank two works on non-living targets");
		check(!Talent.canParasitize(hero, hero), "hero cannot parasite itself");

		living.HP = living.HT = 50;
		hero.HP = 10;
		com.shatteredpixel.shatteredpixeldungeon.plants.Plant.Seed seed =
				new com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Seed();
		check(Talent.trySavageParasite(hero, seed, living), "seed parasitizes the target");
		check(living.HP == 45, "drain takes ten percent of max hp");
		check(hero.HP == 10, "drain does not heal the hero");
		Talent.SavageParasitism parasite = living.buff(Talent.SavageParasitism.class);
		check(parasite != null && parasite.seedClass == seed.getClass(), "parasite buff stores the seed");
		java.util.Arrays.fill(Dungeon.level.heroFOV, false); //无头环境没有渲染场景
		parasite.trigger();
		check(living.buff(Talent.SavageParasitism.class) == null, "parasite detaches after triggering");
		//自然之履式催发：直接结算种子效果，不生成植物
		check(living.buff(com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass.Health.class) != null,
				"triggered seed resolves its effect on the host");
		check(Dungeon.level.plants.get(living.pos) == null, "no plant is spawned into the level");

		//吸取可以致其死亡
		Char weak = new Char(){
			@Override
			public void die(Object src){ HP = 0; }
			@Override
			public boolean isImmune(Class effect){ return false; }
		};
		weak.alignment = Char.Alignment.ENEMY;
		weak.pos = 363;
		weak.HP = 5;
		weak.HT = 50;
		Actor.add(weak);
		check(Talent.trySavageParasite(hero, seed, weak), "seed drains the weakened target");
		check(!weak.isAlive() && weak.HP == 0, "drain can kill the target");
		check(weak.buff(Talent.SavageParasitism.class) == null, "no parasite attaches to a drained corpse");
	}

	private static void perfectExecution(){
		Hero hero = hero(Talent.PERFECT_EXECUTION, 1);
		Char target = new Char(){
			@Override
			public void die(Object src){ HP = 0; }
			@Override
			public boolean isImmune(Class effect){ return false; }
		};
		target.alignment = Char.Alignment.ENEMY;
		target.pos = 313;
		target.HP = target.HT = 100;
		Actor.add(target);

		//+1按 投掷→法杖→符石 顺序叠层，顺序不符不叠层
		Talent.onExecutionStack(hero, Talent.EXEC_COND_WAND);
		Talent.PerfectExecutionTracker tracker = hero.buff(Talent.PerfectExecutionTracker.class);
		check(tracker != null && tracker.conds == 0, "out of order condition does not stack");
		check(tracker.icon() == BuffIndicator.NONE, "icon hidden while stacking");
		Talent.onExecutionStack(hero, Talent.EXEC_COND_THROWN);
		check(tracker.conds == Talent.EXEC_COND_THROWN, "thrown condition recorded first");
		Talent.onExecutionStack(hero, Talent.EXEC_COND_RUNE);
		check(tracker.conds == Talent.EXEC_COND_THROWN, "rune before wand does not stack at rank one");
		Talent.onExecutionStack(hero, Talent.EXEC_COND_WAND);
		check(tracker.conds == Talent.EXEC_COND_ALL - Talent.EXEC_COND_RUNE, "two conditions stack in order");
		Talent.onExecutionStack(hero, Talent.EXEC_COND_RUNE);
		check(tracker.conds == Talent.EXEC_COND_ALL, "three conditions in order become ready");
		check(tracker.icon() != BuffIndicator.NONE, "icon shows when ready");
		check(target.HP == 100, "no execution before the melee finisher");

		//手中武器命中是终结技：三层叠满后近战命中触发处决
		check(Talent.tryExecuteOnAttack(hero, target), "melee finisher executes the target");
		check(!target.isAlive() && target.HP == 0, "execution costs all remaining health");

		//+2任意顺序叠层
		Hero hero2 = hero(Talent.PERFECT_EXECUTION, 2);
		Talent.onExecutionStack(hero2, Talent.EXEC_COND_RUNE);
		Talent.onExecutionStack(hero2, Talent.EXEC_COND_THROWN);
		Talent.onExecutionStack(hero2, Talent.EXEC_COND_WAND);
		Talent.PerfectExecutionTracker tracker2 = hero2.buff(Talent.PerfectExecutionTracker.class);
		check(tracker2 != null && tracker2.conds == Talent.EXEC_COND_ALL, "rank two stacks in any order");

		//不能处决同一个怪物两次：boss承受死神伤害的一半，且第二次不再处决
		Char boss = new Char(){
			{
				properties.add(Char.Property.BOSS);
			}
			@Override
			public void die(Object src){ HP = 0; }
			@Override
			public boolean isImmune(Class effect){ return false; }
		};
		boss.alignment = Char.Alignment.ENEMY;
		boss.pos = 314;
		boss.HP = boss.HT = 100;
		Actor.add(boss);
		Talent.onExecutionStack(hero, Talent.EXEC_COND_THROWN);
		Talent.onExecutionStack(hero, Talent.EXEC_COND_WAND);
		Talent.onExecutionStack(hero, Talent.EXEC_COND_RUNE);
		check(Talent.tryExecuteOnAttack(hero, boss), "boss execution goes through");
		check(boss.HP == 50 && boss.isAlive(), "boss takes half as grim damage");
		Talent.onExecutionStack(hero, Talent.EXEC_COND_THROWN);
		Talent.onExecutionStack(hero, Talent.EXEC_COND_WAND);
		Talent.onExecutionStack(hero, Talent.EXEC_COND_RUNE);
		check(!Talent.tryExecuteOnAttack(hero, boss), "same monster cannot be executed twice");
		check(boss.HP == 50, "second execution attempt leaves boss untouched");
	}

	private static void crossfire(){
		Hero hero = hero(Talent.CROSSFIRE, 1);
		Talent.CrossfireTracker cross = Buff.affect(hero, Talent.CrossfireTracker.class);
		Char target = target(313);
		Char ally = new Char(){ };
		ally.alignment = Char.Alignment.ALLY;
		ally.pos = 314;
		ally.HP = ally.HT = 100;
		Actor.add(ally);
		target.HP = target.HT = 1000;

		//英雄尚未攻击时连段未激活：图标隐藏，友方攻击不叠加、伤害不递增
		check(cross.icon() == BuffIndicator.NONE, "icon hidden before any attack");
		cross.onAllyAttack();
		target.damage(10, ally);
		check(target.HP == 990, "no ramp before hero attacks");

		//英雄攻击动作做出（无论命中）即重置为1层并显示图标
		cross.onHeroAttack();
		check(cross.stacks == 1 && cross.icon() != BuffIndicator.NONE, "hero attack resets to one visible stack");
		cross.act(); //英雄行动结束，连段保持
		//友方攻击动作做出（无论命中）叠加1层，伤害按攻击前已有的层数递增（每层+100%，首次仅吃英雄攻击那1层）
		cross.onAllyAttack();
		check(cross.stacks == 2, "ally attack adds a stack");
		target.damage(10, ally);
		check(target.HP == 990 - 20, "first ally hit deals double damage");
		cross.onAllyAttack();
		target.damage(10, ally);
		check(target.HP == 970 - 30, "second ally hit ramps to triple damage");

		//英雄再次攻击：重置为一层重新开始，不会一直叠加
		cross.onHeroAttack();
		cross.act();
		check(cross.stacks == 1, "hero attack restarts from one stack");
		cross.onAllyAttack();
		target.damage(10, ally);
		check(target.HP == 940 - 20, "ramp restarts from double after hero attack");

		//英雄执行非攻击操作：连段结束，图标隐藏，友方伤害回到一倍
		cross.act();
		check(cross.stacks == 0 && cross.icon() == BuffIndicator.NONE, "non attack action ends the chain");
		target.damage(10, ally);
		check(target.HP == 920 - 10, "ally damage returns to normal after hero acts");
	}

	private static void setup(){
		WarriorTalentsRegression.setupHeadless();
		Dungeon.level.setSize(25, 25);
		Arrays.fill(Dungeon.level.map, Terrain.EMPTY);
		Dungeon.level.mobs = new HashSet<>();
		Dungeon.level.blobs = new java.util.HashMap<>();
		Dungeon.level.heaps = new com.watabou.utils.SparseArray<>();
		Dungeon.level.plants = new com.watabou.utils.SparseArray<>();
		Dungeon.level.traps = new com.watabou.utils.SparseArray<>();
		Dungeon.level.buildFlagMaps();
		Dungeon.level.heroFOV = new boolean[Dungeon.level.length()];
	}

	public static void main(String[] args){
		setup();
		if (args.length > 0 && "talent-pools".equals(args[0])) {
			indexingAndPools();
			System.out.println("PASS: public talent common and rare pools");
			return;
		}
		if (args.length > 0 && "mana-overload".equals(args[0])) {
			manaOverload();
			System.out.println("PASS: mana overload standard charge rules");
			return;
		}
		indexingAndPools();
		mealAndMirrorLink();
		escapePlan();
		mirrorVision();
		firepower();
		realityWarp();
		manaOverload();
		savageParasite();
		perfectExecution();
		crossfire();
		System.out.println("PASS: public talent indexing, tiers, meal, mirror link, escape plan, firepower, reality warp, mana overload, savage parasite, perfect execution and crossfire");
	}
}
