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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

public class WarriorsShortsword extends WornShortsword {

	public static final String AC_REFORGE = "REFORGE";

	private static final float TIME_TO_REFORGE = 2f;

	{
		image = ItemSpriteSheet.WARRIORS_SHORTSWORD;

		unique = true;
		bones = false;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (trueLevel() > 0) {
			actions.add(AC_REFORGE);
		}
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		if (action.equals(AC_REFORGE)) {
			curUser = hero;
			GameScene.selectItem(itemSelector);
		} else {
			super.execute(hero, action);
		}
	}

	@Override
	public boolean isUpgradable() {
		return trueLevel() < 1;
	}

	boolean canReforge(Item item) {
		return item instanceof MeleeWeapon && item != this && item.isUpgradable();
	}

	boolean reforge(Hero hero, MeleeWeapon weapon) {
		if (trueLevel() <= 0 || !canReforge(weapon)
				|| (!isEquipped(hero) && !hero.belongings.contains(this))) {
			return false;
		}

		if (hero.belongings.weapon == this) {
			hero.belongings.weapon = null;
		} else if (hero.belongings.secondWep == this) {
			hero.belongings.secondWep = null;
		} else {
			detachAll(hero.belongings.backpack);
		}
		Dungeon.quickslot.clearItem(this);

		if (weapon.hasGoodEnchant()) {
			weapon.upgrade(true);
		} else {
			weapon.upgrade();
		}
		Badges.validateItemLevelAquired(weapon);
		Item.updateQuickslot();
		return true;
	}

	private final WndBag.ItemSelector itemSelector = new WndBag.ItemSelector() {

		@Override
		public String textPrompt() {
			return Messages.get(WarriorsShortsword.class, "reforge_prompt");
		}

		@Override
		public Class<? extends Bag> preferredBag() {
			return Belongings.Backpack.class;
		}

		@Override
		public boolean itemSelectable(Item item) {
			return canReforge(item);
		}

		@Override
		public void onSelect(Item item) {
			if (item instanceof MeleeWeapon && reforge(curUser, (MeleeWeapon)item)) {
				Sample.INSTANCE.play(Assets.Sounds.EVOKE);
				ScrollOfUpgrade.upgrade(curUser);
				evoke(curUser);
				GLog.p(Messages.get(WarriorsShortsword.class, "reforged", item.name()));
				curUser.spendAndNext(TIME_TO_REFORGE);
			}
		}
	};
}
