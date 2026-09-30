package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.BlacksmithRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;

/** 固定的四向连通危险区；墙被清空后立即重新填满，不参与逐回合调度。 */
public final class CaveCollapse implements Hero.Doom {

	private final Level level;
	private final ArrayList<int[]> regions = new ArrayList<>();
	private final ArrayList<Integer> walls = new ArrayList<>();
	private final int[] regionAt;
	private boolean collapsing;

	public CaveCollapse(Level level) {
		this.level = level;
		regionAt = new int[level.length()];
		Arrays.fill(regionAt, -1);
		level.interactions().setDefault(Terrain.COLLAPSE_WALL, TerrainInteractions.Rule.NONE
				.on(TerrainInteractions.Source.CLICK, TerrainInteractions.Response.replaceWith(Terrain.EMPTY))
				.on(TerrainInteractions.Source.MISSILE, TerrainInteractions.Response.replaceWith(Terrain.EMPTY))
				.on(TerrainInteractions.Source.SHOCKWAVE, TerrainInteractions.Response.replaceWith(Terrain.EMPTY))
				.on(TerrainInteractions.Source.WAND, TerrainInteractions.Response.replaceWith(Terrain.EMPTY)));
	}

	public boolean contains(int cell) {
		return cell >= 0 && cell < regionAt.length && regionAt[cell] >= 0;
	}

	/** 在绘制结束后生成；不能完成指定数量时重试整个楼层，避免悄悄少生成。 */
	public boolean generate(ArrayList<Room> rooms) {
		int count = Random.Int(2) == 0 ? 1 : 2;
		ArrayList<Room> candidates = new ArrayList<>();
		for (Room room : rooms) {
			if (room instanceof StandardRoom && !(room instanceof BlacksmithRoom)
					&& !room.isEntrance() && !room.isExit()) candidates.add(room);
		}
		Random.shuffle(candidates);
		for (Room room : candidates) {
			int area = (room.right - room.left - 1) * (room.bottom - room.top - 1);
			int minimum = Math.max(3, (int)Math.ceil(area * 0.05f));
			int maximum = (int)Math.floor(area * 0.5f);
			if (maximum < minimum) continue;
			boolean[] ground = new boolean[level.length()];
			ArrayList<Integer> seeds = new ArrayList<>();
			for (int y = room.top + 1; y < room.bottom; y++) {
				for (int x = room.left + 1; x < room.right; x++) {
					int cell = x + y * level.width();
					int tile = level.map[cell];
					if (tile != Terrain.EMPTY && tile != Terrain.EMPTY_DECO && tile != Terrain.GRASS
							&& tile != Terrain.HIGH_GRASS && tile != Terrain.FURROWED_GRASS) continue;
					if (contains(cell) || level.traps.get(cell) != null || level.plants.get(cell) != null
							|| level.heaps.get(cell) != null || level.findMob(cell) != null
							|| level.getTransition(cell) != null) continue;
					boolean doorway = false;
					for (Room.Door door : room.connected.values()) {
						if (door != null && level.distance(cell, level.pointToCell(door)) <= 1) doorway = true;
					}
					if (!doorway) { ground[cell] = true; seeds.add(cell); }
				}
			}
			if (seeds.size() < minimum) continue;
			for (int attempt = 0; attempt < 64; attempt++) {
				int target = Random.IntRange(minimum, Math.min(maximum, seeds.size()));
				ArrayList<Integer> cells = new ArrayList<>();
				ArrayList<Integer> frontier = new ArrayList<>();
				boolean[] queued = new boolean[level.length()];
				int seed = Random.element(seeds);
				frontier.add(seed);
				queued[seed] = true;
				while (!frontier.isEmpty() && cells.size() < target) {
					int cell = frontier.remove(Random.Int(frontier.size()));
					cells.add(cell);
					for (int offset : PathFinder.NEIGHBOURS4) {
						int next = cell + offset;
						if (level.insideMap(next) && ground[next] && !queued[next]) {
							queued[next] = true;
							frontier.add(next);
						}
					}
				}
				if (cells.size() != target) continue;
				int left = level.width(), right = 0, top = level.height(), bottom = 0;
				for (int cell : cells) {
					left = Math.min(left, cell % level.width()); right = Math.max(right, cell % level.width());
					top = Math.min(top, cell / level.width()); bottom = Math.max(bottom, cell / level.width());
				}
				// 排除任何填满外接矩形的形状，包括一字形；保留随机凹凸和分支。
				if ((right - left + 1) * (bottom - top + 1) == cells.size()) continue;
				boolean[] passable = new boolean[level.length()];
				for (int i = 0; i < passable.length; i++) {
					int flags = Terrain.flags[level.map[i]];
					passable[i] = (flags & (Terrain.PASSABLE | Terrain.AVOID)) != 0 && (flags & Terrain.PIT) == 0;
				}
				PathFinder.buildDistanceMap(seed, passable);
				int[] reachable = PathFinder.distance.clone();
				Random.shuffle(cells);
				ArrayList<Integer> supports = new ArrayList<>();
				int supportCount = Random.IntRange(1, 2);
				for (int cell : cells) {
					// 初始支撑墙的八邻格与其他墙体留出间隔，也不贴另一格支撑墙。
					boolean nearWall = false;
					for (int offset : PathFinder.NEIGHBOURS8) {
						int next = cell + offset;
						if (!level.insideMap(next) || DungeonTileSheet.wallStitcheable(level.map[next])
								|| supports.contains(next)) {
							nearWall = true;
							break;
						}
					}
					if (nearWall) continue;
					passable[cell] = false;
					int origin = -1;
					for (int i = 0; i < reachable.length; i++) {
						if (passable[i] && reachable[i] != Integer.MAX_VALUE) { origin = i; break; }
					}
					if (origin < 0) { passable[cell] = true; continue; }
					PathFinder.buildDistanceMap(origin, passable);
					boolean blocked = false;
					for (int i = 0; i < reachable.length; i++) {
						if (passable[i] && reachable[i] != Integer.MAX_VALUE && PathFinder.distance[i] == Integer.MAX_VALUE) {
							blocked = true; break;
						}
					}
					if (blocked) passable[cell] = true;
					else supports.add(cell);
					if (supports.size() == supportCount) break;
				}
				if (supports.isEmpty()) continue;
				int[] region = new int[cells.size()];
				for (int i = 0; i < region.length; i++) {
					region[i] = cells.get(i);
					regionAt[region[i]] = regions.size();
				}
				regions.add(region);
				walls.add(supports.size());
				for (int cell : supports) Painter.set(level, cell, Terrain.COLLAPSE_WALL);
				break;
			}
			if (regions.size() == count) return true;
		}
		return false;
	}

	/** 从 Level.set 接收实际变化，任何开墙入口都不会遗漏最后一格。 */
	public void terrainChanged(int cell, int before, int after) {
		if (collapsing || !contains(cell) || before == after) return;
		if (before == Terrain.COLLAPSE_WALL && level == Dungeon.level && level.heroFOV[cell]
				&& Game.scene() instanceof GameScene) {
			CellEmitter.get(cell).burst(Speck.factory(Speck.ROCK), 8);
			Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.4f, 1.3f);
		}
		int region = regionAt[cell];
		int remaining = walls.get(region) + (after == Terrain.COLLAPSE_WALL ? 1 : 0)
				- (before == Terrain.COLLAPSE_WALL ? 1 : 0);
		walls.set(region, remaining);
		if (before == Terrain.COLLAPSE_WALL && remaining == 0) collapse(region);
	}

	private void collapse(int index) {
		collapsing = true;
		try {
			int[] cells = regions.get(index);
			LinkedHashSet<Char> victims = new LinkedHashSet<>(Actor.chars());
			victims.addAll(level.mobs);
			if (Dungeon.hero != null) victims.add(Dungeon.hero);
			victims.removeIf(ch -> !ch.isAlive() || !contains(ch.pos) || regionAt[ch.pos] != index);
			boolean killHero = victims.remove(Dungeon.hero);
			// 英雄最后结算，避免终局先保存成绩/删除存档，其他单位尚未完成掉落。
			for (Char ch : victims) { ch.HP = 0; ch.die(this); }
			boolean seen = false;
			for (int cell : cells) {
				if (level.plants.get(cell) != null) level.plants.get(cell).wither();
				Level.set(cell, Terrain.COLLAPSE_WALL, level);
				for (int offset : PathFinder.NEIGHBOURS9) {
					int next = cell + offset;
					if (level.discoverable != null) level.discoverable[next] = true;
					GameScene.updateMap(next);
				}
				if (level.heroFOV[cell] && Game.scene() instanceof GameScene) {
					CellEmitter.get(cell).burst(Speck.factory(Speck.ROCK), 6);
					seen = true;
				}
			}
			walls.set(index, cells.length);
			if (seen) {
				PixelScene.shake(3, 0.7f);
				Sample.INSTANCE.play(Assets.Sounds.ROCKS);
				GLog.w(Messages.get(this, "collapse"));
			}
			if (killHero) { Dungeon.hero.HP = 0; Dungeon.hero.die(this); }
		} finally {
			collapsing = false;
		}
	}

	public void storeInBundle(Bundle bundle) {
		bundle.put("count", regions.size());
		for (int i = 0; i < regions.size(); i++) bundle.put("region_" + i, regions.get(i));
	}

	public void restoreFromBundle(Bundle bundle) {
		regions.clear(); walls.clear(); Arrays.fill(regionAt, -1);
		for (int i = 0; i < bundle.getInt("count"); i++) {
			int[] cells = bundle.getIntArray("region_" + i);
			int remaining = 0;
			for (int cell : cells) {
				regionAt[cell] = i;
				if (level.map[cell] == Terrain.COLLAPSE_WALL) remaining++;
			}
			regions.add(cells); walls.add(remaining);
		}
	}

	@Override public void onDeath() {
		Dungeon.fail(getClass());
		GLog.n(Messages.get(this, "ondeath"));
	}
}
