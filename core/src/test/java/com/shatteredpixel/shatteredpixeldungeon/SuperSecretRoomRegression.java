package com.shatteredpixel.shatteredpixeldungeon;

import com.badlogic.gdx.utils.GdxNativesLoader;
import com.badlogic.gdx.Preferences;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Group;
import com.watabou.utils.GameSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ForceCube;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.HiddenLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.TerrainInteractions;
import com.shatteredpixel.shatteredpixeldungeon.levels.builders.Builder;
import com.shatteredpixel.shatteredpixeldungeon.levels.builders.LoopBuilder;
import com.shatteredpixel.shatteredpixeldungeon.levels.builders.FigureEightBuilder;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.SecretRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.SuperSecretRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.EmptyRoom;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.lang.reflect.Proxy;

/** 固定种子的房型、区域配额、奖励分布和破墙回归，不需要图形环境。 */
public class SuperSecretRoomRegression {

	private static int checks;

	public static void main(String[] args) {
		GdxNativesLoader.load();
		TextureCache.create(Assets.Sprites.ITEM_ICONS, 128, 128);
		TextureCache.create(Assets.Effects.EFFECTS, 64, 64);
		// 用内存配置验证全局提示，避免写入玩家的真实设置。
		HashMap<String, Object> preferences = new HashMap<>();
		GameSettings.set((Preferences) Proxy.newProxyInstance(Preferences.class.getClassLoader(),
				new Class<?>[]{Preferences.class}, (proxy, method, values) -> {
					if (method.getName().startsWith("put") && values != null && values.length == 2) {
						preferences.put((String) values[0], values[1]);
						return proxy;
					}
					if (method.getName().startsWith("get") && values != null && values.length == 2) {
						return preferences.getOrDefault(values[0], values[1]);
					}
					if (method.getReturnType() == boolean.class) return false;
					return null;
				}));
		Dungeon.hero = new Hero();
		Dungeon.hero.sprite = new CharSprite();
		new Group().add(Dungeon.hero.sprite);
		Dungeon.depth = 1;
		Dungeon.branch = 0;
		Random.pushGenerator(20260929L);
		Generator.fullReset();
		com.shatteredpixel.shatteredpixeldungeon.journal.Notes.reset();
		regions();
		floorIntegration();
		rotationsAndRewards();
		generation();
		Random.popGenerator();
		System.out.println("SuperSecretRoomRegression passed: " + checks + " checks");
	}

	private static void floorIntegration() {
		SpecialRoom.initForRun();
		SecretRoom.initForRun();
		TestRegularLevel level = new TestRegularLevel();
		for (int depth = 1; depth <= 24; depth++) {
			if (depth % 5 == 0) continue;
			Dungeon.depth = depth;
			check(level.countSuperRooms() == (SecretRoom.superSecretForFloor(depth) ? 1 : 0), "普通楼层生成入口接入区域计划");
			Dungeon.branch = 2;
			check(level.countSuperRooms() == 0, "支线不重复生成超级隐藏房");
			Dungeon.branch = 0;
		}
	}

	private static void regions() {
		boolean[] seen = new boolean[25];
		for (int run = 0; run < 200; run++) {
			SecretRoom.initForRun();
			boolean[] selected = new boolean[25];
			for (int region = 0; region < 5; region++) {
				int count = 0;
				for (int depth = region * 5 + 1; depth <= region * 5 + 5; depth++) {
					if (SecretRoom.superSecretForFloor(depth)) {
						selected[depth] = seen[depth] = true;
						count++;
					}
				}
				check(count == 1, "每区域恰好一间，Boss 层不生成");
			}
			Bundle saved = new Bundle();
			SecretRoom.storeRoomsInBundle(saved);
			SecretRoom.initForRun();
			SecretRoom.restoreRoomsFromBundle(saved);
			for (int depth = 1; depth < 25; depth++) {
				check(SecretRoom.superSecretForFloor(depth) == selected[depth], "读档保留楼层选择且查询不消耗配额");
			}
		}
		for (int depth = 1; depth < 25; depth++) check(seen[depth] == (depth % 5 != 0), "所有普通楼层都有机会生成");
		check(!SecretRoom.superSecretForFloor(0) && !SecretRoom.superSecretForFloor(26), "范围外不生成");
	}

	private static void rotationsAndRewards() {
		int[] upgrades = new int[4];
		int[] categories = new int[4];
		for (int sample = 0; sample < 4000; sample++) {
			Dungeon.depth = (sample % 5) * 5 + 1;
			TestLevel level = new TestLevel();
			level.setSize(15, 15);
			SuperSecretRoom room = new SuperSecretRoom();
			int direction = sample % 4;
			room.resize(direction < 2 ? 6 : 4, direction < 2 ? 4 : 6);
			room.setPos(4, 4);
			Room.Door door = direction == 0 ? new Room.Door(room.left, room.top + 2)
					: direction == 1 ? new Room.Door(room.right, room.top + 2)
					: direction == 2 ? new Room.Door(room.left + 2, room.top)
					: new Room.Door(room.left + 2, room.bottom);
			room.connected.put(new EmptyRoom(), door);
			room.paint(level);
			level.buildFlagMaps();
			level.cleanWalls();
			validate(level, room);
			int pedestal = level.pointToCell(room.pointInside(door, 4));
			Item prize = level.heaps.get(pedestal).peek();
			check(prize.level() >= 1 && prize.level() <= 3, "最终等级在 +1 到 +3 之间");
			upgrades[prize.level()]++;
			int category = prize instanceof Weapon ? 0 : prize instanceof Armor ? 1 : prize instanceof Wand ? 2 : 3;
			check(category != 3 || prize instanceof Ring, "奖励属于四类装备");
			categories[category]++;
			check(!prize.cursed && prize.cursedKnown, "奖励明确未诅咒");
			if (prize instanceof Weapon) check(!((Weapon) prize).hasCurseEnchant(), "武器无诅咒附魔");
			if (prize instanceof Armor) check(!((Armor) prize).hasCurseGlyph(), "护甲无诅咒刻印");
			int tier = prize instanceof MeleeWeapon ? ((MeleeWeapon) prize).tier : prize instanceof Armor ? ((Armor) prize).tier : 0;
			if (tier != 0) {
				int minTier = Dungeon.depth < 6 ? 2 : Dungeon.depth < 16 ? 3 : 4;
				check(tier >= minTier && tier <= 5, "阶数服从隐藏迷宫房的下一区域概率表");
			}
			if (sample < 4) interactions(level, room);
		}
		check(Math.abs(upgrades[1] / 4000f - 0.5f) < 0.03f, "+1 概率约 50%");
		check(Math.abs(upgrades[2] / 4000f - 0.3f) < 0.03f, "+2 概率约 30%");
		check(Math.abs(upgrades[3] / 4000f - 0.2f) < 0.03f, "+3 概率约 20%");
		float[] categoryChances = {0.125f, 0.125f, 0.5f, 0.25f};
		for (int i = 0; i < categories.length; i++) {
			check(Math.abs(categories[i] / 4000f - categoryChances[i]) < 0.03f,
					"武器护甲合计 25%，法杖 50%，戒指 25%");
		}
		System.out.println("强化等级抽样: " + Arrays.toString(upgrades) + "; 装备类别抽样: " + Arrays.toString(categories));
	}

	private static void validate(TestLevel level, SuperSecretRoom room) {
		Room.Door door = room.entrance();
		int entrance = level.pointToCell(door);
		int pedestal = level.pointToCell(room.pointInside(door, 4));
		check(level.solid[entrance] && !level.secret[entrance], "入口是实墙而非隐藏门");
		check(door.type == Room.Door.Type.WALL, "共享连接不能被画成门");
		int floorCount = 0;
		for (Point p : room.getPoints()) if (level.passable[level.pointToCell(p)]) floorCount++;
		check(floorCount == 11, "恰好两格直道加 3×3 宝物区");
		for (int step = 1; step <= 2; step++) check(level.passable[level.pointToCell(room.pointInside(door, step))], "直道连续");
		check(level.map[pedestal] == Terrain.PEDESTAL && level.heaps.get(pedestal).items.size() == 1, "基座仅放一件奖励");
		check(level.heaps.get(pedestal).autoExplored, "基座奖励不计探索分");
		for (int offset : PathFinder.NEIGHBOURS8) {
			Heap heap = level.heaps.get(pedestal + offset);
			check(heap != null && heap.items.size() == 1 && heap.peek() instanceof Gold, "周围八格各有一堆金币");
			check(heap.autoExplored, "周围金币不计探索分");
			check(heap.peek().quantity() >= 15 + Dungeon.depth * 5
					&& heap.peek().quantity() <= 30 + Dungeon.depth * 10, "金币数量使用减半后的楼层范围");
		}
	}

	private static void interactions(TestLevel level, SuperSecretRoom room) {
		Dungeon.level = level;
		Dungeon.hero.pos = 16;
		Dungeon.hero.fieldOfView = new boolean[level.length()];
		int wall = level.pointToCell(room.entrance());
		Bundle saved = new Bundle();
		level.interactions().storeInBundle(saved);
		level.terrainInteractions = new TerrainInteractions(level);
		level.interactions().restoreFromBundle(saved);
		for (TerrainInteractions.Source source : new TerrainInteractions.Source[]{
				TerrainInteractions.Source.CLICK, TerrainInteractions.Source.FIRE, TerrainInteractions.Source.ELECTRIC}) {
			check(!level.affectTerrain(wall, source) && level.solid[wall], "点击、火焰、电击不能破墙");
		}
		TerrainInteractions.Rule rule = level.interactions().ruleAt(wall);
		for (TerrainInteractions.Source source : new TerrainInteractions.Source[]{
				TerrainInteractions.Source.EXPLOSION, TerrainInteractions.Source.DISINTEGRATION, TerrainInteractions.Source.SHOCKWAVE}) {
			Level.set(wall, Terrain.WALL, level);
			level.interactions().setOverride(wall, rule);
			check(level.affectTerrain(wall, source) && level.passable[wall] && !level.losBlocking[wall], "读档后各类作用仍可破墙并更新通行、视野");
			check(!level.affectTerrain(wall + 1, source) || !level.solid[wall + 1], "相邻普通墙不受影响");
		}
		Level.set(wall, Terrain.WALL, level);
		level.interactions().setOverride(wall, rule);
		int inside = level.pointToCell(room.pointInside(room.entrance(), 1));
		int outside = 2 * wall - inside;
		Level.set(outside, Terrain.EMPTY, level);
		new Bomb().explode(outside);
		check(level.passable[wall], "实际炸弹从入口外炸开墙壁");
		Level.set(wall, Terrain.WALL, level);
		level.interactions().setOverride(wall, rule);
		new WandOfBlastWave().onZap(new Ballistica(outside, wall, Ballistica.PROJECTILE));
		check(level.passable[wall], "实际冲击波法杖从入口外炸开墙壁");
		Level.set(wall, Terrain.WALL, level);
		level.interactions().setOverride(wall, rule);
		new TestCube().throwAt(outside);
		check(level.passable[wall], "实际震爆方石从入口外炸开墙壁");
		Dungeon.level = null;
	}

	private static void generation() {
		int[] orientations = new int[4];
		for (int sample = 0; sample < 100; sample++) {
			Dungeon.depth = sample % 5 * 5 + 1;
			TestLevel level = new TestLevel();
			ArrayList<Room> initial = new ArrayList<>();
			initial.add(new EmptyRoom() { @Override public boolean isEntrance() { return true; } });
			initial.add(new EmptyRoom() { @Override public boolean isExit() { return true; } });
			for (int i = 0; i < 8; i++) initial.add(new EmptyRoom());
			SuperSecretRoom room = new SuperSecretRoom();
			initial.add(room);
			Builder builder = sample % 2 == 0 ? new LoopBuilder() : new FigureEightBuilder();
			ArrayList<Room> placed = null;
			int attempts = 0;
			while (placed == null && attempts++ < 100) {
				for (Room r : initial) { r.neigbours.clear(); r.connected.clear(); }
				placed = builder.build(new ArrayList<>(initial));
			}
			check(placed != null, "两种地图构建器均能放置超级隐藏房");
			RegularPainter painter = sample % 5 == 0 ? new SewerPainter() : sample % 5 == 1 ? new PrisonPainter()
					: sample % 5 == 2 ? new CavesPainter() : sample % 5 == 3 ? new CityPainter() : new HallsPainter();
			painter.setWater(0.8f, 4).setGrass(0.8f, 4);
			check(painter.paint(level, placed), "五区域画笔可绘制房间");
			level.buildFlagMaps();
			level.cleanWalls();
			validate(level, room);
			Room.Door door = room.entrance();
			int direction = door.x == room.left ? 0 : door.x == room.right ? 1 : door.y == room.top ? 2 : 3;
			orientations[direction]++;
			int wall = level.pointToCell(door);
			int outside = 2 * wall - level.pointToCell(room.pointInside(door, 1));
			check(level.passable[outside] || level.avoid[outside], "入口直接接入可到达的外部通路");
			//清除普通房间的门障碍，保留超级隐藏房的实墙与未见奖励，隔离可选解谜的评分影响。
			for (Room r : placed) level.addRoom(r);
			for (int cell = 0; cell < level.length(); cell++) {
				if (level.map[cell] == Terrain.SECRET_DOOR || level.map[cell] == Terrain.LOCKED_DOOR
						|| level.map[cell] == Terrain.BARRICADE) Level.set(cell, Terrain.DOOR, level);
			}
			check(level.levelExplorePercent(Dungeon.depth) == 1f, "未破超级隐藏房入口实墙、未见奖励仍为满探索分");
			// 高水草覆盖可能没有合格地面，为隐藏入口评分检查明确预留一格空地。
			Level.set(level.pointToCell(level.room(EmptyRoom.class).center()), Terrain.EMPTY, level);
			check(level.placeHiddenEntrance(false), "普通房间可放置隐藏楼层入口");
			check(level.levelExplorePercent(Dungeon.depth) == 1f, "未揭露隐藏楼层入口不扣探索分");
			if (sample < 2) {
				Dungeon.level = level;
				Dungeon.hero.pos = outside;
				Dungeon.hero.fieldOfView = new boolean[level.length()];
				Dungeon.observe();
				check(level.heaps.get(outside) == null, "未看见入口时不提前生成提示炸弹");
				level.visibleCells = new int[]{wall, outside};
				if (sample == 0) {
					check(!SPDSettings.getBoolean(SPDSettings.KEY_SUPER_SECRET_HINT, false), "未遇到入口不消耗提示");
					Dungeon.observe();
					Heap hint = level.heaps.get(outside);
					check(hint != null && hint.peek() instanceof Bomb && hint.items.size() == 1, "首次看见入口在墙外脚下生成一个炸弹");
					check(hint.autoExplored && hint.peek().quantity() == 1, "提示炸弹仅一枚且不计探索分");
					check(((Bomb) hint.peek()).fuse == null, "提示炸弹未点燃");
					Dungeon.observe();
					check(hint.items.size() == 1, "重复刷新视野不会重复生成");
					level.heaps.remove(outside);
					SecretRoom.initForRun();
					Dungeon.LimitedDrops.reset();
					check(SPDSettings.getBoolean(SPDSettings.KEY_SUPER_SECRET_HINT, false), "新开局不重置全局提示");
				} else {
					Dungeon.observe();
					check(level.heaps.get(outside) == null, "另一局遇到新房间也不再生成提示炸弹");
				}
				Dungeon.level = null;
			}
			Dungeon.level = level;
			check(level.affectTerrain(wall, TerrainInteractions.Source.DISINTEGRATION), "评分验证中可破坏超级隐藏房入口墙");
			check(level.levelExplorePercent(Dungeon.depth) == 1f, "破墙后未取奖励仍为满探索分");
			int hiddenEntrance = level.hiddenEntranceCell;
			if (level.hiddenEntranceGrass) Level.set(hiddenEntrance, Terrain.EMPTY, level);
			else check(level.affectTerrain(hiddenEntrance, TerrainInteractions.Source.EXPLOSION), "评分验证中可揭露隐藏楼梯");
			check(level.hiddenEntranceCell == -1 && level.map[hiddenEntrance] == Terrain.EXIT,
					"隐藏入口解谜确实完成");
			check(level.levelExplorePercent(Dungeon.depth) == 1f, "隐藏入口解谜前后探索分一致");
			Dungeon.branch = 0;
			Dungeon.updateLevelExplored();
			check(Statistics.floorsExplored.get(Dungeon.depth) == 1f, "主线记录满探索分");
			Dungeon.branch = 2;
			Dungeon.level = new HiddenLevel();
			Dungeon.updateLevelExplored();
			check(Statistics.floorsExplored.get(Dungeon.depth) == 1f, "隐藏支线不覆盖同深度主线探索分");
			Dungeon.branch = 0;
			Dungeon.level = level;
			Room ordinaryRoom = level.room(EmptyRoom.class);
			int ordinaryCell = level.pointToCell(ordinaryRoom.center());
			int originalTerrain = level.map[ordinaryCell];
			Level.set(ordinaryCell, Terrain.BARRICADE, level);
			check(level.levelExplorePercent(Dungeon.depth) == 0.5f, "普通房间的未解除障碍仍扣探索分");
			Level.set(ordinaryCell, originalTerrain, level);
			Dungeon.level = null;
		}
		for (int count : orientations) check(count > 0, "地图生成覆盖四向旋转");
	}

	private static void check(boolean condition, String message) {
		checks++;
		if (!condition) throw new AssertionError(message);
	}

	private static class TestRegularLevel extends RegularLevel {
		@Override protected Painter painter() { return new SewerPainter(); }
		int countSuperRooms() {
			int count = 0;
			for (Room room : initRooms()) if (room instanceof SuperSecretRoom) count++;
			return count;
		}
	}

	private static class TestCube extends ForceCube {
		void throwAt(int cell) { curUser = Dungeon.hero; onThrow(cell); }
		@Override protected void rangedHit(Char enemy, int cell) { }
	}

	/** 只替换物品精灵与视野显示，保留真实生成和交互逻辑。 */
	private static class TestLevel extends SewerLevel {
		int[] visibleCells = new int[0];
		void addRoom(Room r) { rooms.add(r); }
		TestLevel() {
			rooms = new ArrayList<>();
			blobs = new HashMap<>(); traps = new SparseArray<>(); heaps = new SparseArray<>();
			plants = new SparseArray<>(); mobs = new HashSet<>(); transitions = new ArrayList<>();
			customTiles = new ArrayList<>(); customTerrain = new ArrayList<>(); customWalls = new ArrayList<>();
		}
		@Override protected boolean build() { return true; }
		@Override protected void createMobs() { }
		@Override protected void createItems() { }
		@Override public Heap drop(Item item, int cell) {
			Heap heap = heaps.get(cell);
			if (heap == null) { heap = new Heap(); heap.pos = cell; heaps.put(cell, heap); }
			heap.items.add(item);
			return heap;
		}
		@Override public void updateFieldOfView(Char ch, boolean[] fov) {
			Arrays.fill(fov, false);
			for (int cell : visibleCells) fov[cell] = true;
		}
	}
}
