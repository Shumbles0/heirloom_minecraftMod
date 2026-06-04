package com.shumbles.gearoverhaul.ritual;

import com.shumbles.gearoverhaul.Heirloom;
import com.shumbles.gearoverhaul.temper.Tempering;
import com.shumbles.gearoverhaul.usage.UsageGate;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * The chestplate charge's 5-second fuse. Arming a charge starts a countdown with beeps that
 * quicken as it runs out; at zero it detonates — a memorable burst of fire, smoke and sparks — and
 * sets the wearer to half a heart (never death). If a tempered chestplate is still worn, the rite
 * completes. Server-side; a disconnect cancels the fuse.
 */
public final class ChargeHandler {
	public static final int FUSE_TICKS = 100; // ~5 seconds

	private static final Map<UUID, Fuse> armed = new HashMap<>();

	private ChargeHandler() {
	}

	public static boolean isEligible(ItemStack chest) {
		return UsageGate.kindOf(chest) == UsageGate.Kind.CHESTPLATE
			&& Tempering.getLevel(chest) == Rituals.RITUAL_FROM;
	}

	/** Light the fuse on a player (re-lighting resets it). */
	public static void arm(ServerPlayerEntity player) {
		armed.put(player.getUuid(), new Fuse(FUSE_TICKS));
		ServerWorld world = (ServerWorld) player.getEntityWorld();
		world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.PLAYERS, 1.0f, 1.0f);
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (armed.isEmpty()) {
				return;
			}
			Iterator<Map.Entry<UUID, Fuse>> it = armed.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<UUID, Fuse> entry = it.next();
				ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
				if (player == null) {
					it.remove(); // disconnected — fuse fizzles out
					continue;
				}
				Fuse fuse = entry.getValue();
				fuse.ticksLeft--;
				if (--fuse.beepCooldown <= 0) {
					beep(player, fuse.ticksLeft);
					fuse.beepCooldown = beepInterval(fuse.ticksLeft);
				}
				if (fuse.ticksLeft <= 0) {
					detonate(player);
					it.remove();
				}
			}
		});
		Heirloom.LOGGER.info("[Ritual] Chestplate charge fuse registered.");
	}

	private static int beepInterval(int ticksLeft) {
		return Math.max(2, 2 + ticksLeft * 18 / FUSE_TICKS); // ~20 ticks apart at the start, ~2 at the end
	}

	private static void beep(ServerPlayerEntity player, int ticksLeft) {
		ServerWorld world = (ServerWorld) player.getEntityWorld();
		float pitch = 0.8f + (1.0f - (float) ticksLeft / FUSE_TICKS) * 1.4f; // rises 0.8 → ~2.2
		world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(),
			SoundCategory.PLAYERS, 0.8f, pitch);
		world.spawnParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.2, player.getZ(),
			2, 0.1, 0.1, 0.1, 0.0);
	}

	private static void detonate(ServerPlayerEntity player) {
		ServerWorld world = (ServerWorld) player.getEntityWorld();
		double x = player.getX(), y = player.getY(), z = player.getZ();

		world.createExplosion(null, x, y + 0.8, z, 0.0f, World.ExplosionSourceType.NONE);
		// A memorable burst — fire, heavy smoke and sparks.
		world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, x, y + 0.9, z, 1, 0.0, 0.0, 0.0, 0.0);
		world.spawnParticles(ParticleTypes.FLAME, x, y + 0.9, z, 60, 0.6, 0.6, 0.6, 0.12);
		world.spawnParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.9, z, 45, 0.5, 0.5, 0.5, 0.05);
		world.spawnParticles(ParticleTypes.FIREWORK, x, y + 0.9, z, 60, 0.7, 0.7, 0.7, 0.25);

		// To the brink regardless of armour, never to death.
		player.setHealth(1.0f);

		ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
		if (isEligible(chest)) {
			Rituals.finish(chest, world, new Vec3d(x, y, z), player);
		}
	}

	private static final class Fuse {
		int ticksLeft;
		int beepCooldown;

		Fuse(int ticksLeft) {
			this.ticksLeft = ticksLeft;
			this.beepCooldown = 0;
		}
	}
}
