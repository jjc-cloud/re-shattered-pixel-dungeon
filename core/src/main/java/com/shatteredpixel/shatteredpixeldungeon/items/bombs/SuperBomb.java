/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.shatteredpixel.shatteredpixeldungeon.items.bombs;

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

public class SuperBomb extends Bomb {

	{
		image = ItemSpriteSheet.SUPER_BOMB;
	}

	@Override
	protected int damageRoll() {
		return 3 * super.damageRoll();
	}
}
