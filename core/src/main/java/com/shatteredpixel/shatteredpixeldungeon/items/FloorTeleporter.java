package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;

import java.util.ArrayList;

/** Reusable floor navigation utility supplied by the test supply pack. */
public class FloorTeleporter extends Item {

	private static final String NEXT_FLOOR = "NEXT_FLOOR";
	private static final String CHOOSE_FLOOR = "CHOOSE_FLOOR";
	private static final int LAST_FLOOR = 26;
	private static final int PAGE_SIZE = 5;

	{
		image = ItemSpriteSheet.BEACON;
		defaultAction = NEXT_FLOOR;
		unique = true;
		keptThoughLostInvent = true;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(NEXT_FLOOR);
		actions.add(CHOOSE_FLOOR);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (NEXT_FLOOR.equals(action)) {
			if (Dungeon.depth >= LAST_FLOOR) {
				GLog.w(Messages.get(this, "no_next"));
			} else {
				teleportTo(Dungeon.depth + 1);
			}
		} else if (CHOOSE_FLOOR.equals(action)) {
			showFloors(visitedFloors(), 0);
		}
	}

	private ArrayList<Integer> visitedFloors() {
		ArrayList<Integer> floors = new ArrayList<>();
		for (int depth = 1; depth <= Math.min(Statistics.deepestFloor, LAST_FLOOR); depth++) {
			if (depth != Dungeon.depth && Dungeon.levelHasBeenGenerated(depth, 0)) {
				floors.add(depth);
			}
		}
		return floors;
	}

	private void showFloors(ArrayList<Integer> floors, int page) {
		if (floors.isEmpty()) {
			GLog.w(Messages.get(this, "no_visited"));
			return;
		}

		int start = page * PAGE_SIZE;
		int end = Math.min(start + PAGE_SIZE, floors.size());
		ArrayList<String> options = new ArrayList<>();
		for (int i = start; i < end; i++) {
			options.add(Messages.get(this, "floor", floors.get(i)));
		}
		int previous = -1;
		int next = -1;
		if (page > 0) {
			previous = options.size();
			options.add(Messages.get(this, "previous"));
		}
		if (end < floors.size()) {
			next = options.size();
			options.add(Messages.get(this, "next"));
		}
		int cancel = options.size();
		options.add(Messages.get(this, "cancel"));
		int previousIndex = previous;
		int nextIndex = next;

		GameScene.show(new WndOptions(new ItemSprite(this), Messages.titleCase(name()),
				Messages.get(this, "prompt"), options.toArray(new String[0])) {
			@Override
			protected void onSelect(int index) {
				if (index == previousIndex) {
					showFloors(floors, page - 1);
				} else if (index == nextIndex) {
					showFloors(floors, page + 1);
				} else if (index != cancel) {
					teleportTo(floors.get(start + index));
				}
			}
		});
	}

	private void teleportTo(int depth) {
		Level.beforeTransition();
		Invisibility.dispel();
		InterlevelScene.mode = InterlevelScene.Mode.RETURN;
		InterlevelScene.returnDepth = depth;
		InterlevelScene.returnBranch = 0;
		InterlevelScene.returnPos = -1;
		Game.switchScene(InterlevelScene.class);
	}
}
