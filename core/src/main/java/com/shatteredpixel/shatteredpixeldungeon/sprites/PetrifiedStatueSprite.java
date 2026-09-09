package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.PetrifiedStatue;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.TextureFilm;
import java.util.Arrays;

public class PetrifiedStatueSprite extends MobSprite {
	public PetrifiedStatueSprite(PetrifiedStatue statue) {
		SmartTexture stone = TextureCache.createPixels("statue-" + statue.imageWidth + "-" + Arrays.toString(statue.pixels),
				statue.imageWidth, statue.imageHeight, statue.pixels);
		texture(stone);
		idle = new Animation(1, true);
		idle.frames(new TextureFilm(stone, statue.imageWidth, statue.imageHeight), 0);
		run = attack = zap = die = idle.clone();
		play(idle);
		flipHorizontal = statue.flipped;
		paused = true;
	}
	@Override public void turnTo(int from, int to) { }
	@Override public void showAlert() { }
	@Override public void showLost() { }
	@Override public void showSleep() { }
	@Override public void die() {
		if (visible && parent != null) Splash.at(center(), 0xFF888888, 8);
		killAndErase();
	}
}
