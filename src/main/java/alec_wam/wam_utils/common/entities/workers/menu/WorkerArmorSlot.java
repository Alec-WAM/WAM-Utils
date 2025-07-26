package alec_wam.wam_utils.common.entities.workers.menu;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class WorkerArmorSlot extends SlotItemHandler {
    private final LivingEntity owner;
    private final EquipmentSlot slot;
    @Nullable
    private final ResourceLocation emptyIcon;

    public WorkerArmorSlot(
        IItemHandler container, LivingEntity owner, EquipmentSlot slot, int slotIndex, int x, int y, @Nullable ResourceLocation emptyIcon
    ) {
        super(container, slotIndex, x, y);
        this.owner = owner;
        this.slot = slot;
        this.emptyIcon = emptyIcon;
    }

    @Override
    public void setByPlayer(ItemStack p_345031_, ItemStack p_344961_) {
        this.owner.onEquipItem(this.slot, p_344961_, p_345031_);
        super.setByPlayer(p_345031_, p_344961_);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean mayPlace(ItemStack p_345029_) {
        return p_345029_.canEquip(this.slot, this.owner);
    }

    @Override
    public boolean isActive() {
        return this.owner.canUseSlot(this.slot);
    }

    @Override
    public boolean mayPickup(Player p_345575_) {
        ItemStack itemstack = this.getItem();
        return !itemstack.isEmpty() && !p_345575_.isCreative() && EnchantmentHelper.has(itemstack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)
            ? false
            : super.mayPickup(p_345575_);
    }

    @Nullable
    @Override
    public ResourceLocation getNoItemIcon() {
        return this.emptyIcon;
    }
}
