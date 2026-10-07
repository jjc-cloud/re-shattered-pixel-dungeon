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

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Ordinary vault enemies; bosses and quest NPCs are generated separately. */
public final class VaultMobPool {

	private VaultMobPool() {}

	public static final Class<? extends Mob>[] TIER_1 = new Class[]{VaultSkeleton.class, VaultDM100.class};
	public static final Class<? extends Mob>[] TIER_2 = new Class[]{VaultShaman.class, VaultDM200.class, VaultGhoul.class};
	public static final Class<? extends Mob>[] TIER_3 = new Class[]{VaultElemental.class, VaultGolem.class};

	private static final List<Class<? extends Mob>> TYPES;
	static {
		ArrayList<Class<? extends Mob>> types = new ArrayList<>();
		types.addAll(Arrays.asList(TIER_1));
		types.addAll(Arrays.asList(TIER_2));
		types.addAll(Arrays.asList(TIER_3));
		TYPES = Collections.unmodifiableList(types);
	}

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
