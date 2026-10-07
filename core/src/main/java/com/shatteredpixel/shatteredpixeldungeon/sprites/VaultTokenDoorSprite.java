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
import com.watabou.noosa.Game;
import com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultTokenDoor;

public class VaultTokenDoorSprite extends MobSprite {

	private boolean wide;
	private float fadeStart;
	private float fadeTarget;
	private float fadeElapsed = 1f;

	public VaultTokenDoorSprite () {
		super();

		perspectiveRaise = 4/16f; //2 pixels less, to align with door visuals
		renderShadow = false;
		visibleOutOfFFOV = true;

		wide = Dungeon.level instanceof VaultLevel && ((VaultLevel) Dungeon.level).isRegularQuest();
		texture(wide ? Assets.Environment.CITY_QUEST : Assets.Sprites.VAULT_TOKENS_DOOR);
		TextureFilm frames;
		if (wide) {
			perspectiveRaise = 0;
			frames = new TextureFilm(texture);
			frames.add(0, texture.uvRect(0, 128, 48, 176));
		} else frames = new TextureFilm(texture, 16, 16);

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
		fadeStart = fadeTarget = 0f;
		fadeElapsed = 1f;
		flipHorizontal = false;
	}

	@Override
	public void update() {
		super.update();
		if (ch != null && Dungeon.hero != null && Dungeon.level != null) {
			int distance = Dungeon.level.distance(ch.pos, Dungeon.hero.pos);
			if (wide && ch instanceof VaultTokenDoor) {
				for (int offset = -1; offset <= 1; offset++) distance = Math.min(distance, Dungeon.level.distance(ch.pos + offset, Dungeon.hero.pos));
				float target = distance >= 3 ? 0f : distance == 2 ? .5f : 1f;
				if (target != fadeTarget) {
					fadeStart = alpha();
					fadeTarget = target;
					fadeElapsed = 0f;
				}
				fadeElapsed = Math.min(1f, fadeElapsed + Game.elapsed);
				alpha(fadeStart + (fadeTarget - fadeStart) * fadeElapsed);
			} else {
				alpha(0.25f * Math.max(0, 4 - Math.max(1, distance)));
			}
		}
	}
}
