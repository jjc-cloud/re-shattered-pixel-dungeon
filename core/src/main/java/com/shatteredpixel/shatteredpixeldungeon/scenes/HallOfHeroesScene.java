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
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndRanking;
import com.watabou.noosa.Camera;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.RectF;

import java.util.List;

public class HallOfHeroesScene extends PixelScene {

	private static final int ROW_HEIGHT = 20;
	private static final int PAGE_BUTTON_SIZE = 20;
	private static final int PAGE_BUTTON_GAP = 8;

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
		float footerTop = insets.top + height - PAGE_BUTTON_SIZE - 4;

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
			int listWidth = (int)Math.min(160, width) - 8;
			int rowWidth = listWidth - 2;
			float left = align(insets.left + (width - listWidth) / 2);
			float top = align(Math.max(insets.top + 24, title.bottom() + 6));
			Component content = new Component();
			content.active = false;
			ScrollPane list = new ScrollPane(content) {
				@Override
				public void onClick(float x, float y) {
					int index = (int)(y / ROW_HEIGHT);
					if (x >= 0 && x < rowWidth && y >= 0 && index < records.size()) {
						HallOfHeroesScene.this.addToFront(new WndRanking(records.get(index), page));
					}
				}
			};
			for (int index = 0; index < records.size(); index++) {
				RankingsScene.Record row = new RankingsScene.Record(index, false, records.get(index));
				row.setRect(0, (float)ROW_HEIGHT * index, rowWidth, ROW_HEIGHT);
				content.add(row);
			}
			content.setSize(listWidth, (float)ROW_HEIGHT * records.size());
			add(list);
			list.setRect(left, top, listWidth, (int)(footerTop - top - 6));
		}

		ExitButton exit = new ExitButton() {
			@Override
			protected void onClick() {
				onBackPressed();
			}
		};
		exit.setPos(insets.left + width - exit.width(), insets.top);
		add(exit);

		RenderedTextBlock indicator = renderTextBlock(Messages.get(this, "page", page + 1, Rankings.HALL_PAGES), 8);
		indicator.hardlight(0x3399FF);
		indicator.setPos(insets.left + (width - indicator.width()) / 2,
				footerTop + (PAGE_BUTTON_SIZE - indicator.height()) / 2);
		align(indicator);
		add(indicator);
		addPageButton(Icons.LEFTARROW, -1, indicator.left() - PAGE_BUTTON_GAP - PAGE_BUTTON_SIZE, footerTop);
		addPageButton(Icons.RIGHTARROW, 1, indicator.right() + PAGE_BUTTON_GAP, footerTop);
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
		button.setRect(x, y, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE);
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
