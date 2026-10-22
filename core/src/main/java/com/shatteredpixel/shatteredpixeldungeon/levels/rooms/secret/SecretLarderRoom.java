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

package com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.items.food.ChargrilledMeat;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Pasty;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.CellPlacement;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.plants.BlandfruitBush;
import com.watabou.utils.Point;

public class SecretLarderRoom extends SecretRoom {
	
	@Override
	public int minHeight() {
		return 6;
	}
	
	@Override
	public int minWidth() {
		return 6;
	}
	
	@Override
	public void paint(Level level) {
		Painter.fill(level, this, CellPlacement.ground(Terrain.EMPTY, Terrain.WALL));
		Painter.fill(level, this, 1, CellPlacement.reset(Terrain.EMPTY_SP));
		
		Point c = center();
		
		Painter.fill(level, c.x-1, c.y-1, 3, 3, CellPlacement.content(Terrain.WATER));
		// 水池中心是承载无味果丛的干燥草地，需要移除刚铺设的水。
		Painter.set(level, c, CellPlacement.reset(Terrain.EMPTY, Terrain.GRASS));
		
		level.plant(new BlandfruitBush.Seed(), level.pointToCell(c));
		
		int extraFood = (int)(Hunger.STARVING - Hunger.HUNGRY) * (1 + Dungeon.depth / 5);
		
		while (extraFood > 0){
			Food food;
			if (extraFood >= Hunger.STARVING){
				food = new Pasty();
				extraFood -= Hunger.STARVING;
			} else {
				food = new ChargrilledMeat();
				extraFood -= (Hunger.STARVING - Hunger.HUNGRY);
			}
			int foodPos;
			do {
				foodPos = level.pointToCell(random());
			} while (level.terrainAt(foodPos) != Terrain.EMPTY_SP || level.heaps.get(foodPos) != null);
			level.drop(food, foodPos);
		}
		
		entrance().set(Door.Type.HIDDEN);
	}
	
	
}
