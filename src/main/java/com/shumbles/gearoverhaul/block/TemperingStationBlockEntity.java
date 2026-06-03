package com.shumbles.gearoverhaul.block;

import com.shumbles.gearoverhaul.screen.TemperingStationScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

/**
 * Holds the station's inventory (slot 0 = gear, slots 1–6 = materials) and opens the
 * tempering menu. The tempering action itself lives in {@link TemperingStationScreenHandler}.
 */
public class TemperingStationBlockEntity extends BlockEntity implements Inventory, NamedScreenHandlerFactory {
	public static final int SIZE = 7;
	public static final int GEAR_SLOT = 0;
	public static final int FIRST_MATERIAL_SLOT = 1;
	public static final int MATERIAL_SLOTS = 6;

	private final DefaultedList<ItemStack> items = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);

	public TemperingStationBlockEntity(BlockPos pos, BlockState state) {
		super(HeirloomBlockEntities.TEMPERING_STATION, pos, state);
	}

	// --- NBT ---

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		Inventories.writeData(view, items);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		Inventories.readData(view, items);
	}

	// --- Inventory ---

	@Override
	public int size() {
		return SIZE;
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack stack : items) {
			if (!stack.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getStack(int slot) {
		return items.get(slot);
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		ItemStack result = Inventories.splitStack(items, slot, amount);
		if (!result.isEmpty()) {
			markDirty();
		}
		return result;
	}

	@Override
	public ItemStack removeStack(int slot) {
		return Inventories.removeStack(items, slot);
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		items.set(slot, stack);
		if (stack.getCount() > getMaxCountPerStack()) {
			stack.setCount(getMaxCountPerStack());
		}
		markDirty();
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return world != null && world.getBlockEntity(pos) == this
			&& player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
	}

	@Override
	public void clear() {
		items.clear();
	}

	// --- Menu ---

	@Override
	public Text getDisplayName() {
		return Text.translatable("block.heirloom.tempering_station");
	}

	@Override
	public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
		return new TemperingStationScreenHandler(syncId, playerInventory, this,
			ScreenHandlerContext.create(world, pos));
	}
}
