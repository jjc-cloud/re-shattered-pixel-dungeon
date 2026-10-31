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

package com.shatteredpixel.shatteredpixeldungeon.mechanics;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.levels.CellContent;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.Door;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.IronDoor;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.List;

public class Ballistica {

	//note that the path is the FULL path of the projectile, including tiles after collision.
	//make sure to generate a subPath for the common case of going source to collision.
	public ArrayList<Integer> path = new ArrayList<>();
	public Integer sourcePos = null;
	public Integer collisionPos = null;
	public Integer collisionProperties = null;
	public Integer dist = 0;
	public final Projectile projectile;
	private static final ThreadLocal<Projectile> castingProjectile = new ThreadLocal<>();

	//parameters to specify the colliding cell
	public static final int STOP_TARGET = 1;    //ballistica will stop at the target cell
	public static final int STOP_CHARS = 2;     //ballistica will stop on first char hit
	public static final int STOP_SOLID = 4;     //ballistica will stop on solid terrain
	public static final int IGNORE_SOFT_SOLID = 8; //ballistica will ignore soft solid terrain, such as doors and webs
	public static final int PASS_DISINTEGRABLE = 16; //ballistica will pass through solid terrain that a disintegration ray would destroy
	public static final int STOP_PROJECTILE_COVER = 32; //实体投射物受架空设施遮挡，范围传播与角色位移不使用此标记
	public static final int IGNORE_PROJECTILE_BARRIERS = 64; //非弹道攻击可穿过仅拦截投射物的屏障
	public static final int IRON_DOOR_CHANCE = 128; //攻击弹道预览允许穿铁门，实际发射复用本发的 roll 结果

	public static final int PROJECTILE =  	STOP_TARGET	| STOP_CHARS	| STOP_SOLID;

	public static final int MAGIC_BOLT =    STOP_CHARS  | STOP_SOLID;

	public static final int WONT_STOP =     0;


	public Ballistica( int from, int to, int params ){
		this(from, to, params, castingProjectile.get());
	}

	public Ballistica(int from, int to, int params, Projectile projectile) {
		this.projectile = projectile;
		sourcePos = from;
		collisionProperties = params;
		build(from, to,
				(params & STOP_TARGET) > 0,
				(params & STOP_CHARS) > 0,
				(params & STOP_SOLID) > 0,
				(params & IGNORE_SOFT_SOLID) > 0);

		if (collisionPos != null) {
			dist = path.indexOf(collisionPos);
		} else if (!path.isEmpty()) {
			collisionPos = path.get(dist = path.size() - 1);
		} else {
			path.add(from);
			collisionPos = from;
			dist = 0;
		}
	}

	public static Projectile currentProjectile() {
		return castingProjectile.get();
	}

	public static Projectile setProjectile(Projectile projectile) {
		Projectile previous = castingProjectile.get();
		if (projectile == null) castingProjectile.remove();
		else castingProjectile.set(projectile);
		return previous;
	}

	public static Ballistica projectile(Hero hero, int from, int to, int params) {
		Projectile projectile = new Projectile(hero);
		Ballistica shot = new Ballistica(from, to, params | IRON_DOOR_CHANCE, projectile);
		projectile.finish();
		return shot;
	}

	public static class Projectile implements Bundlable {
		private int accuracy, evasion;
		private boolean rolled, passed, finished;

		public Projectile() { }

		public Projectile(Hero hero) {
			accuracy = hero.baseAttackSkill();
			evasion = 5 + Dungeon.depth;
		}

		private synchronized boolean passesIronDoor() {
			if (!rolled && !finished) {
				passed = Random.Float(accuracy) >= Random.Float(evasion);
				rolled = true;
			}
			return passed;
		}

		public synchronized void finish() {
			finished = true;
		}

		@Override
		public synchronized void storeInBundle(Bundle bundle) {
			bundle.put("accuracy", accuracy);
			bundle.put("evasion", evasion);
			bundle.put("rolled", rolled);
			bundle.put("passed", passed);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			accuracy = bundle.getInt("accuracy");
			evasion = bundle.getInt("evasion");
			rolled = bundle.getBoolean("rolled");
			passed = bundle.getBoolean("passed");
			finished = true;
		}
	}

	private void build( int from, int to, boolean stopTarget, boolean stopChars, boolean stopTerrain, boolean ignoreSoftSolid ) {
		int w = Dungeon.level.width();
		boolean passDisintegrable = (collisionProperties & PASS_DISINTEGRABLE) > 0;
		boolean passProjectileBarriers = (collisionProperties & IGNORE_PROJECTILE_BARRIERS) != 0;
		boolean stopProjectileCover = (collisionProperties & STOP_PROJECTILE_COVER) != 0
				&& !Dungeon.level.cellAt(from).has(CellContent.BLOCKS_PROJECTILES);

		int x0 = from % w;
		int x1 = to % w;
		int y0 = from / w;
		int y1 = to / w;

		int dx = x1 - x0;
		int dy = y1 - y0;

		int stepX = dx > 0 ? +1 : -1;
		int stepY = dy > 0 ? +1 : -1;

		dx = Math.abs( dx );
		dy = Math.abs( dy );

		int stepA;
		int stepB;
		int dA;
		int dB;

		if (dx > dy) {

			stepA = stepX;
			stepB = stepY * w;
			dA = dx;
			dB = dy;

		} else {

			stepA = stepY * w;
			stepB = stepX;
			dA = dy;
			dB = dx;

		}

		int cell = from;

		int err = dA / 2;
		while (Dungeon.level.insideMap(cell)) {
			boolean passBarrier = passProjectileBarriers && Dungeon.level.cellAt(cell).has(CellContent.PASSES_NON_PROJECTILES);
			if (collisionPos == null && cell != sourcePos && !passBarrier
					&& (stopTerrain || stopProjectileCover)
					&& (collisionProperties & IRON_DOOR_CHANCE) != 0
					&& Dungeon.level.cellAt(cell).has(CellContent.BLOCKS_PROJECTILES)
					&& Door.at(Dungeon.level, cell) instanceof IronDoor
					&& !Door.is(Dungeon.level, cell, Door.State.SECRET)) {
				passBarrier = projectile == null || projectile.passesIronDoor();
			}
			// 从外侧射来的投射物停在架构前，不能命中同格内的角色；内部发射不受架构限制。
			if (collisionPos == null && cell != sourcePos && stopProjectileCover && !passBarrier
					&& Dungeon.level.cellAt(cell).has(CellContent.BLOCKS_PROJECTILES)) {
				collide(path.get(path.size() - 1));
			}

			//if we're in solid terrain, and there's no char there, collide with the previous cell.
			// we don't use solid here because we don't want to stop short of closed doors.
			if (collisionPos == null
					&& stopTerrain
					&& cell != sourcePos
					&& !Dungeon.level.passable[cell]
					&& !Dungeon.level.avoid[cell]
					&& !passBarrier
					&& !(passDisintegrable && Dungeon.level.passthroughDisintegrationRay(cell))
					&& Actor.findChar(cell) == null) {
				collide(path.get(path.size() - 1));
			}

			path.add(cell);

			if (collisionPos == null && stopTerrain && cell != sourcePos && Dungeon.level.solid[cell]) {
				if (passBarrier || (ignoreSoftSolid && (Dungeon.level.passable[cell] || Dungeon.level.avoid[cell]))) {
					//do nothing
				} else if (passDisintegrable && Dungeon.level.passthroughDisintegrationRay(cell)) {
					//do nothing
				} else {
					collide(cell);
				}
			}
			if (collisionPos == null && cell != sourcePos && stopChars && Actor.findChar( cell ) != null) {
				collide(cell);
			}
			if (collisionPos == null && cell == to && stopTarget){
				collide(cell);
			}

			cell += stepA;

			err += dB;
			if (err >= dA) {
				err = err - dA;
				cell = cell + stepB;
			}
		}
	}

	//we only want to record the first position collision occurs at.
	private void collide(int cell){
		if (collisionPos == null) {
			collisionPos = cell;
		}
	}

	//returns a segment of the path from start to end, inclusive.
	//if there is an error, returns an empty arraylist instead.
	public List<Integer> subPath(int start, int end){
		try {
			end = Math.min( end, path.size()-1);
			return path.subList(start, end+1);
		} catch (Exception e){
			ShatteredPixelDungeon.reportException(e);
			return new ArrayList<>();
		}
	}
}
