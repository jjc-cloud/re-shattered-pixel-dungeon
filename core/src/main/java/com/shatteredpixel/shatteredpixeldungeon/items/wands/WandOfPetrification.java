package com.shatteredpixel.shatteredpixeldungeon.items.wands;

import static com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfSpellpower.scaleEffect;

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
			float progress = scaleSpellEffect(100 * ZAP_PROGRESS, spellPower()) / 100f;
			if (progress > 0) Petrification.apply(ch, progress);
		} else {
			Dungeon.level.pressCell(bolt.collisionPos);
		}
	}

	@Override
	public void fx(Ballistica bolt, Callback callback) {
		Beam.LightRay beam = new Beam.LightRay(fxSource().center(),
				DungeonTilemap.raisedTileCenterToWorld(bolt.collisionPos));
		beam.tint(0.5f, 0.5f, 0.5f, 1f);
		fxSource().parent.add(beam);
		Sample.INSTANCE.play(Assets.Sounds.RAY);
		callback.call();
	}

	@Override
	public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {
		float progress = (0.1f + staff.buffedLvl() * 0.05f) * procChanceMultiplier(attacker);
		Petrification.apply(defender, scaleEffect(attacker, progress));
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
