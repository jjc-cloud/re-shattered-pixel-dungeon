package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Petrification;
import com.shatteredpixel.shatteredpixeldungeon.sprites.EyeSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MedusaEyeSprite;
import com.watabou.utils.Bundle;

/** A rare eye which channels petrification instead of firing a death ray. */
public class MedusaEye extends Eye {
	{
		spriteClass = MedusaEyeSprite.class;
		HUNTING = new PetrifyingHunting();

		immunities.add(Petrification.class);
	}

	private int gazeTarget = -1;
	private int rejectedTarget = -1;

	private boolean useMelee(Char target) {
		return !Petrification.canAffect(target) || target.id() == rejectedTarget;
	}

	private void stopGaze() {
		beamCharged = false;
		gazeTarget = -1;
		if (sprite != null) sprite.idle();
	}

	@Override
	protected boolean canAttack(Char enemy) {
		if (enemy != null && useMelee(enemy)) return canMeleeAttack(enemy);
		return enemy != null && enemy.isAlive() && enemy.invisible == 0
				&& !isCharmedBy(enemy) && fieldOfView[enemy.pos];
	}

	@Override
	protected boolean doAttack(Char enemy) {
		if (useMelee(enemy)) {
			stopGaze();
			return doMeleeAttack(enemy);
		}
		if (!beamCharged || gazeTarget != enemy.id()) {
			gazeTarget = enemy.id();
			beamCharged = true;
			if (sprite != null) ((EyeSprite)sprite).charge(enemy.pos);
		} else {
			if (sprite != null) sprite.turnTo(pos, enemy.pos);
			if (!Petrification.apply(enemy)) {
				rejectedTarget = enemy.id();
				stopGaze();
			}
		}
		spend(TICK);
		return true;
	}

	private class PetrifyingHunting extends Mob.Hunting {
		@Override
		public boolean act(boolean enemyInFOV, boolean justAlerted) {
			if (beamCharged && (enemy == null || useMelee(enemy) || !canAttack(enemy))) stopGaze();
			return super.act(enemyInFOV, justAlerted);
		}
	}

	@Override public com.shatteredpixel.shatteredpixeldungeon.items.Item createLoot() {
		return new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfSirensSong();
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("gaze_target", gazeTarget);
		bundle.put("rejected_target", rejectedTarget);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		gazeTarget = bundle.contains("gaze_target") ? bundle.getInt("gaze_target") : -1;
		rejectedTarget = bundle.contains("rejected_target") ? bundle.getInt("rejected_target") : -1;
	}
}
