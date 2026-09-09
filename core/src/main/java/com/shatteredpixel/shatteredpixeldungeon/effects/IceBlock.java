/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.audio.Sample;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Texture;
import com.watabou.utils.RectF;

public class IceBlock extends Gizmo {
	
	private float phase;
	
	private CharSprite target;
	private boolean stone;
	
	public IceBlock( CharSprite target ) {
		super();

		this.target = target;
		phase = 0;
	}
	
	@Override
	public void update() {
		super.update();
		if (stone) {
			if (!target.alive) {
				killAndErase();
				return;
			}
			target.paused = true;
			phase = Math.min(1f, phase + Game.elapsed * 2);
			target.tint(0.65f, 0.65f, 0.65f, phase * 0.25f);
			return;
		}

		if ((phase += Game.elapsed * 2) < 1) {
			target.tint( 0.83f, 1.17f, 1.33f, phase * 0.6f );
		} else {
			target.tint( 0.83f, 1.17f, 1.33f, 0.6f );
		}
	}
	
	public void melt() {

		target.resetColor();
		killAndErase();

		if (visible) {
			Splash.at( target.center(), 0xFFB2D6FF, 5 );
			Sample.INSTANCE.play( Assets.Sounds.SHATTER );
		}
	}
	
	public static IceBlock freeze( CharSprite sprite ) {
		
		IceBlock iceBlock = new IceBlock( sprite );
		if (sprite.parent != null)
			sprite.parent.add( iceBlock );
		
		return iceBlock;
	}

	public static IceBlock petrify(CharSprite sprite) {
		// Keep the frozen pose's shading while removing all color from the statue.
		SmartTexture source = sprite.texture;
		SmartTexture gray = TextureCache.create("stone-sprite-" + source.hashCode(), source.width, source.height);
		gray.bitmap.setBlending(com.badlogic.gdx.graphics.Pixmap.Blending.None);
		for (int y = 0; y < source.height; y++) {
			for (int x = 0; x < source.width; x++) {
				int pixel = source.bitmap.getPixel(x, y);
				int value = ((pixel >>> 24) * 30 + ((pixel >>> 16) & 255) * 59
						+ ((pixel >>> 8) & 255) * 11) / 100;
				gray.bitmap.drawPixel(x, y, (value << 24) | (value << 16) | (value << 8) | (pixel & 255));
			}
		}
		gray.filter(Texture.NEAREST, Texture.NEAREST);
		RectF pose = sprite.frame();
		sprite.texture(gray);
		sprite.frame(pose);
		IceBlock block = new IceBlock(sprite);
		block.stone = true;
		sprite.paused = true;
		if (sprite.parent != null) sprite.parent.add(block);
		return block;
	}
}
