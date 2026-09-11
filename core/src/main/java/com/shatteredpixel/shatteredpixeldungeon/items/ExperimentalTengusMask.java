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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;

public class ExperimentalTengusMask extends TengusMask {

	private HeroSubClass[] choices;

	@Override
	protected HeroSubClass[] subclassChoices(Hero hero) {
		if (choices == null) {
			ArrayList<HeroSubClass> pool = new ArrayList<>(Arrays.asList(HeroSubClass.values()));
			pool.remove(HeroSubClass.NONE);
			pool.removeAll(Arrays.asList(hero.heroClass.subClasses()));
			Random.shuffle(pool);
			choices = new HeroSubClass[]{pool.get(0), pool.get(1)};
		}
		return choices;
	}

	@Override
	public void choose(HeroSubClass way) {
		super.choose(way);
		grantRequiredClassItem(way);
	}

	private void grantRequiredClassItem(HeroSubClass way) {
		Item item = requiredClassItem(way);
		if (item != null && Dungeon.hero.belongings.getItem(item.getClass()) == null) {
			item.identify();
			if (!item.collect()) Dungeon.level.drop(item, Dungeon.hero.pos).sprite.drop();
		}
	}

	protected Item requiredClassItem(HeroSubClass way) {
		switch (way) {
			case BERSERKER: case GLADIATOR:
				return new BrokenSeal();
			case BATTLEMAGE:
				return new MagesStaff(new WandOfMagicMissile());
			case ASSASSIN: case FREERUNNER:
				return new CloakOfShadows();
			case SNIPER:
				return new SpiritBow();
			case PRIEST: case PALADIN:
				return new HolyTome();
			default:
				return null;
		}
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return ItemSprite.Glowing.rainbow(2f);
	}

	private static final String CHOICES = "subclass_choices";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		if (choices != null) bundle.put(CHOICES,
				new String[]{choices[0].name(), choices[1].name()});
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		String[] stored = bundle.getStringArray(CHOICES);
		if (stored != null && stored.length == 2) choices = new HeroSubClass[]{
				HeroSubClass.valueOf(stored[0]), HeroSubClass.valueOf(stored[1])};
	}
}
