package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.watabou.utils.Bundle;

public class RealityWarpPersistenceRegression {

	public static class PotionA extends Potion {}
	public static class PotionB extends Potion {}
	public static class ScrollA extends Scroll {
		@Override
		public void doRead() {}
	}
	public static class ScrollB extends Scroll {
		@Override
		public void doRead() {}
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}

	public static void main(String[] args) {
		RealityWarp.setPotionSwap(PotionA.class, PotionB.class);
		RealityWarp.setScrollSwap(ScrollA.class, ScrollB.class);

		Bundle warpedSave = new Bundle();
		RealityWarp.storeInBundle(warpedSave);

		RealityWarp.reset();
		check(!RealityWarp.potionSwapped() && !RealityWarp.scrollSwapped(),
				"new run clears previous reality swaps");

		RealityWarp.restoreFromBundle(warpedSave);
		check(RealityWarp.potionPartner(PotionA.class) == PotionB.class
				&& RealityWarp.scrollPartner(ScrollA.class) == ScrollB.class,
				"the owning save restores its own reality swaps");

		RealityWarp.restoreFromBundle(new Bundle());
		check(!RealityWarp.potionSwapped() && !RealityWarp.scrollSwapped(),
				"a save without reality swaps cannot inherit another save's state");

		System.out.println("PASS: reality warp state is isolated per save");
	}
}
