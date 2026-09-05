package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Berserk;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;

/** Dedicated berserker rage control; deliberately independent from ActionIndicator. */
public class RageIndicator extends Tag {

	private Visual icon;
	private BitmapText value;

	public RageIndicator() {
		super(0x664400);
		setSize(SIZE, SIZE);
		visible = false;
	}

	@Override
	protected void createChildren() {
		super.createChildren();
		value = new BitmapText(PixelScene.pixelFont);
		add(value);
	}

	@Override
	public void update() {
		super.update();
		Berserk rage = Dungeon.hero == null ? null : Dungeon.hero.buff(Berserk.class);
		visible = rage != null;
		if (!visible) return;
		if (icon == null) {
			icon = new HeroIcon(rage);
			add(icon);
		}
		value.text(Math.round(rage.power() * 100f) + "%");
		value.measure();
		setColor(rage.indicatorColor());
		if (icon instanceof Image) rage.tintIcon((Image) icon);
		layout();
		icon.alpha(Dungeon.hero.ready ? 1f : 0.5f);
		value.alpha(Dungeon.hero.ready ? 1f : 0.5f);
	}

	@Override
	protected void layout() {
		super.layout();
		if (icon == null || value == null) return;
		icon.x = x + (width - icon.width()) / 2f;
		icon.y = y + (height - icon.height()) / 2f;
		value.x = x + width - value.width() - 1;
		value.y = y + height - value.baseLine() - 1;
		PixelScene.align(icon);
		PixelScene.align(value);
	}

	@Override
	protected void onClick() {
		super.onClick();
		Berserk rage = Dungeon.hero == null ? null : Dungeon.hero.buff(Berserk.class);
		if (rage != null && Dungeon.hero.ready) rage.doAction();
	}

	@Override
	protected String hoverText() {
		Berserk rage = Dungeon.hero == null ? null : Dungeon.hero.buff(Berserk.class);
		return rage == null ? null : Messages.titleCase(rage.actionName());
	}
}
