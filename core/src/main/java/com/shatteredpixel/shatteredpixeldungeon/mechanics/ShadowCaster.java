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

package com.shatteredpixel.shatteredpixeldungeon.mechanics;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.LevelLighting;
import com.watabou.utils.BArray;

//based on: http://www.roguebasin.com/index.php?title=FOV_using_recursive_shadowcasting
public final class ShadowCaster {

	public static final int MAX_DISTANCE = 128;
	
	//max length of rows as FOV moves out, for each FOV distance
	//This is used to make the overall FOV circular, instead of square
	public static int[][] rounding;
	private static final int[] roundingAtTwo = {0, 1, 2};
	static {
		rounding = new int[MAX_DISTANCE+1][];
		for (int i=1; i <= MAX_DISTANCE; i++) {
			rounding[i] = new int[i+1];
			for (int j=1; j <= i; j++) {
				//testing the middle of a cell, so we use i + 0.5
				rounding[i][j] = (int)Math.min(
						j,
						Math.round( i * Math.cos( Math.asin( j / (i + 0.5) ))));
			}
		}
	}
	
	public static void castShadow( int x, int y, int w, boolean[] fieldOfView, boolean[] blocking, int distance ) {
		castShadow(x, y, w, fieldOfView, blocking, distance, null);
	}

	/** 在同一次投射中合并自然视野和最多 16 格的环境光视野。 */
	public static void castShadow(int x, int y, int w, boolean[] fieldOfView, boolean[] blocking,
	                              int distance, boolean[] illuminated) {
		distance = Math.max(0, Math.min(distance, MAX_DISTANCE));
		BArray.setFalse(fieldOfView);
		cast(x, y, w, fieldOfView, blocking, distance, illuminated, 0, 0, w, fieldOfView.length / w);
	}

	/** 光源只合并到指定区域，不清空整图；多个来源因此可以重叠。 */
	public static void castLight(int x, int y, int w, boolean[] illuminated, boolean[] blocking,
	                             int radius, int left, int top, int right, int bottom) {
		cast(x, y, w, illuminated, blocking, Math.max(0, Math.min(radius, MAX_DISTANCE)),
				null, left, top, right, bottom);
	}

	private static void cast(int x, int y, int w, boolean[] fieldOfView, boolean[] blocking,
	                         int naturalDistance, boolean[] illuminated, int left, int top, int right, int bottom) {
		if (x >= left && x < right && y >= top && y < bottom) {
			fieldOfView[y * w + x] = true;
		}
		int distance = illuminated == null ? naturalDistance : Math.max(naturalDistance, LevelLighting.MAX_SIGHT_DISTANCE);
		if (distance == 0) return;
		//scans octants, clockwise
		try {
			scanOctant(distance, fieldOfView, blocking, 1, x, y, w, 0.0, 1.0, +1, -1, false, naturalDistance, illuminated, left, top, right, bottom);
			scanOctant(distance, fieldOfView, blocking, 1, x, y, w, 0.0, 1.0, -1, +1, true, naturalDistance, illuminated, left, top, right, bottom);
			scanOctant(distance, fieldOfView, blocking, 1, x, y, w, 0.0, 1.0, +1, +1, true, naturalDistance, illuminated, left, top, right, bottom);
			scanOctant(distance, fieldOfView, blocking, 1, x, y, w, 0.0, 1.0, +1, +1, false, naturalDistance, illuminated, left, top, right, bottom);
			scanOctant(distance, fieldOfView, blocking, 1, x, y, w, 0.0, 1.0, -1, +1, false, naturalDistance, illuminated, left, top, right, bottom);
			scanOctant(distance, fieldOfView, blocking, 1, x, y, w, 0.0, 1.0, +1, -1, true, naturalDistance, illuminated, left, top, right, bottom);
			scanOctant(distance, fieldOfView, blocking, 1, x, y, w, 0.0, 1.0, -1, -1, true, naturalDistance, illuminated, left, top, right, bottom);
			scanOctant(distance, fieldOfView, blocking, 1, x, y, w, 0.0, 1.0, -1, -1, false, naturalDistance, illuminated, left, top, right, bottom);
		} catch (Exception e){
			ShatteredPixelDungeon.reportException(e);
			BArray.setFalse(fieldOfView);
		}

	}
	
	//scans a single 45 degree octant of the FOV.
	//This can add up to a whole FOV by mirroring in X(mX), Y(mY), and X=Y(mXY)
	private static void scanOctant(int distance, boolean[] fov, boolean[] blocking, int row,
	                               int x, int y, int w, double lSlope, double rSlope,
	                               int mX, int mY, boolean mXY, int naturalDistance,
	                               boolean[] illuminated, int left, int top, int right, int bottom){
		
		boolean inBlocking = false;
		int start, end;
		int col;

		int[] roundingAtDist = distance == 2 ? roundingAtTwo : rounding[distance];
		int[] naturalRounding = naturalDistance == 2 ? roundingAtTwo : rounding[naturalDistance];
		int height = fov.length / w;
		
		//calculations are offset by 0.5 because FOV is coming from the center of the source cell
		
		//for each row, starting with the current one
		for (; row <= distance; row++){

			//if we have negative space to traverse, just quit.
			if (rSlope < lSlope) return;
			
			//we offset by slightly less than 0.5 to account for slopes just touching a cell
			if (lSlope == 0)    start = 0;
			else                start = (int)Math.floor((row - 0.5) * lSlope + 0.499);
			
			// 环境光按项目 distance() 的格距限制，16 格以内包含对角格。
			int rowEnd = illuminated != null && row <= LevelLighting.MAX_SIGHT_DISTANCE ? row : roundingAtDist[row];
			if (rSlope == 1)    end = rowEnd;
			else                end = Math.min( rowEnd,
			                                    (int)Math.ceil((row + 0.5) * rSlope - 0.499));
			
			//coordinates of source
			int cell = x + y*w;
			
			//plus coordinates of current cell (including mirroring in x, y, and x=y)
			if (mXY)    cell += mX*start*w + mY*row;
			else        cell += mX*start + mY*row*w;
			
			//for each column in this row, which
			for (col = start; col <= end; col++){


				//handles the error case of the slope value at the end of a cell being 1 farther
				// along then at the beginning of the cell, and that earlier cell is vision blocking
				if (col == end && inBlocking && (int)Math.ceil((row - 0.5) * rSlope - 0.499) != end){
					break;
				}
				
				int tx = mXY ? x + mY * row : x + mX * col;
				int ty = mXY ? y + mX * col : y + mY * row;
				boolean inside = tx >= 0 && tx < w && ty >= 0 && ty < height;
				if (inside && tx >= left && tx < right && ty >= top && ty < bottom
						&& (illuminated == null || (row <= naturalDistance && col <= naturalRounding[row])
						|| (row <= LevelLighting.MAX_SIGHT_DISTANCE && illuminated[cell]))) {
					fov[cell] = true;
				}
				if (!inside || blocking[cell]){
					if (!inBlocking){
						inBlocking = true;
						
						//start a new scan, 1 row deeper, ending at the left side of current cell
						if (col != start){
							scanOctant(distance, fov, blocking, row+1, x, y, w, lSlope,
									//change in x over change in y
									(col - 0.5) / (row + 0.5),
									mX, mY, mXY, naturalDistance, illuminated, left, top, right, bottom);
						}
					}
				
				} else {
					if (inBlocking){
						inBlocking = false;
						
						//restrict current scan to the left side of current cell for future rows
						
						//change in x over change in y
						lSlope = (col - 0.5) / (row - 0.5);
					}
				}
				
				if (!mXY)   cell += mX;
				else        cell += mX*w;
				
			}
			
			//if the row ends in a blocking cell, this scan is finished.
			if (inBlocking) return;
		}
	}
}
