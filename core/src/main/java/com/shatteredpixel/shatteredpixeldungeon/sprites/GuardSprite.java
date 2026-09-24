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

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.TextureFilm;

public class GuardSprite extends MobSprite {

	public GuardSprite() {
		this(0);
	}

	/** Rare variants of the guard use further rows of the same sprite sheet. */
	protected GuardSprite(int row) {
		super();

		texture( Assets.Sprites.GUARD );

		TextureFilm frames = new TextureFilm( texture, 12, 16 );
		int offset = row * (texture.width / 12);

		idle = new Animation( 2, true );
		idle.frames( frames, offset, offset, offset, offset+1, offset, offset, offset+1, offset+1 );

		run = new MovieClip.Animation( 15, true );
		run.frames( frames, offset+2, offset+3, offset+4, offset+5, offset+6, offset+7 );

		attack = new MovieClip.Animation( 12, false );
		attack.frames( frames, offset+8, offset+9, offset+10 );

		die = new MovieClip.Animation( 8, false );
		die.frames( frames, offset+11, offset+12, offset+13, offset+14 );

		play( idle );
	}

	@Override
	public void play( Animation anim ) {
		if (anim == die) {
			emitter().burst( ShadowParticle.UP, 4 );
		}
		super.play( anim );
	}
}