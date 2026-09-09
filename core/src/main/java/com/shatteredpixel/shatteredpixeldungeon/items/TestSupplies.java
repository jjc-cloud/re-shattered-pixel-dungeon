package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/** Unlimited supplies for explicitly enabled test starts. Never randomly generated. */
public class TestSupplies extends Item {
	private static final String GENERATE = "GENERATE";
	private static final int PAGE_SIZE = 5;
	{
		image = ItemSpriteSheet.ARTIFACT_TOOLKIT;
		defaultAction = GENERATE;
		unique = true;
		keptThoughLostInvent = true;
	}
	@Override public boolean isUpgradable() { return false; }
	@Override public boolean isIdentified() { return true; }
	@Override public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(GENERATE);
		return actions;
	}
	@Override public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (GENERATE.equals(action)) showCategories(hero);
	}

	public static ArrayList<Class<? extends Item>> items(Catalog category) {
		ArrayList<Class<? extends Item>> result = new ArrayList<>();
		if (category == Catalog.MISC_CONSUMABLES) result.add(BasicSupplies.class);
		for (Class<?> type : category.items()) {
			if (Item.class.isAssignableFrom(type)) result.add(type.asSubclass(Item.class));
		}
		// This project's special warrior weapon is not in the random loot catalog.
		if (category == Catalog.MELEE_WEAPONS
				&& !result.contains(com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WarriorsShortsword.class)) {
			result.add(com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WarriorsShortsword.class);
		}
		return result;
	}

	private void showCategories(Hero hero) {
		ArrayList<Catalog> categories = new ArrayList<>();
		for (Catalog category : Catalog.values()) if (!items(category).isEmpty()) categories.add(category);
		choose(name(), categories, Catalog::title, 0,
				category -> showItems(hero, category, 0), () -> {});
	}
	private void showItems(Hero hero, Catalog category, int page) {
		choose(category.title(), items(category), type -> Messages.get(type, "name"), page,
				type -> configure(hero, category, type), () -> showCategories(hero));
	}
	private <T> void choose(String title, List<T> entries, Function<T, String> label, int page,
	                       Consumer<T> select, Runnable back) {
		int start = page * PAGE_SIZE, end = Math.min(start + PAGE_SIZE, entries.size());
		ArrayList<String> options = new ArrayList<>();
		for (int i = start; i < end; i++) options.add(label.apply(entries.get(i)));
		int previous = -1, next = -1;
		if (page > 0) { previous = options.size(); options.add(Messages.get(this, "previous")); }
		if (end < entries.size()) { next = options.size(); options.add(Messages.get(this, "next")); }
		int backIndex = options.size();
		options.add(Messages.get(this, "back"));
		final int prevIndex = previous, nextIndex = next;
		GameScene.show(new WndOptions(title, Messages.get(this, "page", page + 1,
				(entries.size() + PAGE_SIZE - 1) / PAGE_SIZE), options.toArray(new String[0])) {
			@Override protected void onSelect(int index) {
				if (index == prevIndex) choose(title, entries, label, page - 1, select, back);
				else if (index == nextIndex) choose(title, entries, label, page + 1, select, back);
				else if (index == backIndex) back.run();
				else select.accept(entries.get(start + index));
			}
		});
	}
	private void configure(Hero hero, Catalog category, Class<? extends Item> type) {
		GameScene.show(new WndTextInput(Messages.get(type, "name"), Messages.get(this, "settings"),
				"1,0", 12, false, Messages.get(this, "ac_generate"), Messages.get(this, "back")) {
			@Override public void onSelect(boolean positive, String text) {
				if (positive) {
					try {
						String[] parts = text.trim().split("[,，]", -1);
						if (parts.length != 2) throw new IllegalArgumentException();
						int count = Integer.parseInt(parts[0].trim()), level = Integer.parseInt(parts[1].trim());
						for (Item item : createItems(type, count, level)) {
							if (item instanceof Gold || item instanceof EnergyCrystal || item instanceof Dewdrop
									|| item instanceof com.shatteredpixel.shatteredpixeldungeon.items.keys.Key
									|| !item.collect(hero.belongings.backpack)) {
								Dungeon.level.drop(item, hero.pos).sprite.drop();
							}
						}
						Item.updateQuickslot();
						GLog.p(Messages.get(TestSupplies.this, "created", Messages.get(type, "name"), count));
					} catch (IllegalArgumentException e) {
						GLog.w(Messages.get(TestSupplies.this, "invalid"));
						configure(hero, category, type);
						return;
					}
				}
				showItems(hero, category, items(category).indexOf(type) / PAGE_SIZE);
			}
		});
	}

	public static ArrayList<Item> createItems(Class<? extends Item> type, int count, int level) {
		if (count < 1 || count > 100 || level < 0 || level > 30) throw new IllegalArgumentException();
		ArrayList<Item> result = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			Item item = Reflection.newInstance(type);
			if (item == null) throw new IllegalArgumentException();
			item.quantity(1);
			if (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.keys.Key) {
				((com.shatteredpixel.shatteredpixeldungeon.items.keys.Key)item).depth = Dungeon.depth;
			}
			if (item.isUpgradable()) item.upgrade(level);
			item.identify();
			if (item.stackable) { item.quantity(count); result.add(item); break; }
			result.add(item);
		}
		return result;
	}
}
