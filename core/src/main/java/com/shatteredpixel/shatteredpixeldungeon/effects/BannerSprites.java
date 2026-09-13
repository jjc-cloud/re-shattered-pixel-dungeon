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
import com.watabou.noosa.Image;

public class BannerSprites {

	public enum  Type {
		TITLE_PORT,
		TITLE_GLOW_PORT,
		TITLE_LAND,
		TITLE_GLOW_LAND,
		BOSS_SLAIN,
		GAME_OVER,
	}

	public static Image get( Type type ) {
		Image icon = new Image( Assets.Interfaces.BANNERS );
		switch (type) {
			case TITLE_PORT:
			case TITLE_LAND:
				// The replacement atlas has one title shared by both orientations.
				icon.frame( icon.texture.uvRect( 46, 7, 210, 95 ) );
				break;
			case TITLE_GLOW_PORT:
			case TITLE_GLOW_LAND:
				icon.frame( icon.texture.uvRect( 51, 98, 205, 177 ) );
				break;
			case BOSS_SLAIN:
				icon.frame( icon.texture.uvRect( 67, 190, 189, 224 ) );
				break;
			case GAME_OVER:
				icon.frame( icon.texture.uvRect( 81, 222, 168, 256 ) );
				break;
		}
		return icon;
	}
}
