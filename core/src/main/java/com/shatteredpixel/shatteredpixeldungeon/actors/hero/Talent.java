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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtifactRecharge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CounterBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnhancedRings;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LostInventory;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MonkEnergy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PhysicalEmpower;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Recharging;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RevealedArea;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ScrollEmpower;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WandEmpower;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.Ratmogrify;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.DivineSense;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.RecallInscription;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Brute;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.MirrorImage;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.MirrorLink;
import com.shatteredpixel.shatteredpixeldungeon.items.RealityWarp;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfIntuition;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Grim;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.Chasm;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndRealityWarp;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameMath;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

public enum Talent {

	//Warrior T1
	HEARTY_MEAL(0), VETERANS_INTUITION(1), PROVOKED_ANGER(2), IRON_WILL(3),
	//Warrior T2
	// Keep the legacy IDs so saved points stay in their original talent slots.
	// LIQUID_WILLPOWER is now Hold Fast; HOLD_FAST is now Liquid Revival.
	IRON_STOMACH(4), LIQUID_WILLPOWER(5), RUNIC_TRANSFERENCE(6), LETHAL_MOMENTUM(7), IMPROVISED_PROJECTILES(8),
	//Warrior T3
	HOLD_FAST(9, 3), STRONGMAN(10, 3),
	//Berserker T3
	ENDLESS_RAGE(11, 3), DEATHLESS_FURY(12, 3), ENRAGED_CATALYST(13, 3),
	//Gladiator T3
	CLEAVE(14, 3), LETHAL_DEFENSE(15, 3), ENHANCED_COMBO(16, 3),
	//Heroic Leap T4
	BODY_SLAM(17, 4), IMPACT_WAVE(18, 4), DOUBLE_JUMP(19, 4),
	//Shockwave T4
	EXPANDING_WAVE(20, 4), STRIKING_WAVE(21, 4), SHOCK_FORCE(22, 4),
	//Endure T4
	SUSTAINED_RETRIBUTION(23, 4), SHRUG_IT_OFF(24, 4), EVEN_THE_ODDS(25, 4),

	//Mage T1
	EMPOWERING_MEAL(32), SCHOLARS_INTUITION(33), LINGERING_MAGIC(34), BACKUP_BARRIER(35),
	//Mage T2
	ENERGIZING_MEAL(36), INSCRIBED_POWER(37), WAND_PRESERVATION(38), ARCANE_VISION(39), SHIELD_BATTERY(40),
	//Mage T3
	DESPERATE_POWER(41, 3), ALLY_WARP(42, 3),
	//Battlemage T3
	EMPOWERED_STRIKE(43, 3), MYSTICAL_CHARGE(44, 3), EXCESS_CHARGE(45, 3),
	//Warlock T3
	SOUL_EATER(46, 3), SOUL_SIPHON(47, 3), NECROMANCERS_MINIONS(48, 3),
	//Elemental Blast T4
	BLAST_RADIUS(49, 4), ELEMENTAL_POWER(50, 4), REACTIVE_BARRIER(51, 4),
	//Wild Magic T4
	WILD_POWER(52, 4), FIRE_EVERYTHING(53, 4), CONSERVED_MAGIC(54, 4),
	//Warp Beacon T4
	TELEFRAG(55, 4), REMOTE_BEACON(56, 4), LONGRANGE_WARP(57, 4),

	//Rogue T1
	CACHED_RATIONS(64), THIEFS_INTUITION(65), SUCKER_PUNCH(66), PROTECTIVE_SHADOWS(67),
	//Rogue T2
	MYSTICAL_MEAL(68), INSCRIBED_STEALTH(69), WIDE_SEARCH(70), SILENT_STEPS(71), ROGUES_FORESIGHT(72),
	//Rogue T3
	ENHANCED_RINGS(73, 3), LIGHT_CLOAK(74, 3),
	//Assassin T3
	ENHANCED_LETHALITY(75, 3), ASSASSINS_REACH(76, 3), BOUNTY_HUNTER(77, 3),
	//Freerunner T3
	EVASIVE_ARMOR(78, 3), PROJECTILE_MOMENTUM(79, 3), SPEEDY_STEALTH(80, 3),
	//Smoke Bomb T4
	HASTY_RETREAT(81, 4), BODY_REPLACEMENT(82, 4), SHADOW_STEP(83, 4),
	//Death Mark T4
	FEAR_THE_REAPER(84, 4), DEATHLY_DURABILITY(85, 4), DOUBLE_MARK(86, 4),
	//Shadow Clone T4
	SHADOW_BLADE(87, 4), CLONED_ARMOR(88, 4), PERFECT_COPY(89, 4),

	//Huntress T1
	NATURES_BOUNTY(96), SURVIVALISTS_INTUITION(97), FOLLOWUP_STRIKE(98), NATURES_AID(99),
	//Huntress T2
	INVIGORATING_MEAL(100), LIQUID_NATURE(101), REJUVENATING_STEPS(102), HEIGHTENED_SENSES(103), DURABLE_PROJECTILES(104),
	//Huntress T3
	POINT_BLANK(105, 3), SEER_SHOT(106, 3),
	//Sniper T3
	FARSIGHT(107, 3), SHARED_ENCHANTMENT(108, 3), SHARED_UPGRADES(109, 3),
	//Warden T3
	DURABLE_TIPS(110, 3), BARKSKIN(111, 3), SHIELDING_DEW(112, 3),
	//Spectral Blades T4
	FAN_OF_BLADES(113, 4), PROJECTING_BLADES(114, 4), SPIRIT_BLADES(115, 4),
	//Natures Power T4
	GROWING_POWER(116, 4), NATURES_WRATH(117, 4), WILD_MOMENTUM(118, 4),
	//Spirit Hawk T4
	EAGLE_EYE(119, 4), GO_FOR_THE_EYES(120, 4), SWIFT_SPIRIT(121, 4),

	//Duelist T1
	STRENGTHENING_MEAL(128), ADVENTURERS_INTUITION(129), PATIENT_STRIKE(130), AGGRESSIVE_BARRIER(131),
	//Duelist T2
	FOCUSED_MEAL(132), LIQUID_AGILITY(133), WEAPON_RECHARGING(134), LETHAL_HASTE(135), SWIFT_EQUIP(136),
	//Duelist T3
	PRECISE_ASSAULT(137, 3), DEADLY_FOLLOWUP(138, 3),
	//Champion T3
	VARIED_CHARGE(139, 3), TWIN_UPGRADES(140, 3), COMBINED_LETHALITY(141, 3),
	//Monk T3
	UNENCUMBERED_SPIRIT(142, 3), MONASTIC_VIGOR(143, 3), COMBINED_ENERGY(144, 3),
	//Challenge T4
	CLOSE_THE_GAP(145, 4), INVIGORATING_VICTORY(146, 4), ELIMINATION_MATCH(147, 4),
	//Elemental Strike T4
	ELEMENTAL_REACH(148, 4), STRIKING_FORCE(149, 4), DIRECTED_POWER(150, 4),
	//Feint T4
	FEIGNED_RETREAT(151, 4), EXPOSE_WEAKNESS(152, 4), COUNTER_ABILITY(153, 4),

	//Cleric T1
	SATIATED_SPELLS(160), HOLY_INTUITION(161), SEARING_LIGHT(162), SHIELD_OF_LIGHT(163),
	//Cleric T2
	ENLIGHTENING_MEAL(164), RECALL_INSCRIPTION(165), SUNRAY(166), DIVINE_SENSE(167), BLESS(168),
	//Cleric T3
	CLEANSE(169, 3), LIGHT_READING(170, 3),
	//Priest T3
	HOLY_LANCE(171, 3), HALLOWED_GROUND(172, 3), MNEMONIC_PRAYER(173, 3),
	//Paladin T3
	LAY_ON_HANDS(174, 3), AURA_OF_PROTECTION(175, 3), WALL_OF_LIGHT(176, 3),
	//Ascended Form T4
	DIVINE_INTERVENTION(177, 4), JUDGEMENT(178, 4), FLASH(179, 4),
	//Trinity T4
	BODY_FORM(180, 4), MIND_FORM(181, 4), SPIRIT_FORM(182, 4),
	//Power of Many T4
	BEAMING_RAY(183, 4), LIFE_LINK(184, 4), STASIS(185, 4),

	//universal T4
	HEROIC_ENERGY(26, 4), //See icon() and title() for special logic for this one
	//Ratmogrify T4
	RATSISTANCE(215, 4), RATLOMACY(216, 4), RATFORCEMENTS(217, 4),

	//public metamorph talents
	UPLIFTING_MEAL(224), MULTIPLE_EXISTENCE(225), ESCAPE_PLAN(226), VOID_WALKER(227),
	MANA_OVERLOAD(230, 2), SAVAGE_PARASITE(231, 2), CROSSFIRE(233, 2),
	REALITY_WARP(229, 2, true), PERFECT_EXECUTION(232, 2, true),
	FIREPOWER_BARRAGE(228, 2, true);

	private static final Talent[] COMMON_METAMORPH_TALENTS = {
			UPLIFTING_MEAL, MULTIPLE_EXISTENCE, ESCAPE_PLAN, VOID_WALKER,
			MANA_OVERLOAD, SAVAGE_PARASITE, CROSSFIRE
	};
	private static final Talent[] RARE_METAMORPH_TALENTS = {
			FIREPOWER_BARRAGE, REALITY_WARP, PERFECT_EXECUTION
	};

	public static class ImprovisedProjectileCooldown extends FlavourBuff{
		public int icon() { return BuffIndicator.TIME; }
		public void tintIcon(Image icon) { icon.hardlight(0.15f, 0.2f, 0.5f); }
		public float iconFadePercent() { return Math.max(0, visualcooldown() / 50); }
	};
	public static class LethalMomentumTracker extends FlavourBuff{};
	public static class StrikingWaveTracker extends FlavourBuff{};
	public static class WandPreservationCounter extends CounterBuff{{revivePersists = true;}};
	public static class EmpoweredStrikeTracker extends FlavourBuff{
		//blast wave on-hit doesn't resolve instantly, so we delay detaching for it
		public boolean delayedDetach = false;
	};
	public static class ProtectiveShadowsTracker extends Buff {
		float barrierInc = 0.5f;

		@Override
		public boolean act() {
			//barrier every 2/1 turns, to a max of 3/5
			if (((Hero)target).hasTalent(Talent.PROTECTIVE_SHADOWS) && target.invisible > 0){
				Barrier barrier = Buff.affect(target, Barrier.class);
				if (barrier.shielding() < 1 + 2*((Hero)target).pointsInTalent(Talent.PROTECTIVE_SHADOWS)) {
					barrierInc += 0.5f * ((Hero) target).pointsInTalent(Talent.PROTECTIVE_SHADOWS);
				}
				if (barrierInc >= 1){
					barrierInc = 0;
					barrier.incShield(1);
				} else {
					barrier.incShield(0); //resets barrier decay
				}
			} else {
				detach();
			}
			spend( TICK );
			return true;
		}

		private static final String BARRIER_INC = "barrier_inc";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put( BARRIER_INC, barrierInc);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			barrierInc = bundle.getFloat( BARRIER_INC );
		}
	}
	public static class BountyHunterTracker extends FlavourBuff{};
	public static class RejuvenatingStepsCooldown extends FlavourBuff{
		public int icon() { return BuffIndicator.TIME; }
		public void tintIcon(Image icon) { icon.hardlight(0f, 0.35f, 0.15f); }
		public float iconFadePercent() { return GameMath.gate(0, visualcooldown() / (15 - 5*Dungeon.hero.pointsInTalent(REJUVENATING_STEPS)), 1); }
	};
	public static class RejuvenatingStepsFurrow extends CounterBuff{{revivePersists = true;}};
	public static class SeerShotCooldown extends FlavourBuff{
		public int icon() { return target.buff(RevealedArea.class) != null ? BuffIndicator.NONE : BuffIndicator.TIME; }
		public void tintIcon(Image icon) { icon.hardlight(0.7f, 0.4f, 0.7f); }
		public float iconFadePercent() { return Math.max(0, visualcooldown() / 20); }
	};
	public static class SpiritBladesTracker extends FlavourBuff{};
	public static class PatientStrikeTracker extends Buff {
		public int pos;
		{ type = Buff.buffType.POSITIVE; }
		public int icon() { return BuffIndicator.TIME; }
		public void tintIcon(Image icon) { icon.hardlight(0.5f, 0f, 1f); }
		@Override
		public boolean act() {
			if (pos != target.pos) {
				detach();
			} else {
				spend(TICK);
			}
			return true;
		}
		private static final String POS = "pos";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(POS, pos);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			pos = bundle.getInt(POS);
		}
	};
	public static class AggressiveBarrierCooldown extends FlavourBuff{
		public int icon() { return BuffIndicator.TIME; }
		public void tintIcon(Image icon) { icon.hardlight(0.35f, 0f, 0.7f); }
		public float iconFadePercent() { return Math.max(0, visualcooldown() / 50); }
	};
	public static class LiquidAgilEVATracker extends FlavourBuff{
		{
			//detaches after hero acts, not after mobs act
			actPriority = HERO_PRIO+1;
		}
	};
	public static class LiquidAgilACCTracker extends FlavourBuff{
		public int uses;

		{ type = buffType.POSITIVE; }
		public int icon() { return BuffIndicator.INVERT_MARK; }
		public void tintIcon(Image icon) { icon.hardlight(0.5f, 0f, 1f); }
		public float iconFadePercent() { return Math.max(0, 1f - (visualcooldown() / 5)); }

		private static final String USES = "uses";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(USES, uses);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			uses = bundle.getInt(USES);
		}
	};
	public static class LethalHasteCooldown extends FlavourBuff{
		public int icon() { return BuffIndicator.TIME; }
		public void tintIcon(Image icon) { icon.hardlight(0.35f, 0f, 0.7f); }
		public float iconFadePercent() { return Math.max(0, visualcooldown() / 100); }
	};
	public static class SwiftEquipCooldown extends FlavourBuff{
		public boolean secondUse;
		public boolean hasSecondUse(){
			return secondUse;
		}

		public int icon() { return BuffIndicator.TIME; }
		public void tintIcon(Image icon) {
			if (hasSecondUse()) icon.hardlight(0.85f, 0f, 1.0f);
			else                icon.hardlight(0.35f, 0f, 0.7f);
		}
		public float iconFadePercent() { return GameMath.gate(0, visualcooldown() / 20f, 1); }

		private static final String SECOND_USE = "second_use";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(SECOND_USE, secondUse);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			secondUse = bundle.getBoolean(SECOND_USE);
		}
	};
	public static class DeadlyFollowupTracker extends FlavourBuff{
		public int object;
		{ type = Buff.buffType.POSITIVE; }
		public int icon() { return BuffIndicator.INVERT_MARK; }
		public void tintIcon(Image icon) { icon.hardlight(0.5f, 0f, 1f); }
		public float iconFadePercent() { return Math.max(0, 1f - (visualcooldown() / 5)); }
		private static final String OBJECT    = "object";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(OBJECT, object);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			object = bundle.getInt(OBJECT);
		}
	}
	public static class PreciseAssaultTracker extends FlavourBuff{
		{ type = buffType.POSITIVE; }
		public int icon() { return BuffIndicator.INVERT_MARK; }
		public void tintIcon(Image icon) { icon.hardlight(1f, 1f, 0.0f); }
		public float iconFadePercent() { return Math.max(0, 1f - (visualcooldown() / 5)); }
	};
	public static class VariedChargeTracker extends Buff{
		public Class weapon;

		private static final String WEAPON    = "weapon";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(WEAPON, weapon);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			weapon = bundle.getClass(WEAPON);
		}
	}
	public static class CounterAbilityTacker extends FlavourBuff{}
	public static class SatiatedSpellsTracker extends Buff{
		@Override
		public int icon() {
			return BuffIndicator.SPELL_FOOD;
		}
	}
	//used for metamorphed searing light
	public static class SearingLightCooldown extends FlavourBuff{
		@Override
		public int icon() {
			return BuffIndicator.TIME;
		}
		public void tintIcon(Image icon) { icon.hardlight(0f, 0f, 1f); }
		public float iconFadePercent() { return Math.max(0, visualcooldown() / 20); }
	}

	int icon;
	int maxPoints;
	boolean rare;

	// tiers 1/2/3/4 start at levels 2/7/13/21
	public static int[] tierLevelThresholds = new int[]{0, 2, 7, 13, 21, 31};

	Talent( int icon ){
		this(icon, 2);
	}

	Talent( int icon, int maxPoints ){
		this(icon, maxPoints, false);
	}

	Talent( int icon, int maxPoints, boolean rare ){
		this.icon = icon;
		this.maxPoints = maxPoints;
		this.rare = rare;
	}

	public boolean isPublicMetamorphTalent(){
		for (Talent talent : COMMON_METAMORPH_TALENTS) if (this == talent) return true;
		for (Talent talent : RARE_METAMORPH_TALENTS) if (this == talent) return true;
		return false;
	}

	public boolean isRare(){
		return rare;
	}

	public static boolean publicMetamorphTalentsAvailable(int tier){
		return tier == 1 || tier == 2;
	}

	public static Talent randomPublicMetamorphTalent( boolean rare, HashSet<Talent> excluded ){
		Talent[] pool = rare ? RARE_METAMORPH_TALENTS : COMMON_METAMORPH_TALENTS;
		ArrayList<Talent> available = new ArrayList<>();
		for (Talent talent : pool){
			if (!excluded.contains(talent)) available.add(talent);
		}
		if (available.isEmpty() && rare){
			for (Talent talent : COMMON_METAMORPH_TALENTS){
				if (!excluded.contains(talent)) available.add(talent);
			}
		}
		return available.isEmpty() ? null : Random.element(available);
	}

	public int icon(){
		if (this == HEROIC_ENERGY){
			if (Ratmogrify.useRatroicEnergy){
				return 218;
			}
			HeroClass cls = Dungeon.hero != null ? Dungeon.hero.heroClass : GamesInProgress.selectedClass;
			switch (cls){
				case WARRIOR: default:
					return 26;
				case MAGE:
					return 58;
				case ROGUE:
					return 90;
				case HUNTRESS:
					return 122;
				case DUELIST:
					return 154;
				case CLERIC:
					return 186;
			}
		} else {
			return icon;
		}
	}

	public int maxPoints(){
		return maxPoints;
	}

	public String title(){
		if (this == HEROIC_ENERGY && Ratmogrify.useRatroicEnergy){
			return Messages.get(this, name() + ".rat_title");
		}
		return Messages.get(this, name() + ".title");
	}

	public final String desc(){
		return desc(false);
	}

	public String desc(boolean metamorphed){
		String desc;
		if (metamorphed){
			String metaDesc = Messages.get(this, name() + ".meta_desc");
			if (!metaDesc.equals(Messages.NO_TEXT_FOUND)){
				desc = Messages.get(this, name() + ".desc") + "\n\n" + metaDesc;
			} else {
				desc = Messages.get(this, name() + ".desc");
			}
		} else {
			desc = Messages.get(this, name() + ".desc");
		}
		if (rare){
			String rareDesc = Messages.get(Talent.class, "rare_desc");
			if (!rareDesc.equals(Messages.NO_TEXT_FOUND)) desc = rareDesc + "\n\n" + desc;
		}
		return desc;
	}

	public static void onTalentUpgraded( Hero hero, Talent talent ){
		if (talent == MULTIPLE_EXISTENCE){
			ensureSpecialItems(hero);
			Dungeon.observe();
		}
		//扭曲现实：升级时弹出选品窗口，+1选药水，+2选卷轴
		if (talent == REALITY_WARP){
			if (hero.pointsInTalent(REALITY_WARP) == 1){
				GameScene.show(new WndRealityWarp(WndRealityWarp.Mode.POTION));
			} else if (hero.pointsInTalent(REALITY_WARP) == 2){
				GameScene.show(new WndRealityWarp(WndRealityWarp.Mode.SCROLL));
			}
		}
		//交叉火力：获得后挂上连击计数buff
		if (talent == CROSSFIRE){
			Buff.affect(hero, CrossfireTracker.class);
		}
		//for metamorphosis
		if (talent == VETERANS_INTUITION && hero.pointsInTalent(VETERANS_INTUITION) == 2){
			if (hero.belongings.armor() != null && !ShardOfOblivion.passiveIDDisabled())  {
				hero.belongings.armor.identify();
			}
		}
		if (talent == THIEFS_INTUITION && hero.pointsInTalent(THIEFS_INTUITION) == 2){
			if (hero.belongings.ring instanceof Ring && !ShardOfOblivion.passiveIDDisabled()) {
				hero.belongings.ring.identify();
			}
			if (hero.belongings.misc instanceof Ring && !ShardOfOblivion.passiveIDDisabled()) {
				hero.belongings.misc.identify();
			}
			if (hero.belongings.extraMisc instanceof Ring && !ShardOfOblivion.passiveIDDisabled()) {
				hero.belongings.extraMisc.identify();
			}
			for (Item item : Dungeon.hero.belongings){
				if (item instanceof Ring){
					((Ring) item).setKnown();
				}
			}
		}
		if (talent == THIEFS_INTUITION && hero.pointsInTalent(THIEFS_INTUITION) == 1){
			if (hero.belongings.ring instanceof Ring) hero.belongings.ring.setKnown();
			if (hero.belongings.misc instanceof Ring) ((Ring) hero.belongings.misc).setKnown();
			if (hero.belongings.extraMisc instanceof Ring) ((Ring) hero.belongings.extraMisc).setKnown();
		}
		if (talent == ADVENTURERS_INTUITION && hero.pointsInTalent(ADVENTURERS_INTUITION) == 2){
			if (hero.belongings.weapon() != null && !ShardOfOblivion.passiveIDDisabled()){
				hero.belongings.weapon().identify();
			}
			if (hero.belongings.secondWep() != null && !ShardOfOblivion.passiveIDDisabled()){
				hero.belongings.secondWep().identify();
			}
		}

		if (talent == PROTECTIVE_SHADOWS && hero.invisible > 0){
			Buff.affect(hero, Talent.ProtectiveShadowsTracker.class);
		}

		if (talent == LIGHT_CLOAK && hero.heroClass == HeroClass.ROGUE){
			for (Item item : Dungeon.hero.belongings.backpack){
				if (item instanceof CloakOfShadows){
					if (!hero.belongings.lostInventory() || item.keptThroughLostInventory()) {
						((CloakOfShadows) item).activate(Dungeon.hero);
					}
				}
			}
		}

		if (talent == HEIGHTENED_SENSES || talent == FARSIGHT || talent == DIVINE_SENSE){
			Dungeon.observe();
		}

		if (talent == TWIN_UPGRADES || talent == DESPERATE_POWER
				|| talent == STRONGMAN || talent == DURABLE_PROJECTILES){
			Item.updateQuickslot();
		}

		if (talent == UNENCUMBERED_SPIRIT && hero.pointsInTalent(talent) == 3){
			Item toGive = new ClothArmor().identify();
			if (!toGive.collect()){
				Dungeon.level.drop(toGive, hero.pos).sprite.drop();
			}
		}

		if (talent == LIGHT_READING && hero.heroClass == HeroClass.CLERIC){
			for (Item item : Dungeon.hero.belongings.backpack){
				if (item instanceof HolyTome){
					if (!hero.belongings.lostInventory() || item.keptThroughLostInventory()) {
						((HolyTome) item).activate(Dungeon.hero);
					}
				}
			}
		}

		//if we happen to have spirit form applied with a ring of might
		if (talent == SPIRIT_FORM){
			Dungeon.hero.updateHT(false);
		}
	}

	public static class CachedRationsDropped extends CounterBuff{{revivePersists = true;}};
	public static class NatureBerriesDropped extends CounterBuff{{revivePersists = true;}};

	public static void onFoodEaten( Hero hero, float foodVal, Item foodSource ){
		if (hero.hasTalent(UPLIFTING_MEAL)){
			Buff.affect(hero, Swiftthistle.TimeBubble.class).reset(
					hero.pointsInTalent(UPLIFTING_MEAL) == 1 ? 2 : 4);
		}
		if (hero.hasTalent(HEARTY_MEAL)){
			//4/6 HP healed, when hero is below 33% health (with a little rounding up)
			if (hero.HP/(float)hero.HT < 0.334f) {
				int healing = 2 + 2 * hero.pointsInTalent(HEARTY_MEAL);
				hero.HP = Math.min(hero.HP + healing, hero.HT);
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healing), FloatingText.HEALING);

			}
		}
		if (hero.hasTalent(IRON_STOMACH)){
			if (hero.cooldown() > 0) {
				Buff.affect(hero, WarriorFoodImmunity.class, hero.cooldown());
			}
		}
		if (hero.hasTalent(EMPOWERING_MEAL)){
			//2/3 bonus wand damage for next 3 zaps
			Buff.affect( hero, WandEmpower.class).set(1 + hero.pointsInTalent(EMPOWERING_MEAL), 3);
			ScrollOfRecharging.charge( hero );
		}
		int wandChargeTurns = 0;
		if (hero.hasTalent(ENERGIZING_MEAL)){
			//5/8 turns of recharging
			wandChargeTurns += 2 + 3*hero.pointsInTalent(ENERGIZING_MEAL);
		}
		int artifactChargeTurns = 0;
		if (hero.hasTalent(MYSTICAL_MEAL)){
			//3/5 turns of recharging
			artifactChargeTurns += 1 + 2*hero.pointsInTalent(MYSTICAL_MEAL);
		}
		if (hero.hasTalent(INVIGORATING_MEAL)){
			//effectively 1/2 turns of haste
			Buff.prolong( hero, Haste.class, 0.67f+hero.pointsInTalent(INVIGORATING_MEAL));
		}
		if (hero.hasTalent(STRENGTHENING_MEAL)){
			if (hero.heroClass == HeroClass.DUELIST){
				//0.67 weapon charge at +1, 1 at +2
				Buff.affect( hero, MeleeWeapon.Charger.class).gainCharge(
						hero.pointsInTalent(STRENGTHENING_MEAL) == 1 ? 0.67f : 1f);
			} else {
				//for other classes this keeps the lvl/3 / lvl/2 bonus dmg that focused meal used to give them
				Buff.affect( hero, PhysicalEmpower.class).set(Math.round(hero.lvl / (4f - hero.pointsInTalent(STRENGTHENING_MEAL))), 1);
			}
		}
		if (hero.hasTalent(FOCUSED_MEAL)){
			//75%/100% chance for a moment of focus, for any class
			if (Random.Float() < 0.5f + 0.25f*hero.pointsInTalent(FOCUSED_MEAL)){
				//the same buff the monk's ability gives, so the two sources can never be stacked.
				//The flag only swaps in a description that names this talent as the source
				Buff.affect( hero, MonkEnergy.MonkAbility.Focus.FocusBuff.class ).fromMeal = true;
			}
		}
		if (hero.hasTalent(SATIATED_SPELLS)){
			if (hero.heroClass == HeroClass.CLERIC) {
				Buff.affect(hero, SatiatedSpellsTracker.class);
			} else {
				//3/5 shielding, delayed up to 10 turns
				int amount = 1 + 2*hero.pointsInTalent(SATIATED_SPELLS);
				Barrier b = Buff.affect(hero, Barrier.class);
				if (b.shielding() <= amount){
					b.setShield(amount);
					b.delay(Math.max(10-b.cooldown(), 0));
				}
			}
		}
		if (hero.hasTalent(ENLIGHTENING_MEAL)){
			if (hero.heroClass == HeroClass.CLERIC) {
				HolyTome tome = hero.belongings.getItem(HolyTome.class);
				if (tome != null) {
					// 2/3 of a charge at +1, 1 full charge at +2
					tome.directCharge( (1+hero.pointsInTalent(ENLIGHTENING_MEAL))/3f );
					ScrollOfRecharging.charge(hero);
				}
			} else {
				//2/3 turns of recharging, both kinds
				wandChargeTurns += 1 + hero.pointsInTalent(ENLIGHTENING_MEAL);
				artifactChargeTurns += 1 + hero.pointsInTalent(ENLIGHTENING_MEAL);
			}
		}

		//we process these at the end as they can stack together from some talents
		if (wandChargeTurns > 0){
			Buff.prolong( hero, Recharging.class, wandChargeTurns );
			ScrollOfRecharging.charge( hero );
			SpellSprite.show(hero, SpellSprite.CHARGE);
		}
		if (artifactChargeTurns > 0){
			ArtifactRecharge buff = Buff.affect( hero, ArtifactRecharge.class);
			if (buff.left() < artifactChargeTurns){
				buff.set(artifactChargeTurns).ignoreHornOfPlenty = foodSource instanceof HornOfPlenty;
			}
			ScrollOfRecharging.charge( hero );
			SpellSprite.show(hero, SpellSprite.CHARGE, 0, 1, 1);
		}
	}

	public static void ensureSpecialItems( Hero hero ){
		if (hero.hasTalent(MULTIPLE_EXISTENCE) && hero.belongings.getItem(MirrorLink.class) == null){
			MirrorLink link = new MirrorLink();
			if (!link.collect(hero.belongings.backpack) && Dungeon.level != null){
				Dungeon.level.drop(link, hero.pos).sprite.drop();
			}
		}
	}

	public static void onTalentRemoved( Hero hero, Talent talent ){
		if (talent == CROSSFIRE && !hero.hasTalent(CROSSFIRE)){
			CrossfireTracker tracker = hero.buff(CrossfireTracker.class);
			if (tracker != null) tracker.detach();
		}
		if (talent == PERFECT_EXECUTION && !hero.hasTalent(PERFECT_EXECUTION)){
			PerfectExecutionTracker tracker = hero.buff(PerfectExecutionTracker.class);
			if (tracker != null) tracker.detach();
		}
		if (talent == MULTIPLE_EXISTENCE && !hero.hasTalent(MULTIPLE_EXISTENCE)){
			MirrorLink link = hero.belongings.getItem(MirrorLink.class);
			if (link != null){
				for (Bag bag : hero.belongings.getBags()){
					if (bag.items.contains(link)){
						link.detachAll(bag);
						break;
					}
				}
			}
			if (Dungeon.level != null && Dungeon.level.mobs != null){
				for (Mob mob : Dungeon.level.mobs){
					if (mob instanceof MirrorImage) ((MirrorImage)mob).disableDirection();
				}
			}
			Dungeon.observe();
		}
	}

	public static class WarriorFoodImmunity extends FlavourBuff{
		{ actPriority = HERO_PRIO+1; }
	}

	public static float itemIDSpeedFactor( Hero hero, Item item ){
		float factor = 1f;

		// Affected by both Warrior(1.75x/2.5x) and Duelist(2.5x/inst.) talents
		if (item instanceof MeleeWeapon){
			factor *= 1f + 1.5f*hero.pointsInTalent(ADVENTURERS_INTUITION); //instant at +2 (see onItemEquipped)
			factor *= 1f + 0.75f*hero.pointsInTalent(VETERANS_INTUITION);
		}
		// Affected by both Warrior(2.5x/inst.) and Duelist(1.75x/2.5x) talents
		if (item instanceof Armor){
			factor *= 1f + 0.75f*hero.pointsInTalent(ADVENTURERS_INTUITION);
			factor *= 1f + hero.pointsInTalent(VETERANS_INTUITION); //instant at +2 (see onItemEquipped)
		}
		// 3x/instant for Mage (see Wand.wandUsed())
		if (item instanceof Wand){
			factor *= 1f + 2.0f*hero.pointsInTalent(SCHOLARS_INTUITION);
		}
		// 3x/instant speed with Huntress talent (see MissileWeapon.proc)
		if (item instanceof MissileWeapon){
			factor *= 1f + 2.0f*hero.pointsInTalent(SURVIVALISTS_INTUITION);
		}
		// 2x/instant for Rogue (see onItemEqupped), also id's type on equip/on pickup
		if (item instanceof Ring){
			factor *= 1f + hero.pointsInTalent(THIEFS_INTUITION);
		}
		return factor;
	}

	public static void onPotionUsed( Hero hero, int cell, float factor ){
		if (hero.hasTalent(HOLD_FAST)){
			// Liquid Revival: 5/10/15% of max HP, preserving potion multipliers.
			int healing = Math.min(hero.HT - hero.HP,
					Math.round(factor * hero.HT * 0.05f * hero.pointsInTalent(HOLD_FAST)));
			if (healing > 0) {
				hero.HP += healing;
				if (hero.sprite != null) {
					hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healing), FloatingText.HEALING);
				}
			}
		}
		if (hero.hasTalent(LIQUID_NATURE)){
			ArrayList<Integer> grassCells = new ArrayList<>();
			for (int i : PathFinder.NEIGHBOURS9){
				grassCells.add(cell+i);
			}
			Random.shuffle(grassCells);
			for (int grassCell : grassCells){
				Char ch = Actor.findChar(grassCell);
				if (ch != null && ch.alignment == Char.Alignment.ENEMY){
					//1/2 turns of roots
					Buff.affect(ch, Roots.class, factor * hero.pointsInTalent(LIQUID_NATURE));
				}
				if (Dungeon.level.map[grassCell] == Terrain.EMPTY ||
						Dungeon.level.map[grassCell] == Terrain.EMBERS ||
						Dungeon.level.map[grassCell] == Terrain.EMPTY_DECO){
					Level.set(grassCell, Terrain.GRASS);
					GameScene.updateMap(grassCell);
				}
				CellEmitter.get(grassCell).burst(LeafParticle.LEVEL_SPECIFIC, 4);
			}
			// 4/6 cells total
			int totalGrassCells = (int) (factor * (2 + 2 * hero.pointsInTalent(LIQUID_NATURE)));
			while (grassCells.size() > totalGrassCells){
				grassCells.remove(0);
			}
			for (int grassCell : grassCells){
				int t = Dungeon.level.map[grassCell];
				if ((t == Terrain.EMPTY || t == Terrain.EMPTY_DECO || t == Terrain.EMBERS
						|| t == Terrain.GRASS || t == Terrain.FURROWED_GRASS)
						&& Dungeon.level.plants.get(grassCell) == null){
					Level.set(grassCell, Terrain.HIGH_GRASS);
					GameScene.updateMap(grassCell);
				}
			}
			Dungeon.observe();
		}
		if (hero.hasTalent(LIQUID_AGILITY)){
			Buff.prolong(hero, LiquidAgilEVATracker.class, hero.cooldown() + Math.max(0, factor-1));
			if (factor >= 0.5f){
				Buff.prolong(hero, LiquidAgilACCTracker.class, 5f).uses = Math.round(factor);
			}
		}
	}

	public static void onScrollUsed( Hero hero, int pos, float factor, Class<?extends Item> cls ){
		if (hero.hasTalent(INSCRIBED_POWER)){
			// 2/3 empowered wand zaps
			Buff.affect(hero, ScrollEmpower.class).reset((int) (factor * (1 + hero.pointsInTalent(INSCRIBED_POWER))));
		}
		if (hero.hasTalent(INSCRIBED_STEALTH)){
			// 3/5 turns of stealth
			Buff.affect(hero, Invisibility.class, factor * (1 + 2*hero.pointsInTalent(INSCRIBED_STEALTH)));
			Sample.INSTANCE.play( Assets.Sounds.MELD );
		}
		if (hero.hasTalent(RECALL_INSCRIPTION) && Scroll.class.isAssignableFrom(cls) && cls != ScrollOfUpgrade.class){
			if (hero.heroClass == HeroClass.CLERIC){
				Buff.prolong(hero, RecallInscription.UsedItemTracker.class, hero.pointsInTalent(RECALL_INSCRIPTION) == 2 ? 300 : 10).item = cls;
			} else {
				// 10/15%
				if (Random.Int(20) < 1 + hero.pointsInTalent(RECALL_INSCRIPTION)){
					Reflection.newInstance(cls).collect();
					GLog.p(Messages.get(Talent.class, RECALL_INSCRIPTION.name() + ".refunded"));
				}
			}
		}
	}

	public static void onRunestoneUsed( Hero hero, int pos, Class<?extends Item> cls ){
		if (hero.hasTalent(RECALL_INSCRIPTION) && Runestone.class.isAssignableFrom(cls)){
			if (hero.heroClass == HeroClass.CLERIC){
				Buff.prolong(hero, RecallInscription.UsedItemTracker.class, hero.pointsInTalent(RECALL_INSCRIPTION) == 2 ? 300 : 10).item = cls;
			} else {

				//don't trigger on 1st intuition use
				if (cls.equals(StoneOfIntuition.class) && hero.buff(StoneOfIntuition.IntuitionUseTracker.class) != null){
					return;
				}
				// 10/15%
				if (Random.Int(20) < 1 + hero.pointsInTalent(RECALL_INSCRIPTION)){
					Reflection.newInstance(cls).collect();
					GLog.p(Messages.get(Talent.class, RECALL_INSCRIPTION.name() + ".refunded"));
				}
			}
		}
	}

	public static void onArtifactUsed( Hero hero ){
		if (hero.hasTalent(ENHANCED_RINGS)){
			Buff.prolong(hero, EnhancedRings.class, 3f*hero.pointsInTalent(ENHANCED_RINGS));
		}

		if (Dungeon.hero.heroClass != HeroClass.CLERIC
				&& Dungeon.hero.hasTalent(Talent.DIVINE_SENSE)){
			Buff.prolong(Dungeon.hero, DivineSense.DivineSenseTracker.class, Dungeon.hero.cooldown()+1);
		}

		// 10/20/30%
		if (Dungeon.hero.heroClass != HeroClass.CLERIC
				&& Dungeon.hero.hasTalent(Talent.CLEANSE)
				&& Random.Int(10) < Dungeon.hero.pointsInTalent(Talent.CLEANSE)){
			boolean removed = false;
			for (Buff b : Dungeon.hero.buffs()) {
				if (b.type == Buff.buffType.NEGATIVE
						&& !(b instanceof LostInventory)) {
					b.detach();
					removed = true;
				}
			}
			if (removed && Dungeon.hero.sprite != null) {
				new Flare( 6, 32 ).color(0xFF4CD2, true).show( Dungeon.hero.sprite, 2f );
			}
		}
	}

	public static void onItemEquipped( Hero hero, Item item ){
		boolean identify = false;
		if (hero.pointsInTalent(VETERANS_INTUITION) == 2 && item instanceof Armor){
			identify = true;
		}
		if (hero.hasTalent(THIEFS_INTUITION) && item instanceof Ring){
			if (hero.pointsInTalent(THIEFS_INTUITION) == 2){
				identify = true;
			}
			((Ring) item).setKnown();
		}
		if (hero.pointsInTalent(ADVENTURERS_INTUITION) == 2 && item instanceof Weapon){
			identify = true;
		}

		if (identify) {
			if (ShardOfOblivion.passiveIDDisabled()) {
				if (item instanceof Weapon){
					((Weapon) item).setIDReady();
				} else if (item instanceof Armor){
					((Armor) item).setIDReady();
				} else if (item instanceof Ring){
					((Ring) item).setIDReady();
				}
			} else {
				item.identify();
			}
		}
	}

	public static void onItemCollected( Hero hero, Item item ){
		if (hero.pointsInTalent(THIEFS_INTUITION) == 2){
			if (item instanceof Ring) ((Ring) item).setKnown();
		}
	}

	public static int onAttackProc( Hero hero, Char enemy, int dmg ){

		if (hero.hasTalent(Talent.PROVOKED_ANGER)
			&& hero.buff(ProvokedAngerTracker.class) != null){
			dmg += 1 + 2*hero.pointsInTalent(Talent.PROVOKED_ANGER);
			hero.buff(ProvokedAngerTracker.class).detach();
		}

		if (hero.hasTalent(Talent.LINGERING_MAGIC)
				&& hero.buff(LingeringMagicTracker.class) != null){
			dmg += Random.IntRange(hero.pointsInTalent(Talent.LINGERING_MAGIC) , 2);
			hero.buff(LingeringMagicTracker.class).detach();
		}

		if (hero.hasTalent(Talent.SUCKER_PUNCH)
				&& enemy instanceof Mob && ((Mob) enemy).surprisedBy(hero)
				&& enemy.buff(SuckerPunchTracker.class) == null){
			dmg += Random.IntRange(hero.pointsInTalent(Talent.SUCKER_PUNCH) , 2);
			Buff.affect(enemy, SuckerPunchTracker.class);
		}

		if (hero.hasTalent(Talent.FOLLOWUP_STRIKE) && enemy.isAlive() && enemy.alignment == Char.Alignment.ENEMY) {
			if (hero.belongings.attackingWeapon() instanceof MissileWeapon) {
				Buff.prolong(hero, FollowupStrikeTracker.class, 5f).object = enemy.id();
			} else if (hero.buff(FollowupStrikeTracker.class) != null
					&& hero.buff(FollowupStrikeTracker.class).object == enemy.id()){
				dmg += 1 + hero.pointsInTalent(FOLLOWUP_STRIKE);
				hero.buff(FollowupStrikeTracker.class).detach();
			}
		}

		if (hero.buff(Talent.SpiritBladesTracker.class) != null
				&& Random.Int(10) < 3*hero.pointsInTalent(Talent.SPIRIT_BLADES)){
			SpiritBow bow = hero.belongings.getItem(SpiritBow.class);
			if (bow != null) dmg = bow.proc( hero, enemy, dmg );
			hero.buff(Talent.SpiritBladesTracker.class).detach();
		}

		if (hero.hasTalent(PATIENT_STRIKE)){
			if (hero.buff(PatientStrikeTracker.class) != null
					&& !(hero.belongings.attackingWeapon() instanceof MissileWeapon)){
				hero.buff(PatientStrikeTracker.class).detach();
				dmg += Random.IntRange(hero.pointsInTalent(Talent.PATIENT_STRIKE), 2);
			}
		}

		if (hero.hasTalent(DEADLY_FOLLOWUP) && enemy.alignment == Char.Alignment.ENEMY) {
			if (hero.belongings.attackingWeapon() instanceof MissileWeapon) {
				if (!(hero.belongings.attackingWeapon() instanceof SpiritBow.SpiritArrow)) {
					Buff.prolong(hero, DeadlyFollowupTracker.class, 5f).object = enemy.id();
				}
			} else if (hero.buff(DeadlyFollowupTracker.class) != null
					&& hero.buff(DeadlyFollowupTracker.class).object == enemy.id()){
				dmg = Math.round(dmg * (1.0f + .1f*hero.pointsInTalent(DEADLY_FOLLOWUP)));
			}
		}

		//完美处决：处决就绪时手中武器命中触发处决；投掷武器命中（含灵能短弓）叠1/3层
		if (hero.hasTalent(PERFECT_EXECUTION) && enemy.alignment == Char.Alignment.ENEMY){
			if (hero.belongings.attackingWeapon() instanceof MissileWeapon){
				onExecutionStack(hero, EXEC_COND_THROWN);
			} else {
				tryExecuteOnAttack(hero, enemy);
			}
		}

		return dmg;
	}

	public static class ProvokedAngerTracker extends FlavourBuff{
		{ type = Buff.buffType.POSITIVE; }
		public int icon() { return BuffIndicator.WEAPON; }
		public void tintIcon(Image icon) { icon.hardlight(1.43f, 1.43f, 1.43f); }
		public float iconFadePercent() { return Math.max(0, 1f - (visualcooldown() / 5)); }
	}
	public static class LingeringMagicTracker extends FlavourBuff{
		{ type = Buff.buffType.POSITIVE; }
		public int icon() { return BuffIndicator.WEAPON; }
		public void tintIcon(Image icon) { icon.hardlight(1.43f, 1.43f, 0f); }
		public float iconFadePercent() { return Math.max(0, 1f - (visualcooldown() / 5)); }
	}
	public static class SuckerPunchTracker extends Buff{};
	public static class FollowupStrikeTracker extends FlavourBuff{
		public int object;
		{ type = Buff.buffType.POSITIVE; }
		public int icon() { return BuffIndicator.INVERT_MARK; }
		public void tintIcon(Image icon) { icon.hardlight(0f, 0.75f, 1f); }
		public float iconFadePercent() { return Math.max(0, 1f - (visualcooldown() / 5)); }
		private static final String OBJECT    = "object";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(OBJECT, object);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			object = bundle.getInt(OBJECT);
		}
	};

	public static class VoidWalkerBuff extends FlavourBuff {
		{ type = buffType.POSITIVE; }

		@Override
		public int icon() {
			return target != null && Dungeon.level != null && Dungeon.level.pit[target.pos]
					? BuffIndicator.VOID_WALKER : BuffIndicator.NONE;
		}

		@Override
		public void detach() {
			Char affected = target;
			super.detach();
			if (affected == Dungeon.hero && Dungeon.level != null
					&& Dungeon.level.pit[affected.pos] && !affected.flying){
				Chasm.heroFall(affected.pos);
			}
		}
	}

	public static final int MAX_TALENT_TIERS = 4;

	//完美处决的三个叠层条件（位标记）：投掷武器命中（含灵能短弓）、法杖命中、丢出即被消耗的符石；叠满后手中武器命中触发处决
	public static final int EXEC_COND_THROWN    = 1; //投掷武器命中（含灵能短弓）
	public static final int EXEC_COND_WAND      = 2; //法杖命中
	public static final int EXEC_COND_RUNE      = 4; //丢出即被消耗的符石（不要求命中）
	public static final int EXEC_COND_ALL       = EXEC_COND_THROWN | EXEC_COND_WAND | EXEC_COND_RUNE;

	//本局处决后仍未死亡的怪物id（大部分怪会被直接处决，无需记录），处决不能对同一个怪物生效两次
	private static final HashSet<Integer> executionSurvivors = new HashSet<>();

	//扭曲现实：是否有尚未完成的互换选择（用于点击天赋时重新打开选品窗口）
	public static boolean realityWarpSelectionPending( Hero hero ){
		int pts = hero.pointsInTalent(REALITY_WARP);
		if (pts >= 1 && !RealityWarp.potionSwapped()) return true;
		if (pts >= 2 && !RealityWarp.scrollSwapped()) return true;
		return false;
	}

	//野蛮寄生：目标是否可以被寄生，+1仅限生物，+2对非生物目标也生效
	public static boolean canParasitize( Hero hero, Char target ){
		if (target == null || target == hero || !target.isAlive()) return false;
		if (target.alignment != Char.Alignment.ENEMY) return false;
		if (hero.pointsInTalent(SAVAGE_PARASITE) < 2
				&& (Char.hasProp(target, Char.Property.INORGANIC)
					|| Char.hasProp(target, Char.Property.UNDEAD))){
			return false;
		}
		return true;
	}

	//野蛮寄生：向敌人投掷种子时调用，命中返回true，种子随之消失
	public static boolean trySavageParasite( Hero hero, Plant.Seed seed, Char target ){
		if (!hero.hasTalent(SAVAGE_PARASITE) || !canParasitize(hero, target)) return false;

		//吸取10%的生命生根，可以吸取至死亡
		int drain = Math.min(Math.round(target.HT * 0.10f), target.HP);
		if (drain > 0){
			target.HP -= drain;
			if (target.sprite != null){
				target.sprite.showStatusWithIcon(CharSprite.NEGATIVE, Integer.toString(drain), FloatingText.PHYS_DMG);
			}
		}
		if (!target.isAlive()){
			for (Buff b : target.buffs(Brute.BruteRage.class)){
				b.detach();
			}
			if (!target.isAlive()){
				target.die( hero );
				return true; //目标已被吸干，种子随之消失
			}
		}

		SavageParasitism parasite = Buff.affect(target, SavageParasitism.class);
		parasite.seedClass = seed.getClass();
		GLog.i( Messages.get(Talent.class, "savage_parasite.rooted", target.name()) );
		return true;
	}

	//完美处决：达成一个叠层条件；+1时须按 投掷→法杖→符石 的顺序连续叠层，顺序不符则清空进度，+2任意顺序，全部叠满后处决就绪
	public static void onExecutionStack( Hero hero, int cond ){
		if (!hero.hasTalent(PERFECT_EXECUTION)) return;
		if (cond != EXEC_COND_THROWN && cond != EXEC_COND_WAND && cond != EXEC_COND_RUNE) return;

		PerfectExecutionTracker tracker = hero.buff(PerfectExecutionTracker.class);
		if (tracker == null){
			tracker = Buff.affect(hero, PerfectExecutionTracker.class);
			tracker.conds = 0;
		}

		//+1时顺序不符的行动会清空已有进度；已就绪时不再受叠层条件影响
		if (hero.pointsInTalent(PERFECT_EXECUTION) < 2
				&& tracker.conds != EXEC_COND_ALL
				&& Integer.numberOfTrailingZeros(cond) != Integer.bitCount(tracker.conds)){
			tracker.conds = 0;
			return;
		}

		if ((tracker.conds & cond) != 0) return;

		tracker.conds |= cond;
		if (tracker.conds != EXEC_COND_ALL){
			GLog.i( Messages.get(Talent.class, "perfect_execution.progress", Integer.bitCount(tracker.conds)) );
		}
	}

	//完美处决：手中武器命中敌人时若处决已就绪（三层叠满），则触发处决并消耗就绪状态，返回是否触发
	public static boolean tryExecuteOnAttack( Hero hero, Char enemy ){
		if (!hero.hasTalent(PERFECT_EXECUTION)) return false;
		if (enemy == null || enemy == hero || !enemy.isAlive()) return false;
		if (enemy.alignment != Char.Alignment.ENEMY) return false;
		if (executionSurvivors.contains(enemy.id())) return false;

		PerfectExecutionTracker tracker = hero.buff(PerfectExecutionTracker.class);
		if (tracker == null || tracker.conds != EXEC_COND_ALL) return false;

		executeTarget(hero, enemy);
		return true;
	}

	//交叉火力：英雄的近战/投掷/法杖攻击动作做出即触发（无论是否命中），连段重置为1层
	public static void onCrossfireHeroAttack(){
		if (Dungeon.hero == null || !Dungeon.hero.hasTalent(CROSSFIRE)) return;
		CrossfireTracker tracker = Dungeon.hero.buff(CrossfireTracker.class);
		if (tracker == null) tracker = Buff.affect(Dungeon.hero, CrossfireTracker.class);
		tracker.onHeroAttack();
	}

	//交叉火力：除英雄外的任何单位每次攻击敌方单位（无论是否命中）都会叠加1层
	public static void onCrossfireOtherAttack(){
		if (Dungeon.hero == null || !Dungeon.hero.hasTalent(CROSSFIRE)) return;
		CrossfireTracker tracker = Dungeon.hero.buff(CrossfireTracker.class);
		if (tracker == null) tracker = Buff.affect(Dungeon.hero, CrossfireTracker.class);
		tracker.onOtherAttack();
	}

	//处决等同于触发一次死神附魔，可以对boss使用（boss按死神附魔抗性结算）
	private static void executeTarget( Hero hero, Char target ){
		hero.buff(PerfectExecutionTracker.class).detach();

		int dmg = Math.round(target.HP * target.resist(Grim.class));
		target.HP = Math.max(0, target.HP - dmg);

		if (target.sprite != null){
			target.sprite.emitter().burst( ShadowParticle.UP, 5 );
			target.sprite.showStatus( CharSprite.NEGATIVE, Messages.get(Talent.class, "perfect_execution.executed") );
		}
		GLog.i( Messages.get(Talent.class, "perfect_execution.killed", target.name()) );

		if (!target.isAlive()){
			for (Buff b : target.buffs(Brute.BruteRage.class)){
				b.detach();
			}
			if (!target.isAlive()){
				target.die( hero );
			}
		} else {
			//处决后仍未死亡（如boss承受一半死神伤害），只需记录这些没被处决的怪
			executionSurvivors.add(target.id());
		}
	}

	//交叉火力的跟进连段：英雄攻击动作做出（无论是否命中）即重置为1层，友方每次攻击敌方单位叠1层；
	//英雄的非攻击行动会结束连段并隐藏图标
	public static class CrossfireTracker extends Buff {
		{ type = buffType.POSITIVE; actPriority = HERO_PRIO+1; }
		public int stacks = 0;               //当前连段层数，0为未激活（不显示图标）
		public boolean heroAttacked = false; //英雄刚结束的行动是否为攻击动作

		//层数为0时隐藏图标，仅在攻击后显示
		public int icon() { return stacks > 0 ? BuffIndicator.INVERT_MARK : BuffIndicator.NONE; }
		public void tintIcon(Image icon) { icon.hardlight(1f, 0.5f, 0f); }
		public String iconTextDisplay() { return Integer.toString(stacks); }

		//英雄近战/投掷/法杖攻击动作做出（无论是否命中）：连段重置为1层
		public void onHeroAttack() {
			heroAttacked = true;
			if (stacks != 1){
				stacks = 1;
				BuffIndicator.refreshHero();
			}
		}

		//除英雄外的单位攻击敌方单位的动作做出（无论是否命中）：叠1层
		public void onOtherAttack() {
			if (stacks > 0){
				stacks++;
				BuffIndicator.refreshHero();
			}
		}

		@Override
		public boolean act() {
			//英雄的每次行动之后结算：攻击行动保持连段（已在攻击时重置为1层），非攻击行动结束连段
			if (heroAttacked){
				heroAttacked = false;
			} else if (stacks > 0){
				stacks = 0;
				BuffIndicator.refreshHero();
			}
			spend( TICK );
			return true;
		}

		private static final String STACKS = "ally_hits";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(STACKS, stacks);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			stacks = bundle.getInt(STACKS);
		}
	}

	//完美处决的叠层记录：三个条件叠满后显示就绪图标，buff存在时下一次手中武器命中触发处决
	public static class PerfectExecutionTracker extends Buff {
		{ type = buffType.POSITIVE; }
		public int conds = 0;

		//叠满后才显示图标
		@Override
		public int icon() {
			return conds == EXEC_COND_ALL ? BuffIndicator.INVERT_MARK : BuffIndicator.NONE;
		}
		@Override
		public void tintIcon(Image icon) { icon.hardlight(1f, 0.2f, 0.2f); }

		@Override
		public boolean act() {
			spend( TICK );
			return true;
		}

		private static final String CONDS = "conds";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(CONDS, conds);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			conds = bundle.getInt(CONDS);
		}
	}

	//寄生负面效果，宿主被攻击时结算对应种子的效果；挂上时自动弹出“寄生”负面提示（同石化等负面效果的announced机制）
	public static class SavageParasitism extends Buff {
		{ type = buffType.NEGATIVE; announced = true; }
		public Class<? extends Plant.Seed> seedClass;

		public int icon() { return BuffIndicator.INVERT_MARK; }
		public void tintIcon(Image icon) { icon.hardlight(0f, 0.75f, 0.2f); }

		@Override
		public boolean act() {
			spend( TICK );
			return true;
		}

		public void trigger(){
			Char host = target;
			detach();
			if (host == null || seedClass == null || Dungeon.level == null) return;
			//使用自然之履的催发逻辑：直接结算种子的效果，不受荒芜之地挑战影响
			Plant plant = Reflection.newInstance(seedClass).couch(host.pos, null);
			if (Dungeon.level.heroFOV[host.pos]){
				CellEmitter.get(host.pos).burst(LeafParticle.GENERAL, 6);
				Sample.INSTANCE.play(Assets.Sounds.PLANT);
			}
			plant.activate( host );
		}

		private static final String SEED_CLASS = "seed_class";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(SEED_CLASS, seedClass);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			seedClass = bundle.getClass(SEED_CLASS);
		}
	}

	private static final String EXECUTION_SURVIVORS = "execution_survivors";

	public static void storeExecutedTargets( Bundle bundle ){
		int[] ids = new int[executionSurvivors.size()];
		int i = 0;
		for (int id : executionSurvivors){
			ids[i++] = id;
		}
		bundle.put( EXECUTION_SURVIVORS, ids );
	}

	public static void restoreExecutedTargets( Bundle bundle ){
		executionSurvivors.clear();
		if (bundle.contains(EXECUTION_SURVIVORS)){
			for (int id : bundle.getIntArray(EXECUTION_SURVIVORS)){
				executionSurvivors.add(id);
			}
		}
	}

	public static void initClassTalents( Hero hero ){
		initClassTalents( hero.heroClass, hero.talents, hero.metamorphedTalents );
	}

	public static void initClassTalents( HeroClass cls, ArrayList<LinkedHashMap<Talent, Integer>> talents){
		initClassTalents( cls, talents, new LinkedHashMap<>());
	}

	public static void initClassTalents( HeroClass cls, ArrayList<LinkedHashMap<Talent, Integer>> talents, LinkedHashMap<Talent, Talent> replacements ){
		while (talents.size() < MAX_TALENT_TIERS){
			talents.add(new LinkedHashMap<>());
		}

		ArrayList<Talent> tierTalents = new ArrayList<>();

		//tier 1
		switch (cls){
			case WARRIOR: default:
				Collections.addAll(tierTalents, HEARTY_MEAL, VETERANS_INTUITION, PROVOKED_ANGER, IRON_WILL);
				break;
			case MAGE:
				Collections.addAll(tierTalents, EMPOWERING_MEAL, SCHOLARS_INTUITION, LINGERING_MAGIC, BACKUP_BARRIER);
				break;
			case ROGUE:
				Collections.addAll(tierTalents, CACHED_RATIONS, THIEFS_INTUITION, SUCKER_PUNCH, PROTECTIVE_SHADOWS);
				break;
			case HUNTRESS:
				Collections.addAll(tierTalents, NATURES_BOUNTY, SURVIVALISTS_INTUITION, FOLLOWUP_STRIKE, NATURES_AID);
				break;
			case DUELIST:
				Collections.addAll(tierTalents, STRENGTHENING_MEAL, ADVENTURERS_INTUITION, PATIENT_STRIKE, AGGRESSIVE_BARRIER);
				break;
			case CLERIC:
				Collections.addAll(tierTalents, SATIATED_SPELLS, HOLY_INTUITION, SEARING_LIGHT, SHIELD_OF_LIGHT);
				break;
		}
		for (Talent talent : tierTalents){
			if (replacements.containsKey(talent)){
				talent = replacements.get(talent);
			}
			talents.get(0).put(talent, 0);
		}
		tierTalents.clear();

		//tier 2
		switch (cls){
			case WARRIOR: default:
				Collections.addAll(tierTalents, IRON_STOMACH, LIQUID_WILLPOWER, RUNIC_TRANSFERENCE, LETHAL_MOMENTUM, IMPROVISED_PROJECTILES);
				break;
			case MAGE:
				Collections.addAll(tierTalents, ENERGIZING_MEAL, INSCRIBED_POWER, WAND_PRESERVATION, ARCANE_VISION, SHIELD_BATTERY);
				break;
			case ROGUE:
				Collections.addAll(tierTalents, MYSTICAL_MEAL, INSCRIBED_STEALTH, WIDE_SEARCH, SILENT_STEPS, ROGUES_FORESIGHT);
				break;
			case HUNTRESS:
				Collections.addAll(tierTalents, INVIGORATING_MEAL, LIQUID_NATURE, REJUVENATING_STEPS, HEIGHTENED_SENSES, DURABLE_PROJECTILES);
				break;
			case DUELIST:
				Collections.addAll(tierTalents, FOCUSED_MEAL, LIQUID_AGILITY, WEAPON_RECHARGING, LETHAL_HASTE, SWIFT_EQUIP);
				break;
			case CLERIC:
				Collections.addAll(tierTalents, ENLIGHTENING_MEAL, RECALL_INSCRIPTION, SUNRAY, DIVINE_SENSE, BLESS);
				break;
		}
		for (Talent talent : tierTalents){
			if (replacements.containsKey(talent)){
				talent = replacements.get(talent);
			}
			talents.get(1).put(talent, 0);
		}
		tierTalents.clear();

		//tier 3
		switch (cls){
			case WARRIOR: default:
				Collections.addAll(tierTalents, HOLD_FAST, STRONGMAN);
				break;
			case MAGE:
				Collections.addAll(tierTalents, DESPERATE_POWER, ALLY_WARP);
				break;
			case ROGUE:
				Collections.addAll(tierTalents, ENHANCED_RINGS, LIGHT_CLOAK);
				break;
			case HUNTRESS:
				Collections.addAll(tierTalents, POINT_BLANK, SEER_SHOT);
				break;
			case DUELIST:
				Collections.addAll(tierTalents, PRECISE_ASSAULT, DEADLY_FOLLOWUP);
				break;
			case CLERIC:
				Collections.addAll(tierTalents, CLEANSE, LIGHT_READING);
				break;
		}
		for (Talent talent : tierTalents){
			if (replacements.containsKey(talent)){
				talent = replacements.get(talent);
			}
			talents.get(2).put(talent, 0);
		}
		tierTalents.clear();

		//tier4
		//TBD
	}

	public static void initSubclassTalents( Hero hero ){
		initSubclassTalents( hero.subClass, hero.talents );
	}

	public static void initSubclassTalents( HeroSubClass cls, ArrayList<LinkedHashMap<Talent, Integer>> talents ){
		if (cls == HeroSubClass.NONE) return;

		while (talents.size() < MAX_TALENT_TIERS){
			talents.add(new LinkedHashMap<>());
		}

		ArrayList<Talent> tierTalents = new ArrayList<>();

		//tier 3
		switch (cls){
			case BERSERKER: default:
				Collections.addAll(tierTalents, ENDLESS_RAGE, DEATHLESS_FURY, ENRAGED_CATALYST);
				break;
			case GLADIATOR:
				Collections.addAll(tierTalents, CLEAVE, LETHAL_DEFENSE, ENHANCED_COMBO);
				break;
			case BATTLEMAGE:
				Collections.addAll(tierTalents, EMPOWERED_STRIKE, MYSTICAL_CHARGE, EXCESS_CHARGE);
				break;
			case WARLOCK:
				Collections.addAll(tierTalents, SOUL_EATER, SOUL_SIPHON, NECROMANCERS_MINIONS);
				break;
			case ASSASSIN:
				Collections.addAll(tierTalents, ENHANCED_LETHALITY, ASSASSINS_REACH, BOUNTY_HUNTER);
				break;
			case FREERUNNER:
				Collections.addAll(tierTalents, EVASIVE_ARMOR, PROJECTILE_MOMENTUM, SPEEDY_STEALTH);
				break;
			case SNIPER:
				Collections.addAll(tierTalents, FARSIGHT, SHARED_ENCHANTMENT, SHARED_UPGRADES);
				break;
			case WARDEN:
				Collections.addAll(tierTalents, DURABLE_TIPS, BARKSKIN, SHIELDING_DEW);
				break;
			case CHAMPION:
				Collections.addAll(tierTalents, VARIED_CHARGE, TWIN_UPGRADES, COMBINED_LETHALITY);
				break;
			case MONK:
				Collections.addAll(tierTalents, UNENCUMBERED_SPIRIT, MONASTIC_VIGOR, COMBINED_ENERGY);
				break;
			case PRIEST:
				Collections.addAll(tierTalents, HOLY_LANCE, HALLOWED_GROUND, MNEMONIC_PRAYER);
				break;
			case PALADIN:
				Collections.addAll(tierTalents, LAY_ON_HANDS, AURA_OF_PROTECTION, WALL_OF_LIGHT);
				break;
		}
		for (Talent talent : tierTalents){
			talents.get(2).put(talent, 0);
		}
		tierTalents.clear();

	}

	public static void initArmorTalents( Hero hero ){
		initArmorTalents( hero.armorAbility, hero.talents);
	}

	public static void initArmorTalents(ArmorAbility abil, ArrayList<LinkedHashMap<Talent, Integer>> talents ){
		if (abil == null) return;

		while (talents.size() < MAX_TALENT_TIERS){
			talents.add(new LinkedHashMap<>());
		}

		for (Talent t : abil.talents()){
			talents.get(3).put(t, 0);
		}
	}

	private static final String TALENT_TIER = "talents_tier_";

	public static void storeTalentsInBundle( Bundle bundle, Hero hero ){
		for (int i = 0; i < MAX_TALENT_TIERS; i++){
			LinkedHashMap<Talent, Integer> tier = hero.talents.get(i);
			Bundle tierBundle = new Bundle();

			for (Talent talent : tier.keySet()){
				if (tier.get(talent) > 0){
					tierBundle.put(talent.name(), tier.get(talent));
				}
				if (tierBundle.contains(talent.name())){
					tier.put(talent, Math.min(tierBundle.getInt(talent.name()), talent.maxPoints()));
				}
			}
			bundle.put(TALENT_TIER+(i+1), tierBundle);
		}

		Bundle replacementsBundle = new Bundle();
		for (Talent t : hero.metamorphedTalents.keySet()){
			replacementsBundle.put(t.name(), hero.metamorphedTalents.get(t));
		}
		bundle.put("replacements", replacementsBundle);
	}

	private static final HashSet<String> removedTalents = new HashSet<>();
	static{
		//nothing atm
	}

	private static final HashMap<String, String> renamedTalents = new HashMap<>();
	static{
		//nothing atm
	}

	public static void restoreTalentsFromBundle( Bundle bundle, Hero hero ){
		if (bundle.contains("replacements")){
			Bundle replacements = bundle.getBundle("replacements");
			for (String key : replacements.getKeys()){
				String value = replacements.getString(key);
				if (renamedTalents.containsKey(key)) key = renamedTalents.get(key);
				if (renamedTalents.containsKey(value)) value = renamedTalents.get(value);
				if (!removedTalents.contains(key) && !removedTalents.contains(value)){
					try {
						hero.metamorphedTalents.put(Talent.valueOf(key), Talent.valueOf(value));
					} catch (Exception e) {
						ShatteredPixelDungeon.reportException(e);
					}
				}
			}
		}

		if (hero.heroClass != null)     initClassTalents(hero);
		if (hero.subClass != null)      initSubclassTalents(hero);
		if (hero.armorAbility != null)  initArmorTalents(hero);

		for (int i = 0; i < MAX_TALENT_TIERS; i++){
			LinkedHashMap<Talent, Integer> tier = hero.talents.get(i);
			Bundle tierBundle = bundle.contains(TALENT_TIER+(i+1)) ? bundle.getBundle(TALENT_TIER+(i+1)) : null;

			if (tierBundle != null){
				for (String tName : tierBundle.getKeys()){
					int points = tierBundle.getInt(tName);
					if (renamedTalents.containsKey(tName)) tName = renamedTalents.get(tName);
					if (!removedTalents.contains(tName)) {
						try {
							Talent talent = Talent.valueOf(tName);
							if (tier.containsKey(talent)) {
								tier.put(talent, Math.min(points, talent.maxPoints()));
							}
						} catch (Exception e) {
							ShatteredPixelDungeon.reportException(e);
						}
					}
				}
			}
		}
	}

}
