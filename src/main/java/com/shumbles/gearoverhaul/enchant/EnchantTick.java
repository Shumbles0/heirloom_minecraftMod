package com.shumbles.gearoverhaul.enchant;

import com.shumbles.gearoverhaul.Heirloom;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Server-side upkeep for the tick / event-driven enchant attributes:
 * <ul>
 *   <li><b>Bloodlust / Bloodbath</b> (Momentum) — a kill feeds a stacking Speed / damage streak that
 *       sours into Slowness / −damage once you stop killing.</li>
 * </ul>
 * (Adrenal Surge / Kinetic Plating react inside the damage path; this class only handles the parts
 * that need a kill event or a per-tick sweep.)
 */
public final class EnchantTick {
	private EnchantTick() {
	}

	public static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (source.getAttacker() instanceof ServerPlayerEntity killer
				&& killer.getEntityWorld() instanceof ServerWorld world) {
				EnchantEffects.onKill(world, killer);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				if (player.getEntityWorld() instanceof ServerWorld world) {
					EnchantEffects.tickPlayer(world, player);
				}
			}
		});
		Heirloom.LOGGER.info("[Enchant] Tick-based attribute effects registered.");
	}
}
