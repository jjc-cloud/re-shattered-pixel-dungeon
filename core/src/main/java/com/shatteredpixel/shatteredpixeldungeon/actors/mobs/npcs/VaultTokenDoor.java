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

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.MagicPocket;
import com.watabou.utils.Bundle;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.DwarfToken;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.VaultTokenDoorSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

public class VaultTokenDoor extends NPC {

	public int doorWidth = 1;
	public boolean battleSeal;

	@Override
	public boolean canInteract(Char c) {
		for (int offset = -doorWidth / 2; offset <= doorWidth / 2; offset++) {
			if (Dungeon.level.adjacent(pos + offset, c.pos)) return true;
		}
		return super.canInteract(c);
	}

	@Override
	public String description() {
		return doorWidth == 3 ? Messages.get(this, battleSeal ? "sealed_wide" : "desc_wide") : super.description();
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("door_width", doorWidth);
		bundle.put("battle_seal", battleSeal);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		doorWidth = bundle.contains("door_width") ? bundle.getInt("door_width") : 1;
		battleSeal = bundle.getBoolean("battle_seal");
	}


	{
		spriteClass = VaultTokenDoorSprite.class;

		properties.add(Property.IMMOVABLE);
		properties.add(Property.OBJECT);
	}

	@Override
	protected void throwItems() {
		Heap heap = Dungeon.level.heaps.get( pos );
		if (heap != null) {
			Dungeon.level.drop( heap.pickUp(), pos+Dungeon.level.width() ).sprite.drop( pos );
		}
	}

	@Override
	public boolean interact(Char c) {
		if (c instanceof Hero){
			Hero h = (Hero) c;

			int required = doorWidth == 3 ? 6 : 10;
			MagicPocket pocket = h.belongings.getItem(MagicPocket.class);
			Item tokens = doorWidth == 3 ? (pocket == null ? null : pocket.items.stream()
					.filter(item -> item instanceof DwarfToken && item.isVaultLoot()).findFirst().orElse(null))
					: h.belongings.getItem(DwarfToken.class);

			String descText = description();
			if (!battleSeal) {
				if (tokens == null){
					descText += "\n\n" + Messages.get(this, "no_tokens");
				} else if (tokens.quantity() < required){
					descText += "\n\n" + Messages.get(this, "too_few_tokens");
				} else {
					descText += "\n\n" + Messages.get(this, "enough_tokens");
				}
			}

			String finalDescText = descText;

			ShatteredPixelDungeon.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					if (tokens != null && !battleSeal && tokens.quantity() >= required) {
						GameScene.show(new WndOptions(sprite(),
								Messages.titleCase(name()),
								finalDescText,
								Messages.get(VaultTokenDoor.class, "open"),
								Messages.get(VaultTokenDoor.class, "not_yet")) {
							@Override
							protected void onSelect(int index) {
								super.onSelect(index);
								if (index == 0){
									if (!Dungeon.level.mobs.contains(VaultTokenDoor.this) || battleSeal || tokens.quantity() < required) return;
									c.sprite.operate(pos);
									Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
									Sample.INSTANCE.playDelayed(Assets.Sounds.UNLOCK, 0.25f);
									GLog.p(Messages.get(VaultTokenDoor.class, doorWidth == 3 ? "unlocked_wide" : "unlocked"));
									VaultTokenDoor.this.destroy();
									if (doorWidth == 3 && Dungeon.level instanceof VaultLevel
											&& ((VaultLevel) Dungeon.level).isRegularQuest() && !Imp.Quest.vaultGateOpened) {
										Imp.Quest.vaultGateOpened = true;
										Statistics.questScores[3] += 2000;
									}
									for (int offset = -doorWidth / 2; offset <= doorWidth / 2; offset++) {
										Level.set(pos + offset, doorWidth == 3 ? Terrain.EMPTY : Terrain.EMPTY_SP);
										GameScene.updateMap(pos + offset);
										ScrollOfMagicMapping.discover(pos + offset);
									}
									sprite.killAndErase();
									if (doorWidth == 3 && tokens.quantity() > required) tokens.quantity(tokens.quantity() - required);
									else tokens.detachAll(h.belongings.backpack);
									Item.updateQuickslot();
								}
							}
						});
					} else {
						GameScene.show(new WndTitledMessage(sprite(),
								Messages.titleCase(name()),
								finalDescText));
					}
				}

			});
		}

		return false;
	}

	@Override
	public int defenseSkill( Char enemy ) {
		return INFINITE_EVASION;
	}

	@Override
	public void damage( int dmg, Object src ) {
		//do nothing
	}

	@Override
	public boolean add( Buff buff ) {
		return false;
	}

	@Override
	public boolean reset() {
		return true;
	}
}
