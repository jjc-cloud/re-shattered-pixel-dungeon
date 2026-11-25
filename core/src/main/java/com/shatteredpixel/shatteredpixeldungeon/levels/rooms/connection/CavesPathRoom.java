package com.shatteredpixel.shatteredpixeldungeon.levels.rooms.connection;

import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.CellPlacement;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/** 房间之间的独立连接，较长的走廊可在岩壁或悬崖之间转弯。 */
public class CavesPathRoom extends TunnelRoom {
    public int branch;

    @Override public int minWidth() { return 2; }
    @Override public int minHeight() { return 2; }

    @Override public int maxWidth() { return 6; }
    @Override public int maxHeight() { return 6; }

    @Override public void paint(Level level) {
        // 沿用桥廊的铺法，先在足够宽的连接房内留出悬崖，再画通路。
        if (level.feeling != Level.Feeling.CHASM && Math.min(width(), height()) > 3 && Random.Int(3) == 0) {
            Painter.fill(level, this, 1, CellPlacement.reset(Terrain.CHASM));
        }
        super.paint(level);
    }

    @Override public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put("caves_branch", branch);
    }

    @Override public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        branch = bundle.getInt("caves_branch");
    }
}
