package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Petrification;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.PetrifiedStatueSprite;
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
		PetrifiedStatue statue = new PetrifiedStatue();
		statue.pos = victim.pos;
		statue.victimName = victim.name();
		CharSprite source = victim.sprite;
		if (source != null && source.texture != null) {
			source.idle();
			statue.imageWidth = Math.round(source.frame().width() * source.texture.width);
			statue.imageHeight = Math.round(source.frame().height() * source.texture.height);
			int left = Math.round(source.frame().left * source.texture.width);
			int top = Math.round(source.frame().top * source.texture.height);
			statue.pixels = new int[statue.imageWidth * statue.imageHeight];
			statue.flipped = source.flipHorizontal;
			for (int y = 0; y < statue.imageHeight; y++) for (int x = 0; x < statue.imageWidth; x++) {
				int pixel = source.texture.bitmap.getPixel(left + x, top + y);
				int gray = ((pixel >>> 24) * 30 + ((pixel >>> 16) & 255) * 59 + ((pixel >>> 8) & 255) * 11) / 100;
				statue.pixels[y * statue.imageWidth + x] = (gray << 24) | (gray << 16) | (gray << 8) | (pixel & 255);
			}
			source.killAndErase();
		}
		GameScene.add(statue);
		return statue;
	}

	@Override public CharSprite sprite() { return new PetrifiedStatueSprite(this); }
	@Override public String name() { return Messages.get(this, "name", victimName); }
	@Override public String description() { return Messages.get(this, "desc"); }
	@Override public boolean heroShouldInteract() { return false; }
	@Override public boolean interact(Char other) { return false; }
	@Override protected boolean act() { diactivate(); return true; }
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
	}
	@Override public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		victimName = bundle.getString("victim_name");
		imageWidth = bundle.getInt("image_width");
		imageHeight = bundle.getInt("image_height");
		pixels = bundle.getIntArray("pixels");
		flipped = bundle.getBoolean("flipped");
		// Repair saves made before statues ignored alarms and monster AI.
		alignment = Alignment.NEUTRAL;
		state = PASSIVE;
		enemy = null;
		enemyID = -1;
		target = -1;
		alerted = false;
	}
}
