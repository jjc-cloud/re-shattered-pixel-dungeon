package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.tiles.custom.Carpet;
import com.watabou.noosa.Group;
import com.watabou.utils.Random;

import java.util.ArrayDeque;
import java.util.ArrayList;

/** 隐藏楼层：随机大小形状的单间，按所在区域继承默认环境与特色装饰，三个宝箱基座。 */
public class HiddenLevel extends Level {

	{
		viewDistance = 1;
	}

	private int region() {
		return (Dungeon.depth - 1) / 5;
	}

	@Override
	protected boolean build() {
		int region = region();
		int w = 13 + 2 * Random.Int(5);
		int h = 13 + 2 * Random.Int(5);
		setSize(w, h);
		Painter.fill(this, 0, 0, w, h, Terrain.WALL);
		Painter.fill(this, 1, 1, w - 2, h - 2, Terrain.EMPTY);

		//随机形状：切四角 + 边缘浅凹口；底边正中留给入口，不动
		int maxCut = region == 2 ? 5 : 3; //矿洞的房间最不规则
		cutCorner(1, 1, +1, +1, Random.Int(maxCut + 1));
		cutCorner(w - 2, 1, -1, +1, Random.Int(maxCut + 1));
		cutCorner(1, h - 2, +1, -1, Random.Int(maxCut + 1));
		cutCorner(w - 2, h - 2, -1, -1, Random.Int(maxCut + 1));
		int notches = Random.Int(region == 2 ? 4 : 3);
		for (int i = 0; i < notches; i++) {
			int len = 2 + Random.Int(3);
			int depth = 1 + Random.Int(2);
			switch (Random.Int(3)) { //0上 1左 2右
				case 0:
					Painter.fill(this, 2 + Random.Int(w - 4 - len), 1, len, depth, Terrain.WALL);
					break;
				case 1:
					Painter.fill(this, 1, 2 + Random.Int(h - 4 - depth), depth, len, Terrain.WALL);
					break;
				default:
					Painter.fill(this, w - 1 - depth, 2 + Random.Int(h - 4 - depth), depth, len, Terrain.WALL);
					break;
			}
		}

		//入口放 h-3 行（老布局约定）：更新其邻格地形时不会探到地图外
		int entrance = w / 2 + (h - 3) * width();
		Painter.set(this, entrance, Terrain.ENTRANCE);
		transitions.add(new LevelTransition(this, entrance, LevelTransition.Type.BRANCH_ENTRANCE,
				Dungeon.depth, 0, LevelTransition.Type.BRANCH_EXIT));

		//环境（水、草、地面装饰）直接沿用各区域关卡的默认 painter 配置
		Painter painter;
		switch (region) {
			case 0: painter = new SewerLevel().painter(); break;
			case 1: painter = new PrisonLevel().painter(); break;
			case 2: painter = new CavesLevel().painter(); break;
			case 3: painter = new CityLevel().painter(); break;
			default: painter = new HallsLevel().painter(); break;
		}
		if (!painter.paint(this, null)) return false;

		scatterRegionDeco(region, entrance);
		sealPockets(entrance);

		ArrayList<Integer> pedestals = new ArrayList<>();
		ArrayList<Integer> candidates = new ArrayList<>();
		for (int cell = 0; cell < length(); cell++) {
			if (map[cell] != Terrain.EMPTY && map[cell] != Terrain.EMPTY_DECO
					&& map[cell] != Terrain.GRASS) continue;
			int dist = distance(entrance, cell);
			if (dist <= 2 || dist == Integer.MAX_VALUE) continue;
			candidates.add(cell);
		}
		Random.shuffle(candidates);
		for (int cell : candidates) {
			boolean separated = true;
			for (int placed : pedestals) {
				if (distance(placed, cell) < 3) {
					separated = false;
					break;
				}
			}
			if (separated) {
				pedestals.add(cell);
				Painter.set(this, cell, Terrain.PEDESTAL);
				if (pedestals.size() == 3) break;
			}
		}
		if (pedestals.size() != 3) return false;

		if (region == 3) layCarpets(pedestals);
		for (Trap trap : traps.valueList()) {
			trap.reveal();
			if (map[trap.pos] == Terrain.SECRET_TRAP) {
				Painter.set(this, trap.pos, Terrain.TRAP);
			}
		}
		return true;
	}

	/** 在 (cx,cy) 角上切掉边长 c 的直角三角形，(dx,dy) 为朝房间内的方向。 */
	private void cutCorner(int cx, int cy, int dx, int dy, int c) {
		for (int i = 0; i < c; i++) {
			for (int j = 0; j < c - i; j++) {
				Painter.set(this, cx + dx * i, cy + dy * j, Terrain.WALL);
			}
		}
	}

	/** 沿墙撒该区域的特色装饰：木桶/铁笼/金属架构/瓦砾/雕像，交互行为继承区域默认规则。 */
	private void scatterRegionDeco(int region, int entrance) {
		ArrayList<Integer> edgeCells = new ArrayList<>();
		for (int cell = 0; cell < length(); cell++) {
			if (map[cell] != Terrain.EMPTY) continue;
			if (distance(entrance, cell) <= 2) continue;
			if (map[cell - 1] == Terrain.WALL || map[cell + 1] == Terrain.WALL
					|| map[cell - width()] == Terrain.WALL || map[cell + width()] == Terrain.WALL) {
				edgeCells.add(cell);
			}
		}
		if (edgeCells.isEmpty()) return;

		if (region == 3) {
			//矮人都市：几尊雕像（可被炸毁，_SP 为特殊地板变体）
			int statues = 3 + Random.Int(4);
			for (int i = 0; i < statues && !edgeCells.isEmpty(); i++) {
				int pos = edgeCells.remove(Random.Int(edgeCells.size()));
				Painter.set(this, pos, Random.Int(3) == 0 ? Terrain.STATUE_SP : Terrain.STATUE);
			}
		} else {
			//下水道木桶 / 监狱铁笼 / 矿洞金属架构 / 恶魔大厅岩石瓦砾，全是 REGION_DECO
			int placed = 0;
			for (int cell : new ArrayList<>(edgeCells)) {
				if (Random.Int(3) == 0) {
					Painter.set(this, cell, Random.Int(3) == 0 ? Terrain.REGION_DECO_ALT : Terrain.REGION_DECO);
					placed++;
				}
			}
			//小房间贴墙空地少时按概率可能一个都出不来，保底铺三个
			while (placed < 3 && !edgeCells.isEmpty()) {
				int pos = edgeCells.remove(Random.Int(edgeCells.size()));
				Painter.set(this, pos, Random.Int(3) == 0 ? Terrain.REGION_DECO_ALT : Terrain.REGION_DECO);
				placed++;
			}
		}
	}

	/** 装饰撒好后把封出来的死角填回墙，保证全层自入口可达。 */
	private void sealPockets(int entrance) {
		boolean[] seen = new boolean[length()];
		ArrayDeque<Integer> queue = new ArrayDeque<>();
		seen[entrance] = true;
		queue.add(entrance);
		while (!queue.isEmpty()) {
			int cur = queue.removeFirst();
			for (int n : new int[]{cur - 1, cur + 1, cur - width(), cur + width()}) {
				if (seen[n]) continue;
				if ((Terrain.flags[map[n]] & Terrain.PASSABLE) == 0) continue;
				seen[n] = true;
				queue.add(n);
			}
		}
		for (int cell = 0; cell < length(); cell++) {
			if (!seen[cell] && (Terrain.flags[map[cell]] & Terrain.PASSABLE) != 0) {
				Painter.set(this, cell, Terrain.WALL);
			}
		}
	}

	/** 都市随机铺 0~2 块小地毯；用真正的 Carpet 图层，可被爆炸/解离摧毁并显示对应区域的破坏过渡图。 */
	private void layCarpets(ArrayList<Integer> pedestals) {
		int carpets = Random.Int(3);
		for (int i = 0; i < carpets; i++) {
			int cw = 2 + Random.Int(2);
			int ch = 3 + Random.Int(3);
			for (int t = 0; t < 20; t++) {
				int x = 2 + Random.Int(width() - 4 - cw);
				int y = 2 + Random.Int(height() - 4 - ch);
				boolean ok = true;
				for (int yy = y; yy < y + ch && ok; yy++) {
					for (int xx = x; xx < x + cw && ok; xx++) {
						int cell = xx + yy * width();
						if (map[cell] != Terrain.EMPTY || pedestals.contains(cell)) ok = false;
					}
				}
				if (ok) {
					Carpet carpet = new Carpet();
					carpet.setRect(x, y, cw, ch);
					customTiles.add(carpet);
					break;
				}
			}
		}
	}

	@Override
	protected void createMobs() {
	}

	@Override
	protected void createItems() {
		for (int cell = 0; cell < length(); cell++) {
			if (map[cell] != Terrain.PEDESTAL) continue;
			Item item;
			switch (Random.Int(4)) {
				case 0:
					//与迷宫隐藏房宝箱一致：武器/护甲、高一档阶数、非诅咒、33% 额外 +1
					if (Random.Int(2) == 0) {
						item = Generator.randomWeapon((Dungeon.depth / 5) + 1, true);
						if (((Weapon) item).hasCurseEnchant()) {
							((Weapon) item).enchant(null);
						}
					} else {
						item = Generator.randomArmor((Dungeon.depth / 5) + 1);
						if (((Armor) item).hasCurseGlyph()) {
							((Armor) item).inscribe(null);
						}
					}
					item.cursed = false;
					item.cursedKnown = true;
					if (Random.Int(3) == 0) {
						item.upgrade();
					}
					break;
				case 1:
					//随机法杖，非诅咒
					item = (Wand) Generator.random(Generator.Category.WAND);
					item.cursed = false;
					item.cursedKnown = true;
					break;
				case 2:
					//随机药剂或卷轴
					item = Generator.random(Random.oneOf(Generator.Category.POTION, Generator.Category.SCROLL));
					break;
				default:
					//与箭术隐藏房一致：按当前楼层阶数随机、默认等级
					item = Generator.randomMissile(true);
					break;
			}
			Heap chest = drop(item, cell);
			chest.type = Heap.Type.CHEST;
			chest.autoExplored = true;
		}
	}

	@Override
	public String tileName(int tile) {
		switch (region()) {
			case 0:
				if (tile == Terrain.REGION_DECO || tile == Terrain.REGION_DECO_ALT
						|| tile == Terrain.SEWER_BARREL_MARKED || tile == Terrain.SEWER_BARREL_MARKED_ALT) {
					return Messages.get(SewerLevel.class, "region_deco_name");
				}
				break;
			case 1:
				if (tile == Terrain.REGION_DECO || tile == Terrain.REGION_DECO_ALT) {
					return Messages.get(PrisonLevel.class, "region_deco_name");
				}
				break;
			case 2:
				if (tile == Terrain.REGION_DECO || tile == Terrain.REGION_DECO_ALT) {
					return Messages.get(CavesLevel.class, "region_deco_name");
				}
				break;
			case 4:
				if (tile == Terrain.REGION_DECO || tile == Terrain.REGION_DECO_ALT) {
					return Messages.get(HallsLevel.class, "region_deco_name");
				}
				break;
		}
		return super.tileName(tile);
	}

	@Override
	public String tileDesc(int tile) {
		switch (region()) {
			case 0:
				if (tile == Terrain.SEWER_BARREL_MARKED || tile == Terrain.SEWER_BARREL_MARKED_ALT) {
					return Messages.get(SewerLevel.class, "marked_barrel_desc");
				}
				if (tile == Terrain.REGION_DECO || tile == Terrain.REGION_DECO_ALT) {
					return Messages.get(SewerLevel.class, "region_deco_desc");
				}
				break;
			case 1:
				if (tile == Terrain.REGION_DECO || tile == Terrain.REGION_DECO_ALT) {
					return Messages.get(PrisonLevel.class, "region_deco_desc");
				}
				break;
			case 2:
				if (tile == Terrain.REGION_DECO || tile == Terrain.REGION_DECO_ALT) {
					return Messages.get(CavesLevel.class, "region_deco_desc");
				}
				break;
			case 3:
				if (tile == Terrain.STATUE || tile == Terrain.STATUE_SP) {
					return Messages.get(CityLevel.class, "statue_desc");
				}
				break;
			case 4:
				if (tile == Terrain.REGION_DECO || tile == Terrain.REGION_DECO_ALT) {
					return Messages.get(HallsLevel.class, "region_deco_desc");
				}
				break;
		}
		return super.tileDesc(tile);
	}

	@Override
	protected int staticEnvironmentalLightRadius(int cell) {
		return map[cell] == Terrain.PEDESTAL ? 1 : super.staticEnvironmentalLightRadius(cell);
	}

	@Override
	public Group addVisuals() {
		super.addVisuals();
		for (int cell = 0; cell < length(); cell++) {
			if (map[cell] == Terrain.PEDESTAL
					|| (Dungeon.depth >= 6 && Dungeon.depth <= 10 && map[cell] == Terrain.WALL_DECO)) {
				PrisonLevel.Torch halo = new PrisonLevel.Torch(cell);
				halo.on = false;
				halo.autoKill = false;
				visuals.add(halo);
			}
		}
		return visuals;
	}

	@Override
	public String tilesTex() {
		switch (region()) {
			case 0: return Assets.Environment.TILES_SEWERS;
			case 1: return Assets.Environment.TILES_PRISON;
			case 2: return Assets.Environment.TILES_CAVES;
			case 3: return Assets.Environment.TILES_CITY;
			default: return Assets.Environment.TILES_HALLS;
		}
	}

	@Override
	public String waterTex() {
		switch (region()) {
			case 0: return Assets.Environment.WATER_SEWERS;
			case 1: return Assets.Environment.WATER_PRISON;
			case 2: return Assets.Environment.WATER_CAVES;
			case 3: return Assets.Environment.WATER_CITY;
			default: return Assets.Environment.WATER_HALLS;
		}
	}
}
