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

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.ChangesScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.utils.DeviceCompat;

import java.util.ArrayList;

public class ReShatteredChanges {

	public static void addAllChanges(ArrayList<ChangeInfo> changeInfos) {
		add_v0_1_0_Changes(changeInfos);
		addDevelopmentPreview(changeInfos);
	}

	public static void add_v0_1_0_Changes(ArrayList<ChangeInfo> changeInfos) {

		ChangeInfo changes = new ChangeInfo("v0.1.0", true, "");
		changes.hardlight(Window.TITLE_COLOR);
		changeInfos.add(changes);

		changes = new ChangeInfo("新内容", false, null);
		changes.hardlight(Window.TITLE_COLOR);
		changeInfos.add(changes);

		changes.addButton(new ChangeButton(ChangeIcons.V035_WARRIOR_HEROARM, "战士转职重做",
				"\n现在战士转职后会破损纹章会修复为完整纹章，根据转职获得不同的效果。现在角斗士更专注于战斗技巧与抵御伤害，狂战士则更专注于进攻及怒气控制。"));
		changes.addButton(new ChangeButton(ChangeIcons.V010_FLUORESCENT_MOSS, "环境感知",
				"\n现在灯火、地面的发光苔藓与火焰会照亮周围的环境，怪物在水面上移动时更容易被玩家发现。"));
		changes.addButton(new ChangeButton(ChangeIcons.V010_WAND_OF_PETRIFICATION, "石化法杖",
				"\n新增石化法杖，发射石化光线使敌人石化为雕像，雕像可以阻挡敌人，但可被摧毁。"));
		changes.addButton(new ChangeButton(ChangeIcons.V070_ALCHEMY_POT, "实验性炼金",
				"\n新增实验性炼金，可以在未鉴定的情况下定向炼金，增加了一部分只能在实验性炼金中获得的物品。"));
		changes.addButton(new ChangeButton(ChangeIcons.V010_MEDUSA_EYE, "蛇发邪眼",
				"\n新增邪眼稀有变种，蛇发邪眼会给予玩家不可逆的负面效果，_极度危险_。"));
		changes.addButton(new ChangeButton(ChangeIcons.V010_DUNGEON_BLUEPRINT, "地牢蓝图",
				"\n新增探地卷轴变种，探地卷轴有极低的概率生成为魔法地图或地牢蓝图，这两者都拥有强大的效果。"));

		changes = new ChangeInfo("改动", false, null);
		changes.hardlight(CharSprite.WARNING);
		changeInfos.add(changes);

		changes.addButton(new ChangeButton(ChangeIcons.V23_ARROWS, "基础属性调整",
				"\n现在所有角色的基础属性做出了区分。\n\n-战士拥有更快的生命回复速度。\n\n-法师拥有更快的法杖充能速度，不仅限于魔杖，但神器充能速度下降。\n\n-盗贼更加耐饿，且更容易发现地牢的秘密，但法杖充能速度下降。\n\n-女猎手更难以被怪物发现。\n\n-决斗家可以双持武器。\n\n-牧师拥有更快的神器充能速度，但受到的伤害更多。"));
		changes.addButton(new ChangeButton(ChangeIcons.V20_DUELIST_CLOTH,"决斗家调整",
				"\n现在决斗家初始双持独特的刺剑与破损的短剑，转职成勇士后会获得二连击，转职成武僧则会失去所有武器栏，并获得一个额外的饰品栏。"));
	}

	public static void addDevelopmentPreview(ArrayList<ChangeInfo> changeInfos) {
		ChangeInfo changes = new ChangeInfo("独立页面", true, "");
		changes.hardlight(Window.TITLE_COLOR);
		changeInfos.add(changes);

		changes = new ChangeInfo("新内容", false, null);
		changes.hardlight(Window.TITLE_COLOR);
		changeInfos.add(changes);

		changes.addButton(new ChangeButton(ChangeIcons.V40_CHANGES, "独立更新日志",
				"\n新增更新日志，独立于原版更新记录。"));

		changes = new ChangeInfo("削弱", false, null);
		changes.hardlight(CharSprite.NEGATIVE);
		changeInfos.add(changes);

		changes.addButton(new ChangeButton(ChangeIcons.V035_WARRIOR_CLOTH, "战士的立绘太强大了",
				"\n将战士的立绘，武器和纹章回退到了早期版本。"));

	}
}
