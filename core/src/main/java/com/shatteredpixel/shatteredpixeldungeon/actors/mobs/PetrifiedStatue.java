package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Petrification;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.PetrifiedStatueSprite;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.utils.Bundle;

/** An inert, breakable occupant, with a saved snapshot rather than a living victim. */
public class PetrifiedStatue extends Mob {
	private String victimName = "";
	public int imageWidth = 1, imageHeight = 1;
	public int[] pixels = {0x888888FF};
	public boolean flipped;
	{
		HP = HT = 1;
		EXP = 0;
		alignment = Alignment.NEUTRAL;
		state = PASSIVE;
		properties.add(Property.IMMOVABLE);
		immunities.add(Petrification.class);
	}

	public static PetrifiedStatue leave(Char victim) {
		if (Dungeon.level == null) return null;
		if (Dungeon.level.pit[victim.pos]) {
			if (victim.sprite != null) victim.sprite.petrifiedFall();
			return null;
		}
		PetrifiedStatue statue = from(victim);
		statue.pos = victim.pos;
		if (victim.sprite != null) victim.sprite.killAndErase();
		GameScene.add(statue);
		statue.throwAllItems();
		return statue;
	}

	/** Creates an inert statue snapshot without adding either object to the current scene. */
	public static PetrifiedStatue from(Char subject) {
		return from(subject, false, subject.sprite != null && subject.sprite.flipHorizontal);
	}

	/** 截取当前动作；未放置的生物先选择待机或攻击动作。朝向由调用方指定。 */
	public static PetrifiedStatue from(Char subject, boolean attacking, boolean flipped) {
		PetrifiedStatue statue = new PetrifiedStatue();
		statue.victimName = subject.name();
		CharSprite source = subject.sprite;
		boolean temporarySprite = source == null;
		if (temporarySprite && subject instanceof Mob) {
			source = ((Mob)subject).sprite();
			source.linkVisuals(subject);
			if (attacking) {
				source.ch = subject;
				source.attack(subject.pos + (flipped ? -1 : 1));
			}
		}
		if (source != null && source.texture != null) {
			statue.capture(source.texture, Math.round(source.frame().left * source.texture.width),
					Math.round(source.frame().top * source.texture.height),
					Math.round(source.frame().width() * source.texture.width),
					Math.round(source.frame().height() * source.texture.height));
			statue.flipped = flipped;
		}
		if (temporarySprite && source != null) source.destroy();
		return statue;
	}

	/** Creates a non-living, breakable statue from a pixel rectangle in an asset texture. */
	public static PetrifiedStatue fromTexture(Object texture, int left, int top,
			int width, int height, String name, boolean flipped) {
		PetrifiedStatue statue = new PetrifiedStatue();
		statue.victimName = name;
		statue.properties.add(Property.OBJECT);
		statue.flipped = flipped;
		statue.capture(TextureCache.get(texture), left, top, width, height);
		return statue;
	}

	private void capture(SmartTexture texture, int left, int top, int width, int height) {
		if (width <= 0 || height <= 0 || left < 0 || top < 0
				|| left + width > texture.width || top + height > texture.height) {
			throw new IllegalArgumentException("Invalid statue texture rectangle");
		}
		imageWidth = width;
		imageHeight = height;
		pixels = new int[imageWidth * imageHeight];
		for (int y = 0; y < imageHeight; y++) for (int x = 0; x < imageWidth; x++) {
			int pixel = texture.bitmap.getPixel(left + x, top + y);
			int gray = ((pixel >>> 24) * 30 + ((pixel >>> 16) & 255) * 59 + ((pixel >>> 8) & 255) * 11) / 100;
			pixels[y * imageWidth + x] = (gray << 24) | (gray << 16) | (gray << 8) | (pixel & 255);
		}
	}

	public boolean hasLivingOrigin() {
		return !properties.contains(Property.OBJECT);
	}

	private void throwAllItems() {
		Heap heap = Dungeon.level.heaps.get(pos);
		while (heap != null && !heap.isEmpty()) {
			int oldSize = heap.size();
			throwItems();
			Heap remaining = Dungeon.level.heaps.get(pos);
			if (remaining == heap && remaining.size() == oldSize) break;
			heap = remaining;
		}
	}

	@Override public CharSprite sprite() { return new PetrifiedStatueSprite(this); }
	@Override public String name() { return Messages.get(this, "name", victimName); }
	@Override public String description() { return Messages.get(this, hasLivingOrigin() ? "desc" : "desc_object"); }
	@Override public boolean heroShouldInteract() { return false; }
	@Override public boolean interact(Char other) { return false; }
	@Override protected boolean act() { throwAllItems(); diactivate(); return true; }
	@Override protected void onAdd() { }
	@Override public void aggro(Char enemy) { }
	@Override public void beckon(int cell) { }
	@Override public void notice() { }
	@Override public float spawningWeight() { return 0f; }
	@Override public boolean surprisedBy(Char enemy, boolean attacking) { return false; }
	@Override public int defenseProc(Char enemy, int damage) { return damage; }
	@Override public boolean add(Buff buff) {
		// Damage-over-time sources can still break stone; control effects cannot animate it.
		if (buff instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning
				|| buff instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison
				|| buff instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion
				|| buff instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding
				|| buff instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze) return super.add(buff);
		return false;
	}
	@Override public int defenseSkill(Char attacker) { return 0; }
	@Override public int drRoll() { return 0; }
	@Override public void damage(int damage, Object source) {
		if (damage >= 0 && isAlive()) die(source);
	}
	@Override public void destroy() {
		HP = 0;
		Actor.remove(this);
		if (Dungeon.level != null) Dungeon.level.mobs.remove(this);
	}
	@Override public void die(Object source) {
		destroy();
		if (sprite != null) sprite.die();
	}
	@Override public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("victim_name", victimName);
		bundle.put("image_width", imageWidth);
		bundle.put("image_height", imageHeight);
		bundle.put("pixels", pixels);
		bundle.put("flipped", flipped);
		bundle.put("living_origin", hasLivingOrigin());
	}
	@Override public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		victimName = bundle.getString("victim_name");
		imageWidth = bundle.getInt("image_width");
		imageHeight = bundle.getInt("image_height");
		pixels = bundle.getIntArray("pixels");
		flipped = bundle.getBoolean("flipped");
		if (!bundle.getBoolean("living_origin")) properties.add(Property.OBJECT);
		alignment = Alignment.NEUTRAL;
		state = PASSIVE;
		enemy = null;
		enemyID = -1;
		target = -1;
		alerted = false;
	}
}
