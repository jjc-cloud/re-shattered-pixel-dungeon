package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.TestSupplies;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.IronKey;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.watabou.utils.Bundle;
import java.util.ArrayList;

public class TestSuppliesRegression {
	private static void check(boolean value, String message) {
		if (!value) throw new AssertionError(message);
	}
	public static void main(String[] args) {
		com.badlogic.gdx.utils.GdxNativesLoader.load();
		WarriorTalentsRegression.setupHeadless();
		com.badlogic.gdx.Gdx.app = (com.badlogic.gdx.Application)java.lang.reflect.Proxy.newProxyInstance(
				TestSuppliesRegression.class.getClassLoader(), new Class[]{com.badlogic.gdx.Application.class},
				(proxy, method, values) -> {
					if (method.getName().equals("log") || method.getName().equals("postRunnable")) return null;
					throw new UnsupportedOperationException(method.getName());
				});
		Dungeon.hero = new Hero();
		Potion.initColors(); Scroll.initLabels(); Ring.initGems();
		check(TestSupplies.items(Catalog.EXOTIC_POTIONS).contains(PotionOfCleansing.class), "cleansing is available by category");
		check(TestSupplies.items(Catalog.ENCHANTMENTS).isEmpty(), "non-item catalog entries are excluded");
		ArrayList<Item> potions = TestSupplies.createItems(PotionOfCleansing.class, 5, 0);
		check(potions.size() == 1 && potions.get(0).quantity() == 5 && potions.get(0).isIdentified(), "identified consumable stack");
		ArrayList<Item> weapons = TestSupplies.createItems(Greatsword.class, 2, 3);
		check(weapons.size() == 2 && weapons.get(0) != weapons.get(1), "equipment instances are independent");
		check(weapons.get(0).level() == 3 && weapons.get(1).level() == 3, "requested upgrade level");
		Dungeon.depth = 21;
		check(((IronKey)TestSupplies.createItems(IronKey.class, 1, 0).get(0)).depth == 21, "keys match current floor");
		int count = 0;
		for (Catalog category : Catalog.values()) {
			for (Class<? extends Item> type : TestSupplies.items(category)) {
				check(TestSupplies.createItems(type, 1, 0).get(0).getClass() == type, "catalog item can be generated: " + type);
				count++;
			}
		}
		boolean rejected = false;
		try { TestSupplies.createItems(Greatsword.class, 0, 0); } catch (IllegalArgumentException expected) { rejected = true; }
		check(rejected, "invalid counts rejected");
		Bundle bundle = new Bundle();
		bundle.put("tool", new TestSupplies());
		check(bundle.get("tool") instanceof TestSupplies, "tool can be restored from a save");
		TestStart.apply();
		check(Dungeon.hero.belongings.getItem(TestSupplies.class) != null, "test start grants the generator");
		System.out.println("TestSuppliesRegression passed: " + count + " catalog items");
	}
}
