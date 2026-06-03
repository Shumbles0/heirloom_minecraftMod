package com.shumbles.gearoverhaul.client;

import com.shumbles.gearoverhaul.codex.CodexComponents;
import com.shumbles.gearoverhaul.codex.CodexEntries;
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

/** The Codex menu: read entries (opens the full-size book) or inscribe new recipe chapters. */
public class CodexScreen extends HandledScreen<CodexScreenHandler> {
	private boolean inscribing = false;
	private int selectedRarity = 0;

	private ButtonWidget readButton;
	private ButtonWidget toggleButton;
	private ButtonWidget prevButton;
	private ButtonWidget nextButton;
	private ButtonWidget revealButton;
	private final ButtonWidget[] choiceButtons = new ButtonWidget[3];

	public CodexScreen(CodexScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.backgroundWidth = 176;
		this.backgroundHeight = 200;
	}

	@Override
	protected void init() {
		super.init();
		this.readButton = ButtonWidget.builder(Text.literal("Read Codex"), b -> {
			if (this.client != null) {
				this.client.setScreen(new CodexReadingScreen());
			}
		}).dimensions(this.x + 34, this.y + 34, 108, 20).build();
		this.addDrawableChild(this.readButton);

		// Position is set each frame: centered under Read in the menu, top-right while inscribing.
		this.toggleButton = ButtonWidget.builder(Text.literal("Inscribe"), b -> this.inscribing = !this.inscribing)
			.dimensions(this.x + 60, this.y + 58, 56, 20).build();
		this.addDrawableChild(this.toggleButton);

		this.prevButton = ButtonWidget.builder(Text.literal("<"), b -> cycleRarity(-1))
			.dimensions(this.x + 8, this.y + 44, 20, 20).build();
		this.addDrawableChild(this.prevButton);
		this.nextButton = ButtonWidget.builder(Text.literal(">"), b -> cycleRarity(1))
			.dimensions(this.x + 148, this.y + 44, 20, 20).build();
		this.addDrawableChild(this.nextButton);

		this.revealButton = ButtonWidget.builder(Text.literal("Reveal"),
				b -> clickButton(CodexScreenHandler.REVEAL_BUTTON_BASE + selectedRarity))
			.dimensions(this.x + 34, this.y + 70, 108, 20).build();
		this.addDrawableChild(this.revealButton);

		int[] ys = {44, 68, 92};
		for (int i = 0; i < 3; i++) {
			final int choice = i;
			this.choiceButtons[i] = ButtonWidget.builder(Text.literal("..."), b ->
					clickButton(CodexScreenHandler.CHOICE_BUTTON_BASE + choice))
				.dimensions(this.x + 34, this.y + ys[i], 108, 20).build();
			this.addDrawableChild(this.choiceButtons[i]);
		}
	}

	private void cycleRarity(int delta) {
		int n = CodexEntries.Rarity.values().length;
		selectedRarity = (selectedRarity + delta + n) % n;
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

	private boolean canReveal(ItemStack codex) {
		ItemStack manuscripts = this.handler.getManuscriptStack();
		return CodexItems.isTemperingManuscript(manuscripts)
			&& manuscripts.getCount() >= CodexScreenHandler.MANUSCRIPT_COST
			&& !CodexEntries.lockedInRarity(rarity(), CodexComponents.getUnlocked(codex)).isEmpty();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		ItemStack codex = codex();
		boolean choosing = !CodexComponents.getPending(codex).isEmpty();
		List<Integer> pending = CodexComponents.getPending(codex);

		this.toggleButton.setMessage(Text.literal(this.inscribing ? "Back" : "Inscribe"));
		this.readButton.visible = !this.inscribing;
		// Centered under Read in the menu; tucked top-right while inscribing so it clears the selector.
		if (this.inscribing) {
			this.toggleButton.setPosition(this.x + 112, this.y + 4);
		} else {
			this.toggleButton.setPosition(this.x + 60, this.y + 58);
		}

		boolean selecting = this.inscribing && !choosing;
		this.prevButton.visible = selecting;
		this.nextButton.visible = selecting;
		this.revealButton.visible = selecting;
		this.revealButton.active = canReveal(codex);

		for (int i = 0; i < 3; i++) {
			boolean show = this.inscribing && choosing && i < pending.size();
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
			if (slot.id == CodexScreenHandler.MANUSCRIPT_SLOT && !this.inscribing) {
				continue;
			}
			int sx = left + slot.x;
			int sy = top + slot.y;
			context.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF5A4A30);
			context.fill(sx, sy, sx + 16, sy + 16, 0xFFB8A77E);
		}
		if (this.inscribing && this.handler.getManuscriptStack().isEmpty()) {
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
		context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0xFF3A2A18, false);
		ItemStack codex = codex();

		if (this.inscribing) {
			if (!CodexComponents.getPending(codex).isEmpty()) {
				context.drawText(this.textRenderer, Text.literal("Choose a chapter:"), 8, 36, 0xFF3A2A18, false);
				return;
			}
			int locked = CodexEntries.lockedInRarity(rarity(), CodexComponents.getUnlocked(codex)).size();
			Text caption = Text.literal("Rarity");
			context.drawText(this.textRenderer, caption,
				(this.backgroundWidth - this.textRenderer.getWidth(caption)) / 2, 36, 0xFF6B5A40, false);
			Text name = Text.literal(rarity().label + "  (" + locked + " left)");
			context.drawText(this.textRenderer, name,
				(this.backgroundWidth - this.textRenderer.getWidth(name)) / 2, 50, 0xFF3A2A18, false);
			context.drawText(this.textRenderer, Text.literal("3 manuscripts -> reveal 3, pick 1"), 8, 96, 0xFF6B5A40, false);
			return;
		}

		int unlocked = CodexComponents.getUnlocked(codex).size();
		context.drawText(this.textRenderer, Text.literal(unlocked + " / " + CodexEntries.chapterCount() + " chapters inscribed"),
			8, 24, 0xFF6B5A40, false);
	}
}
