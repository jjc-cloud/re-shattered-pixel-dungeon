package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.Key;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChestSession {

	private final Heap heap;
	private final Mimic mimic;
	private final boolean unlocked;
	private boolean moved;
	private boolean closed;

	public ChestSession(Heap heap, boolean unlocked) {
		this.heap = heap;
		this.mimic = null;
		this.unlocked = unlocked;
	}

	public ChestSession(Mimic mimic) {
		this.heap = null;
		this.mimic = mimic;
		this.unlocked = false;
	}

	public String title() {
		return heap == null ? mimic.name() : heap.title();
	}

	public boolean isMimic() {
		return mimic != null;
	}

	public List<Item> items() {
		return heap == null ? (mimic.items == null ? Collections.emptyList() : mimic.items) : heap.items;
	}

	public boolean take(Item item) {
		if (closed || item == null || !items().contains(item)) return false;
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.isAlive()) return false;
		if (mimic != null && (!hero.ready || hero.invisible <= 0 || !mimic.isAlive()
				|| mimic.alignment != Char.Alignment.NEUTRAL
				|| !mimic.canInteract(hero))) return false;
		if (heap != null && !heap.opened) {
			if (!heap.resolveOpeningEffects(hero)) return false;
		}
		if (!hero.pickUpFromChest(item, pos())) return false;
		items().remove(item);
		moved = true;
		if (heap != null && !heap.items.isEmpty() && heap.sprite != null) {
			heap.sprite.view(heap).place(heap.pos);
		}
		// 宝箱怪逐件结算，让隐身等状态在下一次取物前正常推进。
		if (mimic != null) hero.spendAndNextConstant(1f);
		return true;
	}

	public boolean put(Item item, Bag bag) {
		if (closed || mimic != null || item == null || bag == null || item instanceof Bag
				|| item.isEquipped(Dungeon.hero) || !bag.items.contains(item)
				|| heap.items.size() >= 5) return false;
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.isAlive()) return false;
		if (!heap.opened) {
			if (!heap.resolveOpeningEffects(hero)) return false;
			if (heap.items.size() >= 5) return false;
		}
		Item detached = item.detachAll(bag);
		if (detached == null) return false;
		heap.items.add(detached);
		moved = true;
		if (heap.sprite != null) heap.sprite.view(heap).place(heap.pos);
		return true;
	}

	//empties the chest: whatever fits goes into the bag, the rest drops to the floor.
	//pickUpFromChest is the same path a single pickup takes, so stacking and automatic
	//sorting into specialized bags (seeds, scrolls, potions...) still apply
	public void spill() {
		if (closed || heap == null) return;
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.isAlive()) return;
		moved = true;
		if (!heap.resolveOpeningEffects(hero)) return;
		ArrayList<Item> remaining = new ArrayList<>(heap.items);
		int pos = heap.pos;
		heap.destroy();
		for (Item item : remaining) {
			if (hero.isAlive() && hero.pickUpFromChest(item, pos)) continue;
			Dungeon.level.drop(item, pos).sprite.drop(pos);
		}
	}

	public void wakeMimic() {
		closed = true;
	}

	public Mimic mimic() {
		return mimic;
	}

	public void close() {
		if (closed) return;
		closed = true;
		if (heap != null && heap.items.isEmpty() && Dungeon.level.heaps.get(heap.pos) == heap) heap.destroy();
		if (mimic == null && (moved || unlocked) && Dungeon.hero.isAlive()) Dungeon.hero.spendAndNext(Key.TIME_TO_UNLOCK);
	}

	private int pos() {
		return heap == null ? mimic.pos : heap.pos;
	}
}
