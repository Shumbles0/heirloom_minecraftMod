package com.shumbles.gearoverhaul.ritual;

import com.mojang.serialization.MapCodec;
import com.shumbles.gearoverhaul.block.HeirloomBlockEntities;
import com.shumbles.gearoverhaul.temper.Tempering;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The sword rite's forge. Right-click with a sword tempered to 19 to start heating it; right-click
 * again to pull the (now hot) blade — it carries the heat it was pulled at. Quench it at the
 * Quenching Trough within the window to complete the rite. Behaviour is server-authoritative.
 */
public class RitualForgeBlock extends BlockWithEntity {
	public static final MapCodec<RitualForgeBlock> CODEC = createCodec(RitualForgeBlock::new);

	public RitualForgeBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return CODEC;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new RitualForgeBlockEntity(pos, state);
	}

	@Override
	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Override
	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		return world.isClient() ? null
			: validateTicker(type, HeirloomBlockEntities.RITUAL_FORGE, RitualForgeBlockEntity::tick);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient()) {
			return ActionResult.SUCCESS;
		}
		if (!(world.getBlockEntity(pos) instanceof RitualForgeBlockEntity forge)) {
			return ActionResult.PASS;
		}
		ItemStack held = player.getMainHandStack();

		if (forge.isHeating()) {
			// Pull: stamp the blade with the heat it was pulled at and the tick.
			int heatAtPull = forge.heat();
			ItemStack blade = forge.pull();
			blade.set(RitualComponents.FORGE_HEAT, heatAtPull);
			blade.set(RitualComponents.FORGE_PULL_TICK, world.getTime());
			if (held.isEmpty()) {
				player.setStackInHand(Hand.MAIN_HAND, blade);
			} else {
				player.getInventory().offerOrDrop(blade);
			}
			world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 0.6f, 1.2f);
			if (SwordRitual.isMelting(heatAtPull)) {
				// Melting is unavoidable ruin: the blade loses a temper level the instant it is pulled.
				int level = Tempering.getLevel(blade);
				if (level > Tempering.MIN_TEMPER) {
					Tempering.setLevel(blade, level - 1);
				}
				player.sendMessage(Text.literal("The sword melted — retemper it.").formatted(Formatting.RED), true);
			} else {
				player.sendMessage(Text.literal("You pull the glowing blade — quench it, quickly.")
					.formatted(Formatting.GOLD), true);
			}
			return ActionResult.SUCCESS;
		}

		if (SwordRitual.isEligible(held)) {
			forge.start(held.copy());
			player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
			world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 0.5f, 1.0f);
			player.sendMessage(Text.literal("The blade settles into the coals and begins to heat.")
				.formatted(Formatting.GRAY), true);
			return ActionResult.SUCCESS;
		}

		return ActionResult.PASS;
	}
}
