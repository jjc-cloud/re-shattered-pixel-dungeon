/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.items.scrolls;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

public class ScrollOfMagicMap extends ScrollOfMagicMapping {

	{
		image = ItemSpriteSheet.SCROLL_MAGIC_MAP;
	}

	@Override
	public void doRead() {
		Dungeon.revealCurrentRegion();
		super.doRead();
	}

	@Override
	public boolean isKnown() {
		return true;
	}

	@Override
	public Item identify( boolean byHero ) {
		return this;
	}

	@Override
	public int value() {
		return 80 * quantity;
	}
}
