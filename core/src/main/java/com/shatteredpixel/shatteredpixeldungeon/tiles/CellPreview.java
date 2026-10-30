package com.shatteredpixel.shatteredpixeldungeon.tiles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.levels.CellContent;
import com.shatteredpixel.shatteredpixeldungeon.levels.CellState;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.AmbitiousImpRoom;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.custom.Carpet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.custom.VaultTiles;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Image;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.utils.PointF;
import com.watabou.utils.RectF;

import java.util.ArrayList;

/** 顶部快照截取地图实际渲染帧，单项查询图标仍独立取图。 */
public class CellPreview extends Image {
	private static final ArrayList<CellPreview> pendingSnapshots = new ArrayList<>();
	private final ArrayList<Image> layers = new ArrayList<>();
	private final ArrayList<Image> fixedLayers = new ArrayList<>();
	private final int contentType;
	private int snapshotCell = -1;
	private Level snapshotLevel;

	// 不指定内容时请求实际帧；指定内容或图像时只生成独立查询图标。
	public CellPreview(int cell, Object... content) {
		width = height = DungeonTilemap.SIZE;
		contentType = content.length > 0 && content[0] instanceof Integer ? (Integer) content[0] : -1;
		if (content.length == 0) {
			snapshotCell = cell;
			snapshotLevel = Dungeon.level;
			height += 4; // 雕像等物件向本格上方伸出的部分也保留。
			synchronized (pendingSnapshots) {
				pendingSnapshots.add(this);
			}
			return;
		}
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

	/** 仅在地图画完、界面画出前由渲染线程调用，每个窗口只截一次。 */
	public static void capturePending() {
		ArrayList<CellPreview> requests;
		synchronized (pendingSnapshots) {
			if (pendingSnapshots.isEmpty()) return;
			requests = new ArrayList<>(pendingSnapshots);
			pendingSnapshots.clear();
		}
		Camera camera = Camera.main;
		int bufferWidth = Gdx.graphics.getBackBufferWidth();
		int bufferHeight = Gdx.graphics.getBackBufferHeight();
		float pixelScaleX = bufferWidth / (float) Game.width;
		float pixelScaleY = bufferHeight / (float) Game.height;
		for (CellPreview preview : requests) {
			if (!preview.alive || preview.snapshotLevel != Dungeon.level) continue;
			float worldX = preview.snapshotCell % Dungeon.level.width() * DungeonTilemap.SIZE;
			float worldY = preview.snapshotCell / Dungeon.level.width() * DungeonTilemap.SIZE - 4;
			// 直接使用这一帧的相机矩阵，缩放、平移和震屏都与地图一致。
			int left = Math.round((worldX * camera.matrix[0] + camera.matrix[12] + 1) * bufferWidth / 2);
			int top = Math.round((1 - worldY * camera.matrix[5] - camera.matrix[13]) * bufferHeight / 2);
			int width = Math.max(1, Math.round(preview.width * camera.zoom * pixelScaleX));
			int height = Math.max(1, Math.round(preview.height * camera.zoom * pixelScaleY));
			int clipLeft = Math.max(left, Math.max(0, Math.round(camera.x * pixelScaleX)));
			int clipTop = Math.max(top, Math.max(0, Math.round(camera.y * pixelScaleY)));
			int clipRight = Math.min(left + width, Math.min(bufferWidth,
					Math.round((camera.x + camera.screenWidth()) * pixelScaleX)));
			int clipBottom = Math.min(top + height, Math.min(bufferHeight,
					Math.round((camera.y + camera.screenHeight()) * pixelScaleY)));
			int[] pixels = new int[width * height];
			java.util.Arrays.fill(pixels, 0x000000FF);
			if (clipRight > clipLeft && clipBottom > clipTop) {
				Pixmap frame = Pixmap.createFromFrameBuffer(clipLeft, bufferHeight - clipBottom,
						clipRight - clipLeft, clipBottom - clipTop);
				try {
					for (int y = 0; y < frame.getHeight(); y++) for (int x = 0; x < frame.getWidth(); x++) {
						// OpenGL 从底向上读回，转成图像的从顶向下行序。
						pixels[x + clipLeft - left + (y + clipTop - top) * width]
								= frame.getPixel(x, frame.getHeight() - 1 - y) | 0xFF;
					}
				} finally {
					frame.dispose();
				}
			}
			preview.layers.add(new Image(TextureCache.createPixels(preview, width, height, pixels)));
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
			if (snapshotCell >= 0) {
				image.scale.set(scale.x * width / image.width, scale.y * height / image.height);
				image.x = x;
				image.y = y;
				image.camera = camera();
				image.draw();
				continue;
			}
			float fit = Math.min(1f, DungeonTilemap.SIZE / Math.max(image.width, image.height));
			image.scale.set(scale).scale(fit);
			image.x = x + (width * scale.x - image.width()) / 2;
			image.y = y + (height * scale.y - image.height()) / 2;
			image.camera = camera();
			image.draw();
		}
	}

	@Override public void destroy() {
		synchronized (pendingSnapshots) {
			pendingSnapshots.remove(this);
		}
		for (Image image : layers) image.destroy();
		layers.clear();
		fixedLayers.clear();
		if (snapshotCell >= 0) TextureCache.remove(this);
		super.destroy();
	}
}
