package alec_wam.wam_utils.common.menu;

import alec_wam.wam_utils.common.blocks.BaseBE;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class AbstractBaseBEMenu<T extends BaseBE> extends AbstractContainerMenu {

    public final T blockEntity;
    public final ContainerLevelAccess access;

    @SuppressWarnings("unchecked")
    public AbstractBaseBEMenu(MenuType<?> menuType, int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        super(menuType, containerId);
        Level level = playerInv.player.level();
        BlockEntity foundBE = level.getBlockEntity(extraData.readBlockPos());
        this.blockEntity = foundBE instanceof BaseBE  ? (T) foundBE : null;
        this.access = ContainerLevelAccess.create(level, this.blockEntity.getBlockPos());
        this.addSlots(playerInv);
    }

    public AbstractBaseBEMenu(MenuType<?> menuType, int containerId, Inventory playerInv, final T blockEntity) {
        super(menuType, containerId);
        this.blockEntity = blockEntity;        
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.addSlots(playerInv);
    }

    public abstract void addSlots(Inventory playerInv);
    
    public abstract boolean isValidBlock(net.minecraft.world.level.block.state.BlockState state);

    @Override
    public boolean stillValid(Player player) {
        if(this.blockEntity == null || this.access == null) return false;
        return this.access
            .evaluate(
                (p_339525_, p_339526_) -> !this.isValidBlock(p_339525_.getBlockState(p_339526_)) ? false : player.canInteractWithBlock(p_339526_, 4.0), true
            );
    }
    
}
