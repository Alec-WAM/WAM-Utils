package alec_wam.wam_utils.blocks.mob_statue;

import com.mojang.datafixers.util.Pair;

import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.init.ContainerInit;
import alec_wam.wam_utils.server.container.WAMContainerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

public class MobStatueContainer extends WAMContainerMenu {

	public MobStatueBE blockEntity;
    private Player playerEntity;
    private IItemHandler playerInventory;
    public static final ResourceLocation[] TEXTURE_EMPTY_SLOTS = new ResourceLocation[]{InventoryMenu.EMPTY_ARMOR_SLOT_HELMET, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS};
    private static final EquipmentSlot[] SLOT_IDS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    
	public MobStatueContainer(int windowId, BlockPos pos, Inventory playerInventory, Player player) {
		super(ContainerInit.MOB_STATUE_CONTAINER.get(), windowId);
		blockEntity = (MobStatueBE) player.getCommandSenderWorld().getBlockEntity(pos);
        this.playerEntity = player;
        this.playerInventory = new InvWrapper(playerInventory);

        if (blockEntity != null) {
            addSlot(new SlotItemHandler(blockEntity.displayItems, 0, 62, 35));            
            addSlot(new SlotItemHandler(blockEntity.displayItems, 1, 98, 35) {
            	@Override
            	public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            		return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
            	}
            });
            for(int i = 0; i < 4; i++) {
            	final EquipmentSlot equipmentslot = SLOT_IDS[i];
            	final int slotIndex = i;
            	final int internalSlotIndex = blockEntity.getSlotIndex(equipmentslot);
	            addSlot(new SlotItemHandler(blockEntity.displayItems, 2 + i, 80, 17 + (18 * i)) {
	            	@Override
	            	public void set(ItemStack p_219985_) {
	            		ItemStack itemstack = this.getItem();
	            		super.set(p_219985_);
	            		//p_39708_.onEquipItem(equipmentslot, itemstack, p_219985_);
	            	}

	            	@Override
	            	public int getMaxStackSize() {
	            		return 1;
	            	}

	            	@Override
	            	public boolean mayPlace(ItemStack p_39746_) {
	            		return blockEntity.isValidDisplayItem(internalSlotIndex, p_39746_);
	            	}

	            	@Override
	            	public boolean mayPickup(Player p_39744_) {
	            		ItemStack itemstack = this.getItem();
	            		return !itemstack.isEmpty() && !p_39744_.isCreative() && EnchantmentHelper.hasBindingCurse(itemstack) ? false : super.mayPickup(p_39744_);
	            	}

	            	@Override
	            	public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
	            		return Pair.of(InventoryMenu.BLOCK_ATLAS, TEXTURE_EMPTY_SLOTS[slotIndex]);
	            	}
	            });
            }
            trackRotation();
        }
        layoutPlayerInventorySlots(this.playerInventory, 8, 104);
	}
	
	private void trackRotation() {
		//addGenericData(GenericContainerData.energy(this.blockEntity.energyStorage));
    }

	@Override
    public boolean stillValid(Player playerIn) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), playerEntity, BlockInit.MOB_STATUE_BLOCK.get());
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if(index < 6) {
            	//Display Slots
            	if(!this.moveItemStackTo(stack, 33, 42, false)) {
	            	if (!this.moveItemStackTo(stack, 6, 34, false)) {
	                  return ItemStack.EMPTY;
	            	}
            	}
            }
            else {
            	//Other Slots before hand
            	if (!this.moveItemStackTo(stack, 2, 6, false)) {
            		if (!this.moveItemStackTo(stack, 0, 2, false)) {
	            		if (index < 33 && !this.moveItemStackTo(stack, 33, 42, false)) {
	                      return ItemStack.EMPTY;
	            		}
	            		else if (!this.moveItemStackTo(stack, 6, 33, false)) {
	                        return ItemStack.EMPTY;
	              		}
            		}
            	}
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, stack);
        }

        return itemstack;
    }

}
