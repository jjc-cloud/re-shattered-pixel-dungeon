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
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.LastShopLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.CellContent;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.tiles.custom.VaultTiles;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.tweeners.ScaleTweener;
import com.watabou.utils.PointF;
import com.watabou.utils.RectF;
import com.watabou.utils.SparseArray;

public class TerrainFeaturesTilemap extends DungeonTilemap {
	public static final int OVERHEAD_BODY = -4;
	public static final int OVERHEAD_TOP = -5;

	private static TerrainFeaturesTilemap instance;

	private SparseArray<Plant> plants;
	private int contentType = -1;
	private SparseArray<Trap> traps;

	public TerrainFeaturesTilemap(SparseArray<Plant> plants, SparseArray<Trap> traps) {
		this(plants, traps, -1);
	}

	public TerrainFeaturesTilemap(SparseArray<Plant> plants, SparseArray<Trap> traps, int contentType) {
		super(Assets.Environment.TERRAIN_FEATURES);

		this.contentType = contentType;
		this.plants = plants;
		this.traps = traps;
		if (contentType == OVERHEAD_TOP) {
			texture = TextureCache.get(Dungeon.level.tilesTex());
			tileset = new TextureFilm(texture, SIZE, SIZE);
		}
		if (contentType == Terrain.STATUE || contentType == Terrain.STATUE_SP) {
			texture = contentImage(0, contentType).texture;
			tileset = new TextureFilm(texture, SIZE, SIZE);
		}

		if (Dungeon.level != null) {
			map(Dungeon.level.terrainVisuals(), Dungeon.level.width());
		}

		if (contentType != Terrain.STATUE && contentType != Terrain.STATUE_SP
				&& contentType != OVERHEAD_BODY && contentType != OVERHEAD_TOP) instance = this;
	}

	protected int getTileVisual(int pos, int tile, boolean flat){
		if (contentType == OVERHEAD_BODY || contentType == OVERHEAD_TOP) {
			int cell = contentType == OVERHEAD_TOP ? pos + mapWidth : pos;
			if (cell >= size) return -1;
			CellContent frame = Dungeon.level.cellAt(cell).firstWith(CellContent.OVERHEAD);
			if (frame == null) return -1;
			if (contentType == OVERHEAD_BODY) return contentVisual(cell, frame.type());
			return frame.type() == Terrain.REGION_DECO_ALT
					? DungeonTileSheet.REGION_DECO_ALT_OVERHANG : DungeonTileSheet.REGION_DECO_OVERHANG;
		}
		if (Dungeon.level.cellAt(pos).buried()) return -1;
		if (contentType >= 0) {
			if (!Dungeon.level.cellAt(pos).hasTerrain(contentType)) return -1;
			CellContent content = Dungeon.level.cellAt(pos).content(contentType);
			if (content != null && content.has(CellContent.OVERHEAD)) return -1;
			if (contentType == Terrain.STATUE && Dungeon.level instanceof VaultLevel
					&& ((VaultLevel) Dungeon.level).customStatueCell(pos)) return -1;
			return contentType == Terrain.STATUE || contentType == Terrain.STATUE_SP
					? 0 : contentVisual(pos, contentType);
		}
		if (contentType != -3 && traps != null && traps.get(pos) != null){
			Trap trap = traps.get(pos);
			if (!trap.visible)
				return -1;
			else
				return (trap.active ? trap.color : Trap.BLACK) + (trap.shape * 16);
		}

		if (contentType != -2 && plants != null && plants.get(pos) != null){
			return plants.get(pos).image + 7*16;
		}

		if (contentType != -1) return -1;
		return contentVisual(pos, tile);
	}

	public static int contentVisual(int pos, int tile) {
		int stage = (Dungeon.depth-1)/5;
		if (Dungeon.depth == 21 && Dungeon.level instanceof LastShopLevel) stage--;
		stage = Math.min(stage, 4);
		if (tile == Terrain.HIGH_GRASS){
			if (DungeonTileSheet.getVisualWithAlts(DungeonTileSheet.RAISED_HIGH_GRASS, pos) == DungeonTileSheet.RAISED_HIGH_GRASS_ALT){
				return 128 + 16*stage + 1;
			} else {
				return 128 + 16*stage;
			}
		} else if (tile == Terrain.FURROWED_GRASS){
			if (DungeonTileSheet.getVisualWithAlts(DungeonTileSheet.RAISED_FURROWED_GRASS, pos) == DungeonTileSheet.RAISED_FURROWED_ALT){
				return 130 + 16*stage + 1;
			} else {
				return 130 + 16*stage;
			}
		} else if (tile == Terrain.GRASS) {
			if (DungeonTileSheet.getVisualWithAlts(DungeonTileSheet.GRASS, pos) == DungeonTileSheet.GRASS_ALT){
				return 132 + 16*stage + 1;
			} else {
				return 132 + 16*stage;
			}
		} else if (tile == Terrain.BARRICADE) {
			return 134 + 16*stage;

		} else if (tile == Terrain.ALCHEMY) {
			return 135 + 16*stage;

		} else if (tile == Terrain.STATUE || tile == Terrain.STATUE_SP) {
			return 136 + 16*stage;

		} else if (tile == Terrain.REGION_DECO) {
			// 标记木桶直接使用区域贴图；这里的普通木桶前景会遮住桶身标记。
			return 137 + 16 * stage;

		} else if (tile == Terrain.REGION_DECO_ALT) {
			return 138 + 16 * stage;

		} else if (tile == Terrain.EMBERS) {
			int embers = DungeonTileSheet.EMBERS;
			int embersAlt = DungeonTileSheet.EMBERS_ALT;
			if (DungeonTileSheet.getVisualWithAlts(embers, pos) == embersAlt){
				return 208 + 1;
			} else {
				return 208;
			}
		} else if (tile == Terrain.MINE_CRYSTAL){
			int vis = DungeonTileSheet.getVisualWithAlts(DungeonTileSheet.RAISED_MINE_CRYSTAL_BLUE_1, pos);
			if (vis == DungeonTileSheet.RAISED_MINE_CRYSTAL_RED_2){
				return 210 + 5;
			} else if (vis == DungeonTileSheet.RAISED_MINE_CRYSTAL_RED_1){
				return 210 + 4;
			} else if (vis == DungeonTileSheet.RAISED_MINE_CRYSTAL_GREEN_2){
				return 210 + 3;
			} else if (vis == DungeonTileSheet.RAISED_MINE_CRYSTAL_GREEN_1){
				return 210 + 2;
			} else if (vis == DungeonTileSheet.RAISED_MINE_CRYSTAL_BLUE_2){
				return 210 + 1;
			} else {
				return 210 + 0;
			}
		} else if (tile == Terrain.MINE_BOULDER){
			int vis = DungeonTileSheet.getVisualWithAlts(DungeonTileSheet.RAISED_MINE_BOULDER, pos);
			if (vis == DungeonTileSheet.RAISED_MINE_BOULDER_ALT_2){
				return 216 + 2;
			} else if (vis == DungeonTileSheet.RAISED_MINE_BOULDER_ALT){
				return 216 + 1;
			} else {
				return 216;
			}
		}

		return -1;
	}

	public static Image contentImage(int pos, int type) {
		if (type == Terrain.STATUE && Dungeon.level instanceof VaultLevel
				&& ((VaultLevel) Dungeon.level).customStatueCell(pos)) {
			for (CustomTilemap custom : Dungeon.level.customTerrain) {
				if (!(custom instanceof VaultTiles.EntranceDecorations)) continue;
				int tx = pos % Dungeon.level.width() - custom.tileX;
				int ty = pos / Dungeon.level.width() - custom.tileY;
				if (tx < 0 || ty < 1 || tx >= custom.tileW || ty >= custom.tileH) continue;
				int[] data = ((VaultTiles.EntranceDecorations) custom).tileData();
				int upper = data[tx + (ty - 1) * custom.tileW];
				if (upper < 0 || data[tx + ty * custom.tileW] != upper + 16) continue;
				// 使用专用图层的朝向和上下两格，检视不再叠普通雕像或截掉头部。
				return new Image(Assets.Environment.CITY_QUEST, upper % 16 * SIZE,
						upper / 16 * SIZE, SIZE, SIZE * 2);
			}
		}
		int visual = contentVisual(pos, type);
		if (visual < 0) return null;
		Image image = new Image(Assets.Environment.TERRAIN_FEATURES);
		image.frame((visual % 16) * SIZE, (visual / 16) * SIZE, SIZE, SIZE);
		if (type == Terrain.STATUE || type == Terrain.STATUE_SP) {
			Pixmap foreground = image.texture.bitmap;
			Pixmap terrain = TextureCache.get(Dungeon.level.tilesTex()).bitmap;
			// 普通与特殊地板雕像共用透明本体和阴影；特殊底图里的阴影并未按特殊地板着色。
			int raised = DungeonTileSheet.RAISED_STATUE;
			int floor = DungeonTileSheet.FLOOR;
			int[] pixels = new int[SIZE * SIZE];
			for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) {
				int rgba = foreground.getPixel(visual % 16 * SIZE + x, visual / 16 * SIZE + y);
				if ((rgba & 0xFF) == 0) {
					int ground = terrain.getPixel(floor % 16 * SIZE + x, floor / 16 * SIZE + y);
					int statue = terrain.getPixel(raised % 16 * SIZE + x, raised / 16 * SIZE + y);
					// 原图的落地阴影把地面压暗到 40%，转为透明黑色后可叠在任意地面上。
					boolean shadow = (ground & 0xFFFFFF00) != 0;
					for (int shift = 8; shift <= 24; shift += 8) {
						if (Math.abs(((ground >>> shift) & 0xFF) * 2 / 5
								- ((statue >>> shift) & 0xFF)) > 1) shadow = false;
					}
					if (shadow) rgba = 0x00000099;
				}
				pixels[x + y * SIZE] = rgba;
			}
			image.texture(TextureCache.createPixels("statue-with-shadow:" + Dungeon.level.tilesTex() + ':' + type + ':' + visual,
					SIZE, SIZE, pixels));
		}
		return image;
	}

	public static Image getTrapVisual( Trap trap ){
		if (instance == null) instance = new TerrainFeaturesTilemap(null, null);

		RectF uv = instance.tileset.get((trap.active ? trap.color : Trap.BLACK) + (trap.shape * 16));
		if (uv == null) return null;

		Image img = new Image( instance.texture );
		img.frame(uv);
		return img;
	}

	public static Image getPlantVisual( Plant plant ){
		if (instance == null) instance = new TerrainFeaturesTilemap(null, null);

		RectF uv = instance.tileset.get(plant.image + 7*16);
		if (uv == null) return null;

		Image img = new Image( instance.texture );
		img.frame(uv);
		return img;
	}

	public void growPlant( final int pos ){
		Plant target = plants == null ? null : plants.get(pos);
		if (target == null || Dungeon.level.cellAt(pos).buried()) return;
		final Image plant = getPlantVisual(target);
		if (plant == null) return;
		
		plant.origin.set( 8, 12 );
		plant.scale.set( 0 );
		plant.point( DungeonTilemap.tileToWorld( pos ) );

		parent.add( plant );

		parent.add( new ScaleTweener( plant, new PointF(1, 1), 0.2f ) {
			protected void onComplete() {
				plant.killAndErase();
				killAndErase();
				updateMapCell(pos);
			}
		} );
	}

}
