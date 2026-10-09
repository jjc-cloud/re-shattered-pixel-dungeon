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

package com.shatteredpixel.shatteredpixeldungeon.items.bags;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultTokenDoor;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultBossElemental;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.EnergyCrystal;
import com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.EscapeCrystal;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.ImpStatue;
import com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.vault.VaultFinalRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

/** The Imp's loan bag keeps vault loot separate from the hero's belongings. */
public class MagicPocket extends Bag {

	{
		image = ItemSpriteSheet.MAGIC_POCKET;
		markPlayerOwned();
	}

	@Override
	public int capacity() { return 19; }

	@Override
	public boolean canHold(Item item) {
		return item != this && item.isVaultLoot() && super.canHold(item);
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = new ArrayList<>();
		actions.add(AC_OPEN);
		return actions;
	}

	@Override
	public void doDrop(Hero hero) {}

	@Override
	public void cast(Hero hero, int cell) {}

	public static MagicPocket issue(Hero hero) {
		MagicPocket pocket = hero.belongings.getItem(MagicPocket.class);
		if (pocket == null) {
			pocket = new MagicPocket();
			if (!pocket.collect(hero.belongings.backpack)) return null;
		}
		if (hero.belongings.getItem(EscapeCrystal.class) == null) {
			EscapeCrystal crystal = new EscapeCrystal();
			crystal.markPlayerOwned();
			if (!crystal.collect(hero.belongings.backpack)) hero.belongings.backpack.items.add(crystal);
			Item.updateQuickslot();
		}
		return pocket;
	}

	public static boolean isVault() {
		return Dungeon.branch == 1 && Dungeon.level instanceof VaultLevel
				&& ((VaultLevel) Dungeon.level).isRegularQuest();
	}

	/** Items with special pickup effects (including currency) must also stay in the bag. */
	public static boolean pickUp(Item item, Hero hero, int cell) {
		if (item.isVaultLoot() && isVault()) {
			MagicPocket pocket = hero.belongings.getItem(MagicPocket.class);
			if (pocket == null) return false;
			if (!item.collect(pocket)) {
				if (pocket.items.size() >= pocket.capacity()) {
					GLog.n(Messages.get(pocket, "full"));
				} else {
					GLog.n(Messages.capitalize(Messages.get(hero, "you_cant_have", item.name())));
				}
				return false;
			}
			GameScene.pickUp(item, cell);
			Sample.INSTANCE.play(Assets.Sounds.ITEM);
			hero.spendAndNext(item.pickupDelay());
			return true;
		}
		return item.doPickUp(hero, cell);
	}

	public int completionScore(VaultLevel level) {
		if (level.locked) return 0;
		for (Char mob : level.mobs) {
			if (mob instanceof VaultTokenDoor && !((VaultTokenDoor) mob).battleSeal) return 0;
		}
		VaultFinalRoom room = (VaultFinalRoom) level.room(VaultFinalRoom.class);
		if (room != null && room.elementalWasSummoned()) {
			for (Char mob : level.mobs) if (mob instanceof VaultBossElemental && mob.isAlive()) return 0;
			return 4000;
		}
		// 打开大门可以带走一件低于 +1 的物品，开店仍需实际交付雕像。
		return 2000;
	}

	public boolean rewardAllowed(Item item, int score) {
		if (item == null || !item.isVaultLoot() || item.unique || item instanceof Bag
				|| !Dungeon.hero.belongings.contains(item)) return false;
		return score > 0 && (score >= 4000 || item.level() < 1);
	}

	/** 装备栏中的宝库物品也保留借用标记，离开时一并归还。 */
	private void gatherEquippedLoans(Hero hero) {
		ArrayList<Item> equipped = new ArrayList<>();
		for (Item item : hero.belongings) if (item.isVaultLoot() && item.isEquipped(hero)) equipped.add(item);
		for (Item item : equipped) {
			boolean cursed = item.cursed;
			item.cursed = false;
			try { ((EquipableItem) item).doUnequip(hero, false, false); }
			finally { item.cursed = cursed; }
			// Exit-only staging: equipped loans are returned immediately, even if all slots are occupied.
			if (!item.collect(this)) items.add(item);
		}
		// Also recover marked loans misplaced in the backpack or another bag.
		for (Item item : loanItems(hero)) {
			if (contains(item)) continue;
			item.detachAll(hero.belongings.backpack);
			if (!item.collect(this)) items.add(item);
		}
	}

	/** 归还借用物资，自动交付雕像并携出至多一件选中的普通奖励。 */
	public boolean returnToImp(Hero hero, Item reward, int score) {
		if (hero.belongings.getItem(MagicPocket.class) != this) return false;
		if (isVault() && (score <= 0 || score != completionScore((VaultLevel) Dungeon.level))) return false;
		if (reward != null && !rewardAllowed(reward, score)) return false;
		gatherEquippedLoans(hero);
		ImpStatue statue = null;
		for (Item item : this) {
			if (item instanceof ImpStatue) {
				statue = (ImpStatue) item;
				break;
			}
		}
		// 雕像自动交付，不占普通奖励的位置；放弃普通奖励也会完成交付。
		if (statue != null) statue.detachAll(this);
		ArrayList<BrokenSeal> playerSeals = new ArrayList<>();
		for (Item item : this) if (item instanceof Armor && item != reward) {
			Armor armor = (Armor) item;
			if (armor.checkSeal() != null && !armor.checkSeal().isVaultLoot()) {
				BrokenSeal seal = armor.detachSeal();
				seal.markPlayerOwned();
				playerSeals.add(seal);
			}
		}
		// Detach the pocket first so clearing it cannot grab more items from the backpack.
		detachAll(hero.belongings.backpack);
		Item kept = reward == null ? null : reward.detachAll(this);
		clearLoans(this);
		for (BrokenSeal seal : playerSeals) {
			Armor playerArmor = null;
			for (Item item : hero.belongings) if (item instanceof Armor && !item.isVaultLoot()
					&& ((Armor) item).checkSeal() == null) {
				playerArmor = (Armor) item;
				break;
			}
			if (playerArmor != null) playerArmor.affixSeal(seal);
			else if (!seal.collect(hero.belongings.backpack)) hero.belongings.backpack.items.add(seal);
		}
		if (kept != null) {
			kept.markPlayerOwned();
			if (kept instanceof Gold) Dungeon.gold += kept.quantity();
			else if (kept instanceof EnergyCrystal) Dungeon.energy += kept.quantity();
			else if (!kept.collect(hero.belongings.backpack)) Imp.Quest.reward = kept;
		}
		if (!Imp.Quest.isCompleted()) {
			Imp.Quest.returnOutcome = statue == null
					? (kept == null ? Imp.Quest.RETURN_EMPTY : Imp.Quest.RETURN_ITEM)
					: (kept == null ? Imp.Quest.RETURN_STATUE : Imp.Quest.RETURN_STATUE_ITEM);
			// 战斗得分已在开门和击败元素时结算，空手返回不撤销这些分数。
			Imp.Quest.complete(statue == null ? 0 : 4000);
		}
		Item.updateQuickslot();
		return true;
	}

	private static void clearLoans(Bag bag) {
		for (Item item : new ArrayList<>(bag.items)) {
			if (item instanceof Bag) clearLoans((Bag) item);
			item.detachAll(bag);
		}
	}

	public ArrayList<Item> loanItems(Hero hero) {
		ArrayList<Item> result = new ArrayList<>();
		for (Item item : this) result.add(item);
		for (Item item : hero.belongings) {
			if (item.isVaultLoot() && !result.contains(item)) result.add(item);
		}
		return result;
	}

	public void requestReturn(Hero hero, VaultLevel level, Runnable leave) {
		Game.runOnRenderThread(() -> {
			if (Dungeon.level != level || hero.belongings.getItem(MagicPocket.class) != this) return;
			int score = completionScore(level);
			boolean eligible = false;
			for (Item item : loanItems(hero)) eligible |= rewardAllowed(item, score);
			final boolean hasReward = eligible;
			GameScene.show(new WndOptions(name(), Messages.get(EscapeCrystal.class, "prompt"),
					hasReward ? new String[]{Messages.get(this, "choose"), Messages.get(this, "leave_empty"), Messages.get(this, "stay")}
							: new String[]{Messages.get(this, "leave_empty"), Messages.get(this, "stay")}) {
				@Override
				protected void onSelect(int index) {
					if (index == (hasReward ? 2 : 1)) return;
					if (!hasReward || index == 1) {
						if (returnToImp(hero, null, score)) leave.run();
						return;
					}
					GameScene.selectItem(new WndBag.ItemSelector() {
						@Override public Class<? extends Bag> preferredBag() { return Belongings.Backpack.class; }
						@Override public String textPrompt() { return Messages.get(EscapeCrystal.class, "prompt"); }
						@Override public boolean itemSelectable(Item item) { return rewardAllowed(item, score); }
						@Override public void onSelect(Item item) {
							if (item == null || !rewardAllowed(item, score)) return;
							GameScene.show(new WndOptions(new ItemSprite(item), Messages.titleCase(item.title()),
									Messages.get(EscapeCrystal.class, "leaving_item"),
									Messages.get(EscapeCrystal.class, "leaving_yes"),
									Messages.get(EscapeCrystal.class, "leaving_no")) {
								@Override protected void onSelect(int index) {
									if (index == 0 && returnToImp(hero, item, score)) leave.run();
								}
							});
						}
					});
				}
			});
		});
	}
}
