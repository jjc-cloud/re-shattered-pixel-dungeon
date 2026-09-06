package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.*;

public class ChampionAbilityRegression {
    private static class Target extends Char {
        int hits;
        int damage;
        Target() { HP = HT = 1000; pos = 21; alignment = Alignment.ENEMY; }
        @Override public int defenseSkill(Char enemy) { return 0; }
        @Override public int drRoll() { return 0; }
        @Override public void damage(int amount, Object source) { HP -= amount; hits++; damage += amount; }
    }

    public static void main(String[] args) {
        WarriorTalentsRegression.setupHeadless();
        Hero hero = new Hero() {
            @Override public int damageRoll() { return 20; }
        };
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
        if (target.hits != 2 || target.damage != 50) throw new AssertionError("ability must deal 40 plus 10 secondary damage exactly once");
        if (hero.belongings.abilityWeapon != hero.belongings.weapon) throw new AssertionError("ability weapon context lost");
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
        if (target.hits != 2 || target.damage != 30) throw new AssertionError("ordinary attack must still have exactly one secondary strike");
        System.out.println("PASS: champion ability follow-up, half damage, range, strength and recursion guard");
    }
}
