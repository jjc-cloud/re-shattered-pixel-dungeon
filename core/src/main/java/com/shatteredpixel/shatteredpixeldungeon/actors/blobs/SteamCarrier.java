package com.shatteredpixel.shatteredpixeldungeon.actors.blobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;

public class SteamCarrier extends Blob {

	@Override
	protected void evolve() {
		super.evolve();

		WaterVapor vapor = (WaterVapor)Dungeon.level.blobs.get(WaterVapor.class);
		Blizzard blizzard = (Blizzard)Dungeon.level.blobs.get(Blizzard.class);
		Freezing freezing = (Freezing)Dungeon.level.blobs.get(Freezing.class);
		int duration = Actor.curActorPriority() < BLOB_PRIO ? 1 : 2;
		for (int y = area.top; y < area.bottom; y++) {
			for (int x = area.left; x < area.right; x++) {
				int cell = x + y * Dungeon.level.width();
				if (cur[cell] > 0) {
					if (blizzard != null && blizzard.volume > 0 && blizzard.cur[cell] > 0) {
						blizzard.clear(cell);
						off[cell] = cur[cell] = 0;
						Dungeon.level.setCellToWater(true, cell);
						continue;
					}
					if (freezing != null && freezing.volume > 0 && freezing.cur[cell] > 0) {
						freezing.clear(cell);
						off[cell] = cur[cell] = 0;
						Dungeon.level.setCellToWater(true, cell);
						continue;
					}
					int currentDuration = vapor == null || vapor.cur == null ? 0 : vapor.cur[cell];
					vapor = Blob.seed(cell, Math.max(0, duration - currentDuration),
							WaterVapor.class, Dungeon.level);
				}
			}
		}

		if (vapor != null) {
			GameScene.add(vapor);
			Dungeon.observe();
		}
	}

	@Override
	public void use(BlobEmitter emitter) {
		super.use(emitter);
		emitter.pour(Speck.factory(Speck.STEAM_CARRIER, true), 0.4f);
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
}
