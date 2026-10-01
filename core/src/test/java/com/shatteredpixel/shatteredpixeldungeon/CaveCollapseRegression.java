package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Spear;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ForceCube;
import com.shatteredpixel.shatteredpixeldungeon.levels.CaveCollapse;
import com.shatteredpixel.shatteredpixeldungeon.levels.CavesLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.TerrainInteractions;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.EmptyRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.WornDartTrap;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/** 不依赖图形窗口，检查生成几何、实际武器入口、死亡与掩埋、持久化。 */
public class CaveCollapseRegression {
	private static int checks;
	private static final int[] REGION = {60, 61, 72, 73, 84};

	public static void main(String[] args) throws Exception {
		new com.watabou.noosa.Game(GameScene.class, null);
		com.badlogic.gdx.Gdx.app = (com.badlogic.gdx.Application) java.lang.reflect.Proxy.newProxyInstance(
				com.badlogic.gdx.Application.class.getClassLoader(), new Class<?>[]{com.badlogic.gdx.Application.class},
				(proxy, method, values) -> {
					if (method.getName().equals("getType")) return com.badlogic.gdx.Application.ApplicationType.Desktop;
					if (method.getReturnType() == int.class) return 0;
					return null;
				});
		com.badlogic.gdx.Gdx.files = (com.badlogic.gdx.Files) java.lang.reflect.Proxy.newProxyInstance(
				com.badlogic.gdx.Files.class.getClassLoader(), new Class<?>[]{com.badlogic.gdx.Files.class},
				(proxy, method, values) -> new com.badlogic.gdx.files.FileHandle(
						new java.io.File("src/main/assets", (String) values[0])));
		com.badlogic.gdx.utils.GdxNativesLoader.load();
		com.watabou.gltextures.TextureCache.create(Assets.Sprites.ITEMS, 256, 1024);
		com.watabou.gltextures.TextureCache.create(Assets.Sprites.ITEM_ICONS, 128, 128);
		com.watabou.gltextures.TextureCache.create(Assets.Effects.EFFECTS, 64, 64);
		com.watabou.utils.GameSettings.set((com.badlogic.gdx.Preferences) java.lang.reflect.Proxy.newProxyInstance(
				com.badlogic.gdx.Preferences.class.getClassLoader(), new Class<?>[]{com.badlogic.gdx.Preferences.class},
				(proxy, method, values) -> {
					if (method.getName().startsWith("get") && values != null && values.length == 2) return values[1];
					if (method.getReturnType() == boolean.class) return false;
					return null;
				}));
		generation();
		realCaves();
		collapseAndSave();
		weapons();
		burialAndRevival();
		System.out.println("CaveCollapseRegression passed: " + checks + " checks");
	}

	private static void generation() {
		int doubles = 0;
		int waterCells = 0, trapCells = 0;
		int[] filledWater = new int[2];
		int[] groundTypes = {Terrain.EMPTY, Terrain.EMPTY_DECO, Terrain.EMPTY_SP, Terrain.CUSTOM_DECO_EMPTY,
				Terrain.GRASS, Terrain.HIGH_GRASS, Terrain.FURROWED_GRASS, Terrain.WATER,
				Terrain.SECRET_TRAP, Terrain.TRAP, Terrain.INACTIVE_TRAP, Terrain.EMBERS, Terrain.EMBERS_SP};
		for (int trial = 0; trial < 100; trial++) {
			Random.pushGenerator(20260930L + trial);
			try {
				TestLevel level = level(31);
				ArrayList<Room> rooms = new ArrayList<>();
				for (int[] rect : new int[][]{{2, 2, 14, 14}, {16, 2, 28, 14}, {2, 16, 14, 28}}) {
					EmptyRoom room = new EmptyRoom();
					room.set(rect[0], rect[1], rect[2], rect[3]);
					rooms.add(room);
					for (int y = room.top + 1; y < room.bottom; y++) {
						for (int x = room.left + 1; x < room.right; x++) {
							int cell = x + y * level.width();
							int tile = trial < 80 ? (Random.Int(5) == 0 ? Terrain.CHASM : groundTypes[Random.Int(groundTypes.length)])
									: groundTypes[trial % groundTypes.length];
							level.map[cell] = tile;
							if (tile == Terrain.SECRET_TRAP || tile == Terrain.TRAP || tile == Terrain.INACTIVE_TRAP) {
								Trap trap = new WornDartTrap().set(cell);
								trap.visible = tile != Terrain.SECRET_TRAP;
								trap.active = tile != Terrain.INACTIVE_TRAP;
								level.traps.put(cell, trap);
							}
						}
					}
				}
				int[] original = level.map.clone();
				level.caveCollapse = new CaveCollapse(level);
				check(level.caveCollapse.generate(rooms), "合格房间足够时完成生成");
				Bundle regions = new Bundle(); level.caveCollapse.storeInBundle(regions);
				int count = regions.getInt("count");
				check(count == 1 || count == 2, "每层一或两处");
				if (count == 2) doubles++;
				HashSet<Integer> all = new HashSet<>();
				for (int index = 0; index < count; index++) {
					int[] cells = regions.getIntArray("region_" + index);
					check(cells.length >= 7 && cells.length <= 60, "房间内部面积121，危险区占5%到50%");
					HashSet<Integer> remaining = new HashSet<>();
					int walls = 0, left = 31, right = 0, top = 31, bottom = 0;
					for (int cell : cells) {
						check(original[cell] != Terrain.CHASM && !com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet
								.wallStitcheable(original[cell]), "悬崖和原有墙不进入区域");
						if (original[cell] == Terrain.WATER) waterCells++;
						if (original[cell] == Terrain.TRAP || original[cell] == Terrain.SECRET_TRAP
								|| original[cell] == Terrain.INACTIVE_TRAP) trapCells++;
						check(all.add(cell), "区域互不重叠");
						remaining.add(cell);
						left = Math.min(left, cell % 31); right = Math.max(right, cell % 31);
						top = Math.min(top, cell / 31); bottom = Math.max(bottom, cell / 31);
						if (level.map[cell] == Terrain.COLLAPSE_WALL) {
							int buried = regions.getIntArray("buried_terrain")[cell];
							int expected = original[cell] == Terrain.HIGH_GRASS || original[cell] == Terrain.FURROWED_GRASS
									? Terrain.GRASS : original[cell];
							check(expected == Terrain.WATER ? buried == Terrain.EMPTY || buried == Terrain.EMPTY_DECO
									: buried == expected, "初始支撑墙记住原地形，高草压扁、水面填成随机空地");
							if (original[cell] == Terrain.WATER) filledWater[buried == Terrain.EMPTY ? 0 : 1]++;
							check(level.traps.get(cell) == null, "初始支撑墙下陷阱从活动地图移除");
							walls++;
							for (int offset : PathFinder.NEIGHBOURS8) {
								check(!com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet
										.wallStitcheable(level.map[cell + offset]), "初始支撑墙八邻格不贴墙或其他支撑墙");
							}
						}
					}
					check(walls >= 1 && walls <= 2, "初始支撑墙数量1到2");
					check((right - left + 1) * (bottom - top + 1) > cells.length, "形状不是方正矩形或直线");
					ArrayList<Integer> queue = new ArrayList<>(); queue.add(cells[0]); remaining.remove(cells[0]);
					for (int head = 0; head < queue.size(); head++) {
						for (int offset : PathFinder.NEIGHBOURS4) {
							int next = queue.get(head) + offset;
							if (remaining.remove(next)) queue.add(next);
						}
					}
					check(remaining.isEmpty(), "整个区域四向连通");
				}
			} finally { Random.popGenerator(); }
		}
		check(doubles >= 30 && doubles <= 70, "第二处的生成概率约50%");
		check(waterCells > 0 && trapCells > 0, "混合地形与全水面、全陷阱房间均能生成危险区");
		check(filledWater[0] > 0 && filledWater[1] > 0, "水面填平的随机空地包括普通和装饰两种地形");
	}

	private static void collapseAndSave() {
		TestLevel level = fixture();
		TestMob enemy = new TestMob(); enemy.pos = 61;
		TestMob ally = new TestMob(); ally.pos = 72; ally.alignment = Char.Alignment.ALLY; ally.flying = true;
		TestMob outside = new TestMob(); outside.pos = 62;
		for (TestMob mob : new TestMob[]{enemy, ally, outside}) { level.mobs.add(mob); Actor.add(mob); }
		Buff.affect(enemy, Barrier.class).setShield(10000);
		Heap original = level.drop(new Item(), 73);
		check(level.affectTerrain(60, TerrainInteractions.Source.CLICK), "拆掉第一格支撑墙");
		check(enemy.isAlive() && level.map[61] == Terrain.EMPTY, "还有墙时不坍塌");
		check(level.affectTerrain(84, TerrainInteractions.Source.MISSILE), "最后墙补回后仍报告成功");
		check(!enemy.isAlive() && !ally.isAlive() && outside.isAlive(), "秒杀区域敌友与飞行单位，护盾不阻挡，区域外不受影响");
		check(enemy.deathCause == level.caveCollapse && ally.deathCause == level.caveCollapse, "使用单位死亡流程");
		check(level.heaps.get(61).size() == 1 && level.heaps.get(73) == original, "死亡掉落与原有物品保留原格");
		for (int cell : REGION) check(level.map[cell] == Terrain.COLLAPSE_WALL && level.solid[cell] && !level.passable[cell], "坍塌填满墙并同步标记");
		check(level.map[96] == Terrain.COLLAPSE_WALL, "另一片区域的墙保持原样");
		check(level.affectTerrain(73, TerrainInteractions.Source.CLICK) && level.heaps.get(73) == original, "开墙不会删除埋藏物品");
		Bundle saved = new Bundle(); level.storeInBundle(saved); saved.put("version", ShatteredPixelDungeon.v3_1_1);
		TestLevel restored = new TestLevel(); restored.restoreFromBundle(saved); Dungeon.level = restored;
		check(restored.caveCollapse.contains(73) && restored.map[73] == Terrain.EMPTY, "保存读取固定区域与已清理地形");
		check(restored.heaps.get(73).size() == 1 && restored.heaps.get(61).size() == 1, "掩埋物品正常保存读取");
		Actor.clear(); restored.mobs.clear();
		for (int cell : REGION) if (restored.map[cell] == Terrain.COLLAPSE_WALL) restored.affectTerrain(cell, TerrainInteractions.Source.CLICK);
		for (int cell : REGION) check(restored.map[cell] == Terrain.COLLAPSE_WALL, "读档后清空所有墙再次坍塌");

		// 用真实陷阱对象验证覆盖、读档、挖开与再次塌方，避免只恢复一个空陷阱图块。
		level = fixture();
		Level.set(61, Terrain.WATER, level);
		Level.set(72, Terrain.EMPTY_DECO, level);
		Level.set(73, Terrain.SECRET_TRAP, level);
		Trap originalTrap = level.setTrap(new WornDartTrap(), 73);
		originalTrap.outdated = true;
		originalTrap.primed = true;
		level.affectTerrain(60, TerrainInteractions.Source.CLICK);
		Level.set(60, Terrain.EMPTY_SP, level);
		level.affectTerrain(84, TerrainInteractions.Source.WAND);
		check(level.traps.get(73) == null, "塌方掩埋陷阱，不显示或参与普通陷阱查询");
		check(!level.water[61] && level.solid[61], "被落石覆盖的水面同步变为实体墙");
		saved = new Bundle(); level.storeInBundle(saved); saved.put("version", ShatteredPixelDungeon.v3_1_1);
		int filled = saved.getBundle("cave_collapse").getIntArray("buried_terrain")[61];
		restored = new TestLevel(); restored.restoreFromBundle(saved); Dungeon.level = restored;
		check(restored.traps.get(73) == null, "读档后陷阱仍被掩埋");
		for (int cell : new int[]{60, 61, 72, 73}) check(restored.affectTerrain(cell, TerrainInteractions.Source.MISSILE), "挖开恢复原地形仍报告成功");
		check(restored.map[60] == Terrain.EMPTY_SP && restored.map[72] == Terrain.EMPTY_DECO, "保留特殊地面与装饰地面的区别");
		check(restored.map[61] == Terrain.EMPTY || restored.map[61] == Terrain.EMPTY_DECO, "读档后挖开被填平的水面成为随机空地");
		check(restored.map[61] == filled, "读档与挖开不重新随机填水后的地形");
		check(!restored.water[61] && restored.passable[61] && !restored.solid[61], "填平的水面恢复为空地通行标记");
		Trap recovered = restored.traps.get(73);
		check(restored.map[73] == Terrain.SECRET_TRAP && recovered instanceof WornDartTrap
				&& !recovered.visible && recovered.active && recovered.outdated && recovered.primed, "恢复真实隐藏陷阱及待触发状态");
		check(com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoCell.cellName(72).equals(restored.tileName(Terrain.EMPTY_DECO)), "危险区内保留装饰地面名称");
		recovered.disarm();
		Level.set(61, Terrain.EMBERS, restored);
		restored.affectTerrain(84, TerrainInteractions.Source.SHOCKWAVE);
		check(restored.traps.get(73) == null, "再次塌方重新掩埋已经解除的陷阱");
		restored.affectTerrain(61, TerrainInteractions.Source.CLICK);
		restored.affectTerrain(73, TerrainInteractions.Source.CLICK);
		check(restored.map[61] == Terrain.EMBERS, "再次塌方记住最新地形，不回退到初始水面");
		check(restored.map[73] == Terrain.INACTIVE_TRAP && restored.traps.get(73) == recovered && !recovered.active,
				"再次挖开保持陷阱解除状态，不复活陷阱");

		for (int tile : new int[]{Terrain.HIGH_GRASS, Terrain.FURROWED_GRASS, Terrain.GRASS,
				Terrain.EMPTY_DECO, Terrain.EMPTY_SP, Terrain.CUSTOM_DECO_EMPTY, Terrain.EMBERS, Terrain.EMBERS_SP}) {
			level = fixture();
			Level.set(61, tile, level);
			check(com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoCell.cellName(61).equals(level.tileName(tile)),
					"塌方前查看保留各自地形名称");
			level.affectTerrain(60, TerrainInteractions.Source.CLICK);
			level.affectTerrain(84, TerrainInteractions.Source.CLICK);
			saved = new Bundle(); level.storeInBundle(saved); saved.put("version", ShatteredPixelDungeon.v3_1_1);
			restored = new TestLevel(); restored.restoreFromBundle(saved); Dungeon.level = restored;
			restored.affectTerrain(61, TerrainInteractions.Source.CLICK);
			int expected = tile == Terrain.HIGH_GRASS || tile == Terrain.FURROWED_GRASS ? Terrain.GRASS : tile;
			check(restored.map[61] == expected && restored.passable[61] && !restored.losBlocking[61],
					"高草压成普通草地，其他地形恢复各自类型与通行视线标记");
		}
	}

	private static void realCaves() {
		for (int trial = 0; trial < 32; trial++) {
			Actor.clear(); Dungeon.hero = new TestHero(); Dungeon.seed = 918273L + trial;
			Dungeon.depth = 11 + trial % 4; Dungeon.branch = 0; Dungeon.level = null;
			Random.pushGenerator(Dungeon.seed);
			try {
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith.Quest.reset();
				com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom.initForRun();
				com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.SecretRoom.initForRun();
				TestCaves caves = new TestCaves(); caves.create();
				check(caves.caveCollapse != null, "真实主线洞穴生成塌方组件");
				Bundle data = new Bundle(); caves.caveCollapse.storeInBundle(data);
				check(data.getInt("count") == 1 || data.getInt("count") == 2, "真实生成流程恰好一或两处");
				for (int i = 0; i < data.getInt("count"); i++) {
					int supportCount = 0;
					for (int cell : data.getIntArray("region_" + i)) {
						check(!caves.pit[cell], "真实楼层危险区不覆盖悬崖");
						if (caves.map[cell] == Terrain.COLLAPSE_WALL) {
							supportCount++;
							for (int offset : PathFinder.NEIGHBOURS8) {
								check(!com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet
										.wallStitcheable(caves.map[cell + offset]), "真实楼层初始支撑墙不贴墙");
							}
						}
					}
					check(supportCount >= 1 && supportCount <= 2, "真实楼层初始支撑墙1到2格");
				}
			} finally { Random.popGenerator(); }
		}
		for (int branch : new int[]{1, 2}) {
			Dungeon.depth = 11; Dungeon.branch = branch; Dungeon.level = null;
			com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom.initForRun();
			com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.SecretRoom.initForRun();
			TestCaves caves = new TestCaves(); caves.create();
			check(caves.caveCollapse == null, "支线不生成塌方区域");
		}
		Dungeon.branch = 0;
	}

	private static void weapons() throws Exception {
		TestLevel level = fixture(); Dungeon.hero.pos = 57;
		for (Wand wand : new Wand[]{new WandOfMagicMissile(), new WandOfFrost(), new WandOfLightning(),
				new WandOfDisintegration(), new WandOfWarding(), new WandOfRegrowth()}) {
			level.map[60] = Terrain.COLLAPSE_WALL; level.buildFlagMaps();
			check(wand.zapTerrain(Dungeon.hero, 60) && level.map[60] == Terrain.EMPTY, "法杖直接命中岩壁");
			check(level.map[84] == Terrain.COLLAPSE_WALL && level.map[96] == Terrain.COLLAPSE_WALL, "连锁、贯穿与范围不能额外拆墙");
			Level.set(60, Terrain.COLLAPSE_WALL, level);
		}
		for (Wand wand : new Wand[]{new WandOfFireblast(), new WandOfCorrosion()}) {
			check(!wand.zapTerrain(Dungeon.hero, 60), "焰浪和酸蚀不破墙");
		}
		for (TerrainInteractions.Source source : new TerrainInteractions.Source[]{TerrainInteractions.Source.FIRE,
				TerrainInteractions.Source.ELECTRIC, TerrainInteractions.Source.DISINTEGRATION,
				TerrainInteractions.Source.EXPLOSION}) {
			check(!level.affectTerrain(60, source, true), "环境、连锁与范围入口无法拆墙");
		}
		level.map[58] = Terrain.WALL; level.buildFlagMaps();
		check(!new WandOfMagicMissile().zapTerrain(Dungeon.hero, 60), "被前方实体墙阻挡不能隔墙拆目标");
		check(new WandOfDisintegration().zapTerrain(Dungeon.hero, 60) && level.map[84] == Terrain.COLLAPSE_WALL,
				"解离保留贯穿能力，只破坏弹道第一格岩壁");
		Level.set(60, Terrain.COLLAPSE_WALL, level);
		Dungeon.hero.pos = 18;
		check(!new WandOfDisintegration().zapTerrain(Dungeon.hero, 96), "解离不能破坏自身射程外的墙");
		Dungeon.hero.pos = 57;
		TestStone stone = new TestStone();
		check(stone.throwPos(Dungeon.hero, 60) != 60, "投掷弹道尊重前方阻挡");
		level.map[58] = Terrain.EMPTY; level.buildFlagMaps();
		for (Wand wand : new Wand[]{new WandOfMagicMissile(), new WandOfFrost(), new WandOfLightning(),
				new WandOfDisintegration(), new WandOfWarding(), new WandOfRegrowth()}) {
			Level.set(61, Terrain.COLLAPSE_WALL, level);
			check(wand.targetingPos(Dungeon.hero, 63) == 60, "瞄准墙后地面时预览指向途中第一格岩壁");
			check(wand.zapTerrain(Dungeon.hero, 63) && level.map[60] == Terrain.EMPTY
					&& level.map[61] == Terrain.COLLAPSE_WALL, "法杖破坏途中命中的第一格，不拆后续墙");
			Level.set(60, Terrain.COLLAPSE_WALL, level);
		}
		check(stone.throwPos(Dungeon.hero, 63) == 60, "投掷武器瞄准墙后地面时命中途中岩壁");
		check(stone.throwPos(Dungeon.hero, 59) == 59, "投掷物停在墙前选定格时不会越过终点破墙");
		check(new WandOfBlastWave().targetingPos(Dungeon.hero, 59) == 59, "停在目标格的法杖不会越过终点命中墙");
		for (Wand wand : new Wand[]{new WandOfFireblast(), new WandOfCorrosion()}) {
			check(!wand.zapTerrain(Dungeon.hero, 63), "焰浪和酸蚀经过岩壁也不破坏");
		}
		check(new WandOfMagicMissile().zapTerrain(Dungeon.hero, 61) && level.map[60] == Terrain.EMPTY
				&& level.map[61] == Terrain.COLLAPSE_WALL, "瞄准后方岩壁也只破坏前方实际命中的一格");
		Level.set(60, Terrain.COLLAPSE_WALL, level);
		TestMob blocker = new TestMob(); blocker.pos = 58; Actor.add(blocker); level.mobs.add(blocker);
		check(!new WandOfMagicMissile().zapTerrain(Dungeon.hero, 63) && level.map[60] == Terrain.COLLAPSE_WALL,
				"前方单位拦截法杖时不破坏单位后方岩壁");
		check(stone.throwPos(Dungeon.hero, 63) == 58, "前方单位拦截投掷武器");
		Actor.remove(blocker); level.mobs.remove(blocker);
		Dungeon.hero.pos = 59;
		check(new WandOfMagicMissile().targetingPos(Dungeon.hero, 60) == 60, "邻格墙面命中修正，不能误判为向自己施法");
		Dungeon.hero.pos = 57;
		check(stone.throwPos(Dungeon.hero, 60) == 60, "投掷修正到实际墙面");
		stone.land(60);
		check(level.map[60] == Terrain.EMPTY && level.heaps.get(60).items.contains(stone), "实际投掷命中破墙并留下投掷武器");
		Level.set(60, Terrain.COLLAPSE_WALL, level);
		Dungeon.hero.pos = 58; Dungeon.hero.belongings.weapon = new Spear();
		Method reach = Hero.class.getDeclaredMethod("canInteractTerrain", int.class); reach.setAccessible(true);
		check((boolean)reach.invoke(Dungeon.hero, 60), "长柄近战在两格外可以直接攻击墙");
		Callback[] pending = {null};
		Dungeon.hero.sprite = new CharSprite() {
			@Override public synchronized void attack(int cell, Callback callback) { pending[0] = callback; }
			@Override public boolean looping() { return false; }
		};
		Dungeon.hero.curAction = new HeroAction.InteractTerrain(60);
		Method attack = Hero.class.getDeclaredMethod("actInteractTerrain", HeroAction.InteractTerrain.class); attack.setAccessible(true);
		attack.invoke(Dungeon.hero, Dungeon.hero.curAction);
		check(pending[0] != null && Dungeon.hero.pos == 58, "长柄地形攻击直接播放动画，不走进危险区");
		try { pending[0].call(); } catch (NullPointerException noUI) {
			if (!noUI.getStackTrace()[0].getClassName().equals("com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator")) throw noUI;
		}
		check(level.map[60] == Terrain.EMPTY && ((TestHero)Dungeon.hero).turns == 1, "近战动画完成后破墙并扣一回合");
		Level.set(60, Terrain.COLLAPSE_WALL, level);
		level.map[48] = level.map[59] = level.map[70] = Terrain.WALL; level.buildFlagMaps();
		check(!(boolean)reach.invoke(Dungeon.hero, 60), "近战距离检查不能穿过另一堵墙");

		java.lang.reflect.Field current = Actor.class.getDeclaredField("current"); current.setAccessible(true);
		for (boolean replace : new boolean[]{false, true}) {
			level = fixture();
			TestHero hero = (TestHero)Dungeon.hero;
			hero.pos = 59; hero.belongings.weapon = new Spear();
			Arrays.fill(hero.fieldOfView = new boolean[level.length()], true);
			Level.set(61, Terrain.COLLAPSE_WALL, level);
			pending[0] = null;
			hero.sprite = new CharSprite() {
				@Override public synchronized void attack(int cell, Callback callback) { pending[0] = callback; }
				@Override public boolean looping() { return false; }
			};
			HeroAction.InteractTerrain first = new HeroAction.InteractTerrain(60);
			hero.curAction = first; hero.ready = true; current.set(null, hero);
			attack.invoke(hero, first);
			check(!hero.ready && Actor.processing(), "破墙动画中保持忙碌，等待回调");
			Callback firstCallback = pending[0];
			if (replace) hero.handle(61);
			else hero.curAction = null; // 忙碌时再次点击会走 GameScene.cancel 清空动作。
			HeroAction queued = hero.curAction;
			firstCallback.call();
			check(level.map[60] == Terrain.EMPTY && hero.turns == 1, "取消或更换动作不丢失已提交的破墙结算");
			check(!Actor.processing() && !hero.ready, "回调释放调度，但等待英雄下回合再开放输入");
			check(hero.curAction == queued, "清理旧动作不覆盖后续动作");
			firstCallback.call();
			check(hero.turns == 1 && hero.curAction == queued, "重复旧回调不会重复扣时或清空新动作");
			if (replace) {
				current.set(null, hero);
				attack.invoke(hero, queued);
				pending[0].call();
				check(level.map[61] == Terrain.EMPTY && hero.turns == 2 && hero.curAction == null
						&& !Actor.processing(), "后续破墙只执行一次，正常退出队列");
			}
			try { hero.act(); } catch (NullPointerException noUI) {
				if (!noUI.getStackTrace()[0].getClassName().equals("com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator")) throw noUI;
			}
			check(hero.ready && hero.curAction == null, "下一次英雄调度恢复输入，不重播破墙动作");
		}

		java.lang.reflect.Field user = Item.class.getDeclaredField("curUser"); user.setAccessible(true);
		Method cubeThrow = ForceCube.class.getDeclaredMethod("onThrow", int.class); cubeThrow.setAccessible(true);
		for (boolean cube : new boolean[]{false, true}) {
			level = fixture(); Dungeon.hero.pos = 57;
			Dungeon.hero.sprite = new CharSprite();
			new com.watabou.noosa.Group().add(Dungeon.hero.sprite);
			user.set(null, Dungeon.hero);
			Level.set(72, Terrain.COLLAPSE_WALL, level);
			TestMob enemy = new TestMob(); enemy.pos = 49; enemy.rooted = true; enemy.sprite.visible = false;
			level.mobs.add(enemy); Actor.add(enemy);
			if (cube) cubeThrow.invoke(new ForceCube(), 60);
			else {
				WandOfBlastWave blast = new WandOfBlastWave();
				check(!blast.zapTerrain(Dungeon.hero, 60), "冲击波直接命中岩壁仍交给范围伤害结算");
				Ballistica bolt = new Ballistica(Dungeon.hero.pos, 63, Ballistica.PROJECTILE);
				bolt.collisionPos = blast.targetingPos(Dungeon.hero, 63); bolt.dist = bolt.path.indexOf(bolt.collisionPos);
				blast.onZap(bolt);
			}
			check(level.map[60] == Terrain.EMPTY && level.map[72] == Terrain.EMPTY, "真实冲击波和方石范围同时击碎多格岩壁");
			check(level.map[84] == Terrain.COLLAPSE_WALL, "范围外支撑墙保留，不提前坍塌");
			check(enemy.HP < enemy.HT, "破墙不影响原有范围伤害");

			level = fixture(); Dungeon.hero.pos = 57;
			Dungeon.hero.sprite = new CharSprite(); new com.watabou.noosa.Group().add(Dungeon.hero.sprite);
			user.set(null, Dungeon.hero);
			level.affectTerrain(60, TerrainInteractions.Source.CLICK);
			TestMob victim = new TestMob(); victim.pos = 73; level.mobs.add(victim); Actor.add(victim);
			if (cube) cubeThrow.invoke(new ForceCube(), 84);
			else {
				Ballistica bolt = new Ballistica(Dungeon.hero.pos, 84, Ballistica.PROJECTILE);
				bolt.collisionPos = 84; bolt.dist = bolt.path.indexOf(84);
				new WandOfBlastWave().onZap(bolt);
			}
			for (int cell : REGION) check(level.map[cell] == Terrain.COLLAPSE_WALL, "范围击碎最后支撑墙后整片重新填满，不被同次攻击再次清理");
			check(!victim.isAlive() && level.heaps.get(73) != null, "范围触发坍塌正常结算死亡并掩埋掉落");
		}
	}

	private static void burialAndRevival() throws Exception {
		TestLevel level = fixture();
		Heap heap = level.drop(new Item(), 60); heap.seen = true;
		heap.sprite.link(heap); heap.sprite.update();
		check(!heap.sprite.visible, "墙下物品不显示");
		Method objects = GameScene.class.getDeclaredMethod("getObjectsAtCell", int.class); objects.setAccessible(true);
		check(!((ArrayList<?>)objects.invoke(null, 60)).contains(heap), "查看菜单不列出埋藏物品");
		TestHero hero = (TestHero)Dungeon.hero;
		hero.belongings.weapon = new Spear(); hero.pos = 60;
		Arrays.fill(hero.fieldOfView = new boolean[level.length()], true);
		check(hero.handle(60) && hero.curAction instanceof HeroAction.InteractTerrain, "脚下有埋藏物品仍执行破墙");
		Method reach = Hero.class.getDeclaredMethod("canInteractTerrain", int.class); reach.setAccessible(true);
		check((boolean)reach.invoke(hero, 60), "复活后能破坏脚下墙体脱困");
		hero.pos = 73; Actor.add(hero);
		level.affectTerrain(60, TerrainInteractions.Source.CLICK);
		level.affectTerrain(84, TerrainInteractions.Source.CLICK);
		check(hero.deaths == 1 && hero.HP == 5 && level.map[hero.pos] == Terrain.COLLAPSE_WALL, "坍塌调用英雄死亡入口，尊重入口的复活结果");
		hero.pos = 23;
		level.affectTerrain(60, TerrainInteractions.Source.CLICK); heap.sprite.update();
		check(heap.sprite.visible && ((ArrayList<?>)objects.invoke(null, 60)).contains(heap), "开墙后物品重新显示和可查看");

		int previousColor = Terrain.COLLAPSE_WALL_COLOR;
		try {
			Terrain.COLLAPSE_WALL_COLOR = 0x80C0FF;
			com.watabou.gltextures.SmartTexture original = com.watabou.gltextures.TextureCache.create(
					Assets.Environment.TILES_CAVES, 256, 256);
			original.bitmap.setBlending(com.badlogic.gdx.graphics.Pixmap.Blending.None);
			original.bitmap.drawPixel(2, 3, 0xA0C0E080);
			TestCaves rendered = new TestCaves(); rendered.setSize(11, 11);
			rendered.map = level.map.clone(); rendered.caveCollapse = level.caveCollapse;
			rendered.map[60] = Terrain.COLLAPSE_WALL; rendered.map[62] = Terrain.WALL;
			rendered.map[71] = Terrain.EMPTY;
			Dungeon.level = rendered;
			com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet.setupVariance(rendered.length(), 1234L);
			com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTerrainTilemap terrain =
					new com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTerrainTilemap();
			com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonWallsTilemap walls =
					new com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonWallsTilemap();
			java.lang.reflect.Field offsetField = com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap.class
					.getDeclaredField("collapseVisualOffset"); offsetField.setAccessible(true);
			int offset = offsetField.getInt(terrain);
			java.lang.reflect.Field textureField = com.watabou.noosa.Tilemap.class.getDeclaredField("texture");
			textureField.setAccessible(true);
			com.watabou.gltextures.SmartTexture colored = (com.watabou.gltextures.SmartTexture)textureField.get(terrain);
			check(colored.bitmap.getPixel(2, 3) == 0xA0C0E080, "原贴图片段保留原色和透明度");
			check(colored.bitmap.getPixel(2, 3 + original.height) == 0x5090E080, "染色副本按RGB乘色，保留透明度");
			check(textureField.get(walls) == colored, "墙面和墙顶复用同一染色缓存");
			Method terrainVisual = terrain.getClass().getDeclaredMethod("getTileVisual", int.class, int.class, boolean.class);
			terrainVisual.setAccessible(true);
			check((int)terrainVisual.invoke(terrain, 60, Terrain.COLLAPSE_WALL, false)
					== (int)terrainVisual.invoke(terrain, 60, Terrain.WALL, false) + offset, "塌方墙面使用染色片段");
			check((int)terrainVisual.invoke(terrain, 60, Terrain.COLLAPSE_WALL, true)
					== (int)terrainVisual.invoke(terrain, 60, Terrain.WALL, true) + offset, "俯视和详情贴图使用染色片段");
			check((int)terrainVisual.invoke(terrain, 62, Terrain.WALL, false) < offset, "普通墙保留原贴图");
			Method wallVisual = walls.getClass().getDeclaredMethod("getTileVisual", int.class, int.class, boolean.class);
			wallVisual.setAccessible(true);
			check((int)wallVisual.invoke(walls, 49, Terrain.EMPTY, false) >= offset, "塌方墙上沿使用染色片段");
			rendered.map[71] = Terrain.COLLAPSE_WALL;
			check((int)wallVisual.invoke(walls, 60, Terrain.COLLAPSE_WALL, false) >= offset, "塌方墙顶使用染色片段");
			check((int)terrainVisual.invoke(terrain, 60, Terrain.COLLAPSE_WALL, false) == -1, "被墙顶遮盖的墙面继续不绘制");
		} finally {
			Terrain.COLLAPSE_WALL_COLOR = previousColor;
			Dungeon.level = level;
		}

		float previousInterval = Terrain.COLLAPSE_PARTICLE_INTERVAL;
		try {
			Terrain.COLLAPSE_PARTICLE_INTERVAL = 0.75f;
			java.lang.reflect.Field members = com.watabou.noosa.Group.class.getDeclaredField("members");
			members.setAccessible(true);
			ArrayList<?> winds = (ArrayList<?>)members.get(level.addWallVisuals());
			check(winds.size() == 8, "塌方区域每格保留独立粒子标记");
			java.lang.reflect.Field interval = com.watabou.noosa.particles.Emitter.class.getDeclaredField("interval");
			interval.setAccessible(true);
			for (Object wind : winds) check(interval.getFloat(wind) == 0.75f, "塌方粒子使用可配置生成间隔");
			level.map[23] = Terrain.CHASM; level.buildFlagMaps();
			ArrayList<?> chasms = (ArrayList<?>)members.get(level.addVisuals());
			check(chasms.size() == 1 && interval.getFloat(chasms.get(0)) == 2.5f, "普通深渊粒子保持原密度");
			Terrain.COLLAPSE_PARTICLE_INTERVAL = 0;
			check(((ArrayList<?>)members.get(level.addWallVisuals())).isEmpty(), "生成间隔为0时关闭塌方粒子");
		} finally { Terrain.COLLAPSE_PARTICLE_INTERVAL = previousInterval; }
	}

	private static TestLevel fixture() {
		TestLevel level = level(11);
		for (int y = 1; y < 10; y++) for (int x = 1; x < 10; x++) level.map[x + y * 11] = Terrain.EMPTY;
		level.map[60] = level.map[84] = level.map[96] = Terrain.COLLAPSE_WALL;
		level.buildFlagMaps(); level.cleanWalls();
		level.caveCollapse = new CaveCollapse(level);
		Bundle regions = new Bundle(); regions.put("count", 2);
		regions.put("region_0", REGION); regions.put("region_1", new int[]{96, 97, 108});
		int[] ground = level.map.clone();
		ground[60] = ground[84] = ground[96] = Terrain.EMPTY;
		regions.put("buried_terrain", ground);
		level.caveCollapse.restoreFromBundle(regions);
		return level;
	}

	private static TestLevel level(int width) {
		Actor.clear(); Dungeon.hero = new TestHero(); Dungeon.hero.pos = width + 1;
		Dungeon.depth = 11; Dungeon.branch = 0;
		TestLevel level = new TestLevel(); level.setSize(width, width);
		level.blobs = new HashMap<>(); level.traps = new SparseArray<>(); level.heaps = new SparseArray<>();
		level.plants = new SparseArray<>(); level.mobs = new HashSet<>(); level.transitions = new ArrayList<>();
		level.customTiles = new ArrayList<>(); level.customTerrain = new ArrayList<>(); level.customWalls = new ArrayList<>();
		Dungeon.level = level;
		return level;
	}

	private static void check(boolean condition, String message) {
		checks++; if (!condition) throw new AssertionError(message);
	}

	public static class TestLevel extends Level {
		@Override protected boolean build() { return true; }
		@Override protected void createMobs() { }
		@Override protected void createItems() { }
		@Override public void updateFieldOfView(Char ch, boolean[] fov) { Arrays.fill(fov, false); }
		@Override public Heap drop(Item item, int cell) {
			Heap heap = super.drop(item, cell);
			if (heap.sprite == null) heap.sprite = new ItemSprite() {
				@Override public ItemSprite view(Heap heap) { return this; }
				@Override public void place(int cell) { }
				@Override public void drop(int from) { }
				@Override public void drop() { }
			};
			return heap;
		}
	}
	private static class TestHero extends Hero {
		int deaths;
		int turns;
		@Override public void die(Object cause) { deaths++; HP = 5; curAction = null; }
		@Override public void spendAndNext(float time) { turns++; super.spendAndNext(time); }
	}
	private static class TestMob extends Mob {
		TestMob() { HP = HT = 20; sprite = new CharSprite() { @Override public void die() { } }; }
		@Override public void rollToDropLoot() { Dungeon.level.drop(new Item(), pos); }
		@Override public void destroy() { HP = 0; Actor.remove(this); Dungeon.level.mobs.remove(this); }
	}
	private static class TestStone extends ThrowingStone {
		void land(int cell) { curUser = Dungeon.hero; onThrow(cell); }
	}
	private static class TestCaves extends CavesLevel {
		@Override protected void createMobs() { }
		@Override protected void createItems() { }
		@Override public Heap drop(Item item, int cell) {
			Heap heap = super.drop(item, cell);
			if (heap.sprite == null) heap.sprite = new ItemSprite() {
				@Override public ItemSprite view(Heap heap) { return this; }
				@Override public void place(int cell) { }
				@Override public void drop(int from) { }
				@Override public void drop() { }
			};
			return heap;
		}
	}
}
