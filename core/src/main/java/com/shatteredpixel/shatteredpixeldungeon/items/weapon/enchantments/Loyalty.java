package com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

public class Loyalty extends Weapon.Enchantment {

	private static final ItemSprite.Glowing GLOW = new ItemSprite.Glowing(0x66CCFF);

	@Override
	public int proc(Weapon weapon, Char attacker, Char defender, int damage) {
		return damage;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return GLOW;
	}

	public static boolean scheduleReturn(MissileWeapon weapon, Hero hero) {
		if (hero == null || weapon.spawnedForEffect || weapon.durabilityLeft() <= 0
				|| !weapon.hasEnchant(Loyalty.class, hero)) {
			return false;
		}
		ReturningWeapon returning = Buff.append(hero, ReturningWeapon.class, Actor.TICK);
		if (returning.target != hero) return false;
		returning.weapon = weapon;
		return true;
	}

	public static class ReturningWeapon extends FlavourBuff {

		private static final String WEAPON = "weapon";
		private MissileWeapon weapon;

		{
			revivePersists = true;
		}

		@Override
		public boolean act() {
			Hero hero = (Hero) target;
			if (weapon != null) {
				if (weapon.collect(hero.belongings.backpack)) {
					GameScene.pickUp(weapon, hero.pos);
					Sample.INSTANCE.play(Assets.Sounds.ITEM);
				} else {
					Dungeon.level.drop(weapon, hero.pos).sprite.drop();
				}
			}
			return super.act();
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(WEAPON, weapon);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			weapon = (MissileWeapon) bundle.get(WEAPON);
		}
	}
}
