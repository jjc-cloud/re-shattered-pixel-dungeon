package com.shatteredpixel.shatteredpixeldungeon.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.journal.Journal;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.JournalScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndJournal;
import com.watabou.noosa.Game;
import com.watabou.utils.FileUtils;
import com.watabou.utils.GameSettings;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

/** 用隐藏桌面窗口验证真实日志 UI 的创建，不访问玩家配置和存档。 */
public class JournalRegression {
	private static int tabsChecked;
	public static void main(String[] args) {
		Game.version = "journal-regression";
		GameSettings.set((Preferences) Proxy.newProxyInstance(Preferences.class.getClassLoader(),
				new Class<?>[]{Preferences.class}, (proxy, method, values) -> {
					if (method.getName().startsWith("get") && values != null && values.length == 2) return values[1];
					if (method.getReturnType() == boolean.class) return false;
					return null;
				}));
		FileUtils.setDefaultFileProperties(Files.FileType.Absolute,
				new File("build/journal-regression").getAbsolutePath() + "/");
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setTitle("Journal regression");
		config.setInitialVisible(false);
		config.disableAudio(true);
		boolean portrait = args.length > 0 && args[0].equals("portrait");
		config.setWindowedMode(portrait ? 480 : 960, portrait ? 800 : 640);
		new Lwjgl3Application(new Game(ProbeScene.class, new DesktopPlatformSupport()), config);
	}

	public static class ProbeScene extends JournalScene {
		private int frames;

		@Override public void create() {
			Messages.setup(Languages.CHI_SMPL);
			Dungeon.hero = null;
			Dungeon.level = null;
			// 在隔离目录创建正常的空白日志档案，覆盖实际读取链路。
			if (tabsChecked == 0) Journal.saveGlobal(true);
			try {
				Field tab = JournalScene.class.getDeclaredField("lastIDX");
				tab.setAccessible(true);
				tab.setInt(null, Math.min(tabsChecked, 3));
			} catch (ReflectiveOperationException e) {
				throw new AssertionError(e);
			}
			super.create();
			if (tabsChecked == 4) {
				GamesInProgress.selectedClass = HeroClass.WARRIOR;
				Dungeon.init();
				Dungeon.level = new SewerLevel();
				Dungeon.level.create();
				add(new WndJournal());
			}
		}

		@Override public void update() {
			super.update();
			// 每页实际绘制一帧后再切换，覆盖创建、布局与渲染路径。
			if (++frames == 2) {
				tabsChecked++;
				if (tabsChecked < 5) {
					Game.switchScene(ProbeScene.class);
				} else {
					System.out.println("JournalRegression passed: 4 menu tabs without a level and the in-run journal created and rendered");
					Gdx.app.exit();
				}
			}
		}
	}
}
