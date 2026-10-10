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

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

public class SwarmIntelTracker extends Buff {

	// 仅用于一次检查内复用视野，不属于存档状态。
	public static long sightCheck;

	// 每个被发现的单位各自持有一份记录；呼叫者死亡也保留身份，直到完全脱离发现。
	public int callerId = -1;
	public float timeSeenAt = Float.MAX_VALUE;
	public int alertRange;
	private float leftAtZero;
	private int trackingDepth = Dungeon.depth;
	private int trackingBranch = Dungeon.branch;

	/** 检查所有发现者，呼叫者暂时失去视野时只暂停呼叫，不让其他怪物接班。 */
	public void refreshSight() {
		if (trackingDepth != Dungeon.depth || trackingBranch != Dungeon.branch) {
			callerId = -1;
			timeSeenAt = Float.MAX_VALUE;
			alertRange = 0;
			trackingDepth = Dungeon.depth;
			trackingBranch = Dungeon.branch;
		}
		boolean seen = false;
		boolean callerSees = false;
		if (Dungeon.isChallenged(Challenges.SWARM_INTELLIGENCE) && target.isAlive()) {
			for (Mob mob : Dungeon.level.mobs) {
				if (mob.seesSwarmEnemy(target)) {
					seen = true;
					if (mob.id() == callerId) callerSees = true;
				}
			}
		}
		if (!seen) callerId = -1;
		if (!callerSees) {
			timeSeenAt = Float.MAX_VALUE;
			alertRange = 0;
		} else if (timeSeenAt != Float.MAX_VALUE) {
			alertRange = Math.min(8, 4 + 2 * (int)Math.max(0, Actor.now() - timeSeenAt));
		}
	}

	/** 第一发现者登记并呼叫援军；每个目标的范围独立从 4 格增长到 8 格。 */
	public void onSeen(Mob caller) {
		refreshSight();
		if (callerId == -1) callerId = caller.id();
		if (callerId != caller.id()) return;
		if (timeSeenAt == Float.MAX_VALUE) timeSeenAt = Actor.now();
		alertRange = Math.min(8, 4 + 2 * (int)Math.max(0, Actor.now() - timeSeenAt));
		leftAtZero = 5f;

		for (Mob mob : Dungeon.level.mobs) {
			if (mob.alignment != Char.Alignment.ENEMY || mob.paralysed > 0 || mob.state == mob.HUNTING
					|| Dungeon.level.distance(caller.pos, mob.pos) > alertRange || !mob.isSwarmEnemy(target)) continue;
			// 同时收到多份呼叫时选择最近目标，等距时按 Actor ID 固定选择。
			Char destination = target;
			for (Char alternative : Actor.chars()) {
				SwarmIntelTracker other = alternative.buff(SwarmIntelTracker.class);
				if (other == null || other.alertRange == 0 || !mob.isSwarmEnemy(alternative)) continue;
				Actor owner = Actor.findById(other.callerId);
				if (!(owner instanceof Mob) || !((Mob) owner).seesSwarmEnemy(alternative)
						|| Dungeon.level.distance(((Mob) owner).pos, mob.pos) > other.alertRange) continue;
				int distance = Dungeon.level.distance(mob.pos, alternative.pos);
				int currentDistance = Dungeon.level.distance(mob.pos, destination.pos);
				if (distance < currentDistance || (distance == currentDistance && alternative.id() < destination.id())) {
					destination = alternative;
				}
			}
			mob.beckon(destination.pos);
		}
	}

	@Override
	public boolean act() {

		if (!target.isAlive()) {
			detach();
			return true;
		}
		sightCheck++;
		refreshSight();
		float lowestCooldown = 1;
		for (Mob m : Dungeon.level.mobs){
			if (m.cooldown() > 0 && m.cooldown() < lowestCooldown){
				lowestCooldown = m.cooldown();
			}
		}

		if (alertRange > 0){
			leftAtZero = 5f;
		} else {
			leftAtZero = Math.max(0, leftAtZero - lowestCooldown);
		}

		//always acts right after the next mob, or 1 turn at most
		spend(lowestCooldown);
		return true;
	}

	@Override
	public int icon() {
		if (alertRange == 0 && leftAtZero <= 0){
			return BuffIndicator.NONE;
		} else {
			return BuffIndicator.TARGETED;
		}
	}

	@Override
	public float iconFadePercent() {
		return (8-alertRange)/8f;
	}

	@Override
	public void tintIcon(Image icon) {
		if (alertRange == 0){
			icon.brightness(0.5f);
		} else {
			icon.resetColor();
		}
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(alertRange);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", alertRange);
	}

	public static final String ALERT_RANGE = "alert_range";
	public static final String LEFT = "left";


	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(ALERT_RANGE, alertRange);
		bundle.put(LEFT, leftAtZero);
		bundle.put("caller_id", callerId);
		bundle.put("time_seen_at", timeSeenAt);
		bundle.put("tracking_depth", trackingDepth);
		bundle.put("tracking_branch", trackingBranch);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		alertRange = bundle.getInt(ALERT_RANGE);
		leftAtZero = bundle.getFloat(LEFT);
		callerId = bundle.getInt("caller_id");
		timeSeenAt = bundle.getFloat("time_seen_at");
		trackingDepth = bundle.getInt("tracking_depth");
		trackingBranch = bundle.getInt("tracking_branch");
	}
}
