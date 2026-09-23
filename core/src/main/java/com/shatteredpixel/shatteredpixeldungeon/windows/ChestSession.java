package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.Key;

import java.util.ArrayList;
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
		return heap == null ? mimic.items : heap.items;
	}

	public boolean take(Item item) {
		if (closed || item == null || !items().contains(item)) return false;
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.isAlive() || !hero.pickUpFromChest(item, pos())) return false;
		items().remove(item);
		moved = true;
		if (heap != null && !heap.items.isEmpty() && heap.sprite != null) {
			heap.sprite.view(heap).place(heap.pos);
		}
		return true;
	}

	public boolean put(Item item, Bag bag) {
		if (closed || mimic != null || item == null || bag == null || item instanceof Bag
				|| item.isEquipped(Dungeon.hero) || !bag.items.contains(item)
				|| heap.items.size() >= 5) return false;
		Item detached = item.detachAll(bag);
		if (detached == null) return false;
		heap.items.add(detached);
		moved = true;
		if (heap.sprite != null) heap.sprite.view(heap).place(heap.pos);
		return true;
	}

	public void spill() {
		if (closed || heap == null) return;
		ArrayList<Item> dropped = new ArrayList<>(heap.items);
		int pos = heap.pos;
		heap.destroy();
		for (Item item : dropped) {
			Dungeon.level.drop(item, pos).sprite.drop(pos);
		}
		moved = true;
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
		if (moved || unlocked) Dungeon.hero.spendAndNext(Key.TIME_TO_UNLOCK);
	}

	private int pos() {
		return heap == null ? mimic.pos : heap.pos;
	}
}
