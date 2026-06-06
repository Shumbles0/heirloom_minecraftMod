package com.shumbles.gearoverhaul.client;

import com.shumbles.gearoverhaul.enchant.AdvancedTableBlockEntity;
import com.shumbles.gearoverhaul.screen.AdvancedTableScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * The Advanced Enchanting Table screen: gear + Arcane Dust + four material slots, with a brew
 * progress bar. Selection is by inserting selector items; this screen just shows the slots,
 * progress, and a short hint about what the table is doing.
 */
public class AdvancedTableScreen extends HandledScreen<AdvancedTableScreenHandler> {
	private static final int INK = 0xFF2A2350;
	private static final int INK_FADED = 0xFF5A4A78;

	public AdvancedTableScreen(AdvancedTableScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.backgroundWidth = 176;
		this.backgroundHeight = 166;
		this.playerInventoryTitleY = this.backgroundHeight - 94;
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		int left = this.x;
		int top = this.y;
		context.fill(left, top, left + this.backgroundWidth, top + this.backgroundHeight, 0xFF2A2350);
		context.fill(left + 4, top + 4, left + this.backgroundWidth - 4, top + this.backgroundHeight - 4, 0xFFC9BEDF);

		drawSlot(context, left + 26, top + 17); // dust
		drawSlot(context, left + 26, top + 47); // gear
		for (int i = 0; i < AdvancedTableBlockEntity.MAT_COUNT; i++) {
			drawSlot(context, left + 152, top + 17 + i * 18);
		}

		// Brew progress arrow (left → right), from the dust/gear column toward the materials.
		int barX = left + 52;
		int barY = top + 36;
		int barW = 92;
		context.fill(barX, barY, barX + barW, barY + 6, 0xFF4A3F6E);
		int max = Math.max(1, this.handler.getMaxProgress());
		int filled = barW * Math.min(this.handler.getProgress(), max) / max;
		if (filled > 0) {
			context.fill(barX, barY, barX + filled, barY + 6, 0xFFB44AE0);
		}
	}

	private static void drawSlot(DrawContext context, int x, int y) {
		context.fill(x - 1, y - 1, x + 17, y + 17, 0xFF5A4A78);
		context.fill(x, y, x + 16, y + 16, 0xFFB8A7CE);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		this.drawMouseoverTooltip(context, mouseX, mouseY);
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		context.drawText(this.textRenderer, Text.literal("Advanced Enchanting Table"), 8, 6, INK, false);
		ItemStack gear = this.handler.gear();
		String hint;
		if (gear.isEmpty()) {
			hint = "Place gear + Arcane Dust";
		} else if (this.handler.getProgress() > 0) {
			hint = "Brewing...";
		} else {
			hint = "Add dust, a selector, or glowstone";
		}
		context.drawText(this.textRenderer, Text.literal(hint), 50, 20, INK_FADED, false);
	}
}
