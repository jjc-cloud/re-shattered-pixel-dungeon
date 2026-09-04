package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Combo;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HoldFast;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

/** Run main with the core test runtime classpath; no graphics context required. */
public class WarriorTalentsRegression {

	public static class TestHero extends Hero {
		@Override public void spendAndNextConstant(float time) { spendConstant(time); }
	}

	private static Hero hero(int holdFast, int revival) {
		Hero hero = new TestHero();
		hero.heroClass = HeroClass.WARRIOR;
		Talent.initClassTalents(hero);
		hero.talents.get(1).put(Talent.LIQUID_WILLPOWER, holdFast);
		hero.talents.get(2).put(Talent.HOLD_FAST, revival);
		hero.pos = 20;
		hero.HT = 200;
		hero.HP = 100;
		Dungeon.hero = hero;
		return hero;
	}

	public static void main(String[] args) {
		Game.version = "regression-test";
		com.badlogic.gdx.Gdx.app = (com.badlogic.gdx.Application)
				java.lang.reflect.Proxy.newProxyInstance(WarriorTalentsRegression.class.getClassLoader(),
						new Class[]{com.badlogic.gdx.Application.class}, (proxy, method, values) -> {
							if (method.getName().equals("log")) return null;
							throw new UnsupportedOperationException(method.getName());
						});
		// Load real Chinese messages for failed-cast checks without launching a window.
		com.watabou.utils.GameSettings.set((com.badlogic.gdx.Preferences)
				java.lang.reflect.Proxy.newProxyInstance(WarriorTalentsRegression.class.getClassLoader(),
						new Class[]{com.badlogic.gdx.Preferences.class}, (proxy, method, values) -> {
							if (method.getName().equals("getString")) return "zh";
							throw new UnsupportedOperationException(method.getName());
						}));
		com.badlogic.gdx.Gdx.files = (com.badlogic.gdx.Files)
				java.lang.reflect.Proxy.newProxyInstance(WarriorTalentsRegression.class.getClassLoader(),
						new Class[]{com.badlogic.gdx.Files.class}, (proxy, method, values) -> {
							if (method.getName().equals("internal")) {
								return new com.badlogic.gdx.files.FileHandle("core/src/main/assets/" + values[0]);
							}
							throw new UnsupportedOperationException(method.getName());
						});
		Dungeon.level = new SewerLevel() {
			@Override public void occupyCell(Char ch) { }
		};
		Dungeon.level.setSize(9, 9);
		check(Talent.LIQUID_WILLPOWER.maxPoints() == 2, "tier two ranks");
		check(Talent.HOLD_FAST.maxPoints() == 3, "tier three ranks");
		for (int rank = 1; rank <= 2; rank++) {
			Hero hero = hero(rank, 0);
			hero.rest(false);
			HoldFast stance = hero.buff(HoldFast.class);
			check(stance != null, "wait activates stance");
			for (int i = 0; i < 100; i++) {
				int armor = stance.armorBonus();
				check(armor >= rank && armor <= 2 * rank, "armor range");
			}
			check(HoldFast.buffDecayFactor(hero) == (rank == 1 ? 0.5f : 0f), "decay factor");
			Barrier shield = Buff.affect(hero, Barrier.class);
			shield.setShield(20);
			Combo combo = Buff.affect(hero, Combo.class);
			combo.addTime(10);
			Bundle before = new Bundle();
			combo.storeInBundle(before);
			for (int i = 0; i < 2; i++) { shield.act(); combo.act(); }
			Bundle after = new Bundle();
			combo.storeInBundle(after);
			check(shield.shielding() == (rank == 1 ? 19 : 20), "shield decay");
			check(before.getFloat("combotime") - after.getFloat("combotime") == (rank == 1 ? 1f : 0f), "combo decay");
			hero.move(21, false);
			check(hero.buff(HoldFast.class) == null && HoldFast.buffDecayFactor(hero) == 1f, "movement clears stance");
			hero.move(20, false);
			check(hero.buff(HoldFast.class) == null, "returning does not restore stance");
			Char enemy = new Char() {
				@Override public boolean isInvulnerable(Class effect) { return true; }
			};
			enemy.pos = 22;
			check(!hero.attack(enemy), "unsuccessful attack");
			check(hero.buff(HoldFast.class) != null, "attack attempt activates stance");
			Talent.onPotionUsed(hero, hero.pos, 1f);
			check(hero.HP == 100 && shield.shielding() == (rank == 1 ? 19 : 20), "tier two grants no potion healing or shield");
			Buff.detach(hero, HoldFast.class);
			hero.belongings.weapon = new Sword();
			hero.attack(enemy);
			check(hero.buff(HoldFast.class) != null, "equipped weapon activates stance");
			Buff.detach(hero, HoldFast.class);
			hero.shoot(enemy, new ThrowingStone());
			check(hero.buff(HoldFast.class) != null, "thrown weapon activates stance");
			Buff.detach(hero, HoldFast.class);
			WandOfMagicMissile wand = new WandOfMagicMissile();
			check(wand.beginZap(hero, enemy.pos), "charged wand accepted");
			check(hero.buff(HoldFast.class) != null, "wand activates stance before effects");
			Buff.detach(hero, HoldFast.class);
			wand.curCharges = 0;
			check(!wand.beginZap(hero, enemy.pos), "empty wand rejected");
			check(hero.buff(HoldFast.class) == null, "empty wand grants no stance");
			wand.curCharges = 1;
			Buff.affect(hero, MagicImmune.class);
			check(!wand.beginZap(hero, enemy.pos), "magic immunity rejects cast");
			check(hero.buff(HoldFast.class) == null, "blocked magic grants no stance");
			Buff.detach(hero, MagicImmune.class);
			WandOfMagicMissile rejectedWand = new WandOfMagicMissile() {
				@Override public boolean tryToZap(Hero owner, int target) { return false; }
			};
			check(!rejectedWand.beginZap(hero, enemy.pos), "wand-specific validation respected");
			check(hero.buff(HoldFast.class) == null, "invalid target grants no stance");
		}
		for (int rank = 1; rank <= 3; rank++) {
			Hero hero = hero(0, rank);
			hero.rest(false);
			check(hero.buff(HoldFast.class) == null, "tier three grants no stance");
			check(new WandOfMagicMissile().beginZap(hero, 22), "wand without hold fast accepted");
			check(hero.buff(HoldFast.class) == null, "wand requires tier two talent");
			Talent.onPotionUsed(hero, hero.pos, 1f);
			check(hero.HP == 100 + 10 * rank, "5/10/15 percent healing");
			check(hero.buff(Barrier.class) == null, "healing does not grant shield");
			hero.HP = 100;
			Talent.onPotionUsed(hero, hero.pos, 2f);
			check(hero.HP == 100 + 20 * rank, "potion multiplier");
			hero.HP = 199;
			Talent.onPotionUsed(hero, hero.pos, 1f);
			check(hero.HP == 200, "healing cap");
			Talent.onPotionUsed(hero, hero.pos, 1f);
			check(hero.HP == 200, "full health");
		}
		System.out.println("PASS: warrior talent triggers, armor, decay, movement and healing");
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
