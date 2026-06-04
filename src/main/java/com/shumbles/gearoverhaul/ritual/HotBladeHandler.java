package com.shumbles.gearoverhaul.ritual;

import com.shumbles.gearoverhaul.Heirloom;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * A blade pulled from the forge stays dangerously hot: while it is held (and carries its forge
 * heat) it scorches the holder every {@link SwordRitual#BURN_INTERVAL} ticks, like a magma block,
 * until it is quenched. If it is never quenched it simply cools after {@link SwordRitual#COOL_TIME}
 * (heat cleared, no more burn) — so a missed rite costs some health, not the blade. Server-side.
 */
public final class HotBladeHandler {
	private HotBladeHandler() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				tickHand(player, player.getMainHandStack());
				tickHand(player, player.getOffHandStack());
			}
		});
		Heirloom.LOGGER.info("[Ritual] Hot-blade scorch registered.");
	}

	private static void tickHand(ServerPlayerEntity player, ItemStack stack) {
		if (stack.isEmpty() || !stack.contains(RitualComponents.FORGE_HEAT)) {
			return;
		}
		ServerWorld world = (ServerWorld) player.getEntityWorld();
		long age = world.getTime() - stack.getOrDefault(RitualComponents.FORGE_PULL_TICK, 0L);
		if (age > SwordRitual.COOL_TIME) {
			// Cooled on its own — no longer dangerous, but no longer quenchable either.
			stack.remove(RitualComponents.FORGE_HEAT);
			stack.remove(RitualComponents.FORGE_PULL_TICK);
			return;
		}
		if (world.getTime() % SwordRitual.BURN_INTERVAL == 0) {
			player.damage(world, world.getDamageSources().hotFloor(), SwordRitual.BURN_DAMAGE);
			world.spawnParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0, player.getZ(),
				3, 0.2, 0.3, 0.2, 0.0);
		}
	}
}
