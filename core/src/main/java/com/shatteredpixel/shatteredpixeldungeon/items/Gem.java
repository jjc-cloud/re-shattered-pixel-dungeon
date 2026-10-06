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

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;

/** 不同种类的宝石共用一个物品堆，放下和出售整堆，投掷随机取一颗。 */
public class Gem extends Item {

	public static final float INVENTORY_SCALE = 1f / 2f;
	private static final float UNPLACED = -1000f;
	private static float[][][][] overlapRatios;

	public enum Type {
		ROUGH_RUBY(ItemSpriteSheet.ROUGH_RUBY, 300),
		ROUGH_DIAMOND(ItemSpriteSheet.ROUGH_DIAMOND, 300),
		ROUGH_TOPAZ(ItemSpriteSheet.ROUGH_TOPAZ, 300),
		RUBY(ItemSpriteSheet.RUBY, 600),
		DIAMOND(ItemSpriteSheet.DIAMOND, 600),
		TOPAZ(ItemSpriteSheet.TOPAZ, 600);

		public final int image;
		public final int price;

		Type(int image, int price) {
			this.image = image;
			this.price = price;
		}
	}

	public int[] counts = new int[Type.values().length];
	public float[] offsetX = new float[counts.length];
	public float[] offsetY = new float[counts.length];
	private int layoutSpan;
	private int thrownType = -1;

	public Gem() {
		this(Type.ROUGH_RUBY, 1);
	}

	public Gem(Type type, int amount) {
		stackable = true;
		levelKnown = cursedKnown = true;
		defaultAction = AC_THROW;
		usesTargeting = true;
		image = type.image;
		counts[type.ordinal()] = amount;
		quantity = amount;
		Arrays.fill(offsetX, UNPLACED);
		Arrays.fill(offsetY, UNPLACED);
		layout();
	}

	/** 先决定种类，抽中金币后才复用 Gold.random() 生成数量。 */
	public static Item generateTreasure() {
		if (Dungeon.depth >= 1 && Dungeon.depth <= 24) {
			float roll = Random.Float();
			float roughChance = Dungeon.depth <= 12 ? 0.20f : 0.10f;
			float gemChance = Dungeon.depth <= 12 ? 0.10f : 0.20f;
			if (roll < roughChance) return new Gem(Type.values()[Random.Int(3)], 1);
			if (roll < roughChance + gemChance) return new Gem(Type.values()[3 + Random.Int(3)], 1);
		}
		return new Gold().random();
	}

	@Override
	public Item random() {
		Arrays.fill(counts, 0);
		Arrays.fill(offsetX, UNPLACED);
		Arrays.fill(offsetY, UNPLACED);
		Type type = Random.element(Type.values());
		counts[type.ordinal()] = quantity = 1;
		image = type.image;
		layout();
		return this;
	}

	@Override
	public Item merge(Item other) {
		if (isSimilar(other)) {
			Gem gems = (Gem) other;
			// 合并后仍让拾取动画使用原来的单颗贴图。
			gems.image = gems.image();
			for (int i = 0; i < counts.length; i++) {
				counts[i] += gems.counts[i];
			}
			super.merge(other);
			Arrays.fill(gems.counts, 0);
			layout();
		}
		return this;
	}

	private int randomType() {
		ArrayList<Integer> available = new ArrayList<>();
		for (int i = 0; i < counts.length; i++) {
			if (counts[i] > 0) available.add(i);
		}
		return Random.element(available);
	}

	@Override
	public void cast(Hero user, int dst) {
		// 在动画开始前选一次，拆分时沿用，保证飞行和落地的是同一颗。
		thrownType = randomType();
		super.cast(user, dst);
	}

	@Override
	public Item split(int amount) {
		if (amount <= 0 || amount >= quantity) return null;
		Gem split = (Gem) duplicate();
		Arrays.fill(split.counts, 0);
		for (int i = 0; i < amount; i++) {
			int type = thrownType >= 0 ? thrownType : randomType();
			thrownType = -1;
			counts[type]--;
			split.counts[type]++;
		}
		quantity -= amount;
		split.quantity = amount;
		return split;
	}

	@Override
	protected void onThrow(int cell) {
		thrownType = -1;
		super.onThrow(cell);
	}

	@Override
	public Item quantity(int value) {
		if (value <= 0) {
			Arrays.fill(counts, 0);
			quantity = 0;
		} else if (value < quantity) {
			split(quantity - value);
		} else if (value > quantity) {
			int type = 0;
			while (type < counts.length - 1 && counts[type] == 0) type++;
			counts[type] += value - quantity;
			quantity = value;
			layout();
		}
		return this;
	}

	@Override
	public Item virtual() {
		return duplicate().quantity(0);
	}

	@Override
	public int image() {
		if (thrownType >= 0) return Type.values()[thrownType].image;
		for (int i = 0; i < counts.length; i++) {
			if (counts[i] > 0) return Type.values()[i].image;
		}
		return image;
	}

	@Override
	public String name() {
		int kinds = 0, type = 0;
		for (int i = 0; i < counts.length; i++) {
			if (counts[i] > 0) {
				kinds++;
				type = i;
			}
		}
		return kinds == 1 ? Messages.get(this, Type.values()[type].name().toLowerCase(java.util.Locale.ROOT)) : super.name();
	}

	@Override
	public String desc() {
		StringBuilder description = new StringBuilder(super.desc());
		for (int i = 0; i < counts.length; i++) {
			if (counts[i] > 0) {
				description.append("\n").append(Messages.get(this, "contents",
						Messages.get(this, Type.values()[i].name().toLowerCase(java.util.Locale.ROOT)), counts[i]));
			}
		}
		return description.toString();
	}

	@Override
	public int value() {
		int total = 0;
		for (int i = 0; i < counts.length; i++) total += counts[i] * Type.values()[i].price;
		return total;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	/** 按可见像素排列成一堆，每种宝石至少与另一种重叠 10%～20%，方向随机。 */
	private void layout() {
		ArrayList<Integer> kinds = new ArrayList<>();
		for (int i = 0; i < counts.length; i++) if (counts[i] > 0) kinds.add(i);
		if (kinds.isEmpty()) return;
		int size = ItemSpriteSheet.SIZE;
		int nominalSpan = Math.round(size / INVENTORY_SCALE) - size;
		if (kinds.size() == 1) {
			Arrays.fill(offsetX, UNPLACED);
			Arrays.fill(offsetY, UNPLACED);
			int kind = kinds.get(0);
			offsetX[kind] = offsetY[kind] = nominalSpan * INVENTORY_SCALE / 2f;
			layoutSpan = nominalSpan;
			return;
		}

		// 缓存各贴图的可见像素相交比例，透明边角不参与重叠计算。
		if (overlapRatios == null) {
			int[][] rows = new int[counts.length][size];
			int[] areas = new int[counts.length];
			for (int i = 0; i < counts.length; i++) {
				for (int y = 0; y < size; y++) {
					for (int x = 0; x < size; x++) {
						if ((ItemSprite.pick(Type.values()[i].image, x, y) >>> 24) != 0) rows[i][y] |= 1 << x;
					}
					areas[i] += Integer.bitCount(rows[i][y]);
				}
			}
			float[][][][] ratios = new float[counts.length][counts.length][size * 2 - 1][size * 2 - 1];
			for (int i = 0; i < counts.length; i++) {
				for (int j = i; j < counts.length; j++) {
					for (int dx = 1 - size; dx < size; dx++) {
						for (int dy = 1 - size; dy < size; dy++) {
							int intersection = 0;
							for (int y = 0; y < size; y++) {
								int otherY = y - dy;
								if (otherY < 0 || otherY >= size) continue;
								int shifted = dx >= 0 ? rows[j][otherY] << dx : rows[j][otherY] >>> -dx;
								intersection += Integer.bitCount(rows[i][y] & shifted);
							}
							float ratio = intersection / (float) Math.max(1, Math.min(areas[i], areas[j]));
							ratios[i][j][dx + size - 1][dy + size - 1] = ratio;
							ratios[j][i][-dx + size - 1][-dy + size - 1] = ratio;
						}
					}
				}
			}
			overlapRatios = ratios;
		}

		int[] positionsX = new int[counts.length];
		int[] positionsY = new int[counts.length];
		float savedShift = (nominalSpan - layoutSpan) / 2f;
		boolean valid = layoutSpan >= nominalSpan;
		boolean[][] touches = new boolean[counts.length][counts.length];
		for (int kind : kinds) {
			float x = offsetX[kind] / INVENTORY_SCALE - savedShift;
			float y = offsetY[kind] / INVENTORY_SCALE - savedShift;
			positionsX[kind] = Math.round(x);
			positionsY[kind] = Math.round(y);
			valid &= x >= 0 && x <= layoutSpan && y >= 0 && y <= layoutSpan;
		}
		if (valid) {
			for (int i : kinds) {
				for (int j : kinds) {
					if (i == j) continue;
					float relativeX = (offsetX[j] - offsetX[i]) / INVENTORY_SCALE;
					float relativeY = (offsetY[j] - offsetY[i]) / INVENTORY_SCALE;
					int dx = positionsX[j] - positionsX[i], dy = positionsY[j] - positionsY[i];
					valid &= Math.abs(relativeX - dx) < 0.001f && Math.abs(relativeY - dy) < 0.001f;
					float ratio = Math.abs(dx) < size && Math.abs(dy) < size
							? overlapRatios[i][j][dx + size - 1][dy + size - 1] : 0;
					valid &= ratio <= 0.20f;
					touches[i][j] = ratio >= 0.10f;
				}
			}
			boolean[] connected = new boolean[counts.length];
			connected[kinds.get(0)] = true;
			for (int pass = 0; pass < kinds.size(); pass++) {
				for (int i : kinds) for (int j : kinds) if (connected[i] && touches[i][j]) connected[j] = true;
			}
			for (int kind : kinds) valid &= connected[kind];
			if (valid) return;
		}

		// 使用独立随机源安排外观，避免位置搜索消耗地牢或投掷的随机序列。
		Random.pushGenerator();
		try {
			// 随机搜索二维位置，失败时回退重选位置，不使用分行排列。
			int totalBudget = 3_000_000;
			int maxSpan = nominalSpan + size;
			for (int span = nominalSpan; span <= maxSpan && totalBudget > 0; span += 2) {
				int side = span + 1;
				int[][] candidates = new int[kinds.size()][side * side];
				for (int[] choices : candidates) for (int i = 0; i < choices.length; i++) choices[i] = i;
				for (int attempt = 0; attempt < 32 && totalBudget > 0; attempt++) {
					Random.shuffle(kinds);
					for (int[] choices : candidates) Random.shuffle(choices);
					int first = kinds.get(0);
					positionsX[first] = span / 2 + Random.IntRange(-2, 2);
					positionsY[first] = span / 2 + Random.IntRange(-2, 2);
					int[] next = new int[kinds.size()];
					int depth = 1;
					int budget = 40000;
					while (depth > 0 && depth < kinds.size() && budget-- > 0 && totalBudget-- > 0) {
						if (next[depth] == candidates[depth].length) {
							depth--;
							continue;
						}
						int kind = kinds.get(depth);
						int choice = candidates[depth][next[depth]++];
						int x = choice % side, y = choice / side;
						boolean overlaps = false, allowed = true;
						for (int j = 0; j < depth; j++) {
							int other = kinds.get(j);
							int dx = positionsX[other] - x, dy = positionsY[other] - y;
							float ratio = Math.abs(dx) < size && Math.abs(dy) < size
									? overlapRatios[kind][other][dx + size - 1][dy + size - 1] : 0;
							if (ratio > 0.20f) {
								allowed = false;
								break;
							}
							overlaps |= ratio >= 0.10f;
						}
						if (!allowed || !overlaps) continue;
						positionsX[kind] = x;
						positionsY[kind] = y;
						if (++depth < kinds.size()) next[depth] = 0;
					}
					if (depth != kinds.size()) continue;

					int minX = span, maxX = 0, minY = span, maxY = 0;
					for (int kind : kinds) {
						minX = Math.min(minX, positionsX[kind]); maxX = Math.max(maxX, positionsX[kind]);
						minY = Math.min(minY, positionsY[kind]); maxY = Math.max(maxY, positionsY[kind]);
					}
					float centerX = (nominalSpan - minX - maxX) / 2f;
					float centerY = (nominalSpan - minY - maxY) / 2f;
					Arrays.fill(offsetX, UNPLACED);
					Arrays.fill(offsetY, UNPLACED);
					for (int kind : kinds) {
						offsetX[kind] = (positionsX[kind] + centerX) * INVENTORY_SCALE;
						offsetY[kind] = (positionsY[kind] + centerY) * INVENTORY_SCALE;
					}
					// 记录实际跨度，使保存的位置能在下次载入时保持稳定。
					layoutSpan = Math.max(nominalSpan, Math.max(maxX - minX, maxY - minY));
					return;
				}
			}
			// 耗尽整体预算后保留已有外观，仅为新种类填入有界随机偏移。
			// 此降级只保证位置可用，不承诺满足严格的重叠比例。
			for (int kind : kinds) {
				if (offsetX[kind] == UNPLACED) offsetX[kind] = Random.Int(nominalSpan + 1) * INVENTORY_SCALE;
				if (offsetY[kind] == UNPLACED) offsetY[kind] = Random.Int(nominalSpan + 1) * INVENTORY_SCALE;
			}
			layoutSpan = Math.max(layoutSpan, nominalSpan);
		} finally {
			Random.popGenerator();
		}
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("gem_counts", counts);
		bundle.put("gem_offset_x", offsetX);
		bundle.put("gem_offset_y", offsetY);
		bundle.put("gem_layout_span", layoutSpan);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		counts = bundle.getIntArray("gem_counts");
		offsetX = bundle.getFloatArray("gem_offset_x");
		offsetY = bundle.getFloatArray("gem_offset_y");
		layoutSpan = bundle.getInt("gem_layout_span");
		thrownType = -1;
		layout();
	}
}
