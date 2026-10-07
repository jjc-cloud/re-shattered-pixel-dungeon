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
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.VaultFlameTraps;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultTokenDoor;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.EscapeCrystal;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultMobPool;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.MagicPocket;
import com.shatteredpixel.shatteredpixeldungeon.items.Torch;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.MailArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ScaleArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfExperience;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfInvisibility;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLevitation;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfMindVision;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfPurity;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfForce;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfMight;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfWealth;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfLullaby;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMirrorImage;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRage;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRetribution;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTerror;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTransmutation;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfAggression;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfAugmentation;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlast;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlink;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfClairvoyance;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfDeepSleep;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfEnchantment;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfFear;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfFlock;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfShock;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorruption;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfPetrification;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfTransfusion;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.CityPainter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.vault.VaultEntranceRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.vault.VaultFinalRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.vault.VaultLayout;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.vault.VaultCorridorPatrols;
import com.shatteredpixel.shatteredpixeldungeon.tiles.custom.VaultTiles;
import com.shatteredpixel.shatteredpixeldungeon.tiles.custom.Carpet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.watabou.utils.Bundle;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.EmptyRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Blindweed;
import com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot;
import com.shatteredpixel.shatteredpixeldungeon.plants.Firebloom;
import com.shatteredpixel.shatteredpixeldungeon.plants.Icecap;
import com.shatteredpixel.shatteredpixeldungeon.plants.Mageroyal;
import com.shatteredpixel.shatteredpixeldungeon.plants.Sorrowmoss;
import com.shatteredpixel.shatteredpixeldungeon.plants.Starflower;
import com.shatteredpixel.shatteredpixeldungeon.plants.Stormvine;
import com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass;
import com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Callback;
import com.watabou.utils.Point;
import com.watabou.utils.Random;
import com.watabou.noosa.Group;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

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
		roomEntrance = new VaultEntranceRoom();
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
		if (isRegularQuest() && transition.type == LevelTransition.Type.BRANCH_ENTRANCE) {
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

	//only occurs in levelgen, no need to bundle these
	// use arrays here as we want to be able to track and access indices
	// this lets us guarantee an even distribution of loot
	// more specifically, every 6 items generated from T2/3 and T0/1 are guaranteed to be:
	// 2x melee weapon, 1x armor, 1x thrown weapon, 1x wand, 1x ring
	Item[][] equipmentLoot = new Item[4][];
	int higherTierIdx = 0;
	int lowerTierIdx = 0;

	public void setupEquipment(){
		for (int i = 0; i < equipmentLoot.length; i++){
			boolean empty = true;
			if (equipmentLoot[i] != null) {
				for (int j = 0; j < equipmentLoot[i].length; j++) {
					if (equipmentLoot[i][j] != null) {
						empty = false;
					}
				}
			}
			if (empty){
				setupEquipmentAtTier(i);
			}
		}
	}

	//cannot generate two of the same equipment item (except armor)
	// note that this does currently result in infinite loops if too many items are generated
	// currently we're designing around a theoretical max of about 12 genereated from each tier, which is almost 2x the actual max
	private HashSet<Class<? extends Item>> generatedClasses = new HashSet<>();

	public void setupEquipmentAtTier(int lootTier){

		ArrayList<Item> lootList = new ArrayList<>();

		Item loot;
		//first weapon (lower tier, more upgrades)
		do {
			switch (lootTier) {
				default:
				case 0:
					loot = Generator.randomUsingDefaults(Generator.Category.WEP_T2);
					break;
				case 1:
					loot = Generator.randomUsingDefaults(Generator.Category.WEP_T2);
					break;
				case 2:
					loot = Generator.randomUsingDefaults(Generator.Category.WEP_T3);
					break;
				case 3:
					loot = Generator.randomUsingDefaults(Generator.Category.WEP_T4);
					break;
			}
		//T2 weapon duplicates allowed, because so many can be generated
		} while (lootTier > 1 && generatedClasses.contains(loot.getClass()));
		generatedClasses.add(loot.getClass());
		if (lootTier == 0) { //always +0 at T0
			loot.level(lootTier);
		} else {
			loot.level(lootTier+1);
		}
		if (Random.Int(3) >= lootTier) {
			((Weapon) loot).enchant(null);
		} else {
			((Weapon) loot).enchant();
		}
		lootList.add(loot);

		//second weapon (higher tier, fewer upgrades)
		do {
			switch (lootTier) {
				default:
				case 0:
					loot = Generator.randomUsingDefaults(Generator.Category.WEP_T2);
					break;
				case 1:
					loot = Generator.randomUsingDefaults(Generator.Category.WEP_T3);
					break;
				case 2:
					loot = Generator.randomUsingDefaults(Generator.Category.WEP_T4);
					break;
				case 3:
					loot = Generator.randomUsingDefaults(Generator.Category.WEP_T5);
					break;
			}
		//T2 weapon duplicates allowed, because so many can be generated
		} while (lootTier > 0 && generatedClasses.contains(loot.getClass()));
		generatedClasses.add(loot.getClass());
		loot.level(lootTier);
		if (Random.Int(3) >= lootTier) {
			((Weapon) loot).enchant(null);
		} else {
			((Weapon) loot).enchant();
		}
		lootList.add(loot);

		//missile weapon (same level/tiering as 2nd weapon)
		do {
			switch (lootTier) {
				default:
				case 0:
					loot = Generator.randomUsingDefaults(Generator.Category.MIS_T2);
					break;
				case 1:
					loot = Generator.randomUsingDefaults(Generator.Category.MIS_T3);
					break;
				case 2:
					loot = Generator.randomUsingDefaults(Generator.Category.MIS_T4);
					break;
				case 3:
					loot = Generator.randomUsingDefaults(Generator.Category.MIS_T5);
					break;
			}
		} while (generatedClasses.contains(loot.getClass()));
		generatedClasses.add(loot.getClass());
		loot.level(lootTier);
		if (Random.Int(3) >= lootTier) {
			((Weapon) loot).enchant(null);
		} else {
			((Weapon) loot).enchant();
		}
		lootList.add(loot);

		//armor (same level/tiering as 2nd weapon)
		switch (lootTier) {
			default:
			case 0:
				loot = new LeatherArmor();
				break;
			case 1:
				loot = new MailArmor();
				break;
			case 2:
				loot = new ScaleArmor();
				break;
			case 3:
				loot = new PlateArmor();
				break;
		}
		//skip duplicate check, only 1 armor per tier atm anyway
		generatedClasses.add(loot.getClass());
		loot.level(lootTier);
		if (Random.Int(3) >= lootTier) {
			((Armor) loot).inscribe(null);
		} else {
			((Armor) loot).inscribe();
		}
		lootList.add(loot);

		//wand (some wands are banned)
		do {
			loot = Generator.randomUsingDefaults(Generator.Category.WAND);
		} while (generatedClasses.contains(loot.getClass()) || loot instanceof WandOfRegrowth
				|| loot instanceof WandOfTransfusion || loot instanceof WandOfCorruption);
		generatedClasses.add(loot.getClass());
		loot.level(lootTier);
		((Wand)loot).curCharges = ((Wand)loot).maxCharges;
		lootList.add(loot);

		//ring (some rings are banned)
		do {
			loot = Generator.randomUsingDefaults(Generator.Category.RING);
		} while (generatedClasses.contains(loot.getClass()) || loot instanceof RingOfWealth
				|| loot instanceof RingOfMight || loot instanceof RingOfForce);
		generatedClasses.add(loot.getClass());
		loot.level(lootTier);
		lootList.add(loot);

		equipmentLoot[lootTier] = lootList.toArray(new Item[0]);
	}

	public Item createEquipment(int lootTier) {

		//ensure we don't have any empty loot lists
		setupEquipment();

		int idx;
		if (lootTier >= 2){
			idx = higherTierIdx;
			if (idx >= equipmentLoot[lootTier].length){
				idx = 0;
				higherTierIdx = 0;
			} else {
				higherTierIdx++;
			}

		} else {
			idx = lowerTierIdx;
			if (idx >= equipmentLoot[lootTier].length){
				idx = 0;
				lowerTierIdx = 0;
			} else {
				lowerTierIdx++;
			}

		}

		while(equipmentLoot[lootTier][idx] ==  null){
			idx++;
			if (idx >= equipmentLoot[lootTier].length){
				idx = 0;
			}
		}

		Item loot = equipmentLoot[lootTier][idx];
		equipmentLoot[lootTier][idx] = null;

		loot.cursed = false;
		if (loot instanceof Ring){
			//rings in the vault get 20% of ID for each defeated enemy. See Mob.destroy()
			loot.levelKnown = loot.cursedKnown = true;
		} else {
			loot.identify(false);
		}

		return loot;
	}

	//only occurs in levelgen, no need to bundle these
	ArrayList<ArrayList<Item>> consumableLoot = new ArrayList<>();

	private void setupConsumables(){
		if (consumableLoot.isEmpty()) {
			consumableLoot.add(new ArrayList<>());
			consumableLoot.add(new ArrayList<>());
			consumableLoot.add(new ArrayList<>());
			consumableLoot.add(new ArrayList<>());
		}

		//T0, floor loot
		if (consumableLoot.get(0).isEmpty()){
			consumableLoot.get(0).addAll(Arrays.asList(
					Reflection.newInstance(Random.oneOf(PotionOfFrost.class, PotionOfLevitation.class)),
					Reflection.newInstance(Random.oneOf(Mageroyal.Seed.class, Icecap.Seed.class, Stormvine.Seed.class)),
					Reflection.newInstance(Random.oneOf(ScrollOfMirrorImage.class, ScrollOfTeleportation.class)),
					Reflection.newInstance(Random.oneOf(StoneOfFlock.class, StoneOfShock.class, StoneOfFear.class))));
			Collections.shuffle(consumableLoot.get(0));
			//first item in each tier is always a potion of healing (except T3, which has one randomly)
			consumableLoot.get(0).add(0, new PotionOfHealing());
		}

		//T1
		if (consumableLoot.get(1).isEmpty()){
			consumableLoot.get(1).addAll(Arrays.asList(
					Reflection.newInstance(Random.oneOf(PotionOfToxicGas.class, PotionOfParalyticGas.class)),
					Reflection.newInstance(Random.oneOf(Firebloom.Seed.class, Sorrowmoss.Seed.class, Blindweed.Seed.class)),
					Reflection.newInstance(Random.oneOf(ScrollOfRecharging.class, ScrollOfTerror.class)),
					Reflection.newInstance(Random.oneOf(StoneOfDeepSleep.class, StoneOfClairvoyance.class, StoneOfAggression.class))));
			Collections.shuffle(consumableLoot.get(1));
			consumableLoot.get(1).add(0, new PotionOfHealing());
		}

		//T2
		if (consumableLoot.get(2).isEmpty()){
			consumableLoot.get(2).addAll(Arrays.asList(
					Reflection.newInstance(Random.oneOf(PotionOfMindVision.class, PotionOfLiquidFlame.class)),
					Reflection.newInstance(Random.oneOf(Swiftthistle.Seed.class, Sungrass.Seed.class)),
					Reflection.newInstance(Random.oneOf(ScrollOfLullaby.class, ScrollOfMagicMapping.class)).random(),
					Reflection.newInstance(Random.oneOf(StoneOfBlast.class, StoneOfBlink.class))));
			Collections.shuffle(consumableLoot.get(2));
			consumableLoot.get(2).add(0, new PotionOfHealing());
		}

		//T3
		if (consumableLoot.get(3).isEmpty()){
			consumableLoot.get(3).addAll(Arrays.asList(
					Reflection.newInstance(Random.oneOf(PotionOfExperience.class, PotionOfInvisibility.class)),
					Reflection.newInstance(Random.oneOf(Earthroot.Seed.class, Starflower.Seed.class)),
					Reflection.newInstance(Random.oneOf(ScrollOfRetribution.class, ScrollOfTransmutation.class)),
					Reflection.newInstance(Random.oneOf(StoneOfEnchantment.class, StoneOfAugmentation.class)),
					new PotionOfHealing()));
			Collections.shuffle(consumableLoot.get(3));
		}
	}

	public Item createConsumabe(int tier){
		if (consumableLoot.isEmpty() || consumableLoot.get(tier).isEmpty()){
			setupConsumables();
		}
		Item result = consumableLoot.get(tier).remove(0);
		return result;
	}

	static Class<?extends Item>[] T3SolveItems = new Class[]{
			StoneOfBlink.class,
			PotionOfInvisibility.class
	};

	public Item findT3SolveItem(){
		Random.shuffle(T3SolveItems);
		Item result = null;
		for (Class<?extends Item> itemCls : T3SolveItems){
			result = findPrizeItem(itemCls);
			if (result != null){
				return result;
			}
		}
		return null;
	}

	static Class<?extends Item>[] T2SolveItems = new Class[]{
			StoneOfBlink.class,
			PotionOfInvisibility.class
	};

	public Item findT2SolveItem(){
		Random.shuffle(T2SolveItems);
		Item result = null;
		for (Class<?extends Item> itemCls : T2SolveItems){
			result = findPrizeItem(itemCls);
			if (result != null){
				return result;
			}
		}
		//if we can't find a T2 solve, try to place a T3 solve instead (instead of having it be floor loot)
		return findT3SolveItem();
	}

	public static Class<?extends Mob>[] T1Mobs = VaultMobPool.TIER_1;

	public static Class<?extends Mob>[] T2Mobs = VaultMobPool.TIER_2;

	public static Class<?extends Mob>[] T3Mobs = VaultMobPool.TIER_3;

	private ArrayList<Class<?extends Mob>> mobsToSpawn = new ArrayList<>();

	@Override
	public Mob createMob() {
		if (mobsToSpawn.isEmpty()){
			//rotation is 3 mobs at each tier
			Collections.addAll(mobsToSpawn, T1Mobs);
			mobsToSpawn.add(Random.oneOf(T1Mobs));
			Collections.addAll(mobsToSpawn, T2Mobs);
			Collections.addAll(mobsToSpawn, T3Mobs);
			mobsToSpawn.add(Random.oneOf(T3Mobs));
			Random.shuffle(mobsToSpawn);
		}
		Class<? extends Mob> cls = mobsToSpawn.remove(0);
		return VaultMobPool.create(cls);
	}

	//important to try and preserve mobs that can't spawn in a certain place (e.g. corridors)
	public void returnMob( Class<?extends Mob> cls){
		mobsToSpawn.add(0, cls);
	}

	@Override
	protected void createMobs() {
		if (fixedLayout()) VaultCorridorPatrols.createMobs(this);
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

	private boolean fixedLayout() {
		return width() == VaultLayout.WIDTH && height() == VaultLayout.HEIGHT
				&& transitions.size() == 1
				&& entrance() == VaultLayout.ENTRANCE_X + VaultLayout.ENTRANCE_Y * width();
	}

	public boolean customDoorCell(int cell) {
		if (!fixedLayout()) return false;
		int x = cell % width(), y = cell / width();
		return x == VaultLayout.TREASURE_DOOR_X && y == VaultLayout.TREASURE_DOOR_Y;
	}

	public boolean isRegularQuest() {
		return fixedLayout();
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		if (!isRegularQuest()) return;
		for (int cell = 0; cell < length(); cell++) {
			char layout = VaultLayout.tile(cell % width(), cell / width());
			if (map[cell] == Terrain.CUSTOM_DECO && (layout == 's' || layout == 'o')) {
				Level.set(cell, layout == 's' ? Terrain.STATUE : Terrain.REGION_DECO, this);
			}
			if (cell / width() == 72 && map[cell] == Terrain.CUSTOM_DECO_EMPTY) Level.set(cell, Terrain.EMPTY, this);
		}
		customTerrain.removeIf(tile -> tile instanceof VaultTiles.EntranceColumns);
		VaultTiles.EntranceCarpet carpet = null;
		boolean rug = false;
		for (CustomTilemap tile : customTiles) {
			if (tile instanceof VaultTiles.EntranceCarpet) carpet = (VaultTiles.EntranceCarpet) tile;
			if (tile instanceof VaultTiles.EntranceRug) rug = true;
		}
		if (carpet == null) {
			carpet = new VaultTiles.EntranceCarpet();
			for (int i = customTiles.size() - 1; i >= 0; i--) {
				CustomTilemap tile = customTiles.get(i);
				if (tile instanceof Carpet && tile.tileX == 18 && tile.tileY == 45
						&& tile.tileW == 5 && tile.tileH == 28) {
					Bundle pattern = new Bundle();
					tile.storeInBundle(pattern);
					carpet.restoreFromBundle(pattern);
					customTiles.remove(i);
				}
			}
			customTiles.add(0, carpet);
		}
		if (!rug) {
			customTiles.removeIf(tile -> tile instanceof VaultEntranceRoom.QuestEntranceInternal);
			customTiles.add(new VaultTiles.EntranceRug());
		}
	}

	public boolean customStatueCell(int cell) {
		return fixedLayout() && VaultLayout.tile(cell % width(), cell / width()) == 's'
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
	protected int staticEnvironmentalLightRadius(int cell) {
		return map[cell] == Terrain.REGION_DECO || map[cell] == Terrain.REGION_DECO_ALT ? 2 : super.staticEnvironmentalLightRadius(cell);
	}

	@Override
	public Group addWallVisuals() {
		super.addWallVisuals();
		CityLevel.addCityWallVisuals(this, wallVisuals);
		return wallVisuals;
	}

	@Override
	protected int randomDropCell() {
		if (!fixedLayout()) return super.randomDropCell();
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

	public static class VaultFlameTrap extends Trap {

		{
			color = BLACK;
			shape = DOTS;

			canBeHidden = false;
			active = false;
		}

		@Override
		public void activate() {
			//does nothing, this trap is just decoration and is always deactivated
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc");
		}

		public static void setupTrap(Level level, int cell, int initialCD, int afterTriggerCD, int triggers){
			VaultFlameTraps traps = Blob.seed(0, 0, VaultFlameTraps.class, level);
			traps.curCooldowns[cell] = initialCD;
			traps.afterTriggerCooldowns[cell] = afterTriggerCD;
			traps.triggersAfterCooldown[cell] = triggers;
			level.setTrap(new VaultLevel.VaultFlameTrap().reveal(), cell);
			Painter.set(level, cell, Terrain.INACTIVE_TRAP);
		}

	}

}
