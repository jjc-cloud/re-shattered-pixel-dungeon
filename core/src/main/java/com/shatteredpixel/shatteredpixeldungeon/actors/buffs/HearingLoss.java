package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Image;

public class HearingLoss extends FlavourBuff {

	public static final float DURATION = 50f;

	{
		// 后遗症同时作为秘药的再次饮用间隔。
		type = buffType.NEGATIVE;
		announced = true;
	}

	@Override
	public int icon() {
		return BuffIndicator.ACUTE_HEARING;
	}

	@Override
	public void tintIcon(Image icon) {
		// 按原图的明暗转为灰度，保留图案与透明度，不修改敏锐听觉的原图集。
		SmartTexture source = icon.texture;
		int x = Math.round(icon.frame().left * source.width);
		int y = Math.round(icon.frame().top * source.height);
		int width = Math.round(icon.frame().width() * source.width);
		int height = Math.round(icon.frame().height() * source.height);
		int[] pixels = new int[width * height];
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				int pixel = source.bitmap.getPixel(x + col, y + row);
				int r = pixel >>> 24;
				int g = (pixel >>> 16) & 255;
				int b = (pixel >>> 8) & 255;
				int value = (299 * r + 587 * g + 114 * b + 500) / 1000;
				pixels[row * width + col] = (value << 24) | (value << 16)
						| (value << 8) | (pixel & 255);
			}
		}
		icon.texture(TextureCache.createPixels("hearing-loss-icon-" + width + "x" + height,
				width, height, pixels));
		icon.frame(0, 0, width, height);
		icon.resetColor();
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (DURATION - visualcooldown()) / DURATION);
	}
}
