package com.shumbles.gearoverhaul.loot;

import com.shumbles.gearoverhaul.Heirloom;
import com.shumbles.gearoverhaul.codex.CodexItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Rate-limited Tempering Manuscript drops from combat: a player gets at most one manuscript per
 * {@link #COOLDOWN} ticks — the next hostile mob they slay after the cooldown drops one. Kills
 * during the cooldown drop nothing, so mob farms can't churn them out. Server-side.
 */
public final class ManuscriptDrops {
	public static final int COOLDOWN = 1200; // 60 seconds

	private static final Map<UUID, Long> lastDrop = new HashMap<>();

	private ManuscriptDrops() {
	}

	public static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(entity instanceof Monster) || !(source.getAttacker() instanceof ServerPlayerEntity player)) {
				return;
			}
			if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
				return;
			}
			long now = world.getTime();
			Long last = lastDrop.get(player.getUuid());
			if (last != null && now - last < COOLDOWN) {
				return; // still on cooldown — no drop
			}
			lastDrop.put(player.getUuid(), now);
			ItemEntity drop = new ItemEntity(world, entity.getX(), entity.getY() + 0.5, entity.getZ(),
				new ItemStack(CodexItems.TEMPERING_MANUSCRIPT));
			drop.setToDefaultPickupDelay();
			world.spawnEntity(drop);
		});
		Heirloom.LOGGER.info("[Loot] Tempering Manuscript mob drop registered (1 per {} ticks).", COOLDOWN);
	}
}
