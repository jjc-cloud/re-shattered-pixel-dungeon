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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.Tilemap;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.GameMath;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;

public abstract class DungeonTilemap extends Tilemap {

	public static final int SIZE = 16;

	protected int[] map;
	protected int collapseVisualOffset;

	public DungeonTilemap(String tex) {
		super(tex, new TextureFilm( tex, SIZE, SIZE ) );
		int color = Terrain.COLLAPSE_WALL_COLOR & 0xFFFFFF;
		if (color != 0xFFFFFF && Dungeon.level.caveCollapse != null
				&& (this instanceof DungeonTerrainTilemap || this instanceof DungeonWallsTilemap)) {
			int width = texture.width, height = texture.height;
			int[] pixels = new int[width * height * 2];
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int pixel = texture.bitmap.getPixel(x, y);
					int red = (pixel >>> 24) * (color >>> 16) / 255;
					int green = ((pixel >>> 16) & 0xFF) * ((color >>> 8) & 0xFF) / 255;
					int blue = ((pixel >>> 8) & 0xFF) * (color & 0xFF) / 255;
					pixels[x + y * width] = pixel;
					pixels[x + (y + height) * width] = (red << 24) | (green << 16) | (blue << 8) | (pixel & 0xFF);
				}
			}
			// 上半张保留原贴图，下半张存染色副本，只有塌方墙的绘制编号使用副本。
			collapseVisualOffset = (width / SIZE) * (height / SIZE);
			texture = TextureCache.createPixels(tex + "#collapse-wall:" + color, width, height * 2, pixels);
			tileset = new TextureFilm(texture, SIZE, SIZE);
		}
	}

	@Override
	//we need to retain two arrays, map is the dungeon tilemap which we can reference.
	// Data is our own internal image representation of the tiles, which may differ.
	public void map(int[] data, int cols) {
		map = data;
		super.map(new int[data.length], cols);
	}

	@Override
	public synchronized void updateMap() {
		for (int i = 0; i < data.length; i++)
			data[i] = getTileVisual(i ,map[i], false);
		super.updateMap();
	}

	@Override
	public synchronized void updateMapCell(int cell) {
		//update in a 3x3 grid to account for neighbours which might also be affected
		if (Dungeon.level.insideMap(cell)) {
			for (int i : PathFinder.NEIGHBOURS9) {
				data[cell + i] = getTileVisual(cell + i, map[cell + i], false);
			}
			super.updateMapCell(cell - mapWidth - 1);
			super.updateMapCell(cell + mapWidth + 1);

		//unless we're at the level's edge, then just do the one tile.
		} else {
			data[cell] = getTileVisual(cell, map[cell], false);
			super.updateMapCell(cell);
		}
	}

	protected abstract int getTileVisual(int pos, int tile, boolean flat);

	public int screenToTile(int x, int y ){
		return screenToTile(x, y, false);
	}

	public int screenToTile(int x, int y, boolean wallAssist ) {
		PointF p = camera().screenToCamera( x, y ).
			offset( this.point().negate() ).
			invScale( SIZE );
		
		//snap to the edges of the tilemap
		p.x = GameMath.gate(0, p.x, Dungeon.level.width()-0.001f);
		p.y = GameMath.gate(0, p.y, Dungeon.level.height()-0.001f);

		int cell = (int)p.x + (int)p.y * Dungeon.level.width();

		//wall assist is used to make raised perspective tapping a bit easier.
		// If the pressed tile is a wall tile, the tap can be 'bumped' down into a none-wall tile.
		// currently this happens if the bottom 1/4 of the wall tile is pressed.
		if (wallAssist
				&& map != null
				&& isWallAssistable(cell)){

			if (cell + mapWidth < size
					&& p.y % 1 >= 0.75f
					&& !isWallAssistable(cell + mapWidth)){
				cell += mapWidth;
			}

		}

		return cell;
	}

	private boolean isWallAssistable(int cell){
		if (map == null || cell >= size){
			return false;
		}

		if (DungeonTileSheet.wallStitcheable(map[cell])){
			return true;
		}

		//caves region deco is very wall-like, so it counts
		if (Dungeon.depth >= 10 && Dungeon.depth <= 15
				&& (map[cell] == Terrain.REGION_DECO || map[cell] == Terrain.REGION_DECO_ALT)) {
			return true;
		}

		return false;
	}
	
	@Override
	public boolean overlapsPoint( float x, float y ) {
		return true;
	}
	
	public void discover( int pos, int oldValue ) {
		
		int visual = getTileVisual( pos, oldValue, false);
		if (visual < 0) return;
		
		final Image tile = new Image( texture );
		tile.frame( tileset.get( getTileVisual( pos, oldValue, false)));
		tile.point( tileToWorld( pos ) );

		parent.add( tile );
		
		parent.add( new AlphaTweener( tile, 0, 0.6f ) {
			protected void onComplete() {
				tile.killAndErase();
				killAndErase();
			}
		} );
	}
	
	public static PointF tileToWorld( int pos ) {
		return new PointF( pos % Dungeon.level.width(), pos / Dungeon.level.width()  ).scale( SIZE );
	}
	
	public static PointF tileCenterToWorld( int pos ) {
		return new PointF(
			(pos % Dungeon.level.width() + 0.5f) * SIZE,
			(pos / Dungeon.level.width() + 0.5f) * SIZE );
	}

	public static PointF raisedTileCenterToWorld( int pos ) {
		return new PointF(
				(pos % Dungeon.level.width() + 0.5f) * SIZE,
				(pos / Dungeon.level.width() + 0.1f) * SIZE );
	}

	public static int worldToTile( float x, float y, int width){
		return (int)(x / SIZE) + ((int)(y / SIZE) * width);
	}
	
	@Override
	public boolean overlapsScreenPoint( int x, int y ) {
		return true;
	}

}
