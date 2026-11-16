package com.shatteredpixel.shatteredpixeldungeon.levels.features;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.HermitCrab;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.items.Gem;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.levels.CellContent;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class SewerBarrel extends CellContent {

	public SewerBarrel() {}

	public SewerBarrel(int terrain) {
		super(terrain, 0);
	}

	@Override
	public void destroyed(Level level, int cell, boolean burned) {
		super.destroyed(level, cell, burned);
		if (type == Terrain.REGION_DECO || type == Terrain.SEWER_BARREL_MARKED) {
			level.cellAt(cell).addContent(new CellContent(Terrain.WATER, level.region()));
		}
		if (level == Dungeon.level) Splash.at(cell, 0xFF8A5A2E, 8);
		if (type == Terrain.SEWER_BARREL_MARKED || type == Terrain.SEWER_BARREL_MARKED_ALT) {
			Heap heap = level.drop(Random.Int(4) == 0 ? Generator.random() : Gem.generateTreasure(), cell);
			heap.autoExplored = true;
			if (heap.sprite != null) heap.sprite.drop();
		}
		if (level != Dungeon.level || Random.Int(20) != 0) return;

		int spawn = cell;
		if (!level.passable[cell] || Actor.findChar(cell) != null) {
			ArrayList<Integer> candidates = new ArrayList<>();
			for (int offset : PathFinder.NEIGHBOURS8) {
				int next = cell + offset;
				if (level.insideMap(next) && level.passable[next] && Actor.findChar(next) == null) {
					candidates.add(next);
				}
			}
			if (candidates.isEmpty()) return;
			spawn = Random.element(candidates);
		}

		HermitCrab crab = new HermitCrab();
		crab.pos = spawn;
		GameScene.add(crab);
		GLog.w(Messages.get(SewerLevel.class, "barrel_crab"));
	}
}
