package com.shumbles.gearoverhaul.client;

import com.shumbles.gearoverhaul.Heirloom;
import com.shumbles.gearoverhaul.screen.TemperingStationScreenHandler;
import com.shumbles.gearoverhaul.temper.Tempering;
import com.shumbles.gearoverhaul.usage.UsageGate;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** GUI for the Tempering Station. */
public class TemperingStationScreen extends HandledScreen<TemperingStationScreenHandler> {
	private static final Identifier TEXTURE = Identifier.of(Heirloom.MOD_ID, "textures/gui/tempering_station.png");

	public TemperingStationScreen(TemperingStationScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.backgroundWidth = 176;
		this.backgroundHeight = 166;
		this.playerInventoryTitleY = this.backgroundHeight - 94;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		// Draws the standard hover tooltip (name, durability, attributes, our description)
		// for whatever slot the mouse is over — same as the normal inventory.
		this.drawMouseoverTooltip(context, mouseX, mouseY);
	}

	@Override
	protected void init() {
		super.init();
		ButtonWidget temper = ButtonWidget.builder(Text.literal("Temper"), button -> sendTemper())
			.dimensions(this.x + 86, this.y + 20, 44, 20)
			.build();
		this.addDrawableChild(temper);
	}

	private void sendTemper() {
		if (this.client != null && this.client.interactionManager != null) {
			this.client.interactionManager.clickButton(this.handler.syncId, TemperingStationScreenHandler.TEMPER_BUTTON);
		}
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, this.x, this.y, 0.0f, 0.0f,
			this.backgroundWidth, this.backgroundHeight, this.backgroundWidth, this.backgroundHeight);
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0xFF404040, false);

		ItemStack gear = this.handler.getSlot(TemperingStationScreenHandler.GEAR_SLOT).getStack();
		Text line;
		if (gear.isEmpty()) {
			line = Text.literal("Place gear");
		} else {
			int level = Tempering.getLevel(gear);
			UsageGate.Kind kind = UsageGate.kindOf(gear);
			if (level >= Tempering.MAX_TEMPER) {
				line = Text.literal("Temper " + level + " (max)");
			} else if (level + 1 == 20) {
				line = Text.literal("Lvl " + level + " - ritual");
			} else if (level == 10 && kind != null && !UsageGate.isComplete(gear)) {
				// Milestone blocks the 10 -> 11 step; say so (vague), with a word-only sense of progress.
				line = Text.literal("Milestone unmet - " + MilestoneText.stage(kind, UsageGate.progress(gear)));
			} else {
				line = Text.literal("Temper " + level + " → " + (level + 1));
			}
		}
		context.drawText(this.textRenderer, line, 8, 44, 0xFF404040, false);
	}
}
