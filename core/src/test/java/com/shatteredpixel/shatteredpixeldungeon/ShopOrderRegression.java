package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.ShopOrder;
import com.shatteredpixel.shatteredpixeldungeon.items.Stylus;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.ShopRoom;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class ShopOrderRegression {

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}

	public static void main(String[] args) {
		WarriorTalentsRegression.setupHeadless();
		Bundle emptyState = new Bundle();
		emptyState.put("pending_items", new ArrayList<Item>());
		ShopOrder.restoreFromBundle(emptyState);

		Dungeon.depth = 6;
		check(ShopOrder.onShopGenerated().isEmpty(), "empty shop has no delivery");
		check(ShopOrder.canOrder(), "first shop can place an order");

		Item first = new Bomb();
		Item second = new Stylus();
		ShopOrder.place(new ArrayList<>(Arrays.asList(first, second)));
		check(ShopOrder.pendingCount() == 2, "pending count uses the stored order");

		Dungeon.depth = 11;
		ArrayList<Item> delivery = ShopOrder.onShopGenerated();
		check(delivery.size() == 2 && delivery.get(0) == first && delivery.get(1) == second,
				"next shop receives the original order through its item list");
		check(ShopOrder.pendingCount() == 0, "delivery clears pending items");
		check(first.shopOrderTag != 0 && first.shopOrderTag == second.shopOrderTag,
				"delivered items retain their shared order tag");
		check(Shopkeeper.sellPrice(first) == first.value() * 5 * (Dungeon.depth / 5 + 1) * 3 / 2,
				"delivered item keeps the order price multiplier");
		check(!ShopOrder.canOrder(), "unfinished delivery blocks another order");

		ShopOrder.notifyPurchased(first);
		ShopOrder.notifyPurchased(second);
		check(ShopOrder.canOrder(), "fully purchased delivery reopens ordering");

		ShopOrder.place(new ArrayList<>(Arrays.asList(new Bomb())));
		Dungeon.depth = 16;
		ArrayList<Item> cityDelivery = ShopOrder.onShopGenerated();
		ShopOrder.notifyPurchased(cityDelivery.get(0));
		check(ShopOrder.canOrder(), "completed prison and caves orders unlock the city order");

		Dungeon.depth = 20;
		ShopOrder.onShopGenerated();
		check(!ShopOrder.canOrder(), "a shop beyond the existing equipment tiers cannot order");

		Level level = Dungeon.level;
		level.setSize(9, 9);
		Arrays.fill(level.map, Terrain.EMPTY_SP);
		level.mobs = new HashSet<>();
		level.blobs = new java.util.HashMap<>();
		level.heaps = new com.watabou.utils.SparseArray<>();
		level.plants = new com.watabou.utils.SparseArray<>();
		level.traps = new com.watabou.utils.SparseArray<>();
		level.buildFlagMaps();
		TestShopRoom room = new TestShopRoom();
		room.set(1, 1, 7, 7);
		int keeperPos = 4 + 4 * level.width();
		ArrayList<Integer> orderCells = room.orderCells(level, keeperPos);
		check(orderCells.size() >= 3, "shopkeeper has enough cells for the maximum order");
		for (int cell : orderCells) {
			check(level.adjacent(keeperPos, cell), "ordered item is adjacent to the shopkeeper");
			com.watabou.utils.Point point = level.cellToPoint(cell);
			check(point.x > room.left && point.x < room.right
					&& point.y > room.top && point.y < room.bottom,
					"ordered item is not placed on the outer room edge");
		}

		System.out.println("PASS: shop orders reuse known item and shop tier interfaces");
	}

	public static class TestShopRoom extends ShopRoom {
		public ArrayList<Integer> orderCells(Level level, int keeperPos) {
			shopkeeperPos = keeperPos;
			return orderedItemCells(level);
		}
	}
}
