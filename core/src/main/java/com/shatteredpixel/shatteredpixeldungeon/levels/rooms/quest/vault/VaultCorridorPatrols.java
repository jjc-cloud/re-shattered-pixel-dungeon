package com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.vault;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultDM100;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultGolem;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultMobPool;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.watabou.utils.Random;

/** Three vertical lanes, each divided into two independent eleven-cell patrols. */
public final class VaultCorridorPatrols {

	private VaultCorridorPatrols() {}

	public static final int TOP = 45;
	public static final int SEGMENT_LENGTH = 11;
	public static final int LEFT_X = 12, CENTER_X = 20, RIGHT_X = 28;

	public static void createMobs(Level level) {
		for (int x : new int[]{LEFT_X, CENTER_X, RIGHT_X}) {
			for (int segment = 0; segment < 2; segment++) {
				int top = TOP + segment * SEGMENT_LENGTH;
				int bottom = top + SEGMENT_LENGTH - 1;
				boolean entranceGuard = x == CENTER_X && segment == 1;
				Mob mob = entranceGuard ? VaultMobPool.create(VaultDM100.class)
						: x == CENTER_X ? VaultMobPool.create(VaultGolem.class) : VaultMobPool.random();
				int spawnY = entranceGuard ? top : Random.IntRange(top, bottom);
				int destination = entranceGuard || spawnY == top ? 1 : spawnY == bottom ? 0 : Random.Int(2);
				mob.pos = x + spawnY * level.width();
				mob.markVaultCorridorGuard();
				mob.setupStealthGameplayLinePatrol(new int[]{x + top * level.width(), x + bottom * level.width()}, destination);
				level.mobs.add(mob);
			}
		}
	}
}
