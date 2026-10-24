package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntSet;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.PathFinder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** 作用来源只提供物理作用，内容按自身属性参与；没有逐格响应表。 */
public final class TerrainInteractions {

    public enum Result { UNCHANGED, CHANGED }
    /** 会移除内容的属性位；来源只传自己那一位，点击不再借用 BREAKABLE。 */
    private static final int DESTRUCTIVE = CellContent.BREAKABLE | CellContent.BURNABLE
            | CellContent.CLICK_BREAKABLE | CellContent.EXPLOSION_BREAKABLE
            | CellContent.DISINTEGRATION_BREAKABLE | CellContent.SHOCKWAVE_BREAKABLE;
    private final Level level;
    private final ArrayDeque<TerrainPropagation> propagationPool = new ArrayDeque<>();
    public TerrainInteractions(Level level) { this.level = level; }

    public boolean allows(int cell, int property) {
        return level.insideMap(cell) && level.cellAt(cell).has(property);
    }
    public boolean conducts(int cell) {
        return level.insideMap(cell) && level.cellAt(cell).has(CellContent.CONDUCTIVE);
    }
    public boolean hasConductors() {
        for (int i = 0; i < level.length(); i++) if (conducts(i)) return true;
        return false;
    }
    public boolean hasEffects(int property) {
        for (int i = 0; i < level.length(); i++) if (allows(i, property)) return true;
        return false;
    }
    public void propertiesAt(int cell, int type, int add, int remove) {
        CellState state = level.cellAt(cell);
        CellContent content = state.content(type);
        if (content == null) throw new IllegalArgumentException("该格没有可设置属性的物件");
        content.properties(add, remove);
        level.cellChanged(cell);
    }
    TerrainPropagation acquirePropagation(int property) {
        TerrainPropagation event = propagationPool.pollFirst();
        if (event == null) event = new TerrainPropagation();
        event.begin(level, property);
        event.poolOwner = this;
        return event;
    }
    void releasePropagation(TerrainPropagation event) { propagationPool.addFirst(event); }

    public Result interact(int cell, int property) {
        if (!level.insideMap(cell) || (property & DESTRUCTIVE) == 0) return Result.UNCHANGED;
        return interact(cell, property, level.cellAt(cell).targets(property));
    }

    public boolean affectCells(List<Integer> cells, int property) {
        ArrayList<Integer> positions = new ArrayList<>();
        ArrayList<List<CellContent>> targets = new ArrayList<>();
        IntSet seen = new IntSet();
        for (int cell : cells) if (level.insideMap(cell) && seen.add(cell)) {
            positions.add(cell);
            targets.add(level.cellAt(cell).targets(property));
        }
        boolean changed = false;
        for (int i = 0; i < positions.size(); i++) {
            changed |= interact(positions.get(i), property, targets.get(i)) == Result.CHANGED;
        }
        return changed;
    }

    private Result interact(int cell, int property, List<CellContent> targets) {
        if ((property & DESTRUCTIVE) == 0) return Result.UNCHANGED;
        boolean changed = level.cellAt(cell).affect(property,
                (property & CellContent.BURNABLE) != 0, targets);
        if (!changed) return Result.UNCHANGED;
        if (level == Dungeon.level) {
            for (int offset : PathFinder.NEIGHBOURS9) {
                int next = cell + offset;
                if (next < 0 || next >= level.length()) continue;
                if (level.discoverable != null) level.discoverable[next] = true;
                GameScene.updateMap(next);
            }
        }
        return Result.CHANGED;
    }
    public IntArray blastSurfaces(List<Integer> affected, int[] distance, int range) {
        IntArray surfaces = new IntArray();
        IntSet seen = new IntSet();
        for (int cell : affected) {
            if (distance[cell] >= range) continue;
            for (int offset : PathFinder.NEIGHBOURS8) {
                int next = cell + offset;
                if (level.insideMap(next) && level.adjacent(cell, next)
                        && distance[next] == Integer.MAX_VALUE && level.solid[next]
                        && allows(next, CellContent.BREAKABLE | CellContent.BURNABLE | CellContent.EXPLOSION_BREAKABLE) && seen.add(next)) surfaces.add(next);
            }
        }
        return surfaces;
    }
}
