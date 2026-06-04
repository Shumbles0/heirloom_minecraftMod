package com.shumbles.gearoverhaul.ritual;

import com.shumbles.gearoverhaul.temper.Tempering;
import com.shumbles.gearoverhaul.usage.UsageGate;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * The trident's Level-20 rite (lightning catch). A thrown trident tempered to 19, flying during a
 * thunderstorm at least {@link #MIN_HEIGHT_ABOVE_ROD} blocks above a lightning rod (in the rod's
 * own column), has a {@code 1}-in-{@link #STRIKE_CHANCE_IN} chance — once per throw — to draw the
 * bolt: real lightning strikes it, it floats {@link #FLOAT_TICKS} ticks, then falls. The freeze is
 * driven by the trident mixin; this class supplies the conditions and the strike.
 */
public final class TridentRitual {
	public static final int STRIKE_CHANCE_IN = 5;       // 1-in-5 per qualifying throw
	public static final int MIN_HEIGHT_ABOVE_ROD = 15;
	public static final int FLOAT_TICKS = 40;           // ~2s hover after the strike
	private static final int COLUMN_SCAN = 64;          // how far down to look for a rod

	private TridentRitual() {
	}

	public static boolean isEligible(ItemStack stack) {
		return UsageGate.kindOf(stack) == UsageGate.Kind.TRIDENT
			&& Tempering.getLevel(stack) == Rituals.RITUAL_FROM;
	}

	/** Y of the topmost lightning rod in {@code pos}'s column at or below it, or {@code MIN_VALUE}. */
	public static int rodTopBelow(ServerWorld world, BlockPos pos) {
		int floor = Math.max(world.getBottomY(), pos.getY() - COLUMN_SCAN);
		BlockPos.Mutable cursor = new BlockPos.Mutable(pos.getX(), pos.getY(), pos.getZ());
		for (int y = pos.getY(); y >= floor; y--) {
			cursor.setY(y);
			if (world.getBlockState(cursor).isOf(Blocks.LIGHTNING_ROD)) {
				return y;
			}
		}
		return Integer.MIN_VALUE;
	}

	/** Real lightning at the trident, then complete the rite (19 → 20). */
	public static void strike(ServerWorld world, TridentEntity trident, ItemStack stack) {
		LightningEntity bolt = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
		bolt.refreshPositionAfterTeleport(trident.getX(), trident.getY(), trident.getZ());
		if (trident.getOwner() instanceof ServerPlayerEntity channeler) {
			bolt.setChanneler(channeler);
		}
		world.spawnEntity(bolt);

		PlayerEntity owner = trident.getOwner() instanceof PlayerEntity p ? p : null;
		Rituals.finish(stack, world, new Vec3d(trident.getX(), trident.getY(), trident.getZ()), owner);
	}
}
