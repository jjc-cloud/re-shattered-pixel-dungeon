package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.AntiMagic;

public abstract class DamageDomain extends Buff {

	public boolean blocksPhysical;
	public boolean blocksMagic;

	{
		type = buffType.NEUTRAL;
		announced = true;
	}

	@Override
	public boolean attachTo(Char target) {
		if (blocksMagic) {
			for (Class effect : AntiMagic.RESISTS) {
				if (Buff.class.isAssignableFrom(effect)) immunities.add(effect);
			}
		}
		if (!super.attachTo(target)) return false;

		//按 Char.isImmune 的方向清除免疫类及其子类，不关闭法杖、戒指和神器。
		if (blocksMagic) {
			for (Buff buff : target.buffs()) {
				for (Class immunity : immunities) {
					if (immunity.isAssignableFrom(buff.getClass())) {
						buff.detach();
						break;
					}
				}
			}
		}
		return true;
	}
}
