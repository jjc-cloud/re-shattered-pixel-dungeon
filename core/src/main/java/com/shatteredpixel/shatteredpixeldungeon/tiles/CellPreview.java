package com.shatteredpixel.shatteredpixeldungeon.tiles;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.levels.CellContent;
import com.shatteredpixel.shatteredpixeldungeon.levels.CellState;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.custom.Carpet;
import com.watabou.noosa.Image;

import java.util.ArrayList;

/** 检查窗口使用实际格子的叠图，不需要创建“灰烬地板”等组合类型。 */
public class CellPreview extends Image {
	private final ArrayList<Image> layers = new ArrayList<>();

	public CellPreview(int cell) {
		width = height = DungeonTilemap.SIZE;
		CellState state = Dungeon.level.cellAt(cell);
		int terrain = state.terrain();
		boolean structure = DungeonTileSheet.wallStitcheable(terrain) || DungeonTileSheet.doorTile(terrain);
		if (state.hasTerrain(Terrain.WATER) && !structure) {
			Image water = new Image(Dungeon.level.waterTex());
			water.frame(0, 0, 16, 16);
			layers.add(water);
		} else layers.add(DungeonTerrainTilemap.tile(cell, structure ? terrain : state.ground()));
		if (!structure && !CellState.isGround(terrain) && terrain != Terrain.WATER
				&& CellContent.family(terrain) != Terrain.TRAP
				&& TerrainFeaturesTilemap.contentVisual(cell, terrain) < 0) {
			layers.add(DungeonTerrainTilemap.tile(cell, terrain));
		}
		addCustom(cell, Dungeon.level.customTiles);
		addCarpetSeams(cell);
		if (!state.buried()) {
			for (int type : CellFeatures.CONTENT_TYPES) if (state.hasTerrain(type)) {
				Image image = TerrainFeaturesTilemap.contentImage(cell, type);
				if (image != null) layers.add(image);
			}
			Trap trap = Dungeon.level.traps.get(cell);
			if (trap != null && trap.visible) layers.add(TerrainFeaturesTilemap.getTrapVisual(trap));
			Plant plant = Dungeon.level.plants.get(cell);
			if (plant != null) layers.add(TerrainFeaturesTilemap.getPlantVisual(plant));
		}
		addCustom(cell, Dungeon.level.customTerrain);
		Heap heap = Dungeon.level.heaps.get(cell);
		if (heap != null && heap.seen && !state.buried()) {
			ItemSprite image = new ItemSprite();
			image.view(heap);
			layers.add(image);
		}
		Char ch = Actor.findChar(cell);
		if (ch != null && ch.sprite != null && Dungeon.level.heroFOV[cell] && ch.sprite.visible) {
			Image image = new Image(ch.sprite);
			image.scale = new com.watabou.utils.PointF(1, 1);
			layers.add(image);
		}
	}

	private void addCustom(int cell, Iterable<CustomTilemap> tilemaps) {
		int x = cell % Dungeon.level.width(), y = cell / Dungeon.level.width();
		for (CustomTilemap tile : tilemaps) {
			int tx = x - tile.tileX, ty = y - tile.tileY;
			if (tx >= 0 && ty >= 0 && tx < tile.tileW && ty < tile.tileH) {
				Image image = tile.image(tx, ty);
				if (image != null) layers.add(image);
			}
		}
	}

	private void addCarpetSeams(int cell) {
		for (int side = 0; side < 4; side++) {
			int piece = Carpet.seamPiece(cell, side);
			if (piece < 0) continue;
			Image seam = new Image(Assets.Environment.CARPET);
			seam.frame((piece % 16) * DungeonTilemap.SIZE, (piece / 16) * DungeonTilemap.SIZE,
					DungeonTilemap.SIZE, DungeonTilemap.SIZE);
			layers.add(seam);
		}
	}

	@Override public void draw() {
		for (Image image : layers) {
			image.x = x;
			image.y = y;
			image.scale.set(scale);
			image.camera = camera();
			image.draw();
		}
	}

	@Override public void destroy() {
		for (Image image : layers) image.destroy();
		layers.clear();
		super.destroy();
	}
}
