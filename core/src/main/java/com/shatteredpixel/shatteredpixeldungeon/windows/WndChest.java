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
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;

import java.util.ArrayList;

public class WndChest extends WndTabbed {

	//visible gap between the chest frame and the bag frame, on top of the frames' own margins
	private static final int GAP = 4;
	//vertical space reserved for a section's title line
	private static final int TITLE_H = 11;
	//height of the header line on the mobile interface, where the chest's title and button share a row
	private static final int CHEST_HEADER_H = 12;
	//the chest's button takes whatever width the title doesn't use, but never less than this
	private static final int CHEST_BTN_MIN_W = 60;
	//on the mobile interface the status pane is anchored to the top of the screen and the toolbar to
	//the bottom, so a window has to fit in the room between them. Sizes taken from StatusPane's own
	//layout and from Toolbar's inventory button (GameScene sets both up)
	private static final int STATUS_PANE_H = 38;
	private static final int TOOLBAR_H = 26;
	//slot height stops shrinking here, so the slots stay usable on very short screens
	private static final int MIN_SLOT_H = 20;

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

		//the status pane and the toolbar leave only a limited band in the middle of the screen, and
		//the window is centred on the whole screen, so it has to fit in that band. If it doesn't, the
		//slots shrink until it does, the same way WndBag shrinks its slots to fit the screen.
		if (!desktop) {
			int room = (int)PixelScene.uiCamera.height - STATUS_PANE_H - TOOLBAR_H;
			while (slotH > MIN_SLOT_H) {
				int total = (int)chrome.marginTop()                                    //above the chest frame
						+ CHEST_HEADER_H + slotH                                       //chest frame
						+ (int)chrome.marginBottom() + GAP + (int)chrome.marginTop()   //its margin and the gap
						+ TITLE_H + (rows * slotH + (rows - 1))                        //bag frame
						+ tabHeight();                                                //bag tabs
				if (total <= room) break;
				slotH--;
			}
		}

		//on the mobile interface the chest's title and button share a single line, and the larger
		//interface keeps the roomier title / slots / button layout it had before
		int chestH;
		int chestSlotsY;
		if (desktop) {
			chestH = TITLE_H + slotH + 3 + 12;
			chestSlotsY = TITLE_H;
		} else {
			chestH = CHEST_HEADER_H + slotH;
			chestSlotsY = CHEST_HEADER_H;
		}
		chestBg = Chrome.get(Chrome.Type.TOAST_TR_HEAVY);
		chestBg.x = frameX;
		chestBg.y = frameY;
		chestBg.size(frameW, chestH + (int)chrome.marginVer());
		add(chestBg);

		RenderedTextBlock chestTitle = PixelScene.renderTextBlock(Messages.titleCase(session.title()), 8);
		chestTitle.hardlight(TITLE_COLOR);
		if (desktop) {
			chestTitle.maxWidth(contentW);
			chestTitle.setPos(0, 0);
		} else {
			//the button needs the rest of the line, so the title only gets what it doesn't need
			chestTitle.maxWidth(contentW - CHEST_BTN_MIN_W - 4);
			chestTitle.setPos(0, Math.max(0, (CHEST_HEADER_H - chestTitle.height()) / 2f));
		}
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
			slot.setRect(i * (slotW + 1), chestSlotsY, slotW, slotH);
			chestSlots.add(slot);
			add(slot);
		}

		RedButton spill = new RedButton(Messages.get(this, session.isMimic() ? "wake" : "spill"), 6) {
			@Override protected void onClick() {
				spillAll();
			}
		};
		if (desktop) {
			spill.setRect(0, TITLE_H + slotH + 3, contentW, 12);
		} else {
			float btnX = chestTitle.width() + 4;
			spill.setRect(btnX, (CHEST_HEADER_H - 12) / 2f, Math.max(contentW - btnX, CHEST_BTN_MIN_W), 12);
		}
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

		//WndTabbed centres the window on the whole screen, which on the mobile interface puts its top
		//under the status pane. Nudge it down so its top clears the pane, which is what the slots
		//above were shrunk to make room for
		if (!desktop) {
			int top = (int)(((int)PixelScene.uiCamera.height - camera.height) / 2);
			if (top < STATUS_PANE_H) {
				offset(0, STATUS_PANE_H - top);
			}
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
