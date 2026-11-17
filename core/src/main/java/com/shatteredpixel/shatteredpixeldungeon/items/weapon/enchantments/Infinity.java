package com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;

public class Infinity extends Weapon.Enchantment {

	private static final ItemSprite.Glowing GLOW = new ItemSprite.Glowing(0xCC66FF);

	@Override
	public int proc(Weapon weapon, Char attacker, Char defender, int damage) {
		return damage;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return GLOW;
	}
}
