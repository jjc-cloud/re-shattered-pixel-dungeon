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

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Honeypot;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.ShopOrder;
import com.shatteredpixel.shatteredpixeldungeon.items.Stylus;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfAugmentation;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.ItemButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

//商店订购界面：三个商品槽位，点击槽位打开五类选品界面
public class WndShopOrder extends Window {

	private static final int WIDTH      = 120;
	private static final int BTN_HEIGHT = 18;
	private static final int SLOT_SIZE  = 24;

	private final Item[] chosen = new Item[ShopOrder.MAX_ITEMS];
	private final ItemButton[] slots = new ItemButton[ShopOrder.MAX_ITEMS];
	private RedButton btnConfirm;

	public WndShopOrder() {
		IconTitle title = new IconTitle(new ItemSprite(ItemSpriteSheet.GOLD), Messages.get(this, "title"));
		title.setRect(0, 0, WIDTH, 0);
		add(title);

		RenderedTextBlock prompt = PixelScene.renderTextBlock(Messages.get(this, "prompt"), 6);
		prompt.maxWidth(WIDTH);
		prompt.setPos(0, title.bottom() + 2);
		add(prompt);

		float pos = prompt.bottom() + 2;
		float margin = (WIDTH - SLOT_SIZE * ShopOrder.MAX_ITEMS) / (ShopOrder.MAX_ITEMS + 1f);
		for (int i = 0; i < ShopOrder.MAX_ITEMS; i++) {
			final int idx = i;
			slots[i] = new ItemButton() {
				@Override
				protected void onClick() {
					GameScene.show(new ItemPicker(idx));
				}

				@Override
				protected boolean onLongClick() {
					if (chosen[idx] != null) {
						GameScene.show(new WndInfoItem(chosen[idx]));
						return true;
					}
					return false;
				}
			};
			slots[i].setRect(margin + i * (SLOT_SIZE + margin), pos, SLOT_SIZE, SLOT_SIZE);
			add(slots[i]);
		}
		pos += SLOT_SIZE + 2;

		btnConfirm = new RedButton(Messages.get(this, "confirm")) {
			@Override
			protected void onClick() {
				ArrayList<Item> items = new ArrayList<>();
				for (Item item : chosen) {
					if (item != null) items.add(item);
				}
				ShopOrder.place(items);
				GLog.i(Messages.get(ShopOrder.class, "placed"));
				hide();
			}
		};
		btnConfirm.setRect(0, pos, WIDTH, BTN_HEIGHT);
		btnConfirm.enable(false);
		add(btnConfirm);
		pos += BTN_HEIGHT + 1;

		RedButton btnCancel = new RedButton(Messages.get(this, "cancel")) {
			@Override
			protected void onClick() {
				hide();
			}
		};
		btnCancel.setRect(0, pos, WIDTH, BTN_HEIGHT);
		add(btnCancel);
		pos += BTN_HEIGHT;

		resize(WIDTH, (int) pos);
	}

	private int countChosen() {
		int count = 0;
		for (Item item : chosen) {
			if (item != null) count++;
		}
		return count;
	}

	private void setSlot(int idx, Item item) {
		chosen[idx] = item;
		slots[idx].item(item);
		btnConfirm.enable(countChosen() > 0);
	}

	//五类商品：药剂、卷轴、投掷武器、武器、杂项
	@SuppressWarnings("unchecked")
	private ArrayList<Item> categoryItems(int category) {
		ArrayList<Item> result = new ArrayList<>();
		switch (category) {
			case 0: //药剂：仅展示已鉴定的种类
				for (Class<?> cls : Generator.Category.POTION.classes) {
					Potion item = Reflection.newInstance((Class<Potion>) cls);
					if (item.isKnown()) result.add(item);
				}
				break;
			case 1: //卷轴：仅展示已鉴定的种类
				for (Class<?> cls : Generator.Category.SCROLL.classes) {
					Scroll item = Reflection.newInstance((Class<Scroll>) cls);
					if (item.isKnown()) result.add(item);
				}
				break;
			case 2: //投掷武器：展示下一间商店售卖阶数的全部种类
			case 3: { //武器：展示下一间商店售卖阶数的全部种类
				int tier = ShopOrder.nextShopTier();
				if (tier != -1) {
					Generator.Category cat = Generator.Category.valueOf(
							(category == 2 ? "MIS_T" : "WEP_T") + tier);
					for (Class<?> cls : cat.classes) {
						Item item = Reflection.newInstance((Class<Item>) cls);
						if (item instanceof Weapon) {
							//与商店货架上的武器保持同等待遇
							((Weapon) item).enchant(null);
							item.cursed = false;
							item.level(0);
							item.identify(false);
						}
						result.add(item);
					}
				}
				break;
			}
			case 4: //杂项
				result.add(new Stylus());
				result.add(new Honeypot());
				result.add(new StoneOfAugmentation());
				result.add(new Bomb());
				break;
		}
		return result;
	}

	//选品界面，参考炼金场景的实验产品选择窗口
	private class ItemPicker extends Window {

		private static final int GRID_SLOT = 20;

		private final int slotIdx;
		private int selectedCategory;
		private final RedButton[] categories = new RedButton[5];
		private Component grid;
		private float gridTop;
		private int pickerHeight;

		ItemPicker(int slotIdx) {
			super();
			this.slotIdx = slotIdx;

			IconTitle title = new IconTitle(new ItemSprite(ItemSpriteSheet.GOLD),
					Messages.get(WndShopOrder.class, "pick_title"));
			title.setRect(0, 0, WIDTH, 0);
			add(title);

			RenderedTextBlock prompt = PixelScene.renderTextBlock(
					Messages.get(WndShopOrder.class, "pick_prompt"), 6);
			prompt.maxWidth(WIDTH);
			prompt.setPos(0, title.bottom() + 2);
			add(prompt);

			Item[] icons = {new PotionOfHealing(), new ScrollOfIdentify(), new Dart(),
					new WornShortsword(), new Bomb()};
			for (int i = 0; i < categories.length; i++) {
				final int category = i;
				categories[i] = new RedButton("") {
					@Override
					protected void onClick() {
						selectedCategory = category;
						rebuild();
					}

					@Override
					protected String hoverText() {
						return Messages.get(WndShopOrder.class, "category_" + category);
					}
				};
				categories[i].icon(new ItemSprite(icons[i]));
				categories[i].setRect(i * WIDTH / 5f, prompt.bottom() + 2, WIDTH / 5f, 18);
				add(categories[i]);
			}

			gridTop = categories[0].bottom() + 2;
			//绘制前先确认各分类最多需要几行，窗口按最大行数固定，切换分类时不再改变大小
			int maxRows = 1;
			for (int i = 0; i < categories.length; i++) {
				maxRows = Math.max(maxRows, rowsNeeded(categoryItems(i)));
			}
			pickerHeight = (int) (gridTop + (maxRows + 1) * GRID_SLOT);
			selectedCategory = 0;
			resize(WIDTH, pickerHeight);
			rebuild();
		}

		private int rowsNeeded(ArrayList<Item> items) {
			int rows = 1;
			int left = 0;
			for (int i = 0; i < items.size(); i++) {
				left += GRID_SLOT;
				if (left > WIDTH - 19) {
					rows++;
					left = 0;
				}
			}
			return rows;
		}

		private void rebuild() {
			if (grid != null) {
				//清空容器不会注销旧按钮的点击监听，重建前必须销毁
				grid.destroy();
				erase(grid);
			}
			grid = new Component();
			grid.setRect(0, gridTop, WIDTH, 0);
			add(grid);
			for (int i = 0; i < categories.length; i++) {
				categories[i].enable(i != selectedCategory);
			}

			ArrayList<Item> items = categoryItems(selectedCategory);
			int left = 0;
			int top = 0;
			for (final Item item : items) {
				ItemButton button = new ItemButton() {
					@Override
					protected void onClick() {
						setSlot(slotIdx, item);
						ItemPicker.this.hide();
					}

					@Override
					protected boolean onLongClick() {
						GameScene.show(new WndInfoItem(item));
						return true;
					}
				};
				button.item(item);
				button.setRect(grid.left() + left, grid.top() + top, 19, 19);
				grid.add(button);
				left += GRID_SLOT;
				if (left > WIDTH - 19) {
					top += GRID_SLOT;
					left = 0;
				}
			}
		}
	}
}
