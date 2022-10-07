package alec_wam.wam_utils.blocks.advanced_portal;

import java.util.UUID;

import javax.annotation.Nullable;

import alec_wam.wam_utils.blocks.WAMUtilsBlockEntityBlock;
import alec_wam.wam_utils.init.ItemInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class AdvancedPortalHostBlock extends WAMUtilsBlockEntityBlock {
    public static final String SCREEN_NAME = "screen.wam_utils.advanced_portal_host";
    public static final EnumProperty<FrontAndTop> ORIENTATION = BlockStateProperties.ORIENTATION;
	
    public AdvancedPortalHostBlock() {
    	super(Properties.of(Material.METAL)
                .sound(SoundType.METAL)
                .strength(2.0f)
                .requiresCorrectToolForDrops()
            );
		this.registerDefaultState(this.stateDefinition.any().setValue(ORIENTATION, FrontAndTop.NORTH_UP));
	}

    @Override
	public BlockState rotate(BlockState p_54241_, Rotation p_54242_) {
    	return p_54241_.setValue(ORIENTATION, p_54242_.rotation().rotate(p_54241_.getValue(ORIENTATION)));
    }

    @Override
	public BlockState mirror(BlockState p_54238_, Mirror p_54239_) {
    	return p_54238_.setValue(ORIENTATION, p_54239_.rotation().rotate(p_54238_.getValue(ORIENTATION)));
    }

    @Override
	public BlockState getStateForPlacement(BlockPlaceContext p_54227_) {
    	Direction direction = p_54227_.getNearestLookingDirection().getOpposite();
    	Direction direction1;
    	if (direction.getAxis() == Direction.Axis.Y) {
    		direction1 = p_54227_.getHorizontalDirection();
    	} else {
    		direction1 = Direction.UP;
    	}

    	return this.defaultBlockState().setValue(ORIENTATION, FrontAndTop.fromFrontAndTop(direction, direction1));
    }

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_48725_) {
		p_48725_.add(ORIENTATION);
	}
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AdvancedPortalHostBE(pos, state);
    }
    
    @Override
	public void onRemove(BlockState p_49076_, Level p_49077_, BlockPos p_49078_, BlockState p_49079_, boolean p_49080_) {
    	if (!p_49076_.is(p_49079_.getBlock())) {
    		BlockEntity blockentity = p_49077_.getBlockEntity(p_49078_);
    		if (blockentity !=null && blockentity instanceof AdvancedPortalHostBE portal) {
    			portal.removePortal();
    		}
    		super.onRemove(p_49076_, p_49077_, p_49078_, p_49079_, p_49080_);        	
    	}
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return (lvl, pos, stt, te) -> {
                if (te instanceof AdvancedPortalHostBE portal) portal.tickServer();
            };
        }
        else {
        	return null;
        }
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult trace) {
    	//TODO Block users who are not the owner from opening screen
    	BlockEntity be = level.getBlockEntity(pos);
    	if(!player.isShiftKeyDown()) {
	    	if (be instanceof AdvancedPortalHostBE) {
	        	AdvancedPortalHostBE portal = (AdvancedPortalHostBE)be;
	        	ItemStack heldStack = player.getItemInHand(hand);
	        	if(heldStack.is(ItemInit.TELEPORT_CARD.get())) {
	        		GlobalPos teleportPos = PortalCardItem.getItemGlobalPos(heldStack, PortalCardItem.NBT_POS);
	        		if(teleportPos != null) {
	        			if(!level.isClientSide) {
	        				portal.setTeleportPos(teleportPos);
	        				portal.unlinkFromOtherPortal();
	        			}
	        			return InteractionResult.sidedSuccess(level.isClientSide);
	        		}
	        		
	        		if(heldStack.hasTag() && heldStack.getTag().contains(PortalCardItem.NBT_PORTAL)) {
		        		UUID otherPortalUUID = heldStack.getTag().getUUID(PortalCardItem.NBT_PORTAL);
		        		if(otherPortalUUID != null && !otherPortalUUID.equals(portal.getPortalUUID())) {
		        			if(!level.isClientSide) {
		        				portal.linkToOtherPortal(otherPortalUUID);
		        				portal.setTeleportPos(null);
		        			}
		        			return InteractionResult.sidedSuccess(level.isClientSide);
		        		}
	        		}
	        		
	        		if(!heldStack.hasTag() || !heldStack.getTag().contains(PortalCardItem.NBT_POS) && !heldStack.getTag().contains(PortalCardItem.NBT_PORTAL)) {
	        			heldStack.getOrCreateTag().putUUID(PortalCardItem.NBT_PORTAL, portal.getPortalUUID());
	        			return InteractionResult.sidedSuccess(level.isClientSide);
	        		}
	        		return InteractionResult.PASS;
	        	}
	        	if(heldStack.is(Items.STICK)) {
	        		if(!level.isClientSide) {
	    				portal.unlinkFromOtherPortal();
	    			}
	    			return InteractionResult.sidedSuccess(level.isClientSide);
	        	}
	        	
	        	if(!level.isClientSide) {
		            MenuProvider containerProvider = new MenuProvider() {
		                @Override
		                public Component getDisplayName() {
		                    return Component.translatable(SCREEN_NAME);
		                }
		
		                @Override
		                public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player playerEntity) {
		                    return new AdvancedPortalHostContainer(windowId, pos, playerInventory, playerEntity);
		                }
		            };
		            NetworkHooks.openScreen((ServerPlayer) player, containerProvider, be.getBlockPos());
	        	}
	            return InteractionResult.sidedSuccess(level.isClientSide);
	        }
    	}
        return InteractionResult.PASS;
    }
}
