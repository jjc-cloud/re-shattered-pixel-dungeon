package com.shatteredpixel.shatteredpixeldungeon.items.scrolls;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

public class ScrollOfDungeonBlueprint extends ScrollOfMagicMapping {

	{
		image = ItemSpriteSheet.DUNGEON_BLUEPRINT;
	}

	@Override
	protected void readEffect() {
		Dungeon.revealEntireDungeon();
		super.readEffect();
	}

	@Override
	protected void logLayout() {
		GLog.n( Messages.get(this, "layout") );
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
