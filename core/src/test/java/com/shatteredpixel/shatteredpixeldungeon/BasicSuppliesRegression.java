package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.AlchemistsToolkit;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfDivination;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfMetamorphosis;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.watabou.utils.Bundle;
import java.util.HashMap;

public class BasicSuppliesRegression {
	private static void check(boolean value, String message) {
		if (!value) throw new AssertionError(message);
	}
	public static void main(String[] args) {
		com.badlogic.gdx.utils.GdxNativesLoader.load();
		WarriorTalentsRegression.setupHeadless();
		com.badlogic.gdx.Gdx.app = (com.badlogic.gdx.Application)java.lang.reflect.Proxy.newProxyInstance(
				BasicSuppliesRegression.class.getClassLoader(), new Class[]{com.badlogic.gdx.Application.class},
				(proxy, method, values) -> {
					if (method.getName().equals("log") || method.getName().equals("postRunnable")) return null;
					throw new UnsupportedOperationException(method.getName());
				});
		Dungeon.hero = new Hero();
		Potion.initColors(); Scroll.initLabels(); Ring.initGems();
		HashMap<Class<?>, Item> contents = new HashMap<>();
		int bags = 0;
		for (Item item : BasicSupplies.contents()) {
			check(contents.put(item.getClass(), item) == null, "no duplicated contents");
			if (item instanceof Bag) bags++;
			if (item instanceof Potion || item instanceof Scroll || item instanceof AlchemistsToolkit)
				check(!item.isIdentified(), "content remains unidentified: " + item.getClass());
		}
		check(bags == 4, "all four storage bags");
		check(contents.containsKey(TengusMask.class) && contents.containsKey(KingsCrown.class)
				&& contents.containsKey(Ankh.class), "progression and revival items");
		check(contents.get(AlchemistsToolkit.class).level() == 10, "toolkit is +10");
		for (Catalog category : new Catalog[]{Catalog.POTIONS, Catalog.SCROLLS, Catalog.SEEDS})
			for (Class<?> type : category.items()) check(contents.get(type).quantity() == 99, "99 of " + type);
		check(contents.get(ScrollOfDivination.class).quantity() == 99, "99 divination scrolls");
		check(contents.get(ScrollOfMetamorphosis.class).quantity() == 99, "99 metamorphosis scrolls");
		BasicSupplies pack = new BasicSupplies();
		pack.collect();
		Dungeon.gold = 7; Dungeon.energy = 3;
		check(pack.open(Dungeon.hero), "pack opens from inventory");
		check(Dungeon.gold == 10006 && Dungeon.energy == 1002, "currencies added, not overwritten");
		check(!pack.open(Dungeon.hero), "same pack cannot be opened twice");
		check(Dungeon.hero.belongings.getItem(BasicSupplies.class) == null, "pack consumed");
		check(!Dungeon.hero.belongings.getItem(AlchemistsToolkit.class).isIdentified(), "collection does not identify toolkit");
		for (Catalog category : new Catalog[]{Catalog.POTIONS, Catalog.SCROLLS})
			for (Class<?> type : category.items()) check(!Dungeon.hero.belongings.getItem(type.asSubclass(Item.class)).isIdentified(), "collection does not identify " + type);
		Bundle bundle = new Bundle();
		bundle.put("pack", new BasicSupplies());
		check(bundle.get("pack") instanceof BasicSupplies, "pack saves and restores");
		System.out.println("BasicSuppliesRegression passed: " + contents.size() + " item types");
	}
}
