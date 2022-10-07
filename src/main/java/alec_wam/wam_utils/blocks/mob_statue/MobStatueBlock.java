package alec_wam.wam_utils.blocks.mob_statue;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import alec_wam.wam_utils.blocks.WAMUtilsBlockEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

public class MobStatueBlock extends WAMUtilsBlockEntityBlock implements SimpleWaterloggedBlock {
    public static final String SCREEN_MOB_STATUE_BLOCK = "screen.wam_utils.mob_statue";
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);
	
    public MobStatueBlock() {
    	super(Properties.of(Material.METAL)
                .sound(SoundType.METAL)
                .strength(2.0f)
                .requiresCorrectToolForDrops()
            );
    	this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, Boolean.valueOf(false)));
	}

    @Override
    public VoxelShape getShape(BlockState p_51309_, BlockGetter p_51310_, BlockPos p_51311_, CollisionContext p_51312_) {
       return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState p_51307_) {
       return RenderShape.MODEL;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MobStatueBE(pos, state);
    }   


    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext p_49163_) {
       FluidState fluidstate = p_49163_.getLevel().getFluidState(p_49163_.getClickedPos());
       return this.defaultBlockState().setValue(WATERLOGGED, Boolean.valueOf(fluidstate.getType() == Fluids.WATER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_153490_) {
       p_153490_.add(WATERLOGGED);
    }

    @SuppressWarnings("deprecation")
	@Override
    public FluidState getFluidState(BlockState p_153492_) {
       return p_153492_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_153492_);
    }

    @Override
    public boolean isPathfindable(BlockState p_153469_, BlockGetter p_153470_, BlockPos p_153471_, PathComputationType p_153472_) {
       return false;
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return (lvl, pos, stt, te) -> {
                if (te instanceof MobStatueBE milker) milker.tickServer();
            };
        }
        else {
        	return (lvl, pos, stt, te) -> {
                if (te instanceof MobStatueBE milker) milker.tickClient();
            };
        }
    }
    
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
    	super.setPlacedBy(level, pos, state, entity, stack);
    	if (!level.isClientSide) {
    		BlockEntity be = level.getBlockEntity(pos);
    		if (be instanceof MobStatueBE) {
    			MobStatueBE statue = (MobStatueBE)be;
    			Direction dir = entity.getDirection().getOpposite();
    			float f = dir.toYRot();
    			statue.setRotation((int)f);
    		}
    	}
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult trace) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MobStatueBE) {
            	MobStatueBE statue = (MobStatueBE)be;
            	ItemStack heldStack = player.getItemInHand(hand);
            	
            	if (!heldStack.isEmpty()) {
        			if (heldStack.getItem() instanceof SpawnEggItem) {
        				statue.setEntityFromStack(heldStack);
        				return InteractionResult.SUCCESS;
        			}
        		}
            	
            	MenuProvider containerProvider = new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable(SCREEN_MOB_STATUE_BLOCK);
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player playerEntity) {
                        return new MobStatueContainer(windowId, pos, playerInventory, playerEntity);
                    }
                };
                NetworkHooks.openScreen((ServerPlayer) player, containerProvider, be.getBlockPos());
            } else {
                throw new IllegalStateException("Our named container provider is missing!");
            }
        }
        return InteractionResult.SUCCESS;
    }
    
    @Override
    public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> stacks) {
    	super.fillItemCategory(tab, stacks);
    	if(allowedIn(tab)) {
	    	for(SpawnEggItem egg : SpawnEggItem.eggs()) {
	    		stacks.add(MobStatueBE.createStatueItem(egg.getType(null)));
	    	}
    	}
    }

    protected boolean allowedIn(CreativeModeTab p_220153_) {
    	if (asItem().getCreativeTabs().stream().anyMatch(tab -> tab == p_220153_)) return true;
    	CreativeModeTab creativemodetab = asItem().getItemCategory();
    	return creativemodetab != null && (p_220153_ == CreativeModeTab.TAB_SEARCH || p_220153_ == creativemodetab);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter getter, List<Component> tooltip, TooltipFlag flag) {
    	//super.appendHoverText(stack, getter, tooltip, flag);
    	if(stack.hasTag()) {
    		Optional<EntityType<?>> entityType = MobStatueBE.loadTypeFromStatueItem(stack);
    		if(entityType.isPresent()) {
    			ResourceLocation entityID = EntityType.getKey(entityType.get());
    			tooltip.add(Component.literal(entityID.toString()));
    		}
    	}
    }
}
