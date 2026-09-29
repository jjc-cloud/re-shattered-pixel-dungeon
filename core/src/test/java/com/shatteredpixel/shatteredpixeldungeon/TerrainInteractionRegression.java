package com.shatteredpixel.shatteredpixeldungeon;

import com.badlogic.gdx.utils.IntArray;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Electricity;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.HiddenLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.CityLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.TerrainInteractions;
import com.shatteredpixel.shatteredpixeldungeon.levels.TerrainInteractions.Rule;
import com.shatteredpixel.shatteredpixeldungeon.levels.TerrainInteractions.Response;
import com.shatteredpixel.shatteredpixeldungeon.levels.TerrainInteractions.Source;
import com.shatteredpixel.shatteredpixeldungeon.levels.TerrainPropagation;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.shatteredpixel.shatteredpixeldungeon.tiles.custom.Carpet;
import com.watabou.utils.Callback;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.SparseArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

/** 验证类别规则，不依赖渲染或特定物品名称。 */
public class TerrainInteractionRegression {
	private static int checks;
	private static final Rule BREAK = Rule.NONE
			.on(Source.EXPLOSION, Response.replaceWith(Terrain.EMPTY_DECO))
			.on(Source.DISINTEGRATION, Response.replaceWith(Terrain.EMPTY_DECO));
	private static final Rule METAL = Rule.LEGACY.on(Source.ELECTRIC, Response.CONDUCT);

	public static void main(String[] args) throws Exception {
		Dungeon.hero = new Hero();
		Dungeon.depth = 1;
		configuration();
		clickAction();
		explosions();
		hiddenEntrances();
		hiddenLevelGeneration();
		ratKingStatues();
		cityBossCarpet();
		propagation();
		electricField();
		performance();
		System.out.println("TerrainInteractionRegression passed: " + checks + " checks");
	}

	private static void configuration() throws Exception {
		TestLevel level = level(11);
		check(level.terrainInteractions == null, "普通层无规则组件");
		check(TerrainPropagation.event(level, Source.ELECTRIC) == null, "未启用时不创建传播事件");
		Level.set(60, Terrain.WALL, level);
		check(!level.affectTerrain(60, Source.EXPLOSION), "旧墙不可爆破");
		level.interactions().setDefault(Terrain.WALL, BREAK);
		level.interactions().setOverride(60, Rule.NONE);
		check(!level.affectTerrain(60, Source.EXPLOSION), "逐格拒绝覆盖默认");
		level.interactions().clearOverride(60);
		check(!level.affectTerrain(60, Source.CLICK), "类别间互不冒充");
		check(!level.affectTerrain(60, Source.FIRE), "非可燃易碎墙不接受火焰");
		check(level.affectTerrain(60, Source.DISINTEGRATION), "解离类别触发替换");
		check(level.passable[60] && !level.solid[60] && !level.losBlocking[60], "地形标记同步");
		check(level.discoverable[60], "新开墙面可发现");
		check(level.interactions().ruleAt(60).response(Source.EXPLOSION).action == TerrainInteractions.Action.LEGACY,
				"地形更换后规则回落到新类型");
		// 任意元素可以组合类别响应，框架没有木桶或铁笼类型判断。
		Rule composite = Rule.LEGACY.on(Source.CLICK, Response.replaceWith(Terrain.WATER))
				.on(Source.FIRE, Response.IGNORE).on(Source.ELECTRIC, Response.CONDUCT);
		Level.set(60, Terrain.STATUE, level);
		level.interactions().setOverride(60, composite);
		check(level.interactions().hasConductors(Source.ELECTRIC), "类别能力计数");
		check(!level.affectTerrain(60, Source.FIRE), "显式忽略");
		check(level.affectTerrain(60, Source.CLICK) && level.water[60], "任意元素点击响应");
		check(!level.interactions().hasConductors(Source.ELECTRIC), "清除覆盖同步能力计数");
		Level.set(60, Terrain.BARRICADE, level);
		level.interactions().setOverride(60, Rule.NONE);
		check(!level.affectTerrain(60, Source.FIRE) && level.map[60] == Terrain.BARRICADE, "拒绝不可回退旧燃烧");
		check(!level.affectTerrain(60, Source.FIRE, true), "强制旧清理也不能绕过类别拒绝");
		level.interactions().setOverride(60, Rule.LEGACY);
		check(level.affectTerrain(60, Source.FIRE), "显式旧行为可燃烧");
		Level.set(60, Terrain.EMPTY, level);
		check(level.affectTerrain(60, Source.FIRE, true) && level.map[60] == Terrain.EMBERS, "旧空地清理结果保留");

		level.interactions().setDefault(Terrain.REGION_DECO, composite);
		Level.set(60, Terrain.REGION_DECO, level);
		level.interactions().setOverride(61, BREAK);
		Bundle saved = new Bundle();
		level.storeInBundle(saved);
		saved.put("version", ShatteredPixelDungeon.v3_1_1);
		TestLevel restored = new TestLevel();
		restored.restoreFromBundle(saved);
		Dungeon.level = restored;
		check(restored.interactions().ruleAt(60).conducts(Source.ELECTRIC), "保存读取组合规则");
		check(restored.interactions().ruleAt(61).allows(Source.EXPLOSION), "保存读取逐格规则");
		check(!restored.interactions().ruleAt(61).allows(Source.CLICK), "保存读取拒绝类别");
		restored.setSize(9, 9);
		check(restored.terrainInteractions == null, "地图重新分配时清空规则");

		SewerLevel sewer = new SewerLevel();
		init(sewer, 11);
		Dungeon.level = sewer;
		Level.set(60, Terrain.REGION_DECO, sewer);
		Level.set(62, Terrain.REGION_DECO_ALT, sewer);
		Rule click = Rule.LEGACY.on(Source.CLICK, Response.DESTROY);
		sewer.interactions().setDefault(Terrain.REGION_DECO, click);
		sewer.interactions().setDefault(Terrain.REGION_DECO_ALT, click);
		Dungeon.hero.pos = 59;
		Dungeon.hero.fieldOfView = new boolean[sewer.length()];
		sewer.visited[60] = true;
		Dungeon.hero.handle(60);
		check(!(Dungeon.hero.curAction instanceof HeroAction.InteractTerrain), "未装备武器不分派挥击动作");
		Dungeon.hero.belongings.weapon = new WornShortsword();
		Dungeon.hero.handle(60);
		check(Dungeon.hero.curAction instanceof HeroAction.InteractTerrain, "装备武器后分派地形动作");
		check(sewer.affectTerrain(60, Source.CLICK) && sewer.map[60] == Terrain.WATER, "复用区域破坏响应一");
		check(sewer.affectTerrain(62, Source.CLICK) && sewer.map[62] == Terrain.EMPTY_SP, "复用区域破坏响应二");
		Dungeon.hero.curAction = null;
	}

	private static void explosions() {
		TestLevel level = level(11);
		int origin = 5 * 11 + 4, wall = origin + 1;
		for (int y = 1; y < 10; y++) Level.set(y * 11 + 5, Terrain.WALL, level);
		level.interactions().setOverride(wall, BREAK);
		level.interactions().setOverride(wall + 11, Rule.NONE);
		PathFinder.buildDistanceMap(origin, BArray.not(level.solid, null), 2);
		ArrayList<Integer> affected = new ArrayList<>();
		for (int i = 0; i < level.length(); i++) if (PathFinder.distance[i] != Integer.MAX_VALUE) affected.add(i);
		IntArray surfaces = level.interactions().blastSurfaces(affected, PathFinder.distance, 2);
		check(surfaces.size == 1 && surfaces.get(0) == wall, "墙面接触及去重，不包含拒绝的墙");
		check(!affected.contains(wall + 1), "爆炸前范围不穿墙");
		Level.set(wall + 1, Terrain.BARRICADE, level);
		new Bomb().explode(origin);
		check(level.map[wall] == Terrain.EMPTY_DECO, "实际炸弹接入类别响应");
		check(level.map[wall + 1] == Terrain.BARRICADE, "同次爆炸不透过刚打开的墙");
		check(level.map[wall + 11] == Terrain.WALL, "同类型未允许的墙保留");
		new Bomb().explode(wall);
		check(level.map[wall + 1] == Terrain.EMBERS, "下一次爆炸使用新地形");
		Level.set(wall, Terrain.WALL, level);
		level.interactions().setOverride(wall, BREAK);
		new Bomb() { @Override public boolean explodesDestructively() { return false; } }.explode(origin);
		check(level.map[wall] == Terrain.WALL, "非破坏性爆炸不获得拆墙能力");

		TestLevel carpetLevel = level(11);
		Carpet carpet = new Carpet();
		carpet.setRect(4, 4, 3, 3);
		carpet.overrideTile(1, 1, Carpet.CITY_STATUE);
		carpetLevel.customTiles.add(carpet);
		int statueCell = 5 * 11 + 5;
		int carpetFloor = statueCell - 1;
		Level.set(statueCell, Terrain.STATUE, carpetLevel);
		carpetLevel.interactions().setDefault(Terrain.STATUE, Rule.LEGACY.on(
				Source.EXPLOSION, Response.replaceWith(Terrain.EMBERS)));
		new Bomb().explode(carpetFloor);
		check(carpetLevel.map[statueCell] == Terrain.EMBERS
				&& !carpet.overrideTile(statueCell, carpetLevel, Carpet.DESTROYED), "炸毁雕像同时移除地毯雕像图块");
		check(carpetLevel.map[carpetFloor] == Terrain.EMBERS
				&& !carpet.overrideTile(carpetFloor, carpetLevel, Carpet.DESTROYED), "地毯地面受爆炸后留下余烬");

		// 装饰地面 EMPTY_DECO 由区域画笔在铺完房间后随机撒，和普通地面同等处理；
		// 不能变成余烬的功能格则保留原地毯图层，不能直接抹掉。
		TestCarpet decoCarpet = new TestCarpet();
		TestLevel decoLevel = level(11);
		decoCarpet.setRect(4, 4, 3, 3);
		decoLevel.customTiles.add(decoCarpet);
		int decoBomb = 5 * 11 + 4, decoCell = decoBomb - 11;
		Level.set(decoCell, Terrain.EMPTY_DECO, decoLevel);
		new Bomb().explode(decoBomb);
		check(decoLevel.map[decoCell] == Terrain.EMBERS && decoCarpet.destroyedAt(decoCell, decoLevel),
				"装饰地面上的地毯受爆炸后留下余烬");

		TestCarpet gateCarpet = new TestCarpet();
		TestLevel gateLevel = level(11);
		gateCarpet.setRect(4, 4, 3, 3);
		gateLevel.customTiles.add(gateCarpet);
		int gateBomb = 5 * 11 + 4, gateCell = gateBomb - 11;
		Level.set(gateCell, Terrain.ENTRANCE, gateLevel);
		new Bomb().explode(gateBomb);
		check(gateLevel.map[gateCell] == Terrain.ENTRANCE && !gateCarpet.touchedAt(gateCell, gateLevel),
				"爆炸不抹掉不能露出余烬的功能格地毯");

		CityLevel rayLevel = new CityLevel();
		init(rayLevel, 11);
		Dungeon.level = rayLevel;
		Carpet rayCarpet = new Carpet();
		rayCarpet.setRect(4, 4, 3, 3);
		rayLevel.customTiles.add(rayCarpet);
		int rayFloor = 5 * 11 + 5;
		check(rayLevel.affectTerrain(rayFloor, Source.DISINTEGRATION)
				&& rayLevel.map[rayFloor] == Terrain.EMBERS
				&& !rayCarpet.overrideTile(rayFloor, rayLevel, Carpet.DESTROYED), "解离射线破坏地毯并留下余烬");
		int rayStatue = rayFloor - 1;
		Level.set(rayStatue, Terrain.STATUE, rayLevel);
		rayCarpet.overrideTile(0, 1, Carpet.CITY_STATUE);
		check(rayLevel.affectTerrain(rayStatue, Source.DISINTEGRATION)
				&& rayLevel.map[rayStatue] == Terrain.STATUE_EMBERS && rayLevel.solid[rayStatue]
				&& !rayCarpet.overrideTile(rayStatue, rayLevel, Carpet.DESTROYED), "解离射线保留雕像并替换底部余烬");
		int rayStatueSp = rayFloor - 11;
		Level.set(rayStatueSp, Terrain.STATUE_SP, rayLevel);
		check(rayLevel.affectTerrain(rayStatueSp, Source.DISINTEGRATION)
				&& rayLevel.map[rayStatueSp] == Terrain.STATUE_SP_EMBERS
				&& rayLevel.solid[rayStatueSp], "解离射线保留特殊地面的雕像");
		int raySpecialFloor = rayFloor + 11;
		Level.set(raySpecialFloor, Terrain.EMPTY_SP, rayLevel);
		check(rayLevel.affectTerrain(raySpecialFloor, Source.DISINTEGRATION)
				&& rayLevel.map[raySpecialFloor] == Terrain.EMBERS_SP, "特殊地毯地面变为对应余烬");
		int rayEntrance = rayFloor + 1;
		Level.set(rayEntrance, Terrain.ENTRANCE, rayLevel);
		check(!rayLevel.affectTerrain(rayEntrance, Source.DISINTEGRATION)
				&& rayLevel.map[rayEntrance] == Terrain.ENTRANCE, "解离射线保留功能格的地毯图层");
		int rayDeco = 4 * 11 + 4;
		Level.set(rayDeco, Terrain.EMPTY_DECO, rayLevel);
		check(rayLevel.affectTerrain(rayDeco, Source.DISINTEGRATION)
				&& rayLevel.map[rayDeco] == Terrain.EMBERS
				&& !rayCarpet.overrideTile(rayDeco, rayLevel, Carpet.DESTROYED), "解离射线把装饰地面上的地毯也烧成余烬");

		SewerBossLevel ratLevel = new SewerBossLevel();
		init(ratLevel, 11);
		Dungeon.level = ratLevel;
		Carpet ratCarpet = new Carpet();
		ratCarpet.setRect(4, 4, 3, 3);
		ratLevel.customTiles.add(ratCarpet);
		int ratStatue = 5 * 11 + 5;
		Level.set(ratStatue, Terrain.CUSTOM_DECO, ratLevel);
		check(ratLevel.affectTerrain(ratStatue, Source.DISINTEGRATION)
				&& ratLevel.map[ratStatue] == Terrain.CUSTOM_DECO_EMBERS_SP
				&& ratLevel.solid[ratStatue]
				&& !ratCarpet.overrideTile(ratStatue, ratLevel, Carpet.DESTROYED), "鼠王房雕像底部显示特殊余烬");
		int ratFloor = ratStatue + 1;
		Level.set(ratFloor, Terrain.EMPTY_SP, ratLevel);
		new Bomb().explode(ratFloor);
		check(ratLevel.map[ratFloor] == Terrain.EMBERS_SP
				&& !ratCarpet.overrideTile(ratFloor, ratLevel, Carpet.DESTROYED), "鼠王房地毯受爆炸后露出特殊余烬");
	}

	/** 鼠王房挂在下水道 Boss 层，雕像用 CUSTOM_DECO 表示，爆炸应与解离结果一致。 */
	private static void ratKingStatues() {
		TestBossLevel boss = new TestBossLevel();
		Dungeon.level = boss;
		Dungeon.depth = 5;
		try {
			boss.create();
		} catch (Throwable headless) {
			// 无图形环境下物品贴图的静态初始化会挂在这里；
			// 规则表、地形与旗帜图在此之前已经建好，断言不受影响。
		}
		int statue = -1;
		for (int i = 0; i < boss.length(); i++) if (boss.map[i] == Terrain.CUSTOM_DECO) { statue = i; break; }
		check(statue >= 0, "鼠王房放置了雕像");
		check(boss.interactions().ruleAt(statue).allows(Source.EXPLOSION), "鼠王房雕像接受爆炸类别");
		int beside = statue;
		for (int offset : PathFinder.NEIGHBOURS8) {
			if (boss.insideMap(statue + offset) && !boss.solid[statue + offset]) { beside = statue + offset; break; }
		}
		check(beside != statue, "雕像旁边有可落炸药的格");
		// 爆炸结尾会跑 Dungeon.observe，英雄要站在合法格上、且视野数组长度正确。
		int far = statue;
		for (int i = 0; i < boss.length(); i++) {
			if (boss.insideMap(i) && boss.distance(statue, i) > boss.distance(statue, far)) far = i;
		}
		Dungeon.hero.pos = far;
		Dungeon.hero.fieldOfView = new boolean[boss.length()];
		new Bomb().explode(beside);
		check(boss.map[statue] == Terrain.EMBERS, "爆炸把雕像炸成普通余烬");
		check(boss.passable[statue] && !boss.solid[statue] && !boss.losBlocking[statue], "炸掉后雕像那格能走");
	}

	/** 通往王座的两条直毯改成 Carpet 图层后，应该能被爆炸摧毁并露出余烬。 */
	private static void cityBossCarpet() {
		TestCityBossLevel boss = new TestCityBossLevel();
		Dungeon.level = boss;
		Dungeon.depth = 20;
		try {
			boss.create();
		} catch (Throwable headless) {
			// 无图形环境下 ImpShopRoom 生成物品时会碰物品贴图的静态初始化，build() 会在那一步中断；
			// 地形、王座与地毯在此之前就建好了，下面补跑 build() 之后本会执行的旗帜与可发现性计算。
			boss.buildFlagMaps();
			boss.cleanWalls();
		}
		int carpetCount = 0;
		for (CustomTilemap tilemap : boss.customTiles){
			if (tilemap instanceof Carpet) carpetCount++;
		}
		check(carpetCount > 0, "通往王座的直毯是 Carpet 图层");
		// 挑一块被地毯盖住、旁边又能落炸药的地毯格；取扫描到的最后一格，离王座尽量远
		int target = -1, beside = -1;
		Carpet targetCarpet = null;
		for (int i = 0; i < boss.length(); i++){
			if (boss.map[i] != Terrain.CUSTOM_DECO_EMPTY) continue;
			Carpet covering = null;
			for (CustomTilemap tilemap : boss.customTiles){
				if (!(tilemap instanceof Carpet)) continue;
				int x = i % boss.width() - tilemap.tileX, y = i / boss.width() - tilemap.tileY;
				if (x >= 0 && y >= 0 && x < tilemap.tileW && y < tilemap.tileH) covering = (Carpet) tilemap;
			}
			if (covering == null) continue;
			for (int offset : PathFinder.NEIGHBOURS4){
				if (boss.insideMap(i + offset) && !boss.solid[i + offset]){
					target = i; beside = i + offset; targetCarpet = covering; break;
				}
			}
		}
		check(target >= 0, "存在可被炸到的直毯格");
		// 爆炸结尾会跑 Dungeon.observe，英雄要站在合法格上、且视野数组长度正确。
		int far = target;
		for (int i = 0; i < boss.length(); i++){
			if (boss.insideMap(i) && boss.distance(target, i) > boss.distance(target, far)) far = i;
		}
		Dungeon.hero.pos = far;
		Dungeon.hero.fieldOfView = new boolean[boss.length()];
		new Bomb().explode(beside);
		check(boss.map[target] == Terrain.EMBERS, "爆炸把直毯烧成余烬");
		check(!targetCarpet.overrideTile(target, boss, Carpet.DESTROYED), "地毯记录为已破坏");
	}

	private static void clickAction() throws Exception {
		TestHero hero = new TestHero();
		Dungeon.hero = hero;
		hero.belongings.weapon = new WornShortsword();
		TestLevel level = level(11);
		hero.pos = 59;
		hero.HP = hero.HT = 20;
		Callback[] pending = {null};
		hero.sprite = new CharSprite() {
			@Override public synchronized void attack(int cell, Callback callback) { pending[0] = callback; }
			@Override public boolean looping() { return false; }
		};
		java.lang.reflect.Method act = Hero.class.getDeclaredMethod("actInteractTerrain", HeroAction.InteractTerrain.class);
		act.setAccessible(true);
		Rule click = Rule.NONE.on(Source.CLICK, Response.replaceWith(Terrain.EMPTY));
		for (boolean revoke : new boolean[]{false, true}) {
			Level.set(60, Terrain.STATUE, level);
			level.interactions().setOverride(60, click);
			HeroAction.InteractTerrain action = new HeroAction.InteractTerrain(60);
			hero.curAction = action;
			int before = hero.turns;
			act.invoke(hero, action);
			check(level.map[60] == Terrain.STATUE && hero.turns == before, "动画完成前不破坏、不扣回合");
			if (revoke) level.interactions().setOverride(60, Rule.NONE);
			completeWithoutUI(pending[0]);
			check(hero.turns == before + (revoke ? 0 : 1), "成功扣一回合，失效不扣回合");
			check(level.map[60] == (revoke ? Terrain.STATUE : Terrain.EMPTY), "回调重新验证权限");
			pending[0].call();
			check(hero.turns == before + (revoke ? 0 : 1), "重复回调不重复结算");
		}
		Dungeon.hero = new Hero();
	}

	private static void completeWithoutUI(Callback callback) {
		try {
			callback.call();
		} catch (NullPointerException noUI) {
			// ready() 先清理动作和恢复状态，然后刷新 UI；无图形测试只运行到这一边界。
			StackTraceElement frame = noUI.getStackTrace()[0];
			if (!frame.getClassName().equals("com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator")
					|| !frame.getMethodName().equals("updateState")) throw noUI;
		}
	}

	private static void propagation() {
		java.util.Random random = new java.util.Random(20260927);
		TerrainPropagation traversal = new TerrainPropagation();
		for (int trial = 0; trial < 100; trial++) {
			int w = 9 + trial % 8;
			TestLevel level = level(w);
			boolean[] mask = new boolean[level.length()];
			for (int c = 0; c < mask.length; c++) if (level.insideMap(c) && random.nextFloat() < 0.4f) {
				mask[c] = true;
				level.interactions().setOverride(c, METAL);
			}
			int seed = w + 1;
			mask[seed] = true;
			level.interactions().setOverride(seed, METAL);
			PathFinder.buildDistanceMap(seed, mask, level.length());
			int[] reference = PathFinder.distance.clone();
			traversal.begin(level, Source.ELECTRIC);
			traversal.seed(seed, seed);
			HashSet<Integer> found = new HashSet<>();
			while (traversal.hasNext()) check(found.add(traversal.next()), "单次事件无重复展开");
			for (int c = 0; c < mask.length; c++) check(found.contains(c) == (reference[c] != Integer.MAX_VALUE), "八向传播与参考一致");
			check(Arrays.equals(reference, PathFinder.distance), "不覆盖共享寻路结果");
		}
		TestLevel level = level(15);
		for (int x = 3; x <= 10; x++) {
			Level.set(7 * 15 + x, Terrain.STATUE, level);
			level.interactions().setOverride(7 * 15 + x, METAL);
		}
		Ballistica bolt = new Ballistica(7 * 15 + 1, 7 * 15 + 3, Ballistica.MAGIC_BOLT);
		check(bolt.collisionPos == 7 * 15 + 2, "原弹道在实体前停止");
		check(TerrainPropagation.impactCell(level, bolt, Source.ELECTRIC) == 7 * 15 + 3, "类别接触到实体表面");
		Level.set(7 * 15 + 2, Terrain.DOOR, level);
		Ballistica blocked = new Ballistica(7 * 15 + 1, 7 * 15 + 3, Ballistica.MAGIC_BOLT);
		check(TerrainPropagation.impactCell(level, blocked, Source.ELECTRIC) == blocked.collisionPos, "不能越过前方的门接触导体");
		Level.set(7 * 15 + 2, Terrain.EMPTY, level);
		Mob near = new Mob() {}; near.pos = 7 * 15 + 11;
		Mob far = new Mob() {}; far.pos = 2 * 15 + 11;
		Actor.add(near); Actor.add(far);
		ArrayList<Char> targets = new ArrayList<>();
		TerrainPropagation event = TerrainPropagation.event(level, Source.ELECTRIC);
		event.touch(7 * 15 + 2);
		event.extendTargets(targets, null);
		check(targets.size() == 1 && targets.get(0) == near, "传播扩展到远端邻接生物，排除无关生物");
		int[] received = {0};
		TerrainPropagation.point(level, Source.ELECTRIC, 7 * 15 + 2, far, null, target -> received[0]++);
		check(received[0] == 2, "单点类别入口复用来源效果回调");
		Actor.remove(near); Actor.remove(far);
		TerrainPropagation first = TerrainPropagation.event(level, Source.ELECTRIC);
		TerrainPropagation nested = TerrainPropagation.event(level, Source.ELECTRIC);
		check(first != nested, "嵌套事件隔离缓存");
		first.end();
		TerrainPropagation reused = TerrainPropagation.event(level, Source.ELECTRIC);
		check(first == reused, "瞬时事件归还并复用缓存");
		reused.end(); nested.end();
		Level.set(31, Terrain.STATUE, level);
		level.interactions().setOverride(31, Rule.NONE.on(Source.ELECTRIC, Response.replaceWith(Terrain.WATER)));
		TerrainPropagation.point(level, Source.ELECTRIC, 31, null, null, target -> { throw new AssertionError(); });
		check(level.water[31], "电类别可响应地形替换，不限于导电");
		// 同一传播器接受其他作用类别，不绑定雷霆或铁笼。
		level.interactions().setOverride(31, Rule.NONE.on(Source.FIRE, Response.CONDUCT));
		check(TerrainPropagation.event(level, Source.FIRE) != null, "传播按类别配置，无来源类名单");
		traversal.begin(level, Source.ELECTRIC);
		traversal.seed(31, 31);
		check(!traversal.hasNext(), "热传导配置不等于导电");
	}

	private static void electricField() {
		TestLevel level = level(15);
		for (int x = 3; x <= 10; x++) {
			Level.set(7 * 15 + x, Terrain.STATUE, level);
			level.interactions().setOverride(7 * 15 + x, METAL);
		}
		Electricity field = new Electricity();
		field.seed(level, 7 * 15 + 2, 6);
		field.act();
		check(field.cur[7 * 15 + 11] == 5, "持续电场共享导体类别，保留时长递减");
		field.act();
		check(field.cur[7 * 15 + 11] == 4, "导体环路不叠加持续时间");
		for (int i = 0; i < 4; i++) field.act();
		check(field.volume == 0, "导电网络按原电场时间耗尽");
	}

	private static void performance() {
		for (int w : new int[]{32, 128, 256}) {
			TestLevel level = level(w);
			for (int y = 2; y < 10; y++) for (int x = 2; x < 10; x++) level.interactions().setOverride(y*w+x, METAL);
			TerrainPropagation graph = new TerrainPropagation();
			long start = 0;
			int total = 0;
			for (int i = 0; i < 20000; i++) {
				if (i == 10000) start = System.nanoTime();
				graph.begin(level, Source.ELECTRIC);
				graph.seed(2*w+2, 2*w+2);
				while (graph.hasNext()) { graph.next(); if (i >= 10000) total++; }
			}
			check(total == 640000, "实际遍历不随无关地图面积增加");
			System.out.printf(Locale.ROOT, "map=%d connected=64 visits/cast=64 warmMean=%.2f us%n", w*w, (System.nanoTime()-start)/10000.0/1000.0);
		}
	}

	private static void hiddenEntrances() {
		Dungeon.depth = 7;
		TestLevel floor = level(11);
		int cell = 60;
		floor.hiddenEntranceCell = cell;
		floor.interactions().setOverride(cell, Level.HIDDEN_ENTRANCE_RULE);
		ScrollOfMagicMapping.reveal(floor, false);
		check(floor.hiddenEntranceCell == cell && floor.map[cell] == Terrain.EMPTY
				&& floor.transitions.isEmpty(), "探地不会揭露隐藏楼梯");
		check(floor.affectTerrain(cell, Source.EXPLOSION), "爆炸揭露普通地板入口");
		check(floor.map[cell] == Terrain.EXIT && floor.hiddenEntranceCell == -1,
				"普通地板揭露后成为楼梯");
		check(floor.transitions.size() == 1 && floor.transitions.get(0).destDepth == 7
				&& floor.transitions.get(0).destBranch == 2, "楼梯指向同深度的隐藏分支");
		floor.revealHiddenEntrance(cell);
		check(floor.transitions.size() == 1, "重复揭露不会重复创建楼梯");

		TestLevel grass = level(11);
		Level.set(cell, Terrain.GRASS, grass);
		grass.hiddenEntranceCell = cell;
		grass.hiddenEntranceGrass = true;
		Level.set(cell, Terrain.HIGH_GRASS, grass);
		Level.set(cell, Terrain.GRASS, grass);
		check(grass.hiddenEntranceCell == cell && grass.transitions.isEmpty(), "高草踩踏不揭露入口");
		Level.set(cell, Terrain.EMPTY, grass);
		check(grass.map[cell] == Terrain.EXIT && grass.transitions.size() == 1,
				"草地变成非草地时揭露入口");
	}

	private static void hiddenLevelGeneration() {
		Dungeon.seed = 12345L;
		Dungeon.branch = 2;
		for (int depth : new int[]{1, 7, 11, 16, 21}) {
			Dungeon.depth = depth;
			Dungeon.level = null;
			HiddenLevel hidden = new TestHiddenLevel();
			hidden.create();
			check(hidden.getTransition(LevelTransition.Type.BRANCH_ENTRANCE)
					.destDepth == depth, "隐藏地图返回原深度");
			check(hidden.transitions.size() == 1 && hidden.transitions.get(0).destBranch == 0,
					"隐藏地图只保留回主线的楼梯");
			check(hidden.viewDistance == 1, "隐藏地图视距为一格");
			int pedestals = 0;
			int regionTerrain = 0;
			for (int cell = 0; cell < hidden.length(); cell++) {
				if (hidden.map[cell] == Terrain.PEDESTAL) pedestals++;
				if (hidden.map[cell] == Terrain.WATER || hidden.map[cell] == Terrain.GRASS
						|| hidden.map[cell] == Terrain.HIGH_GRASS || hidden.map[cell] == Terrain.EMPTY_DECO
						|| hidden.map[cell] == Terrain.WALL_DECO) regionTerrain++;
			}
			check(pedestals == 3 && hidden.heaps.valueList().isEmpty(), "单房间地图有三个基座");
			check(regionTerrain > 0, "单房间仍使用区域地形绘制");
			Dungeon.level = hidden;
			for (int cell = 0; cell < hidden.length(); cell++) {
				if (hidden.map[cell] == Terrain.HIGH_GRASS || hidden.map[cell] == Terrain.FURROWED_GRASS) {
					Level.set(cell, Terrain.GRASS, hidden);
				}
			}
			Dungeon.hero.pos = hidden.getTransition(LevelTransition.Type.BRANCH_ENTRANCE).cell();
			hidden.updateFieldOfView(Dungeon.hero, hidden.heroFOV);
			for (int cell = 0; cell < hidden.length(); cell++) {
				if (!hidden.heroFOV[cell] || hidden.distance(Dungeon.hero.pos, cell) <= 1) continue;
				boolean nearLight = false;
				for (int source = 0; source < hidden.length(); source++) {
					if ((hidden.map[source] == Terrain.PEDESTAL
							|| (depth == 7 && hidden.map[source] == Terrain.WALL_DECO))
							&& hidden.distance(source, cell) <= 1) nearLight = true;
				}
				check(nearLight, "环境光之外的视野限于一格");
			}
			for (int pedestal = 0; pedestal < hidden.length(); pedestal++) {
				if (hidden.map[pedestal] != Terrain.PEDESTAL) continue;
				for (int y = -1; y <= 1; y++) {
					for (int x = -1; x <= 1; x++) {
						check(hidden.heroFOV[pedestal + x + y * hidden.width()],
								"基座周围九格强制照亮");
					}
				}
			}
			for (int offset : PathFinder.NEIGHBOURS8) {
				Level.set(Dungeon.hero.pos + offset, Terrain.WALL, hidden);
			}
			Arrays.fill(hidden.heroFOV, false);
			hidden.updateFieldOfView(Dungeon.hero, hidden.heroFOV);
			for (int pedestal = 0; pedestal < hidden.length(); pedestal++) {
				if (hidden.map[pedestal] == Terrain.PEDESTAL) {
					check(!hidden.heroFOV[pedestal], "墙壁遮挡后远处基座不会无条件显示");
				}
			}
			if (depth == 7) {
				for (int offset : PathFinder.NEIGHBOURS8) {
					Level.set(Dungeon.hero.pos + offset, Terrain.EMPTY, hidden);
				}
				for (int cell = 0; cell < hidden.length(); cell++) {
					if (hidden.map[cell] == Terrain.PEDESTAL) Level.set(cell, Terrain.EMPTY, hidden);
				}
				int wallLight = Dungeon.hero.pos - 2 * hidden.width();
				int target = wallLight + 1;
				Level.set(wallLight, Terrain.WALL_DECO, hidden);
				Level.set(target, Terrain.EMPTY, hidden);
				Arrays.fill(hidden.heroFOV, false);
				hidden.updateFieldOfView(Dungeon.hero, hidden.heroFOV);
				check(hidden.heroFOV[target], "监狱隐藏层灯火墙壁照亮视野外邻格");
			}
			Statistics.floorsExplored.clear();
			Dungeon.updateLevelExplored();
			check(Statistics.floorsExplored.valueList().isEmpty(), "隐藏楼层不计入探索分");
		}
		Dungeon.branch = 0;
	}

	private static TestLevel level(int width) {
		Actor.clear();
		TestLevel level = new TestLevel(); init(level, width);
		Dungeon.level = level; Dungeon.hero.pos = width + 1;
		return level;
	}
	private static void init(Level level, int width) {
		level.setSize(width, width);
		level.blobs = new HashMap<>(); level.traps = new SparseArray<>();
		level.heaps = new SparseArray<>(); level.plants = new SparseArray<>();
		level.mobs = new HashSet<>(); level.transitions = new ArrayList<>();
		level.customTiles = new ArrayList<>(); level.customTerrain = new ArrayList<>(); level.customWalls = new ArrayList<>();
		for (int y = 1; y < width-1; y++) for (int x = 1; x < width-1; x++) level.map[y*width+x] = Terrain.EMPTY;
		level.buildFlagMaps(); level.cleanWalls();
	}
	private static void check(boolean condition, String message) {
		checks++;
		if (!condition) throw new AssertionError(message);
	}
	/** 鼠王房按真实流程生成，需要绕开无图形环境的两处：落地精灵与视野计算。 */
	private static class TestBossLevel extends SewerBossLevel {
		@Override public Heap drop(Item item, int cell) { return null; }
		@Override public void updateFieldOfView(Char ch, boolean[] fieldOfView) { Arrays.fill(fieldOfView, false); }
	}
	/** 王座厅同样按真实流程生成。 */
	private static class TestCityBossLevel extends CityBossLevel {
		@Override public Heap drop(Item item, int cell) { return null; }
		@Override public void updateFieldOfView(Char ch, boolean[] fieldOfView) { Arrays.fill(fieldOfView, false); }
	}
	public static class TestLevel extends Level {
		@Override protected boolean build() { return true; }
		@Override protected void createMobs() { }
		@Override protected void createItems() { }
		@Override public void updateFieldOfView(Char ch, boolean[] fieldOfView) { Arrays.fill(fieldOfView, false); }
	}
	private static class TestHiddenLevel extends HiddenLevel {
		@Override protected void createItems() { }
	}
	/** 读原始覆盖值：`overrideTile(cell, level, DESTROYED)` 对 SKIP 和「从未覆盖」都返回 false，区分不开。 */
	private static class TestCarpet extends Carpet {
		private Integer rawAt(int cell, Level level) {
			return tileOverrides.get(cell % level.width() - tileX + tileW * (cell / level.width() - tileY));
		}
		boolean destroyedAt(int cell, Level level) {
			Integer raw = rawAt(cell, level);
			return raw != null && raw == Carpet.DESTROYED;
		}
		boolean touchedAt(int cell, Level level) { return rawAt(cell, level) != null; }
	}
	private static class TestHero extends Hero {
		int turns;
		@Override public void spendAndNext(float time) { turns++; }
	}
}
