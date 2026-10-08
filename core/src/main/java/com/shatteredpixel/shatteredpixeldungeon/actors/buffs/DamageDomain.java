package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

public abstract class DamageDomain extends Buff {

	//仅用于持有者的出伤判断，不提供受伤或状态免疫。
	public boolean blocksPhysical;
	public boolean blocksMagic;

	{
		type = buffType.NEUTRAL;
		announced = true;
	}
}
