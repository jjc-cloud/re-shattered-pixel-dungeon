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

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.MirrorImage;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

public class MirrorLink extends Item {

	private static final String AC_DIRECT = "DIRECT";
	private static final String BOUND_MIRROR = "bound_mirror";

	//the bound mirror is only replaced when it dies, so that directing doesn't jump between mirrors
	private int boundMirrorID = -1;

	{
		image = ItemSpriteSheet.MIRROR_LINK;
		defaultAction = AC_DIRECT;
		unique = true;
		bones = false;
		keptThoughLostInvent = true;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.remove(AC_DROP);
		actions.remove(AC_THROW);
		if (!actions.contains(AC_DIRECT)) actions.add(AC_DIRECT);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (!AC_DIRECT.equals(action) || !hero.hasTalent(Talent.MULTIPLE_EXISTENCE)) return;

		MirrorImage bound = boundMirror();

		if (bound == null) {
			MirrorImage closest = null;
			float distance = Float.MAX_VALUE;
			for (Mob mob : Dungeon.level.mobs) {
				if (mob instanceof MirrorImage && mob.isAlive()) {
					float candidateDistance = Dungeon.level.trueDistance(hero.pos, mob.pos);
					if (candidateDistance < distance) {
						distance = candidateDistance;
						closest = (MirrorImage) mob;
					}
				}
			}

			if (closest == null) {
				GLog.w(Messages.get(this, "no_images"));
				return;
			}

			boundMirrorID = closest.id();
			bound = closest;
			GLog.p(Messages.get(this, "bind"));
		}

		for (Mob mob : Dungeon.level.mobs) {
			if (mob instanceof MirrorImage) {
				if (mob == bound) ((MirrorImage) mob).enableDirection();
				else              ((MirrorImage) mob).disableDirection();
			}
		}

		final MirrorImage directed = bound;
		GameScene.selectCell(new CellSelector.Listener() {
			@Override
			public void onSelect(Integer cell) {
				if (cell != null && directed.isAlive()) directed.directTocell(cell);
			}

			@Override
			public String prompt() {
				return Messages.get(MirrorLink.class, "prompt");
			}
		});
	}

	private MirrorImage boundMirror() {
		if (boundMirrorID != -1) {
			for (Mob mob : Dungeon.level.mobs) {
				if (mob instanceof MirrorImage && mob.id() == boundMirrorID) {
					if (mob.isAlive()) return (MirrorImage) mob;
					break;
				}
			}
		}
		boundMirrorID = -1;
		return null;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(BOUND_MIRROR, boundMirrorID);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		boundMirrorID = bundle.getInt(BOUND_MIRROR);
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public int value() {
		return 0;
	}
}
