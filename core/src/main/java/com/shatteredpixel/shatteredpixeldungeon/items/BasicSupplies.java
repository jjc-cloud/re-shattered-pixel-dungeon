package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.AlchemistsToolkit;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.MagicalHolster;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.PotionBandolier;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.ScrollHolder;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.VelvetPouch;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfDivination;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfMetamorphosis;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Reflection;
import java.util.ArrayList;

/** A single-use test supply chest. Contents retain their normal identification state. */
public class BasicSupplies extends Item {
	private static final String OPEN = "OPEN";
	{
		image = ItemSpriteSheet.CHEST;
		defaultAction = OPEN;
		unique = true;
		keptThoughLostInvent = true;
	}
	@Override public boolean isUpgradable() { return false; }
	@Override public boolean isIdentified() { return true; }
	@Override public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(OPEN);
		return actions;
	}
	@Override public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (OPEN.equals(action) && open(hero)) GLog.p(Messages.get(this, "opened"));
	}

	public boolean open(Hero hero) {
		if (!hero.belongings.backpack.contains(this)) return false;
		ArrayList<Item> contents = contents();
		detachAll(hero.belongings.backpack);
		// Install bags first so the supplies can be sorted into them on collection.
		for (Item item : contents) {
			if (item instanceof Bag && hero.belongings.getItem(item.getClass()) != null) continue;
			if (!item.collect(hero.belongings.backpack)) Dungeon.level.drop(item, hero.pos).sprite.drop();
		}
		Dungeon.gold += 9999;
		Dungeon.energy += 999;
		Item.updateQuickslot();
		return true;
	}

	public static ArrayList<Item> contents() {
		ArrayList<Item> items = new ArrayList<>();
		items.add(new VelvetPouch());
		items.add(new ScrollHolder());
		items.add(new PotionBandolier());
		items.add(new MagicalHolster());
		items.add(new TengusMask());
		items.add(new KingsCrown());
		items.add(new Ankh());
		items.add(new AlchemistsToolkit().upgrade(10));
		// Ordinary potion/scroll sets, plus the two explicitly requested exotic scrolls.
		for (Catalog category : new Catalog[]{Catalog.POTIONS, Catalog.SCROLLS, Catalog.SEEDS}) {
			for (Class<?> type : category.items()) {
				items.add(Reflection.newInstance(type.asSubclass(Item.class)).quantity(99));
			}
		}
		items.add(new ScrollOfDivination().quantity(99));
		items.add(new ScrollOfMetamorphosis().quantity(99));
		return items;
	}
}
