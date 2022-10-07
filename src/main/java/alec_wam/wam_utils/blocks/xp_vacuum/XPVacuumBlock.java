package alec_wam.wam_utils.blocks.xp_vacuum;

import javax.annotation.Nullable;

import alec_wam.wam_utils.blocks.WAMUtilsBlockEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.network.NetworkHooks;

public class XPVacuumBlock extends WAMUtilsBlockEntityBlock {
    public static final String SCREEN_NAME = "screen.wam_utils.xp_vacuum";
    
    public XPVacuumBlock() {
    	super(Properties.of(Material.METAL)
                .sound(SoundType.METAL)
                .strength(2.0f)
                .requiresCorrectToolForDrops()
            );
	}
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new XPVacuumBE(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (lvl, pos, stt, te) -> {
            if (te instanceof XPVacuumBE be) be.tickCommon();
        };
    }
    
    @Override
    public void dropInventoryItems(Level level, BlockPos pos) {
    	BlockEntity blockentity = level.getBlockEntity(pos);
    	if (blockentity instanceof XPVacuumBE be) {
    		ItemStack stack = be.upgradeItems.getStackInSlot(0);
    		if(!stack.isEmpty()) {
    			Containers.dropItemStack(level, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), stack);
    		}
    		level.updateNeighbourForOutputSignal(pos, this);
    	}
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult trace) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof XPVacuumBE) {
            	XPVacuumBE xpVacuum = (XPVacuumBE)be;
            	ItemStack heldStack = player.getItemInHand(hand);
            	if(heldStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent()) {
            		IFluidHandlerItem fluidHandler = heldStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
            		FluidStack storedXP = xpVacuum.fluidStorage.getFluid();
            		int fillAmount = fluidHandler.fill(storedXP, FluidAction.SIMULATE);
            		if(fillAmount > 0) {
            			int realFillAmount = fluidHandler.fill(storedXP, FluidAction.EXECUTE);
            			xpVacuum.fluidStorage.drain(realFillAmount, FluidAction.EXECUTE);
            			return InteractionResult.SUCCESS; 
            		}
            	}
            	
                MenuProvider containerProvider = new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable(SCREEN_NAME);
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player playerEntity) {
                        return new XPVacuumContainer(windowId, pos, playerInventory, playerEntity);
                    }
                };
                NetworkHooks.openScreen((ServerPlayer) player, containerProvider, be.getBlockPos());
            } else {
                throw new IllegalStateException("Our named container provider is missing!");
            }
        }
        return InteractionResult.SUCCESS;
    }
}
