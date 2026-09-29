package com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.TerrainInteractions;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

/** 入口墙后是两格直道，再接中央带基座的 3×3 宝物区。 */
public class SuperSecretRoom extends SecretRoom {

	private static final TerrainInteractions.Rule ENTRANCE_RULE = TerrainInteractions.Rule.NONE
			.on(TerrainInteractions.Source.EXPLOSION, TerrainInteractions.Response.replaceWith(Terrain.EMPTY))
			.on(TerrainInteractions.Source.DISINTEGRATION, TerrainInteractions.Response.replaceWith(Terrain.EMPTY))
			.on(TerrainInteractions.Source.SHOCKWAVE, TerrainInteractions.Response.replaceWith(Terrain.EMPTY));

	@Override public int minWidth() { return 5; }
	@Override public int maxWidth() { return 7; }
	@Override public int minHeight() { return 5; }
	@Override public int maxHeight() { return 7; }

	@Override
	public boolean setSize() {
		return setSizeWithLimit(7, 7);
	}

	@Override
	public boolean setSizeWithLimit(int w, int h) {
		boolean horizontal = w >= 7 && h >= 5;
		boolean vertical = w >= 5 && h >= 7;
		if (!horizontal && !vertical) return false;
		if (horizontal && (!vertical || Random.Int(2) == 0)) {
			resize(6, 4);
		} else {
			resize(4, 6);
		}
		return true;
	}

	@Override
	public boolean canConnect(Point p) {
		if (width() == 7) {
			return (p.x == left || p.x == right) && p.y == (top + bottom) / 2;
		} else {
			return (p.y == top || p.y == bottom) && p.x == (left + right) / 2;
		}
	}

	@Override
	public void paint(Level level) {
		Painter.fill(level, this, Terrain.WALL);
		Door door = entrance();
		door.set(Door.Type.WALL);
		Painter.set(level, pointInside(door, 1), Terrain.EMPTY);
		Painter.set(level, pointInside(door, 2), Terrain.EMPTY);

		Point pedestal = pointInside(door, 4);
		Painter.fill(level, pedestal.x - 1, pedestal.y - 1, 3, 3, Terrain.EMPTY_SP);
		Painter.set(level, pedestal, Terrain.PEDESTAL);
		level.drop(createPrize(), level.pointToCell(pedestal)).autoExplored = true;
		for (int y = -1; y <= 1; y++) {
			for (int x = -1; x <= 1; x++) {
				if (x != 0 || y != 0) {
					level.drop(new Gold().random(), level.pointToCell(new Point(pedestal.x + x, pedestal.y + y))).autoExplored = true;
				}
			}
		}
		level.interactions().setOverride(level.pointToCell(door), ENTRANCE_RULE);
	}

	private Item createPrize() {
		Item prize;
		switch (Random.Int(4)) {
			case 0:
				// 与隐藏迷宫房相同：使用下一区域的阶数概率及默认武器池。
				prize = Generator.randomWeapon(Dungeon.depth / 5 + 1, true);
				if (((Weapon) prize).hasCurseEnchant()) ((Weapon) prize).enchant(null);
				break;
			case 1:
				prize = Generator.randomArmor(Dungeon.depth / 5 + 1);
				if (((Armor) prize).hasCurseGlyph()) ((Armor) prize).inscribe(null);
				break;
			case 2:
				prize = Generator.randomUsingDefaults(Generator.Category.WAND);
				break;
			default:
				prize = Generator.randomUsingDefaults(Generator.Category.RING);
				break;
		}
		prize.cursed = false;
		prize.cursedKnown = true;
		// 覆盖生成器自带的强化等级，保证最终等级严格为 +1 / +2 / +3。
		prize.level(0);
		prize.upgrade(1 + Random.chances(new float[]{5, 3, 2}));
		return prize;
	}

	@Override public boolean canPlaceWater(Point p) { return false; }
	@Override public boolean canPlaceGrass(Point p) { return false; }
	@Override public boolean canPlaceTrap(Point p) { return false; }
	@Override public boolean canPlaceItem(Point p, Level l) { return false; }
	@Override public boolean canPlaceCharacter(Point p, Level l) { return false; }
}
