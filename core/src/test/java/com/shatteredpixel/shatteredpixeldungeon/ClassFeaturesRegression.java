package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Warlock;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.*;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.*;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.*;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone;
import com.watabou.utils.Bundle;

/** Standalone checks of class mechanics after implementation. */
public class ClassFeaturesRegression {
    public static class TestHero extends Hero {
        @Override public void spendAndNext(float time) { spend(time); }
    }

    private static TestHero hero(HeroClass type, HeroSubClass subclass) {
        TestHero hero = new TestHero();
        Dungeon.hero = hero;
        Dungeon.depth = 1;
        Dungeon.branch = 0;
        Dungeon.level.locked = false;
        hero.heroClass = type;
        hero.subClass = subclass;
        hero.HP = hero.HT = 1000;
        hero.pos = 20;
        hero.damageInterrupt = false;
        Talent.initClassTalents(hero);
        Talent.initSubclassTalents(hero);
        return hero;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void close(float actual, float expected, String message) {
        check(Math.abs(actual - expected) < 0.0001f, message + ": " + actual + " != " + expected);
    }

    private static float wandRate(HeroClass type, HeroSubClass subclass, boolean staffScale) {
        Hero hero = hero(type, subclass);
        Wand wand = new WandOfMagicMissile();
        wand.curCharges = 0;
        if (staffScale) wand.charge(hero, MagesStaff.STAFF_SCALE_FACTOR);
        else wand.charge(hero);
        hero.buff(Wand.Charger.class).act();
        return wand.partialCharge;
    }

    private static void charging() {
        float base = wandRate(HeroClass.WARRIOR, HeroSubClass.NONE, false);
        float mage = wandRate(HeroClass.MAGE, HeroSubClass.NONE, false);
        check(mage > base, "mage wands recharge faster");
        close(mage, wandRate(HeroClass.MAGE, HeroSubClass.NONE, true), "mage wands match staff scale without double bonus");
        close(wandRate(HeroClass.ROGUE, HeroSubClass.NONE, false), base * .85f, "rogue wand penalty");
        close(wandRate(HeroClass.DUELIST, HeroSubClass.MONK, false), base * 1.2f, "monk wand bonus");
        close(RingOfEnergy.artifactChargeMultiplier(hero(HeroClass.MAGE, HeroSubClass.NONE)), .85f, "mage artifact penalty");
        close(RingOfEnergy.artifactChargeMultiplier(hero(HeroClass.CLERIC, HeroSubClass.NONE)), 1.15f, "cleric artifact bonus");
        close(RingOfEnergy.artifactChargeMultiplier(hero(HeroClass.DUELIST, HeroSubClass.MONK)), 1.2f, "monk artifact bonus");
        Hero hero = hero(HeroClass.MAGE, HeroSubClass.NONE);
        Wand wand = new WandOfMagicMissile();
        wand.curCharges = 0;
        wand.charge(hero);
        Buff.affect(hero, MagicImmune.class);
        hero.buff(Wand.Charger.class).act();
        close(wand.partialCharge, 0, "magic immunity still blocks charging");
        Buff.detach(hero, MagicImmune.class);
        Buff.affect(hero, LockedFloor.class).removeTime(100);
        hero.buff(Wand.Charger.class).act();
        close(wand.partialCharge, 0, "locked-floor restriction still blocks charging");
    }

    private static void hungerAndStealth() {
        Hero warrior = hero(HeroClass.WARRIOR, HeroSubClass.NONE);
        Hunger normal = Buff.affect(warrior, Hunger.class);
        for (int i = 0; i < 120; i++) normal.act();
        Hero rogue = hero(HeroClass.ROGUE, HeroSubClass.NONE);
        Hunger slower = Buff.affect(rogue, Hunger.class);
        for (int i = 0; i < 120; i++) slower.act();
        Bundle state = new Bundle();
        slower.storeInBundle(state);
        check(normal.hunger() == 120, "normal hunger unchanged");
        close(state.getFloat("level"), 100f, "rogue gains hunger 1.2 times more slowly");
        state = new Bundle();
        state.put("level", Hunger.STARVING);
        slower.restoreFromBundle(state);
        for (int i = 0; i < 12; i++) slower.act();
        Bundle after = new Bundle();
        slower.storeInBundle(after);
        close(rogue.HT - rogue.HP + after.getFloat("partialDamage"), 10f, "legacy starvation pacing retained");
        float base = hero(HeroClass.WARRIOR, HeroSubClass.NONE).stealth();
        close(hero(HeroClass.HUNTRESS, HeroSubClass.NONE).stealth(), base + 2, "huntress has two extra stealth");
    }

    private static void clericDamage() {
        Char enemy = new Char() { };
        enemy.alignment = Char.Alignment.ENEMY;
        Hero cleric = hero(HeroClass.CLERIC, HeroSubClass.NONE);
        cleric.damage(20, enemy);
        check(cleric.HP == 977, "cleric takes fifteen percent more enemy damage");
        cleric.HP = 1000;
        cleric.damage(6, enemy);
        check(cleric.HP == 994, "cleric penalty floors instead of rounding");
        cleric.HP = 1000;
        Buff.affect(cleric, Barrier.class).setShield(10);
        cleric.damage(20, enemy);
        check(cleric.HP == 989, "cleric penalty applies after shielding");
        cleric.HP = 1000;
        Buff.affect(cleric, Barrier.class).setShield(20);
        cleric.damage(20, enemy);
        check(cleric.HP == 1000, "fully absorbed hits have no extra damage");
        cleric.damage(20, new Warlock.DarkBolt());
        check(cleric.HP == 977, "enemy spell damage also gets penalty");
        cleric.HP = 1000;
        enemy.alignment = Char.Alignment.ALLY;
        cleric.damage(20, enemy);
        check(cleric.HP == 980, "ally damage has no penalty");
        cleric.HP = 1000;
        cleric.damage(20, new Hunger());
        check(cleric.HP == 980, "hunger has no enemy penalty");
    }

    private static class FoundSecret extends RuntimeException { }

    private static boolean search(HeroClass type, int level, int terrain, boolean searchable, float roll) {
        com.shatteredpixel.shatteredpixeldungeon.levels.Level previous = Dungeon.level;
        Dungeon.level = new com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel() {
            @Override public void discover(int cell) { throw new FoundSecret(); }
        };
        Dungeon.level.setSize(9, 9);
        Dungeon.level.traps = new com.watabou.utils.SparseArray<>();
        Hero hero = hero(type, HeroSubClass.NONE);
        hero.lvl = level;
        hero.fieldOfView = new boolean[81];
        hero.fieldOfView[21] = true;
        Dungeon.level.map[21] = terrain;
        Dungeon.level.secret[21] = true;
        if (!searchable) {
            com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap trap = new com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap() {
                @Override public void activate() { }
            };
            trap.canBeSearched = false;
            Dungeon.level.traps.put(21, trap);
        }
        long seed = 0;
        while (true) {
            com.watabou.utils.Random.pushGenerator(seed);
            float candidate = com.watabou.utils.Random.Float();
            com.watabou.utils.Random.popGenerator();
            if (candidate > roll && candidate < roll + .01f) break;
            seed++;
        }
        com.watabou.utils.Random.pushGenerator(seed);
        try {
            hero.search(false);
            return false;
        } catch (FoundSecret found) {
            return true;
        } finally {
            com.watabou.utils.Random.popGenerator();
            Dungeon.level = previous;
        }
    }

    private static void searching() {
        int door = com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.SECRET_DOOR;
        int trap = com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.SECRET_TRAP;
        check(!search(HeroClass.WARRIOR, 1, door, true, .21f), "baseline door detection unchanged");
        check(search(HeroClass.ROGUE, 1, door, true, .21f), "rogue has legacy awareness advantage at level one");
        check(!search(HeroClass.ROGUE, 1, door, true, .30f), "low-level awareness is limited");
        check(search(HeroClass.ROGUE, 9, door, true, .30f), "rogue awareness grows through level nine");
        check(search(HeroClass.ROGUE, 1, trap, true, .42f), "rogue advantage also improves modern trap detection");
        check(!search(HeroClass.ROGUE, 9, trap, false, .01f), "unsearchable traps remain unsearchable");
    }

    public static class StrikeHero extends TestHero {
        int hits;
        KindOfWeapon first, second;
        float secondMultiplier;
        boolean killFirst, missFirst, failSecond;
        @Override public boolean attack(Char enemy, float multiplier, float bonus, float accuracy) {
            hits++;
            if (hits == 1) {
                first = belongings.attackingWeapon();
                if (killFirst) enemy.HP = 0;
                return !missFirst;
            }
            second = belongings.attackingWeapon();
            secondMultiplier = multiplier;
            if (failSecond) throw new IllegalStateException("test second strike failure");
            return true;
        }
    }

    private static StrikeHero champion() {
        StrikeHero hero = new StrikeHero();
        Dungeon.hero = hero;
        hero.heroClass = HeroClass.DUELIST;
        hero.subClass = HeroSubClass.CHAMPION;
        hero.pos = 20;
        hero.STR = 20;
        hero.belongings.weapon = new Rapier();
        hero.belongings.secondWep = new WornShortsword();
        Talent.initClassTalents(hero);
        Talent.initSubclassTalents(hero);
        return hero;
    }

    private static void normalAttack(StrikeHero hero, int position) throws Exception {
        Char enemy = new Char() { };
        enemy.alignment = Char.Alignment.ENEMY;
        enemy.pos = position;
        enemy.HP = enemy.HT = 100;
        hero.attackWithWeapons(enemy);
    }

    private static void championAttacks() throws Exception {
        StrikeHero hero = champion();
        float before = hero.cooldown();
        float delay = hero.attackDelay();
        normalAttack(hero, 21);
        check(hero.hits == 2 && hero.first == hero.belongings.weapon && hero.second == hero.belongings.secondWep,
                "champion strikes with primary then secondary");
        close(hero.secondMultiplier, .5f, "secondary strike has half damage");
        close(hero.cooldown() - before, 0, "secondary strike does not independently spend time");
        close(hero.attackDelay(), delay, "primary attack delay unchanged by secondary weapon");
        check(hero.belongings.abilityWeapon == null, "secondary context cleared");
        hero = champion(); hero.missFirst = true;
        normalAttack(hero, 21);
        check(hero.hits == 2, "primary miss does not suppress secondary attack");
        hero = champion(); hero.killFirst = true;
        normalAttack(hero, 21);
        check(hero.hits == 1, "no attack on dead target");
        hero = champion(); hero.STR = 0;
        normalAttack(hero, 21);
        check(hero.hits == 1, "overstrength secondary weapon cannot strike");
        hero = champion();
        normalAttack(hero, 22);
        check(hero.hits == 1, "out-of-reach secondary weapon cannot strike");
        hero = champion(); hero.belongings.secondWep = new Spear();
        normalAttack(hero, 22);
        check(hero.hits == 2, "reach weapon can perform secondary strike at range");
        hero = champion(); hero.subClass = HeroSubClass.NONE;
        normalAttack(hero, 21);
        check(hero.hits == 1, "base duelist has no double strike");
        hero = champion(); hero.failSecond = true;
        try { normalAttack(hero, 21); throw new AssertionError("expected second-strike failure"); }
        catch (IllegalStateException expected) { check(hero.belongings.abilityWeapon == null, "context restored on failure"); }
        hero = champion();
        Char enemy = new Char() { }; enemy.HP = 100; enemy.pos = 21;
        hero.shoot(enemy, new ThrowingStone());
        check(hero.hits == 1, "throws do not trigger secondary strikes");
    }

    private static void monkEquipment() {
        Hero hero = hero(HeroClass.DUELIST, HeroSubClass.NONE);
        HeroClass.DUELIST.initHero(hero);
        check(hero.belongings.weapon instanceof Rapier && hero.belongings.secondWep instanceof WornShortsword,
                "duelist starts with equipped rapier and worn shortsword");
        KindOfWeapon primary = hero.belongings.weapon, secondary = hero.belongings.secondWep;
        int oldCapacity = hero.belongings.backpack.capacity();
        primary.cursed = secondary.cursed = true;
        hero.subClass = HeroSubClass.MONK;
        Talent.initSubclassTalents(hero);
        hero.removeMonkWeapons();
        check(hero.belongings.weapon == null && hero.belongings.secondWep == null, "monk removes both weapons including cursed ones");
        check(hero.belongings.contains(primary) && hero.belongings.contains(secondary), "removed weapons preserved");
        check(primary.cursed && secondary.cursed, "conversion does not cleanse weapon curses");
        check(hero.belongings.backpack.capacity() == oldCapacity + 1, "secondary slot returns to backpack capacity");
        check(!primary.doEquip(hero) && !secondary.equipSecondary(hero), "monk cannot re-equip weapons");
        check(!primary.actions(hero).contains(EquipableItem.AC_EQUIP), "weapon equip action hidden");

        Hero full = hero(HeroClass.DUELIST, HeroSubClass.NONE);
        Dungeon.level.heaps = new com.watabou.utils.SparseArray<>();
        full.belongings.weapon = new Rapier();
        full.belongings.secondWep = new WornShortsword();
        while (full.belongings.backpack.items.size() < full.belongings.backpack.capacity()) {
            full.belongings.backpack.items.add(new WornShortsword());
        }
        KindOfWeapon overflow = full.belongings.secondWep;
        full.subClass = HeroSubClass.MONK;
        full.removeMonkWeapons();
        check(full.belongings.weapon == null && full.belongings.secondWep == null, "full backpack does not prevent monk conversion");
        check(Dungeon.level.heaps.get(full.pos).items.contains(overflow), "overflow weapon drops at hero position");

        hero = hero(HeroClass.DUELIST, HeroSubClass.MONK);
        new CloakOfShadows().identify().collect();
        Artifact cloak = hero.belongings.getItem(CloakOfShadows.class);
        check(cloak.doEquip(hero), "first artifact equips");
        RingOfEnergy first = new RingOfEnergy(); first.identify().collect(); check(first.doEquip(hero), "first ring equips");
        RingOfEnergy second = new RingOfEnergy(); second.identify().collect(); check(second.doEquip(hero), "second ring equips");
        RingOfEnergy third = new RingOfEnergy(); third.identify().collect(); check(third.doEquip(hero), "third ring equips in monk slot");
        check(hero.belongings.extraMisc == third && third.isEquipped(hero), "extra ring recognized as equipped");
        close(RingOfEnergy.wandChargeMultiplier(hero), 1.2f * (float)Math.pow(1.175, 3), "all three ring buffs apply");
        check(third.combinedBonus(hero) == 3, "ring description includes third ring");
        Bundle state = new Bundle(); hero.belongings.storeInBundle(state);
        Hero loaded = hero(HeroClass.DUELIST, HeroSubClass.MONK);
        loaded.belongings.restoreFromBundle(state);
        check(loaded.belongings.extraMisc instanceof RingOfEnergy, "extra slot survives save/load");
        close(RingOfEnergy.wandChargeMultiplier(loaded), 1.2f * (float)Math.pow(1.175, 3), "extra slot passive restored once");
        loaded.belongings.lostInventory(true);
        check(loaded.belongings.extraMisc() == null, "lost inventory disables extra slot accessor");
        loaded.belongings.lostInventory(false);
        check(loaded.belongings.extraMisc.doUnequip(loaded, true, false), "extra ring unequips");
        check(loaded.belongings.extraMisc == null, "extra slot cleared on unequip");
        close(RingOfEnergy.wandChargeMultiplier(loaded), 1.2f * (float)Math.pow(1.175, 2), "unequipped extra ring stops buffing");
        Artifact tome = new HolyTome(); tome.identify().collect();
        check(tome.doEquip(loaded) && loaded.belongings.extraMisc == tome, "extra slot also accepts artifacts");
        check(loaded.buff(HolyTome.TomeRecharge.class) != null, "extra artifact passive active");
        Artifact duplicate = new HolyTome();
        check(!duplicate.doEquip(loaded), "duplicate artifacts still rejected including extra slot");
        tome.cursed = true;
        loaded.sprite = new com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite() {
            @Override public com.watabou.noosa.particles.Emitter emitter() {
                return new com.watabou.noosa.particles.Emitter();
            }
        };
        loaded.belongings.uncurseEquipped();
        check(!tome.cursed, "extra slot participates in equipped curse removal");

        Hero artifacts = hero(HeroClass.DUELIST, HeroSubClass.MONK);
        Artifact[] worn = {new CloakOfShadows(), new HolyTome(), new HornOfPlenty()};
        for (Artifact artifact : worn) {
            artifact.identify().collect();
            check(artifact.doEquip(artifacts), "monk can equip three distinct artifacts");
        }
        check(artifacts.belongings.extraMisc == worn[2], "third artifact occupies extra slot");

        Hero inventory = hero(HeroClass.DUELIST, HeroSubClass.MONK);
        inventory.belongings.extraMisc = new RingOfEnergy();
        Item stored = new WornShortsword();
        inventory.belongings.backpack.items.add(stored);
        java.util.Iterator<Item> iterator = inventory.belongings.iterator();
        check(iterator.next() == inventory.belongings.extraMisc, "equipped iterator includes extra slot");
        iterator.remove();
        check(inventory.belongings.extraMisc == null, "iterator removes the extra slot correctly");
        check(iterator.next() == stored, "iterator continues into backpack");
    }

    private static void monkEnergy() {
        for (int points = 1; points <= 3; points++) {
            Hero hero = hero(HeroClass.DUELIST, HeroSubClass.MONK);
            hero.talents.get(2).put(Talent.UNENCUMBERED_SPIRIT, points);
            hero.belongings.armor = points == 1 ? new MailArmor() : points == 2 ? new LeatherArmor() : new ClothArmor();
            MonkEnergy energy = Buff.affect(hero, MonkEnergy.class);
            energy.gainEnergy(new Mob() { });
            close(energy.energy, 1.5f + .5f * points, "armor-only energy bonus rank " + points);
            energy.energy = 0;
            hero.belongings.armor = null;
            energy.gainEnergy(new Mob() { });
            close(energy.energy, 1, "no armor gives no talent bonus");
        }
        Hero hero = hero(HeroClass.DUELIST, HeroSubClass.MONK);
        hero.talents.get(2).put(Talent.UNENCUMBERED_SPIRIT, 3);
        Talent.onTalentUpgraded(hero, Talent.UNENCUMBERED_SPIRIT);
        check(hero.belongings.backpack.items.size() == 1 && hero.belongings.backpack.items.get(0) instanceof ClothArmor,
                "maxed talent gives only cloth armor");
    }

    public static void main(String[] args) throws Exception {
        com.badlogic.gdx.utils.GdxNativesLoader.load();
        new com.watabou.noosa.Game(com.watabou.noosa.Scene.class, null);
        WarriorTalentsRegression.setupHeadless();
        com.watabou.utils.GameSettings.set((com.badlogic.gdx.Preferences) java.lang.reflect.Proxy.newProxyInstance(
                ClassFeaturesRegression.class.getClassLoader(), new Class[]{com.badlogic.gdx.Preferences.class}, (proxy, method, values) -> {
                    if (method.getName().equals("getString")) return "zh";
                    if (method.getName().equals("getBoolean")) return false;
                    if (method.getName().equals("getInteger")) return values.length > 1 ? values[1] : 0;
                    if (method.getName().startsWith("put")) return proxy;
                    if (method.getName().equals("flush")) return null;
                    throw new UnsupportedOperationException(method.getName());
                }));
        Ring.initGems();
        com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion.initColors();
        com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll.initLabels();
        charging();
        hungerAndStealth();
        searching();
        clericDamage();
        championAttacks();
        monkEquipment();
        monkEnergy();
        System.out.println("PASS: class charging, hunger, stealth, cleric damage, champion strikes, monk slots and energy");
    }
}
