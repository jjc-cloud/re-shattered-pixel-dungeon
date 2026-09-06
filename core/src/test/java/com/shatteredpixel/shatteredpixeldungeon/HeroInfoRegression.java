package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfEnergy;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.noosa.Image;
import com.watabou.noosa.ui.Component;
import java.lang.reflect.Field;

/** Headless verification of description layout and cleric artifact charging only. */
public class HeroInfoRegression {
    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    public static void main(String[] args) throws Exception {
        WarriorTalentsRegression.setupHeadless();
        // Skip graphics construction, but exercise the actual tab's setSize/layout code.
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field singleton = unsafeClass.getDeclaredField("theUnsafe");
        singleton.setAccessible(true);
        Object allocator = singleton.get(null);
        Class<?> tabClass = Class.forName("com.shatteredpixel.shatteredpixeldungeon.windows.WndHeroInfo$HeroInfoTab");
        for (HeroClass heroClass : HeroClass.values()) {
            Component tab = (Component) unsafeClass.getMethod("allocateInstance", Class.class).invoke(allocator, tabClass);
            int paragraphs = heroClass.desc().split("\n\n").length;
            RenderedTextBlock[] info = new RenderedTextBlock[paragraphs];
            for (int i = 0; i < info.length; i++) info[i] = new RenderedTextBlock(6);
            int iconCount = heroClass == HeroClass.WARRIOR || heroClass == HeroClass.MAGE ? 3 : 4;
            Image[] icons = new Image[iconCount];
            for (int i = 0; i < icons.length; i++) icons[i] = new Image();
            set(tab, "title", new RenderedTextBlock(9));
            set(tab, "info", info);
            set(tab, "icons", icons);
            tab.setSize(120, 0);
            if (tab.height() <= 0) throw new AssertionError("layout has no height: " + heroClass);
        }
        Hero cleric = new Hero();
        cleric.heroClass = HeroClass.CLERIC;
        Dungeon.hero = cleric;
        if (Math.abs(RingOfEnergy.artifactChargeMultiplier(cleric) - 1.2f) > .00001f) {
            throw new AssertionError("cleric artifact charge bonus must be twenty percent");
        }
        if (!HeroClass.CLERIC.desc().contains("神器充能速度提升20%")) {
            throw new AssertionError("cleric description must show twenty percent");
        }
        System.out.println("PASS: all six hero description layouts and cleric twenty-percent artifact charging");
    }
}
