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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.watabou.noosa.TextureFilm;

public class VaultTokenDoorSprite extends MobSprite {

	public VaultTokenDoorSprite () {
		super();

		perspectiveRaise = 4/16f; //2 pixels less, to align with door visuals
		renderShadow = false;
		visibleOutOfFFOV = true;

		texture(Assets.Sprites.VAULT_TOKENS_DOOR );

		TextureFilm frames = new TextureFilm( texture, 16, 16 );

		idle = new Animation( 1, false );
		idle.frames( frames, 0 );

		run = idle.clone();

		attack = idle.clone();

		die = idle.clone();

		play( idle );
	}

	@Override
	public void link(Char ch) {
		super.link(ch);
		renderShadow = false;
		// 仅地图上绑定的门参与距离显形，查看窗口的预览保持完整显示。
		alpha(0f);
	}

	@Override
	public void update() {
		super.update();
		if (ch != null && Dungeon.hero != null && Dungeon.level != null) {
			// 4 格及以上全透明，3、2、1 格分别显示 25%、50%、75%。
			int distance = Dungeon.level.distance(ch.pos, Dungeon.hero.pos);
			alpha(0.25f * Math.max(0, 4 - Math.max(1, distance)));
		}
	}

}
