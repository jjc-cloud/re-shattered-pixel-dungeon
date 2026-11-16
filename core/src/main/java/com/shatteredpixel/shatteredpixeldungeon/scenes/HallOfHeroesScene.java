package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Rankings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.Archs;
import com.shatteredpixel.shatteredpixeldungeon.ui.ExitButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndRanking;
import com.watabou.noosa.Camera;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.RectF;

import java.util.List;

public class HallOfHeroesScene extends PixelScene {

	private static int page;

	@Override
	public void create() {
		super.create();
		Music.INSTANCE.playTracks(new String[]{Assets.Music.THEME_1, Assets.Music.THEME_2}, new float[]{1, 1}, false);
		uiCamera.visible = false;

		Archs background = new Archs();
		background.setSize(Camera.main.width, Camera.main.height);
		add(background);
		RectF insets = getCommonInsets();
		float width = Camera.main.width - insets.left - insets.right;
		float height = Camera.main.height - insets.top - insets.bottom;

		IconTitle title = new IconTitle(new ItemSprite(ItemSpriteSheet.AMULET, null), Messages.get(this, "title"));
		title.setSize(200, 0);
		title.setPos(insets.left + (width - title.reqWidth()) / 2, insets.top + (20 - title.height()) / 2);
		align(title);
		add(title);

		List<Rankings.Record> records = Rankings.INSTANCE.hallPage(page);
		if (records.isEmpty()) {
			RenderedTextBlock empty = renderTextBlock(Messages.get(this, "no_games"), 8);
			empty.hardlight(0xCCCCCC);
			empty.setPos(insets.left + (width - empty.width()) / 2, insets.top + (height - empty.height()) / 2);
			align(empty);
			add(empty);
		} else {
			float rowHeight = Math.min(20, (height - 40) / records.size());
			float rowWidth = Math.min(160, width) - 8;
			float left = insets.left + (width - rowWidth) / 2;
			float top = insets.top + (height - rowHeight * records.size()) / 2;
			for (int index = 0; index < records.size(); index++) {
				Rankings.Record record = records.get(index);
				RankingsScene.Record row = new RankingsScene.Record(index, false, record) {
					@Override
					protected void onClick() {
						HallOfHeroesScene.this.addToFront(new WndRanking(record, page));
					}
				};
				row.setRect(left, top + rowHeight * index, rowWidth, rowHeight);
				add(row);
			}
		}

		ExitButton exit = new ExitButton() {
			@Override
			protected void onClick() {
				onBackPressed();
			}
		};
		exit.setPos(insets.left + width - exit.width(), insets.top);
		add(exit);

		float bottom = insets.top + height;
		RenderedTextBlock indicator = renderTextBlock(Messages.get(this, "page", page + 1, Rankings.HALL_PAGES), 8);
		indicator.hardlight(0x3399FF);
		indicator.setPos(insets.left + (width - indicator.width()) / 2, bottom - indicator.height() - 8);
		align(indicator);
		add(indicator);
		addPageButton(Icons.LEFTARROW, -1, insets.left + 8, bottom - 21);
		addPageButton(Icons.RIGHTARROW, 1, insets.left + width - 28, bottom - 21);
		fadeIn();
	}

	private void addPageButton(Icons icon, int direction, float x, float y) {
		int next = (page + direction + Rankings.HALL_PAGES) % Rankings.HALL_PAGES;
		IconButton button = new IconButton(icon.get()) {
			@Override
			protected void onClick() {
				page = next;
				ShatteredPixelDungeon.switchNoFade(HallOfHeroesScene.class);
			}
		};
		button.setRect(x, y, 20, 20);
		add(button);
	}

	@Override
		protected void onBackPressed() {
		ShatteredPixelDungeon.switchNoFade(RankingsScene.class);
	}

	@Override
	public void destroy() {
		super.destroy();
		Dungeon.daily = Dungeon.dailyReplay = false;
	}
}
