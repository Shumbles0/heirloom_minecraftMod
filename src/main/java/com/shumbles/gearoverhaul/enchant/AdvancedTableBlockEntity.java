package com.shumbles.gearoverhaul.enchant;

import com.shumbles.gearoverhaul.block.HeirloomBlockEntities;
import com.shumbles.gearoverhaul.screen.AdvancedTableScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The Advanced Enchanting Table's storage + brewing. Slot 0 = gear, slot 1 = Arcane Dust (the base
 * reagent of every brew), slots 2-5 = materials (selectors + upgrade reagents). Brews auto-run,
 * brewing-stand style, over {@link #BREW_TICKS} ticks:
 * <ul>
 *   <li><b>Stir</b> — dust → readies the gear for an attribute (awkward-potion step).</li>
 *   <li><b>Attribute</b> — dust + a selector item → sets that direction's attribute.</li>
 *   <li><b>Upgrade</b> — dust + glowstone dust → +1 level on an attribute (softening its drawback).</li>
 * </ul>
 */
public class AdvancedTableBlockEntity extends BlockEntity implements Inventory, NamedScreenHandlerFactory {
	public static final int SIZE = 6;
	public static final int GEAR = 0;
	public static final int DUST = 1;
	public static final int MAT_START = 2;
	public static final int MAT_COUNT = 4;
	public static final int BREW_TICKS = 300; // ~15 s

	private static final int OP_NONE = 0;
	private static final int OP_STIR = 1;
	private static final int OP_ATTR = 2;
	private static final int OP_UPGRADE = 3;

	private final DefaultedList<ItemStack> items = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
	private int progress = 0;
	private boolean stirred = false;
	private int lastOp = OP_NONE;

	private final PropertyDelegate properties = new PropertyDelegate() {
		@Override
		public int get(int index) {
			return index == 0 ? progress : (index == 1 ? BREW_TICKS : 0);
		}

		@Override
		public void set(int index, int value) {
			if (index == 0) {
				progress = value;
			}
		}

		@Override
		public int size() {
			return 2;
		}
	};

	public AdvancedTableBlockEntity(BlockPos pos, BlockState state) {
		super(HeirloomBlockEntities.ADVANCED_TABLE, pos, state);
	}

	public PropertyDelegate getProperties() {
		return properties;
	}

	// ---- brewing ------------------------------------------------------------

	public static void tick(World world, BlockPos pos, BlockState state, AdvancedTableBlockEntity be) {
		if (world.isClient()) {
			return;
		}
		if (be.getStack(GEAR).isEmpty()) {
			be.stirred = false;
		}
		int op = be.currentOp();
		if (op == OP_NONE) {
			if (be.progress != 0) {
				be.progress = 0;
				be.markDirty();
			}
			be.lastOp = OP_NONE;
			return;
		}
		if (op != be.lastOp) {
			be.progress = 0;
			be.lastOp = op;
		}
		be.progress++;
		if (be.progress % 45 == 0) {
			world.playSound(null, pos, SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS,
				0.6f, 0.8f + 0.6f * be.progress / BREW_TICKS);
		}
		if (be.progress >= BREW_TICKS) {
			be.complete((ServerWorld) world, pos, op);
			be.progress = 0;
			be.lastOp = OP_NONE;
		}
		be.markDirty();
	}

	private int currentOp() {
		ItemStack gear = getStack(GEAR);
		if (!EnchantDirections.canTakeDirections(gear)) {
			return OP_NONE;
		}
		if (!getStack(DUST).isOf(EnchantItems.ARCANE_DUST) || getStack(DUST).getCount() < 1) {
			return OP_NONE;
		}
		if (findMaterial(Items.GLOWSTONE_DUST) >= 0 && EnchantDirections.firstUpgradeableSlot(gear) >= 0) {
			return OP_UPGRADE;
		}
		if (stirred && findSelector(gear) != null) {
			return OP_ATTR;
		}
		if (!stirred && EnchantDirections.hasUnsetDirection(gear)) {
			return OP_STIR;
		}
		return OP_NONE;
	}

	private void complete(ServerWorld world, BlockPos pos, int op) {
		ItemStack gear = getStack(GEAR);
		switch (op) {
			case OP_STIR -> {
				stirred = true;
				getStack(DUST).decrement(1);
			}
			case OP_ATTR -> {
				EnchantDirections.Selection s = findSelector(gear);
				int matSlot = findSelectorSlot(gear);
				if (s != null) {
					EnchantComponents.setAttribute(gear, s.slot(), s.attribute());
					stirred = false;
					getStack(DUST).decrement(1);
					if (matSlot >= 0) {
						getStack(matSlot).decrement(1);
					}
				}
			}
			case OP_UPGRADE -> {
				int slot = EnchantDirections.firstUpgradeableSlot(gear);
				int glow = findMaterial(Items.GLOWSTONE_DUST);
				if (slot >= 0 && glow >= 0) {
					EnchantComponents.setLevel(gear, slot, EnchantComponents.levelAt(gear, slot) + 1);
					getStack(DUST).decrement(1);
					getStack(glow).decrement(1);
				}
			}
			default -> {
			}
		}
		// Re-bake the gear's attribute modifiers so the new attribute / level takes effect.
		com.shumbles.gearoverhaul.temper.TemperStats.refresh(getStack(GEAR));
		world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 0.8f, 1.1f);
		world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 0.7f, 1.4f);
		markDirty();
	}

	private EnchantDirections.Selection findSelector(ItemStack gear) {
		for (int i = MAT_START; i < MAT_START + MAT_COUNT; i++) {
			ItemStack m = getStack(i);
			if (m.isEmpty()) {
				continue;
			}
			EnchantDirections.Selection s = EnchantDirections.matchSelector(gear, m.getItem());
			if (s != null) {
				return s;
			}
		}
		return null;
	}

	private int findSelectorSlot(ItemStack gear) {
		for (int i = MAT_START; i < MAT_START + MAT_COUNT; i++) {
			ItemStack m = getStack(i);
			if (!m.isEmpty() && EnchantDirections.matchSelector(gear, m.getItem()) != null) {
				return i;
			}
		}
		return -1;
	}

	private int findMaterial(Item item) {
		for (int i = MAT_START; i < MAT_START + MAT_COUNT; i++) {
			if (getStack(i).isOf(item)) {
				return i;
			}
		}
		return -1;
	}

	// ---- inventory ----------------------------------------------------------

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
		if (slot == GEAR && items.get(GEAR).getItem() != stack.getItem()) {
			stirred = false; // a different piece was placed
		}
		items.set(slot, stack);
		if (stack.getCount() > getMaxCount(slot)) {
			stack.setCount(getMaxCount(slot));
		}
		markDirty();
	}

	private int getMaxCount(int slot) {
		return slot == GEAR ? 1 : 64;
	}

	@Override
	public boolean isValid(int slot, ItemStack stack) {
		if (slot == GEAR) {
			return EnchantDirections.canTakeDirections(stack);
		}
		if (slot == DUST) {
			return stack.isOf(EnchantItems.ARCANE_DUST);
		}
		return true;
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

	public DefaultedList<ItemStack> getItems() {
		return items;
	}

	// ---- nbt + menu ---------------------------------------------------------

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		Inventories.writeData(view, items);
		view.putInt("Progress", progress);
		view.putBoolean("Stirred", stirred);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		Inventories.readData(view, items);
		progress = view.getInt("Progress", 0);
		stirred = view.getBoolean("Stirred", false);
	}

	@Override
	public Text getDisplayName() {
		return Text.translatable("block.heirloom.advanced_enchanting_table");
	}

	@Override
	public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
		return new AdvancedTableScreenHandler(syncId, playerInventory, this, properties);
	}
}
