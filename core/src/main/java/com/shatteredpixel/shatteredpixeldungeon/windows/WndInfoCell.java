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

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.CellContent;
import com.shatteredpixel.shatteredpixeldungeon.levels.CellState;
import com.shatteredpixel.shatteredpixeldungeon.levels.CaveCollapse;
import com.shatteredpixel.shatteredpixeldungeon.levels.CavesLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.AmbitiousImpRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CellPreview;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.custom.Carpet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.custom.VaultTiles;
import com.shatteredpixel.shatteredpixeldungeon.tiles.TerrainFeaturesTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Image;
import com.watabou.noosa.ui.Component;
import java.util.ArrayList;

public class WndInfoCell extends Window {
    private static final int WIDTH = 120;
    private static final int CONTENT_SLOT = 24;
    private static final int TEXT_MARGIN = 2;
    private ScrollPane scroll;
    public static Image cellImage(int cell) { return new CellPreview(cell); }
    public static String cellName(int cell) {
        return Dungeon.level.tileName(Dungeon.level.cellAt(cell).ground());
    }

    public WndInfoCell(int cell) {
        CellState state = Dungeon.level.cellAt(cell);
        Image preview = cellImage(cell);
        IconTitle titlebar = new IconTitle(preview, Messages.get(GameScene.class, "examine"));
        titlebar.setRect(TEXT_MARGIN, TEXT_MARGIN, WIDTH - 2 * TEXT_MARGIN, 0);
        add(titlebar);

        ArrayList<Object> objects = GameScene.getObjectsAtCell(cell);
        ArrayList<Object> contents = new ArrayList<>(objects);
        StringBuilder desc = new StringBuilder();
        int top = state.terrain();
        boolean coveredGround = state.buried() || DungeonTileSheet.wallStitcheable(top);
        if (!coveredGround) {
            desc.append(Messages.get(this, "ground", Dungeon.level.tileName(state.ground())));
            String groundDesc = Dungeon.level.tileDesc(state.ground());
            if (groundDesc != null && !groundDesc.isEmpty()) desc.append("\n").append(groundDesc);
        }
        ArrayList<String> contentNames = new ArrayList<>();
        ArrayList<String> details = new ArrayList<>();
        boolean carpetDescribed = false;
        for (CellContent content : state.contents()) {
            if (!state.exposed(content) || content.type() == CellContent.HIDDEN_ENTRANCE
                    || CellContent.family(content.type()) == Terrain.TRAP) continue;
            contents.add(content);
            if (content.type() == CellContent.CARPET) {
                if (!carpetDescribed) {
                    contentNames.add(Messages.get(Carpet.class, "name"));
                    details.add(Messages.get(Carpet.class, "desc"));
                    carpetDescribed = true;
                }
                continue;
            }
            String name = Dungeon.level.tileName(content.type());
            String detail = Dungeon.level.tileDesc(content.type());
            if (!contentNames.contains(name)) {
                contentNames.add(name);
            }
            if (detail != null && !detail.isEmpty()) {
                details.add(detail);
            }
        }
        if (!carpetDescribed && !coveredGround) {
            for (int side = 0; side < Carpet.SEAM_PIECES; side++) {
                if (Carpet.seamPiece(cell, side) < 0) continue;
                contentNames.add(Messages.get(this, "carpet"));
                details.add(Messages.get(this, "carpet_desc"));
                contents.add(CellContent.CARPET);
                break;
            }
        }
        ArrayList<String> objectNames = GameScene.getObjectNames(objects);
        for (String name : objectNames) {
            if (!contentNames.contains(name)) {
                contentNames.add(name);
            }
        }
        boolean contentsStarted = !contentNames.isEmpty();
        if (contentsStarted) {
            if (desc.length() > 0) desc.append("\n\n");
            desc.append(Messages.get(this, "contents"));
            for (int i = 0; i < contentNames.size(); i++) {
                desc.append(" _").append(contentNames.get(i)).append("_");
                if (i < contentNames.size() - 1) desc.append("、");
            }
        }
        if (!details.isEmpty()) {
            desc.append("\n\n");
            for (int i = 0; i < details.size(); i++) {
                if (i > 0) desc.append("\n\n");
                desc.append(details.get(i));
            }
        }
        int x = cell % Dungeon.level.width(), y = cell / Dungeon.level.width();
        ArrayList<CustomTilemap> custom = new ArrayList<>(Dungeon.level.customTiles);
        custom.addAll(Dungeon.level.customTerrain);
        for (CustomTilemap tile : custom) {
            if (tile instanceof Carpet || (carpetDescribed && (tile instanceof VaultTiles.EntranceRug
                    || tile instanceof AmbitiousImpRoom.QuestEntrance))) continue;
            int tx = x - tile.tileX, ty = y - tile.tileY;
            if (tx < 0 || ty < 0 || tx >= tile.tileW || ty >= tile.tileH) continue;
            String detail = tile.desc(tx, ty);
            if (detail != null && !detail.isEmpty() && !details.contains(detail)) {
                if (desc.length() > 0) desc.append("\n\n");
                desc.append(detail);
            }
        }
        for (Blob blob : Dungeon.level.blobs.values()) {
            if ((Dungeon.level.heroFOV[cell] || blob.alwaysVisible) && blob.volume > 0
                    && blob.cur != null && blob.cur[cell] > 0 && blob.tileDesc() != null) {
                if (desc.length() > 0) desc.append("\n\n");
                desc.append(blob.tileDesc());
            }
        }
        if (Dungeon.level.caveCollapse != null && Dungeon.level.caveCollapse.contains(cell) && !state.buried())
            desc.append("\n\n").append(Messages.get(CaveCollapse.class, "ground_desc"));
        if (Dungeon.level.caveCollapse != null && Dungeon.level.caveCollapse.hasGold(cell))
            desc.append("\n\n").append(Messages.get(CavesLevel.class, "wall_deco_desc"));
        if (desc.length() == 0) desc.append(Messages.get(this, "nothing"));
        if (!objects.isEmpty() && !contentsStarted) {
            if (desc.length() > 0) desc.append("\n\n");
            desc.append(Messages.get(this, "contents"));
        }

        Component body = new Component();
        body.setSize(WIDTH, 0);
        // ScrollPane 的 PointerArea 后注册会先接收事件。
        // 先创建滚动容器，再创建条目按钮，按钮才能点击。
        scroll = new ScrollPane(body);
        add(scroll);
        RenderedTextBlock info = PixelScene.renderTextBlock(desc.toString(), 6);
        // 字符和描边可能超出测量框，避开滚动相机的裁切边界。
        info.maxWidth(WIDTH - 2 * TEXT_MARGIN);
        info.setPos(TEXT_MARGIN, TEXT_MARGIN);
        body.add(info);
        float iconTop = info.bottom() + 4;
        int contentCount = 0;
        for (Object object : contents) {
            final Object content = object;
            Image icon;
            if (object == Dungeon.hero) {
                icon = HeroSprite.avatar((Hero)object);
            } else if (object instanceof Char && ((Char)object).sprite != null) {
                icon = new Image(((Char)object).sprite);
            } else if (object instanceof Mob) {
                CharSprite sprite = ((Mob)object).sprite();
                sprite.idle();
                icon = new Image(sprite);
            } else if (object instanceof Heap) {
                icon = new ItemSprite((Heap)object);
            } else if (object instanceof Item) {
                icon = new ItemSprite((Item)object);
            } else if (object instanceof Plant) {
                icon = TerrainFeaturesTilemap.getPlantVisual((Plant)object);
            } else if (object instanceof Trap) {
                icon = TerrainFeaturesTilemap.getTrapVisual((Trap)object);
            } else if (object instanceof CellContent || object instanceof Integer) {
                int type = object instanceof CellContent ? ((CellContent)object).type() : (Integer)object;
                icon = new CellPreview(cell, type);
            } else {
                continue;
            }
            if (!(icon instanceof CellPreview)) icon = new CellPreview(cell, icon);
            IconButton button = new IconButton(icon) {
                @Override protected void onClick() {
                    if (content instanceof CellContent || content instanceof Integer) {
                        int type = content instanceof CellContent ? ((CellContent)content).type() : (Integer)content;
                        String name;
                        String detail;
                        if (type == CellContent.CARPET) {
                            boolean intact = state.hasTerrain(CellContent.CARPET);
                            name = intact ? Messages.get(Carpet.class, "name") : Messages.get(WndInfoCell.this, "carpet");
                            detail = intact ? Messages.get(Carpet.class, "desc") : Messages.get(WndInfoCell.this, "carpet_desc");
                        } else {
                            name = Dungeon.level.tileName(type);
                            detail = Dungeon.level.tileDesc(type);
                            if (type == Terrain.GRASS && (detail == null || detail.isEmpty()))
                                detail = Messages.get(Level.Feeling.class, "grass_desc");
                            if (detail == null || detail.isEmpty()) detail = Messages.get(WndInfoCell.this, "nothing");
                        }
                        GameScene.show(new WndTitledMessage(new CellPreview(cell, type), name, detail));
                    } else {
                        GameScene.examineObject(content);
                    }
                }
            };
            button.setRect(contentCount % 5 * CONTENT_SLOT,
                    iconTop + contentCount / 5 * CONTENT_SLOT,
                    CONTENT_SLOT, CONTENT_SLOT);
            body.add(button);
            contentCount++;
        }
        int rows = (contentCount + 4) / 5;
        float bottom = iconTop + rows * CONTENT_SLOT + TEXT_MARGIN;
        body.setSize(WIDTH, bottom);
        float topOffset = titlebar.bottom() + 4;
        int height = (int)Math.ceil(Math.max(CONTENT_SLOT, Math.min(bottom,
                PixelScene.uiCamera.height - topOffset - 30)));
        // 滚动区使用独立相机，必须在窗口尺寸和屏幕位置确定之后布局。
        resize(WIDTH, (int)topOffset + height + 2);
        scroll.setRect(0, topOffset, WIDTH, height);
    }

    @Override
    public void offset(int xOffset, int yOffset) {
        super.offset(xOffset, yOffset);
        if (scroll != null) scroll.setPos(scroll.left(), scroll.top());
    }
}
