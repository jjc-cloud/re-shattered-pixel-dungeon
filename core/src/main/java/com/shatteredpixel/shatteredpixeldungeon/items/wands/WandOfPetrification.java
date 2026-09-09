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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Petrification;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

public class WandOfPetrification extends Wand {

	private static final float ZAP_PROGRESS = 0.5f;

	{
		image = ItemSpriteSheet.WAND_PETRIFICATION;
	}

	@Override
	public void onZap(Ballistica bolt) {
		Char ch = Actor.findChar(bolt.collisionPos);
		if (ch != null) {
			wandProc(ch, chargesPerCast());
			Petrification.apply(ch, ZAP_PROGRESS);
		} else {
			Dungeon.level.pressCell(bolt.collisionPos);
		}
	}

	@Override
	public void fx(Ballistica bolt, Callback callback) {
		Beam.LightRay beam = new Beam.LightRay(curUser.sprite.center(),
				DungeonTilemap.raisedTileCenterToWorld(bolt.collisionPos));
		beam.tint(0.8f, 0.8f, 0.8f, 0.3f);
		curUser.sprite.parent.add(beam);
		Sample.INSTANCE.play(Assets.Sounds.RAY);
		callback.call();
	}

	@Override
	public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {
		float progress = (0.1f + staff.buffedLvl() * 0.05f) * procChanceMultiplier(attacker);
		Petrification.apply(defender, progress);
	}

	@Override
	public void staffFx(MagesStaff.StaffParticle particle) {
		particle.color(0xCCCCCC);
		particle.am = 0.3f;
		particle.setLifespan(1f);
		particle.speed.polar(Random.Float(PointF.PI2), 1f);
		particle.setSize(0.5f, 1.5f);
		particle.radiateXY(0.3f);
	}
}
