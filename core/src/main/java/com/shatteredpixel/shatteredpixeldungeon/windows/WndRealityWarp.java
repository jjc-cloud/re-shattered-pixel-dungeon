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
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
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

//扭曲现实的选品窗口：从已知的药水/卷轴中选择两种互换效果
public class WndRealityWarp extends Window {

	public enum Mode {
		POTION, SCROLL
	}

	private static final int WIDTH      = 120;
	private static final int BTN_HEIGHT = 18;
	private static final int SLOT_SIZE  = 24;
	private static final int GRID_SLOT  = 20;

	private final Mode mode;
	private final Item[] chosen = new Item[2];
	private final ItemButton[] slots = new ItemButton[2];
	private final RedButton btnConfirm;

	public WndRealityWarp(Mode mode) {
		super();
		this.mode = mode;

		boolean potionMode = mode == Mode.POTION;

		IconTitle title = new IconTitle(new ItemSprite(potionMode ? ItemSpriteSheet.POTION_HOLDER
				: ItemSpriteSheet.SCROLL_HOLDER), Messages.get(this, "title"));
		title.setRect(0, 0, WIDTH, 0);
		add(title);

		RenderedTextBlock prompt = PixelScene.renderTextBlock(
				Messages.get(this, potionMode ? "prompt_potion" : "prompt_scroll"), 6);
		prompt.maxWidth(WIDTH);
		prompt.setPos(0, title.bottom() + 2);
		add(prompt);

		float pos = prompt.bottom() + 2;
		float margin = (WIDTH - SLOT_SIZE * 2) / 3f;
		for (int i = 0; i < 2; i++) {
			final int idx = i;
			slots[i] = new ItemButton() {
				@Override
				protected void onClick() {
					if (chosen[idx] != null) {
						setSlot(idx, null);
					}
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

		Component grid = new Component();
		grid.setRect(0, pos, WIDTH, 0);
		add(grid);

		ArrayList<Item> candidates = potionMode ? potionCandidates() : scrollCandidates();
		int left = 0;
		int top = 0;
		for (final Item cand : candidates) {
			ItemButton button = new ItemButton() {
				@Override
				protected void onClick() {
					Integer at = slotIndexOf(cand);
					if (at != null) {
						setSlot(at, null);
					} else if (chosen[0] == null) {
						setSlot(0, cand);
					} else if (chosen[1] == null) {
						setSlot(1, cand);
					} else {
						setSlot(0, cand);
					}
				}

				@Override
				protected boolean onLongClick() {
					GameScene.show(new WndInfoItem(cand));
					return true;
				}
			};
			button.item(cand);
			button.setRect(grid.left() + left, grid.top() + top, 19, 19);
			grid.add(button);
			left += GRID_SLOT;
			if (left > WIDTH - 19) {
				top += GRID_SLOT;
				left = 0;
			}
		}
		pos += top + GRID_SLOT + 2;

		btnConfirm = new RedButton(Messages.get(this, "confirm")) {
			@Override
			@SuppressWarnings("unchecked")
			protected void onClick() {
				if (chosen[0] == null || chosen[1] == null) return;
				if (mode == Mode.POTION) {
					RealityWarp.setPotionSwap((Class<? extends Potion>) chosen[0].getClass(),
							(Class<? extends Potion>) chosen[1].getClass());
					GLog.i(Messages.get(WndRealityWarp.class, "potion_swapped",
							chosen[0].name(), chosen[1].name()));
				} else {
					RealityWarp.setScrollSwap((Class<? extends Scroll>) chosen[0].getClass(),
							(Class<? extends Scroll>) chosen[1].getClass());
					GLog.i(Messages.get(WndRealityWarp.class, "scroll_swapped",
							chosen[0].name(), chosen[1].name()));
				}
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

	//已知的药水，力量药剂无法被选择
	@SuppressWarnings("unchecked")
	private ArrayList<Item> potionCandidates() {
		ArrayList<Item> result = new ArrayList<>();
		for (Class<?> cls : Generator.Category.POTION.classes) {
			if (cls == PotionOfStrength.class) continue;
			Potion item = Reflection.newInstance((Class<Potion>) cls);
			if (item.isKnown()) result.add(item);
		}
		return result;
	}

	//已知的卷轴，升级卷轴无法被选择
	@SuppressWarnings("unchecked")
	private ArrayList<Item> scrollCandidates() {
		ArrayList<Item> result = new ArrayList<>();
		for (Class<?> cls : Generator.Category.SCROLL.classes) {
			if (cls == ScrollOfUpgrade.class) continue;
			Scroll item = Reflection.newInstance((Class<Scroll>) cls);
			if (item.isKnown()) result.add(item);
		}
		return result;
	}

	private Integer slotIndexOf(Item item) {
		for (int i = 0; i < 2; i++) {
			if (chosen[i] == item) return i;
		}
		return null;
	}

	private void setSlot(int idx, Item item) {
		chosen[idx] = item;
		slots[idx].item(item);
		btnConfirm.enable(chosen[0] != null && chosen[1] != null);
	}
}
