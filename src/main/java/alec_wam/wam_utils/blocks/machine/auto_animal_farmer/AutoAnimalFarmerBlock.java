package alec_wam.wam_utils.blocks.machine.auto_animal_farmer;

import javax.annotation.Nullable;

import alec_wam.wam_utils.blocks.WAMUtilsBlockEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

public class AutoAnimalFarmerBlock extends WAMUtilsBlockEntityBlock {
    public static final String SCREEN_AUTO_ANIMAL_FARMER_BLOCK = "screen.wam_utils.auto_animal_farmer";
    
    public AutoAnimalFarmerBlock() {
    	super(Properties.of(Material.METAL)
                .sound(SoundType.METAL)
                .strength(2.0f)
                .requiresCorrectToolForDrops()
            );
	}
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AutoAnimalFarmerBE(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return (lvl, pos, stt, te) -> {
                if (te instanceof AutoAnimalFarmerBE milker) milker.tickServer();
            };
        }
        else {
        	return (lvl, pos, stt, te) -> {
                if (te instanceof AutoAnimalFarmerBE milker) milker.tickClient();
            };
        }
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult trace) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AutoAnimalFarmerBE) {
            	AutoAnimalFarmerBE milker = (AutoAnimalFarmerBE)be;
            	ItemStack heldStack = player.getItemInHand(hand);
            	if(heldStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent()) {
            		IFluidHandlerItem fluidHandler = heldStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
            		FluidStack storedMilk = milker.fluidStorage.getFluid();
            		int fillAmount = fluidHandler.fill(storedMilk, FluidAction.SIMULATE);
            		if(fillAmount > 0) {
            			int realFillAmount = fluidHandler.fill(storedMilk, FluidAction.EXECUTE);
            			milker.fluidStorage.forceDrain(realFillAmount, FluidAction.EXECUTE);
            			return InteractionResult.SUCCESS; 
            		}
            	}
            	
                MenuProvider containerProvider = new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable(SCREEN_AUTO_ANIMAL_FARMER_BLOCK);
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player playerEntity) {
                        return new AutoAnimalFarmerContainer(windowId, pos, playerInventory, playerEntity);
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
