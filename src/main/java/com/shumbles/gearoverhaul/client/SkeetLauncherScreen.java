package com.shumbles.gearoverhaul.client;

import com.shumbles.gearoverhaul.screen.SkeetLauncherScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

/** Simple loading GUI for the Skeet Launcher (drawn with fills — no texture needed). */
public class SkeetLauncherScreen extends HandledScreen<SkeetLauncherScreenHandler> {
	private static final int INK = 0xFF3A2A18;
	private static final int INK_FADED = 0xFF6B5A40;

	public SkeetLauncherScreen(SkeetLauncherScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.backgroundWidth = 176;
		this.backgroundHeight = 166;
		this.playerInventoryTitleY = this.backgroundHeight - 94;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
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
			int sx = left + slot.x;
			int sy = top + slot.y;
			context.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF5A4A30);
			context.fill(sx, sy, sx + 16, sy + 16, 0xFFB8A77E);
		}
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, INK, false);
		context.drawText(this.textRenderer, Text.literal("Load clay pigeons; power with redstone"),
			8, 22, INK_FADED, false);
	}
}
