/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.ElementalBlast;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

public class ExperimentalKingsCrown extends KingsCrown {

	private ArmorAbility[] choices;

	@Override
	protected ArmorAbility[] abilityChoices(Hero hero) {
		if (choices == null) {
			ArrayList<ArmorAbility> pool = new ArrayList<>();
			for (HeroClass heroClass : HeroClass.values()) {
				if (heroClass == hero.heroClass) continue;
				for (ArmorAbility ability : heroClass.armorAbilities()) {
					if (ability instanceof ElementalBlast
							&& hero.belongings.getItem(MagesStaff.class) == null) continue;
					if (ability instanceof Trinity
							&& hero.belongings.getItem(HolyTome.class) == null) continue;
					pool.add(ability);
				}
			}
			Random.shuffle(pool);
			choices = new ArmorAbility[]{pool.get(0), pool.get(1), pool.get(2)};
		}
		return choices;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return ItemSprite.Glowing.rainbow(2f);
	}

	private static final String CHOICES = "ability_choices";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		if (choices != null) {
			String[] stored = new String[choices.length];
			for (int i = 0; i < choices.length; i++) stored[i] = choices[i].getClass().getName();
			bundle.put(CHOICES, stored);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		String[] stored = bundle.getStringArray(CHOICES);
		if (stored != null && stored.length == 3) {
			choices = new ArmorAbility[stored.length];
			try {
				for (int i = 0; i < stored.length; i++) choices[i] = Reflection.newInstance(
						(Class<? extends ArmorAbility>)Class.forName(stored[i]));
			} catch (ClassNotFoundException e) {
				choices = null;
			}
		}
	}
}
