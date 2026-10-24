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

package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SacrificialFire;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SewerSpring;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Web;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.WellWater;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Awareness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ForcedBlindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionEnemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LockedFloor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Light;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSight;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MindVision;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PinCushion;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RevealedArea;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Shadows;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.DivineSense;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Stasis;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GnollGeomancer;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.MobSpawner;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Piranha;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogFist;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.MirrorImage;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Sheep;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.effects.TargetedCell;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlowParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SacrificialParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.WindParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Gem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Stylus;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfChallenge;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfEnchantment;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfIntuition;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.DimensionalSundial;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.EyeOfNewt;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.MossyClump;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.TrapMechanism;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.TrinketCatalyst;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.HeavyBoomerang;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.Chasm;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.Door;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.HighGrass;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ShadowCaster;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.shatteredpixel.shatteredpixeldungeon.tiles.custom.Carpet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameMath;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;
import com.watabou.utils.Rect;
import com.watabou.utils.SparseArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public abstract class Level implements Bundlable {
	
	public static enum Feeling {
		NONE,
		CHASM,
		WATER,
		GRASS,
		DARK,
		LARGE,
		TRAPS,
		SECRETS;

		public String title(){
			return Messages.get(this, name()+"_title");
		}

		public String desc() {
			return Messages.get(this, name()+"_desc");
		}
	}

	protected int width;
	protected int height;
	protected int length;
	
	protected static final float TIME_TO_RESPAWN	= 50;

	public int version;
	
	private CellState[] cells;
	private int[] terrainVisuals;
	private boolean flagsReady;

	/** 只在放置新门时选择默认材质；已有门改变状态不会重新调用工厂。 */
	public Door createDoor(int terrain) { return new Door(terrain); }

	public CellState cellAt(int cell) { return cells[cell]; }
	public int region() { return Dungeon.depth >= 1 ? (Dungeon.depth - 1) / 5 : -1; }
	public int terrainAt(int cell) { return cells[cell].terrain(); }
	public boolean hasTerrain(int cell, int terrain) {
		CellState state = cells[cell];
		if (!state.hasTerrain(terrain)) return false;
		CellContent content = state.content(terrain);
		return CellState.isGround(terrain) || (content != null ? state.exposed(content) : !state.buried());
	}
	/** 只供地图素材拼接使用，不是可写的世界状态。 */
	public int[] terrainVisuals() { return terrainVisuals; }

	public void cellChanged(int cell) {
		int previous = terrainVisuals[cell];
		terrainVisuals[cell] = cells[cell].terrain();
		if (!flagsReady) return;
		updateCellFlags(cell);
		if (sewerSpring != null) sewerSpring.cellChanged(this, cell);
		if (caveCollapse != null) caveCollapse.terrainChanged(cell, previous, terrainVisuals[cell]);
		if (cell == hiddenEntranceCell) {
			if (hiddenEntranceGrass
					&& cells[cell].content(Terrain.GRASS) == null
					&& cells[cell].content(Terrain.HIGH_GRASS) == null
					&& cells[cell].content(Terrain.FURROWED_GRASS) == null) {
				revealHiddenEntrance(cell);
			} else if (hiddenEntranceWater && !cells[cell].hasTerrain(Terrain.WATER)) {
				revealHiddenEntrance(cell);
			}
		}
		if (this == Dungeon.level) GameScene.updateMap(cell);
	}
	public boolean[] visited;
	public boolean[] mapped;
	public boolean[] discoverable;

	public int viewDistance = Dungeon.isChallenged( Challenges.DARKNESS ) ? 2 : 8;
	
	public boolean[] heroFOV;
	
	public boolean[] passable;
	public boolean[] losBlocking;
	public boolean[] flamable;
	public boolean[] secret;
	public boolean[] solid;
	public boolean[] avoid;
	public boolean[] water;
	public boolean[] pit;

	public boolean[] openSpace;

	public TerrainInteractions terrainInteractions;
	public CaveCollapse caveCollapse;
	// 下水道出水口；只在 region 0 的楼层存在，其余楼层保持 null
	public SewerSpring sewerSpring;

	/** 只在生成器或测试场景显式配置交互时创建。运行时查询应直接检查可空字段。 */
	public TerrainInteractions interactions() {
		if (terrainInteractions == null) terrainInteractions = new TerrainInteractions(this);
		return terrainInteractions;
	}

	/** 作用只指定要作用的内容属性，格子内符合属性的内容自行结算。 */
	public boolean affectTerrain(int cell, int property) {
		return interactions().interact(cell, property) == TerrainInteractions.Result.CHANGED;
	}

	/**
	 * 无副作用查询：该格是否属于“不遮挡视野且可被解离摧毁”的地形。
	 * 判定与 affectTerrain(cell, DISINTEGRATION) 的破坏结果一致，但不会改动地图。
	 */
	public boolean passthroughDisintegrationRay(int cell) {
		return insideMap(cell) && !losBlocking[cell]
				&& cellAt(cell).has(CellContent.BREAKABLE | CellContent.BURNABLE
						| CellContent.DISINTEGRATION_BREAKABLE);
	}
	
	public Feeling feeling = Feeling.NONE;
	
	public int entrance;
	public int exit;

	public ArrayList<LevelTransition> transitions;
	public int hiddenEntranceCell = -1;
	public boolean hiddenEntranceGrass;
	public boolean hiddenEntranceWater;

	//when a boss level has become locked.
	public boolean locked = false;
	
	public HashSet<Mob> mobs;
	public SparseArray<Heap> heaps;
	public HashMap<Class<? extends Blob>,Blob> blobs;
	public SparseArray<Plant> plants;
	public SparseArray<Trap> traps;

	//outdated design challenge: counts traps placed on this level, so that every second one
	//needs a second trigger to take effect. Starts at a random phase each level
	public int outdatedTrapCounter = Random.Int(2);
	public ArrayList<CustomTilemap> customTiles;
	public ArrayList<CustomTilemap> customTerrain;
	public ArrayList<CustomTilemap> customWalls;
	
	protected ArrayList<Item> itemsToSpawn = new ArrayList<>();

	protected Group visuals;
	protected Group wallVisuals;
	
	public int color1 = 0x004400;
	public int color2 = 0x88CC44;

	private static final String VERSION     = "version";
	private static final String WIDTH       = "width";
	private static final String HEIGHT      = "height";
	private static final String CELLS = "cell_composition";
	private static final String VISITED		= "visited";
	private static final String MAPPED		= "mapped";
	private static final String TRANSITIONS	= "transitions";
	private static final String HIDDEN_ENTRANCE = "hidden_entrance";
	private static final String HIDDEN_ENTRANCE_GRASS = "hidden_entrance_grass";
	private static final String HIDDEN_ENTRANCE_WATER = "hidden_entrance_water";
	private static final String LOCKED      = "locked";
	private static final String HEAPS		= "heaps";
	private static final String PLANTS		= "plants";
	private static final String TRAPS       = "traps";
	private static final String CUSTOM_TILES= "customTiles";
	private static final String CUSTOM_TERRAIN= "customTerrain";
	private static final String CUSTOM_WALLS= "customWalls";
	private static final String MOBS		= "mobs";
	private static final String BLOBS		= "blobs";
	private static final String FEELING		= "feeling";

	public void create() {

		TargetedCell.cells.clear();
		Random.pushGenerator( Dungeon.seedCurDepth() );

		//TODO maybe just make this part of RegularLevel?
		if (!Dungeon.bossLevel() && Dungeon.branch == 0) {

			addItemToSpawn(Generator.random(Generator.Category.FOOD));

			if (Dungeon.posNeeded()) {
				Dungeon.LimitedDrops.STRENGTH_POTIONS.count++;
				addItemToSpawn( new PotionOfStrength() );
			}
			if (Dungeon.souNeeded()) {
				Dungeon.LimitedDrops.UPGRADE_SCROLLS.count++;
				//every 2nd scroll of upgrade is removed with forbidden runes challenge on
				//TODO while this does significantly reduce this challenge's levelgen impact, it doesn't quite remove it
				//for 0 levelgen impact, we need to do something like give the player all SOU, but nerf them
				//or give a random scroll (from a separate RNG) instead of every 2nd SOU
				if (!Dungeon.isChallenged(Challenges.NO_SCROLLS) || Dungeon.LimitedDrops.UPGRADE_SCROLLS.count%2 != 0){
					addItemToSpawn(new ScrollOfUpgrade());
				}
			}
			if (Dungeon.asNeeded()) {
				Dungeon.LimitedDrops.ARCANE_STYLI.count++;
				addItemToSpawn( new Stylus() );
			}
			if ( Dungeon.enchStoneNeeded() ){
				Dungeon.LimitedDrops.ENCH_STONE.drop();
				addItemToSpawn( new StoneOfEnchantment() );
			}
			if ( Dungeon.intStoneNeeded() ){
				Dungeon.LimitedDrops.INT_STONE.drop();
				addItemToSpawn( new StoneOfIntuition() );
			}
			if ( Dungeon.trinketCataNeeded() ){
				Dungeon.LimitedDrops.TRINKET_CATA.drop();
				addItemToSpawn( new TrinketCatalyst());
			}
			
			if (Dungeon.depth > 1) {
				// 洞穴 11～14 层不生成悬空类型，重抽仍使用本层种子的随机序列。
				int feelingRoll;
				do {
					feelingRoll = Random.Int(14);
				} while (feelingRoll == 0 && Dungeon.depth >= 11 && Dungeon.depth <= 14);
				switch (feelingRoll) {
					case 0:
						feeling = Feeling.CHASM;
						break;
					case 1:
						feeling = Feeling.WATER;
						break;
					case 2:
						feeling = Feeling.GRASS;
						break;
					case 3:
						feeling = Feeling.DARK;
						viewDistance = Math.round(5*viewDistance/8f);
						break;
					case 4:
						feeling = Feeling.LARGE;
						addItemToSpawn(Generator.random(Generator.Category.FOOD));
						break;
					case 5:
						feeling = Feeling.TRAPS;
						break;
					case 6:
						feeling = Feeling.SECRETS;
						break;
					default:
						//if-else statements are fine here as only one chance can be above 0 at a time
						// we pre-generate the floats to ensure Random is called consistently
						float mossyChance = Random.Float();
						float trapMechChance = Random.Float();
						if (mossyChance < MossyClump.overrideNormalLevelChance()){
							feeling = MossyClump.getNextFeeling();
						} else if (trapMechChance < TrapMechanism.overrideNormalLevelChance()) {
							feeling = TrapMechanism.getNextFeeling();
						} else {
							feeling = Feeling.NONE;
						}
				}
			}
		}
		
		do {
			width = height = length = 0;

			transitions = new ArrayList<>();

			mobs = new HashSet<>();
			heaps = new SparseArray<>();
			blobs = new HashMap<>();
			plants = new SparseArray<>();
			traps = new SparseArray<>();
			customTiles = new ArrayList<>();
			customTerrain = new ArrayList<>();
			customWalls = new ArrayList<>();
			
		} while (!build());
		//区域默认环境：按深度区域而不是具体关卡类配置，同区域的任何楼层
		//（主线、Boss、任务支线、隐藏楼层等）都自动获得同一套装饰交互行为。
		int region = Dungeon.depth >= 1 ? (Dungeon.depth - 1) / 5 : -1;
		if (region == 0) {
			for (int i = 0; i < length(); i++) {
				if (hasTerrain(i, Terrain.REGION_DECO) && Random.Int(10) == 0) {
					cellAt(i).place(Terrain.SEWER_BARREL_MARKED);
				} else if (hasTerrain(i, Terrain.REGION_DECO_ALT) && Random.Int(10) == 0) {
					cellAt(i).place(Terrain.SEWER_BARREL_MARKED_ALT);
				}
			}
			// 水已经铺完，把每处出水口相连的水体登记下来
			SewerSpring.create(this);
		}
		
		for (CustomTilemap tile : customTiles) if (tile instanceof Carpet) ((Carpet)tile).registerContents(this);
		buildFlagMaps();
		interactions();
		cleanWalls();
		
		createMobs();
		createItems();
		for (Heap heap : heaps.valueList()) {
			if (heap.type == Heap.Type.CHEST || heap.type == Heap.Type.LOCKED_CHEST
					|| heap.type == Heap.Type.CRYSTAL_CHEST) {
				heap.items.add(Gem.generateTreasure());
			}
		}

		Random.popGenerator();
	}
	
	public void setSize(int w, int h){
		terrainInteractions = null;
		flagsReady = false;
		caveCollapse = null;
		sewerSpring = null;
		
		width = w;
		height = h;
		length = w * h;
		
		cells = new CellState[length];
		terrainVisuals = new int[length];
		for (int cell = 0; cell < length; cell++) {
			cells[cell] = new CellState(this, cell);
			cells[cell].setGround(feeling == Level.Feeling.CHASM ? Terrain.CHASM : Terrain.EMPTY);
			if (feeling != Level.Feeling.CHASM) cells[cell].place(Terrain.WALL);
		}
		
		visited     = new boolean[length];
		mapped      = new boolean[length];
		
		heroFOV     = new boolean[length];
		heroFOVBuffer = heroMindFov = null;
		lighting = null;
		fieldOfViewChanges.setEmpty();
		
		passable	= new boolean[length];
		losBlocking	= new boolean[length];
		flamable	= new boolean[length];
		secret		= new boolean[length];
		solid		= new boolean[length];
		avoid		= new boolean[length];
		water		= new boolean[length];
		pit			= new boolean[length];

		openSpace   = new boolean[length];
		
		PathFinder.setMapSize(w, h);
	}
	
	public void reset() {
		
		for (Mob mob : mobs.toArray( new Mob[0] )) {
			if (!mob.reset()) {
				mobs.remove( mob );
			}
		}
		createMobs();
	}

	public void playLevelMusic(){
		//do nothing by default
	}
	
	@Override
	public void restoreFromBundle( Bundle bundle ) {

		version = bundle.getInt( VERSION );
		
		//saves from before v3.1.1 are not supported
		if (version < ShatteredPixelDungeon.v3_1_1){
			throw new RuntimeException("old save");
		}

		setSize( bundle.getInt(WIDTH), bundle.getInt(HEIGHT));
		
		mobs = new HashSet<>();
		heaps = new SparseArray<>();
		blobs = new HashMap<>();
		plants = new SparseArray<>();
		traps = new SparseArray<>();
		customTiles = new ArrayList<>();
		customTerrain = new ArrayList<>();
		customWalls = new ArrayList<>();
		
		if (!bundle.contains(CELLS)) throw new RuntimeException("old save");
		int cellIndex = 0;
		for (Bundlable saved : bundle.getCollection(CELLS)) {
			if (cellIndex >= length) throw new IllegalArgumentException("格子存档尺寸不符");
			cells[cellIndex] = (CellState)saved;
			cells[cellIndex].bind(this, cellIndex);
			terrainVisuals[cellIndex] = cells[cellIndex].terrain();
			cellIndex++;
		}
		if (cellIndex != length) throw new IllegalArgumentException("格子存档尺寸不符");
		hiddenEntranceCell = bundle.contains(HIDDEN_ENTRANCE) ? bundle.getInt(HIDDEN_ENTRANCE) : -1;
		hiddenEntranceGrass = bundle.getBoolean(HIDDEN_ENTRANCE_GRASS);
		hiddenEntranceWater = bundle.getBoolean(HIDDEN_ENTRANCE_WATER);
		interactions();

		visited	= bundle.getBooleanArray( VISITED );
		mapped	= bundle.getBooleanArray( MAPPED );

		transitions = new ArrayList<>();
		for (Bundlable b : bundle.getCollection( TRANSITIONS )){
			transitions.add((LevelTransition) b);
		}

		locked      = bundle.getBoolean( LOCKED );
		
		Collection<Bundlable> collection = bundle.getCollection( HEAPS );
		for (Bundlable h : collection) {
			Heap heap = (Heap)h;
			if (!heap.isEmpty())
				heaps.put( heap.pos, heap );
		}
		
		collection = bundle.getCollection( PLANTS );
		for (Bundlable p : collection) {
			Plant plant = (Plant)p;
			plants.put( plant.pos, plant );
		}

		collection = bundle.getCollection( TRAPS );
		for (Bundlable p : collection) {
			Trap trap = (Trap)p;
			traps.put( trap.pos, trap );
		}

		collection = bundle.getCollection( CUSTOM_TILES );
		for (Bundlable p : collection) {
			CustomTilemap vis = (CustomTilemap)p;
			customTiles.add(vis);
		}

		collection = bundle.getCollection( CUSTOM_TERRAIN );
		for (Bundlable p : collection) {
			CustomTilemap vis = (CustomTilemap)p;
			customTerrain.add(vis);
		}

		collection = bundle.getCollection( CUSTOM_WALLS );
		for (Bundlable p : collection) {
			CustomTilemap vis = (CustomTilemap)p;
			customWalls.add(vis);
		}
		
		collection = bundle.getCollection( MOBS );
		for (Bundlable m : collection) {
			Mob mob = (Mob)m;
			if (mob != null) {
				mobs.add( mob );
			}
		}
		
		collection = bundle.getCollection( BLOBS );
		for (Bundlable b : collection) {
			Blob blob = (Blob)b;
			blobs.put( blob.getClass(), blob );
		}

		feeling = bundle.getEnum( FEELING, Feeling.class );
		if (feeling == Feeling.DARK) {
			viewDistance = Math.round(5 * viewDistance / 8f);
		}

		if (bundle.contains( "mobs_to_spawn" )) {
			for (Class<? extends Mob> mob : bundle.getClassArray("mobs_to_spawn")) {
				if (mob != null) mobsToSpawn.add(mob);
			}
		}

		if (bundle.contains( "respawner" )){
			respawner = (MobSpawner) bundle.get("respawner");
		}

		TargetedCell.cells.clear();
		if (bundle.contains( "targeted_cells" )){
			collection = bundle.getCollection( "targeted_cells" );
			for (Bundlable c : collection) {
				TargetedCell cell = (TargetedCell)c;
				if (cell != null) {
					TargetedCell.cells.put(cell.pos, cell);
				}
			}
		}

		buildFlagMaps();
		cleanWalls();
		sewerSpring = (SewerSpring) blobs.get(SewerSpring.class);
		if (bundle.contains("cave_collapse")) {
			caveCollapse = new CaveCollapse(this);
			caveCollapse.restoreFromBundle(bundle.getBundle("cave_collapse"));
		}

	}
	
	@Override
	public void storeInBundle( Bundle bundle ) {
		bundle.put( VERSION, Game.versionCode );
		bundle.put( WIDTH, width );
		bundle.put( HEIGHT, height );
		bundle.put(CELLS, Arrays.asList(cells));
		if (caveCollapse != null) {
			Bundle regions = new Bundle();
			caveCollapse.storeInBundle(regions);
			bundle.put("cave_collapse", regions);
		}
		bundle.put( HIDDEN_ENTRANCE, hiddenEntranceCell );
		bundle.put( HIDDEN_ENTRANCE_GRASS, hiddenEntranceGrass );
		bundle.put( HIDDEN_ENTRANCE_WATER, hiddenEntranceWater );
		bundle.put( VISITED, visited );
		bundle.put( MAPPED, mapped );
		bundle.put( TRANSITIONS, transitions );
		bundle.put( LOCKED, locked );
		bundle.put( HEAPS, heaps.valueList() );
		bundle.put( PLANTS, plants.valueList() );
		bundle.put( TRAPS, traps.valueList() );
		bundle.put( CUSTOM_TILES, customTiles );
		bundle.put( CUSTOM_TERRAIN, customTerrain);
		bundle.put( CUSTOM_WALLS, customWalls );
		bundle.put( MOBS, mobs );
		bundle.put( BLOBS, blobs.values() );
		bundle.put( FEELING, feeling );
		bundle.put( "mobs_to_spawn", mobsToSpawn.toArray(new Class[0]));
		bundle.put( "respawner", respawner );
		bundle.put( "targeted_cells", TargetedCell.cells.valueList() );
	}
	
	public int tunnelTile() {
		return feeling == Feeling.CHASM ? Terrain.EMPTY_SP : Terrain.EMPTY;
	}

	public int width() {
		return width;
	}

	public int height() {
		return height;
	}

	public int length() {
		return length;
	}
	
	public String tilesTex() {
		return null;
	}
	
	public String waterTex() {
		return null;
	}
	
	abstract protected boolean build();
	
	private ArrayList<Class<?extends Mob>> mobsToSpawn = new ArrayList<>();
	
	public Mob createMob() {
		if (mobsToSpawn == null || mobsToSpawn.isEmpty()) {
			mobsToSpawn = MobSpawner.getMobRotation(Dungeon.depth);
		}

		Mob m = Reflection.newInstance(mobsToSpawn.remove(0));
		ChampionEnemy.rollForChampion(m);
		return m;
	}

	abstract protected void createMobs();

	abstract protected void createItems();

	public int entrance(){
		LevelTransition l = getTransition(null);
		if (l != null){
			return l.cell();
		}
		return 0;
	}

	public int exit(){
		LevelTransition l = getTransition(LevelTransition.Type.REGULAR_EXIT);
		if (l != null){
			return l.cell();
		}
		return 0;
	}

	public LevelTransition getTransition(LevelTransition.Type type){
		if (transitions.isEmpty()){
			return null;
		}
		for (LevelTransition transition : transitions){
			//if we don't specify a type, prefer to return any entrance
			if (type == null &&
					(transition.type == LevelTransition.Type.REGULAR_ENTRANCE
							|| transition.type == LevelTransition.Type.BRANCH_ENTRANCE
							|| transition.type == LevelTransition.Type.SURFACE)){
				return transition;
			} else if (transition.type == type){
				return transition;
			}
		}
		return type != null ? getTransition(null) : transitions.get(0);
	}

	public LevelTransition getTransition(int cell){
		for (LevelTransition transition : transitions){
			// This level may still be generating, before Dungeon.level is assigned.
			if (transition.inside(cellToPoint(cell))){
				return transition;
			}
		}
		return null;
	}

	//returns true if we immediately transition, false otherwise
	public boolean activateTransition(Hero hero, LevelTransition transition){
		if (locked){
			return false;
		}

		beforeTransition();
		InterlevelScene.curTransition = transition;
		if (transition.type == LevelTransition.Type.REGULAR_EXIT
				|| transition.type == LevelTransition.Type.BRANCH_EXIT) {
			InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
		} else {
			InterlevelScene.mode = InterlevelScene.Mode.ASCEND;
		}
		Game.switchScene(InterlevelScene.class);
		return true;
	}

	//some buff effects have special logic or are cancelled from the hero before transitioning levels
	public static void beforeTransition(){

		//time freeze effects need to resolve their pressed cells before transitioning
		TimekeepersHourglass.timeFreeze timeFreeze = Dungeon.hero.buff(TimekeepersHourglass.timeFreeze.class);
		if (timeFreeze != null) timeFreeze.disarmPresses();
		Swiftthistle.TimeBubble timeBubble = Dungeon.hero.buff(Swiftthistle.TimeBubble.class);
		if (timeBubble != null) timeBubble.disarmPresses();

		//iron stomach and challenge arena do not persist between floors
		Talent.WarriorFoodImmunity foodImmune = Dungeon.hero.buff(Talent.WarriorFoodImmunity.class);
		if (foodImmune != null) foodImmune.detach();
		ScrollOfChallenge.ChallengeArena arena = Dungeon.hero.buff(ScrollOfChallenge.ChallengeArena.class);
		if (arena != null) arena.detach();
		//awareness also doesn't, honestly it's weird that it's a buff
		Awareness awareness = Dungeon.hero.buff(Awareness.class);
		if (awareness != null) awareness.detach();

		Char ally = Stasis.getStasisAlly();
		if (Char.hasProp(ally, Char.Property.IMMOVABLE)){
			Dungeon.hero.buff(Stasis.StasisBuff.class).act();
			GLog.w(Messages.get(Stasis.StasisBuff.class, "left_behind"));
		}

		//spend the hero's partial turns,  so the hero cannot take partial turns between floors
		Dungeon.hero.spendToWhole();
		for (Actor a : Actor.all()){
			//also adjust any other actors that are now ahead of the hero due to this
			if (a.cooldown() < Dungeon.hero.cooldown()){
				a.spendToWhole();
			}
		}
	}

	public void seal(){
		if (!locked) {
			locked = true;
			Buff.affect(Dungeon.hero, LockedFloor.class);
		}
	}

	public void unseal(){
		if (locked) {
			locked = false;
			if (Dungeon.hero.buff(LockedFloor.class) != null){
				Dungeon.hero.buff(LockedFloor.class).detach();
			}
		}
	}

	public ArrayList<Item> getItemsToPreserveFromSealedResurrect(){
		ArrayList<Item> items = new ArrayList<>();
		for (Heap h : heaps.valueList()){
			if (h.type == Heap.Type.HEAP) {
				for (Item i : h.items){
					if (i instanceof Bomb){
						((Bomb) i).fuse = null;
					}
					items.add(i);
				}
			}
		}
		for (Mob m : mobs){
			for (PinCushion b : m.buffs(PinCushion.class)){
				items.addAll(b.getStuckItems());
			}
		}
		for (HeavyBoomerang.CircleBack b : Dungeon.hero.buffs(HeavyBoomerang.CircleBack.class)){
			if (b.activeDepth() == Dungeon.depth) items.add(b.cancel());
		}
		return items;
	}

	public Group addVisuals() {
		if (visuals == null || visuals.parent == null){
			visuals = new Group();
		} else {
			visuals.clear();
			visuals.camera = null;
		}
		for (int i=0; i < length(); i++) {
			if (pit[i]) {
				visuals.add( new WindParticle.Wind( i ) );
				if (i >= width() && water[i-width()]) {
					visuals.add( new FlowParticle.Flow( i - width() ) );
				}
			}
		}
		return visuals;
	}

	//for visual effects that should render above wall overhang tiles
	public Group addWallVisuals(){
		if (wallVisuals == null || wallVisuals.parent == null){
			wallVisuals = new Group();
		} else {
			wallVisuals.clear();
			wallVisuals.camera = null;
		}
		if (caveCollapse != null && Terrain.COLLAPSE_PARTICLE_INTERVAL > 0) {
			for (int i = 0; i < length(); i++) {
				if (caveCollapse.contains(i)) {
					WindParticle.Wind wind = new WindParticle.Wind(i);
					wind.pour(WindParticle.FACTORY, Terrain.COLLAPSE_PARTICLE_INTERVAL);
					wallVisuals.add(wind);
				}
			}
		}
		return wallVisuals;
	}

	
	public int mobLimit() {
		return 0;
	}

	public int mobCount(){
		float count = 0;
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])){
			if (mob.alignment == Char.Alignment.ENEMY && !mob.properties().contains(Char.Property.MINIBOSS)) {
				count += mob.spawningWeight();
			}
		}
		return Math.round(count);
	}

	public Mob findMob( int pos ){
		for (Mob mob : mobs){
			if (mob.pos == pos){
				return mob;
			}
		}
		return null;
	}

	private MobSpawner respawner;

	public Actor addRespawner() {
		if (respawner == null){
			respawner = new MobSpawner();
			Actor.addDelayed(respawner, respawnCooldown());
		} else {
			Actor.add(respawner);
			if (respawner.cooldown() > respawnCooldown()){
				respawner.resetCooldown();
			}
		}
		return respawner;
	}

	public float respawnCooldown(){
		float cooldown;
		if (Statistics.amuletObtained){
			if (Dungeon.depth == 1){
				//very fast spawns on floor 1! 0/2/4/6/8/10/12, etc.
				cooldown = (Dungeon.level.mobCount()) * (TIME_TO_RESPAWN / 25f);
			} else {
				//respawn time is 5/5/10/15/20/25/25, etc.
				cooldown = Math.round(GameMath.gate( TIME_TO_RESPAWN/10f, Dungeon.level.mobCount() * (TIME_TO_RESPAWN / 10f), TIME_TO_RESPAWN / 2f));
			}
		} else if (Dungeon.level.feeling == Feeling.DARK){
			cooldown = 2*TIME_TO_RESPAWN/3f;
		} else {
			cooldown = TIME_TO_RESPAWN;
		}
		return cooldown / DimensionalSundial.spawnMultiplierAtCurrentTime();
	}

	public boolean spawnMob(int disLimit){
		PathFinder.buildDistanceMap(Dungeon.hero.pos, BArray.or(passable, avoid, null));

		Mob mob = createMob();
		if (mob.state != mob.PASSIVE) {
			mob.state = mob.WANDERING;
		}
		int tries = 30;
		do {
			mob.pos = randomRespawnCell(mob);
			tries--;
		} while ((mob.pos == -1 || PathFinder.distance[mob.pos] < disLimit) && tries > 0);

		if (Dungeon.hero.isAlive() && mob.pos != -1 && PathFinder.distance[mob.pos] >= disLimit) {
			GameScene.add( mob );
			if (!mob.buffs(ChampionEnemy.class).isEmpty()){
				GLog.w(Messages.get(ChampionEnemy.class, "warn"));
			}
			return true;
		} else {
			return false;
		}
	}
	
	public int randomRespawnCell( Char ch ) {
		int cell;
		int count = 0;
		do {

			if (++count > 30) {
				return -1;
			}

			cell = Random.Int( length() );

		} while ((Dungeon.level == this && heroFOV[cell])
				|| !passable[cell]
				|| (Char.hasProp(ch, Char.Property.LARGE) && !openSpace[cell])
				|| Actor.findChar( cell ) != null);
		return cell;
	}
	
	public int randomDestination( Char ch ) {
		int cell;
		do {
			cell = Random.Int( length() );
		} while (!passable[cell]
				|| (Char.hasProp(ch, Char.Property.LARGE) && !openSpace[cell]));
		return cell;
	}
	
	public void addItemToSpawn( Item item ) {
		if (item != null) {
			itemsToSpawn.add( item );
		}
	}

	public Item findPrizeItem(){ return findPrizeItem(null); }

	public Item findPrizeItem(Class<?extends Item> match){
		if (itemsToSpawn.size() == 0)
			return null;

		if (match == null){
			//if we have a trinket catalyst, always return that first
			for (Item item : itemsToSpawn){
				if (item instanceof TrinketCatalyst){
					itemsToSpawn.remove(item);
					return item;
				}
			}

			Item item = Random.element(itemsToSpawn);
			itemsToSpawn.remove(item);
			return item;
		}

		for (Item item : itemsToSpawn){
			if (match.isInstance(item)){
				itemsToSpawn.remove( item );
				return item;
			}
		}

		return null;
	}

	public void buildFlagMaps() {

		// 整体重建也可能绕过 Level.set，照明与遮挡缓存随标记一起重建。
		lighting = null;
		flagsReady = true;

		for (int i=0; i < length(); i++) {
			terrainVisuals[i] = cellAt(i).terrain();
			int flags = cellAt(i).flags();
			passable[i]     = (flags & Terrain.PASSABLE) != 0;
			losBlocking[i]  = (flags & Terrain.LOS_BLOCKING) != 0;
			flamable[i]     = (flags & Terrain.FLAMABLE) != 0;
			secret[i]       = (flags & Terrain.SECRET) != 0;
			solid[i]        = (flags & Terrain.SOLID) != 0;
			avoid[i]        = (flags & Terrain.AVOID) != 0;
			water[i]        = (flags & Terrain.LIQUID) != 0;
			pit[i]          = (flags & Terrain.PIT) != 0;
		}

		for (Blob b : blobs.values()){
			b.onBuildFlagMaps(this);
		}

		int lastRow = length() - width();
		for (int i=0; i < width(); i++) {
			passable[i] = avoid[i] = false;
			losBlocking[i] = solid[i] = true;
			passable[lastRow + i] = avoid[lastRow + i] = false;
			losBlocking[lastRow + i] = solid[lastRow + i] = true;
		}
		for (int i=width(); i < lastRow; i += width()) {
			passable[i] = avoid[i] = false;
			losBlocking[i] = solid[i] = true;
			passable[i + width()-1] = avoid[i + width()-1] = false;
			losBlocking[i + width()-1] = solid[i + width()-1] = true;
		}

		//an open space is large enough to fit large mobs. A space is open when it is not solid
		// and there is an open corner with both adjacent cells opens
		for (int i=0; i < length(); i++) {
			if (solid[i]){
				openSpace[i] = false;
			} else {
				for (int j = 1; j < PathFinder.CIRCLE8.length; j += 2){
					if (solid[i+PathFinder.CIRCLE8[j]]) {
						openSpace[i] = false;
					} else if (!solid[i+PathFinder.CIRCLE8[(j+1)%8]]
							&& !solid[i+PathFinder.CIRCLE8[(j+2)%8]]){
						openSpace[i] = true;
						break;
					}
				}
			}
		}

	}

	//updates open space both on the cell itself and adjacent cells
	public void updateOpenSpace(int cell){
		for (int i : PathFinder.NEIGHBOURS9) {
			if (solid[cell+i]){
				openSpace[cell+i] = false;
			} else {
				for (int j = 1; j < PathFinder.CIRCLE8.length; j += 2){
					if (solid[cell+i+PathFinder.CIRCLE8[j]]) {
						openSpace[cell+i] = false;
					} else if (!solid[cell+i+PathFinder.CIRCLE8[(j+1)%8]]
							&& !solid[cell+i+PathFinder.CIRCLE8[(j+2)%8]]){
						openSpace[cell+i] = true;
						break;
					}
				}
			}
		}
	}

	public void destroy( int pos ) {
		cellAt(pos).affect(CellContent.BREAKABLE | CellContent.BURNABLE, true);
		Blob web = blobs.get(Web.class);
		if (web != null) web.clear(pos);
	}

	public void cleanWalls() {
		if (discoverable == null || discoverable.length != length) {
			discoverable = new boolean[length()];
		}

		for (int i=0; i < length(); i++) {
			
			boolean d = false;
			
			for (int j=0; j < PathFinder.NEIGHBOURS9.length; j++) {
				int n = i + PathFinder.NEIGHBOURS9[j];
				if (n >= 0 && n < length() && !hasTerrain(n, Terrain.WALL) && !hasTerrain(n, Terrain.WALL_DECO)) {
					d = true;
					break;
				}
			}
			
			discoverable[i] = d;
		}
	}
	
	public static void set( int cell, int terrain ){
		set( cell, terrain, Dungeon.level );
	}
	
	public static void set( int cell, int terrain, Level level ) {
		level.cellAt(cell).place(terrain);
	}

	public void revealHiddenEntrance(int cell) {
		if (cell != hiddenEntranceCell) return;
		hiddenEntranceCell = -1;
		hiddenEntranceGrass = false;
		hiddenEntranceWater = false;
		// 楼梯口不保留水面、草地和灰烬
		CellState state = cellAt(cell);
		state.removeTerrain(Terrain.EMBERS);
		state.removeTerrain(Terrain.WATER);
		state.removeTerrain(Terrain.GRASS);
		state.removeTerrain(Terrain.HIGH_GRASS);
		state.removeTerrain(Terrain.FURROWED_GRASS);
		Level.set(cell, Terrain.EXIT, this);
		transitions.add(new LevelTransition(this, cell, LevelTransition.Type.BRANCH_EXIT,
				Dungeon.depth, 2, LevelTransition.Type.BRANCH_ENTRANCE));
		GameScene.updateMap(cell);
		if (Dungeon.level == this && Dungeon.hero != null) Dungeon.observe();
	}

	public void updateCellFlags( int cell ){
		int terrain = terrainAt(cell);

		int flags = cellAt(cell).flags();
		passable[cell]      = (flags & Terrain.PASSABLE) != 0;
		losBlocking[cell]   = (flags & Terrain.LOS_BLOCKING) != 0;
		flamable[cell]      = (flags & Terrain.FLAMABLE) != 0;
		secret[cell]        = (flags & Terrain.SECRET) != 0;
		solid[cell]         = (flags & Terrain.SOLID) != 0;
		avoid[cell]         = (flags & Terrain.AVOID) != 0;
		pit[cell]           = (flags & Terrain.PIT) != 0;
		water[cell]         = (flags & Terrain.LIQUID) != 0;

		for (Blob b : blobs.values()){
			b.onUpdateCellFlags(this, cell);
		}

		if (insideMap(cell)) updateOpenSpace(cell);

		if (lighting != null) lighting.terrainChanged(cell);
	}
	
	public Heap drop( Item item, int cell ) {

		if (item == null || Challenges.isItemBlocked(item)){

			//create a dummy heap, give it a dummy sprite, don't add it to the game, and return it.
			//effectively nullifies whatever the logic calling this wants to do, including dropping items.
			Heap heap = new Heap();
			ItemSprite sprite = heap.sprite = new ItemSprite();
			sprite.link(heap);
			return heap;

		}
		
		Heap heap = heaps.get( cell );
		if (heap != null && (heap.type == Heap.Type.CHEST
				|| heap.type == Heap.Type.LOCKED_CHEST || heap.type == Heap.Type.CRYSTAL_CHEST)) {
			//Search a finite set, rather than bouncing recursively between containers.
			//Only cross other containers; never jump over a trap or a chasm to find safety.
			ArrayList<Integer> containers = new ArrayList<>();
			boolean[] checked = new boolean[length];
			containers.add(cell);
			checked[cell] = true;
			ArrayList<Integer> candidates = new ArrayList<>();
			int bestRisk = Integer.MAX_VALUE;
			for (int i = 0; i < containers.size(); i++) {
				int origin = containers.get(i);
				for (int offset : PathFinder.NEIGHBOURS8) {
					int next = origin + offset;
					if (next < 0 || next >= length || !adjacent(origin, next) || checked[next]) continue;
					checked[next] = true;
					//Room painters can drop items before buildFlagMaps has run.
					boolean canLand = Dungeon.level == this ? passable[next] || avoid[next]
							: (cellAt(next).flags() & (Terrain.PASSABLE | Terrain.AVOID)) != 0;
					if (!canLand) continue;
					Heap other = heaps.get(next);
					if (other != null && other.type != Heap.Type.HEAP) {
						containers.add(next);
						continue;
					}
					int risk = pit[next] || terrainAt(next) == Terrain.CHASM ? 2
							: traps.get(next) != null && traps.get(next).active ? 1 : 0;
					if (risk < bestRisk) {
						candidates.clear();
						bestRisk = risk;
					}
					if (risk == bestRisk) candidates.add(next);
				}
				//Prefer an adjacent landing over crossing another container.
				if (!candidates.isEmpty()) break;
			}
			if (candidates.isEmpty()) {
				//An entirely walled-in cluster has no physical landing. Keep the item
				//on the nearest available ground instead of losing it or looping forever.
				int nearest = Integer.MAX_VALUE;
				for (int next = 0; next < length; next++) {
					Heap other = heaps.get(next);
					boolean canLand = Dungeon.level == this ? passable[next] || avoid[next]
							: (cellAt(next).flags() & (Terrain.PASSABLE | Terrain.AVOID)) != 0;
					if (!canLand || pit[next] || terrainAt(next) == Terrain.CHASM
							|| (other != null && other.type != Heap.Type.HEAP)) continue;
					int distance = distance(cell, next);
					if (distance < nearest) {
						candidates.clear();
						nearest = distance;
					}
					if (distance == nearest) candidates.add(next);
				}
			}
			cell = Random.element(candidates);
			heap = heaps.get(cell);
		}
		if (heap == null) {
			
			heap = new Heap();
			heap.seen = Dungeon.level == this && heroFOV[cell];
			heap.pos = cell;
			heap.drop(item);
			if (terrainAt(cell) == Terrain.CHASM || (Dungeon.level != null && pit[cell])) {
				Dungeon.dropToChasm( item );
				GameScene.discard( heap );
			} else {
				heaps.put( cell, heap );
				GameScene.add( heap );
			}
			
		} else {
			heap.drop(item);
		}
		
		if (Dungeon.level != null && ShatteredPixelDungeon.scene() instanceof GameScene) {
			pressCell( cell );
		}
		
		return heap;
	}
	
	public Plant plant( Plant.Seed seed, int pos ) {

		Plant plant = plants.get( pos );
		if (plant != null) {
			plant.wither();
		}

		if (hasTerrain(pos, Terrain.HIGH_GRASS) ||
				hasTerrain(pos, Terrain.FURROWED_GRASS) ||
				terrainAt(pos) == Terrain.EMPTY ||
				hasTerrain(pos, Terrain.EMBERS) ||
				terrainAt(pos) == Terrain.EMPTY_DECO) {
			set(pos, Terrain.GRASS, this);
			GameScene.updateMap(pos);
		}

		//we have to get this far as grass placement has RNG implications in levelgen
		//barren land challenge: only the plants that have a seed of their own are removed. The seed
		//pool is the list of seeds which can actually be found in the dungeon, so the special plants
		//which never drop a seed (blandfruit bush, dewcatcher, seedpod) are still generated
		if (Dungeon.isChallenged(Challenges.NO_HERBALISM)
				&& Arrays.asList(Generator.Category.SEED.classes).contains(seed.getClass())){
			return null;
		}
		
		plant = seed.couch( pos, this );
		plants.put( pos, plant );
		
		GameScene.plantSeed( pos );

		for (Char ch : Actor.chars()){
			if (ch instanceof WandOfRegrowth.Lotus
					&& ((WandOfRegrowth.Lotus) ch).inRange(pos)
					&& Actor.findChar(pos) != null){
				plant.trigger();
				return null;
			}
		}
		
		return plant;
	}
	
	public void uproot( int pos ) {
		plants.remove(pos);
		GameScene.updateMap( pos );
	}

	public Trap setTrap( Trap trap, int pos ){
		Trap existingTrap = traps.get(pos);
		if (existingTrap != null){
			traps.remove( pos );
		}
		trap.set( pos );
		traps.put( pos, trap );
		cellAt(pos).removeTerrain(Terrain.TRAP);
		cellAt(pos).removeTerrain(Terrain.SECRET_TRAP);
		cellAt(pos).removeTerrain(Terrain.INACTIVE_TRAP);
		cellChanged(pos);
		GameScene.updateMap( pos );
		return trap;
	}

	public void disarmTrap( int pos ) {
		set(pos, Terrain.INACTIVE_TRAP);
		GameScene.updateMap(pos);
	}

	public void discover( int cell ) {
		//隐藏门自身带有“覆盖内容”属性，不能当成压在物件上的墙直接跳过；
		//否则搜索成功后地形仍停在 SECRET_DOOR，看上去还是墙。
		boolean secretDoor = Door.is(this, cell, Door.State.SECRET);
		if (cellAt(cell).buried() && !secretDoor) return;
		if (secretDoor) Door.reveal(this, cell);
		Trap trap = traps.get(cell);
		if (trap != null) trap.reveal();
		else if (hasTerrain(cell, Terrain.SECRET_TRAP))
			cellAt(cell).place(Terrain.discover(Terrain.SECRET_TRAP));
		cellChanged(cell);
	}

	public boolean setCellToWater( boolean includeTraps, int cell ){
		Point p = cellToPoint(cell);

		//if a custom tilemap is over that cell, check if it allows water
		for (CustomTilemap cust : customTiles){
			Point custPoint = new Point(p);
			custPoint.x -= cust.tileX;
			custPoint.y -= cust.tileY;
			if (custPoint.x >= 0 && custPoint.y >= 0
					&& custPoint.x < cust.tileW && custPoint.y < cust.tileH){
				if (!cust.allowWater(custPoint.x, custPoint.y)){
					return false;
				}
			}
		}

		if (!cellAt(cell).supports(CellContent.SUPPORTS_COVER) || solid[cell]
				|| (!includeTraps && traps.get(cell) != null)) return false;
		return cellAt(cell).addContent(new CellContent(Terrain.WATER, region()));
	}
	
	public int fallCell( boolean fallIntoPit ) {
		int result;
		do {
			result = randomRespawnCell( null );
			if (result == -1) return -1;
		} while (traps.get(result) != null
				|| findMob(result) != null);
		return result;
	}
	
	public void occupyCell( Char ch ){
		if (!ch.isImmune(Web.class) && Blob.volumeAt(ch.pos, Web.class) > 0){
			blobs.get(Web.class).clear(ch.pos);
			Web.affectChar( ch );
		}

		if (Blob.volumeAt(ch.pos, SacrificialFire.class) > 0 && ch.buff( SacrificialFire.Marked.class ) == null){
			if (Dungeon.level.heroFOV[ch.pos]) {
				CellEmitter.get(ch.pos).burst( SacrificialParticle.FACTORY, 5 );
			}
			Buff.prolong( ch, SacrificialFire.Marked.class, SacrificialFire.Marked.DURATION );
		}

		if (!ch.flying){

			//we call act here instead of detach in case the debuffs haven't managed to deal dmg once yet
			if (hasTerrain(ch.pos, Terrain.WATER)){
				if (ch.buff(Burning.class) != null){
					ch.buff(Burning.class).act();
				}
				if (ch.buff(Ooze.class) != null){
					ch.buff(Ooze.class).act();
				}
			}

			if ( (hasTerrain(ch.pos, Terrain.GRASS) || hasTerrain(ch.pos, Terrain.EMBERS))
					&& ch == Dungeon.hero && Dungeon.hero.hasTalent(Talent.REJUVENATING_STEPS)
					&& ch.buff(Talent.RejuvenatingStepsCooldown.class) == null){

				if (!Regeneration.regenOn()){
					set(ch.pos, Terrain.FURROWED_GRASS);
				} else if (ch.buff(Talent.RejuvenatingStepsFurrow.class) != null && ch.buff(Talent.RejuvenatingStepsFurrow.class).count() >= 200) {
					set(ch.pos, Terrain.FURROWED_GRASS);
				} else {
					set(ch.pos, Terrain.HIGH_GRASS);
					Buff.count(ch, Talent.RejuvenatingStepsFurrow.class, 3 - Dungeon.hero.pointsInTalent(Talent.REJUVENATING_STEPS));
				}
				GameScene.updateMap(ch.pos);
				Buff.affect(ch, Talent.RejuvenatingStepsCooldown.class, 15f - 5f*Dungeon.hero.pointsInTalent(Talent.REJUVENATING_STEPS));
			}
			
			if (pit[ch.pos]){
				if (ch == Dungeon.hero) {
					if (Dungeon.hero.hasTalent(Talent.VOID_WALKER)){
						if (Dungeon.hero.buff(Talent.VoidWalkerBuff.class) == null){
							Buff.prolong(Dungeon.hero, Talent.VoidWalkerBuff.class,
									Dungeon.hero.pointsInTalent(Talent.VOID_WALKER) == 1 ? 3 : 5);
						}
					} else {
						Chasm.heroFall(ch.pos);
					}
				} else if (ch instanceof Mob) {
					Chasm.mobFall( (Mob)ch );
				}
				return;
			}
			
			//characters which are not the hero or a sheep 'soft' press cells
			pressCell( ch.pos, ch instanceof Hero || ch instanceof Sheep, Door.Opening.CHARACTER);
		} else {
			if (Door.is(this, ch.pos, Door.State.CLOSED)){
				Door.enter( ch.pos, Door.Opening.CHARACTER );
			}
		}

		if (ch.isAlive() && ch instanceof Piranha && !water[ch.pos]){
			((Piranha) ch).dieOnLand();
		}
		// 普通移动、传送和击退共用落点处理，角色光源随落点刷新。
		if (ch != Dungeon.hero && (ch.buff(Burning.class) != null || Char.hasProp(ch, Char.Property.FIERY))) {
			Dungeon.observe();
		}
	}
	
	//public method for forcing the hard press of a cell. e.g. when an item lands on it
	public void pressCell( int cell ){
		pressCell( cell, true, Door.Opening.IMPACT );
	}
	
	//a 'soft' press ignores hidden traps
	//a 'hard' press triggers all things
	private void pressCell( int cell, boolean hard, Door.Opening opening ) {

		if (cellAt(cell).buried()) return;
		Trap trap = traps.get(cell);
		if (trap != null && (!trap.active || (!hard && !trap.visible))) trap = null;
		if (trap != null && !trap.visible) {
			if (primeOutdatedTrap(trap)) trap = null;
			else GLog.i(Messages.get(Level.class, "hidden_trap", trap.name()));
		}
		if (hasTerrain(cell, Terrain.HIGH_GRASS) || hasTerrain(cell, Terrain.FURROWED_GRASS)) HighGrass.trample(this, cell);
		if (cellAt(cell).ground() == Terrain.WELL) WellWater.affectCell(cell);
		if (Door.is(this, cell, Door.State.CLOSED)) Door.enter(cell, opening);

		TimekeepersHourglass.timeFreeze timeFreeze =
				Dungeon.hero.buff(TimekeepersHourglass.timeFreeze.class);

		Swiftthistle.TimeBubble bubble =
				Dungeon.hero.buff(Swiftthistle.TimeBubble.class);

		if (trap != null) {
			if (bubble != null){
				Sample.INSTANCE.play(Assets.Sounds.TRAP);
				discover(cell);
				bubble.setDelayedPress(cell);
				
			} else if (timeFreeze != null){
				Sample.INSTANCE.play(Assets.Sounds.TRAP);
				discover(cell);
				timeFreeze.setDelayedPress(cell);
				
			} else {
				if (Dungeon.hero.pos == cell) {
					Dungeon.hero.interrupt();
				}
				trap.trigger();

			}
		}
		
		Plant plant = plants.get( cell );
		if (plant != null) {
			if (bubble != null){
				Sample.INSTANCE.play(Assets.Sounds.TRAMPLE, 1, Random.Float( 0.96f, 1.05f ) );
				bubble.setDelayedPress(cell);

			} else if (timeFreeze != null){
				Sample.INSTANCE.play(Assets.Sounds.TRAMPLE, 1, Random.Float( 0.96f, 1.05f ) );
				timeFreeze.setDelayedPress(cell);

			} else {
				plant.trigger();

			}
		}

		if (hard && Blob.volumeAt(cell, Web.class) > 0){
			blobs.get(Web.class).clear(cell);
		}
	}

	//outdated design challenge: the first time an outdated trap is triggered (by anything) it silently
	//primes instead of triggering, with no sound or reveal. Returns true if the trap was primed
	private boolean primeOutdatedTrap( Trap trap ){
		if (Dungeon.isChallenged(Challenges.OUTDATED_DESIGN)
				&& trap != null
				&& trap.outdated
				&& !trap.primed){
			trap.primed = true;
			return true;
		}
		return false;
	}

	private boolean[] heroMindFov;
	private boolean[] heroFOVBuffer;
	private LevelLighting lighting;
	private final Rect fieldOfViewChanges = new Rect();

	public LevelLighting lighting() {
		if (lighting == null) lighting = new LevelLighting(this);
		return lighting;
	}

	/** 光源更新不强制创建缓存，生成和读档后的首次观察会读取完整状态。 */
	public void updateBlobLighting(Blob blob) {
		if (lighting != null) lighting.blobChanged(blob);
		if (this == Dungeon.level && blob.getClass() == Fire.class) GameScene.updateFog();
	}

	public void updateBlobLighting(Blob blob, int cell) {
		if (lighting != null) lighting.blobChanged(blob, cell);
		// 自然视野未变化时，书架点燃和熄灭仍须重绘墙面与顶部的迷雾。
		if (this == Dungeon.level && blob.getClass() == Fire.class && hasTerrain(cell, Terrain.BOOKSHELF)) {
			GameScene.updateFog(cell, 1);
		}
	}

	/** 自然视距与 Light 在此收口，天赋修正只用于本次视觉投射。 */
	public int visionDistance(Char viewer) {
		float distance = viewer.viewDistance;
		if (viewer == Dungeon.hero) {
			distance = this instanceof VaultLevel ? Math.min(8, viewDistance) : viewDistance;
			if (viewer.buff(Light.class) != null) {
				distance = Math.max(distance, this instanceof VaultLevel ? 8 : Light.DISTANCE);
			}
			viewer.viewDistance = Math.round(distance);
		} else if (viewer.buff(Light.class) != null) {
			distance = Math.max(distance, Light.DISTANCE);
		}
		if (viewer instanceof Hero) {
			distance *= 1f + 0.25f * ((Hero) viewer).pointsInTalent(Talent.FARSIGHT);
			distance *= EyeOfNewt.visionRangeMultiplier();
		}
		return Math.max(0, Math.min(ShadowCaster.MAX_DISTANCE, Math.round(distance)));
	}

	public void updateFieldOfView( Char c, boolean[] fieldOfView ) {
		// 渲染线程只读取完成的英雄视野，避免普通视野清空后、扩展视野补回前闪烁。
		boolean[] destination = fieldOfView;
		if (c == Dungeon.hero) {
			if (heroFOVBuffer == null || heroFOVBuffer.length != length()) {
				heroFOVBuffer = new boolean[length()];
			}
			fieldOfView = heroFOVBuffer;
		}

		int cx = c.pos % width();
		int cy = c.pos / width();
		int viewDist = visionDistance(c);
		
		boolean sighted = c.buff( Blindness.class ) == null && c.buff( Shadows.class ) == null
						&& c.isAlive();
		if (sighted) {
			boolean seeGrass = (!(this instanceof MiningLevel) || Blacksmith.Quest.Type() != Blacksmith.Quest.FUNGI)
					&& ((c instanceof Hero && ((Hero) c).subClass == HeroSubClass.WARDEN)
					|| c instanceof YogFist.SoiledFist || c instanceof GnollGeomancer);
			boolean seeSmoke = c.alignment == Char.Alignment.ALLY || c instanceof GnollGeomancer;
			LevelLighting light = lighting();
			ShadowCaster.castShadow(cx, cy, width(), fieldOfView, light.blocking(seeGrass, seeSmoke),
					viewDist, light.visibleLights(c.pos));
		} else {
			BArray.setFalse(fieldOfView);
		}
		
		int sense = 1;
		//Currently only the hero can get mind vision
		if (c.isAlive() && c == Dungeon.hero) {
			for (Buff b : c.buffs( MindVision.class )) {
				sense = Math.max( ((MindVision)b).distance, sense );
			}
			if (c.buff(MagicalSight.class) != null){
				sense = Math.max( MagicalSight.DISTANCE, sense );
			}
		}
		
		//uses rounding
		if (!sighted || sense > 1) {
			
			int[][] rounding = ShadowCaster.rounding;
			
			int left, right;
			int pos;
			for (int y = Math.max(0, cy - sense); y <= Math.min(height()-1, cy + sense); y++) {
				if (rounding[sense][Math.abs(cy - y)] < Math.abs(cy - y)) {
					left = cx - rounding[sense][Math.abs(cy - y)];
				} else {
					left = sense;
					while (rounding[sense][left] < rounding[sense][Math.abs(cy - y)]){
						left--;
					}
					left = cx - left;
				}
				right = Math.min(width()-1, cx + cx - left);
				left = Math.max(0, left);
				pos = left + y * width();
				System.arraycopy(discoverable, pos, fieldOfView, pos, right - left + 1);
			}
		}

		if (c instanceof SpiritHawk.HawkAlly && Dungeon.hero.pointsInTalent(Talent.EAGLE_EYE) >= 3){
			int range = 1+(Dungeon.hero.pointsInTalent(Talent.EAGLE_EYE)-2);
			for (Mob mob : mobs) {
				int p = mob.pos;
				if (!fieldOfView[p] && distance(c.pos, p) <= range) {
					for (int i : PathFinder.NEIGHBOURS9) {
						fieldOfView[mob.pos + i] = true;
					}
				}
			}
		}

		//Currently only the hero can get mind vision or awareness
		if (c.isAlive() && c == Dungeon.hero) {

			if (heroMindFov == null || heroMindFov.length != length()){
				heroMindFov = new boolean[length];
			} else {
				BArray.setFalse(heroMindFov);
			}

			Dungeon.hero.mindVisionEnemies.clear();

			int mindVisRange = 0;
			if (c.buff(MindVision.class) != null) {
				mindVisRange = Integer.MAX_VALUE;
			} else {
				if (((Hero) c).hasTalent(Talent.HEIGHTENED_SENSES)) {
					mindVisRange = 1 + ((Hero) c).pointsInTalent(Talent.HEIGHTENED_SENSES);
				}
				if (c.buff(DivineSense.DivineSenseTracker.class) != null) {
					if (((Hero) c).heroClass == HeroClass.CLERIC) {
						mindVisRange = 4 + 4 * ((Hero) c).pointsInTalent(Talent.DIVINE_SENSE);
					} else {
						mindVisRange = 1 + 2 * ((Hero) c).pointsInTalent(Talent.DIVINE_SENSE);
					}
				}
				mindVisRange = Math.max(mindVisRange, EyeOfNewt.mindVisionRange());
			}

			if (mindVisRange >= 1) {

				//power of many's life link spell allows allies to get divine sense
				Char ally = PowerOfMany.getPoweredAlly();
				if (ally != null && ally.buff(DivineSense.DivineSenseTracker.class) == null) {
					ally = null;
				}

				for (Mob mob : mobs) {
					if ((mob instanceof Mimic && mob.alignment == Char.Alignment.NEUTRAL && ((Mimic) mob).stealthy())
						|| Char.hasProp(mob, Char.Property.OBJECT)){
						continue;
					}
					int p = mob.pos;
					if (!fieldOfView[p] && (distance(c.pos, p) <= mindVisRange || (ally != null && distance(ally.pos, p) <= mindVisRange))) {
						for (int i : PathFinder.NEIGHBOURS9) {
							heroMindFov[mob.pos + i] = true;
						}
					}
				}
			}
			
			if (c.buff( Awareness.class ) != null) {
				for (Heap heap : heaps.valueList()) {
					int p = heap.pos;
					for (int i : PathFinder.NEIGHBOURS9) heroMindFov[p+i] = true;
				}
			}

			for (TalismanOfForesight.CharAwareness a : c.buffs(TalismanOfForesight.CharAwareness.class)){
				Char ch = (Char) Actor.findById(a.charID);
				if (ch == null || !ch.isAlive() || Char.hasProp(ch, Char.Property.OBJECT)) {
					continue;
				}
				int p = ch.pos;
				for (int i : PathFinder.NEIGHBOURS9) heroMindFov[p+i] = true;
			}

			for (TalismanOfForesight.HeapAwareness h : c.buffs(TalismanOfForesight.HeapAwareness.class)){
				if (Dungeon.depth != h.depth || Dungeon.branch != h.branch) continue;
				for (int i : PathFinder.NEIGHBOURS9) heroMindFov[h.pos+i] = true;
			}

			for (Mob m : mobs){
				if (m instanceof WandOfWarding.Ward
						|| m instanceof WandOfRegrowth.Lotus
						|| m instanceof SpiritHawk.HawkAlly
						|| m.buff(PowerOfMany.PowerBuff.class) != null){
					if (m.fieldOfView == null || m.fieldOfView.length != length()){
						m.fieldOfView = new boolean[length()];
						Dungeon.level.updateFieldOfView( m, m.fieldOfView );
					}
					BArray.or(heroMindFov, m.fieldOfView, heroMindFov);
				}
			}

			if (((Hero)c).pointsInTalent(Talent.MULTIPLE_EXISTENCE) >= 2){
				for (Mob mob : mobs){
					if (mob instanceof MirrorImage){
						for (int offset : PathFinder.NEIGHBOURS9){
							heroMindFov[mob.pos + offset] = true;
						}
					}
				}
			}

			for (RevealedArea a : c.buffs(RevealedArea.class)){
				if (Dungeon.depth != a.depth || Dungeon.branch != a.branch) continue;
				for (int i : PathFinder.NEIGHBOURS9) heroMindFov[a.pos+i] = true;
			}

			//set mind vision chars
			for (Mob mob : mobs) {
				if (heroMindFov[mob.pos] && !fieldOfView[mob.pos]
						&& !Char.hasProp(mob, Char.Property.OBJECT)){
					Dungeon.hero.mindVisionEnemies.add(mob);
				}
			}

			BArray.or(heroMindFov, fieldOfView, fieldOfView);

		}

		// 在所有透视、感知、环境光及共享视野合并后收口，不移除相关 Buff。
		if (c.buff(ForcedBlindness.class) != null) {
			BArray.setFalse(fieldOfView);
			for (int y = Math.max(0, cy - 1); y <= Math.min(height() - 1, cy + 1); y++) {
				for (int x = Math.max(0, cx - 1); x <= Math.min(width() - 1, cx + 1); x++) {
					int cell = x + y * width();
					fieldOfView[cell] = discoverable[cell];
				}
			}
			if (c == Dungeon.hero) {
				if (heroMindFov != null) BArray.setFalse(heroMindFov);
				Dungeon.hero.mindVisionEnemies.clear();
			}
		}

		if (c == Dungeon.hero) {
			for (Heap heap : heaps.valueList())
				if (!heap.seen && fieldOfView[heap.pos])
					heap.seen = true;
			// 同一次扫描发布完成的结果，并记录新增、消失及尚未探索的可见格。
			for (int cell = 0; cell < length(); cell++) {
				if (destination[cell] != fieldOfView[cell] || (fieldOfView[cell] && !visited[cell])) {
					fieldOfViewChanges.union(cell % width(), cell / width());
				}
				destination[cell] = fieldOfView[cell];
			}
		}

	}

	/** 默认光源按当前楼层环境复用，特殊楼层只补充自己的来源。 */
	protected int staticEnvironmentalLightRadius(int cell) {
		return lighting().terrainRadius(terrainAt(cell));
	}

	/** 最终视野驱动探索和迷雾，熄灯及远处感知消失也会进入更新范围。 */
	public void updateFog() {
		for (int offset : PathFinder.NEIGHBOURS9) {
			int cell = Dungeon.hero.pos + offset;
			if (!visited[cell]) {
				visited[cell] = true;
				fieldOfViewChanges.union(cell % width(), cell / width());
			}
		}
		if (fieldOfViewChanges.isEmpty()) return;
		int left = Math.max(0, fieldOfViewChanges.left - 1);
		int top = Math.max(0, fieldOfViewChanges.top - 1);
		int right = Math.min(width(), fieldOfViewChanges.right + 1);
		int bottom = Math.min(height(), fieldOfViewChanges.bottom + 1);
		for (int y = top; y < bottom; y++) {
			BArray.or(visited, heroFOV, left + y * width(), right - left, visited);
		}
		GameScene.updateFog(left, top, right - left, bottom - top);
		fieldOfViewChanges.setEmpty();
	}

	public float levelExplorePercent( int depth ){
		return 0;
	}
	
	public int distance( int a, int b ) {
		int ax = a % width();
		int ay = a / width();
		int bx = b % width();
		int by = b / width();
		return Math.max( Math.abs( ax - bx ), Math.abs( ay - by ) );
	}
	
	public boolean adjacent( int a, int b ) {
		return distance( a, b ) == 1;
	}
	
	//uses Pythagorean theorem for true distance, as if there was no movement grid
	public float trueDistance(int a, int b){
		int ax = a % width();
		int ay = a / width();
		int bx = b % width();
		int by = b / width();
		return (float)Math.sqrt(Math.pow(Math.abs( ax - bx ), 2) + Math.pow(Math.abs( ay - by ), 2));
	}

	//usually just if the base terrain of a cell is solid, but other cases exist too
	//only check on base terrain, we want to ignore temporary changes from blobs (e.g. light wall)
	public boolean invalidHeroPos( int tile ){
		int flags = cellAt(tile).flags();
		return (flags & Terrain.PASSABLE) != 0 && (flags & Terrain.AVOID) != 0;
	}

	//returns true if the input is a valid tile within the level
	public boolean insideMap( int tile ){
				//top and bottom row and beyond
		return !((tile < width || tile >= length - width) ||
				//left and right column
				(tile % width == 0 || tile % width == width-1));
	}

	public Point cellToPoint( int cell ){
		return new Point(cell % width(), cell / width());
	}

	public int pointToCell( Point p ){
		return p.x + p.y*width();
	}
	
	public String tileName( int tile ) {
		Door.State door = Door.state(tile);
		if (door != null) return Messages.get(Level.class, door.nameKey);
		
		switch (tile) {
			case Terrain.CHASM:
				return Messages.get(Level.class, "chasm_name");
			case Terrain.EMPTY:
			case Terrain.EMPTY_SP:
			case Terrain.EMPTY_DECO:
			case Terrain.CUSTOM_DECO_EMPTY:
			case Terrain.SECRET_TRAP:
				return Messages.get(Level.class, "floor_name");
			case Terrain.GRASS:
				return Messages.get(Level.class, "grass_name");
			case Terrain.WATER:
				return Messages.get(Level.class, "water_name");
			case Terrain.WALL:
			case Terrain.WALL_DECO:
				return Messages.get(Level.class, "wall_name");
			case Terrain.ENTRANCE:
			case Terrain.ENTRANCE_SP:
				return Messages.get(Level.class, "entrace_name");
			case Terrain.EXIT:
				return Messages.get(Level.class, "exit_name");
			case Terrain.EMBERS:

				return Messages.get(Level.class, "embers_name");
			case Terrain.FURROWED_GRASS:
				return Messages.get(Level.class, "furrowed_grass_name");
			case Terrain.PEDESTAL:
				return Messages.get(Level.class, "pedestal_name");
			case Terrain.BARRICADE:
				return Messages.get(Level.class, "barricade_name");
			case Terrain.HIGH_GRASS:
				return Messages.get(Level.class, "high_grass_name");
			case Terrain.LOCKED_EXIT:
				return Messages.get(Level.class, "locked_exit_name");
			case Terrain.UNLOCKED_EXIT:
				return Messages.get(Level.class, "unlocked_exit_name");
			case Terrain.WELL:
				return Messages.get(Level.class, "well_name");
			case Terrain.EMPTY_WELL:
				return Messages.get(Level.class, "empty_well_name");
			case Terrain.STATUE:
			case Terrain.STATUE_SP:


				return Messages.get(Level.class, "statue_name");
			case Terrain.INACTIVE_TRAP:
				return Messages.get(Level.class, "inactive_trap_name");
			case Terrain.BOOKSHELF:
				return Messages.get(Level.class, "bookshelf_name");
			case Terrain.ALCHEMY:
				return Messages.get(Level.class, "alchemy_name");
			default:
				return Messages.get(Level.class, "default_name");
		}
	}
	
	public String tileDesc( int tile ) {
		Door.State door = Door.state(tile);
		if (door != null) return door.descKey.isEmpty() ? "" : Messages.get(Level.class, door.descKey);
		
		switch (tile) {
			case Terrain.CHASM:
				return Messages.get(Level.class, "chasm_desc");
			case Terrain.WATER:
				return Messages.get(Level.class, "water_desc");
			case Terrain.ENTRANCE:
			case Terrain.ENTRANCE_SP:
				return Messages.get(Level.class, "entrance_desc");
			case Terrain.EXIT:
			case Terrain.UNLOCKED_EXIT:
				return Messages.get(Level.class, "exit_desc");
			case Terrain.EMBERS:

				return Messages.get(Level.class, "embers_desc");
			case Terrain.HIGH_GRASS:
			case Terrain.FURROWED_GRASS:
				return Messages.get(Level.class, "high_grass_desc");
			case Terrain.LOCKED_EXIT:
				return Messages.get(Level.class, "locked_exit_desc");
			case Terrain.BARRICADE:
				return Messages.get(Level.class, "barricade_desc");
			case Terrain.INACTIVE_TRAP:
				return Messages.get(Level.class, "inactive_trap_desc");
			case Terrain.STATUE:
			case Terrain.STATUE_SP:


				return Messages.get(Level.class, "statue_desc");
			case Terrain.ALCHEMY:
				return Messages.get(Level.class, "alchemy_desc");
			case Terrain.EMPTY_WELL:
				return Messages.get(Level.class, "empty_well_desc");
			default:
				return "";
		}
	}
}
