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

package com.shatteredpixel.shatteredpixeldungeon.tiles;

import com.badlogic.gdx.graphics.Pixmap;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.MiningLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.PathFinder;

public class DungeonTerrainTilemap extends DungeonTilemap {

	static DungeonTerrainTilemap instance;
	private final int alchemyFloorOffset;

	public DungeonTerrainTilemap(){
		super(Dungeon.level.tilesTex());

		Pixmap terrain = texture.bitmap;
		int width = terrain.getWidth(), height = terrain.getHeight();
		alchemyFloorOffset = width / SIZE * (height / SIZE);
		int[] pixels = new int[width * (height + SIZE * 2)];
		int potX = DungeonTileSheet.RAISED_ALCHEMY_POT % (width / SIZE) * SIZE;
		int potY = DungeonTileSheet.RAISED_ALCHEMY_POT / (width / SIZE) * SIZE;
		// 保留原地板及其变体，仅按旧炼金锅的透明锅口挖空，让底层动态水面透出。
		for (int y = 0; y < height + SIZE * 2; y++) for (int x = 0; x < width; x++) {
			int rgba = terrain.getPixel(x, y < height ? y : y - height);
			if (y >= height && (terrain.getPixel(potX + x % SIZE, potY + (y - height) % SIZE) & 0xFF) == 0) {
				rgba = 0;
			}
			pixels[x + y * width] = rgba;
		}
		texture = TextureCache.createPixels("alchemy-floor:" + Dungeon.level.tilesTex(),
				width, height + SIZE * 2, pixels);
		tileset = new TextureFilm(texture, SIZE, SIZE);

		map( Dungeon.level.terrainVisuals(), Dungeon.level.width() );

		instance = this;
	}

	@Override
	protected int getTileVisual(int pos, int tile, boolean flat) {
        if (!flat && !DungeonTileSheet.wallStitcheable(tile) && !DungeonTileSheet.doorTile(tile)) {
            if (Dungeon.level.cellAt(pos).hasTerrain(Terrain.WATER)) tile = Terrain.WATER;
            else if (TerrainFeaturesTilemap.contentVisual(pos, tile) >= 0
                    || tile == Terrain.TRAP || tile == Terrain.SECRET_TRAP || tile == Terrain.INACTIVE_TRAP)
                tile = Dungeon.level.cellAt(pos).ground();
        }
		if (!flat && DungeonTileSheet.doorTile(tile) && Dungeon.level instanceof VaultLevel
				&& ((VaultLevel) Dungeon.level).customDoorCell(pos)) {
			return DungeonTileSheet.getVisualWithAlts(DungeonTileSheet.FLOOR_SP, pos);
		}
		if (!flat && Dungeon.level instanceof VaultLevel && ((VaultLevel) Dungeon.level).customStatueCell(pos)) {
			return DungeonTileSheet.getVisualWithAlts(DungeonTileSheet.FLOOR, pos);
		}
		int visual = DungeonTileSheet.directVisuals.get(tile, -1);
		if (visual != -1) {
			if (visual == DungeonTileSheet.FLOOR_DECO && Dungeon.level instanceof MiningLevel) {
				for (int i : PathFinder.NEIGHBOURS4) {
					if (map[pos + i] == Terrain.MINE_BOULDER) {
						visual = DungeonTileSheet.MINE_FLOOR_DECO_HEAVY;
						break;
					}
				}
			}
			visual = DungeonTileSheet.getVisualWithAlts(visual, pos);
			if (!flat && visual < 32 && !Dungeon.level.cellAt(pos).buried()
					&& Dungeon.level.cellAt(pos).hasTerrain(Terrain.ALCHEMY)) {
				return alchemyFloorOffset + visual;
			}
			return visual;
		}

		if (tile == Terrain.WATER) {
			return DungeonTileSheet.stitchWaterTile(
					map[pos + PathFinder.CIRCLE4[0]],
					map[pos + PathFinder.CIRCLE4[1]],
					map[pos + PathFinder.CIRCLE4[2]],
					map[pos + PathFinder.CIRCLE4[3]]
			);

		} else if (tile == Terrain.CHASM) {
			return DungeonTileSheet.stitchChasmTile( pos > mapWidth ? map[pos - mapWidth] : -1);
		}

		if (!flat) {
			if ((DungeonTileSheet.doorTile(tile))) {
				return DungeonTileSheet.getRaisedDoorTile(tile, map[pos - mapWidth]);
			} else if (DungeonTileSheet.wallStitcheable(tile)){
				int wall = DungeonTileSheet.getRaisedWallTile(
						tile,
						pos,
						(pos+1) % mapWidth != 0 ?   map[pos + 1] : -1,
						pos + mapWidth < size ?     map[pos + mapWidth] : -1,
						pos % mapWidth != 0 ?       map[pos - 1] : -1
						);
				return wall;
			} else if (tile == Terrain.STATUE) {
				return DungeonTileSheet.RAISED_STATUE;
			} else if (tile == Terrain.STATUE_SP) {
				return DungeonTileSheet.RAISED_STATUE_SP;
			} else if (tile == Terrain.REGION_DECO) {
				return DungeonTileSheet.RAISED_REGION_DECO;
			} else if (tile == Terrain.REGION_DECO_ALT) {
				return DungeonTileSheet.RAISED_REGION_DECO_ALT;
			} else if (tile == Terrain.SEWER_BARREL_MARKED) {
				return DungeonTileSheet.RAISED_SEWER_BARREL_MARKED;
			} else if (tile == Terrain.SEWER_BARREL_MARKED_ALT) {
				return DungeonTileSheet.RAISED_SEWER_BARREL_MARKED_ALT;
			} else if (tile == Terrain.MINE_CRYSTAL) {
				return DungeonTileSheet.getVisualWithAlts(
						DungeonTileSheet.RAISED_MINE_CRYSTAL_BLUE_1,
						pos);
			} else if (tile == Terrain.MINE_BOULDER) {
				return DungeonTileSheet.getVisualWithAlts(
						DungeonTileSheet.RAISED_MINE_BOULDER,
						pos);
			} else if (tile == Terrain.ALCHEMY) {
				return DungeonTileSheet.RAISED_ALCHEMY_POT;
			} else if (tile == Terrain.BARRICADE) {
				return DungeonTileSheet.RAISED_BARRICADE;
			} else if (tile == Terrain.HIGH_GRASS) {
				return DungeonTileSheet.getVisualWithAlts(
						DungeonTileSheet.RAISED_HIGH_GRASS,
						pos);
			} else if (tile == Terrain.FURROWED_GRASS) {
				return DungeonTileSheet.getVisualWithAlts(
						DungeonTileSheet.RAISED_FURROWED_GRASS,
						pos);
			} else {
				return DungeonTileSheet.NULL_TILE;
			}
		} else {
			if (tile == Terrain.SEWER_BARREL_MARKED || tile == Terrain.SEWER_BARREL_MARKED_ALT) {
				return DungeonTileSheet.directFlatVisuals.get(tile);
			}
			visual = DungeonTileSheet.directFlatVisuals.get(tile);
			if (tile == Terrain.COLLAPSE_WALL && Dungeon.level.caveCollapse != null
					&& Dungeon.level.caveCollapse.hasGold(pos)) visual = DungeonTileSheet.FLAT_COLLAPSE_WALL_DECO;
			return DungeonTileSheet.getVisualWithAlts(visual, pos);
		}

	}

	public static Image tile(int pos, int tile ) {
		Image img = new Image( instance.texture );
		img.frame( instance.tileset.get( instance.getTileVisual( pos, tile, true ) ) );
		return img;
	}

	@Override
	protected boolean needsRender(int pos) {
		return super.needsRender(pos) && data[pos] != DungeonTileSheet.WATER;
	}
}
