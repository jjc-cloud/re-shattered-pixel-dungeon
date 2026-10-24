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

package com.shatteredpixel.shatteredpixeldungeon.levels.features;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.levels.CellContent;
import com.shatteredpixel.shatteredpixeldungeon.levels.CellState;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.noosa.audio.Sample;

/** 门是格子里的持久化内容；材质由子类定义，地形编号只表达共用状态和外观。 */
public class Door extends CellContent {

	public enum State {
		CLOSED(Terrain.DOOR, Terrain.PASSABLE | Terrain.LOS_BLOCKING | Terrain.SOLID | Terrain.FLAMABLE, "closed_door_name", ""),
		OPEN(Terrain.OPEN_DOOR, Terrain.PASSABLE | Terrain.FLAMABLE, "open_door_name", ""),
		LOCKED(Terrain.LOCKED_DOOR, Terrain.LOS_BLOCKING | Terrain.SOLID, "locked_door_name", "locked_door_desc"),
		HERO_LOCKED(Terrain.HERO_LKD_DR, Terrain.LOS_BLOCKING | Terrain.SOLID, "locked_door_name", "locked_door_desc"),
		CRYSTAL(Terrain.CRYSTAL_DOOR, Terrain.SOLID, "crystal_door_name", "crystal_door_desc"),
		SECRET(Terrain.SECRET_DOOR, Terrain.LOS_BLOCKING | Terrain.SOLID | Terrain.SECRET, "wall_name", "");

		public final int terrain;
		public final int flags;
		public final String nameKey;
		public final String descKey;

		State(int terrain, int flags, String nameKey, String descKey) {
			this.terrain = terrain;
			this.flags = flags;
			this.nameKey = nameKey;
			this.descKey = descKey;
		}
	}

	/** 角色通行与物品、弹道的外力接触使用不同的开门资格。 */
	public enum Opening { CHARACTER, IMPACT }

	protected boolean opensOnImpact = true;
	protected int excludedFlags;
	protected int materialProperties = OCCUPIES_SPACE | BURNABLE;
	private volatile int removedProperties;
	protected int closedFlags = State.CLOSED.flags;
	protected int openFlags = State.OPEN.flags;
	private static final int DESTRUCTIVE = BURNABLE | BREAKABLE | CLICK_BREAKABLE
			| EXPLOSION_BREAKABLE | DISINTEGRATION_BREAKABLE | SHOCKWAVE_BREAKABLE;
	protected int unlockedOnlyProperties = DESTRUCTIVE;

	public Door() {
		type = Terrain.DOOR;
	}

	public Door(int terrain) {
		this();
		if (state(terrain) == null) throw new IllegalArgumentException("地形不是门的状态");
		type = terrain;
	}

	public static State state(int terrain) {
		switch (terrain) {
			case Terrain.DOOR: return State.CLOSED;
			case Terrain.OPEN_DOOR: return State.OPEN;
			case Terrain.LOCKED_DOOR: return State.LOCKED;
			case Terrain.HERO_LKD_DR: return State.HERO_LOCKED;
			case Terrain.CRYSTAL_DOOR: return State.CRYSTAL;
			case Terrain.SECRET_DOOR: return State.SECRET;
			default: return null;
		}
	}

	public State state() { return state(type); }
	public boolean unlocked() { return state() == State.CLOSED || state() == State.OPEN; }
	public boolean locked() { return state() == State.LOCKED || state() == State.HERO_LOCKED || state() == State.CRYSTAL; }

	/** 只返回当前可接触的门，埋在覆盖物下面的门不能交互。 */
	public static Door at(Level level, int cell) {
		if (cell < 0 || cell >= level.length()) return null;
		for (CellContent content : level.cellAt(cell).contents()) {
			if (content instanceof Door && level.cellAt(cell).exposed(content)) return (Door) content;
		}
		return null;
	}

	public static boolean is(Level level, int cell, State state) {
		Door door = at(level, cell);
		return door != null && door.state() == state;
	}

	public static boolean unlocked(Level level, int cell) {
		Door door = at(level, cell);
		return door != null && door.unlocked();
	}

	public static boolean locked(Level level, int cell) {
		Door door = at(level, cell);
		return door != null && door.locked();
	}

	@Override public int properties() {
		// 未发现时对外表现为墙，材质和实例属性保留到发现后再生效。
		if (state() == State.SECRET) return OCCUPIES_SPACE | COVERS_CONTENTS;
		int properties = materialProperties;
		if (!unlocked()) properties &= ~unlockedOnlyProperties;
		return (properties | super.properties()) & ~removedProperties;
	}

	/** 实例的增删属性独立于材质默认值，开关、上锁及读档都保留这些覆盖。 */
	@Override public void properties(int add, int remove) {
		super.properties(add, remove);
		removedProperties = (removedProperties & ~add) | remove;
	}

	@Override public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("door_removed_properties", removedProperties);
	}

	@Override public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		removedProperties = bundle.getInt("door_removed_properties");
	}

	@Override public int flags() {
		// 隐藏状态优先于材质，不能被透明材质的 excludedFlags 去掉遮光。
		if (state() == State.SECRET) return State.SECRET.flags;
		int flags = state() == State.CLOSED ? closedFlags : state() == State.OPEN ? openFlags : state().flags;
		flags = has(BURNABLE) ? flags | Terrain.FLAMABLE : flags & ~Terrain.FLAMABLE;
		return flags & ~excludedFlags;
	}

	public String name() { return Messages.get(Level.class, state().nameKey); }
	public String desc() { return state().descKey.isEmpty() ? "" : Messages.get(Level.class, state().descKey); }

	/** 保留对象身份及其临时属性，所有状态转换共用同一个刷新入口。 */
	public boolean changeState(Level level, int cell, State next) {
		CellState contents = level.cellAt(cell);
		synchronized (contents) {
			if (contents.content(type) != this || state() == next) return false;
			type = next.terrain;
			level.cellChanged(cell);
		}
		if (level == Dungeon.level && level.heroFOV != null && level.heroFOV[cell]
				&& Game.scene() instanceof GameScene) Dungeon.observe();
		return true;
	}

	/** 技能推开门时不播放角色开门音效。 */
	public static boolean open(Level level, int cell, Opening cause) {
		Door door = at(level, cell);
		return door != null && door.state() == State.CLOSED
				&& (cause == Opening.CHARACTER || door.opensOnImpact)
				&& door.changeState(level, cell, State.OPEN);
	}

	public static void enter(int pos, Opening cause) {
		if (open(Dungeon.level, pos, cause) && Dungeon.level.heroFOV != null && Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.OPEN);
	}

	public static void leave(int pos, Char leaving) {
		Door door = at(Dungeon.level, pos);
		if (door == null || door.state() != State.OPEN) return;
		// 调用方可以在移动前或移动后通知；只忽略明确离开的角色。
		for (Char ch : Actor.chars()) if (ch.pos == pos && ch != leaving) return;
		if (Dungeon.level.heaps.get(pos) == null) door.changeState(Dungeon.level, pos, State.CLOSED);
	}

	/** 钥匙与神器负责支付代价，门负责解锁后的状态及自身消失效果。 */
	public static boolean unlock(Level level, int cell) {
		Door door = at(level, cell);
		if (door == null || !door.locked()) return false;
		if (door.state() == State.CRYSTAL) {
			level.cellAt(cell).removeContent(door);
			if (level == Dungeon.level && Game.scene() instanceof GameScene) {
				Dungeon.observe();
				Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
				CellEmitter.get(cell).start(Speck.factory(Speck.DISCOVER), 0.025f, 20);
			}
			return true;
		}
		return door.changeState(level, cell, State.CLOSED);
	}

	public static boolean lock(Level level, int cell) {
		Door door = at(level, cell);
		return door != null && door.unlocked() && door.changeState(level, cell, State.HERO_LOCKED);
	}

	public static boolean reveal(Level level, int cell) {
		Door door = at(level, cell);
		return door != null && door.state() == State.SECRET && door.changeState(level, cell, State.CLOSED);
	}
}
