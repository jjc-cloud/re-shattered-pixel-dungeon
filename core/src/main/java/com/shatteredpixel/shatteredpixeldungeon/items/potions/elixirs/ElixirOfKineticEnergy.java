package com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Stamina;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

public class ElixirOfKineticEnergy extends Elixir {

	private static final float DURATION = 500f;

	{
		image = ItemSpriteSheet.ELIXIR_KINETIC;
	}

	@Override
	public void apply(Hero hero) {
		identify();
		Buff.prolong(hero, Stamina.class, DURATION);
		SpellSprite.show(hero, SpellSprite.HASTE, 0.5f, 1, 0.5f);
	}
}
