package com.shatteredpixel.shatteredpixeldungeon;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.PetrifiedStatue;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.AlarmTrap;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.gltextures.SmartTexture;
import com.watabou.utils.Bundle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;

/** Simulates an uploaded texture with a GL guard, then exercises crowds and alarms. */
public class StatueCrowdRegression {
	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
	public static void main(String[] args) throws Exception {
		com.badlogic.gdx.utils.GdxNativesLoader.load();
		WarriorTalentsRegression.setupHeadless();
		Dungeon.hero = new Hero(); Dungeon.hero.pos = 60;
		Dungeon.level.mobs = new HashSet<>();
		ArrayList<PetrifiedStatue> crowd = new ArrayList<>();
		// Three distinct images, then a fourth statue reuses the first uploaded image.
		for (int i = 0; i < 48; i++) {
			PetrifiedStatue statue = new PetrifiedStatue();
			statue.pos = 10 + i;
			statue.pixels = new int[]{0x777777FF + (i % 3) * 0x01010100};
			crowd.add(statue);
		}
		SmartTexture[] uploaded = new SmartTexture[3];
		for (int i = 0; i < 3; i++) {
			uploaded[i] = crowd.get(i).sprite().texture;
			uploaded[i].id = 100 + i; // Already rendered in a real scene.
		}
		GL20 originalGL = Gdx.gl;
		Gdx.gl = (GL20)java.lang.reflect.Proxy.newProxyInstance(StatueCrowdRegression.class.getClassLoader(),
				new Class[]{GL20.class}, (proxy, method, values) -> {
			throw new AssertionError("GL call during actor-thread texture reuse: " + method.getName());
		});
		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread actorThread = new Thread(() -> {
			try {
				for (int round = 0; round < 20; round++) {
					for (int i = 0; i < crowd.size(); i++) {
						CharSprite sprite = crowd.get(i).sprite();
						check(sprite.texture == uploaded[i % 3], "snapshot textures are shared without rewriting");
						check(sprite.paused, "crowd sprites remain frozen");
						sprite.killAndErase();
					}
				}
			} catch (Throwable error) { failure.set(error); }
		}, "statue-actor-regression");
		try {
			actorThread.start(); actorThread.join(10000);
			check(!actorThread.isAlive(), "crowd construction completes");
			if (failure.get() != null) throw new AssertionError("Crowd texture reuse failed", failure.get());
		} finally {
			Gdx.gl = originalGL;
			for (SmartTexture texture : uploaded) texture.id = -1;
		}
		Rat rat = new Rat(); rat.pos = 70;
		Dungeon.level.mobs.add(rat);
		int population = Dungeon.level.mobCount();
		for (PetrifiedStatue statue : crowd) {
			Dungeon.level.mobs.add(statue);
			statue.sprite = statue.sprite(); statue.sprite.ch = statue;
		}
		check(Dungeon.level.mobCount() == population, "48 statues do not occupy monster slots");
		for (PetrifiedStatue statue : crowd) {
			statue.alignment = Char.Alignment.ENEMY;
			check(statue.spawningWeight() == 0, "statue weight is zero regardless of alignment");
		}
		check(Dungeon.level.mobCount() == population, "even externally changed alignment does not affect spawn cap");
		Dungeon.level.mobs.remove(rat);
		new AlarmTrap().set(60).activate();
		java.lang.reflect.Field emotion = CharSprite.class.getDeclaredField("emo");
		emotion.setAccessible(true);
		for (PetrifiedStatue statue : crowd) {
			statue.notice(); statue.aggro(Dungeon.hero);
			statue.sprite.showAlert(); statue.sprite.showLost(); statue.sprite.showSleep();
			check(statue.state == statue.PASSIVE, "alarm cannot wake a statue");
			check(emotion.get(statue.sprite) == null, "statues display no alert or other emotion");
			statue.state = statue.WANDERING; // Old saved alarms used to put statues in this state.
			Bundle saved = new Bundle(); saved.put("statue", statue);
			PetrifiedStatue restored = (PetrifiedStatue)saved.get("statue");
			check(restored.state == restored.PASSIVE && restored.alignment == Char.Alignment.NEUTRAL,
					"loading repairs old alarm/hostile statue state");
			check(restored.isActive() && restored.spawningWeight() == 0, "loaded statues remain inspectable without counting as monsters");
		}
		System.out.println("StatueCrowdRegression passed: 48 statues, 960 uploaded-texture reuses, alarms and spawn counts");
	}
}
