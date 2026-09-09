package com.shatteredpixel.shatteredpixeldungeon.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.PetrifiedStatue;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.gltextures.TextureCache;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/** Real desktop GL context, hidden window, actor-thread reuse of uploaded statue textures. */
public class StatueTextureDesktopRegression extends ApplicationAdapter {
	private final ConcurrentLinkedQueue<CharSprite> ready = new ConcurrentLinkedQueue<>();
	private final AtomicReference<Throwable> failure = new AtomicReference<>();
	private Thread actor;
	private int completed;
	private long started;

	@Override public void create() {
		started = System.nanoTime();
		ArrayList<PetrifiedStatue> statues = new ArrayList<>();
		for (int i = 0; i < 48; i++) {
			PetrifiedStatue statue = new PetrifiedStatue();
			statue.imageWidth = 16; statue.imageHeight = 18;
			statue.pixels = new int[16 * 18];
			java.util.Arrays.fill(statue.pixels, 0x666666FF + (i % 3) * 0x11111100);
			statues.add(statue);
		}
		for (int i = 0; i < 3; i++) statues.get(i).sprite().texture.bind();
		actor = new Thread(() -> {
			try {
				for (int round = 0; round < 20; round++) for (PetrifiedStatue statue : statues)
					ready.add(statue.sprite());
			} catch (Throwable error) { failure.set(error); }
		}, "statue-actor-gl-regression");
		actor.start();
	}
	@Override public void render() {
		if (failure.get() != null) { Gdx.app.exit(); return; }
		CharSprite sprite;
		while ((sprite = ready.poll()) != null) {
			sprite.texture.bind();
			if (Gdx.gl.glGetError() != GL20.GL_NO_ERROR) {
				failure.set(new AssertionError("GL error while reusing statue texture"));
				Gdx.app.exit(); return;
			}
			completed++;
		}
		if (!actor.isAlive() && ready.isEmpty()) Gdx.app.exit();
		if (System.nanoTime() - started > 20_000_000_000L) {
			failure.set(new AssertionError("Desktop texture test timed out"));
			Gdx.app.exit();
		}
	}
	@Override public void dispose() { TextureCache.clear(); }
	public static void main(String[] args) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setInitialVisible(false);
		config.setWindowedMode(64, 64);
		config.disableAudio(true);
		StatueTextureDesktopRegression test = new StatueTextureDesktopRegression();
		new Lwjgl3Application(test, config);
		if (test.failure.get() != null) throw new AssertionError("Desktop GL regression failed", test.failure.get());
		if (test.completed != 960) throw new AssertionError("Incomplete desktop GL regression: " + test.completed);
		System.out.println("StatueTextureDesktopRegression passed: 48 statues, 960 reuses with a real desktop GL context");
	}
}
