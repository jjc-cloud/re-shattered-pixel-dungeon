package com.shatteredpixel.shatteredpixeldungeon.items.scrolls;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

public class ScrollOfMagicMap extends ScrollOfMagicMapping {

	{
		image = ItemSpriteSheet.SCROLL_MAGIC_MAP;
	}

	@Override
	protected void readEffect() {
		Dungeon.revealCurrentRegion();
		super.readEffect();
	}

	@Override
	protected void logLayout() {
		GLog.p( Messages.get(this, "layout") );
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
