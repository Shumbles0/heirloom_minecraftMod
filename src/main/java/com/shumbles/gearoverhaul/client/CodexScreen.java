package com.shumbles.gearoverhaul.client;

import com.shumbles.gearoverhaul.codex.CodexComponents;
import com.shumbles.gearoverhaul.codex.CodexEntries;
import com.shumbles.gearoverhaul.codex.CodexEntries.Track;
import com.shumbles.gearoverhaul.codex.CodexItems;
import com.shumbles.gearoverhaul.screen.CodexScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.List;

/**
 * The Codex menu. Opens on a root choice — <b>Read</b> or <b>Inscribe</b>.
 * <ul>
 *   <li><b>Read</b> → pick a track (Tempering / Ritual / Arcane), each showing how many of its
 *       entries are unlocked, then open that track's reading view.</li>
 *   <li><b>Inscribe</b> → spend 3 manuscripts to reveal 3 entries and keep 1. The manuscript in
 *       the slot decides the track: tempering inscribes per rarity; ritual/arcane draw from a
 *       flat pool.</li>
 * </ul>
 */
public class CodexScreen extends HandledScreen<CodexScreenHandler> {
	private enum Mode { ROOT, READ, INSCRIBE }

	private static final int INK = 0xFF3A2A18;
	private static final int INK_FADED = 0xFF6B5A40;

	private Mode mode = Mode.ROOT;
	private int selectedRarity = 0;

	private ButtonWidget readButton;
	private ButtonWidget inscribeButton;
	private ButtonWidget backButton;
	private final ButtonWidget[] trackButtons = new ButtonWidget[Track.values().length];
	private ButtonWidget prevButton;
	private ButtonWidget nextButton;
	private ButtonWidget revealButton;
	private final ButtonWidget[] choiceButtons = new ButtonWidget[3];

	/** Y of each track button in READ mode (count text is drawn just under each). */
	private static final int[] TRACK_Y = {28, 66, 104};

	public CodexScreen(CodexScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.backgroundWidth = 176;
		this.backgroundHeight = 222;
	}

	@Override
	protected void init() {
		super.init();

		// --- root ---
		this.readButton = ButtonWidget.builder(Text.literal("Read"), b -> this.mode = Mode.READ)
			.dimensions(this.x + 34, this.y + 60, 108, 20).build();
		this.addDrawableChild(this.readButton);
		this.inscribeButton = ButtonWidget.builder(Text.literal("Inscribe"), b -> enterInscribe())
			.dimensions(this.x + 34, this.y + 92, 108, 20).build();
		this.addDrawableChild(this.inscribeButton);

		this.backButton = ButtonWidget.builder(Text.literal("Back"), b -> goBack())
			.dimensions(this.x + 8, this.y + 6, 40, 20).build();
		this.addDrawableChild(this.backButton);

		// --- read: one button per track ---
		Track[] tracks = Track.values();
		for (int i = 0; i < tracks.length; i++) {
			final Track track = tracks[i];
			this.trackButtons[i] = ButtonWidget.builder(Text.literal(track.label), b -> openReading(track))
				.dimensions(this.x + 34, this.y + TRACK_Y[i], 108, 20).build();
			this.addDrawableChild(this.trackButtons[i]);
		}

		// --- inscribe ---
		this.prevButton = ButtonWidget.builder(Text.literal("<"), b -> cycleRarity(-1))
			.dimensions(this.x + 8, this.y + 56, 20, 20).build();
		this.addDrawableChild(this.prevButton);
		this.nextButton = ButtonWidget.builder(Text.literal(">"), b -> cycleRarity(1))
			.dimensions(this.x + 148, this.y + 56, 20, 20).build();
		this.addDrawableChild(this.nextButton);

		this.revealButton = ButtonWidget.builder(Text.literal("Reveal"), b -> doReveal())
			.dimensions(this.x + 34, this.y + 88, 108, 20).build();
		this.addDrawableChild(this.revealButton);

		int[] ys = {54, 82, 110};
		for (int i = 0; i < 3; i++) {
			final int choice = i;
			this.choiceButtons[i] = ButtonWidget.builder(Text.literal("..."), b ->
					clickButton(CodexScreenHandler.CHOICE_BUTTON_BASE + choice))
				.dimensions(this.x + 34, this.y + ys[i], 108, 20).build();
			this.addDrawableChild(this.choiceButtons[i]);
		}
	}

	private void openReading(Track track) {
		if (this.client != null) {
			this.client.setScreen(new CodexReadingScreen(track));
		}
	}

	/** Enter the inscribe view and arm the manuscript slot (locally + server-side, so it renders). */
	private void enterInscribe() {
		this.handler.setInscribeActive(true);
		clickButton(CodexScreenHandler.ENTER_INSCRIBE);
		this.mode = Mode.INSCRIBE;
	}

	/** Leave the current view; if leaving inscribe, disarm the slot and return its manuscripts. */
	private void goBack() {
		if (this.mode == Mode.INSCRIBE) {
			this.handler.setInscribeActive(false);
			clickButton(CodexScreenHandler.LEAVE_INSCRIBE);
		}
		this.mode = Mode.ROOT;
	}

	private void cycleRarity(int delta) {
		int n = CodexEntries.Rarity.values().length;
		selectedRarity = (selectedRarity + delta + n) % n;
	}

	/** Reveal routes to the per-rarity (tempering) or flat (ritual/arcane) handler by slot manuscript. */
	private void doReveal() {
		ItemStack manuscript = this.handler.getManuscriptStack();
		if (CodexItems.trackOf(manuscript) == Track.TEMPERING) {
			clickButton(CodexScreenHandler.REVEAL_BUTTON_BASE + selectedRarity);
		} else {
			clickButton(CodexScreenHandler.TRACK_REVEAL_BUTTON);
		}
	}

	private void clickButton(int id) {
		if (this.client != null && this.client.interactionManager != null) {
			this.client.interactionManager.clickButton(this.handler.syncId, id);
		}
	}

	private ItemStack codex() {
		return this.client != null && this.client.player != null
			? CodexItems.findCodex(this.client.player) : ItemStack.EMPTY;
	}

	private CodexEntries.Rarity rarity() {
		return CodexEntries.Rarity.values()[selectedRarity];
	}

	private Track slotTrack() {
		return CodexItems.trackOf(this.handler.getManuscriptStack());
	}

	/** How many entries a reveal of the current slot/rarity could still draw. */
	private int lockedDrawable(ItemStack codex) {
		Track track = slotTrack();
		return track == Track.TEMPERING
			? CodexEntries.revealPool(rarity(), CodexComponents.getUnlocked(codex)).size()
			: CodexEntries.lockedInTrack(track, CodexComponents.getUnlocked(codex)).size();
	}

	private boolean canReveal(ItemStack codex) {
		ItemStack manuscripts = this.handler.getManuscriptStack();
		return CodexItems.isManuscript(manuscripts)
			&& manuscripts.getCount() >= CodexScreenHandler.MANUSCRIPT_COST
			&& lockedDrawable(codex) > 0;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		ItemStack codex = codex();
		boolean choosing = !CodexComponents.getPending(codex).isEmpty();
		List<Integer> pending = CodexComponents.getPending(codex);

		// Root buttons.
		this.readButton.visible = mode == Mode.ROOT;
		this.inscribeButton.visible = mode == Mode.ROOT;
		this.backButton.visible = mode != Mode.ROOT;

		// Read: track buttons with live labels.
		Track[] tracks = Track.values();
		for (int i = 0; i < tracks.length; i++) {
			boolean show = mode == Mode.READ;
			this.trackButtons[i].visible = show;
			if (show) {
				int got = CodexEntries.unlockedInTrack(tracks[i], CodexComponents.getUnlocked(codex));
				this.trackButtons[i].active = got > 0; // nothing to read until something's inscribed
				this.trackButtons[i].setMessage(Text.literal(tracks[i].label));
			}
		}

		// Inscribe: rarity selector (tempering only), reveal, and the choice buttons.
		boolean inscribing = mode == Mode.INSCRIBE;
		boolean selecting = inscribing && !choosing;
		boolean temperingSlot = slotTrack() == Track.TEMPERING;
		this.prevButton.visible = selecting && temperingSlot;
		this.nextButton.visible = selecting && temperingSlot;
		this.revealButton.visible = selecting;
		this.revealButton.active = canReveal(codex);

		for (int i = 0; i < 3; i++) {
			boolean show = inscribing && choosing && i < pending.size();
			this.choiceButtons[i].visible = show;
			if (show) {
				this.choiceButtons[i].setMessage(CodexEntries.title(pending.get(i)));
			}
		}

		super.render(context, mouseX, mouseY, delta);
		this.drawMouseoverTooltip(context, mouseX, mouseY);
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		int left = this.x;
		int top = this.y;
		context.fill(left, top, left + this.backgroundWidth, top + this.backgroundHeight, 0xFF4A3520);
		context.fill(left + 4, top + 4, left + this.backgroundWidth - 4, top + this.backgroundHeight - 4, 0xFFD8CBA8);
		for (Slot slot : this.handler.slots) {
			if (slot.id == CodexScreenHandler.MANUSCRIPT_SLOT && mode != Mode.INSCRIBE) {
				continue;
			}
			int sx = left + slot.x;
			int sy = top + slot.y;
			context.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF5A4A30);
			context.fill(sx, sy, sx + 16, sy + 16, 0xFFB8A77E);
		}
		if (mode == Mode.INSCRIBE && this.handler.getManuscriptStack().isEmpty()) {
			Slot s = this.handler.slots.get(CodexScreenHandler.MANUSCRIPT_SLOT);
			int gx = left + s.x;
			int gy = top + s.y;
			context.fill(gx + 4, gy + 2, gx + 12, gy + 14, 0xFFC9BE9C);
			context.fill(gx + 5, gy + 5, gx + 11, gy + 6, 0xFF8A8068);
			context.fill(gx + 5, gy + 8, gx + 11, gy + 9, 0xFF8A8068);
			context.fill(gx + 5, gy + 11, gx + 11, gy + 12, 0xFF8A8068);
		}
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		// Title to the top-right so the top-left Back button never covers it.
		int titleRight = this.backgroundWidth - this.textRenderer.getWidth(this.title) - 8;
		context.drawText(this.textRenderer, this.title, titleRight, this.titleY, INK, false);
		ItemStack codex = codex();

		switch (mode) {
			case ROOT -> centered(context, "Read or inscribe?", 34, INK_FADED);
			case READ -> drawReadCounts(context, codex);
			case INSCRIBE -> drawInscribe(context, codex);
		}
	}

	private void drawReadCounts(DrawContext context, ItemStack codex) {
		centered(context, "Read", 8, INK);
		Track[] tracks = Track.values();
		for (int i = 0; i < tracks.length; i++) {
			int got = CodexEntries.unlockedInTrack(tracks[i], CodexComponents.getUnlocked(codex));
			int total = CodexEntries.totalInTrack(tracks[i]);
			centered(context, got + " / " + total + " unlocked", TRACK_Y[i] + 22, INK_FADED);
		}
	}

	private void drawInscribe(DrawContext context, ItemStack codex) {
		if (!CodexComponents.getPending(codex).isEmpty()) {
			// Just below the manuscript slot (22–38), right above the first entry button (54).
			centered(context, "Choose an entry:", 42, INK);
			return;
		}

		ItemStack manuscript = this.handler.getManuscriptStack();
		if (!CodexItems.isManuscript(manuscript)) {
			centered(context, "Place 3 manuscripts", 58, INK_FADED);
			centered(context, "to inscribe", 70, INK_FADED);
			return;
		}

		Track track = slotTrack();
		int left = lockedDrawable(codex);
		if (track == Track.TEMPERING) {
			centered(context, "Rarity", 46, INK_FADED);
			centered(context, rarity().label + "  (" + left + " left)", 62, INK);
		} else {
			centered(context, track.label, 46, INK_FADED);
			centered(context, "(" + left + " left)", 62, INK);
		}
		centered(context, "Spend 3: reveal 3, keep 1", 116, INK_FADED);
	}

	private void centered(DrawContext context, String text, int y, int color) {
		Text t = Text.literal(text);
		context.drawText(this.textRenderer, t, (this.backgroundWidth - this.textRenderer.getWidth(t)) / 2, y, color, false);
	}
}
