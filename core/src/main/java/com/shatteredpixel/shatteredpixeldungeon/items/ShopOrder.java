/*
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

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

/**
 * 商店订购：玩家在最新的一间普通商店（6/11/16层）预订至多三件商品，
 * 货物在订购之后生成的下一间商店门口备好，售价为标价的1.5倍。
 * 上一间商店的订购物品没买完时，之后不再提供订购；
 * 6层与11层的订单都全额购完后，16层也可订购，货物改放在小恶魔商店。
 */
public class ShopOrder {

	public static final int MAX_ITEMS = 3;

	private static int seq = 0;                     //订单号发号器
	private static ArrayList<Item> pending = new ArrayList<>(); //已下单待交付
	private static int pendingPlacedDepth;          //待交付订单的下单层
	private static int pendingSeq;                  //待交付订单的订单号

	private static int deliveredSeq;                //最近一笔已交付订单号，0=无
	private static int deliveredPlacedDepth;        //该订单的下单层
	private static int deliveredShopDepth;          //该订单的交付商店层
	private static int remaining;                   //该订单未购完件数

	private static ArrayList<Integer> completedPlacements = new ArrayList<>(); //全额购完订单的下单层
	private static boolean broken;                  //断链后本局永久关闭订购
	private static int lastShopDepth;               //最近一次实体化的商店层（含小恶魔商店）

	public static boolean canOrder() {
		if (broken) return false;
		if (nextShopTier() == -1) return false;            //没有下一间商店接单
		if (Dungeon.depth != lastShopDepth) return false;  //不是最新的商店
		if (!pending.isEmpty()) return false;              //已有待交付的订单
		//本店还摆放着上一间商店订购来的货物且没有买完
		if (deliveredSeq != 0 && deliveredShopDepth == Dungeon.depth && remaining > 0) return false;
		//16层订购要求6层与11层的订单都已全额购完
		if (Dungeon.depth == 16
				&& !(completedPlacements.contains(6) && completedPlacements.contains(11))) {
			return false;
		}
		return true;
	}

	//下一间商店出售的装备阶数（与 ShopRoom.generateItems 的深度映射保持一致），-1表示没有下一间商店
	public static int nextShopTier() {
		switch (Dungeon.depth) {
			case 6:
				return 3;  //11层商店出售三阶装备
			case 11:
				return 4;  //16层商店出售四阶装备
			case 16:
				return 5;  //小恶魔商店出售五阶装备
			default:
				return -1;
		}
	}

	public static void place(ArrayList<Item> items) {
		seq++;
		pending = new ArrayList<>(items);
		pendingPlacedDepth = Dungeon.depth;
		pendingSeq = seq;
		for (Item item : pending) {
			item.shopOrderTag = pendingSeq;
		}
	}

	//小恶魔商店是否正替商人保管着未购完的订购物品
	public static boolean impHoldingGoods() {
		return deliveredSeq != 0 && remaining > 0 && deliveredShopDepth == Dungeon.depth;
	}

	//商店实体化时调用（普通商店在 ShopRoom.paint，小恶魔商店在 ImpShopRoom.spawnShop）
	public static void onShopGenerated(Level level) {
		lastShopDepth = Dungeon.depth;

		Shopkeeper keeper = null;
		for (Mob mob : level.mobs) {
			if (mob instanceof Shopkeeper) {
				keeper = (Shopkeeper) mob;
				break;
			}
		}
		if (keeper == null) return;

		//生成下一间商店时上一笔订购物品没买完，之后不再提供订购
		if (deliveredSeq != 0 && remaining > 0) {
			broken = true;
		}

		if (pending.isEmpty()) return;

		ArrayList<Integer> freeCells = new ArrayList<>();
		for (int i : PathFinder.NEIGHBOURS8) {
			int cell = keeper.pos + i;
			//生成阶段 passable 数组尚未填充，改用地形标记判定
			if ((Terrain.flags[level.map[cell]] & Terrain.PASSABLE) != 0
					&& level.heaps.get(cell) == null
					&& Actor.findChar(cell) == null) {
				freeCells.add(cell);
			}
		}

		for (int i = 0; i < pending.size(); i++) {
			int cell;
			if (i < freeCells.size()) {
				cell = freeCells.get(i);
			} else {
				//老板周围没有更多空格时，并入周围已有的一格
				cell = keeper.pos + PathFinder.NEIGHBOURS8[i % PathFinder.NEIGHBOURS8.length];
			}
			level.drop(pending.get(i), cell).type = Heap.Type.FOR_SALE;
		}
		deliveredSeq = pendingSeq;
		deliveredPlacedDepth = pendingPlacedDepth;
		deliveredShopDepth = Dungeon.depth;
		remaining = pending.size();
		pending = new ArrayList<>();
	}

	//玩家从商店买走订购物品时回调
	public static void notifyPurchased(Item item) {
		if (deliveredSeq != 0 && item.shopOrderTag == deliveredSeq && remaining > 0) {
			remaining--;
			if (remaining == 0) {
				completedPlacements.add(deliveredPlacedDepth);
			}
		}
	}

	private static final String SEQ                = "order_seq";
	private static final String PENDING            = "pending_items";
	private static final String PENDING_DEPTH      = "pending_depth";
	private static final String PENDING_SEQ        = "pending_seq";
	private static final String DELIVERED_SEQ      = "delivered_seq";
	private static final String DELIVERED_PLACED   = "delivered_placed_depth";
	private static final String DELIVERED_SHOP     = "delivered_shop_depth";
	private static final String REMAINING          = "remaining";
	private static final String COMPLETED          = "completed_placements";
	private static final String BROKEN             = "broken";
	private static final String LAST_SHOP          = "last_shop_depth";

	public static void storeInBundle(Bundle bundle) {
		bundle.put(SEQ, seq);
		bundle.put(PENDING, pending);
		bundle.put(PENDING_DEPTH, pendingPlacedDepth);
		bundle.put(PENDING_SEQ, pendingSeq);
		bundle.put(DELIVERED_SEQ, deliveredSeq);
		bundle.put(DELIVERED_PLACED, deliveredPlacedDepth);
		bundle.put(DELIVERED_SHOP, deliveredShopDepth);
		bundle.put(REMAINING, remaining);
		int[] completed = new int[completedPlacements.size()];
		for (int i = 0; i < completed.length; i++) {
			completed[i] = completedPlacements.get(i);
		}
		bundle.put(COMPLETED, completed);
		bundle.put(BROKEN, broken);
		bundle.put(LAST_SHOP, lastShopDepth);
	}

	public static void restoreFromBundle(Bundle bundle) {
		seq = bundle.getInt(SEQ);
		pending = new ArrayList<>();
		for (Bundlable i : bundle.getCollection(PENDING)) {
			pending.add((Item) i);
		}
		pendingPlacedDepth = bundle.getInt(PENDING_DEPTH);
		pendingSeq = bundle.getInt(PENDING_SEQ);
		deliveredSeq = bundle.getInt(DELIVERED_SEQ);
		deliveredPlacedDepth = bundle.getInt(DELIVERED_PLACED);
		deliveredShopDepth = bundle.getInt(DELIVERED_SHOP);
		remaining = bundle.getInt(REMAINING);
		completedPlacements = new ArrayList<>();
		if (bundle.contains(COMPLETED)) {
			for (int depth : bundle.getIntArray(COMPLETED)) {
				completedPlacements.add(depth);
			}
		}
		broken = bundle.getBoolean(BROKEN);
		lastShopDepth = bundle.getInt(LAST_SHOP);
	}
}
