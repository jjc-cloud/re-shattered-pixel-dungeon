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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Combo;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Berserk;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HoldFast;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ShieldBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUseItem;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameMath;

import java.util.ArrayList;
import java.util.Arrays;

public class BrokenSeal extends Item {

	public static final String AC_AFFIX = "AFFIX";

	//only to be used from the quickslot, for tutorial purposes mostly.
	public static final String AC_INFO = "INFO_WINDOW";

	{
		image = ItemSpriteSheet.SEAL;

		cursedKnown = levelKnown = true;
		unique = true;
		bones = false;

		defaultAction = AC_INFO;
	}

	private Armor.Glyph glyph;

	// Same item type and save identity; the warrior's subclass unlocks its full form.
	public static boolean isComplete() {
		return isComplete(Dungeon.hero);
	}

	private static boolean isComplete(Hero hero) {
		return hero != null && hero.heroClass == HeroClass.WARRIOR
				&& (hero.subClass == HeroSubClass.GLADIATOR || hero.subClass == HeroSubClass.BERSERKER);
	}

	public boolean canTransferGlyph(){
		if (glyph == null){
			return false;
		}
		if (Dungeon.hero.pointsInTalent(Talent.RUNIC_TRANSFERENCE) == 2){
			return true;
		} else if (Dungeon.hero.pointsInTalent(Talent.RUNIC_TRANSFERENCE) == 1
			&& (Arrays.asList(Armor.Glyph.common).contains(glyph.getClass())
				|| Arrays.asList(Armor.Glyph.uncommon).contains(glyph.getClass()))){
			return true;
		} else {
			return false;
		}
	}

	public Armor.Glyph getGlyph(){
		return glyph;
	}

	public void setGlyph( Armor.Glyph glyph ){
		this.glyph = glyph;
	}

	public int maxShield( int armTier, int armLvl ){
		// Armor-tier base, plus 10/20% max HP from Iron Will in either seal form.
		return 3 + 2*armTier
				+ Math.round(Dungeon.hero.HT * 0.1f * Dungeon.hero.pointsInTalent(Talent.IRON_WILL));
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return glyph != null ? glyph.glowing() : null;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions =  super.actions(hero);
		actions.add(AC_AFFIX);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {

		super.execute(hero, action);

		if (action.equals(AC_AFFIX)){
			curItem = this;
			GameScene.selectItem(armorSelector);
		} else if (action.equals(AC_INFO)) {
			GameScene.show(new WndUseItem(null, this));
		}
	}

	//outgoing is either the seal itself as an item, or an armor the seal is affixed to
	public void affixToArmor(Armor armor, Item outgoing){
		if (armor != null) {
			if (!armor.cursedKnown){
				GLog.w(Messages.get(BrokenSeal.class, "unknown_armor"));

			} else if (armor.cursed && (getGlyph() == null || !getGlyph().curse())){
				GLog.w(Messages.get(BrokenSeal.class, "cursed_armor"));

			} else {
				if (outgoing == this) {
					detach(Dungeon.hero.belongings.backpack);
				} else if (outgoing instanceof Armor){
					((Armor) outgoing).detachSeal();
				}

				GLog.p(Messages.get(BrokenSeal.class, "affix"));
				Dungeon.hero.sprite.operate(Dungeon.hero.pos);
				Sample.INSTANCE.play(Assets.Sounds.UNLOCK);
				armor.affixSeal(this);
				Dungeon.hero.next();
			}
		}
	}

	@Override
	public String name() {
		String name = isComplete() ? Messages.get(this, "complete_name") : super.name();
		return glyph != null ? glyph.name(name) : name;
	}

	@Override
	public String desc() {
		if (!isComplete()) return super.desc();
		return Messages.get(this, "complete_desc") + "\n\n" + Messages.get(this,
				Dungeon.hero.subClass == HeroSubClass.GLADIATOR ? "gladiator_desc" : "berserker_desc");
	}

	@Override
	public String info() {
		String info = super.info();
		if (glyph != null){
			info += "\n\n" + Messages.get(this, "inscribed", glyph.name());
			info += " " + glyph.desc();
		}
		return info;
	}

	@Override
	//scroll of upgrade can be used directly once, same as upgrading armor the seal is affixed to then removing it.
	public boolean isUpgradable() {
		return level() == 0;
	}

	protected static WndBag.ItemSelector armorSelector = new WndBag.ItemSelector() {

		@Override
		public String textPrompt() {
			return  Messages.get(BrokenSeal.class, "prompt");
		}

		@Override
		public Class<?extends Bag> preferredBag(){
			return Belongings.Backpack.class;
		}

		@Override
		public boolean itemSelectable(Item item) {
			return item instanceof Armor;
		}

		@Override
		public void onSelect( Item item ) {
			if (item instanceof Armor) {
				BrokenSeal seal = (BrokenSeal) curItem;
				seal.affixToArmor((Armor)item, seal);
			}
		}
	};

	private static final String GLYPH = "glyph";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(GLYPH, glyph);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		glyph = (Armor.Glyph)bundle.get(GLYPH);
	}

	public static class WarriorShield extends ShieldBuff {

		{
			type = buffType.POSITIVE;

			detachesAtZero = false;
			shieldUsePriority = 2;
		}

		private Armor armor;

		private int cooldown = 0;
		private float turnsSinceEnemies = 0;
		private int initialShield = 0;

		private int gladiatorDamage;
		private float berserkerLoss;
		private boolean guardReady;
		private boolean deathShield;

		private int cooldownDuration() {
			return target instanceof Hero && isComplete((Hero)target) ? 100 : 150;
		}

		public void updateForm() {
			cooldown = Math.max(-cooldownDuration(), Math.min(cooldown, cooldownDuration()));
			BuffIndicator.refreshHero();
		}

		public boolean completeSealEquipped() {
			return target instanceof Hero && isComplete((Hero)target)
					&& armor != null && armor.isEquipped((Hero)target) && armor.checkSeal() != null;
		}

		// Called after damage mitigation, before shields are consumed.
		public boolean blockEnemyAttack(int damage, Object source) {
			if (damage <= 0 || !guardReady || !completeSealEquipped()
					|| ((Hero)target).subClass != HeroSubClass.BERSERKER || !enemyAttack(source)) return false;
			guardReady = false;
			cooldown = 0;
			activate();
			return true;
		}

		private boolean enemyAttack(Object source) {
			if (source instanceof Char) return ((Char)source).alignment == Char.Alignment.ENEMY;
			// Ranged monster attacks use nested damage markers (e.g. DarkBolt).
			for (Class<?> type = source.getClass().getEnclosingClass(); type != null; type = type.getEnclosingClass()) {
				if (Mob.class.isAssignableFrom(type)) return true;
			}
			return false;
		}

		// Only actual HP loss counts, never damage absorbed by any shield.
		public void onHealthLost(int loss) {
			if (loss <= 0 || !completeSealEquipped()) return;
			if (((Hero)target).subClass == HeroSubClass.GLADIATOR) {
				gladiatorDamage += loss;
				if (gladiatorDamage >= 25) {
					gladiatorDamage %= 25;
					cooldown = 0;
				}
			} else if (((Hero)target).subClass == HeroSubClass.BERSERKER) {
				berserkerLoss += loss / (float)target.HT;
				if (berserkerLoss + 0.000001f >= 0.7f) {
					berserkerLoss = Math.max(0, (berserkerLoss + 0.000001f) % 0.7f);
					guardReady = true;
				}
			}
			BuffIndicator.refreshHero();
		}

		@Override
		public int icon() {
			if (coolingDown() || shielding() > 0 || cooldown < 0 || guardReady){
				return BuffIndicator.SEAL_SHIELD;
			} else {
				return BuffIndicator.NONE;
			}
		}

		@Override
		public void tintIcon(Image icon) {
			icon.resetColor();
			if (guardReady && completeSealEquipped()) {
				icon.hardlight(1f, 0.8f, 0.2f);
			} else if (coolingDown() && shielding() == 0){
				icon.brightness(0.3f);
			} else if (cooldown < 0) {
				icon.invert();
			}
		}

		@Override
		public float iconFadePercent() {
			if (shielding() > 0){
				return GameMath.gate(0, 1f - shielding()/(float)initialShield, 1);
			} else if (coolingDown()){
				return GameMath.gate(0, cooldown / (float)cooldownDuration(), 1);
			} else if (cooldown < 0) {
				return GameMath.gate(0, (cooldownDuration()+cooldown) / (float)cooldownDuration(), 1);
			} else {
				return 0;
			}
		}

		@Override
		public String iconTextDisplay() {
			if (shielding() > 0){
				return Integer.toString(shielding());
			} else if (coolingDown() || cooldown < 0){
				return Integer.toString(cooldown);
			} else {
				return "";
			}
		}

		@Override
		public String desc() {
			String description;
			if (shielding() > 0) {
				description = Messages.get(this, "desc_active", shielding(), cooldown);
			} else if (cooldown < 0) {
				description = Messages.get(this, "desc_negative_cooldown", cooldown, cooldownDuration());
			} else {
				description = Messages.get(this, "desc_cooldown", cooldown);
			}
			if (completeSealEquipped()) {
				if (((Hero)target).subClass == HeroSubClass.GLADIATOR) {
					description += "\n\n" + Messages.get(this, "gladiator_progress", gladiatorDamage);
				} else {
					description += "\n\n" + Messages.get(this, "berserker_progress", Math.round(berserkerLoss * 100));
					if (guardReady) description += "\n\n" + Messages.get(this, "guard_ready");
				}
			}
			return description;
		}

		@Override
		public synchronized boolean act() {
			// Also handles upgraded seals loaded from an older save.
			updateForm();
			if (cooldown > 0 && Regeneration.regenOn()){
				cooldown--;
			}

			if (shielding() > 0){
				if (Dungeon.hero.visibleEnemies() == 0 && Dungeon.hero.buff(Combo.class) == null){
					turnsSinceEnemies += HoldFast.buffDecayFactor(target);
					if (turnsSinceEnemies >= 5){
						if (cooldown > 0) {
							float percentLeft = shielding() / (float)initialShield;
							//max of 50% cooldown refund
							cooldown = Math.max(0, (int)(cooldown - cooldownDuration() * (percentLeft / 2f)));
						}
						decShield(shielding());
					}
				} else {
					turnsSinceEnemies = 0;
				}
			}
			
			if (shielding() <= 0 && maxShield() <= 0 && cooldown == 0){
				detach();
			}
			
			spend(TICK);
			return true;
		}

		public synchronized void activate() {
			deathShield = false;
			incShield(maxShield());
			cooldown = Math.max(0, cooldown+cooldownDuration());
			turnsSinceEnemies = 0;
			initialShield = maxShield();
		}

		public synchronized void activateDeathShield() {
			if (shielding() > 0) decShield(shielding());
			incShield(maxShield());
			cooldown = Math.max(0, cooldown + cooldownDuration());
			turnsSinceEnemies = 0;
			initialShield = shielding();
			deathShield = shielding() > 0;
		}

		public boolean isDeathShield() {
			return deathShield && shielding() > 0 && completeSealEquipped();
		}

		@Override
		public void decShield(int amount) {
			super.decShield(amount);
			if (shielding() <= 0) deathShield = false;
		}

		@Override
		public int absorbDamage(int damage) {
			int result = super.absorbDamage(damage);
			if (shielding() <= 0) deathShield = false;
			return result;
		}

		public boolean coolingDown(){
			return cooldown > 0;
		}

		public void reduceCooldown(float percentage){
			cooldown -= Math.round(cooldownDuration()*percentage);
			cooldown = Math.max(cooldown, -cooldownDuration());
		}

		public synchronized void setArmor(Armor arm){
			armor = arm;
			if (arm == null || target instanceof Hero && !arm.isEquipped((Hero) target)) deathShield = false;
		}

		public synchronized int maxShield() {
			if (armor != null && armor.isEquipped((Hero)target) && armor.checkSeal() != null) {
				int result = armor.checkSeal().maxShield(armor.tier, armor.level());
				if (((Hero) target).subClass == HeroSubClass.BERSERKER) {
					Berserk rage = target.buff(Berserk.class);
					if (rage != null) result += Math.round(armor.DRMax() * rage.power());
				}
				return result;
			} else {
				return 0;
			}
		}

		public static final String COOLDOWN = "cooldown";
		public static final String TURNS_SINCE_ENEMIES = "turns_since_enemies";
		public static final String INITIAL_SHIELD = "initial_shield";
		private static final String GLADIATOR_DAMAGE = "gladiator_damage";
		private static final String BERSERKER_LOSS = "berserker_loss";
		private static final String GUARD_READY = "guard_ready";
		private static final String DEATH_SHIELD = "death_shield";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(COOLDOWN, cooldown);
			bundle.put(TURNS_SINCE_ENEMIES, turnsSinceEnemies);
			bundle.put(INITIAL_SHIELD, initialShield);
			bundle.put(GLADIATOR_DAMAGE, gladiatorDamage);
			bundle.put(BERSERKER_LOSS, berserkerLoss);
			bundle.put(GUARD_READY, guardReady);
			bundle.put(DEATH_SHIELD, deathShield);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			gladiatorDamage = bundle.getInt(GLADIATOR_DAMAGE);
			berserkerLoss = bundle.getFloat(BERSERKER_LOSS);
			guardReady = bundle.getBoolean(GUARD_READY);
			deathShield = bundle.getBoolean(DEATH_SHIELD);
			if (bundle.contains(COOLDOWN)) {
				cooldown = bundle.getInt(COOLDOWN);
				turnsSinceEnemies = bundle.getFloat(TURNS_SINCE_ENEMIES);
				initialShield = bundle.getInt(INITIAL_SHIELD);

			//if we have shield from pre-3.1, have it last a bit
			} else if (shielding() > 0) {
				turnsSinceEnemies = -100;
				initialShield = shielding();
			}
		}
	}
}
