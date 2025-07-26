package alec_wam.wam_utils.common.entities.workers.menu;

import java.util.Map;

import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.items.wrapper.EntityArmorInvWrapper;
import net.neoforged.neoforge.items.wrapper.EntityHandsInvWrapper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

public class WorkerInventoryMenu extends AbstractContainerMenu {
    private static final EquipmentSlot[] SLOT_IDS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Map<EquipmentSlot, ResourceLocation> TEXTURE_EMPTY_SLOTS = Map.of(
        EquipmentSlot.FEET,
        InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS,
        EquipmentSlot.LEGS,
        InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
        EquipmentSlot.CHEST,
        InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
        EquipmentSlot.HEAD,
        InventoryMenu.EMPTY_ARMOR_SLOT_HELMET
    );

    public static final int WORKER_SLOT_HANDS = 0;
    public static final int WORKER_SLOT_ARMOR = 2;
    public static final int WORKER_SLOT_INVENTORY = 6;
    
    public final WorkerEntity workerEntity;
    private CombinedInvWrapper combinedInvWrapper;

    public WorkerInventoryMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        super(ModInit.WORKER_INVENTORY_MENU_TYPE.get(), containerId);
        Level level = playerInv.player.level();
        Entity entity = level.getEntity(extraData.readInt());
        this.workerEntity = entity !=null && entity instanceof WorkerEntity ? (WorkerEntity)entity : null;
        
        this.addSlots(playerInv);
    }

    public WorkerInventoryMenu(int containerId, Inventory playerInv, final WorkerEntity workerEntity) {
        super(ModInit.WORKER_INVENTORY_MENU_TYPE.get(), containerId);
        this.workerEntity = workerEntity;        
        this.addSlots(playerInv);
    }

    public void addSlots(Inventory playerInv){
        if(this.workerEntity != null){
            combinedInvWrapper = new CombinedInvWrapper(
                new EntityHandsInvWrapper(workerEntity),
                new EntityArmorInvWrapper(workerEntity),
                new InvWrapper(workerEntity.getInventory())
            );

            this.addSlot(new SlotItemHandler(combinedInvWrapper, WORKER_SLOT_HANDS + 0, 77, 44) {
                @Override
                public void setByPlayer(ItemStack p_270969_, ItemStack p_299918_) {
                    workerEntity.onEquipItem(EquipmentSlot.MAINHAND, p_299918_, p_270969_);
                    super.setByPlayer(p_270969_, p_299918_);
                }
            });

            this.addSlot(new SlotItemHandler(combinedInvWrapper, WORKER_SLOT_HANDS + 1, 77, 62) {
                @Override
                public void setByPlayer(ItemStack p_270969_, ItemStack p_299918_) {
                    workerEntity.onEquipItem(EquipmentSlot.OFFHAND, p_299918_, p_270969_);
                    super.setByPlayer(p_270969_, p_299918_);
                }

                @Override
                public ResourceLocation getNoItemIcon() {
                    return InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD;
                }
            });

            for (int i = 0; i < 4; i++) {
                EquipmentSlot equipmentslot = SLOT_IDS[i];
                ResourceLocation resourcelocation = TEXTURE_EMPTY_SLOTS.get(equipmentslot);
                this.addSlot(new WorkerArmorSlot(combinedInvWrapper, workerEntity, equipmentslot, WORKER_SLOT_ARMOR + (3 - i), 8, 8 + i * 18, resourcelocation));
            }

            int workerInvX = 8;
            int workerInvY = 84;
            for (int i = 0; i < 9; i++) {
                this.addSlot(new SlotItemHandler(combinedInvWrapper, WORKER_SLOT_INVENTORY + i, workerInvX + i * 18, workerInvY));
            }
        }
        
        this.addStandardInventorySlots(playerInv, 8, 116);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if(this.workerEntity == null) return ItemStack.EMPTY;
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            EquipmentSlot equipmentslot = player.getEquipmentSlotForItem(itemstack);
            int workerInvSize = this.combinedInvWrapper.getSlots();
            if (index < workerInvSize) {
                if (!this.moveItemStackTo(itemstack1, workerInvSize, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (equipmentslot == EquipmentSlot.OFFHAND && !this.slots.get(WORKER_SLOT_HANDS + 1).hasItem()) {
                int i = WORKER_SLOT_HANDS + 1;
                if (!this.moveItemStackTo(itemstack1, i, i + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (equipmentslot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && !this.slots.get((WORKER_SLOT_ARMOR + 3) - equipmentslot.getIndex()).hasItem()) {
                int i = (WORKER_SLOT_ARMOR + 3) - equipmentslot.getIndex();
                if (!this.moveItemStackTo(itemstack1, i, i + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, WORKER_SLOT_INVENTORY - 1, workerInvSize, false)) {
                int j = workerInvSize + 27;
                int k = j + 9;
                if (index >= j && index < k) {
                    if (!this.moveItemStackTo(itemstack1, workerInvSize, j, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= workerInvSize && index < j) {
                    if (!this.moveItemStackTo(itemstack1, j, k, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemstack1, j, j, false)) {
                    return ItemStack.EMPTY;
                }

                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.workerEntity !=null && this.workerEntity.isAlive()
            && player.canInteractWithEntity(this.workerEntity, 8.0);
    }
}
