package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.Door;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.CavesPainter;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.CellPlacement;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.CaveRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.CavesStairRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.builders.CavesBuilder;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.connection.CavesPathRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.BlacksmithRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.SuperSecretRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.SecretRoom;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;

/** Runs without a graphics context: gradlew :core:cavesTerrainRegression. */
public class CavesTerrainRegression {
	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}

	private static void checkBranches(Fixture level, int expected) {
		HashSet<Room> remaining = new HashSet<>(level.rooms);
		remaining.remove(level.roomEntrance);
		remaining.remove(level.roomExit);
		int components = 0, onPath = 0, besidePath = 0;
		while (!remaining.isEmpty()) {
			ArrayList<Room> queue = new ArrayList<>();
			Room first = remaining.iterator().next();
			remaining.remove(first);
			queue.add(first);
			boolean touchesEntrance = false, touchesExit = false;
			HashSet<Integer> branchIds = new HashSet<>();
			for (int i = 0; i < queue.size(); i++) {
				Room room = queue.get(i);
				if (room instanceof CavesPathRoom) branchIds.add(((CavesPathRoom) room).branch);
				int pathConnections = 0;
				for (Room next : room.connected.keySet()) {
					if (next == level.roomEntrance) touchesEntrance = true;
					else if (next == level.roomExit) touchesExit = true;
					else if (remaining.remove(next)) queue.add(next);
					if (!(next instanceof SecretRoom)) pathConnections++;
				}
				if ((room instanceof StandardRoom || room instanceof SpecialRoom && !(room instanceof SecretRoom))
						&& !room.isEntrance() && !room.isExit()) {
					if (pathConnections >= 2) onPath++;
					else if (pathConnections == 1) besidePath++;
				}
			}
			check(branchIds.size() <= 1, "branches cross outside stair rooms");
			if (touchesEntrance && touchesExit) components++;
			else check(touchesEntrance || touchesExit, "side room is disconnected from main branches");
		}
		check(components == expected, "expected " + expected + " distinct branches, got " + components);
		check(onPath > 0 && besidePath > 0, "need rooms both on and beside paths: on=" + onPath
				+ " beside=" + besidePath + " seed=" + Dungeon.seed + " depth=" + Dungeon.depth);
	}

	private static void checkMixedRoomLayouts() {
		for (int seed = 0; seed < 128; seed++) {
			Dungeon.seed = seed;
			Dungeon.depth = 11 + seed % 4;
			Random.pushGenerator(seed);
			try {
				Fixture level = new Fixture();
				ArrayList<Room> rooms = new ArrayList<>();
				rooms.add(level.roomEntrance = new CavesStairRoom(true));
				rooms.add(level.roomExit = new CavesStairRoom(false));
				int budget = seed % 2 == 0 ? 7 : 11;
				for (int i = 0; i < budget; i++) {
					StandardRoom room;
					do { room = StandardRoom.createRoom(); } while (!room.setSizeCat(budget - i));
					rooms.add(room);
					i += room.sizeFactor() - 1;
				}
				rooms.add(new BlacksmithRoom());
				rooms.add(new SuperSecretRoom());
				rooms.add(new TestSpecialRoom(Room.Door.Type.LOCKED, Terrain.LOCKED_DOOR));
				CavesBuilder builder = new CavesBuilder(seed % 2 != 0);
				int attempts = 0;
				do {
					check(++attempts <= 128, "mixed room layout retries exceeded: seed=" + seed);
					level.rooms = builder.build(new ArrayList<>(rooms));
				} while (level.rooms == null);
				checkBranches(level, builder.branchCount());
				for (Room room : level.rooms) if (room instanceof SpecialRoom) {
					check(!room.connected.isEmpty(), "mixed special room is disconnected");
					check(room.connected.size() == 1, "special room must retain its single entrance");
				}
			} finally {
				Random.popGenerator();
			}
		}
	}

	private static int[] seededMap(int seed) {
		Dungeon.seed = seed;
		Dungeon.depth = 11 + seed % 4;
		Random.pushGenerator(seed * 100L + Dungeon.depth);
		try {
			Fixture level = new Fixture();
			level.feeling = seed % 2 == 0 ? Level.Feeling.LARGE : Level.Feeling.CHASM;
			Dungeon.level = level;
			int attempts = 0;
			while (!level.build()) check(++attempts <= 128, "repeatability layout retries exceeded");
			int[] map = Arrays.copyOf(level.terrainVisuals(), level.length() + 2);
			map[level.length()] = level.width();
			map[level.length() + 1] = level.height();
			return map;
		} finally {
			Random.popGenerator();
		}
	}

	public static void main(String[] args) {
		for (int depth = 12; depth <= 14; depth++) {
			Dungeon.depth = depth;
			int threeBranches = 0;
			for (int seed = 0; seed < 1024; seed++) {
				Random.pushGenerator(seed);
				int count = new CavesBuilder(false).branchCount();
				Random.popGenerator();
				Random.pushGenerator(seed);
				check(new CavesBuilder(true).branchCount() == count + 1, "large floor needs exactly one extra branch");
				Random.popGenerator();
				if (count == 3) threeBranches++;
			}
			check(threeBranches > 686 && threeBranches < 850, "expected 75% three-branch layouts");
		}
		check(CavesLevel.Orientation.SOUTH.accepts(41, -100), "north sector interior");
		check(!CavesLevel.Orientation.SOUTH.accepts(42, -100), "north sector exterior");
		check(!CavesLevel.Orientation.SOUTH.accepts(0, 100), "opposite direction");
		check(!CavesLevel.Orientation.SOUTH.accepts(0, 0), "coincident stairs");
		check(CavesLevel.Orientation.NORTH.accepts(-41, 100), "south sector");
		check(CavesLevel.Orientation.EAST.accepts(-100, 41), "west sector");
		check(CavesLevel.Orientation.WEST.accepts(100, -41), "east sector");
		Dungeon.branch = 0;
		checkMixedRoomLayouts();
		EnumSet<CavesLevel.Orientation> orientations = EnumSet.noneOf(CavesLevel.Orientation.class);
		int levels = 0, attempts = 0;
		for (long seed = 0; seed < 32; seed++) {
			Dungeon.seed = seed;
			CavesLevel.Orientation orientation = CavesLevel.orientation();
			orientations.add(orientation);
			Random.pushGenerator(seed);
			long expected = Random.Long();
			Random.popGenerator();
			Random.pushGenerator(seed);
			CavesLevel.orientation();
			check(Random.Long() == expected, "orientation must preserve caller RNG");
			Random.popGenerator();
			for (int depth = 11; depth <= 14; depth++) {
				Dungeon.depth = depth;
				check(CavesLevel.orientation() == orientation, "orientation changed between depths");
				for (Level.Feeling feeling : new Level.Feeling[]{Level.Feeling.NONE, Level.Feeling.CHASM, Level.Feeling.LARGE}) {
					Random.pushGenerator(seed * 100 + depth);
					try {
						Fixture level;
						int tries = 0;
						do {
							check(++tries <= 256, "generation retries exceeded: seed=" + seed + " depth=" + depth);
							level = new Fixture();
							level.feeling = feeling;
							Dungeon.level = level;
						} while (!level.build());
						attempts += tries;
						level.buildFlagMaps();
						int entrance = level.entrance(), exit = level.exit();
						check(entrance == level.pointToCell(new com.watabou.utils.Point(
								(level.roomEntrance.left + level.roomEntrance.right) / 2,
								(level.roomEntrance.top + level.roomEntrance.bottom) / 2)), "entrance moved after planning");
						check(exit == level.pointToCell(new com.watabou.utils.Point(
								(level.roomExit.left + level.roomExit.right) / 2,
								(level.roomExit.top + level.roomExit.bottom) / 2)), "exit moved after planning");
						int branches = ((CavesBuilder) level.builder).branchCount();
						int extra = feeling == Level.Feeling.LARGE ? 1 : 0;
						check(depth == 11 ? branches == 2 + extra : branches >= 2 + extra && branches <= 3 + extra,
								"incorrect branch count");
						checkBranches(level, branches);
						check(orientation.accepts(exit % level.width() - entrance % level.width(),
								exit / level.width() - entrance / level.width()), "stair bearing");
						PathFinder.buildDistanceMap(exit, level.passable);
						check(PathFinder.distance[entrance] != Integer.MAX_VALUE, "stairs are disconnected");
						int metal = 0, pits = 0;
						for (int cell = 0; cell < level.length(); cell++) {
							if (level.hasTerrain(cell, Terrain.REGION_DECO)) {
								metal++;
								check(level.passable[cell], "metal connection blocks walking");
							}
							if (level.pit[cell]) pits++;
						}
						check(metal > 0, "missing metal connections");
						if (feeling == Level.Feeling.CHASM) check(pits > 0, "missing chasms");
						for (Room room : level.rooms) {
							if (room instanceof TestSpecialRoom) {
								for (Room.Door door : room.connected.values()) {
									int cell = level.pointToCell(door);
									check(Door.at(level, cell) == null && level.hasTerrain(cell, Terrain.REGION_DECO),
											"special room entrance must use architecture");
								}
							}
							for (Room neighbour : room.connected.keySet()) {
								if (room instanceof SecretRoom || neighbour instanceof SecretRoom) continue;
								check(Door.at(level, level.pointToCell(room.connected.get(neighbour))) == null,
										"ordinary connection still has door");
							}
						}
						levels++;
					} finally {
						Random.popGenerator();
					}
				}
			}
		}
		check(orientations.size() == 4, "all four orientations must be covered");
		for (int seed = 0; seed < 16; seed++) {
			check(Arrays.equals(seededMap(seed), seededMap(seed)), "same seed generated different maps: " + seed);
		}
		System.out.println("PASS: " + levels + " painted cave layouts + 128 mixed room layouts, all orientations, normal/chasm/large floors, fixed stairs, distinct branches and architecture entrances; " + attempts + " build attempts; branch probability checked over 1024 seeds/depth");
	}

	private static class Fixture extends CavesLevel {
		@Override protected com.shatteredpixel.shatteredpixeldungeon.levels.builders.Builder builder() {
			CavesBuilder result = new CavesBuilder(feeling == Feeling.LARGE) {
				private int attempts;
				@Override public ArrayList<Room> build(ArrayList<Room> rooms) {
					check(++attempts <= 128, "layout retries exceeded: seed=" + Dungeon.seed + " depth=" + Dungeon.depth);
					return super.build(rooms);
				}
			};
			result.chasm = feeling == Feeling.CHASM;
			return result;
		}
		Fixture() {
			transitions = new ArrayList<>();
			mobs = new HashSet<>();
			heaps = new SparseArray<>();
			blobs = new HashMap<>();
			plants = new SparseArray<>();
			traps = new SparseArray<>();
			customTiles = new ArrayList<>();
			customTerrain = new ArrayList<>();
			customWalls = new ArrayList<>();
		}

		@Override protected ArrayList<Room> initRooms() {
			ArrayList<Room> rooms = new ArrayList<>();
			rooms.add(roomEntrance = new CavesStairRoom(true));
			rooms.add(roomExit = new CavesStairRoom(false));
			for (int i = 0; i < 7; i++) {
				CaveRoom room = new CaveRoom();
				while (!room.setSizeCat(7 - i)) { }
				rooms.add(room);
				i += room.sizeFactor() - 1;
			}
			rooms.add(new TestSpecialRoom(Room.Door.Type.LOCKED, Terrain.LOCKED_DOOR));
			rooms.add(new TestSpecialRoom(Room.Door.Type.UNLOCKED, Terrain.DOOR));
			rooms.add(new TestSpecialRoom(Room.Door.Type.HIDDEN, Terrain.SECRET_DOOR));
			return rooms;
		}

		@Override public Painter painter() {
			return new CavesPainter();
		}
	}

	private static class TestSpecialRoom extends SpecialRoom {
		private final Room.Door.Type doorType;
		final int expectedTerrain;

		TestSpecialRoom(Room.Door.Type doorType, int expectedTerrain) {
			this.doorType = doorType;
			this.expectedTerrain = expectedTerrain;
		}

		@Override public void paint(Level level) {
			Painter.fill(level, this, CellPlacement.reset(Terrain.EMPTY, Terrain.WALL));
			Painter.fill(level, this, 1, CellPlacement.reset(Terrain.EMPTY));
			entrance().set(doorType);
		}
	}
}
