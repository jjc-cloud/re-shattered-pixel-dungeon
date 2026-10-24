package com.shatteredpixel.shatteredpixeldungeon.tiles;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.tiles.custom.Carpet;
import com.watabou.noosa.Group;

import java.util.ArrayList;

/** 各种内容独立绘制，灰烬、植物、陷阱和物件不再争用同一个图块。 */
public class CellFeatures extends Group {
	static final int[] CONTENT_TYPES = {Terrain.EMBERS, Terrain.GRASS, Terrain.HIGH_GRASS, Terrain.FURROWED_GRASS,
			Terrain.BARRICADE, Terrain.ALCHEMY, Terrain.STATUE, Terrain.STATUE_SP,
			Terrain.REGION_DECO, Terrain.REGION_DECO_ALT, Terrain.MINE_CRYSTAL, Terrain.MINE_BOULDER};
	private final ArrayList<DungeonTilemap> layers = new ArrayList<>();
	private final TerrainFeaturesTilemap plants;
	public final Group overhead = new Group();

	public CellFeatures() {
		// 地毯的四条破损边和四个角各一层，先铺在地面内容之下。
		for (int side = 0; side < Carpet.SEAM_PIECES; side++) {
			CarpetSeamTilemap seam = new CarpetSeamTilemap(side);
			layers.add(seam);
			add(seam);
		}
		int[] types = java.util.Arrays.copyOf(CONTENT_TYPES, CONTENT_TYPES.length + 2);
		types[types.length - 2] = -2;
		types[types.length - 1] = -3;
		for (int type : types) {
			TerrainFeaturesTilemap layer = new TerrainFeaturesTilemap(Dungeon.level.plants, Dungeon.level.traps, type);
			layers.add(layer);
			add(layer);
		}
		plants = (TerrainFeaturesTilemap) layers.get(layers.size() - 1);
		// 架构上下两部分共用现有贴图与地图刷新，绘制组沿用高草前景的层级。
		for (int type : new int[]{TerrainFeaturesTilemap.OVERHEAD_BODY, TerrainFeaturesTilemap.OVERHEAD_TOP}) {
			TerrainFeaturesTilemap layer = new TerrainFeaturesTilemap(null, null, type);
			layers.add(layer);
			overhead.add(layer);
		}
	}

	public void map(int[] map, int width) { for (DungeonTilemap layer : layers) layer.map(map, width); }
	public void updateMap() { for (DungeonTilemap layer : layers) layer.updateMap(); }
	public void updateMapCell(int cell) { for (DungeonTilemap layer : layers) layer.updateMapCell(cell); }
	public void growPlant(int cell) { plants.growPlant(cell); }
}
