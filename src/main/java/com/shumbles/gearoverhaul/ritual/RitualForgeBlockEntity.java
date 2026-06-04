package com.shumbles.gearoverhaul.ritual;

import com.shumbles.gearoverhaul.block.HeirloomBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Holds the single blade being heated and ticks its heat upward. Audible cues mark the cherry
 * band (high "ready" clink) and white-hot (low "too hot" clink); particles intensify with heat.
 * Past {@link SwordRitual#RUINED} the blade is ejected on its own (a retry, no level loss).
 *
 * <p>Heat itself is not persisted — only the blade is — so a reload simply restarts the heat-up.
 */
public class RitualForgeBlockEntity extends BlockEntity {
	private final DefaultedList<ItemStack> blade = DefaultedList.ofSize(1, ItemStack.EMPTY);
	private int heat = 0;
	private int announcedStage = -1; // highest stage whose sound has played

	public RitualForgeBlockEntity(BlockPos pos, BlockState state) {
		super(HeirloomBlockEntities.RITUAL_FORGE, pos, state);
	}

	public boolean isHeating() {
		return !blade.get(0).isEmpty();
	}

	public int heat() {
		return heat;
	}

	/** Place a blade in the forge and (re)start heating from cold. */
	public void start(ItemStack stack) {
		blade.set(0, stack);
		reset();
		markDirty();
	}

	/** Take the blade out and reset; the caller stamps it with the pulled heat. */
	public ItemStack pull() {
		ItemStack out = blade.get(0);
		blade.set(0, ItemStack.EMPTY);
		reset();
		markDirty();
		return out;
	}

	private void reset() {
		heat = 0;
		announcedStage = -1;
	}

	public static void tick(World world, BlockPos pos, BlockState state, RitualForgeBlockEntity be) {
		if (world.isClient() || be.blade.get(0).isEmpty()) {
			return;
		}
		ServerWorld sw = (ServerWorld) world;
		if (be.heat < SwordRitual.MAX_HEAT) {
			be.heat++;
		}
		double cx = pos.getX() + 0.5, cy = pos.getY() + 1.0, cz = pos.getZ() + 0.5;

		// A distinct sound the first time each stage is reached.
		int stage = SwordRitual.stageIndex(be.heat);
		while (be.announcedStage < stage) {
			be.announcedStage++;
			playStageSound(sw, pos, be.announcedStage);
		}

		// Particles intensify with heat; smoke only once it's melting (searing gives no "bad" tell).
		if (world.getTime() % 5 == 0) {
			int n = 1 + be.heat / 150;
			sw.spawnParticles(ParticleTypes.FLAME, cx, cy, cz, n, 0.2, 0.1, 0.2, 0.01);
			if (SwordRitual.isMelting(be.heat)) {
				sw.spawnParticles(ParticleTypes.SMOKE, cx, cy, cz, 4, 0.2, 0.1, 0.2, 0.01);
			}
		}

		// Show the current stage on the action bar to anyone tending the forge.
		if (world.getTime() % 10 == 0) {
			Text label = Text.literal("Forge: " + SwordRitual.stageName(be.heat)).formatted(stageColor(be.heat));
			for (ServerPlayerEntity p : sw.getPlayers()) {
				if (p.squaredDistanceTo(cx, cy, cz) <= 64.0) {
					p.sendMessage(label, true);
				}
			}
		}
	}

	/** A different cue per stage: rising clinks, a bright tone at white-hot, a hiss when burning up. */
	private static void playStageSound(ServerWorld sw, BlockPos pos, int stage) {
		SoundEvent sound;
		float vol = 0.6f, pitch;
		switch (stage) {
			case 0 -> { sound = SoundEvents.BLOCK_ANVIL_USE; pitch = 0.7f; }       // Warming
			case 1 -> { sound = SoundEvents.BLOCK_ANVIL_USE; pitch = 0.9f; }       // Dull red
			case 2 -> { sound = SoundEvents.BLOCK_ANVIL_USE; pitch = 1.1f; }       // Cherry red
			case 3 -> { sound = SoundEvents.ITEM_TOTEM_USE; vol = 0.4f; pitch = 1.0f; } // Orange-hot
			case 4 -> { sound = SoundEvents.BLOCK_BEACON_ACTIVATE; vol = 0.8f; pitch = 1.5f; } // White-hot (ready!)
			case 5 -> { sound = SoundEvents.BLOCK_ANVIL_USE; pitch = 1.3f; }                  // Searing (sounds like more heat)
			case 6 -> { sound = SoundEvents.BLOCK_ANVIL_USE; vol = 0.9f; pitch = 0.4f; }      // Melting (deep alarm)
			default -> { return; }
		}
		sw.playSound(null, pos, sound, SoundCategory.BLOCKS, vol, pitch);
	}

	private static Formatting stageColor(int heat) {
		if (SwordRitual.isMelting(heat)) {
			return Formatting.RED;
		}
		if (SwordRitual.inWhiteHot(heat)) {
			return Formatting.YELLOW;
		}
		if (heat > SwordRitual.WHITEHOT_MAX) {
			return Formatting.WHITE; // searing — deceptively neutral, no danger tell
		}
		return Formatting.GRAY;
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		Inventories.writeData(view, blade);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		Inventories.readData(view, blade);
	}
}
