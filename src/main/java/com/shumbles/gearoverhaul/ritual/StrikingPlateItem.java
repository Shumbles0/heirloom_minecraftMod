package com.shumbles.gearoverhaul.ritual;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Places a Striking Plate for the mace rite. Right-click a block face to spawn an invulnerable,
 * floating armor stand tagged as a plate ({@link MaceRitual#PLATE_TAG}); slam three of these with
 * a tempered mace to perform the rite. Sneak-attack one to pick it back up.
 */
public class StrikingPlateItem extends Item {
	public StrikingPlateItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext ctx) {
		World world = ctx.getWorld();
		if (world.isClient()) {
			return ActionResult.SUCCESS;
		}
		BlockPos pos = ctx.getBlockPos().offset(ctx.getSide());
		ArmorStandEntity stand = new ArmorStandEntity(EntityType.ARMOR_STAND, world);
		float yaw = ctx.getPlayer() != null ? ctx.getPlayer().getYaw() : 0.0f;
		stand.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0f);
		stand.setInvulnerable(true);
		stand.setNoGravity(true);
		stand.addCommandTag(MaceRitual.PLATE_TAG);
		stand.setCustomName(Text.literal("Striking Plate"));
		stand.setCustomNameVisible(true);
		world.spawnEntity(stand);
		ctx.getStack().decrement(1);
		return ActionResult.SUCCESS;
	}
}
