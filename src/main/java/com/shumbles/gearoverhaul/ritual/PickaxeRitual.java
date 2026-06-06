package com.shumbles.gearoverhaul.ritual;

import com.shumbles.gearoverhaul.Heirloom;
import com.shumbles.gearoverhaul.temper.Tempering;
import com.shumbles.gearoverhaul.usage.UsageGate;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The pickaxe's Level-20 rite (vein-glow chase). A mystic glow seeds rarely onto a deepslate block
 * deep underground; it can be broken only by a pickaxe tempered to 19. Break the seed and a 2.75s
 * chase begins — the glow leaps (instantly, leaving a trail) to a nearby exposed deepslate block,
 * and you must break each new block before the clock runs out. Fifteen in a row completes the rite;
 * hesitate and the glow goes cold and you must find another. Untouched seeds simply wait, despawning
 * only once no one is near. Server-side.
 */
public final class PickaxeRitual {
	public static final int REQUIRED = 15;
	public static final int CHASE_TICKS = 55;   // 2.75 s window per block once the chase is live
	public static final int HOP_RADIUS = 2;     // the glow never travels further than this
	public static final int MAX_GLOWS = 3;      // how many seeds may exist at once
	private static final int SPAWN_INTERVAL = 60;
	private static final int SPAWN_SCAN_RADIUS = 10;

	/** Dormant seed glows, by world + position. */
	private record Glow(ServerWorld world, BlockPos pos) {
	}

	private static final class Chase {
		ServerWorld world;
		BlockPos pos;
		int count;
		int timer;
	}

	private static final Set<Glow> seeds = new HashSet<>();
	private static final Map<UUID, Chase> chases = new HashMap<>();
	private static int spawnClock = 0;

	private PickaxeRitual() {
	}

	public static boolean isEligible(ItemStack stack) {
		return UsageGate.kindOf(stack) == UsageGate.Kind.PICKAXE
			&& Tempering.getLevel(stack) == Rituals.RITUAL_FROM;
	}

	public static void register() {
		// A glowing block resists anyone without a tempered pickaxe.
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> {
			if (world.isClient() || !(world instanceof ServerWorld sw)) {
				return true;
			}
			if (!isGlowAt(sw, pos)) {
				return true;
			}
			return isEligible(player.getMainHandStack()); // false → break cancelled
		});

		// Breaking a glow with a tempered pickaxe seeds or advances the chase.
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, be) -> {
			if (world.isClient() || !(world instanceof ServerWorld sw)
				|| !(player instanceof ServerPlayerEntity sp) || !isEligible(sp.getMainHandStack())) {
				return;
			}
			Glow seed = new Glow(sw, pos);
			if (seeds.remove(seed)) {
				beginChase(sp, sw, pos);
				return;
			}
			Chase chase = chases.get(sp.getUuid());
			if (chase != null && chase.world == sw && chase.pos.equals(pos)) {
				advanceChase(sp, chase);
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(PickaxeRitual::tick);
		Heirloom.LOGGER.info("[Ritual] Pickaxe vein-glow chase registered.");
	}

	// ---- chase ---------------------------------------------------------------

	private static void beginChase(ServerPlayerEntity player, ServerWorld world, BlockPos seedPos) {
		BlockPos next = hopTarget(world, seedPos);
		if (next == null) {
			return; // nowhere to leap — the seed simply fizzles
		}
		Chase chase = new Chase();
		chase.world = world;
		chase.pos = next;
		chase.count = 1;
		chase.timer = CHASE_TICKS;
		chases.put(player.getUuid(), chase);
		trail(world, seedPos, next);
		player.sendMessage(Text.literal("The vein wakes — chase the glow!").formatted(Formatting.LIGHT_PURPLE), true);
	}

	private static void advanceChase(ServerPlayerEntity player, Chase chase) {
		chase.count++;
		if (chase.count >= REQUIRED) {
			chases.remove(player.getUuid());
			Rituals.finish(player.getMainHandStack(), chase.world, Vec3d.ofCenter(chase.pos), player);
			return;
		}
		BlockPos next = hopTarget(chase.world, chase.pos);
		if (next == null) {
			chases.remove(player.getUuid());
			player.sendMessage(Text.literal("The vein dead-ends and goes cold.").formatted(Formatting.GRAY), true);
			return;
		}
		trail(chase.world, chase.pos, next);
		chase.pos = next;
		chase.timer = CHASE_TICKS;
		player.sendMessage(Text.literal("Vein: " + chase.count + " / " + REQUIRED).formatted(Formatting.LIGHT_PURPLE), true);
	}

	/** A random exposed deepslate block within {@link #HOP_RADIUS} of {@code from}, or null. */
	private static BlockPos hopTarget(ServerWorld world, BlockPos from) {
		List<BlockPos> candidates = new ArrayList<>();
		for (int dx = -HOP_RADIUS; dx <= HOP_RADIUS; dx++) {
			for (int dy = -HOP_RADIUS; dy <= HOP_RADIUS; dy++) {
				for (int dz = -HOP_RADIUS; dz <= HOP_RADIUS; dz++) {
					if (dx == 0 && dy == 0 && dz == 0) {
						continue;
					}
					if (dx * dx + dy * dy + dz * dz > HOP_RADIUS * HOP_RADIUS) {
						continue; // keep it within a true 2-block reach
					}
					BlockPos p = from.add(dx, dy, dz);
					if (world.getBlockState(p).isOf(Blocks.DEEPSLATE) && isExposed(world, p)) {
						candidates.add(p);
					}
				}
			}
		}
		if (candidates.isEmpty()) {
			return null;
		}
		return candidates.get(world.getRandom().nextInt(candidates.size()));
	}

	private static boolean isExposed(ServerWorld world, BlockPos pos) {
		for (Direction d : Direction.values()) {
			if (world.getBlockState(pos.offset(d)).isAir()) {
				return true;
			}
		}
		return false;
	}

	// ---- tick: spawn, despawn, particles, timers -----------------------------

	private static void tick(net.minecraft.server.MinecraftServer server) {
		// Active chases: count down, fail on timeout, glow each tick.
		Iterator<Map.Entry<UUID, Chase>> ci = chases.entrySet().iterator();
		while (ci.hasNext()) {
			Map.Entry<UUID, Chase> e = ci.next();
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(e.getKey());
			Chase chase = e.getValue();
			if (player == null) {
				ci.remove();
				continue;
			}
			glow(chase.world, chase.pos);
			if (--chase.timer <= 0) {
				ci.remove();
				player.sendMessage(Text.literal("The glow gutters out — the vein is lost.").formatted(Formatting.GRAY), true);
			}
		}

		// Seeds: glow, and despawn once no one is near.
		int viewBlocks = server.getPlayerManager().getViewDistance() * 16 + 16;
		Iterator<Glow> si = seeds.iterator();
		while (si.hasNext()) {
			Glow seed = si.next();
			if (noPlayerNear(seed, viewBlocks)) {
				si.remove();
				continue;
			}
			glow(seed.world, seed.pos);
		}

		// Occasionally seed a new glow deep underground near a player.
		if (++spawnClock >= SPAWN_INTERVAL) {
			spawnClock = 0;
			trySpawn(server);
		}
	}

	private static boolean noPlayerNear(Glow seed, int blocks) {
		double r2 = (double) blocks * blocks;
		for (ServerPlayerEntity p : seed.world.getPlayers()) {
			if (p.squaredDistanceTo(Vec3d.ofCenter(seed.pos)) <= r2) {
				return false;
			}
		}
		return true;
	}

	private static void trySpawn(net.minecraft.server.MinecraftServer server) {
		if (seeds.size() >= MAX_GLOWS) {
			return;
		}
		List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
		if (players.isEmpty()) {
			return;
		}
		ServerPlayerEntity player = players.get(server.getOverworld().getRandom().nextInt(players.size()));
		if (player.getBlockY() >= 0) {
			return; // only deep, below Y=0
		}
		ServerWorld world = (ServerWorld) player.getEntityWorld();
		BlockPos origin = player.getBlockPos();
		for (int attempt = 0; attempt < 24; attempt++) {
			int dx = world.getRandom().nextInt(SPAWN_SCAN_RADIUS * 2 + 1) - SPAWN_SCAN_RADIUS;
			int dy = world.getRandom().nextInt(SPAWN_SCAN_RADIUS * 2 + 1) - SPAWN_SCAN_RADIUS;
			int dz = world.getRandom().nextInt(SPAWN_SCAN_RADIUS * 2 + 1) - SPAWN_SCAN_RADIUS;
			BlockPos p = origin.add(dx, dy, dz);
			if (p.getY() < 0 && world.getBlockState(p).isOf(Blocks.DEEPSLATE) && isExposed(world, p)) {
				seeds.add(new Glow(world, p));
				return;
			}
		}
	}

	// ---- particles -----------------------------------------------------------

	private static void glow(ServerWorld world, BlockPos pos) {
		Vec3d c = Vec3d.ofCenter(pos);
		world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y, c.z, 2, 0.3, 0.3, 0.3, 0.0);
		world.spawnParticles(ParticleTypes.ENCHANT, c.x, c.y, c.z, 4, 0.4, 0.4, 0.4, 0.02);
	}

	private static void trail(ServerWorld world, BlockPos from, BlockPos to) {
		Vec3d a = Vec3d.ofCenter(from);
		Vec3d b = Vec3d.ofCenter(to);
		for (int i = 0; i <= 8; i++) {
			double t = i / 8.0;
			world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
				a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t,
				1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	// ---- helpers -------------------------------------------------------------

	private static boolean isGlowAt(ServerWorld world, BlockPos pos) {
		if (seeds.contains(new Glow(world, pos))) {
			return true;
		}
		for (Chase chase : chases.values()) {
			if (chase.world == world && chase.pos.equals(pos)) {
				return true;
			}
		}
		return false;
	}
}
