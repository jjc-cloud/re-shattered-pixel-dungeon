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
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.AntiMagic;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameMath;

public class Berserk extends Buff implements ActionIndicator.Action {

	{
		type = buffType.POSITIVE;
	}

	public int powerLossBuffer;
	private float power;
	private float pendingDamageRage;
	private Object pendingDamageSource;
	private boolean deathDefianceAvailable = true;
	private int levelsUntilDefiance;

	private static final String POWER = "power";
	private static final String POWER_BUFFER = "power_buffer";
	private static final String DEATH_DEFIANCE = "death_defiance";
	private static final String LEVELS_UNTIL_DEFIANCE = "levels_until_defiance";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(POWER, power);
		bundle.put(POWER_BUFFER, powerLossBuffer);
		bundle.put(DEATH_DEFIANCE, deathDefianceAvailable);
		bundle.put(LEVELS_UNTIL_DEFIANCE, levelsUntilDefiance);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		power = GameMath.gate(0f, bundle.getFloat(POWER), 4f);
		powerLossBuffer = bundle.getInt(POWER_BUFFER);
		deathDefianceAvailable = !bundle.contains(DEATH_DEFIANCE) || bundle.getBoolean(DEATH_DEFIANCE);
		levelsUntilDefiance = bundle.getInt(LEVELS_UNTIL_DEFIANCE);
	}

	@Override
	public boolean act() {
		if (target.buff(DeathDefianceIndicator.class) == null) Buff.affect(target, DeathDefianceIndicator.class);
		if (powerLossBuffer > 0) {
			powerLossBuffer--;
		} else if (power > 0f && power < 3f) {
			float decay = GameMath.gate(0.1f, power, 1f) * 0.05f
					* (float) Math.pow(target.HP / (float) target.HT, 2);
			if (power >= 2f) decay *= 2f;
			power = Math.max(0f, power - decay);
		}
		BuffIndicator.refreshHero();
		if (ActionIndicator.action != this) ActionIndicator.setAction(this);
		else ActionIndicator.refresh();
		spend(TICK);
		return true;
	}

	@Override
	public void fx(boolean on) {
		if (on) ActionIndicator.setAction(this);
	}

	@Override
	public void detach() {
		super.detach();
		ActionIndicator.clearAction(this);
	}

	public float power() {
		return power;
	}

	public float maxPower() {
		return 1f + ((Hero) target).pointsInTalent(Talent.ENDLESS_RAGE);
	}

	public void gainRage(float amount) {
		if (amount <= 0f) return;
		power = Math.max(power, Math.min(maxPower(), power + amount));
		powerLossBuffer = 3;
		BuffIndicator.refreshHero();
		ActionIndicator.refresh();
	}

	public void forceRage(float amount) {
		power = GameMath.gate(0f, amount, 4f);
		powerLossBuffer = 3;
		BuffIndicator.refreshHero();
		ActionIndicator.refresh();
	}

	public float damageMultiplier() {
		if (power < 2f) return 1f + 0.5f * power;
		if (power < 3f) return 2f - 0.5f * (power - 2f);
		return 1.5f + power - 3f;
	}

	public float incomingDamageMultiplier() {
		if (power < 2f) return 1f + 0.25f * power;
		if (power < 3f) return 1f;
		return 1.5f + 0.5f * (power - 3f);
	}

	public float accuracyMultiplier() {
		if (power < 2f) return 1f + 0.25f * power;
		if (power < 3f) return 1.5f + 0.5f * (power - 2f);
		return 2f + 3f * (power - 3f);
	}

	public float evasionMultiplier() {
		if (power < 2f) return 1f - 0.1f * power;
		if (power < 3f) return 0.8f + 0.2f * (power - 2f);
		return 0.2f;
	}

	public float speedMultiplier() {
		if (power < 2f) return 1f;
		if (power < 3f) return 1f + 0.25f * (power - 2f);
		return 1.25f + 0.25f * (power - 3f);
	}

	public float enchantFactor(float factor) {
		int rank = ((Hero) target).pointsInTalent(Talent.ENRAGED_CATALYST);
		if (rank == 0) return factor;
		float cap = rank == 1 ? 1.5f : rank == 2 ? 2.25f : 3f;
		return factor * (1f + Math.min(power / 2f, 1f) * (cap - 1f));
	}

	public float normalWandShotDelay() {
		return ((Hero) target).pointsInTalent(Talent.ENRAGED_CATALYST) >= 2 ? 0f : TICK;
	}

	public float damageFactor(float damage) {
		return damage * damageMultiplierWithDeathShield();
	}

	public float damageMultiplierWithDeathShield() {
		BrokenSeal.WarriorShield shield = target.buff(BrokenSeal.WarriorShield.class);
		return damageMultiplier() * (shield != null && shield.isDeathShield() ? 1.5f : 1f);
	}

	public void onAttackResolved(int hpBefore, int hpAfter) {
		gainRage(Math.max(0, hpBefore - hpAfter) * 0.005f);
	}

	public float modifyIncomingDamage(int rawDamage, Object source) {
		boolean indirectEnemyDamage = !(source instanceof Char) && isEnemyDamageSource(source);
		float multiplier = indirectEnemyDamage ? incomingDamageMultiplier() : 1f;
		if (source instanceof Char) {
			if (pendingDamageSource != source) queueDamageRage(0f, source);
		} else if (source instanceof Hunger) {
			queueDamageRage(rawDamage * 0.01f, source);
		} else if (indirectEnemyDamage && !AntiMagic.RESISTS.contains(source.getClass())) {
			queueDamageRage(rawDamage * 0.01f, source);
		} else {
			queueDamageRage(0f, source);
		}
		return rawDamage * multiplier;
	}

	public float modifyPhysicalIncomingDamage(int rawDamage, Char source) {
		float multiplier = isEnemyDamageSource(source) ? incomingDamageMultiplier() : 1f;
		queueDamageRage(rawDamage * 0.01f, source);
		return rawDamage * multiplier;
	}

	private void queueDamageRage(float amount, Object source) {
		pendingDamageRage = Math.max(0f, amount);
		pendingDamageSource = source;
	}

	public void resolvePendingDamageRage() {
		float amount = pendingDamageRage;
		pendingDamageRage = 0f;
		pendingDamageSource = null;
		if (amount > 0f && target != null && target.HP > 0) gainRage(amount);
	}

	public static boolean isEnemyDamageSource(Object source) {
		if (source instanceof Char) return ((Char) source).alignment == Char.Alignment.ENEMY;
		if (source == null) return false;
		for (Class<?> type = source.getClass().getEnclosingClass(); type != null; type = type.getEnclosingClass()) {
			if (Mob.class.isAssignableFrom(type)) return true;
		}
		return false;
	}

	// Compatibility bridge until incoming-damage hooks are migrated.
	public void damage(int damage) {
		if (damage > 0) gainRage((damage / (float) target.HT) / 4f);
	}

	public boolean berserking() {
		if (!(target instanceof Hero) || target.HP > 0 || !deathDefianceAvailable) return false;
		if (power >= 2f && power < 3f) return false;
		BrokenSeal.WarriorShield shield = target.buff(BrokenSeal.WarriorShield.class);
		if (shield == null || !shield.completeSealEquipped()) return false;
		consumeDeathDefiance();
		target.HP = Math.max(1, target.HT / 2);
		forceRage(4f);
		shield.activateDeathShield();
		if (target.sprite != null) {
			SpellSprite.show(target, SpellSprite.BERSERK);
			Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
			GameScene.flash(0x80FF0000);
		}
		return true;
	}

	public boolean deathDefianceAvailable() {
		return deathDefianceAvailable;
	}

	public int levelsUntilDeathDefiance() {
		if (deathDefianceAvailable) return 0;
		int rank = ((Hero) target).pointsInTalent(Talent.DEATHLESS_FURY);
		if (rank == 0) return 0;
		return levelsUntilDefiance <= 0 ? 4 - rank : Math.min(levelsUntilDefiance, 4 - rank);
	}

	public void consumeDeathDefiance() {
		deathDefianceAvailable = false;
		int rank = ((Hero) target).pointsInTalent(Talent.DEATHLESS_FURY);
		levelsUntilDefiance = rank == 0 ? 0 : 4 - rank;
		BuffIndicator.refreshHero();
	}

	public void onHeroLevelUp() {
		if (deathDefianceAvailable) return;
		int rank = ((Hero) target).pointsInTalent(Talent.DEATHLESS_FURY);
		if (rank == 0) return;
		if (levelsUntilDefiance <= 0) levelsUntilDefiance = 4 - rank;
		else levelsUntilDefiance = Math.min(levelsUntilDefiance, 4 - rank);
		if (--levelsUntilDefiance <= 0) deathDefianceAvailable = true;
		BuffIndicator.refreshHero();
	}

	public void recover(float percent) {
		// The redesigned controller has no post-berserk recovery state.
	}

	@Override
	public String actionName() {
		return Messages.get(this, "action_name");
	}

	@Override
	public int actionIcon() {
		return HeroIcon.BERSERK;
	}

	@Override
	public Visual primaryVisual() {
		Image icon = new HeroIcon(this);
		tintIcon(icon);
		return icon;
	}

	@Override
	public Visual secondaryVisual() {
		BitmapText text = new BitmapText(PixelScene.pixelFont);
		text.text((int) (power * 100f) + "%");
		if (power >= 3f) text.hardlight(0xFF0000);
		else if (power >= 2f) text.hardlight(0xFFFFFF);
		else text.hardlight(0xFF8000);
		text.measure();
		return text;
	}

	@Override
	public int indicatorColor() {
		if (power >= 3f) return 0x660000;
		if (power >= 2f) return 0x666666;
		return 0x664400;
	}

	@Override
	public void doAction() {
		if (power <= 0f || !(target instanceof Hero)) return;
		power = Math.max(0f, power - 1f);
		BuffIndicator.refreshHero();
		ActionIndicator.refresh();
		((Hero) target).spendAndNextConstant(TICK);
	}

	@Override
	public int icon() {
		return BuffIndicator.BERSERK;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.resetColor();
		if (power >= 3f) icon.hardlight(1f, 0f, 0f);
		else if (power >= 2f) icon.hardlight(1f, 1f, 1f);
		else icon.hardlight(1f, 0.5f, 0f);
	}

	@Override
	public float iconFadePercent() {
		return 1f - power / 4f;
	}

	@Override
	public String iconTextDisplay() {
		return (int) (power * 100f) + "%";
	}

	@Override
	public String name() {
		if (power >= 3f) return Messages.get(this, "berserk");
		if (power >= 2f) return Messages.get(this, "sane");
		return Messages.get(this, "angered");
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", power * 100f, damageMultiplier(),
				incomingDamageMultiplier(), accuracyMultiplier(), evasionMultiplier(), speedMultiplier());
	}

	public static class DeathDefianceIndicator extends Buff {
		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			Berserk rage = target.buff(Berserk.class);
			if (rage == null) return BuffIndicator.NONE;
			if (rage.deathDefianceAvailable() || ((Hero) target).pointsInTalent(Talent.DEATHLESS_FURY) > 0) {
				return BuffIndicator.ANKH;
			}
			return BuffIndicator.NONE;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.resetColor();
			Berserk rage = target.buff(Berserk.class);
			if (rage != null && !rage.deathDefianceAvailable()) icon.brightness(0.3f);
		}

		@Override
		public String iconTextDisplay() {
			Berserk rage = target.buff(Berserk.class);
			return rage != null && !rage.deathDefianceAvailable() && rage.levelsUntilDeathDefiance() > 0
					? Integer.toString(rage.levelsUntilDeathDefiance()) : "";
		}

		@Override
		public boolean act() {
			spend(TICK);
			return true;
		}

		@Override
		public String name() {
			return Messages.get(this, "name");
		}

		@Override
		public String desc() {
			Berserk rage = target.buff(Berserk.class);
			return Messages.get(this, rage != null && rage.deathDefianceAvailable() ? "desc_ready" : "desc_cooldown",
					rage == null ? 0 : rage.levelsUntilDeathDefiance());
		}
	}
}
