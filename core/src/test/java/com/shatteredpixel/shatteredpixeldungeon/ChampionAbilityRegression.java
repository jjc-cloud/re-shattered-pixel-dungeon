package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GreaterHaste;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.*;

import java.util.ArrayList;
import java.util.List;

public class ChampionAbilityRegression {
	private static class TrackingHero extends Hero {
		final List<KindOfWeapon> attackWeapons = new ArrayList<>();
		@Override public int damageRoll() { return 20; }
		@Override public boolean attack(Char enemy, float multiplier, float bonus, float accuracy) {
			attackWeapons.add(belongings.attackingWeapon());
			return super.attack(enemy, multiplier, bonus, accuracy);
		}
	}

    private static class Target extends Char {
        int hits;
        int damage;
        Target() { HP = HT = 1000; pos = 21; alignment = Alignment.ENEMY; }
        @Override public int defenseSkill(Char enemy) { return 0; }
        @Override public int drRoll() { return 0; }
        @Override public void damage(int amount, Object source) { HP -= amount; hits++; damage += amount; }
    }

    public static void main(String[] args) {
		com.badlogic.gdx.utils.GdxNativesLoader.load();
        WarriorTalentsRegression.setupHeadless();
        TrackingHero hero = new TrackingHero();
        Dungeon.hero = hero;
        hero.heroClass = HeroClass.DUELIST;
        hero.subClass = HeroSubClass.CHAMPION;
        hero.pos = 20;
        hero.STR = 20;
        Talent.initClassTalents(hero);
        Talent.initSubclassTalents(hero);
        hero.belongings.weapon = new Rapier();
        hero.belongings.secondWep = new WornShortsword();
        hero.belongings.abilityWeapon = hero.belongings.weapon;
        Target target = new Target();
        hero.attack(target, 2f, 0f, Char.INFINITE_ACCURACY);
        if (target.hits != 2 || target.damage != 48) throw new AssertionError("ability must deal 40 plus a 0.4 follow-up damage exactly once");
        if (hero.attackWeapons.get(0) != hero.belongings.weapon || hero.attackWeapons.get(1) != hero.belongings.secondWep) throw new AssertionError("primary ability must be followed by secondary weapon");
        if (hero.belongings.abilityWeapon != hero.belongings.weapon) throw new AssertionError("ability weapon context lost");
		hero.attackWeapons.clear();
		hero.belongings.abilityWeapon = hero.belongings.secondWep;
		target = new Target();
		hero.attack(target, 2f, 0f, Char.INFINITE_ACCURACY);
		if (target.hits != 2 || target.damage != 48) throw new AssertionError("secondary ability must deal full damage plus one follow-up");
		if (hero.attackWeapons.get(0) != hero.belongings.secondWep || hero.attackWeapons.get(1) != hero.belongings.weapon) throw new AssertionError("secondary ability must be followed by primary weapon");
		if (hero.belongings.abilityWeapon != hero.belongings.secondWep) throw new AssertionError("secondary ability weapon context lost");
        target = new Target(); target.pos = 22;
        hero.attack(target, 2f, 0f, Char.INFINITE_ACCURACY);
        if (target.hits != 1) throw new AssertionError("out-of-range secondary attack");
        target = new Target(); hero.STR = 0;
        hero.attack(target, 2f, 0f, Char.INFINITE_ACCURACY);
        if (target.hits != 1) throw new AssertionError("overstrength secondary attack");
        hero.STR = 20;
        hero.belongings.abilityWeapon = null;
        target = new Target();
        hero.attackWithWeapons(target);
        if (target.hits != 2 || target.damage != 28) throw new AssertionError("ordinary attack must still have exactly one secondary strike");

        hero.belongings.abilityWeapon = hero.belongings.weapon;
        //联合致命每级使追击倍率 +0.2
        hero.talents.get(2).put(Talent.COMBINED_LETHALITY, 1);
        target = new Target();
        hero.attack(target, 2f, 0f, Char.INFINITE_ACCURACY);
        if (target.hits != 2 || target.damage != 52) throw new AssertionError("combined lethality must raise the follow-up damage");
        hero.talents.get(2).put(Talent.COMBINED_LETHALITY, 0);

        //追击击杀算作武技击杀：恢复武器充能并获得夺命余势
        hero.talents.get(1).put(Talent.WEAPON_RECHARGING, 1);
        hero.talents.get(1).put(Talent.LETHAL_HASTE, 1);
        Target surviving = new Target();
        hero.attack(surviving, 2f, 0f, Char.INFINITE_ACCURACY);
        if (!surviving.isAlive() || surviving.hits != 2) throw new AssertionError("target must survive the ability and take the follow-up");
        if (hero.buff(MeleeWeapon.Charger.class) != null || hero.buff(GreaterHaste.class) != null)
            throw new AssertionError("a follow-up that does not kill must not count as an ability kill");
        Target dying = new Target(); dying.HP = 45;
        hero.attack(dying, 2f, 0f, Char.INFINITE_ACCURACY);
        if (dying.isAlive() || dying.hits != 2 || dying.damage != 48) throw new AssertionError("follow-up must finish a target the ability left alive");
        if (hero.buff(MeleeWeapon.Charger.class) == null || hero.buff(MeleeWeapon.Charger.class).partialCharge < 0.19f)
            throw new AssertionError("follow-up kill must recharge the weapon");
        if (hero.buff(GreaterHaste.class) == null) throw new AssertionError("follow-up kill must grant lethal haste");
        System.out.println("PASS: champion ability follow-up, follow-up damage and talents, range, strength, recursion guard and follow-up kills");
    }
}
