package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM100;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Piranha;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.Random;
import com.watabou.utils.RectF;

import java.util.HashSet;

// 雷属性击杀时，把死亡瞬间的精灵帧做成残影，随后碎裂、漂移并淡出。
public class ElectrifiedDeath extends Image {

	private static final float DURATION = 3f;
	private static final float DRIFT_DISTANCE = 8f;
	private static final float PIXEL_LOSS = 0.2f;

	private static final HashSet<Class> LIGHTNING_SOURCES = new HashSet<>();
	static {
		LIGHTNING_SOURCES.addAll(Char.Property.ELECTRIC.resistances());
		LIGHTNING_SOURCES.add(DM100.LightningBolt.class);
	}

	private static int nextKey = 0;

	private final Object textureKey;
	private float lifetime = 0f;

	private ElectrifiedDeath(int[] pixels, int width, int height, boolean flipHorizontal) {
		super();
		this.flipHorizontal = flipHorizontal;
		textureKey = "electrified-death-" + (nextKey++);
		texture(TextureCache.createPixels(textureKey, width, height, pixels));
		frame(0, 0, width, height);

		float angle = Random.Float((float)(Math.PI * 2));
		speed.set((float)Math.cos(angle) * DRIFT_DISTANCE / DURATION,
				(float)Math.sin(angle) * DRIFT_DISTANCE / DURATION);
	}

	// 返回 true 表示已经用残影替代了原本的死亡动画。
	public static boolean trySpawn(Char ch, Object source) {
		if (!(ch instanceof Mob) || ch.sprite == null || !ch.sprite.visible || source == null) {
			return false;
		}

		Class<?> sourceClass = Char.damageSourceClass(source);
		boolean lightning = false;
		for (Class c : LIGHTNING_SOURCES) {
			if (c.isAssignableFrom(sourceClass)) {
				lightning = true;
				break;
			}
		}
		if (!lightning) return false;

		CharSprite sprite = ch.sprite;
		if (sprite.texture == null || sprite.texture.bitmap == null) return false;

		RectF frame = sprite.frame();
		int left = Math.round(frame.left * sprite.texture.width);
		int top = Math.round(frame.top * sprite.texture.height);
		int width = Math.round(frame.width() * sprite.texture.width);
		int height = Math.round(frame.height() * sprite.texture.height);
		if (width <= 0 || height <= 0 || left < 0 || top < 0
				|| left + width > sprite.texture.width || top + height > sprite.texture.height) {
			return false;
		}

		// 食人鱼这类水生怪物保留原色，其余怪物染成亮黄白。
		boolean recolor = !(ch instanceof Piranha);
		int[] pixels = new int[width * height];
		int[] opaque = new int[width * height];
		int opaqueCount = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int pixel = sprite.texture.bitmap.getPixel(left + x, top + y);
				int alpha = pixel & 0xFF;
				if (alpha != 0) {
					if (recolor) {
						int r = (pixel >>> 24) & 0xFF;
						int g = (pixel >>> 16) & 0xFF;
						int b = (pixel >>> 8) & 0xFF;
						int luminance = (r * 30 + g * 59 + b * 11) / 100;
						int green = 235 + Math.round(20 * luminance / 255f);
						int blue = 80 + Math.round(175 * luminance / 255f);
						pixel = (255 << 24) | (green << 16) | (blue << 8) | alpha;
					}
					opaque[opaqueCount++] = y * width + x;
				}
				pixels[y * width + x] = pixel;
			}
		}

		int toRemove = Math.round(opaqueCount * PIXEL_LOSS);
		for (int i = 0; i < toRemove; i++) {
			int j = i + Random.Int(opaqueCount - i);
			int tmp = opaque[i];
			opaque[i] = opaque[j];
			opaque[j] = tmp;
			pixels[opaque[i]] = 0;
		}

		ElectrifiedDeath effect = new ElectrifiedDeath(pixels, width, height, sprite.flipHorizontal);
		effect.scale.set(sprite.scale);
		effect.x = sprite.x;
		effect.y = sprite.y;
		GameScene.effect(effect);
		sprite.killAndErase();
		return true;
	}

	@Override
	public void update() {
		super.update();
		lifetime += Game.elapsed;
		if (lifetime >= DURATION) {
			killAndErase();
			TextureCache.remove(textureKey);
		} else {
			alpha(1f - lifetime / DURATION);
		}
	}

	@Override
	public void destroy() {
		TextureCache.remove(textureKey);
		super.destroy();
	}
}
