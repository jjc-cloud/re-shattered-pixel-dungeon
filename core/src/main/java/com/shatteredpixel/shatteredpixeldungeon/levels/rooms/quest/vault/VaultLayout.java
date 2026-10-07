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

package com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.vault;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultTokenDoor;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.ImpStatue;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.tiles.custom.VaultTiles;
import com.watabou.utils.Random;

import java.util.ArrayList;

/** 16px grid transcribed from 宝库地图.png (640 x 1184), including its wall border. */
public final class VaultLayout {
	private VaultLayout() {}

	public static final int WIDTH = 40;
	public static final int HEIGHT = 74;
	public static final int GATE_X = 19, GATE_Y = 44, GATE_WIDTH = 3;
	public static final int TREASURE_DOOR_X = 20, TREASURE_DOOR_Y = 12;
	public static final int ENTRANCE_X = 20, ENTRANCE_Y = 69;

	// # wall, . ordinary floor, : special floor, g gold, p pedestal,
	// c carpet, s statue, o column, D wide gate, d treasure door.
	private static final String[] MAP = {
		"########################################", // 0
		"##################ggggg#################", // 1
		"#################ggggggg################", // 2
		"################ggpgggpgg###############", // 3
		"###############ggggggggggg##############", // 4
		"###############ggggggggggg##############", // 5
		"###############gpgggpgggpg##############", // 6
		"###############ggggggggggg##############", // 7
		"###############ggggggggggg##############", // 8
		"################ggpgggpgg###############", // 9
		"#################ggggggg################", // 10
		"##################ggggg#################", // 11
		"####################d###################", // 12
		"################:::::::::###############", // 13
		"#############:::::::::::::::############", // 14
		"###########:::::::::::::::::::##########", // 15
		"##########:::::::::::::::::::::#########", // 16
		"#########:::::::::::::::::::::::########", // 17
		"########:::::::::::::::::::::::::#######", // 18
		"#######:::::::::::::::::::::::::::######", // 19
		"######:::::::::::::::::::::::::::::#####", // 20
		"#####:::::::::::::::::::::::::::::::####", // 21
		"#####:::::::::::::::::::::::::::::::####", // 22
		"####:::::::::::::::::::::::::::::::::###", // 23
		"####:::::::::::::::::::::::::::::::::###", // 24
		"####:::::::::::::::::::::::::::::::::###", // 25
		"###::::::::::::::#:::::#::::::::::::::##", // 26
		"###:::::::::::::::::::::::::::::::::::##", // 27
		"###::::::::::::::::...::::::::::::::::##", // 28
		"###:::::::::::::::.....:::::::::::::::##", // 29
		"###::::::::::::::.......::::::::::::::##", // 30
		"###:::::::::::::.........:::::::::::::##", // 31
		"###:::::::::::::.........:::::::::::::##", // 32
		"###::::::::::::#.........#::::::::::::##", // 33
		"###::::::::::::...........::::::::::::##", // 34
		"###::::::::::::...........::::::::::::##", // 35
		"###::::::::::::...........::::::::::::##", // 36
		"###::::::::::::...........::::::::::::##", // 37
		"####:::::::::::...........:::::::::::###", // 38
		"#####::::::::::...........::::::::::####", // 39
		"######::::::::.#.........#.::::::::#####", // 40
		"#######::::::...............::::::######", // 41
		"########::::.................::::#######", // 42
		"#########.......................########", // 43
		"###################DDD##################", // 44
		"#########..........ccc..........########", // 45
		"#########.....#....ccc....#.....########", // 46
		"#########.......s..ccc..s.......########", // 47
		"#########.....#....ccc....#.....########", // 48
		"#########.......s..ccc..s.......########", // 49
		"#########.....#....ccc....#.....########", // 50
		"#########.......s..ccc..s.......########", // 51
		"#########.....#....ccc....#.....########", // 52
		"#########.......s..ccc..s.......########", // 53
		"#########.....#....ccc....#.....########", // 54
		"#########.......s..ccc..s.......########", // 55
		"#########.....#....ccc....#.....########", // 56
		"#########.......s..ccc..s.......########", // 57
		"#########.....#....ccc....#.....########", // 58
		"#########.......s..ccc..s.......########", // 59
		"#########.....#....ccc....#.....########", // 60
		"#########.......s..ccc..s.......########", // 61
		"#########.....#....ccc....#.....########", // 62
		"#########.......s..ccc..s.......########", // 63
		"#########.....#....ccc....#.....########", // 64
		"#########.......s..ccc..s.......########", // 65
		"#########..........ccc..........########", // 66
		"#################.occco.################", // 67
		"#################.ccccc.################", // 68
		"#################.ccccc.################", // 69
		"#################.ccccc.################", // 70
		"#################.occco.################", // 71
		"##################.....#################", // 72
		"########################################", // 73
	};

	public static char tile(int x, int y) {
		if (x < 0 || y < 0 || x >= WIDTH || y >= HEIGHT) return '#';
		return MAP[y].charAt(x);
	}

	public static void paint(Level level) {
		level.setSize(WIDTH, HEIGHT);
		ArrayList<Integer> treasureSpots = new ArrayList<>();
		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
				int cell = x + y * WIDTH;
				switch (tile(x, y)) {
					case '#': level.map[cell] = Terrain.WALL; break;
					case ':': case 'g': level.map[cell] = Terrain.EMPTY_SP; break;
					case 'p':
						level.map[cell] = Terrain.PEDESTAL;
						treasureSpots.add(cell);
						break;
					case 'c': level.map[cell] = Terrain.CUSTOM_DECO_EMPTY; break;
					case 's': level.map[cell] = Terrain.STATUE; break;
					case 'o': level.map[cell] = Terrain.REGION_DECO; break;
					case 'D': level.map[cell] = Terrain.CUSTOM_DECO; break;
					case 'd': level.map[cell] = Terrain.LOCKED_DOOR; break;
					default:
						level.map[cell] = Random.Int(10) == 0 ? Terrain.EMPTY_DECO : Terrain.EMPTY;
						break;
				}
			}
		}

		// 雕像固定在宝库中央的台座，其余奖励随机分配到外围台座。
		int statueCell = 20 + 6 * WIDTH;
		treasureSpots.remove(Integer.valueOf(statueCell));
		level.drop(new ImpStatue(), statueCell);
		Random.shuffle(treasureSpots);
		for (Item reward : Imp.Quest.rewardOptions) {
			level.drop(reward, treasureSpots.remove(0));
		}
		Imp.Quest.rewardOptions.clear();

		VaultTiles.GoldFloor gold = new VaultTiles.GoldFloor();
		gold.tileData(); // Choose variants during level generation, not on the render thread.
		level.customTiles.add(gold);
		level.customTiles.add(new VaultTiles.PlainFloor());
		level.customTiles.add(new VaultTiles.EntranceCarpet());
		int[][] circles = {{20, 19}, {11, 25}, {29, 25}, {8, 35}, {32, 35}};
		for (int[] circle : circles) {
			VaultFinalRoom.MarkerTiles marker = new VaultFinalRoom.MarkerTiles();
			marker.pos(circle[0] - 4, circle[1] - 4);
			level.customTiles.add(marker);
		}
		VaultTiles.EntranceRug entrance = new VaultTiles.EntranceRug();
		level.customTiles.add(entrance);
		level.customTerrain.add(new VaultTiles.EntranceDecorations());
		VaultTokenDoor gate = new VaultTokenDoor();
		gate.doorWidth = GATE_WIDTH;
		gate.pos = GATE_X + 1 + GATE_Y * WIDTH;
		level.mobs.add(gate);
		VaultTiles.Gate smallDoor = new VaultTiles.Gate();
		smallDoor.setRect(TREASURE_DOOR_X, TREASURE_DOOR_Y - 1, 1, 2);
		level.customTerrain.add(smallDoor);

		level.map[ENTRANCE_X + ENTRANCE_Y * WIDTH] = Terrain.ENTRANCE;
		level.transitions.add(new LevelTransition(level, ENTRANCE_X + ENTRANCE_Y * WIDTH,
				LevelTransition.Type.BRANCH_ENTRANCE, Dungeon.depth, 0, LevelTransition.Type.BRANCH_EXIT));
	}
}
