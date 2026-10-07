/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BloodParticle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoMob;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Point;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class BossHealthBar extends Component {


	private static final String asset = Assets.Interfaces.BOSSHP;
	private static final LinkedHashMap<Mob, BossState> bosses = new LinkedHashMap<>();
	private final LinkedHashMap<Mob, BossBar> rows = new LinkedHashMap<>();
	private Camera contentCamera;

	private static class BossState {
		private volatile boolean bleeding;
		private boolean refreshSprite;
	}

	public BossHealthBar() {
		super();
		syncRows();
	}

	@Override
	protected void createChildren() {
		width = SPDSettings.interfaceSize() != 0 ? 128 : 64;
		contentCamera = Camera.add(Camera.createFullscreen(PixelScene.uiCamera.zoom));
	}

	@Override
	public synchronized void destroy() {
		super.destroy();
		rows.clear();
		Camera.remove(contentCamera);
		contentCamera.destroy();
	}

	@Override
	public void update() {
		syncRows();
		super.update();
		layout();
	}

	private void syncRows() {
		synchronized (bosses) {
			// iOS 类库没有 Predicate/removeIf；无 Boss 时也会经过这个入口。
			Iterator<Map.Entry<Mob, BossState>> bossIterator = bosses.entrySet().iterator();
			while (bossIterator.hasNext()) {
				Mob boss = bossIterator.next().getKey();
				if (!boss.isAlive() || Dungeon.level == null || !Dungeon.level.mobs.contains(boss)) {
					bossIterator.remove();
				}
			}
			Iterator<Map.Entry<Mob, BossBar>> iterator = rows.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<Mob, BossBar> entry = iterator.next();
				if (!bosses.containsKey(entry.getKey())) {
					remove(entry.getValue());
					entry.getValue().destroy();
					iterator.remove();
				}
			}
			for (Map.Entry<Mob, BossState> entry : bosses.entrySet()) {
				BossBar row = rows.get(entry.getKey());
				if (row == null) {
					row = new BossBar(entry.getKey(), entry.getValue());
					row.camera = contentCamera;
					rows.put(entry.getKey(), row);
					add(row);
				} else if (entry.getValue().refreshSprite) {
					row.refreshSprite();
				}
				entry.getValue().refreshSprite = false;
			}
		}
		visible = !rows.isEmpty();
		// 隐藏时仍同步登记表，下一只 Boss 出现后可以立即显示。
		active = true;
		layout();
	}

	@Override
	protected void layout() {
		float rowY = 0;
		float maxRowHeight = 0;
		for (BossBar row : rows.values()) {
			row.setPos(0, rowY);
			maxRowHeight = Math.max(maxRowHeight, row.height());
			rowY += row.height() + 2;
		}
		float contentHeight = rows.isEmpty() ? 0 : rowY - 2;
		Camera ui = PixelScene.uiCamera;
		float scale = 1f;
		if (rows.size() > 1) {
			// 使用统一的空间预算：至多约两条血条高，同时不超过屏幕高度的 24%。
			float availableHeight = Math.max(1f, Math.min(ui.height - y - 4f,
					Math.min(ui.height * 0.24f, maxRowHeight * 2f + 2f)));
			float availableWidth = Math.max(1f, Math.min(ui.width * 0.5f,
					2f * Math.min(x + width/2f, ui.width - x - width/2f) - 4f));
			scale = Math.min(1f, Math.min(availableWidth/width, availableHeight/contentHeight));
		}
		height = contentHeight * scale;

		// 内容使用独立摄像机，绘制和点击一起缩放；保留父界面分配的中心锚点。
		Point origin = ui.cameraToScreen(x + width * (1f - scale)/2f, y);
		float zoom = ui.zoom * scale;
		if (contentCamera.zoom != zoom) contentCamera.zoom(zoom);
		contentCamera.x = contentCamera.y = 0;
		contentCamera.resize((int)Math.ceil(Game.width/zoom), (int)Math.ceil(Game.height/zoom));
		contentCamera.scroll.set(-origin.x/zoom, -origin.y/zoom);
		contentCamera.update();
	}

	public static void assignBoss(Mob boss) {
		assignBoss(boss, false);
	}

	public static void assignBoss(Mob boss, boolean forceSpriteRefresh) {
		synchronized (bosses) {
			if (boss == null) {
				bosses.clear();
				return;
			}
			BossState state = bosses.get(boss);
			if (state == null) {
				state = new BossState();
				bosses.put(boss, state);
			}
			state.refreshSprite |= forceSpriteRefresh;
		}
	}

	public static boolean isAssigned() {
		synchronized (bosses) {
			for (Mob boss : bosses.keySet()) {
				if (boss.isAlive() && Dungeon.level != null && Dungeon.level.mobs.contains(boss)) return true;
			}
		}
		return false;
	}

	public static boolean isAssigned(Mob boss) {
		synchronized (bosses) {
			return boss != null && bosses.containsKey(boss) && boss.isAlive()
					&& Dungeon.level != null && Dungeon.level.mobs.contains(boss);
		}
	}

	public static void bleed(boolean value) {
		synchronized (bosses) {
			for (BossState state : bosses.values()) state.bleeding = value;
		}
	}

	public static void bleed(Mob boss, boolean value) {
		synchronized (bosses) {
			BossState state = bosses.get(boss);
			if (state != null) state.bleeding = value;
		}
	}

	public static boolean isBleeding() {
		synchronized (bosses) {
			for (Map.Entry<Mob, BossState> entry : bosses.entrySet()) {
				if (entry.getValue().bleeding && isAssigned(entry.getKey())) return true;
			}
		}
		return false;
	}

	public static boolean isBleeding(Mob boss) {
		synchronized (bosses) {
			BossState state = bosses.get(boss);
			return state != null && state.bleeding && isAssigned(boss);
		}
	}

	private static class BossBar extends Component {
		private Image bar;

		private Image shieldHP;
		private Image hp;
		private Image Dot; //a visual darkening over HP and shield that shows total incoming DOT
		private BitmapText hpText;

		private Button bossInfo;
		private BuffIndicator buffs;

		private final Mob boss;
		private final BossState state;

		private Image skull;
		private Emitter blood;

		private boolean large;

		public BossBar(Mob boss, BossState state) {
			super();
			this.boss = boss;
			this.state = state;
			buffs = new BuffIndicator(boss, large);
			BuffIndicator.setBossInstance(buffs);
			add(buffs);
			refreshSprite();
		}

		private void refreshSprite() {
			remove(skull);
			skull.destroy();
			skull = boss.sprite();
			add(skull);
			// 新头像立即接上本 Boss 的阶段状态，避免首帧颜色或粒子串用。
			if (state.bleeding) skull.tint(0xcc0000, large ? 0.3f : 0.6f);
			blood.on = state.bleeding;
			layout();
		}

		@Override
		protected void createChildren() {
			this.large = SPDSettings.interfaceSize() != 0;

			bar = large ? new Image(asset, 0, 16, 128, 30) : new Image(asset, 0, 0, 64, 16);
			add(bar);

			width = bar.width;
			height = bar.height;

			shieldHP = large ? new Image(asset, 0, 55, 96, 9) : new Image(asset, 71, 5, 47, 4);
			add(shieldHP);

			hp = large ? new Image(asset, 0, 46, 96, 9) : new Image(asset, 71, 0, 47, 4);
			add(hp);

			Dot = large ? new Image(asset, 0, 46, 96, 9) : new Image(asset, 71, 0, 47, 4);
			Dot.hardlight(0, 0, 0);
			Dot.alpha(0.25f);
			add(Dot);

			hpText = new BitmapText(PixelScene.pixelFont);
			hpText.alpha(0.6f);
			add(hpText);

			bossInfo = new Button(){
				@Override
				protected void onClick() {
					super.onClick();
					if (boss != null){
						GameScene.show(new WndInfoMob(boss));
					}
				}

				@Override
				protected String hoverText() {
					if (boss != null){
						return boss.name();
					}
					return super.hoverText();
				}
			};
			add(bossInfo);

			skull = new Image(asset, 64, 0, 6, 6);
			add(skull);

			blood = new Emitter();
			blood.pos(skull);
			blood.pour(BloodParticle.FACTORY, 0.3f);
			blood.autoKill = false;
			blood.on = false;
			add( blood );
		}

		@Override
		protected void layout() {
			bar.x = x;
			bar.y = y;

			hp.x = shieldHP.x = Dot.x = bar.x+(large ? 30 : 15);
			hp.y = shieldHP.y = Dot.y = bar.y+(large ? 2 : 3);

			if (!large) hpText.scale.set(PixelScene.align(0.5f));
			hpText.x = hp.x + (large ? (96-hpText.width())/2f : 1);
			hpText.y = hp.y + (hp.height - (hpText.baseLine()+hpText.scale.y))/2f;
			hpText.y -= 0.001f; //prefer to be slightly higher
			PixelScene.align(hpText);

			bossInfo.setRect(x, y, bar.width, bar.height);

			if (buffs != null) {
				buffs.maxBuffs = 12;
				if (large) {
					//little extra width here for a 6th column
					buffs.setRect(hp.x+1, hp.y + 12, 102, 34);
				} else {
					buffs.setRect(hp.x, hp.y + 5, 47, 16);
				}
			}

			int paneSize = large ? 30 : 16;

			skull.scale.set(Math.min(1f, (large ? 24f : 12f)/Math.max(skull.width, skull.height)));
			skull.x = bar.x + (paneSize - skull.width())/2f;
			skull.y = bar.y + (paneSize - skull.height())/2f;
			PixelScene.align(skull);
			blood.pos(skull);

			height = Math.max(bar.height, buffs.top() - y + buffs.contentHeight);
		}

		@Override
		public void update() {
			super.update();
			if (boss.isAlive()) {
				int health = boss.HP;
				int shield = boss.shielding();
				int incomingDOT = boss.incomingDOT();
				int max = Math.max(1, boss.HT);

				float healthPercent = health/(float)max;
				float shieldPercent = shield/(float)max;
				float DOTPercent    = incomingDOT/(float)max;

				if (healthPercent + shieldPercent > 1f){
					float excess = healthPercent + shieldPercent;
					healthPercent /= excess;
					shieldPercent /= excess;
					DOTPercent    /= excess;
				}

				hp.scale.x = healthPercent;
				shieldHP.scale.x = healthPercent + shieldPercent;
				Dot.scale.x = Math.min(DOTPercent, shieldHP.scale.x);
				Dot.x = shieldHP.x + shieldHP.width() - Dot.width();

				if (state.bleeding != blood.on){
					if (state.bleeding)   skull.tint( 0xcc0000, large ? 0.3f : 0.6f );
					else            skull.resetColor();
					bringToFront(blood);
					blood.pos(skull);
					blood.on = state.bleeding;
				}

				if (shield <= 0){
					hpText.text(health + "/" + max);
				} else {
					hpText.text(health + "+" + shield +  "/" + max);
				}
				hpText.measure();
				hpText.x = hp.x + (large ? (96-hpText.width())/2f : 1);

			}
		}
	}
}
