package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.windows.ChestSession;
import com.watabou.utils.Bundle;
import com.watabou.utils.SparseArray;

import java.util.ArrayList;

public class ChestReworkRegression {

	public static class PickupItem extends Item {
		@Override
		public boolean doPickUp(Hero hero, int pos) {
			hero.spendAndNext(1f);
			return true;
		}
	}

	public static class ChestGoldLevel extends SewerLevel {
		@Override protected boolean build() {
			setSize(9, 9);
			for (int y = 1; y < 8; y++) for (int x = 1; x < 8; x++) map[x + y * 9] = Terrain.EMPTY;
			return true;
		}
		@Override protected void createMobs() { }
		@Override protected void createItems() {
			Heap.Type[] types = {Heap.Type.CHEST, Heap.Type.LOCKED_CHEST, Heap.Type.CRYSTAL_CHEST};
			for (int i = 0; i < types.length; i++) {
				Heap heap = new Heap();
				heap.pos = 20 + i;
				heap.type = types[i];
				heap.items.add(new PickupItem());
				heaps.put(heap.pos, heap);
			}
		}
	}

	private static void check(boolean value, String message) {
		if (!value) throw new AssertionError(message);
	}

	public static void main(String[] args) {
		com.badlogic.gdx.utils.GdxNativesLoader.load();
		WarriorTalentsRegression.setupHeadless();
		ChestGoldLevel generated = new ChestGoldLevel();
		generated.create();
		for (int pos = 20; pos < 23; pos++) {
			Heap generatedChest = generated.heaps.get(pos);
			check(generatedChest.size() == 2 && generatedChest.items.get(1) instanceof Gold,
					"every chest type receives one extra gold group");
			int amount = generatedChest.items.get(1).quantity();
			check(amount >= 30 + Dungeon.depth * 10 && amount <= 60 + Dungeon.depth * 20,
					"extra chest gold uses the normal depth range");
		}
		Mimic generatedMimic = Mimic.spawnAt(19, false, new PickupItem());
		boolean hasExtraGold = false;
		for (Item item : generatedMimic.items) {
			if (item instanceof Gold) hasExtraGold = true;
		}
		check(hasExtraGold, "mimic chests also receive gold");
		Dungeon.level.heaps = new SparseArray<>();
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = 20;

		Heap chest = new Heap();
		chest.pos = 21;
		chest.type = Heap.Type.LOCKED_CHEST;
		chest.opened = true;
		PickupItem prize = new PickupItem();
		chest.items.add(prize);
		chest.items.add(new PickupItem());
		Dungeon.level.heaps.put(chest.pos, chest);

		float before = Dungeon.hero.cooldown();
		ChestSession session = new ChestSession(chest, false);
		check(session.take(prize), "manual pickup succeeds");
		check(Dungeon.hero.cooldown() == before, "pickup does not advance time before closing");
		check(chest.items.size() == 1, "only the chosen item leaves the chest");
		session.close();
		check(Dungeon.hero.cooldown() == before + 1f, "a changed chest costs one turn on close");

		before = Dungeon.hero.cooldown();
		new ChestSession(chest, false).close();
		check(Dungeon.hero.cooldown() == before, "reopening without changes costs no turn");
		new ChestSession(chest, true).close();
		check(Dungeon.hero.cooldown() == before + 1f, "a keyed opening costs one turn even without changes");

		Mimic mimic = new Mimic();
		mimic.setLevel(1);
		mimic.pos = 19;
		mimic.items = new ArrayList<>();
		mimic.items.add(new PickupItem());
		ChestSession mimicSession = new ChestSession(mimic);
		check(mimicSession.take(mimic.items.get(0)), "manual mimic pickup succeeds");
		check(mimic.items.isEmpty() && mimic.isAlive(), "an emptied mimic remains alive");

		Bundle saved = new Bundle();
		chest.storeInBundle(saved);
		Heap restored = new Heap();
		restored.restoreFromBundle(saved);
		check(restored.opened && restored.type == Heap.Type.LOCKED_CHEST,
				"an opened locked chest stays opened after saving");
	}
}
