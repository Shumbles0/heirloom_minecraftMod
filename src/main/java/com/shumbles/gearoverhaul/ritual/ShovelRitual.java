package com.shumbles.gearoverhaul.ritual;

import com.shumbles.gearoverhaul.Heirloom;
import com.shumbles.gearoverhaul.temper.Tempering;
import com.shumbles.gearoverhaul.usage.UsageGate;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The shovel's Level-20 rite (avalanche escape). Set off an Avalanche Cairn with a shovel tempered
 * to 19 and the earth caves in around you — a {@link #RADIUS}-block ball of gravel and dirt buries
 * everything it touches (block entities, bedrock and the like are spared). A small air pocket is
 * left so you don't instantly choke. Claw free ({@link #ESCAPE_BREAKS} blocks of rubble) within
 * {@link #DURATION}, or you're smothered. The ground is fully restored when the rite ends. Server-side.
 */
public final class ShovelRitual {
	public static final int DURATION = 120;    // ~6 s to claw out
	public static final int ESCAPE_BREAKS = 5; // blocks of rubble you must break to count as free
	public static final int RADIUS = 15;       // the collapse buries everything within this radius

	private static final class Cavein {
		final Set<Long> rubble = new HashSet<>();              // placed blocks, for escape counting
		final Map<Long, BlockState> original = new HashMap<>(); // what was there, for the restore
		final ServerWorld world;
		int broken;
		int target;
		long deadline;

		Cavein(ServerWorld world) {
			this.world = world;
		}
	}

	private static final Map<UUID, Cavein> caves = new HashMap<>();

	private ShovelRitual() {
	}

	public static boolean isEligible(ItemStack stack) {
		return UsageGate.kindOf(stack) == UsageGate.Kind.SHOVEL
			&& Tempering.getLevel(stack) == Rituals.RITUAL_FROM;
	}

	public static boolean isActive(ServerPlayerEntity player) {
		return caves.containsKey(player.getUuid());
	}

	public static void register() {
		// Digging out: breaking rubble blocks counts toward freedom.
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, be) -> {
			if (world.isClient() || !(player instanceof ServerPlayerEntity sp)) {
				return;
			}
			Cavein cave = caves.get(sp.getUuid());
			if (cave == null || !isEligible(sp.getMainHandStack())) {
				return;
			}
			if (cave.rubble.remove(pos.asLong())) {
				cave.broken++;
				if (cave.broken >= cave.target) {
					caves.remove(sp.getUuid());
					cleanup(cave);
					Rituals.finish(sp.getMainHandStack(), cave.world, Vec3d.ofCenter(pos), sp);
				} else {
					sp.sendMessage(Text.literal("Digging free: " + cave.broken + " / " + cave.target)
						.formatted(Formatting.GOLD), true);
				}
			}
		});

		// The clock: smothered if you don't break out in time.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Iterator<Map.Entry<UUID, Cavein>> it = caves.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<UUID, Cavein> e = it.next();
				ServerPlayerEntity player = server.getPlayerManager().getPlayer(e.getKey());
				Cavein cave = e.getValue();
				if (player == null) {
					cleanup(cave);
					it.remove();
					continue;
				}
				if (cave.world.getTime() >= cave.deadline) {
					cleanup(cave);
					it.remove();
					player.damage(cave.world, cave.world.getDamageSources().inWall(), 4.0f);
					player.sendMessage(Text.literal("The earth smothers you — dig faster next time.")
						.formatted(Formatting.GRAY), true);
				}
			}
		});

		Heirloom.LOGGER.info("[Ritual] Shovel avalanche escape registered.");
	}

	/** Bury the player under a {@link #RADIUS}-block ball of gravel/dirt and start the escape clock. */
	public static void trigger(ServerPlayerEntity player, ServerWorld world) {
		BlockPos feet = player.getBlockPos();
		BlockPos head = feet.up();
		Cavein cave = new Cavein(world);
		BlockPos.Mutable p = new BlockPos.Mutable();
		int r2 = RADIUS * RADIUS;
		// Flag 2 = notify clients only (no neighbour/redstone cascade); in a solid fill the gravel
		// is supported, so it doesn't avalanche into thousands of falling-block entities.
		int flags = Block.NOTIFY_LISTENERS;

		for (int dx = -RADIUS; dx <= RADIUS; dx++) {
			for (int dy = -RADIUS; dy <= RADIUS; dy++) {
				for (int dz = -RADIUS; dz <= RADIUS; dz++) {
					if (dx * dx + dy * dy + dz * dz > r2) {
						continue; // sphere, not a cube
					}
					p.set(feet.getX() + dx, feet.getY() + dy, feet.getZ() + dz);
					if (p.equals(feet) || p.equals(head)) {
						continue; // breathing pocket
					}
					BlockState prev = world.getBlockState(p);
					if (prev.getHardness(world, p) < 0 || world.getBlockEntity(p) != null) {
						continue; // never touch bedrock/unbreakable or blocks that hold contents
					}
					// Dirt directly over the head pocket so its ceiling can't fall in on the player.
					boolean overHead = dx == 0 && dz == 0 && dy >= 2;
					boolean gravel = !overHead && ((dx + dy + dz) & 1) == 0;
					long key = p.toImmutable().asLong();
					cave.original.put(key, prev);
					cave.rubble.add(key);
					world.setBlockState(p, (gravel ? Blocks.GRAVEL : Blocks.DIRT).getDefaultState(), flags);
				}
			}
		}
		if (cave.rubble.isEmpty()) {
			player.sendMessage(Text.literal("There's nothing here for the earth to bring down.")
				.formatted(Formatting.GRAY), true);
			return;
		}
		cave.target = Math.min(ESCAPE_BREAKS, cave.rubble.size());
		cave.deadline = world.getTime() + DURATION;
		caves.put(player.getUuid(), cave);

		world.playSound(null, feet, SoundEvents.BLOCK_GRAVEL_BREAK, SoundCategory.BLOCKS, 1.4f, 0.4f);
		world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.GRAVEL.getDefaultState()),
			feet.getX() + 0.5, feet.getY() + 1.0, feet.getZ() + 0.5, 120, 1.0, 1.2, 1.0, 0.15);
		player.sendMessage(Text.literal("The earth caves in — claw your way free!").formatted(Formatting.GOLD), true);
	}

	/** Heal the ground: every block the collapse touched goes back to exactly what it was. */
	private static void cleanup(Cavein cave) {
		for (Map.Entry<Long, BlockState> e : cave.original.entrySet()) {
			cave.world.setBlockState(BlockPos.fromLong(e.getKey()), e.getValue(), Block.NOTIFY_LISTENERS);
		}
	}
}
