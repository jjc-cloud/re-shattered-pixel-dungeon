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

	private int type;
	private volatile int properties;
	private int requiredSupport = SUPPORTS_COVER;

	public CellContent() { }

	public CellContent(int type, int region) {
		this.type = type;
		if (type == HIDDEN_ENTRANCE) { properties = BREAKABLE; return; }
		if (type == CARPET) {
			properties = BURNABLE | BREAKABLE | SURFACE_COVER;
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
			case Terrain.STATUE: case Terrain.STATUE_SP:
			case Terrain.COLLAPSE_WALL:
				properties |= BREAKABLE;
				break;
			case Terrain.REGION_DECO: case Terrain.REGION_DECO_ALT:
			case Terrain.SEWER_BARREL_MARKED: case Terrain.SEWER_BARREL_MARKED_ALT:
				if (region == 0) properties |= BURNABLE | BREAKABLE;
				else if (region == 1 || region == 2) properties |= CONDUCTIVE;
				else if (region == 4) properties |= BREAKABLE;
				break;
		}
	}

	public int type() { return type; }
	public int properties() { return properties; }
	public int requiredSupport() { return requiredSupport; }
	public boolean has(int property) { return (properties & property) != 0; }
	public void properties(int add, int remove) { properties = (properties | add) & ~remove; }

	public int flags() {
		int flags = type >= CARPET ? Terrain.PASSABLE : Terrain.flags[type];
		return has(BURNABLE) ? flags | Terrain.FLAMABLE : flags & ~Terrain.FLAMABLE;
	}

	/** 同一种内容的状态共享一个类别，不影响其他内容。 */
	public static int family(int type) {
		switch (type) {
			case Terrain.GRASS: case Terrain.HIGH_GRASS: case Terrain.FURROWED_GRASS:
				return Terrain.GRASS;
			case Terrain.DOOR: case Terrain.OPEN_DOOR: case Terrain.LOCKED_DOOR:
			case Terrain.HERO_LKD_DR: case Terrain.CRYSTAL_DOOR: case Terrain.SECRET_DOOR:
				return Terrain.DOOR;
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
		if (burned && has(BURNABLE)) level.cellAt(cell).addContent(new CellContent(Terrain.EMBERS, level.region()));
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
	}
}
