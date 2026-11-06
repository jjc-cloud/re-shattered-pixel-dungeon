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

public class SkeletonRemainsSprite extends SkeletonSprite {

	private Animation hiding;

	public SkeletonRemainsSprite() {
		super();

		hiding = new Animation( 1, true );
		hiding.frames( ItemSpriteSheet.film, ItemSpriteSheet.SKELETON_REMAINS );
	}

	//潜伏时显示为遗骸。由 SkeletonRemains.sprite() 设置，物品贴图的帧坐标与骷髅贴图不同，切换纹理后需要重新定位。
	public void showRemains() {
		texture( Assets.Sprites.ITEMS );
		perspectiveRaise = 5 / 16f;
		shadowWidth = 1f;
		shadowOffset = 0.5f;
		//texture() 会把帧重置为整张图，循环动画已在播放时 play() 会直接返回，必须强制重设帧
		play( hiding, true );
		if (ch != null) place( ch.pos );
	}

	public void showSkeleton() {
		texture( Assets.Sprites.SKELETON );
		perspectiveRaise = 6 / 16f;
		shadowWidth = 1.2f;
		shadowOffset = 0.25f;
		play( idle, true );
		if (ch != null) place( ch.pos );
	}
}
