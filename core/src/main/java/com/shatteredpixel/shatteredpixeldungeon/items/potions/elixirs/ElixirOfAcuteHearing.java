package com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AcuteHearing;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HearingLoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfMagicalSight;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

public class ElixirOfAcuteHearing extends Elixir {

	{
		image = ItemSpriteSheet.ELIXIR_HEARING;
	}

	@Override
	protected void drink(Hero hero) {
		if (hero.buff(AcuteHearing.class) != null || hero.buff(HearingLoss.class) != null) {
			GLog.w(Messages.get(this, "cannot_drink"));
			return;
		}
		super.drink(hero);
	}

	@Override
	public void apply(Hero hero) {
		if (hero.buff(AcuteHearing.class) != null || hero.buff(HearingLoss.class) != null) return;
		identify();
		Buff.prolong(hero, AcuteHearing.class, AcuteHearing.DURATION);
		SpellSprite.show(hero, SpellSprite.VISION);
	}

	public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe.SimpleRecipe {

		{
			inputs = new Class[]{PotionOfMagicalSight.class};
			inQuantity = new int[]{1};
			cost = 8;
			output = ElixirOfAcuteHearing.class;
			outQuantity = 1;
		}
	}
}
