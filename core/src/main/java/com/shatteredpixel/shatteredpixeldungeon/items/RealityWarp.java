/*
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

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/**
 * 扭曲现实：记录天赋选定的药水/卷轴互换组合。
 * 实际道具始终负责显示、鉴定、消耗和统计；临时代理只提供互换后的效果类型。
 * 炼金同样只替换材料视角，产物不做二次转换。
 */
public class RealityWarp {

	private static Class<? extends Potion> potionA;
	private static Class<? extends Potion> potionB;
	private static Class<? extends Scroll> scrollA;
	private static Class<? extends Scroll> scrollB;

	public static boolean potionSwapped() {
		return potionA != null && potionB != null;
	}

	public static boolean scrollSwapped() {
		return scrollA != null && scrollB != null;
	}

	public static void reset() {
		potionA = null;
		potionB = null;
		scrollA = null;
		scrollB = null;
	}

	public static void setPotionSwap(Class<? extends Potion> a, Class<? extends Potion> b) {
		potionA = a;
		potionB = b;
	}

	public static void setScrollSwap(Class<? extends Scroll> a, Class<? extends Scroll> b) {
		scrollA = a;
		scrollB = b;
	}

	public static Class<? extends Potion> potionSwapA() {
		return potionA;
	}

	public static Class<? extends Potion> potionSwapB() {
		return potionB;
	}

	public static Class<? extends Scroll> scrollSwapA() {
		return scrollA;
	}

	public static Class<? extends Scroll> scrollSwapB() {
		return scrollB;
	}

	//该药水类被使用时的等效种类（未参与互换时返回null）
	@SuppressWarnings("unchecked")
	public static Class<? extends Potion> potionPartner(Class<? extends Potion> cls) {
		if (!potionSwapped()) return null;
		if (cls == potionA) return potionB;
		if (cls == potionB) return potionA;
		return null;
	}

	//该卷轴类被使用时的等效种类（未参与互换时返回null）
	@SuppressWarnings("unchecked")
	public static Class<? extends Scroll> scrollPartner(Class<? extends Scroll> cls) {
		if (!scrollSwapped()) return null;
		if (cls == scrollA) return scrollB;
		if (cls == scrollB) return scrollA;
		return null;
	}

	//解析任意药水类的实际等效身份，未参与互换时原样返回
	public static Class<? extends Potion> resolvePotion(Class<? extends Potion> cls) {
		Class<? extends Potion> partner = potionPartner(cls);
		return partner != null ? partner : cls;
	}

	//解析任意卷轴类的实际等效身份，未参与互换时原样返回
	public static Class<? extends Scroll> resolveScroll(Class<? extends Scroll> cls) {
		Class<? extends Scroll> partner = scrollPartner(cls);
		return partner != null ? partner : cls;
	}

	//返回绑定到原道具的效果代理；未参与互换时返回原道具
	public static Potion potionEffect(Potion potion) {
		Class<? extends Potion> partner = potionPartner(potion.getClass());
		if (partner == null) return potion;
		return Reflection.newInstance(partner).effectOf(potion);
	}

	public static Scroll scrollEffect(Scroll scroll) {
		Class<? extends Scroll> partner = scrollPartner(scroll.getClass());
		if (partner == null) return scroll;
		return Reflection.newInstance(partner).effectOf(scroll);
	}

	public static void applyPotionEffect(Potion potion, Hero hero) {
		potionEffect(potion).apply(hero);
	}

	public static void shatterPotionEffect(Potion potion, int cell) {
		potionEffect(potion).shatter(cell);
	}

	/** Creates the class view recipes should see while retaining the physical source item. */
	public static ArrayList<Item> alchemyIngredients(ArrayList<Item> ingredients) {
		ArrayList<Item> result = new ArrayList<>(ingredients.size());
		for (Item item : ingredients) {
			if (item instanceof Potion) result.add(potionEffect((Potion)item));
			else if (item instanceof Scroll) result.add(scrollEffect((Scroll)item));
			else result.add(item);
		}
		return result;
	}

	/** Converts a canonical guide ingredient into the physical item currently required. */
	@SuppressWarnings("unchecked")
	public static Item recipeIngredient(Item ingredient) {
		Item result = ingredient;
		if (ingredient instanceof Potion) {
			Class<? extends Potion> partner = potionPartner((Class<? extends Potion>)ingredient.getClass());
			if (partner != null) result = Reflection.newInstance(partner);
		} else if (ingredient instanceof Scroll) {
			Class<? extends Scroll> partner = scrollPartner((Class<? extends Scroll>)ingredient.getClass());
			if (partner != null) result = Reflection.newInstance(partner);
		}
		result.quantity(ingredient.quantity());
		return result;
	}

	public static ArrayList<Item> recipeIngredients(ArrayList<Item> ingredients) {
		ArrayList<Item> result = new ArrayList<>(ingredients.size());
		for (Item ingredient : ingredients) result.add(recipeIngredient(ingredient));
		return result;
	}

	private static final String POTION_A = "potion_a";
	private static final String POTION_B = "potion_b";
	private static final String SCROLL_A = "scroll_a";
	private static final String SCROLL_B = "scroll_b";

	public static void storeInBundle(Bundle bundle) {
		bundle.put(POTION_A, potionA);
		bundle.put(POTION_B, potionB);
		bundle.put(SCROLL_A, scrollA);
		bundle.put(SCROLL_B, scrollB);
	}

	public static void restoreFromBundle(Bundle bundle) {
		reset();
		potionA = bundle.getClass(POTION_A);
		potionB = bundle.getClass(POTION_B);
		scrollA = bundle.getClass(SCROLL_A);
		scrollB = bundle.getClass(SCROLL_B);
	}
}
