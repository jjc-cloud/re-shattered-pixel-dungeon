package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.badlogic.gdx.Input;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDAction;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.MagicalHolster;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.PotionBandolier;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.ScrollHolder;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.VelvetPouch;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.InventorySlot;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.input.GameAction;
import com.watabou.input.KeyBindings;
import com.watabou.input.KeyEvent;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.utils.PlatformSupport;
import com.watabou.utils.RectF;

import java.util.ArrayList;

public class WndChest extends WndTabbed {

	//visible gap between the chest frame and the bag frame, on top of the frames' own margins
	private static final int GAP = 4;
	//vertical space reserved for a section's title line
	private static final int TITLE_H = 11;
	//the HUD bands the window has to stay clear of. The status pane pins itself to the top on the
	//mobile interface and to the bottom on the larger one, the toolbar is always at the bottom.
	//Heights come from StatusPane.layout (38 / 39) and from Toolbar's 24x26 inventory button
	private static final int STATUS_PANE_H = 38;
	private static final int STATUS_PANE_H_LARGE = 39;
	private static final int TOOLBAR_H = 26;

	private final ChestSession session;
	private final boolean desktop;
	private Bag currentBag;
	private RenderedTextBlock bagTitle;
	private NinePatch chestBg;
	private NinePatch bagBg;
	private final ArrayList<InventorySlot> chestSlots = new ArrayList<>();
	private final ArrayList<InventorySlot> bagSlots = new ArrayList<>();
	private final ArrayList<InventorySlot> equippedSlots = new ArrayList<>();

	public WndChest(Heap heap, boolean unlocked) {
		this(new ChestSession(heap, unlocked));
	}

	public WndChest(Mimic mimic) {
		this(new ChestSession(mimic));
	}

	public WndChest(ChestSession session) {
		super();
		this.session = session;
		//the window draws no backdrop or shadow of its own, each section brings its own frame instead,
		//so the map stays visible in the gap between the chest frame and the bag frame. The window's
		//shadow is a 16px halo, which would always fill that gap, so it stays off as well
		chrome.visible = false;
		shadow.visible = false;
		desktop = SPDSettings.interfaceSize() == 2;
		currentBag = Dungeon.hero.belongings.backpack;
		int slotW = desktop ? 17 : 25;
		int slotH = desktop ? 24 : 25;
		int columns = desktop ? 10 : 5;
		int rows = (20 + columns - 1) / columns;
		//a full row of bag slots defines the content width shared by both sections
		int contentW = columns * (slotW + 1) - 1;
		int width = contentW;

		//each frame carries the window's own margins, so its content is padded exactly like a normal
		//window and the bag frame ends up flush with the bag tabs instead of leaving them floating
		int frameX = -(int)chrome.marginLeft();
		int frameY = -(int)chrome.marginTop();
		int frameW = width + (int)chrome.marginHor();

		//the chest gets its own frame, stacked on top
		int chestH = TITLE_H + slotH + 3 + 12;
		chestBg = Chrome.get(Chrome.Type.TOAST_TR_HEAVY);
		chestBg.x = frameX;
		chestBg.y = frameY;
		chestBg.size(frameW, chestH + (int)chrome.marginVer());
		add(chestBg);

		RenderedTextBlock chestTitle = PixelScene.renderTextBlock(Messages.titleCase(session.title()), 8);
		chestTitle.hardlight(TITLE_COLOR);
		chestTitle.maxWidth(contentW);
		chestTitle.setPos(0, 0);
		add(chestTitle);

		for (int i = 0; i < 5; i++) {
			InventorySlot slot = new InventorySlot(null) {
				@Override protected void onClick() {
					if (session.take(item())) refresh();
				}
				@Override protected void onRightClick() {
					if (session.isMimic()) wakeMimic();
				}
				@Override protected void onMiddleClick() {
					if (session.isMimic()) wakeMimic();
				}
			};
			slot.setRect(i * (slotW + 1), TITLE_H, slotW, slotH);
			chestSlots.add(slot);
			add(slot);
		}

		RedButton spill = new RedButton(Messages.get(this, session.isMimic() ? "wake" : "spill"), 6) {
			@Override protected void onClick() {
				spillAll();
			}
		};
		spill.setRect(0, TITLE_H + slotH + 3, contentW, 12);
		add(spill);

		//the bag gets a frame below the chest, framed as a normal tabbed window: the bag tabs land on
		//the frame's bottom margin, exactly where they sit in the normal bag window
		int bagTop = chestH + (int)chrome.marginBottom() + GAP + (int)chrome.marginTop();
		int bagH = TITLE_H + (desktop ? slotH + 1 : 0) + (rows * slotH + (rows - 1));
		bagBg = Chrome.get(Chrome.Type.TAB_SET);
		bagBg.x = frameX;
		bagBg.y = bagTop - (int)chrome.marginTop();
		bagBg.size(frameW, bagH + (int)chrome.marginVer());
		add(bagBg);

		bagTitle = PixelScene.renderTextBlock(Messages.titleCase(currentBag.name()), 8);
		bagTitle.hardlight(TITLE_COLOR);
		bagTitle.maxWidth(contentW);
		bagTitle.setPos(0, bagTop);
		add(bagTitle);

		int rowsTop = bagTop + TITLE_H;

		if (desktop) {
			for (int i = 0; i < 5; i++) {
				InventorySlot slot = new InventorySlot(null);
				slot.setRect(i * (slotW + 1), rowsTop, slotW, slotH);
				equippedSlots.add(slot);
				add(slot);
			}
			rowsTop += slotH + 1;
		}

		for (int i = 0; i < 20; i++) {
			InventorySlot slot = new InventorySlot(null) {
				@Override protected void onClick() {
					if (session.isMimic()) {
						wakeMimic();
					} else if (session.put(item(), currentBag)) {
						refresh();
					}
				}
			};
			slot.setRect((i % columns) * (slotW + 1), rowsTop + (i / columns) * (slotH + 1), slotW, slotH);
			bagSlots.add(slot);
			add(slot);
		}

		resize(width, bagTop + bagH);

		//WndTabbed centres the window on the raw game size, skipping the safe insets that
		//Window.resize accounts for, and it ignores the HUD completely. A window this tall then
		//hangs below centre, with its tabs past the bottom edge, so put it back in the band the
		//HUD actually leaves free
		if (!desktop) {
			RectF insets = Game.platform.getSafeInsets(PlatformSupport.INSET_BLK);
			int uiSize = SPDSettings.interfaceSize();

			//undo the inset centring WndTabbed skips, then centre between the HUD bands
			float delta = (insets.top - insets.bottom) / (2f * camera.zoom);
			int hudTop = uiSize == 0 ? STATUS_PANE_H : 0;
			int hudBottom = uiSize == 0 ? TOOLBAR_H : STATUS_PANE_H_LARGE;
			delta += (hudTop - hudBottom) / 2f;

			offset(0, Math.round(delta));
		}

		int index = 1;
		for (Bag bag : Dungeon.hero.belongings.getBags()) {
			if (bag != null) {
				BagTab tab = new BagTab(bag, index++);
				add(tab);
				if (bag == currentBag) select(tab);
			}
		}
		layoutTabs();
		refresh();
		if (desktop) GameScene.setChestInventoryHidden(true);
	}

	private void refresh() {
		ArrayList<Item> chestItems = new ArrayList<>(session.items());
		for (int i = 0; i < chestSlots.size(); i++) {
			Item item = i < chestItems.size() ? chestItems.get(i) : null;
			chestSlots.get(i).item(item);
			chestSlots.get(i).enable(item != null);
		}

		ArrayList<Item> bagItems = new ArrayList<>();
		for (Item item : currentBag.items) if (!(item instanceof Bag)) bagItems.add(item);
		for (int i = 0; i < bagSlots.size(); i++) {
			Item item = i < bagItems.size() ? bagItems.get(i) : null;
			bagSlots.get(i).item(item);
			bagSlots.get(i).enable(item != null && (!Dungeon.hero.belongings.lostInventory()
					|| item.keptThroughLostInventory()));
		}

		if (desktop) {
			Belongings stuff = Dungeon.hero.belongings;
			Item[] equipment = {
					Dungeon.hero.hasWeaponSlots() ? stuff.weapon : stuff.armor,
					Dungeon.hero.hasWeaponSlots() ? stuff.armor : stuff.artifact,
					Dungeon.hero.hasWeaponSlots() ? stuff.artifact : stuff.misc,
					Dungeon.hero.hasWeaponSlots() ? stuff.misc : stuff.extraMisc,
					stuff.ring
			};
			int[] images = {ItemSpriteSheet.WEAPON_HOLDER, ItemSpriteSheet.ARMOR_HOLDER,
					ItemSpriteSheet.ARTIFACT_HOLDER, ItemSpriteSheet.SOMETHING, ItemSpriteSheet.RING_HOLDER};
			for (int i = 0; i < equippedSlots.size(); i++) {
				equippedSlots.get(i).item(equipment[i] == null ? new WndBag.Placeholder(images[i]) : equipment[i]);
				equippedSlots.get(i).enable(false);
			}
		}
		bagTitle.text(Messages.titleCase(currentBag.name()));
	}

	private void spillAll() {
		if (session.isMimic()) {
			wakeMimic();
		} else {
			session.spill();
			hide();
		}
	}

	private void wakeMimic() {
		Mimic mimic = session.mimic();
		session.wakeMimic();
		hide();
		mimic.interruptLooting();
	}

	@Override
	public boolean onSignal(KeyEvent event) {
		if (event.pressed && desktop && event.code == Input.Keys.SPACE) {
			spillAll();
			return true;
		}
		if (event.pressed && KeyBindings.getActionForKey(event) == SPDAction.INVENTORY) {
			onBackPressed();
			return true;
		}
		if (event.pressed && session.isMimic()
				&& KeyBindings.getActionForKey(event) != SPDAction.BACK
				&& KeyBindings.getActionForKey(event) != SPDAction.WAIT
				&& KeyBindings.getActionForKey(event) != SPDAction.BAG_1
				&& KeyBindings.getActionForKey(event) != SPDAction.BAG_2
				&& KeyBindings.getActionForKey(event) != SPDAction.BAG_3
				&& KeyBindings.getActionForKey(event) != SPDAction.BAG_4
				&& KeyBindings.getActionForKey(event) != SPDAction.BAG_5) {
			wakeMimic();
			return true;
		}
		return super.onSignal(event);
	}

	@Override
	protected void onClick(Tab tab) {
		select(tab);
		currentBag = ((BagTab) tab).bag;
		refresh();
	}

	@Override
	protected int tabHeight() {
		return 20;
	}

	@Override
	public void hide() {
		super.hide();
		if (desktop) GameScene.setChestInventoryHidden(false);
		session.close();
	}

	private Image icon(Bag bag) {
		if (bag instanceof VelvetPouch) return Icons.get(Icons.SEED_POUCH);
		if (bag instanceof ScrollHolder) return Icons.get(Icons.SCROLL_HOLDER);
		if (bag instanceof MagicalHolster) return Icons.get(Icons.WAND_HOLSTER);
		if (bag instanceof PotionBandolier) return Icons.get(Icons.POTION_BANDOLIER);
		return Icons.get(Icons.BACKPACK);
	}

	private class BagTab extends IconTab {
		private final Bag bag;
		private final int index;

		private BagTab(Bag bag, int index) {
			super(icon(bag));
			this.bag = bag;
			this.index = index;
		}

		@Override
		public GameAction keyAction() {
			switch (index) {
				case 2: return SPDAction.BAG_2;
				case 3: return SPDAction.BAG_3;
				case 4: return SPDAction.BAG_4;
				case 5: return SPDAction.BAG_5;
				default: return SPDAction.BAG_1;
			}
		}

		@Override
		protected String hoverText() {
			return Messages.titleCase(bag.name());
		}
	}
}
