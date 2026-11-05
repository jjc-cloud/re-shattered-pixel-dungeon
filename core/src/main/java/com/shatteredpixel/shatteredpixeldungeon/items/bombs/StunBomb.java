package com.shatteredpixel.shatteredpixeldungeon.items.bombs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;

public class StunBomb extends Bomb {

	private static final float STUN_DURATION = 20f;

	{
		image = ItemSpriteSheet.STUN_BOMB;
	}

	//和绝大多数炼金炸弹一样是范围 2，眩晕范围和爆炸伤害范围保持一致
	@Override
	protected int explosionRange() {
		return 2;
	}

	@Override
	public void explode(int cell) {
		super.explode(cell);
		PathFinder.buildDistanceMap(cell, BArray.not(Dungeon.level.solid, null), explosionRange());
		for (int i = 0; i < PathFinder.distance.length; i++) {
			Char ch = Actor.findChar(i);
			//范围内所有单位都会被眩晕，不分敌我
			if (PathFinder.distance[i] != Integer.MAX_VALUE && ch != null && ch.isAlive()) {
				Buff.prolong(ch, Vertigo.class, STUN_DURATION);
			}
		}
	}
}
