package com.shatteredpixel.shatteredpixeldungeon.items.bombs;

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

public class SuperBomb extends Bomb {

	{
		image = ItemSpriteSheet.SUPER_BOMB;
	}

	//和绝大多数炼金炸弹一样是范围 2
	@Override
	protected int explosionRange() {
		return 2;
	}

	@Override
	protected int damageRoll() {
		return 3 * super.damageRoll();
	}
}
