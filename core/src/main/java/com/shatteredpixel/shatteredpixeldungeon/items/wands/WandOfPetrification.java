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

package com.shatteredpixel.shatteredpixeldungeon.items.wands;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Petrification;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

public class WandOfPetrification extends DamageWand {
	
	{
		image = ItemSpriteSheet.WAND_PETRIFICATION;
	}
	
	@Override
	protected void onZap(Ballistica bolt) {
		boolean hit = false;
		
		for (int c : bolt.subPath(1, bolt.dist)) {
			Char ch = Actor.findChar(c);
			if (ch != null) {
				hit = true;
				
				// Apply Petrification effect
				if (Petrification.canAffect(ch)) {
					Petrification petrification = Buff.affect(ch, Petrification.class);
					petrification.progress += 0.5f; // 50%石化负面
					
					// No particle effect as requested
				}
			}
		}
		
		// Create beam effect - using LightRay but more transparent
		Beam.PetrificationRay beam = new Beam.PetrificationRay(curUser.sprite.center(), DungeonTilemap.raisedTileCenterToWorld(bolt.collisionPos));
		curUser.sprite.parent.add(beam);
		
		Sample.INSTANCE.play(Assets.Sounds.RAY);
	}
	
	@Override
	public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {
		// Apply Petrification effect on hit - staff bonus effect
		if (Petrification.canAffect(defender)) {
			Petrification petrification = Buff.affect(defender, Petrification.class);
			// 10% + 魔杖等级 * 5%石化负面
			float bonus = 0.1f + staff.buffedLvl() * 0.05f;
			petrification.progress += bonus;
		}
	}
	
	@Override
	public String statsDesc() {
		return Messages.get(this, "stats_desc", min(), max());
	}
	
	@Override
	public String upgradeStat2(int level) {
		return Messages.get(this, "upgrade_stat2", 10 + level * 5);
	}
	
	@Override
	public void staffFx(MagesStaff.StaffParticle particle) {
		particle.color(0xCCCCCC); // Light gray color
		particle.am = 0.3f; // More transparent than prismatic
		particle.setLifespan(1f);
		particle.speed.polar(Random.Float(PointF.PI2), 1f);
		particle.setSize(0.5f, 1.5f);
		particle.radiateXY(0.3f);
	}
	
	@Override
	public String title() {
		return Messages.get(this, "title");
	}
	
	@Override
	public String desc() {
		return Messages.get(this, "desc");
	}
	
	@Override
	public int min(int level) {
		return 2 + level * 2;
	}
	
	@Override
	public int max(int level) {
		return 6 + level * 4;
	}
	
	@Override
	public int damageRoll(int level) {
		return Random.NormalIntRange(min(level), max(level));
	}
}