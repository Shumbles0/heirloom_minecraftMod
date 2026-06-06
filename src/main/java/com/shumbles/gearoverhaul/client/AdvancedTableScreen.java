package com.shumbles.gearoverhaul.client;

import com.shumbles.gearoverhaul.enchant.EnchantComponents;
import com.shumbles.gearoverhaul.enchant.EnchantDirections;
import com.shumbles.gearoverhaul.screen.AdvancedTableScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;

/**
 * The Advanced Enchanting Table screen: pick a direction slot on the left, then choose an attribute
 * for it on the right. Slot selection is local; choosing an attribute sends a button to the handler.
 */
public class AdvancedTableScreen extends HandledScreen<AdvancedTableScreenHandler> {
	private static final int INK = 0xFF2A2350;
	private static final int INK_FADED = 0xFF5A4A78;
	private static final int MAX_ATTRS = 8;

	private int activeSlot = 0;
	private final ButtonWidget[] slotButtons = new ButtonWidget[EnchantComponents.MAX_SLOTS];
	private final ButtonWidget[] attrButtons = new ButtonWidget[MAX_ATTRS];

	public AdvancedTableScreen(AdvancedTableScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.backgroundWidth = 256;
		this.backgroundHeight = 178;
	}

	@Override
	protected void init() {
		super.init();
		for (int i = 0; i < EnchantComponents.MAX_SLOTS; i++) {
			final int s = i;
			this.slotButtons[i] = ButtonWidget.builder(Text.literal("..."), b -> this.activeSlot = s)
				.dimensions(this.x + 10, this.y + 34 + i * 22, 104, 20).build();
			this.addDrawableChild(this.slotButtons[i]);
		}
		for (int j = 0; j < MAX_ATTRS; j++) {
			final int a = j;
			this.attrButtons[j] = ButtonWidget.builder(Text.literal("..."), b -> clickAttr(a))
				.dimensions(this.x + 130, this.y + 28 + j * 17, 116, 16).build();
			this.addDrawableChild(this.attrButtons[j]);
		}
	}

	private void clickAttr(int attrIndex) {
		if (this.client != null && this.client.interactionManager != null) {
			this.client.interactionManager.clickButton(this.handler.syncId, this.activeSlot * 16 + attrIndex);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		ItemStack gear = this.handler.gear();
		int count = EnchantComponents.slotCount(gear);
		if (this.activeSlot >= count) {
			this.activeSlot = 0;
		}

		for (int i = 0; i < EnchantComponents.MAX_SLOTS; i++) {
			boolean show = i < count;
			this.slotButtons[i].visible = show;
			if (show) {
				String attr = EnchantComponents.attributeAt(gear, i);
				this.slotButtons[i].setMessage(Text.literal((i + 1) + ": " + EnchantComponents.directionAt(gear, i)
					+ (attr != null ? " *" : "")));
				this.slotButtons[i].active = i != this.activeSlot;
			}
		}

		List<String> attrs = count > 0 ? EnchantDirections.attributesOf(EnchantComponents.directionAt(gear, this.activeSlot)) : List.of();
		String chosen = count > 0 ? EnchantComponents.attributeAt(gear, this.activeSlot) : null;
		for (int j = 0; j < MAX_ATTRS; j++) {
			boolean show = j < attrs.size();
			this.attrButtons[j].visible = show;
			if (show) {
				this.attrButtons[j].setMessage(Text.literal(attrs.get(j)));
				this.attrButtons[j].active = !attrs.get(j).equals(chosen);
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
		context.drawText(this.textRenderer, Text.literal("Advanced Enchanting Table"), 8, 8, INK, false);
		context.drawText(this.textRenderer, Text.literal("Pick a slot, then its attribute"), 8, 20, INK_FADED, false);
	}
}
