package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Ordinary vault enemies; bosses and quest NPCs are generated separately. */
public final class VaultMobPool {

	private VaultMobPool() {}

	private static final List<Class<? extends Mob>> TYPES = Collections.unmodifiableList(Arrays.asList(
			VaultSkeleton.class, VaultDM100.class, VaultShaman.class, VaultDM200.class,
			VaultGhoul.class, VaultElemental.class, VaultGolem.class));

	public static List<Class<? extends Mob>> types() {
		return TYPES;
	}

	/** Each enemy type has equal weight; elemental forms retain their existing weights. */
	public static Mob random() {
		return create(TYPES.get(Random.Int(TYPES.size())));
	}

	public static Mob create(Class<? extends Mob> type) {
		if (type == VaultElemental.class) type = VaultElemental.random();
		return Reflection.newInstance(type);
	}
}
