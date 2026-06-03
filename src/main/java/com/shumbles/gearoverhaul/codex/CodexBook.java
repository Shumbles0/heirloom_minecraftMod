package com.shumbles.gearoverhaul.codex;

import com.shumbles.gearoverhaul.screen.CodexScreenHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** The Codex book. Right-click to open its menu. */
public class CodexBook extends Item {
	public CodexBook(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		if (!world.isClient()) {
			user.openHandledScreen(new SimpleNamedScreenHandlerFactory(
				(syncId, inventory, player) -> new CodexScreenHandler(syncId, inventory),
				Text.translatable("item.heirloom.codex")));
		}
		return ActionResult.SUCCESS;
	}
}
