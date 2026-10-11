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

package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultTokenDoor;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.EscapeCrystal;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultMobPool;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.MagicPocket;
import com.shatteredpixel.shatteredpixeldungeon.items.Torch;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfInvisibility;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfPurity;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfLullaby;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRage;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRetribution;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTransmutation;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlink;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfEnchantment;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorruption;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfPetrification;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfTransfusion;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.CityPainter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.vault.VaultFinalRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.vault.VaultLayout;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.vault.VaultCorridorPatrols;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.EmptyRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.entrance.EntranceRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Callback;
import com.watabou.utils.Point;
import com.watabou.utils.Random;
import com.watabou.noosa.Group;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Arrays;

public class VaultLevel extends CityLevel {

	@Override
	public void playLevelMusic() {
		Music.INSTANCE.play(Assets.Music.CITY_TENSE, true);
	}

	@Override
	protected boolean build() {
		itemsToSpawn.clear();
		addItemToSpawn(Reflection.newInstance(Random.oneOf(PotionOfParalyticGas.class, PotionOfToxicGas.class)));
		addItemToSpawn(Reflection.newInstance(Random.oneOf(PotionOfPurity.class, PotionOfInvisibility.class)));
		addItemToSpawn(new PotionOfHealing());
		addItemToSpawn(new PotionOfHealing());
		addItemToSpawn(new PotionOfHaste());
		addItemToSpawn(Reflection.newInstance(Random.oneOf(ScrollOfTransmutation.class, ScrollOfRetribution.class)));
		addItemToSpawn(Reflection.newInstance(Random.oneOf(ScrollOfRage.class, ScrollOfLullaby.class)));
		ArrayList<Class<?>> stoneTypes = new ArrayList<>(Arrays.asList(Generator.Category.STONE.classes));
		stoneTypes.remove(StoneOfEnchantment.class);
		for (int i = 0; i < 3; i++) {
			addItemToSpawn((Item) Reflection.newInstance(Random.element(stoneTypes)));
		}

		rooms = new ArrayList<>();
		roomEntrance = new EntranceRoom();
		roomEntrance.set(8, 44, 32, 73);
		rooms.add(roomEntrance);
		VaultFinalRoom arena = new VaultFinalRoom();
		arena.set(2, 12, 38, 44);
		arena.configureFixedDoors(new Point(VaultLayout.GATE_X + 1, VaultLayout.GATE_Y),
				new Point(VaultLayout.TREASURE_DOOR_X, VaultLayout.TREASURE_DOOR_Y), VaultLayout.GATE_WIDTH);
		roomExit = arena;
		rooms.add(arena);
		Room treasure = new EmptyRoom();
		treasure.set(14, 0, 26, 12);
		rooms.add(treasure);
		VaultLayout.paint(this);
		new CityPainter() {
			@Override
			public boolean paint(Level level, ArrayList<Room> rooms) {
				EmptyRoom corridor = new EmptyRoom();
				corridor.set(roomEntrance.left, VaultCorridorPatrols.TOP, roomEntrance.right,
						VaultCorridorPatrols.TOP + 2 * VaultCorridorPatrols.SEGMENT_LENGTH - 1);
				ArrayList<Room> corridorRooms = new ArrayList<>();
				corridorRooms.add(corridor);
				//按普通房间的顺序先生成水草，再装饰剩余地面。
				Random.pushGenerator(Random.Long());
				try {
					for (Point p : corridor.waterPlaceablePoints()) {
						int cell = level.pointToCell(p);
						if (level.map[cell] == Terrain.EMPTY_DECO) level.map[cell] = Terrain.EMPTY;
					}
					paintWater(level, corridorRooms);
					paintGrass(level, corridorRooms);
					for (Point p : corridor.waterPlaceablePoints()) {
						int cell = level.pointToCell(p);
						if (level.map[cell] == Terrain.EMPTY && Random.Int(10) == 0) {
							level.map[cell] = Terrain.EMPTY_DECO;
						}
					}
				} finally {
					Random.popGenerator();
				}
				return true;
			}
		}.setWater(feeling == Feeling.WATER ? 0.90f : 0.30f, 4)
				.setGrass(feeling == Feeling.GRASS ? 0.80f : 0.20f, 3).paint(this, null);
		return true;
	}

	@Override
	public float levelExplorePercent(int depth) {
		// Each 1% of tiles seen = 1.25% explored. 80% seen = 100% explored
		int seen = 0, total = 0;
		for (int i = 0; i < length; i++){
			if (discoverable[i]) total++;
			if (visited[i]) seen++;
		}
		return Math.min(1, (seen*1.25f)/total);
	}

	@Override
	public boolean activateTransition(Hero hero, LevelTransition transition) {
		if (transition.type == LevelTransition.Type.BRANCH_ENTRANCE) {
			boolean gateClosed = false;
			for (Mob mob : mobs) {
				if (mob instanceof VaultTokenDoor && !((VaultTokenDoor) mob).battleSeal) gateClosed = true;
			}
			if (locked || gateClosed) {
				String message = Messages.get(EscapeCrystal.class, locked ? "blocked_battle" : "blocked_gate");
				hero.interrupt();
				GLog.n(message);
				return false;
			}
			MagicPocket pocket = hero.belongings.getItem(MagicPocket.class);
			if (pocket != null) {
				pocket.requestReturn(hero, this, () -> finishVaultExit(hero, transition));
				return false;
			}
			return finishVaultExit(hero, transition);
		}
		return super.activateTransition(hero, transition);
	}

	public boolean finishVaultExit(Hero hero, LevelTransition transition) {
		if (locked || hero.belongings.getItem(MagicPocket.class) != null) return false;
		EscapeCrystal crystal = hero.belongings.getItem(EscapeCrystal.class);
		if (crystal != null) crystal.detachAll(hero.belongings.backpack);
		hero.HP = hero.HT;
		Buff.affect(hero, Hunger.class).satisfy(Hunger.STARVING);
		return super.activateTransition(hero, transition);
	}

	@Override
	public Mob createMob() {
		return VaultMobPool.random();
	}

	@Override
	protected void createMobs() {
		VaultCorridorPatrols.createMobs(this);
	}

	@Override
	public void occupyCell(Char ch) {
		super.occupyCell(ch);
		if (ch == Dungeon.hero) {
			Room r = room(ch.pos);
			if (r instanceof VaultFinalRoom) {
				((VaultFinalRoom) r).processHeroStep((Hero) ch);
			}
		}

	}

	public Actor addRespawner() {
		return null;
	}

	@Override
	protected void createItems() {
		for (Item item : itemsToSpawn) {
			item.markVaultLoot();
			drop(item, randomDropCell()).type = Heap.Type.HEAP;
		}

		// 八堆骷髅各放一份奖励，四种类型各两份。
		for (int i = 0; i < 8; i++) {
			Item reward;
			switch (i / 2) {
				case 0:
					do {
						reward = Generator.randomUsingDefaults(Generator.Category.WAND);
					} while (reward instanceof WandOfCorruption || reward instanceof WandOfTransfusion
							|| reward instanceof WandOfRegrowth || reward instanceof WandOfPetrification);
					break;
				case 1:
					reward = Generator.randomUsingDefaults(Generator.Category.RING);
					break;
				case 2:
					reward = Generator.randomUsingDefaults(Random.oneOf(Generator.Category.WEP_T4, Generator.Category.WEP_T5));
					break;
				default:
					reward = Generator.randomUsingDefaults(Random.oneOf(Generator.Category.MIS_T4, Generator.Category.MIS_T5));
					break;
			}
			reward.level(Random.IntRange(2, 3));
			reward.cursed = false;
			if (reward instanceof Weapon && ((Weapon) reward).hasCurseEnchant()) {
				((Weapon) reward).enchant(null);
			}
			if (reward instanceof Wand) {
				((Wand) reward).curCharges = ((Wand) reward).maxCharges;
			}
			reward.identify(false);
			reward.markVaultLoot();
			drop(reward, randomDropCell()).type = Heap.Type.SKELETON;
		}

		// 入口补给与走廊散落物资分开放置，只在生成地图时给予一次。
		Item blink = new StoneOfBlink().quantity(2);
		blink.markVaultLoot();
		drop(blink, VaultLayout.ENTRANCE_X - 1 + (VaultLayout.ENTRANCE_Y + 1) * width());
		Item food = new Food();
		food.markVaultLoot();
		drop(food, VaultLayout.ENTRANCE_X + 1 + (VaultLayout.ENTRANCE_Y + 1) * width());
		if (Dungeon.isChallenged(Challenges.DARKNESS)) {
			Item torches = new Torch().quantity(2);
			torches.markVaultLoot();
			drop(torches, VaultLayout.ENTRANCE_X + (VaultLayout.ENTRANCE_Y + 2) * width());
		}
	}

	public boolean customDoorCell(int cell) {
		int x = cell % width(), y = cell / width();
		return x == VaultLayout.TREASURE_DOOR_X && y == VaultLayout.TREASURE_DOOR_Y;
	}

	public boolean customStatueCell(int cell) {
		return VaultLayout.tile(cell % width(), cell / width()) == 's'
				&& (map[cell] == Terrain.STATUE || map[cell] == Terrain.STATUE_EMBERS);
	}

	@Override
	public String tileName(int tile) {
		if (tile == Terrain.REGION_DECO || tile == Terrain.REGION_DECO_ALT) return Messages.get(CityLevel.class, "region_deco_name");
		return super.tileName(tile);
	}

	@Override
	public String tileDesc(int tile) {
		if (tile == Terrain.STATUE || tile == Terrain.STATUE_SP || tile == Terrain.STATUE_EMBERS || tile == Terrain.STATUE_SP_EMBERS) {
			return Messages.get(CityLevel.class, "statue_desc");
		}
		if (tile == Terrain.REGION_DECO || tile == Terrain.REGION_DECO_ALT) return Messages.get(CityLevel.class, "region_deco_desc");
		return super.tileDesc(tile);
	}

	@Override
	public Group addWallVisuals() {
		super.addWallVisuals();
		CityLevel.addCityWallVisuals(this, wallVisuals);
		return wallVisuals;
	}

	@Override
	protected int randomDropCell() {
		ArrayList<Integer> candidates = new ArrayList<>();
		for (int y = 45; y <= 66; y++) {
			for (int x = 9; x <= 31; x++) {
				int cell = x + y * width();
				if (passable[cell] && !solid[cell] && heaps.get(cell) == null && findMob(cell) == null) {
					candidates.add(cell);
				}
			}
		}
		return candidates.isEmpty() ? -1 : Random.element(candidates);
	}

	/** The three cells share one gate, including occupancy checks before it closes. */
	@Override
	public void seal() {
		if (!locked) {
			locked = true;
			//don't apply locked floor buff here
			// Keep regeneration; battle seals still block every vault exit.
		}
	}

	@Override
	public void unseal() {
		super.unseal();
		for (Room r : rooms){
			if (r instanceof VaultFinalRoom){
				((VaultFinalRoom) r).unlock();
			}
		}
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				Music.INSTANCE.fadeOut(5f, new Callback() {
					@Override
					public void call() {
						Music.INSTANCE.end();
					}
				});
			}
		});
	}

}
