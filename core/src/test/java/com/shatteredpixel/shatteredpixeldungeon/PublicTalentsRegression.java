package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
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
		check(Talent.publicMetamorphTalentsAvailable(1) && Talent.publicMetamorphTalentsAvailable(2), "public tier one and two");
		check(!Talent.publicMetamorphTalentsAvailable(3) && !Talent.publicMetamorphTalentsAvailable(4), "no public tier three or four");
		check(Talent.randomPublicMetamorphTalent(true, new HashSet<>()) == Talent.FIREPOWER_BARRAGE, "rare pool");
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

	public static void main(String[] args){
		WarriorTalentsRegression.setupHeadless();
		Dungeon.level.setSize(25, 25);
		Arrays.fill(Dungeon.level.map, Terrain.EMPTY);
		Dungeon.level.mobs = new HashSet<>();
		Dungeon.level.blobs = new java.util.HashMap<>();
		Dungeon.level.heaps = new com.watabou.utils.SparseArray<>();
		Dungeon.level.plants = new com.watabou.utils.SparseArray<>();
		Dungeon.level.traps = new com.watabou.utils.SparseArray<>();
		Dungeon.level.buildFlagMaps();
		indexingAndPools();
		mealAndMirrorLink();
		escapePlan();
		mirrorVision();
		firepower();
		System.out.println("PASS: public talent indexing, tiers, meal, mirror link, escape plan and firepower");
	}
}
