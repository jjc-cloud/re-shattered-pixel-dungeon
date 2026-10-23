package com.shatteredpixel.shatteredpixeldungeon.tiles;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.tiles.custom.Carpet;

/**
 * 地毯破损缝。每条边和每个角各一层，按周围现存的地毯现场拼接，
 * 不存在“哪个洞形对应哪张图”的状态或查表。
 */
public class CarpetSeamTilemap extends DungeonTilemap {

	private final int side;

	public CarpetSeamTilemap(int side) {
		super(Assets.Environment.CARPET);
		this.side = side;
		if (Dungeon.level != null) {
			map(Dungeon.level.terrainVisuals(), Dungeon.level.width());
		}
	}

	@Override
	protected int getTileVisual(int pos, int tile, boolean flat) {
		return Carpet.seamPiece(pos, side);
	}
}
