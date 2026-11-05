package com.shatteredpixel.shatteredpixeldungeon.items.potions.brews;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SteamCarrier;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

public class SteamBrew extends Brew {

	{
		image = ItemSpriteSheet.BREW_STEAM;
	}

	@Override
	public void shatter(int cell) {
		splash(cell);
		if (Dungeon.level.heroFOV[cell]) {
			Sample.INSTANCE.play(Assets.Sounds.SHATTER);
			Sample.INSTANCE.play(Assets.Sounds.GAS);
		}

		int centerVolume = 120;
		for (int i : PathFinder.NEIGHBOURS8) {
			if (!Dungeon.level.solid[cell+i]) {
				GameScene.add(Blob.seed(cell+i, 120, SteamCarrier.class));
			} else {
				centerVolume += 120;
			}
		}

		GameScene.add(Blob.seed(cell, centerVolume, SteamCarrier.class));
	}
}
