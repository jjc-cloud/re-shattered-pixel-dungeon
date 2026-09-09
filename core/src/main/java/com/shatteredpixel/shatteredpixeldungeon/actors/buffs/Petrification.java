package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Texture;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/** Progress persists, but only an actively channeling eye can increase it. */
public class Petrification extends Buff implements Hero.Doom {
	private float progress;

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	public static boolean canAffect(Char victim) {
		return victim != null && victim.isAlive() && !victim.isImmune(Petrification.class)
				&& !victim.isInvulnerable(Petrification.class)
				&& victim.buff(com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing.Cleanse.class) == null
				&& victim.resist(Petrification.class) > 0f;
	}

	public static boolean apply(Char victim) {
		return apply(victim, 0.2f);
	}

	public static boolean apply(Char victim, float amount) {
		if (!canAffect(victim)) return false;
		Petrification buff = Buff.affect(victim, Petrification.class);
		if (buff.target != victim) return false;
		buff.progress += victim.resist(Petrification.class) * amount;
		BuffIndicator.refreshHero();
		if (buff.progress >= 1f) {
			Buff.detach(victim, Petrification.class);
			victim.HP = 0;
			// Hero.isAlive triggers the berserker's normal death-defiance rules.
			if (victim instanceof Hero && victim.isAlive()) return true;
			victim.die(buff);
		}
		return true;
	}

	public float progress() { return progress; }
	public float speedFactor() { return Math.max(0.0001f, 1f - progress); }

	@Override public int icon() { return BuffIndicator.INVISIBLE; }
	@Override public void tintIcon(Image icon) {
		// Copy this icon's pixels, replacing blue with gray without tinting the atlas.
		SmartTexture source = icon.texture;
		int x = Math.round(icon.frame().left * source.width);
		int y = Math.round(icon.frame().top * source.height);
		int width = Math.round(icon.frame().width() * source.width);
		int height = Math.round(icon.frame().height() * source.height);
		SmartTexture gray = TextureCache.create("petrification-icon-" + width + "x" + height, width, height);
		gray.bitmap.setBlending(com.badlogic.gdx.graphics.Pixmap.Blending.None);
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				int pixel = source.bitmap.getPixel(x + col, y + row);
				int r = pixel >>> 24, g = (pixel >>> 16) & 255, b = (pixel >>> 8) & 255;
				if (b > r) {
					int value = (r + g + b) / 3;
					pixel = (value << 24) | (value << 16) | (value << 8) | (pixel & 255);
				}
				gray.bitmap.drawPixel(col, row, pixel);
			}
		}
		gray.filter(Texture.NEAREST, Texture.NEAREST);
		icon.texture(gray);
		icon.frame(0, 0, width, height);
		icon.resetColor();
	}
	@Override public String iconTextDisplay() { return Math.round(progress * 100) + "%"; }
	@Override public String desc() { return Messages.get(this, "desc", iconTextDisplay()); }

	@Override public void onDeath() {
		Badges.validateDeathFromEnemyMagic();
		Dungeon.fail(this);
		GLog.n(Messages.get(this, "ondeath"));
	}

	@Override public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("progress", progress);
	}
	@Override public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		progress = bundle.getFloat("progress");
	}
}
