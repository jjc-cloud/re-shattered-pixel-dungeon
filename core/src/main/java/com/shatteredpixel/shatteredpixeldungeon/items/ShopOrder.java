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
import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

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
	private static int stolen;                      //该订单被偷走件数；被偷不计购买，件数一直留在remaining里，remaining-stolen=店内剩余

	private static ArrayList<Integer> completedPlacements = new ArrayList<>(); //全额购完订单的下单层
	private static boolean broken;                  //断链后本局永久关闭订购
	private static int lastShopDepth;               //最近一次实体化的商店层（含小恶魔商店）
	private static boolean doneDialogPending;       //店里的订购物全部离店后，商人待说的那句话（只弹一次）
	private static boolean doneDialogStolen;        //待播的那句话是否为“有货物被偷、剩余买光”版本
	private static boolean impNoteShown;            //小恶魔“代管货物”的开场说明是否已播过（只播一次）

	public static boolean canOrder() {
		if (broken) return false;
		if (lastShopDepth == 0 || Dungeon.depth != lastShopDepth) return false; //不是最新的商店
		//下一地区已经超出常规装备阶层，也就没有后续商店可以接单
		if ((Dungeon.depth + 5) / 5 >= Generator.wepTiers.length) return false;
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

	public static void place(ArrayList<Item> items) {
		seq++;
		pending = new ArrayList<>(items);
		pendingPlacedDepth = Dungeon.depth;
		pendingSeq = seq;
		for (Item item : pending) {
			item.shopOrderTag = pendingSeq;
		}
	}

	//小恶魔商店是否正替商人保管着订购物品（店内还有实物，被偷走的不算）
	public static boolean impHoldingGoods() {
		return deliveredSeq != 0 && remaining > stolen && deliveredShopDepth == Dungeon.depth;
	}

	//取走小恶魔“代管货物”说明的播报标记：店内还有实物且从未播过时返回true（只播一次）
	public static boolean takeImpNote() {
		if (impHoldingGoods() && !impNoteShown) {
			impNoteShown = true;
			return true;
		}
		return false;
	}

	public static int pendingCount() {
		return pending.size();
	}

	//商店实体化时取走待交付货物；具体摆放继续交给商店房间处理
	public static ArrayList<Item> onShopGenerated() {
		lastShopDepth = Dungeon.depth;

		//生成下一间商店时上一笔订购物品没买完，之后不再提供订购
		if (deliveredSeq != 0 && remaining > 0) {
			broken = true;
		}

		if (pending.isEmpty()) return new ArrayList<>();

		ArrayList<Item> delivery = pending;
		deliveredSeq = pendingSeq;
		deliveredPlacedDepth = pendingPlacedDepth;
		deliveredShopDepth = Dungeon.depth;
		remaining = pending.size();
		stolen = 0;
		pending = new ArrayList<>();
		return delivery;
	}

	//玩家从商店买走订购物品时回调
	public static void notifyPurchased(Item item) {
		if (deliveredSeq != 0 && item.shopOrderTag == deliveredSeq && remaining > 0) {
			remaining--;
			if (remaining == 0) {
				completedPlacements.add(deliveredPlacedDepth);
				doneDialogPending = true;
				doneDialogStolen = false;
			//remaining减到与被偷件数持平，说明店里的订购物已一件不剩，也触发结算对话，但走“失窃”版本
			} else if (remaining == stolen) {
				doneDialogPending = true;
				doneDialogStolen = true;
			}
		}
	}

	//玩家偷走订购物品时回调；失窃不计入购买，也不推进订单链
	public static void notifyStolen(Item item) {
		if (deliveredSeq != 0 && item.shopOrderTag == deliveredSeq && remaining > 0) {
			stolen++;
			//偷走最后一件：店里的订购物同样一件不剩，也触发结算对话，走“失窃”版本
			if (remaining == stolen) {
				doneDialogPending = true;
				doneDialogStolen = true;
			}
		}
	}

	//取走“订单已买完”的待播对话标记，只返回一次true
	public static boolean takeDoneDialog() {
		if (doneDialogPending) {
			doneDialogPending = false;
			return true;
		}
		return false;
	}

	//刚触发的买完对话是否为“失窃后买光剩余”版本
	public static boolean doneDialogWasTheft() {
		return doneDialogStolen;
	}

	private static final String SEQ                = "order_seq";
	private static final String PENDING            = "pending_items";
	private static final String PENDING_DEPTH      = "pending_depth";
	private static final String PENDING_SEQ        = "pending_seq";
	private static final String DELIVERED_SEQ      = "delivered_seq";
	private static final String DELIVERED_PLACED   = "delivered_placed_depth";
	private static final String DELIVERED_SHOP     = "delivered_shop_depth";
	private static final String REMAINING          = "remaining";
	private static final String STOLEN             = "stolen";
	private static final String COMPLETED          = "completed_placements";
	private static final String BROKEN             = "broken";
	private static final String LAST_SHOP          = "last_shop_depth";
	private static final String DONE_DIALOG        = "done_dialog_pending";
	private static final String DONE_DIALOG_STOLEN = "done_dialog_stolen";
	private static final String IMP_NOTE           = "imp_note_shown";

	public static void storeInBundle(Bundle bundle) {
		bundle.put(SEQ, seq);
		bundle.put(PENDING, pending);
		bundle.put(PENDING_DEPTH, pendingPlacedDepth);
		bundle.put(PENDING_SEQ, pendingSeq);
		bundle.put(DELIVERED_SEQ, deliveredSeq);
		bundle.put(DELIVERED_PLACED, deliveredPlacedDepth);
		bundle.put(DELIVERED_SHOP, deliveredShopDepth);
		bundle.put(REMAINING, remaining);
		bundle.put(STOLEN, stolen);
		int[] completed = new int[completedPlacements.size()];
		for (int i = 0; i < completed.length; i++) {
			completed[i] = completedPlacements.get(i);
		}
		bundle.put(COMPLETED, completed);
		bundle.put(BROKEN, broken);
		bundle.put(LAST_SHOP, lastShopDepth);
		bundle.put(DONE_DIALOG, doneDialogPending);
		bundle.put(DONE_DIALOG_STOLEN, doneDialogStolen);
		bundle.put(IMP_NOTE, impNoteShown);
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
		stolen = bundle.getInt(STOLEN);
		completedPlacements = new ArrayList<>();
		if (bundle.contains(COMPLETED)) {
			for (int depth : bundle.getIntArray(COMPLETED)) {
				completedPlacements.add(depth);
			}
		}
		broken = bundle.getBoolean(BROKEN);
		lastShopDepth = bundle.getInt(LAST_SHOP);
		doneDialogPending = bundle.getBoolean(DONE_DIALOG);
		doneDialogStolen = bundle.getBoolean(DONE_DIALOG_STOLEN);
		impNoteShown = bundle.getBoolean(IMP_NOTE);
	}

	//新开一局时清空全部状态（Dungeon.init 调用），避免上一局状态残留
	public static void reset() {
		seq = 0;
		pending = new ArrayList<>();
		pendingPlacedDepth = 0;
		pendingSeq = 0;
		deliveredSeq = 0;
		deliveredPlacedDepth = 0;
		deliveredShopDepth = 0;
		remaining = 0;
		stolen = 0;
		completedPlacements = new ArrayList<>();
		broken = false;
		lastShopDepth = 0;
		doneDialogPending = false;
		doneDialogStolen = false;
		impNoteShown = false;
	}
}
