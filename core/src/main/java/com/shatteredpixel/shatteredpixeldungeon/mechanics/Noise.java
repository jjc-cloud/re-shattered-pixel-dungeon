package com.shatteredpixel.shatteredpixeldungeon.mechanics;

import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;

/**
 * 非怪物侧行为发出的声音。开启集群智能时，空闲的怪物会前往最后听到的声源地。
 * wakeSleepers 为 true 时，范围内的睡眠怪物也会被惊醒并前往声源地。
 * 音频播放仍由各行为自己的 Sample 调用负责，这里只处理游戏逻辑。
 */
public final class Noise {

	public static final int WATER_RANGE = 6;
	public static final int EXPLOSION_RANGE = 8;
	public static final int THROW_RANGE = 4;

	private Noise(){}

	/** 在 cell 处发出声音。source 为 null 表示无来源物品/环境声。 */
	public static void emit(Char source, int cell, int range, boolean wakeSleepers) {
		if (!Dungeon.isChallenged(Challenges.SWARM_INTELLIGENCE)) return;
		if (Dungeon.level == null || Dungeon.hero == null) return;
		//只听非怪物侧的声音
		if (source instanceof Mob) return;

		for (Char ch : Actor.chars()) {
			if (!(ch instanceof Mob)) continue;
			Mob mob = (Mob) ch;
			if (!mob.isAlive() || mob.alignment != Char.Alignment.ENEMY) continue;
			//只响应未追击/逃跑的怪物；睡眠怪物只在声音足够响时被惊醒
			if (mob.state == mob.PASSIVE
					|| mob.state == mob.HUNTING || mob.state == mob.FLEEING) continue;
			if (mob.state == mob.SLEEPING && !wakeSleepers) continue;
			if (Dungeon.level.distance(mob.pos, cell) > range) continue;
			//基础响应：走向最后听到的声源地
			mob.beckon(cell);
		}
	}
}
