package com.shumbles.gearoverhaul.client;

import com.shumbles.gearoverhaul.enchant.EnchantComponents;
import com.shumbles.gearoverhaul.screen.DirectionScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

import java.util.List;

/**
 * The direction-selection screen: up to three direction buttons for the held gear. Picking one
 * sends the button to {@link DirectionScreenHandler}, which writes it to the gear and spends the cost.
 */
public class DirectionScreen extends HandledScreen<DirectionScreenHandler> {
	private static final int INK = 0xFF2A2350;
	private static final int INK_FADED = 0xFF5A4A78;

	private final ButtonWidget[] buttons = new ButtonWidget[3];

	public DirectionScreen(DirectionScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.backgroundWidth = 176;
		this.backgroundHeight = 120;
	}

	@Override
	protected void init() {
		super.init();
		for (int i = 0; i < 3; i++) {
			final int choice = i;
			this.buttons[i] = ButtonWidget.builder(Text.literal("..."), b -> clickButton(choice))
				.dimensions(this.x + 18, this.y + 34 + i * 24, 140, 20).build();
			this.addDrawableChild(this.buttons[i]);
		}
	}

	private void clickButton(int id) {
		if (this.client != null && this.client.interactionManager != null) {
			this.client.interactionManager.clickButton(this.handler.syncId, id);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		List<String> offered = this.handler.offered();
		for (int i = 0; i < 3; i++) {
			boolean show = i < offered.size();
			this.buttons[i].visible = show;
			if (show) {
				this.buttons[i].setMessage(Text.literal(offered.get(i)));
			}
		}
		super.render(context, mouseX, mouseY, delta);
		this.drawMouseoverTooltip(context, mouseX, mouseY);
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		int left = this.x;
		int top = this.y;
		context.fill(left, top, left + this.backgroundWidth, top + this.backgroundHeight, 0xFF2A2350);
		context.fill(left + 4, top + 4, left + this.backgroundWidth - 4, top + this.backgroundHeight - 4, 0xFFC9BEDF);
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		context.drawText(this.textRenderer, Text.literal("Choose a Direction"), 8, 8, INK, false);
		int filled = EnchantComponents.getDirections(this.handler.gear()).size();
		context.drawText(this.textRenderer, Text.literal("Slot " + (filled + 1) + " of " + EnchantComponents.MAX_SLOTS
				+ "   (" + DirectionScreenHandler.LAPIS_COST + " lapis, " + DirectionScreenHandler.XP_COST + " levels)"),
			8, 20, INK_FADED, false);
	}
}
