package com.shatteredpixel.shatteredpixeldungeon.levels.painters;

import com.shatteredpixel.shatteredpixeldungeon.levels.CellState;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;

/** 生成指令直接描述地面和内容；几何画笔只负责选中格子，不解释地形编号。 */
public final class CellPlacement {

	public static final CellPlacement NONE = new CellPlacement(false, -1);

	private final boolean clear;
	private final int ground;
	private final int[] contents;

	private CellPlacement(boolean clear, int ground, int... contents) {
		if ((clear || ground != -1) && !CellState.isGround(ground)) {
			throw new IllegalArgumentException("铺设地面必须指定基础地形");
		}
		for (int content : contents) {
			if (CellState.isGround(content)) throw new IllegalArgumentException("内容不能包含基础地形");
		}
		this.clear = clear;
		this.ground = ground;
		this.contents = contents.clone();
	}

	/** 铺地或指定地面上的内容，保留格子已有的其他内容。 */
	public static CellPlacement ground(int ground, int... contents) {
		return new CellPlacement(false, ground, contents);
	}

	/** 放置内容，不改动地面，也不隐式拆除墙或其他类别的内容。 */
	public static CellPlacement content(int... contents) {
		return new CellPlacement(false, -1, contents);
	}

	/** 房间轮廓、通道和完整模板明确重建环境；不能用来代替普通铺地。 */
	public static CellPlacement reset(int ground, int... contents) {
		return new CellPlacement(true, ground, contents);
	}

	/** 房间连接规则查询目标是否为不带内容的指定地面。 */
	public boolean isGround(int type) {
		return ground == type && contents.length == 0;
	}

	public void apply(Level level, int cell) {
		CellState state = level.cellAt(cell);
		if (clear) {
			state.reset(ground, contents);
		} else {
			if (ground != -1) state.setGround(ground);
			for (int content : contents) state.place(content);
		}
	}
}
