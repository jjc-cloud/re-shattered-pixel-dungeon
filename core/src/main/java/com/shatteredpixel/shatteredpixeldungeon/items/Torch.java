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

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Light;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Arrays;

public class Torch extends Item {

	public static final String AC_LIGHT	= "LIGHT";
	public static final String AC_EXTINGUISH = "EXTINGUISH";
	
	public static final float TIME_TO_LIGHT = 1;
	private static final int FULL_FUEL = (int) Light.DURATION;
	private static final int RELIGHT_COST = 5;
	private static final String USED_FUEL = "used_fuel";
	private static final String LIT = "lit";

	// Used torches go first, with the burning torch at index 0. The rest have full fuel.
	private volatile int[] usedFuel = new int[0];
	private volatile boolean lit;
	
	{
		image = ItemSpriteSheet.TORCH;
		
		stackable = true;
	}
	
	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add(defaultAction());
		return actions;
	}
	
	@Override
	public String defaultAction() {
		return lit ? AC_EXTINGUISH : AC_LIGHT;
	}

	public int remainingTurns() {
		int[] fuel = usedFuel;
		return fuel.length == 0 ? FULL_FUEL : fuel[0];
	}

	public static Torch forShortcut(Hero hero) {
		Torch selected = null;
		for (Torch torch : hero.belongings.getAllItems(Torch.class)) {
			if (torch.lit) return torch;
			if (selected == null || (selected.usedFuel.length == 0 && torch.usedFuel.length > 0)) {
				selected = torch;
			}
		}
		return selected;
	}

	@Override
	public String info() {
		return super.info() + "\n\n" + Messages.get(this, "remaining", remainingTurns());
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute(hero, action);
		if (!hero.belongings.contains(this) || !action.equals(defaultAction())) return;

		if (lit) {
			Buff.detach(hero, BurningTorch.class);
			return;
		}

		boolean firstLight = usedFuel.length == 0;
		if (!firstLight && remainingTurns() <= RELIGHT_COST) {
			burn(hero, RELIGHT_COST);
			hero.spendAndNext(TIME_TO_LIGHT);
			return;
		}

		Buff.detach(hero, BurningTorch.class);
		if (firstLight) {
			usedFuel = new int[]{FULL_FUEL};
			Catalog.countUse(getClass());
		} else {
			burn(hero, RELIGHT_COST);
		}
		hero.spend(TIME_TO_LIGHT);
		lit = true;
		// A fresh torch starts consuming fuel after the lighting action.
		Buff.affect(hero, BurningTorch.class, firstLight ? hero.cooldown() : 0).torch = this;

		hero.busy();
		hero.sprite.operate(hero.pos);
		Sample.INSTANCE.play(Assets.Sounds.BURNING);
		Emitter emitter = hero.sprite.centerEmitter();
		emitter.start(FlameParticle.FACTORY, 0.2f, 3);
	}

	private void burn(Hero hero, int turns) {
		int remaining = remainingTurns() - turns;
		if (remaining > 0) {
			usedFuel[0] = remaining;
		} else {
			if (lit) Buff.detach(hero, BurningTorch.class);
			usedFuel = Arrays.copyOfRange(usedFuel, 1, usedFuel.length);
			if (quantity() == 1) {
				detach(hero.belongings.backpack);
			} else {
				quantity--;
				updateQuickslot();
			}
		}
	}

	@Override
	public Item merge(Item other) {
		if (isSimilar(other)) {
			Torch torch = (Torch) other;
			int[] first = torch.lit ? torch.usedFuel : usedFuel;
			int[] second = torch.lit ? usedFuel : torch.usedFuel;
			int[] combined = Arrays.copyOf(first, first.length + second.length);
			System.arraycopy(second, 0, combined, first.length, second.length);
			usedFuel = combined;
			lit |= torch.lit;
			torch.lit = false;
		}
		return super.merge(other);
	}

	@Override
	public Item split(int amount) {
		Torch split = (Torch) super.split(amount);
		if (split != null) {
			// Split fresh torches first and leave any lit torch in the original stack.
			int kept = Math.min(quantity(), usedFuel.length);
			split.usedFuel = Arrays.copyOfRange(usedFuel, kept, usedFuel.length);
			split.lit = false;
			usedFuel = Arrays.copyOf(usedFuel, kept);
		}
		return split;
	}

	@Override
	protected void onDetach() {
		if (lit) {
			lit = false;
			Buff.detach(Dungeon.hero, BurningTorch.class);
		}
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		if (usedFuel.length > 0) bundle.put(USED_FUEL, usedFuel);
		if (lit) bundle.put(LIT, true);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		if (bundle.contains(USED_FUEL)) usedFuel = bundle.getIntArray(USED_FUEL);
		lit = bundle.getBoolean(LIT);
	}

	public static class BurningTorch extends Light {

		private volatile Torch torch;

		@Override
		public boolean act() {
			Hero hero = (Hero) target;
			Torch selected = forShortcut(hero);
			if (selected != null && selected.lit) {
				torch = selected;
				selected.burn(hero, 1);
				if (selected.lit) spend(TICK);
			} else {
				detach();
			}
			return true;
		}

		@Override
		public void weaken(int amount) {
			Torch torch = forShortcut((Hero) target);
			if (torch != null && torch.lit) torch.burn((Hero) target, amount);
		}

		@Override
		public void detach() {
			Torch selected = forShortcut((Hero) target);
			if (selected != null && selected.lit) selected.lit = false;
			if (torch != null) torch.lit = false;
			super.detach();
		}

		@Override
		public float visualcooldown() {
			Torch selected = torch;
			Hero hero = (Hero) target;
			if ((selected == null || !selected.lit) && (hero.ready || !Actor.processing())) {
				selected = forShortcut(hero);
			}
			return selected != null && selected.lit ? selected.remainingTurns() : 0;
		}

		@Override
		public String name() {
			return Messages.get(Light.class, "name");
		}

		@Override
		public String desc() {
			return Messages.get(Light.class, "desc", dispTurns());
		}
	}
	
	@Override
	public boolean isUpgradable() {
		return false;
	}
	
	@Override
	public boolean isIdentified() {
		return true;
	}
	
	@Override
	public int value() {
		return 8 * quantity;
	}

}
