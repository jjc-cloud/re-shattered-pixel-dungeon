package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Gem;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/** 格子中独立存在的内容。属性描述内容本身，不保存动作白名单或目标地形编号。 */
public class CellContent implements Bundlable {

	public static final int BURNABLE = 1;
	public static final int BREAKABLE = 2;
	public static final int CONDUCTIVE = 4;
	public static final int SUPPORTS_COVER = 8;
	public static final int GROWABLE = 16;
	public static final int COVERS_CONTENTS = 32;
	public static final int OCCUPIES_SPACE = 64;
	public static final int SURFACE_COVER = 128;
	public static final int FLOODABLE = 512;
	public static final int CARPET = 256;
	public static final int HIDDEN_ENTRANCE = 257;

	// 作用来源按位区分：点击、爆炸、解离、冲击波各自只查自己那一位。
	// BREAKABLE 继续表示投射物/法杖等通用物理破坏。
	public static final int CLICK_BREAKABLE = 1024;
	public static final int EXPLOSION_BREAKABLE = 2048;
	public static final int DISINTEGRATION_BREAKABLE = 4096;
	public static final int SHOCKWAVE_BREAKABLE = 8192;
	// 被破坏时留下灰烬，不等同于可燃（地毯不可烧但仍会留灰烬）。
	public static final int LEAVES_EMBERS = 16384;
	public static final int OVERHEAD = 32768;
	public static final int BLOCKS_PROJECTILES = 65536;
	public static final int PASSES_NON_PROJECTILES = 131072;
	// 解离射线不破坏：雕像/台柱仍吃爆炸、冲击波、投射物等其他来源。
	public static final int DISINTEGRATION_PROOF = 262144;

	protected volatile int type;
	private volatile int properties;
	private int requiredSupport = SUPPORTS_COVER;

	public CellContent() { }

	public CellContent(int type, int region) {
		this.type = type;
		if (type == HIDDEN_ENTRANCE) {
			properties = EXPLOSION_BREAKABLE | SHOCKWAVE_BREAKABLE;
			return;
		}
		if (type == CARPET) {
			properties = SURFACE_COVER | EXPLOSION_BREAKABLE | DISINTEGRATION_BREAKABLE | LEAVES_EMBERS;
			return;
		}
		if ((Terrain.flags[type] & Terrain.SOLID) != 0 || family(type) == Terrain.DOOR
				|| family(type) == Terrain.TRAP) properties |= OCCUPIES_SPACE;
		if (family(type) == Terrain.GRASS) {
			requiredSupport = GROWABLE;
			properties |= SURFACE_COVER;
		} else if ((Terrain.flags[type] & Terrain.LIQUID) != 0) {
			requiredSupport = FLOODABLE;
			properties |= SURFACE_COVER;
		} else if (type == Terrain.EMBERS) properties |= SURFACE_COVER;
		if ((Terrain.flags[type] & Terrain.FLAMABLE) != 0) properties |= BURNABLE;
		if (type == Terrain.WALL || type == Terrain.WALL_DECO || type == Terrain.SECRET_DOOR || type == Terrain.COLLAPSE_WALL) properties |= COVERS_CONTENTS;
		switch (type) {
			case Terrain.BARRICADE: case Terrain.BOOKSHELF:
			case Terrain.COLLAPSE_WALL:
				properties |= BREAKABLE;
				break;
			case Terrain.STATUE: case Terrain.STATUE_SP:
				properties |= BREAKABLE | DISINTEGRATION_PROOF;
				break;
			case Terrain.REGION_DECO: case Terrain.REGION_DECO_ALT:
			case Terrain.SEWER_BARREL_MARKED: case Terrain.SEWER_BARREL_MARKED_ALT:
				if (region == 0) properties |= BURNABLE | BREAKABLE | CLICK_BREAKABLE;
				else if (region == 1) properties |= CONDUCTIVE;
				else if (region == 2) properties |= CONDUCTIVE | OVERHEAD | BLOCKS_PROJECTILES;
				else if (region == 4) properties |= BREAKABLE | CLICK_BREAKABLE;
				break;
		}
		// 塌方岩壁保留点击挖掘；路障/书架/雕像只响应非点击的破坏来源。
		if (type == Terrain.COLLAPSE_WALL) properties |= CLICK_BREAKABLE;
	}

	public int type() { return type; }
	public int properties() { return properties; }
	public int requiredSupport() { return requiredSupport; }
	public boolean has(int property) { return (properties() & property) != 0; }
	public void properties(int add, int remove) { properties = (properties | add) & ~remove; }

	public int flags() {
		int flags = type >= CARPET ? Terrain.PASSABLE : Terrain.flags[type];
		// 架空设施允许从下方通过，实体弹道遮挡由发射位置另行判定。
		if (has(OVERHEAD)) flags = (flags | Terrain.PASSABLE) & ~(Terrain.SOLID | Terrain.LOS_BLOCKING);
		return has(BURNABLE) ? flags | Terrain.FLAMABLE : flags & ~Terrain.FLAMABLE;
	}

	/** 同一种内容的状态共享一个类别，不影响其他内容。 */
	public static int family(int type) {
		if (com.shatteredpixel.shatteredpixeldungeon.levels.features.Door.state(type) != null) return Terrain.DOOR;
		switch (type) {
			case Terrain.GRASS: case Terrain.HIGH_GRASS: case Terrain.FURROWED_GRASS:
				return Terrain.GRASS;
			case Terrain.WALL: case Terrain.WALL_DECO: case Terrain.COLLAPSE_WALL:
				return Terrain.WALL;
			case Terrain.TRAP: case Terrain.SECRET_TRAP: case Terrain.INACTIVE_TRAP:
				return Terrain.TRAP;
			case Terrain.STATUE: case Terrain.STATUE_SP:
				return Terrain.STATUE;
			case Terrain.REGION_DECO: case Terrain.REGION_DECO_ALT:
			case Terrain.SEWER_BARREL_MARKED: case Terrain.SEWER_BARREL_MARKED_ALT:
				return Terrain.REGION_DECO;
			default: return type;
		}
	}

	/** 放置顺序和接触顺序由内容性质决定，灰烬不会遮住同格物件。 */
	public int layer() {
		if (type == Terrain.COLLAPSE_WALL) return 110;
		if (has(COVERS_CONTENTS)) return 100;
		if (type == Terrain.WATER) return 5;
		if (type == CARPET) return 10;
		if (type == Terrain.EMBERS) return 20;
		if (family(type) == Terrain.GRASS || family(type) == Terrain.TRAP) return 30;
		return 40;
	}

	/** 内容已从格子移除后结算自身产物；不会把整个格子替换成另一种地形。 */
	public void destroyed(Level level, int cell, boolean burned) {
		if (type == HIDDEN_ENTRANCE) { level.revealHiddenEntrance(cell); return; }
		if ((type == Terrain.WALL || type == Terrain.WALL_DECO) && level == Dungeon.level
				&& level.heroFOV[cell] && Game.scene() instanceof GameScene) {
			CellEmitter.get(cell).burst(Speck.factory(Speck.ROCK), 2);
		}
		if ((burned && has(BURNABLE)) || has(LEAVES_EMBERS))
			level.cellAt(cell).addContent(new CellContent(Terrain.EMBERS, level.region()));
		if (family(type) == Terrain.REGION_DECO && level.region() == 0) {
			if (type == Terrain.REGION_DECO || type == Terrain.SEWER_BARREL_MARKED) {
				level.cellAt(cell).addContent(new CellContent(Terrain.WATER, level.region()));
			}
			if (level == Dungeon.level) Splash.at(cell, 0xFF8A5A2E, 8);
			if (type == Terrain.SEWER_BARREL_MARKED || type == Terrain.SEWER_BARREL_MARKED_ALT) {
				Heap heap = level.drop(Random.Int(4) == 0 ? Generator.random() : Gem.generateTreasure(), cell);
				heap.autoExplored = true;
				if (heap.sprite != null) heap.sprite.drop();
			}
		} else if (family(type) == Terrain.REGION_DECO && level.region() == 4 && level == Dungeon.level) {
			Splash.at(cell, 0xFF958472, 8);
		}
	}

	@Override public void storeInBundle(Bundle bundle) {
		bundle.put("type", type);
		bundle.put("properties", properties);
		bundle.put("required_support", requiredSupport);
	}

	@Override public void restoreFromBundle(Bundle bundle) {
		type = bundle.getInt("type");
		properties = bundle.getInt("properties");
		requiredSupport = bundle.getInt("required_support");
		//旧存档补齐按来源区分的属性位；新存档写入的已经是新值，重复执行无副作用
		if (type == CARPET) {
			properties = SURFACE_COVER | EXPLOSION_BREAKABLE | DISINTEGRATION_BREAKABLE | LEAVES_EMBERS;
		} else if (type == HIDDEN_ENTRANCE) {
			properties = EXPLOSION_BREAKABLE | SHOCKWAVE_BREAKABLE;
		} else if (type == Terrain.COLLAPSE_WALL) {
			properties |= CLICK_BREAKABLE;
		} else if ((type == Terrain.REGION_DECO || type == Terrain.REGION_DECO_ALT
				|| type == Terrain.SEWER_BARREL_MARKED || type == Terrain.SEWER_BARREL_MARKED_ALT)
				&& has(BREAKABLE)) {
			properties |= CLICK_BREAKABLE;
		}
	}
}
