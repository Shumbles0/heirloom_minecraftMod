package com.shumbles.gearoverhaul.ritual;

import com.shumbles.gearoverhaul.Heirloom;
import com.shumbles.gearoverhaul.temper.Tempering;
import com.shumbles.gearoverhaul.usage.UsageGate;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * The axe's Level-20 rite (timber rhythm). Strike a Chopping Stump with an axe tempered to 19 to
 * begin: the stump plays a {@link #COUNT_IN}-beat lead-in, then keeps the beat (a clear bell each
 * {@link #BEAT_INTERVAL}). Land each blow within {@link #WINDOW} ticks of a beat, {@link #REQUIRED}
 * in a row, to finish. Miss the window or skip a beat and it restarts from a fresh lead-in. Server-side.
 */
public final class AxeRitual {
	public static final int BEAT_INTERVAL = 20; // 1s between beats
	public static final int WINDOW = 2;         // ± ticks around a beat that counts as on-time
	public static final int COUNT_IN = 3;       // lead-in beats before the scored hits begin
	public static final int REQUIRED = 8;       // on-beat blows in a row after the count-in
	public static final int IDLE_TICKS = 100;   // ends a session after this long without a blow (covers the count-in)

	private static final class Beat {
		BlockPos stump;
		long beat0;
		int combo;
		long lastBeat;
		long lastStrike;
	}

	private static final Map<UUID, Beat> sessions = new HashMap<>();

	private AxeRitual() {
	}

	public static boolean isEligible(net.minecraft.item.ItemStack stack) {
		return UsageGate.kindOf(stack) == UsageGate.Kind.AXE
			&& Tempering.getLevel(stack) == Rituals.RITUAL_FROM;
	}

	public static void register() {
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			if (world.isClient() || !(world instanceof ServerWorld sw) || !(player instanceof ServerPlayerEntity sp)) {
				return ActionResult.PASS;
			}
			if (!sw.getBlockState(pos).isOf(RitualBlocks.CHOPPING_STUMP) || sp.isSneaking()) {
				return ActionResult.PASS; // sneak to actually break/remove the stump
			}
			if (!isEligible(sp.getMainHandStack())) {
				return ActionResult.PASS;
			}
			onStrike(sp, sw, pos);
			return ActionResult.FAIL; // a rhythmic blow, not mining — don't damage the stump
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Iterator<Map.Entry<UUID, Beat>> it = sessions.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<UUID, Beat> e = it.next();
				ServerPlayerEntity player = server.getPlayerManager().getPlayer(e.getKey());
				Beat b = e.getValue();
				if (player == null) {
					it.remove();
					continue;
				}
				long now = ((ServerWorld) player.getEntityWorld()).getTime();
				if (now - b.lastStrike > IDLE_TICKS) {
					it.remove(); // wandered off — beat stops
				} else {
					long since = now - b.beat0;
					if (since > 0 && since % BEAT_INTERVAL == 0) {
						beat((ServerWorld) player.getEntityWorld(), b.stump, since / BEAT_INTERVAL);
					}
				}
			}
		});

		Heirloom.LOGGER.info("[Ritual] Axe timber rhythm registered.");
	}

	private static void onStrike(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
		long now = world.getTime();
		Beat b = sessions.get(player.getUuid());
		if (b == null || !b.stump.equals(pos)) {
			b = new Beat();
			b.stump = pos;
			b.beat0 = now;
			b.combo = 0;
			b.lastBeat = COUNT_IN; // scored beats begin the beat after the lead-in
			b.lastStrike = now;
			sessions.put(player.getUuid(), b);
			chop(world, pos, 1.0f);
			player.sendMessage(Text.literal("Find the beat — three to lead you in.").formatted(Formatting.GOLD), true);
			return;
		}

		b.lastStrike = now;
		long elapsed = now - b.beat0;
		long beatIndex = Math.round(elapsed / (double) BEAT_INTERVAL);
		long dist = Math.abs(elapsed - beatIndex * BEAT_INTERVAL);

		if (beatIndex <= COUNT_IN) {
			return; // still the lead-in — listen, don't strike yet (no penalty)
		}
		if (beatIndex == b.lastBeat) {
			return; // already counted this beat — ignore extra taps
		}

		if (dist <= WINDOW && beatIndex == b.lastBeat + 1) {
			b.combo++;
			b.lastBeat = beatIndex;
			chop(world, pos, 0.8f + b.combo * 0.12f);
			if (b.combo >= REQUIRED) {
				sessions.remove(player.getUuid());
				Rituals.finish(player.getMainHandStack(), world, Vec3d.ofCenter(pos), player);
			} else {
				player.sendMessage(Text.literal("Rhythm: " + b.combo + " / " + REQUIRED).formatted(Formatting.GOLD), true);
			}
		} else {
			// Off the beat, or a beat skipped — restart from a fresh lead-in.
			b.beat0 = now;
			b.combo = 0;
			b.lastBeat = COUNT_IN;
			world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.BLOCKS, 0.8f, 0.5f);
			player.sendMessage(Text.literal("Off the beat — find it again from the top.").formatted(Formatting.GRAY), true);
		}
	}

	private static void chop(ServerWorld world, BlockPos pos, float pitch) {
		world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.BLOCKS, 0.9f,
			Math.min(2.0f, pitch));
		Vec3d c = Vec3d.ofCenter(pos);
		world.spawnParticles(ParticleTypes.CRIT, c.x, c.y + 0.5, c.z, 6, 0.3, 0.2, 0.3, 0.1);
	}

	/** The stump's metronome: a soft tick through the lead-in, then a clear bell on each beat to strike. */
	private static void beat(ServerWorld world, BlockPos pos, long beatIndex) {
		Vec3d c = Vec3d.ofCenter(pos);
		if (beatIndex <= COUNT_IN) {
			world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), SoundCategory.BLOCKS, 0.7f, 1.2f);
			world.spawnParticles(ParticleTypes.NOTE, c.x, c.y + 0.8, c.z, 1, 0.2, 0.1, 0.2, 0.4);
		} else {
			world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.BLOCKS, 0.8f, 1.0f);
			world.spawnParticles(ParticleTypes.NOTE, c.x, c.y + 0.8, c.z, 1, 0.2, 0.1, 0.2, 0.9);
		}
	}
}
