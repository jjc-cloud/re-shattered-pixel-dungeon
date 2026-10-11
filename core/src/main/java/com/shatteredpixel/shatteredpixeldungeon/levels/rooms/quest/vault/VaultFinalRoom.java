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

package com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.vault;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultTokenDoor;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.quest.vault.VaultBossElemental;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ImpSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage;
import com.watabou.noosa.Image;
import com.watabou.noosa.Tilemap;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class VaultFinalRoom extends SpecialRoom {

	@Override
	public boolean isExit() {
		return true;
	}

	@Override
	public boolean canPlaceGrass(Point p) {
		return false;
	}

	@Override
	public boolean canPlaceWater(Point p) {
		return false;
	}

	@Override
	public void paint(Level level) {
		// The fixed arena is painted together with the rest of the map by VaultLayout.
	}

	@Override
	public boolean canPlaceItem(Point p, Level l) {
		return false;
	}

	private Point entryDoor;
	private Point lockedDoor;

	private int entryDoorWidth = 1;

	public void configureFixedDoors(Point entry, Point treasure, int width) {
		entryDoor = entry;
		lockedDoor = treasure;
		entryDoorWidth = width;
	}

	private void setEntryDoor(int terrain) {
		int center = Dungeon.level.pointToCell(entryDoor);
		if (entryDoorWidth == 3) {
			for (Mob mob : new ArrayList<>(Dungeon.level.mobs)) if (mob instanceof VaultTokenDoor && mob.pos == center) {
				mob.destroy();
				if (mob.sprite != null) mob.sprite.killAndErase();
			}
			if (terrain == Terrain.LOCKED_DOOR) {
				VaultTokenDoor door = new VaultTokenDoor();
				door.doorWidth = 3;
				door.battleSeal = true;
				door.pos = center;
				GameScene.add(door);
			}
		}
		for (int offset = -entryDoorWidth / 2; offset <= entryDoorWidth / 2; offset++) {
			Level.set(center + offset, entryDoorWidth == 3 ? (terrain == Terrain.LOCKED_DOOR ? Terrain.CUSTOM_DECO : Terrain.EMPTY) : terrain);
			GameScene.updateMap(center + offset);
		}
	}

	private boolean impWarningShown = false;
	private int warnState = 0;
	private boolean lockTriggered = false;

	public void processHeroStep(Hero hero){
		if (!impWarningShown && Dungeon.level instanceof VaultLevel) {
			impWarningShown = true;
			hero.interrupt();
			ShatteredPixelDungeon.runOnRenderThread(() -> GameScene.show(new WndTitledMessage(new ImpSprite(),
					Messages.titleCase(Messages.get(Imp.class, "name")), Messages.get(VaultFinalRoom.class, "imp_warning_battle"))));
		}
		if (!lockTriggered){
			Point heroPos = Dungeon.level.cellToPoint(hero.pos);
			// The plain floor inside the columns is safe; special floor starts the ritual.
			boolean inRitual = VaultLayout.tile(heroPos.x, heroPos.y) == ':';
			boolean atEdge = false;
			for (int dy = -1; dy <= 1; dy++) {
				for (int dx = -1; dx <= 1; dx++) {
					atEdge |= VaultLayout.tile(heroPos.x + dx, heroPos.y + dy) == ':';
				}
			}
			if (inRitual){
				ArrayList<Integer> bossSpots = new ArrayList<>();
				for (CustomTilemap tile : Dungeon.level.customTiles) {
					if (tile instanceof MarkerTiles) {
						int cell = tile.tileX + tile.tileW / 2
								+ (tile.tileY + tile.tileH / 2) * Dungeon.level.width();
						if (Dungeon.level.passable[cell] && Actor.findChar(cell) == null) bossSpots.add(cell);
					}
				}
				// Wait for three free marker centers rather than spawn outside a marker.
				if (bossSpots.size() < 3) return;
				Random.shuffle(bossSpots);
				ArrayList<VaultBossElemental.ElementalForm> forms = new ArrayList<>();
				forms.add(VaultBossElemental.ElementalForm.FIRE);
				forms.add(VaultBossElemental.ElementalForm.FROST);
				forms.add(VaultBossElemental.ElementalForm.SHOCK);
				Random.shuffle(forms);
				setEntryDoor(Terrain.LOCKED_DOOR);
				for (int i = 0; i < 3; i++) {
					VaultBossElemental boss = new VaultBossElemental();
					boss.pos = bossSpots.get(i);
					boss.setElementalForm(forms.get(i));
					GameScene.add(boss, 1);
					// One turn of grace and initial FOV prevent an opening surprise attack.
					boss.fieldOfView = new boolean[Dungeon.level.length()];
					Dungeon.level.updateFieldOfView(boss, boss.fieldOfView);
					boss.aggro(hero);
					boss.sprite.turnTo(boss.pos, hero.pos);
					boss.setElementalForm(boss.curForm());
				}
				Dungeon.level.seal();
				lockTriggered = true;
			} else if (atEdge && warnState < 2) {
				GLog.n(Messages.get(VaultFinalRoom.class, "final_warning"));
				Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
				hero.interrupt();
				warnState = 2;
			} else if (!atEdge && warnState == 2){
				warnState = 1;
			}
		}
	}

	public boolean elementalWasSummoned(){
		return lockTriggered;
	}

	public void unlock(){
		setEntryDoor(Terrain.DOOR);
		Level.set(Dungeon.level.pointToCell(lockedDoor), Terrain.DOOR);
		GameScene.updateMap(Dungeon.level.pointToCell(lockedDoor));
		for (Heap h : Dungeon.level.heaps.valueList()){
			Item i = h.peek();
			//only final treasure rings don't have level known
			if (i instanceof Ring && !i.levelKnown){
				i.identify(false);
			}
		}
	}

	private static final String ENTRY_DOOR_X = "entry_door_x";
	private static final String ENTRY_DOOR_Y = "entry_door_y";
	private static final String LOCKED_DOOR_X = "locked_door_x";
	private static final String LOCKED_DOOR_Y = "locked_door_y";
	private static final String WARN_STATE = "warn_state";
	private static final String LOCK_TRIGGERED = "lock_triggered";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);

		bundle.put("entry_door_width", entryDoorWidth);
		bundle.put(ENTRY_DOOR_X, entryDoor.x);
		bundle.put(ENTRY_DOOR_Y, entryDoor.y);

		bundle.put(LOCKED_DOOR_X, lockedDoor.x);
		bundle.put(LOCKED_DOOR_Y, lockedDoor.y);

		bundle.put("imp_warning_shown", impWarningShown);
		bundle.put(WARN_STATE, warnState);
		bundle.put(LOCK_TRIGGERED, lockTriggered);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);

		entryDoorWidth = bundle.getInt("entry_door_width");
		entryDoor = new Point();
		entryDoor.x = bundle.getInt(ENTRY_DOOR_X);
		entryDoor.y = bundle.getInt(ENTRY_DOOR_Y);

		lockedDoor = new Point();
		lockedDoor.x = bundle.getInt(LOCKED_DOOR_X);
		lockedDoor.y = bundle.getInt(LOCKED_DOOR_Y);

		impWarningShown = bundle.getBoolean("imp_warning_shown");
		warnState = bundle.getInt(WARN_STATE);
		lockTriggered = bundle.getBoolean(LOCK_TRIGGERED);
	}

	public static class MarkerTiles extends CustomTilemap {

		{
			texture = Assets.Environment.CITY_QUEST;
			tileW = tileH = 9;
		}

		final int TEX_WIDTH = 256;

		@Override
		public Tilemap create() {
			Tilemap v = super.create();
			v.map(mapSimpleImage(5, 4, TEX_WIDTH), 9);
			return v;
		}

		@Override
		public Image image(int tileX, int tileY) {
			//no examine image on sides
			if (tileX == 0 || tileY == 0 || tileX == tileW-1 || tileY == tileH-1){
				return null;
			}
			//...or corners 1 in from the sides
			if ((tileX == 1 || tileX == tileW-2) && (tileY == 1 || tileY == tileH-2)){
				return null;
			}
			return super.image(tileX, tileY);
		}

		@Override
		public String name(int tileX, int tileY) {
			return Messages.get(this, "name");
		}

		@Override
		public String desc(int tileX, int tileY) {
			return Messages.get(this, "desc");
		}

	}

}
