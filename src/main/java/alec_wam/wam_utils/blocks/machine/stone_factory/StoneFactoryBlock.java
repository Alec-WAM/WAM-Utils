package alec_wam.wam_utils.blocks.machine.stone_factory;

import javax.annotation.Nullable;

import alec_wam.wam_utils.blocks.WAMUtilsBlockEntityBlock;
import alec_wam.wam_utils.utils.InventoryUtils;
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
import net.minecraftforge.network.NetworkHooks;

public class StoneFactoryBlock extends WAMUtilsBlockEntityBlock {
    public static final String SCREEN_NAME = "screen.wam_utils.stone_factory";
    
    public StoneFactoryBlock() {
    	super(Properties.of(Material.METAL)
                .sound(SoundType.METAL)
                .strength(2.0f)
                .requiresCorrectToolForDrops()
            );
	}
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StoneFactoryBE(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return (lvl, pos, stt, te) -> {
                if (te instanceof StoneFactoryBE factory) factory.tickServer();
            };
        }
        else {
        	return (lvl, pos, stt, te) -> {
                if (te instanceof StoneFactoryBE factory) factory.tickClient();
            };
        }
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult trace) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof StoneFactoryBE) {
            	StoneFactoryBE factory = (StoneFactoryBE)be;
            	ItemStack heldStack = player.getItemInHand(hand);
            	if(heldStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent()) {
            		if(InventoryUtils.handleFillTankInteraction(player, hand, heldStack, factory.fluidStorageWater, level, pos)
            				|| InventoryUtils.handleFillTankInteraction(player, hand, heldStack, factory.fluidStorageLava, level, pos)) {
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
                        return new StoneFactoryContainer(windowId, pos, playerInventory, playerEntity);
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
