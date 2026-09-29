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

package com.shatteredpixel.shatteredpixeldungeon.items.wands;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.levels.TerrainPropagation;
import com.shatteredpixel.shatteredpixeldungeon.levels.TerrainInteractions;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;

import java.util.ArrayList;

public class WandOfLightning extends DamageWand {

	{
		image = ItemSpriteSheet.WAND_LIGHTNING;
	}
	
	private ArrayList<Char> affected = new ArrayList<>();

	private ArrayList<Lightning.Arc> arcs = new ArrayList<>();
	private TerrainPropagation conduction;
	private boolean conducting;
	private SparseArray<Char> conductorChars;
	private int impactCell;
	private boolean impactInWater;

	public int min(int lvl){
		return 5+lvl;
	}

	public int max(int lvl){
		return 10+5*lvl;
	}
	
	@Override
	public void onZap(Ballistica bolt) {
		int impact = impactCell;

		for (Char ch : affected.toArray(new Char[0])){
			if (ch != curUser && ch.alignment == curUser.alignment && ch.pos != impact){
				affected.remove(ch);
			} else if (ch.buff(LightningCharge.class) != null){
				affected.remove(ch);
			}
		}

		//lightning deals less damage per-target, the more targets that are hit.
		if (affected.isEmpty()) return;
		float multiplier = 0.4f + (0.6f/affected.size());
		//if the main target is in water, all affected take full damage
		if (impactInWater) multiplier = 1f;

		for (Char ch : affected){
			if (ch == Dungeon.hero) PixelScene.shake( 2, 0.3f );
			ch.sprite.centerEmitter().burst( SparkParticle.FACTORY, 3 );
			ch.sprite.flash();

			wandProc(ch, chargesPerCast());
			if (ch == curUser && ch.isAlive()) {
				ch.damage(Math.round(damageRoll() * multiplier * 0.5f), this);
				if (!curUser.isAlive()) {
					Badges.validateDeathFromFriendlyMagic();
					Dungeon.fail( this );
					GLog.n(Messages.get(this, "ondeath"));
				}
			} else {
				ch.damage(Math.round(damageRoll() * multiplier), this);
			}
		}
	}

	@Override
	public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {

		// lvl 0 - 25%
		// lvl 1 - 40%
		// lvl 2 - 50%
		float procChance = (buffedLvl()+1f)/(buffedLvl()+4f) * procChanceMultiplier(attacker);
		if (Random.Float() < procChance) {

			float powerMulti = Math.max(1f, procChance);

			FlavourBuff.prolong(attacker, LightningCharge.class, powerMulti*LightningCharge.DURATION);
			attacker.sprite.centerEmitter().burst( SparkParticle.FACTORY, 10 );
			attacker.sprite.flash();
			Sample.INSTANCE.play( Assets.Sounds.LIGHTNING );

		}
	}

	public static class LightningCharge extends FlavourBuff {

		{
			type = buffType.POSITIVE;
		}

		public static float DURATION = 10f;

		@Override
		public int icon() {
			return BuffIndicator.IMBUE;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(1, 1, 0);
		}
	}

	private void arc( Char ch ) {

		int dist = Dungeon.level.water[ch.pos] ? 2 : 1;

		if (curUser.buff(LightningCharge.class) != null){
			dist++;
		}

		ArrayList<Char> hitThisArc = new ArrayList<>();
		PathFinder.buildDistanceMap( ch.pos, BArray.not( Dungeon.level.solid, null ), dist );
		for (int i = 0; i < PathFinder.distance.length; i++) {
			if (PathFinder.distance[i] < Integer.MAX_VALUE){
				Char n = Actor.findChar( i );
				if (n == Dungeon.hero && PathFinder.distance[i] > 1)
					//the hero is only zapped if they are adjacent
					continue;
				else if (n != null && !affected.contains( n )) {
					hitThisArc.add(n);
				}
			}
		}
		
		affected.addAll(hitThisArc);
		for (Char hit : hitThisArc){
			arcs.add(new Lightning.Arc(ch.sprite.center(), hit.sprite.center()));
			arc(hit);
		}
		if (conducting) conduction.touch(ch.pos);
	}

	private void collectConduction() {
		if (!conduction.hasNext()) return;
		// 一次施法建立一次位置索引，避免每个导体都扫描 Actor 集合。
		if (conductorChars == null) conductorChars = new SparseArray<>();
		conductorChars.clear();
		for (Char ch : Actor.chars()) conductorChars.put(ch.pos, ch);
		while (conduction.hasNext()) {
			int cell = conduction.next();
			int from = conduction.parent();
			if (from != cell && (Dungeon.level.heroFOV[from] || Dungeon.level.heroFOV[cell])) {
				arcs.add(new Lightning.Arc(DungeonTilemap.raisedTileCenterToWorld(from),
						DungeonTilemap.raisedTileCenterToWorld(cell)));
			}
			for (int offset : PathFinder.NEIGHBOURS9) {
				int next = cell + offset;
				if (next != cell && !Dungeon.level.adjacent(cell, next)) continue;
				Char ch = conductorChars.get(next);
				if (ch != null && !affected.contains(ch)) {
					affected.add(ch);
					if (Dungeon.level.heroFOV[cell] || Dungeon.level.heroFOV[next]) {
						arcs.add(new Lightning.Arc(DungeonTilemap.raisedTileCenterToWorld(cell), ch.sprite.center()));
					}
					arc(ch);
				}
			}
		}
		conductorChars.clear();
	}
	
	@Override
	public void fx(Ballistica bolt, Callback callback) {

		affected.clear();
		arcs.clear();
		conducting = Dungeon.level.terrainInteractions != null && Dungeon.level.terrainInteractions.hasEffects(TerrainInteractions.Source.ELECTRIC);
		if (conducting) {
			if (conduction == null) conduction = new TerrainPropagation();
			conduction.begin(Dungeon.level, TerrainInteractions.Source.ELECTRIC);
		}

		int cell = TerrainPropagation.impactCell(Dungeon.level, bolt, TerrainInteractions.Source.ELECTRIC);
		impactCell = cell;
		impactInWater = Dungeon.level.water[cell];

		Char ch = Actor.findChar( cell );
		if (ch != null) {
			if (ch instanceof DwarfKing){
				Statistics.qualifiedForBossChallengeBadge = false;
			}

			affected.add( ch );
			arcs.add( new Lightning.Arc(curUser.sprite.center(), ch.sprite.center()));
			arc(ch);
		} else {
			arcs.add( new Lightning.Arc(curUser.sprite.center(), DungeonTilemap.raisedTileCenterToWorld(cell)));
			CellEmitter.center( cell ).burst( SparkParticle.FACTORY, 3 );
			if (conducting) conduction.touch(cell);
		}
		if (conducting) {
			collectConduction();
			conduction.end();
			conducting = false;
		}

		//don't want to wait for the effect before processing damage.
		curUser.sprite.parent.addToFront( new Lightning( arcs, null ) );
		Sample.INSTANCE.play( Assets.Sounds.LIGHTNING );
		callback.call();
	}

	@Override
	public void staffFx(MagesStaff.StaffParticle particle) {
		particle.color(0xFFFFFF);
		particle.am = 0.6f;
		particle.setLifespan(0.6f);
		particle.acc.set(0, +10);
		particle.speed.polar(-Random.Float(3.1415926f), 6f);
		particle.setSize(0f, 1.5f);
		particle.sizeJitter = 1f;
		particle.shuffleXY(1f);
		float dst = Random.Float(1f);
		particle.x -= dst;
		particle.y += dst;
	}
	
}
