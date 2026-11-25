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

package com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard;

import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.CellPlacement;

//doesn't look much like a bridge, but can easily use it internally
public class RegionDecoBridgeRoom extends StandardBridgeRoom {

	//can be large because the line breaks the space up
	public float[] sizeCatProbs(){
		return new float[]{2, 1, 0};
	}

	protected int maxBridgeWidth( int roomDimension ) {
		return 1;
	}

	protected CellPlacement spaceTile(){
		return CellPlacement.reset(Terrain.EMPTY, Terrain.REGION_DECO_ALT);
	}

	@Override
	protected CellPlacement bridgeTile(Level level) {
		// 矿洞架构下方可以步行，保留贯穿房间的整条架构。
		return level.region() == 2 ? CellPlacement.NONE : super.bridgeTile(level);
	}
}
