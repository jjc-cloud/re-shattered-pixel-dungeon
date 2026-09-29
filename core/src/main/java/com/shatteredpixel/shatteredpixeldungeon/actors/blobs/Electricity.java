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

package com.shatteredpixel.shatteredpixeldungeon.actors.blobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.levels.TerrainPropagation;
import com.shatteredpixel.shatteredpixeldungeon.levels.TerrainInteractions;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class Electricity extends Blob {
	
	{
		//acts after mobs, to give them a chance to resist paralysis
		actPriority = MOB_PRIO - 1;
	}
	
	private boolean[] water;
	private TerrainPropagation conduction;
	
	@Override
	protected void evolve() {
		
		water = Dungeon.level.water;
		int cell;
		boolean terrainAffected = false;
		
		//spread first..
		for (int i = area.left-1; i <= area.right; i++) {
			for (int j = area.top-1; j <= area.bottom; j++) {
				cell = i + j*Dungeon.level.width();
				
				if (cur[cell] > 0) {
					if (Dungeon.level.terrainInteractions != null) {
						terrainAffected |= Dungeon.level.affectTerrain(cell, TerrainInteractions.Source.ELECTRIC);
					}
					spreadFromCell(cell, cur[cell]);
				}
			}
		}
		
		if (Dungeon.level.terrainInteractions != null && Dungeon.level.terrainInteractions.hasConductors(TerrainInteractions.Source.ELECTRIC)) {
			spreadThroughConductors();
		}
		if (terrainAffected) Dungeon.observe();

		//..then decrement/shock
		for (int i = area.left-1; i <= area.right; i++) {
			for (int j = area.top-1; j <= area.bottom; j++) {
				cell = i + j*Dungeon.level.width();
				if (cur[cell] > 0) {
					Char ch = Actor.findChar( cell );
					if (ch != null && !ch.isImmune(this.getClass())) {
						if (ch.buff(Paralysis.class) == null){
							Buff.prolong( ch, Paralysis.class, cur[cell]);
						}
						if (cur[cell] % 2 == 1) {
							ch.damage(Math.round(Random.Float(2 + Dungeon.scalingDepth() / 5f)), this);
							if (!ch.isAlive() && ch == Dungeon.hero){
								Dungeon.fail( this );
								GLog.n( Messages.get(this, "ondeath") );
							}
						}
					}
					
					Heap h = Dungeon.level.heaps.get( cell );
					if (h != null){
						Item toShock = h.peek();
						if (toShock instanceof Wand){
							((Wand) toShock).gainCharge(0.333f);
						} else if (toShock instanceof MagesStaff){
							((MagesStaff) toShock).gainCharge(0.333f);
						}
					}
					
					off[cell] = cur[cell] - 1;
					volume += off[cell];
				} else {
					off[cell] = 0;
				}
			}
		}
		
	}
	
	private void spreadFromCell( int cell, int power ){
		if (cur[cell] == 0) {
			area.union(cell % Dungeon.level.width(), cell / Dungeon.level.width());
		}
		cur[cell] = Math.max(cur[cell], power);
		
		for (int c : PathFinder.NEIGHBOURS4){
			if (water[cell + c] && cur[cell + c] < power){
				spreadFromCell(cell + c, power);
			}
		}
	}

	/** 各连通导体采用接触电场的最大剩余时间，不叠加时间；伤害仍由本 Blob 每格结算一次。 */
	private void spreadThroughConductors() {
		if (conduction == null) conduction = new TerrainPropagation();
		conduction.begin(Dungeon.level, TerrainInteractions.Source.ELECTRIC);
		int width = Dungeon.level.width();
		// 只从本回合已带电的区域发起接触，新增区域由导体遍历处理。
		int left = area.left, right = area.right, top = area.top, bottom = area.bottom;
		for (int y = top; y < bottom; y++) {
			for (int x = left; x < right; x++) {
				int origin = x + y * width;
				if (cur[origin] <= 0) continue;
				for (int offset : PathFinder.NEIGHBOURS9) {
					int seed = origin + offset;
					if (!Dungeon.level.insideMap(seed) || (seed != origin && !Dungeon.level.adjacent(origin, seed))) continue;
					int first = conduction.size();
					conduction.seed(origin, seed);
					if (!conduction.hasNext()) continue;
					int power = cur[origin];
					while (conduction.hasNext()) {
						int cell = conduction.next();
						for (int near : PathFinder.NEIGHBOURS9) power = Math.max(power, cur[cell + near]);
					}
					for (int index = first; index < conduction.size(); index++) {
						int cell = conduction.cell(index);
						spreadFromCell(cell, power);
						for (int near : PathFinder.NEIGHBOURS8) {
							int next = cell + near;
							if (Dungeon.level.insideMap(next) && !Dungeon.level.solid[next]) spreadFromCell(next, power);
						}
					}
				}
			}
		}
		conduction.end();
	}
	
	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
		emitter.start( SparkParticle.FACTORY, 0.05f, 0 );
	}
	
	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
	
}
