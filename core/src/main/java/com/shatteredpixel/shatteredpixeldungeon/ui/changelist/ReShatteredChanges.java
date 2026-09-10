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

package com.shatteredpixel.shatteredpixeldungeon.ui.changelist;

import com.shatteredpixel.shatteredpixeldungeon.scenes.ChangesScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;

import java.util.ArrayList;

public class ReShatteredChanges {

	public static void addAllChanges(ArrayList<ChangeInfo> changeInfos) {
		addDevelopmentPreview(changeInfos);
	}

	private static void addDevelopmentPreview(ArrayList<ChangeInfo> changeInfos) {
		ChangeInfo changes = new ChangeInfo("开发预览", true, "");
		changes.hardlight(Window.TITLE_COLOR);
		changeInfos.add(changes);

		changes = new ChangeInfo("新内容", false, null);
		changes.hardlight(Window.TITLE_COLOR);
		changeInfos.add(changes);

		changes.addButton(new ChangeButton(ChangeIcons.V40_CHANGES, "独立更新日志",
				"新增本作专属更新日志，并保留原版更新记录。点击页面顶部的按钮即可在两份日志之间切换。"));

		changes.addButton(new ChangeButton(ChangeIcons.V061_SPELLBOOK, "石化法杖",
				"加入石化法杖相关内容，为敌人控制与战斗策略提供新的选择。"));

		changes = new ChangeInfo("改动", false, null);
		changes.hardlight(CharSprite.WARNING);
		changeInfos.add(changes);

		changes.addButton(new ChangeButton(ChangeIcons.V081_MISC, "环境感知",
				"扩展英雄对周围环境与光照变化的感知表现。后续细节会随着版本整理继续补充。"));
	}
}
