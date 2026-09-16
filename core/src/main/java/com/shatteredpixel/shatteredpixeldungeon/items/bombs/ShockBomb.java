/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.shatteredpixel.shatteredpixeldungeon.items.bombs;

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BlastParticle;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ShadowCaster;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Point;

public class ShockBomb extends Bomb {

	private static final int RANGE = 8;

	{
		image = ItemSpriteSheet.SHOCK_BOMB;
	}

	@Override
	public boolean explodesDestructively() {
		return false;
	}

	@Override
	public void explode(int cell) {
		super.explode(cell);
		boolean[] visible = new boolean[Dungeon.level.length()];
		Point origin = Dungeon.level.cellToPoint(cell);
		ShadowCaster.castShadow(origin.x, origin.y, Dungeon.level.width(), visible,
				Dungeon.level.losBlocking, RANGE);
		if (Dungeon.level.heroFOV[cell]) {
			CellEmitter.center(cell).burst(BlastParticle.FACTORY, 60);
			GameScene.flash(0x80FFFFFF);
		}
		for (Char ch : Actor.chars().toArray(new Char[0])) {
			//只摧毁能看见炸弹的单位。视线被遮挡、或视野本身就够不到炸弹时，
			//引擎维护的视野数组里都不含炸弹所在格。
			boolean seesBomb = ch.fieldOfView != null
					&& ch.fieldOfView.length == Dungeon.level.length()
					&& ch.fieldOfView[cell];
			if (ch.isAlive() && visible[ch.pos] && seesBomb) {
				ch.damage(Math.round(ch.HT / 2f + ch.HP / 2f), this);
				if (ch == Dungeon.hero && !ch.isAlive()) {
					Badges.validateDeathFromFriendlyMagic();
					GLog.n(Messages.get(this, "ondeath"));
					Dungeon.fail(this);
				}
			}
		}
	}
}
