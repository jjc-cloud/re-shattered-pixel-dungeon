package com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard;

import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.CellPlacement;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.watabou.utils.Bundle;
import com.watabou.utils.Point;

/** 楼梯固定在生成器事先选定的中心，不再在房间内重抽。 */
public class CavesStairRoom extends CaveRoom {
    private boolean entrance;

    public CavesStairRoom() { }
    public CavesStairRoom(boolean entrance) { this.entrance = entrance; }

    @Override public int minWidth() { return 11; }
    @Override public int maxWidth() { return 13; }
    @Override public int minHeight() { return 11; }
    @Override public int maxHeight() { return 13; }
    @Override public boolean isEntrance() { return entrance; }
    @Override public boolean isExit() { return !entrance; }

    @Override public void paint(Level level) {
        super.paint(level);
        Point center = new Point((left + right) / 2, (top + bottom) / 2);
        // 中心通向每个入口，防止洞穴斑块将预定楼梯封在岩壁里。
        for (Door door : connected.values()) {
            Point inside = pointInside(door, 1);
            Point corner = new Point(center.x, inside.y);
            Painter.drawLine(level, inside, corner, CellPlacement.reset(Terrain.EMPTY));
            Painter.drawLine(level, corner, center, CellPlacement.reset(Terrain.EMPTY));
        }
        Painter.fill(level, center.x - 1, center.y - 1, 3, 3, CellPlacement.reset(Terrain.EMPTY));
        int cell = level.pointToCell(center);
        Painter.set(level, cell, CellPlacement.reset(entrance ? Terrain.ENTRANCE : Terrain.EXIT));
        level.transitions.add(new LevelTransition(level, cell, entrance
                ? LevelTransition.Type.REGULAR_ENTRANCE : LevelTransition.Type.REGULAR_EXIT));
    }

    @Override public boolean canPlaceCharacter(Point point, Level level) {
        return super.canPlaceCharacter(point, level)
                && (point.x != (left + right) / 2 || point.y != (top + bottom) / 2);
    }

    @Override public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put("caves_entrance", entrance);
    }
    @Override public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        entrance = bundle.getBoolean("caves_entrance");
    }
}
