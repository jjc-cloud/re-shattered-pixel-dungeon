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

package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.services.news.News;
import com.shatteredpixel.shatteredpixeldungeon.services.news.NewsArticle;
import com.shatteredpixel.shatteredpixeldungeon.ui.ExitButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.ui.TitleBackground;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.watabou.noosa.Camera;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.RectF;

import java.util.ArrayList;

public class NewsScene extends PixelScene {

	@Override
	public void create() {
		super.create();
		uiCamera.visible = false;

		int w = Camera.main.width;
		int h = Camera.main.height;
		RectF insets = getCommonInsets();
		add(new TitleBackground(w, h));
		w -= insets.left + insets.right;
		h -= insets.top + insets.bottom;

		IconTitle title = new IconTitle(Icons.NEWS.get(), Messages.get(this, "title"));
		title.setSize(w - 40, 0);
		title.setPos(insets.left + (w - title.reqWidth()) / 2f, insets.top + 2);
		align(title);
		add(title);

		NinePatch panel = Chrome.get(Chrome.Type.TOAST);
		panel.size(Math.min(w - 8, landscape() ? 280 : 220), h - 28);
		panel.x = insets.left + (w - panel.width()) / 2f;
		panel.y = insets.top + 24;
		align(panel);

		ArrayList<ArticleButton> buttons = new ArrayList<>();
		ScrollPane list = new ScrollPane(new Component()) {
			@Override
			public void onClick(float x, float y) {
				for (ArticleButton button : buttons) {
					if (button.inside(x, y)) {
						button.onClick();
						return;
					}
				}
			}
		};
		add(list);
		Component content = list.content();
		int textWidth = (int)panel.innerWidth();
		float cardWidth = (textWidth - 2) / 2f;
		News.checkForNews();
		for (NewsArticle article : News.articles()) {
			ArticleButton button = new ArticleButton(article);
			button.setRect(0, 0, cardWidth, 0);
			buttons.add(button);
			content.add(button);
		}
		float top = 0;
		for (int i = 0; i < buttons.size(); i += 2) {
			ArticleButton left = buttons.get(i);
			ArticleButton right = i + 1 < buttons.size() ? buttons.get(i + 1) : null;
			float rowHeight = right == null ? left.height() : Math.max(left.height(), right.height());
			left.setRect(0, top, cardWidth, rowHeight);
			if (right != null) right.setRect(cardWidth + 2, top, cardWidth, rowHeight);
			top += rowHeight + 2;
		}
		if (!News.articlesAvailable()) {
			RenderedTextBlock empty = renderTextBlock(Messages.get(this, "no_articles"), 6);
			empty.maxWidth(textWidth);
			content.add(empty);
			top = empty.bottom() + 2;
		}
		content.setSize(textWidth, (int)Math.ceil(top));
		list.setRect(panel.x + panel.marginLeft(), panel.y + panel.marginTop() - 1,
				panel.innerWidth() + 2, panel.innerHeight() + 2);
		list.scrollTo(0, 0);

		ExitButton exit = new ExitButton();
		exit.setPos(insets.left + w - exit.width(), insets.top);
		add(exit);
	}

	@Override
	protected void onBackPressed() {
		ShatteredPixelDungeon.switchNoFade(TitleScene.class);
	}

	private static class ArticleButton extends StyledButton {
		private final NewsArticle article;
		private final RenderedTextBlock summary;
		private final RenderedTextBlock date;

		public ArticleButton(NewsArticle article) {
			super(Chrome.Type.GREY_BUTTON_TR, article.title, 6);
			this.article = article;
			icon(News.parseArticleIcon(article));
			textColor(article.date.getTime() > SPDSettings.newsLastRead() ? Window.SHPX_COLOR : Window.WHITE);
			summary = renderTextBlock(article.summary, 6);
			summary.setHightlighting(false);
			add(summary);
			date = renderTextBlock(News.parseArticleDate(article), 5);
			date.hardlight(0x888888);
			add(date);
		}

		@Override
		protected void layout() {
			super.layout();
			if (icon == null) return;
			float left = x + bg.marginLeft() + 2;
			float top = y + bg.marginTop() + 2;
			icon.x = left;
			icon.y = top;
			align(icon);
			text.maxWidth(Math.max(1, (int)(width - bg.marginHor() - icon.width() - 6)));
			text.setPos(left + icon.width() + 2, top);
			align(text);
			if (summary != null && date != null) {
				summary.maxWidth(Math.max(1, (int)(width - bg.marginHor() - 4)));
				summary.setPos(left, Math.max(text.bottom(), icon.y + icon.height()) + 4);
				date.maxWidth(Math.max(1, (int)(width - bg.marginHor() - 4)));
				date.setPos(left, summary.bottom() + 4);
				height = Math.max(height, date.bottom() - y + bg.marginBottom() + 2);
				bg.size(width, height);
			}
		}

		@Override
		protected void onClick() {
			super.onClick();
			textColor(Window.WHITE);
			SPDSettings.newsLastRead(Math.max(SPDSettings.newsLastRead(), article.date.getTime()));
			ShatteredPixelDungeon.scene().addToFront(new WndArticle(article));
		}
	}

	private static class WndArticle extends Window {
		public WndArticle(NewsArticle article) {
			super();
			int width = Math.min(landscape() ? 220 : 160, (int)uiCamera.width - 20);
			IconTitle title = new IconTitle(News.parseArticleIcon(article), article.title);
			title.setRect(0, 0, width, 0);
			add(title);
			RenderedTextBlock date = renderTextBlock(News.parseArticleDate(article), 6);
			date.hardlight(0x888888);
			date.setPos(0, title.bottom() + 2);
			add(date);

			ScrollPane scroll = new ScrollPane(new Component());
			add(scroll);
			RenderedTextBlock body = renderTextBlock(article.body, 6);
			body.maxWidth(width - 2);
			scroll.content().add(body);
			scroll.content().setSize(width, body.height() + 2);
			float top = date.bottom() + 4;
			int bodyHeight = (int)Math.ceil(Math.min(body.height() + 2, uiCamera.height - top - 20));
			resize(width, (int)Math.ceil(top) + bodyHeight);
			scroll.setRect(0, top, width, bodyHeight);
			scroll.scrollTo(0, 0);
		}
	}
}
