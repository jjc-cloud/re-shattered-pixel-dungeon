package com.shatteredpixel.shatteredpixeldungeon.items.spells;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ForcedBlindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.LiquidMetal;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Blindweed;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

public class BlindingCrystal extends TargetedSpell {

	{
		image = ItemSpriteSheet.BLINDING_CRYSTAL;
		usesTargeting = true;
		talentChance = 1f / Recipe.OUT_QUANTITY;
	}

	@Override
	protected void affectTarget(Ballistica bolt, Hero hero) {
		Char target = Actor.findChar(bolt.collisionPos);
		if (target != null) {
			ForcedBlindness.apply(target);
			if (target.sprite != null) target.sprite.burst(0xFFFFFF88, 6);
			Sample.INSTANCE.play(Assets.Sounds.HIT_MAGIC);
		} else {
			GLog.w(Messages.get(this, "no_target"));
		}
		onSpellused();
	}

	@Override
	public int value() {
		return 10 * quantity;
	}

	@Override
	public int energyVal() {
		return quantity;
	}

	public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe.SpecialRecipe {

		public static final int OUT_QUANTITY = 6;

		public Recipe() {
			super(EXPERIMENTAL_SPELLS, 6, BlindingCrystal.class, LiquidMetal.class, Blindweed.Seed.class);
			inQuantity = new int[]{10, 1};
			allowExcess = new boolean[]{true, false};
			outQuantity = OUT_QUANTITY;
		}
	}
}
