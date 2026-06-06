package com.shumbles.gearoverhaul.ritual;

import com.shumbles.gearoverhaul.block.HeirloomBlockEntities;
import com.shumbles.gearoverhaul.screen.SkeetLauncherScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * The Skeet Launcher's storage + behaviour. Holds loaded clay pigeons (a GUI inventory) and, while
 * receiving redstone power, fires one into the air every {@link BowRitual#FIRE_INTERVAL} ticks. It
 * also tracks the current archer's hit streak; five in a row with a tempered bow finishes the rite.
 *
 * <p>The ammo inventory persists; the streak is transient (a reload just resets it).
 */
public class SkeetLauncherBlockEntity extends BlockEntity implements Inventory, NamedScreenHandlerFactory {
	public static final int SIZE = 9;

	private final DefaultedList<ItemStack> ammo = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
	private int fireTimer = 0;
	private int streak = 0;
	private UUID lastShooter = null;

	public SkeetLauncherBlockEntity(BlockPos pos, BlockState state) {
		super(HeirloomBlockEntities.SKEET_LAUNCHER, pos, state);
	}

	// ---- firing -------------------------------------------------------------

	public static void tick(World world, BlockPos pos, BlockState state, SkeetLauncherBlockEntity be) {
		if (world.isClient()) {
			return;
		}
		if (!world.isReceivingRedstonePower(pos)) {
			be.fireTimer = 0;
			return;
		}
		if (++be.fireTimer >= BowRitual.FIRE_INTERVAL) {
			be.fireTimer = 0;
			be.fire((ServerWorld) world, pos);
		}
	}

	private void fire(ServerWorld world, BlockPos pos) {
		int slot = firstLoadedSlot();
		if (slot < 0) {
			return; // out of ammo
		}
		ammo.get(slot).decrement(1);
		markDirty();

		ClayPigeonEntity pigeon = new ClayPigeonEntity(RitualEntities.CLAY_PIGEON, world);
		pigeon.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0.0f, 0.0f);
		pigeon.setItem(new ItemStack(RitualItems.CLAY_PIGEON));
		// A slight random lean in any direction + a varied apex, so no two shots are alike.
		double angle = world.getRandom().nextDouble() * Math.PI * 2.0;
		double lean = 0.10 + world.getRandom().nextDouble() * 0.15;          // 0.10–0.25 horizontal
		double vy = BowRitual.PIGEON_LAUNCH_VY * (0.85 + world.getRandom().nextDouble() * 0.30); // ±15% height
		pigeon.setVelocity(Math.cos(angle) * lean, vy, Math.sin(angle) * lean);
		pigeon.setLauncher(pos);
		world.spawnEntity(pigeon);
		world.playSound(null, pos, SoundEvents.BLOCK_DISPENSER_LAUNCH, SoundCategory.BLOCKS, 1.0f, 1.2f);
	}

	private int firstLoadedSlot() {
		for (int i = 0; i < SIZE; i++) {
			if (ammo.get(i).isOf(RitualItems.CLAY_PIGEON) && !ammo.get(i).isEmpty()) {
				return i;
			}
		}
		return -1;
	}

	// ---- streak -------------------------------------------------------------

	public void recordHit(ServerWorld world, PlayerEntity shooter) {
		if (!BowRitual.isEligibleBow(shooter.getMainHandStack())) {
			return; // only a tempered bow advances the rite
		}
		if (lastShooter == null || !lastShooter.equals(shooter.getUuid())) {
			streak = 0;
			lastShooter = shooter.getUuid();
		}
		streak++;
		if (streak >= BowRitual.REQUIRED) {
			streak = 0;
			Vec3d at = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
			Rituals.finish(shooter.getMainHandStack(), world, at, shooter);
		} else {
			shooter.sendMessage(Text.literal(streak + "/" + BowRitual.REQUIRED).formatted(Formatting.GOLD), true);
		}
	}

	public void recordMiss(ServerWorld world) {
		if (streak > 0 && lastShooter != null) {
			PlayerEntity p = world.getPlayerByUuid(lastShooter);
			if (p != null) {
				p.sendMessage(Text.literal("Missed — the streak breaks.").formatted(Formatting.GRAY), true);
			}
		}
		streak = 0;
	}

	// ---- inventory ----------------------------------------------------------

	@Override
	public int size() {
		return SIZE;
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack stack : ammo) {
			if (!stack.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getStack(int slot) {
		return ammo.get(slot);
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		ItemStack result = Inventories.splitStack(ammo, slot, amount);
		if (!result.isEmpty()) {
			markDirty();
		}
		return result;
	}

	@Override
	public ItemStack removeStack(int slot) {
		return Inventories.removeStack(ammo, slot);
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		ammo.set(slot, stack);
		if (stack.getCount() > getMaxCountPerStack()) {
			stack.setCount(getMaxCountPerStack());
		}
		markDirty();
	}

	@Override
	public boolean isValid(int slot, ItemStack stack) {
		return stack.isOf(RitualItems.CLAY_PIGEON);
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return world != null && world.getBlockEntity(pos) == this
			&& player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
	}

	@Override
	public void clear() {
		ammo.clear();
	}

	// ---- nbt + menu ---------------------------------------------------------

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		Inventories.writeData(view, ammo);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		Inventories.readData(view, ammo);
	}

	@Override
	public Text getDisplayName() {
		return Text.translatable("block.heirloom.skeet_launcher");
	}

	@Override
	public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
		return new SkeetLauncherScreenHandler(syncId, playerInventory, this);
	}
}
