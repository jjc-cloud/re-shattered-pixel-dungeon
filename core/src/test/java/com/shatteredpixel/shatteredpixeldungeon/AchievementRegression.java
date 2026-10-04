package com.shatteredpixel.shatteredpixeldungeon;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.shatteredpixel.shatteredpixeldungeon.Badges.Badge;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.PetrifiedStatue;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogFist;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndResurrect;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameSettings;
import com.watabou.utils.PathFinder;
import com.watabou.utils.SparseArray;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/** 使用内存成就和真实中文资源，验证判定边界、事件入口和存读档，不写玩家存档。 */
public class AchievementRegression {
	private static int checks;
	private static int banners;
	private static final List<String> logs = new ArrayList<>();
	private static final Badge[] SPEED = {Badge.SPEEDRUN_1, Badge.SPEEDRUN_2, Badge.SPEEDRUN_3};

	public static void main(String[] args) throws Exception {
		new Game(GameScene.class, null);
		Gdx.app = (Application) Proxy.newProxyInstance(Application.class.getClassLoader(),
				new Class<?>[]{Application.class}, (proxy, method, values) -> {
					if (method.getName().equals("getType")) return Application.ApplicationType.Desktop;
					if (method.getName().equals("postRunnable")) banners++;
					if (method.getName().equals("log")) logs.add((String) values[1]);
					if (method.getName().equals("error")) throw new AssertionError(Arrays.toString(values));
					if (method.getReturnType() == int.class) return 0;
					return null;
				});
		Gdx.files = (Files) Proxy.newProxyInstance(Files.class.getClassLoader(),
				new Class<?>[]{Files.class}, (proxy, method, values) -> {
					if (!method.getName().equals("internal")) throw new AssertionError("Unexpected file access: " + method);
					return new FileHandle(new File("core/src/main/assets", (String) values[0]));
				});
		GameSettings.set((Preferences) Proxy.newProxyInstance(Preferences.class.getClassLoader(),
				new Class<?>[]{Preferences.class}, (proxy, method, values) -> {
					if (method.getName().startsWith("get") && values != null && values.length == 2) return values[1];
					if (method.getReturnType() == boolean.class) return false;
					return null;
				}));
		GdxNativesLoader.load();
		TextureCache.create(Assets.Sprites.ITEMS, 256, 1024);
		TextureCache.create(Assets.Sprites.ITEM_ICONS, 128, 128);
		TextureCache.create(Assets.Effects.EFFECTS, 64, 64);
		Camera.main = new Camera(0, 0, 320, 240, 1);
		Messages.setup(Languages.CHI_SMPL);
		speedruns();
		chaos();
		victoryConditions();
		persistenceAndRepeat();
		statues();
		fists();
		allies();
		seedAndTexts();
		System.out.println("AchievementRegression passed: " + checks + " checks");
	}

	private static void reset() throws Exception {
		Actor.clear();
		Dungeon.level = null;
		Dungeon.hero = new Hero() {
			@Override public void interrupt() { }
		};
		Dungeon.hero.heroClass = HeroClass.WARRIOR;
		Dungeon.hero.sprite = new QuietSprite();
		Dungeon.challenges = 0;
		Dungeon.customSeedText = "";
		GamesInProgress.randomizedClass = false;
		Statistics.reset();
		Field global = Badges.class.getDeclaredField("global");
		global.setAccessible(true);
		global.set(null, new HashSet<Badge>());
		Badges.reset();
		Statistics.duration = 9000;
		Statistics.foodEaten = Statistics.itemsCrafted = 1;
		Dungeon.hero.STR = 11;
		Talent.initClassTalents(Dungeon.hero);
		logs.clear();
		banners = 0;
	}

	private static void speedruns() throws Exception {
		float[] turns = {8000.25f, 8000, 6000.25f, 6000, 4000.25f, 4000, 0};
		int[] highest = {-1, 0, 0, 1, 1, 2, 2};
		for (int i = 0; i < turns.length; i++) {
			reset();
			Statistics.duration = turns[i];
			Badges.validateVictory();
			for (int j = 0; j < SPEED.length; j++) {
				check(Badges.isUnlocked(SPEED[j]) == (j <= highest[i]), "speed boundary " + turns[i] + ": " + SPEED[j]);
				check(Badges.filterReplacedBadges(false).contains(SPEED[j]) == (j == highest[i]), "local tier filter");
				check(Badges.filterReplacedBadges(true).contains(SPEED[j]) == (j == highest[i]), "global tier filter");
			}
			long speedNotices = logs.stream().filter(s -> s.contains("速通玩家") || s.contains("借过一下") || s.contains("？？？")).count();
			check(speedNotices == (highest[i] < 0 ? 0 : 1), "one highest speed notification");
		}
		reset();
		Statistics.duration = 7999;
		Field now = Actor.class.getDeclaredField("now");
		now.setAccessible(true);
		now.setFloat(null, 1.25f);
		Badges.validateVictory();
		check(!Badges.isUnlocked(Badge.SPEEDRUN_1), "include current floor time");
		List<Badge> locked = new ArrayList<>(Arrays.asList(SPEED));
		Badges.filterBadgesWithoutPrerequisites(locked);
		check(locked.equals(Arrays.asList(Badge.SPEEDRUN_1)), "show first locked speed tier");
		locked.remove(Badge.SPEEDRUN_1);
		locked.addAll(Arrays.asList(Badge.SPEEDRUN_2, Badge.SPEEDRUN_3));
		Badges.filterBadgesWithoutPrerequisites(locked);
		check(locked.equals(Arrays.asList(Badge.SPEEDRUN_2)), "show next locked speed tier");
	}

	private static void chaos() throws Exception {
		for (int omitted : new int[]{0, Challenges.NO_FOOD, Challenges.OUTDATED_DESIGN}) {
			reset();
			Statistics.qualifiedForRandomVictoryBadge = true;
			Dungeon.hero.subClass = HeroSubClass.GLADIATOR;
			Dungeon.hero.armorAbility = new HeroicLeap();
			Dungeon.challenges = Challenges.MAX_VALUE & ~omitted;
			Badges.validateVictory();
			check(Badges.isUnlocked(Badge.VICTORY_RANDOM), "random victory retained");
			check(Badges.isUnlocked(Badge.VICTORY_CHAOS) == (omitted == 0), "all challenges required");
			check(Badges.filterReplacedBadges(true).contains(Badge.VICTORY_RANDOM) == (omitted != 0), "chaos replaces random");
			if (omitted == 0) check(logs.stream().noneMatch(s -> s.contains("随机获胜！")), "only notify chaos tier");
		}
		for (int missing = 0; missing < 3; missing++) {
			reset();
			Dungeon.challenges = Challenges.MAX_VALUE;
			Statistics.qualifiedForRandomVictoryBadge = missing != 0;
			Dungeon.hero.subClass = missing == 1 ? HeroSubClass.NONE : HeroSubClass.GLADIATOR;
			Dungeon.hero.armorAbility = missing == 2 ? null : new HeroicLeap();
			Badges.validateVictory();
			check(!Badges.isUnlocked(Badge.VICTORY_CHAOS), "chaos qualification " + missing);
			check(!Badges.isUnlocked(Badge.VICTORY_RANDOM), "random qualification " + missing);
		}
	}

	private static void victoryConditions() throws Exception {
		for (int value = 0; value <= 1; value++) {
			reset();
			Statistics.foodEaten = Statistics.itemsCrafted = value;
			Dungeon.hero.STR = 10 + value;
			Badges.validateVictory();
			check(Badges.isUnlocked(Badge.VICTORY_NO_FOOD) == (value == 0), "existing food statistic");
			check(Badges.isUnlocked(Badge.VICTORY_NO_ALCHEMY) == (value == 0), "existing alchemy statistic");
			check(Badges.isUnlocked(Badge.VICTORY_LOW_STRENGTH) == (value == 0), "base strength boundary");
		}
		for (int points : new int[]{0, 1, 36, 37, 38}) {
			reset();
			// 分散在四阶，确认统计总点数而非天赋种数或单阶点数。
			Dungeon.hero.talents.get(0).put(Talent.HEARTY_MEAL, points / 4);
			Dungeon.hero.talents.get(1).put(Talent.IRON_WILL, points / 4);
			Dungeon.hero.talents.get(2).put(Talent.HEROIC_ENERGY, points / 4);
			Dungeon.hero.talents.get(3).put(Talent.LETHAL_MOMENTUM, points - 3 * (points / 4));
			Badges.validateVictory();
			check(Badges.isUnlocked(Badge.VICTORY_NO_TALENTS) == (points == 0), "zero talents " + points);
			check(Badges.isUnlocked(Badge.VICTORY_MANY_TALENTS) == (points >= 37), "37 talent points " + points);
		}
		reset();
		Dungeon.hero.upgradeTalent(Talent.HEARTY_MEAL);
		check(Statistics.talentsActivated, "real upgrade records activation");
		Dungeon.hero.talents.get(0).put(Talent.HEARTY_MEAL, 0);
		Badges.validateVictory();
		check(!Badges.isUnlocked(Badge.VICTORY_NO_TALENTS), "reset talents cannot restore qualification");
	}

	private static void persistenceAndRepeat() throws Exception {
		reset();
		Dungeon.hero.upgradeTalent(Talent.HEARTY_MEAL);
		Statistics.foodEaten = 3;
		Statistics.itemsCrafted = 4;
		Bundle stats = new Bundle();
		Statistics.storeInBundle(stats);
		Statistics.reset();
		check(!Statistics.talentsActivated, "new run clears activation");
		Statistics.restoreFromBundle(stats);
		check(Statistics.talentsActivated && Statistics.foodEaten == 3 && Statistics.itemsCrafted == 4, "restore existing counters and activation");
		Badges.validateVictory();
		Bundle badges = new Bundle();
		Badges.saveLocal(badges);
		Badges.reset();
		Badges.loadLocal(badges);
		Dungeon.hero.talents.get(0).put(Talent.HEARTY_MEAL, 37);
		Statistics.duration = 0;
		Statistics.foodEaten = Statistics.itemsCrafted = 0;
		Dungeon.hero.STR = 10;
		int previousBanners = banners;
		Badges.validateVictory();
		check(!Badges.isUnlocked(Badge.VICTORY_MANY_TALENTS), "no talents award after first victory");
		check(!Badges.isUnlocked(Badge.SPEEDRUN_3) && !Badges.isUnlocked(Badge.VICTORY_NO_FOOD)
				&& !Badges.isUnlocked(Badge.VICTORY_LOW_STRENGTH) && !Badges.isUnlocked(Badge.VICTORY_NO_ALCHEMY), "no reevaluation after load");
		check(banners == previousBanners, "no duplicate victory banners");
		Bundle higherOnly = new Bundle();
		higherOnly.put("badges", new String[]{"VICTORY_CHAOS", "SPEEDRUN_3"});
		HashSet<Badge> restored = Badges.restore(higherOnly);
		check(restored.containsAll(Arrays.asList(Badge.VICTORY_RANDOM, Badge.VICTORY_CHAOS,
				Badge.SPEEDRUN_1, Badge.SPEEDRUN_2, Badge.SPEEDRUN_3)), "restore lower tiers");
	}

	private static void statues() throws Exception {
		for (boolean object : new boolean[]{false, true}) {
			reset();
			PetrifiedStatue statue = new PetrifiedStatue();
			if (object) statue.properties().add(Char.Property.OBJECT);
			statue.damage(1, Dungeon.hero);
			check(!statue.isAlive() && Badges.isUnlocked(Badge.PETRIFIED_STATUE_SHATTERED), "real statue break " + object);
			int count = banners;
			statue.damage(1, Dungeon.hero);
			new PetrifiedStatue().damage(1, Dungeon.hero);
			check(banners == count, "one statue award per run");
			check(Statistics.enemiesSlain == 0, "statue remains outside kill statistics");
		}
	}

	private static void fists() throws Exception {
		for (boolean bright : new boolean[]{false, true}) {
			reset();
			arena();
			YogFist fist = fist(bright);
			fist.damage(1000, Dungeon.hero);
			check(!fist.isAlive() && !fist.hasTeleported && Badges.isUnlocked(Badge.FIST_WITHOUT_TELEPORT), "kill before blink " + bright);
			reset();
			arena();
			fist = fist(bright);
			fist.damage(150, Dungeon.hero);
			check(fist.isAlive() && fist.hasTeleported, "half health triggers blink " + bright);
			Bundle saved = new Bundle();
			fist.storeInBundle(saved);
			YogFist loaded = fist(bright);
			loaded.restoreFromBundle(saved);
			check(loaded.hasTeleported, "blink persists " + bright);
			loaded.damage(1000, Dungeon.hero);
			check(!Badges.isUnlocked(Badge.FIST_WITHOUT_TELEPORT), "blink disqualifies after reload " + bright);
			reset();
			arena();
			fist = fist(bright);
			ScrollOfTeleportation.appear(fist, 14 * 20 + 14);
			check(!fist.hasTeleported, "external teleport is not blink skill");
			fist.damage(1000, Dungeon.hero);
			check(Badges.isUnlocked(Badge.FIST_WITHOUT_TELEPORT), "external teleport retains qualification");
		}
		reset();
		Badges.validateFistSlain(new YogFist.BurningFist());
		check(!Badges.isUnlocked(Badge.FIST_WITHOUT_TELEPORT), "other fist cannot qualify");
	}

	private static void allies() throws Exception {
		for (Char.Alignment alignment : Char.Alignment.values()) {
			reset();
			Mob source = new Mob() { };
			source.alignment = alignment;
			Badges.validateDeathFromAlly(source);
			check(Badges.isUnlocked(Badge.DEATH_FROM_ALLY) == (alignment == Char.Alignment.ALLY), "death alignment " + alignment);
		}
		reset();
		Badges.validateDeathFromAlly(Dungeon.hero);
		Badges.validateDeathFromAlly(new Object());
		Badges.validateDeathFromAlly(null);
		check(!Badges.isUnlocked(Badge.DEATH_FROM_ALLY), "exclude self and non-unit causes");
		// 普通十字架的真实死亡入口会等待复活，不应提前判定为最终死亡。
		Dungeon.hero.belongings.backpack.items.add(new Ankh());
		Mob ally = new Mob() { };
		ally.alignment = Char.Alignment.ALLY;
		Dungeon.hero.HP = 0;
		Dungeon.hero.die(ally);
		check(WndResurrect.instance != null && !Badges.isUnlocked(Badge.DEATH_FROM_ALLY), "ankh resurrection does not award betrayal");
		WndResurrect.instance = null;
	}

	private static void seedAndTexts() throws Exception {
		reset();
		Dungeon.customSeedText = "fixed-seed";
		Statistics.duration = 0;
		Statistics.foodEaten = Statistics.itemsCrafted = 0;
		Badges.validateVictory();
		Badges.validatePetrifiedStatueShattered();
		Badges.validateFistSlain(new YogFist.BrightFist());
		Mob ally = new Mob() { };
		ally.alignment = Char.Alignment.ALLY;
		Badges.validateDeathFromAlly(ally);
		check(Badges.allUnlocked().isEmpty() && banners == 0, "seeded runs cannot unlock new badges");
		for (Badge badge : new Badge[]{Badge.VICTORY_CHAOS, Badge.PETRIFIED_STATUE_SHATTERED,
				Badge.FIST_WITHOUT_TELEPORT, Badge.DEATH_FROM_ALLY, Badge.SPEEDRUN_1, Badge.SPEEDRUN_2,
				Badge.SPEEDRUN_3, Badge.VICTORY_NO_FOOD, Badge.VICTORY_LOW_STRENGTH,
				Badge.VICTORY_NO_ALCHEMY, Badge.VICTORY_NO_TALENTS, Badge.VICTORY_MANY_TALENTS}) {
			check(!badge.title().equals(Messages.NO_TEXT_FOUND) && !badge.desc().equals(Messages.NO_TEXT_FOUND), "Chinese text " + badge);
		}
		check(Badge.SPEEDRUN_3.title().equals("？？？"), "keep requested third-tier title");
	}

	private static void arena() {
		TestLevel level = new TestLevel();
		level.setSize(20, 20);
		level.mobs = new HashSet<>();
		level.blobs = new HashMap<>();
		level.heaps = new SparseArray<>();
		level.traps = new SparseArray<>();
		level.plants = new SparseArray<>();
		level.transitions = new ArrayList<>();
		Arrays.fill(level.map, Terrain.WALL);
		for (int y = 1; y < 19; y++) for (int x = 1; x < 19; x++) level.map[y * 20 + x] = Terrain.EMPTY;
		level.buildFlagMaps();
		PathFinder.setMapSize(20, 20);
		Dungeon.level = level;
		Dungeon.depth = 25;
		Dungeon.hero.pos = 21;
		new Group().add(Dungeon.hero.sprite);
	}

	private static YogFist fist(boolean bright) {
		YogFist fist = bright ? new YogFist.BrightFist() : new YogFist.DarkFist();
		fist.pos = 15 * 20 + 15;
		fist.sprite = new QuietSprite();
		new Group().add(fist.sprite);
		return fist;
	}

	private static void check(boolean condition, String message) {
		checks++;
		if (!condition) throw new AssertionError(message);
	}

	private static class QuietSprite extends CharSprite {
		@Override public void die() { }
		@Override public void showStatusWithIcon(int color, String text, int icon, Object... args) { }
	}

	public static class TestLevel extends Level {
		@Override protected boolean build() { return true; }
		@Override protected void createMobs() { }
		@Override protected void createItems() { }
		@Override public int exit() { return 21; }
	}
}
