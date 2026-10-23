package com.shatteredpixel.shatteredpixeldungeon.tiles;

import com.badlogic.gdx.graphics.Pixmap;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.levels.CellContent;
import com.shatteredpixel.shatteredpixeldungeon.levels.CellState;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.AmbitiousImpRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.custom.Carpet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.custom.VaultTiles;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Image;
import com.watabou.utils.PointF;
import com.watabou.utils.RectF;

import java.util.ArrayList;

/** 检查窗口使用实际格子的叠图，不需要创建“灰烬地板”等组合类型。 */
public class CellPreview extends Image {
	private final ArrayList<Image> layers = new ArrayList<>();
	private final ArrayList<Image> fixedLayers = new ArrayList<>();
	private final int contentType;

	// 不指定内容时显示整格；指定类型时只显示该内容；传入图像时只做居中快照。
	public CellPreview(int cell, Object... content) {
		width = height = DungeonTilemap.SIZE;
		contentType = content.length > 0 && content[0] instanceof Integer ? (Integer) content[0] : -1;
		CellState state = Dungeon.level.cellAt(cell);
		if (content.length > 0 && content[0] instanceof Image) {
			layers.add((Image) content[0]);
		} else if (contentType >= 0) {
			if (contentType == CellContent.CARPET) {
				if (state.hasTerrain(CellContent.CARPET)) addCustom(cell, Dungeon.level.customTiles);
				addCarpetSeams(cell);
			} else if (contentType == Terrain.WATER) {
				layers.add(new Image(Dungeon.level.waterTex(), 0, 0, DungeonTilemap.SIZE, DungeonTilemap.SIZE));
			} else {
				Image image = TerrainFeaturesTilemap.contentImage(cell, contentType);
				if (image == null) {
					int visualType = contentType == Terrain.SECRET_DOOR ? Terrain.WALL : contentType;
					int visual = DungeonTileSheet.directFlatVisuals.get(visualType,
							DungeonTileSheet.directVisuals.get(visualType, DungeonTileSheet.FLOOR));
					image = new Image(Dungeon.level.tilesTex(), visual % 16 * DungeonTilemap.SIZE,
							visual / 16 * DungeonTilemap.SIZE, DungeonTilemap.SIZE, DungeonTilemap.SIZE);
				}
				layers.add(image);
			}
		} else {
			int terrain = state.terrain();
			boolean structure = DungeonTileSheet.wallStitcheable(terrain) || DungeonTileSheet.doorTile(terrain);
			if (state.hasTerrain(Terrain.WATER) && !structure) {
				Image water = new Image(Dungeon.level.waterTex());
				water.frame(0, 0, 16, 16);
				layers.add(water);
			} else if (structure) {
				int visual = DungeonTileSheet.directFlatVisuals.get(terrain, DungeonTileSheet.FLAT_WALL);
				layers.add(new Image(Dungeon.level.tilesTex(), visual % 16 * DungeonTilemap.SIZE,
						visual / 16 * DungeonTilemap.SIZE, DungeonTilemap.SIZE, DungeonTilemap.SIZE));
			} else layers.add(DungeonTerrainTilemap.tile(cell, state.ground()));
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
				layers.add(image);
			}
		}
		// 地面和自定义铺地图层保持拼接位置，物件按实际非透明范围居中。
		for (Image image : layers) {
			image.scale = new PointF(1, 1);
			if (fixedLayers.contains(image) || image instanceof ItemSprite || image.texture == null) continue;
			RectF frame = image.frame();
			int left = Math.round(frame.left * image.texture.width);
			int top = Math.round(frame.top * image.texture.height);
			int right = Math.round(frame.right * image.texture.width);
			int bottom = Math.round(frame.bottom * image.texture.height);
			int minX = right, minY = bottom, maxX = left - 1, maxY = top - 1;
			for (int py = top; py < bottom; py++) for (int px = left; px < right; px++) {
				if ((image.texture.bitmap.getPixel(px, py) & 0xFF) == 0) continue;
				minX = Math.min(minX, px);
				minY = Math.min(minY, py);
				maxX = Math.max(maxX, px);
				maxY = Math.max(maxY, py);
			}
			if (maxX >= minX && maxY >= minY) image.frame(minX, minY, maxX - minX + 1, maxY - minY + 1);
		}
	}

	private void addCustom(int cell, Iterable<CustomTilemap> tilemaps) {
		int x = cell % Dungeon.level.width(), y = cell / Dungeon.level.width();
		for (CustomTilemap tile : tilemaps) {
			// 宝库雕像已由 contentImage 提供完整图像，不再额外叠下半格。
			if (tile instanceof VaultTiles.EntranceDecorations) continue;
			if (contentType >= 0 && !(tile instanceof Carpet) && !(tile instanceof VaultTiles.EntranceRug)
					&& !(tile instanceof AmbitiousImpRoom.QuestEntrance)) continue;
			if (tile instanceof Carpet && !Dungeon.level.cellAt(cell).hasTerrain(CellContent.CARPET)) continue;
			int tx = x - tile.tileX, ty = y - tile.tileY;
			if (tx >= 0 && ty >= 0 && tx < tile.tileW && ty < tile.tileH) {
				Image image = tile.image(tx, ty);
				if (image != null) {
					if (tile instanceof Carpet) {
						// 基座和楼梯的地图专用图块仍带有物件；检视时只取地毯底图。
						RectF frame = image.frame();
						int visual = Math.round(frame.left * image.texture.width) / DungeonTilemap.SIZE
								+ Math.round(frame.top * image.texture.height) / DungeonTilemap.SIZE * 16;
						int carpetVisual;
						switch (visual) {
							case Carpet.CITY_PEDESTAL: case Carpet.CITY_ENTRANCE:
								carpetVisual = 48; break;
							case Carpet.CITY_PEDESTAL_TR:
								carpetVisual = 48 + 3; break;
							case Carpet.CITY_PEDESTAL_TL:
								carpetVisual = 48 + 9; break;
							default:
								carpetVisual = visual;
						}
						image.frame(carpetVisual % 16 * DungeonTilemap.SIZE,
								carpetVisual / 16 * DungeonTilemap.SIZE, DungeonTilemap.SIZE, DungeonTilemap.SIZE);
					}
					layers.add(image);
					fixedLayers.add(image);
				}
			}
		}
	}

	private void addCarpetSeams(int cell) {
		int[] pixels = new int[DungeonTilemap.SIZE * DungeonTilemap.SIZE];
		StringBuilder key = new StringBuilder("cell-preview-carpet");
		Pixmap atlas = TextureCache.get(Assets.Environment.CARPET).bitmap;
		boolean visible = false;
		for (int side = 0; side < Carpet.SEAM_PIECES; side++) {
			int piece = Carpet.seamPiece(cell, side);
			key.append(':').append(piece);
			if (piece < 0) continue;
			visible = true;
			int tx = piece % 16 * DungeonTilemap.SIZE, ty = piece / 16 * DungeonTilemap.SIZE;
			for (int py = 0; py < DungeonTilemap.SIZE; py++) for (int px = 0; px < DungeonTilemap.SIZE; px++) {
				int rgba = atlas.getPixel(tx + px, ty + py);
				if ((rgba & 0xFF) != 0) pixels[px + py * DungeonTilemap.SIZE] = rgba;
			}
		}
		// 保留完整格子的透明留白，单侧残边仍贴在原来的格边。
		if (visible) {
			Image image = new Image(TextureCache.createPixels(key.toString(),
					DungeonTilemap.SIZE, DungeonTilemap.SIZE, pixels));
			layers.add(image);
			fixedLayers.add(image);
		}
	}

	@Override public void draw() {
		for (Image image : layers) {
			float fit = Math.min(1f, DungeonTilemap.SIZE / Math.max(image.width, image.height));
			image.scale.set(scale).scale(fit);
			image.x = x + (width * scale.x - image.width()) / 2;
			image.y = y + (height * scale.y - image.height()) / 2;
			image.camera = camera();
			image.draw();
		}
	}

	@Override public void destroy() {
		for (Image image : layers) image.destroy();
		layers.clear();
		fixedLayers.clear();
		super.destroy();
	}
}
