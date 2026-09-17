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
		add_v0_1_1_Changes(changeInfos);
		add_v0_1_0_Changes(changeInfos);
		add_01_ImportantChanges(changeInfos);
		addDevelopmentPreview(changeInfos);
	}

	public static void add_v0_1_1_Changes(ArrayList<ChangeInfo> changeInfos) {

		ChangeInfo changes = new ChangeInfo("v0.1.1", true, "");
		changes.hardlight(Window.TITLE_COLOR);
		changeInfos.add(changes);

		changes = new ChangeInfo("新内容", false, null);
		changes.hardlight(Window.TITLE_COLOR);
		changeInfos.add(changes);

		changes.addButton(new ChangeButton(ChangeIcons.V21_SHOPKEEPER, "订购",
				"\n新增商店订购功能，现在可以在商店处订购额外的物资。订购的物资没有全部购买，或是使用其他方法取得订购物品，在清空订购物品后会触发特殊对话并取消下一次的订购。"));
		changes.addButton(new ChangeButton(ChangeIcons.V010_VOID_WALKER, "公共天赋",
				"\n新增公共天赋池，使用蜕变密卷时额外抽取两个属于公共天赋池的天赋。\n-振奋一餐：进食冻结周围时间。\n-多重存在：操纵镜像的行为，并获得它们的视野。\n-逃脱计划：可以与一名敌人互换位置。\n-虚空行者：暂时不掉下悬崖。\n-魔能超载：无视充能限制使用法杖神器。\n-野蛮寄生：种子可以在生物上发芽生效。\n-扭曲现实：唯心的改写现实，交换两种药剂或卷轴的效果。\n-交叉火力：大幅增加友军伤害。\n-隐藏技：处决敌人。\n-火力倾泻：法杖与投掷武器不消耗时间。"));
		changes.addButton(new ChangeButton(ChangeIcons.V064_CHALLENGES, "新挑战",
				"\n新增一个陷阱相关挑战，自然生成的陷阱数量翻倍，半数隐藏陷阱只会在第二次触发时触发，探明后则会正常触发。"));
		changes.addButton(new ChangeButton(ChangeIcons.V050_STAIRS, "隐藏房间",
				"\n新增一个隐藏房间类型，里面会固定刷新一把高品质法杖，目前为体验石化法杖，只有一半权重为其他法杖，后续逐步取消优先。"));


		changes = new ChangeInfo("改动", false, null);
		changes.hardlight(CharSprite.WARNING);
		changeInfos.add(changes);

		changes.addButton(new ChangeButton(ChangeIcons.V080_GUARD,"怪物掉落调整",
				"\n现在虚空锁链由监狱守卫掉落，神偷袖章由疯狂小偷及其变种掉落，这两种神器现在不会出现在普通神器池。"));
		changes.addButton(new ChangeButton(ChangeIcons.V070_TOOLKIT,"实验性炼金调整",
				"\n现在在实验性炼金下成功炼制配方后可在炼金中按配方炼制。"));

		changes = new ChangeInfo("增强", false, null);
		changes.hardlight(CharSprite.POSITIVE);
		changeInfos.add(changes);

		changes.addButton(new ChangeButton(ChangeIcons.V035_WARRIOR_HEROARM, "狂战士",
				"\n降低了狂战士的怒气惩罚，提高了理智状态下的收益。现在狂战士的怒气在愤怒状态下不再自然削减，并且采用固定的五回合衰减1%。"));	
		changes.addButton(new ChangeButton(ChangeIcons.PD_GHOST, "幽妹调整",
				"\n现在幽妹可以和英雄一样无视限制穿戴任何非特殊装备，并且可以转移玩家已穿戴的诅咒装备。驱邪会一次性驱除幽妹的所有诅咒。"));	

		changes = new ChangeInfo("削弱", false, null);
		changes.hardlight(CharSprite.NEGATIVE);
		changeInfos.add(changes);

		changes.addButton(new ChangeButton(ChangeIcons.V035_WARRIOR_HEROARM, "战士转职",
				"\n削弱了角斗士纹章的特殊能力，现在和狂战士一样需要损失百分比最大生命值触发；下调了狂战士的攻击倍率。"));
		changes.addButton(new ChangeButton(ChangeIcons.V010_MEDUSA_EYE, "蛇发邪眼",
				"\n石化进度从33%下调为25%，现在在玩家视角不会被两回合击杀。"));
	}


	public static void add_v0_1_0_Changes(ArrayList<ChangeInfo> changeInfos) {

		ChangeInfo changes = new ChangeInfo("v0.1.0", true, "");
		changes.hardlight(Window.TITLE_COLOR);
		changeInfos.add(changes);

		changes = new ChangeInfo("新内容", false, null);
		changes.hardlight(Window.TITLE_COLOR);
		changeInfos.add(changes);

		changes.addButton(new ChangeButton(ChangeIcons.V035_WARRIOR_HEROARM, "战士转职重做",
				"\n现在战士转职后会破损纹章会修复为完整纹章，根据转职获得不同的效果。现在角斗士更专注于战斗技巧与抵御伤害，狂战士则更专注于进攻及怒气控制，预期强度会过高。"));
		changes.addButton(new ChangeButton(ChangeIcons.V010_WAND_OF_PETRIFICATION, "石化法杖",
				"\n新增石化法杖，发射石化光线使敌人石化为雕像，雕像可以阻挡敌人，但可被摧毁。"));
		changes.addButton(new ChangeButton(ChangeIcons.V010_MEDUSA_EYE, "蛇发邪眼",
				"\n新增邪眼稀有变种，蛇发邪眼会给予玩家不可逆的负面效果，_极度危险_。"));
		changes.addButton(new ChangeButton(ChangeIcons.V010_DUNGEON_BLUEPRINT, "地牢蓝图",
				"\n新增探地卷轴变种，探地卷轴有极低的概率生成为魔法地图或地牢蓝图，这两者都拥有强大的效果。"));

		changes = new ChangeInfo("改动", false, null);
		changes.hardlight(CharSprite.WARNING);
		changeInfos.add(changes);

		changes.addButton(new ChangeButton(ChangeIcons.V20_DUELIST_CLOTH,"决斗家调整",
				"\n现在决斗家初始双持独特的刺剑与破损的短剑，转职成勇士后会获得二连击，转职成武僧则会失去所有武器栏，并获得一个额外的饰品栏。"));
		changes.addButton(new ChangeButton(ChangeIcons.V010_HOLD_FAST,"天赋调整",
				"\n不动如山与液蕴意志天赋互换位置，现在不动如山会在战士进行原地攻击后触发，而非强制等待；液蕴意志改为液蕴复苏，使战士回血而非获得护盾。"));
		changes.addButton(new ChangeButton(ChangeIcons.V081_DISPLAY_VERT,"界面回调",
				"\n使用早期的像素风标题与boss击杀。"));		
		changes.addButton(new ChangeButton(ChangeIcons.V075_TENGU,"BOSS调整",
				"\n现在天狗与矮人国王会额外掉落一张蜕变密卷。"));

		changes = new ChangeInfo("增强", false, null);
		changes.hardlight(CharSprite.POSITIVE);
		changeInfos.add(changes);

		changes.addButton(new ChangeButton(ChangeIcons.V010_GLADIATOR,"角斗士",
				"\n角斗士的二连击现在会造成伤害，且战技强化移除了连击数需求。"));
		changes.addButton(new ChangeButton(ChangeIcons.V30_SHARED_UPGRADES,"联动升级",
				"\n联动升级无上限等级限制，且现在按投掷武器的阶数提供增幅，每阶提供2.5%/5%/7.5%伤害，每级提供倍数伤害。"));

		changes = new ChangeInfo("削弱", false, null);
		changes.hardlight(CharSprite.NEGATIVE);
		changeInfos.add(changes);

		changes.addButton(new ChangeButton(ChangeIcons.V035_WARRIOR_CLOTH, "战士",
				"\n只有战士削弱了你才知道你玩的是破碎地牢。"));


	}

	public static void add_01_ImportantChanges(ArrayList<ChangeInfo> changeInfos) {

		ChangeInfo changes = new ChangeInfo("重要改动", true, "");
		changes.hardlight(Window.TITLE_COLOR);
		changeInfos.add(changes);

		changes.addButton(new ChangeButton(ChangeIcons.V23_ARROWS, "基础属性调整",
				"\n现在所有角色的基础属性做出了区分。\n\n-战士拥有更快的生命回复速度。\n\n-法师拥有更快的法杖充能速度，不仅限于魔杖，但神器充能速度下降。\n\n-盗贼更加耐饿，且更容易发现地牢的秘密，但法杖充能速度下降。\n\n-女猎手更难以被怪物发现。\n\n-决斗家可以双持武器。\n\n-牧师拥有更快的神器充能速度，但受到的伤害更多。"));
		changes.addButton(new ChangeButton(ChangeIcons.V010_FLUORESCENT_MOSS, "环境",
				"\n现在灯火、地面的发光苔藓与火焰会照亮周围的环境，怪物在水面上移动时更容易被玩家发现，火焰会使水蒸发。"));
		changes.addButton(new ChangeButton(ChangeIcons.V070_ALCHEMY_POT, "实验性炼金",
				"\n新增实验性炼金，可以在未鉴定的情况下定向炼金，增加了一部分只能在实验性炼金中获得的物品。"));

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
