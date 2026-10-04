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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.NaturesPower;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.ElementalBlast;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

public class ExperimentalKingsCrown extends KingsCrown {

	private ArmorAbility[] choices;

	@Override
	public ArmorAbility[] abilityChoices(Hero hero) {
		if (choices == null) {
			//能力适配只取决于初始职业和面具选择，背包装备变化不改变候选池。
			boolean hasStaff = hero.heroClass == HeroClass.MAGE
					|| hero.subClass == HeroSubClass.BATTLEMAGE || hero.subClass == HeroSubClass.WARLOCK;
			boolean hasTome = hero.heroClass == HeroClass.CLERIC
					|| hero.subClass == HeroSubClass.PRIEST || hero.subClass == HeroSubClass.PALADIN;
			boolean hasBow = hero.heroClass == HeroClass.HUNTRESS
					|| hero.subClass == HeroSubClass.SNIPER || hero.subClass == HeroSubClass.WARDEN;
			ArrayList<ArmorAbility> pool = new ArrayList<>();
			for (HeroClass heroClass : HeroClass.values()) {
				if (!hero.randomMode && heroClass == hero.heroClass) continue;
				for (ArmorAbility ability : heroClass.armorAbilities()) {
					if (ability instanceof ElementalBlast && !hasStaff) continue;
					if (ability instanceof Trinity && !hasTome) continue;
					if (hero.randomMode) {
						if ((ability instanceof AscendedForm || ability instanceof PowerOfMany)
								&& !hasTome) continue;
						if (ability instanceof NaturesPower && !hasBow) continue;
						if (ability instanceof ElementalStrike && !hero.hasWeaponSlots()) continue;
					}
					pool.add(ability);
				}
			}
			//与面具分开派生随机流，同种子、同职业、同副职业固定得到同一组选项。
			Random.pushGenerator(Dungeon.seed ^ 0x43524F574EL
					^ ((long) hero.heroClass.ordinal() << 32) ^ ((long) hero.subClass.ordinal() << 40));
			try {
				Random.shuffle(pool);
			} finally {
				Random.popGenerator();
			}
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
