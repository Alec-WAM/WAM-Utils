package alec_wam.wam_utils.common.blocks.enchantment.bookshelf;

import alec_wam.wam_utils.common.blocks.BaseEntityBlock;
import alec_wam.wam_utils.common.helpers.BlockHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class EnchantmentBookshelfBlock extends BaseEntityBlock {

    protected static final VoxelShape SHAPE_NORTH = Block.box(0.0D, 0.0D, 12.0D, 16.0D, 16.0D, 16.0D);
	protected static final VoxelShape SHAPE_SOUTH = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 4.0D);
	protected static final VoxelShape SHAPE_EAST = Block.box(0.0D, 0.0D, 0.0D, 4.0D, 16.0D, 16.0D);
	protected static final VoxelShape SHAPE_WEST = Block.box(12.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public EnchantmentBookshelfBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, Boolean.valueOf(false)));
    }

    

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_48689_) {
    	FluidState fluidstate = p_48689_.getLevel().getFluidState(p_48689_.getClickedPos());
    	return this.defaultBlockState().setValue(FACING, p_48689_.getHorizontalDirection().getOpposite()).setValue(WATERLOGGED, Boolean.valueOf(fluidstate.getType() == Fluids.WATER));
    }
    
    @Override
    public BlockState rotate(BlockState p_48722_, Rotation p_48723_) {
        return p_48722_.setValue(FACING, p_48723_.rotate(p_48722_.getValue(FACING)));
    }

    @SuppressWarnings("deprecation")
	@Override
    public BlockState mirror(BlockState p_48719_, Mirror p_48720_) {
        return p_48719_.rotate(p_48720_.getRotation(p_48719_.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_48725_) {
    	p_48725_.add(FACING, WATERLOGGED);
    }

    @Override
    public FluidState getFluidState(BlockState p_153492_) {
       return p_153492_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_153492_);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter p_51310_, BlockPos p_51311_, CollisionContext p_51312_) {
       Direction dir = state.getValue(FACING);
       if(dir == Direction.SOUTH) {
    	   return SHAPE_SOUTH;
       }
       if(dir == Direction.EAST) {
    	   return SHAPE_EAST;
       }
       if(dir == Direction.WEST) {
    	   return SHAPE_WEST;
       }
       return SHAPE_NORTH;
    }
    
    @Override
    public boolean hasAnalogOutputSignal(BlockState p_151986_) {
    	return true;
    }
    
    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
    	BlockEntity blockentity = level.getBlockEntity(pos);
        if (blockentity !=null && blockentity instanceof EnchantmentBookshelfBE be) {
        	return ItemHandlerHelper.calcRedstoneFromInventory(be.getExternalItemHandler(null));
        }
    	return 0;
    }

    @Override
    public RenderShape getRenderShape(BlockState p_51307_) {
       return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnchantmentBookshelfBE(pos, state);
    }

    //136 Unique Scrolls to equal a level 30 enchantment
    //45 Enchanted Books to equal level 30 enchantment
    @Override
    public float getEnchantPowerBonus(BlockState state, LevelReader level, BlockPos pos)
    {
        float itemPower = 0.0F;
        
        BlockEntity be = level.getBlockEntity(pos);
        if(be !=null && be instanceof EnchantmentBookshelfBE shelf) {
            IItemHandler handler = shelf.getExternalItemHandler(null);
            if(handler == null) {
            	return 0.0F;
            }
        	for(int i = 0; i < handler.getSlots(); i++) {
        		ItemStack stack = handler.getStackInSlot(i);
        		if(stack.is(Items.ENCHANTED_BOOK)) {
        			//Three books per minecraft bookshelf
        			itemPower += 1.0F;
        		}
        		// if(stack.is(ItemInit.SINGLE_ENCHANTMENT_ITEM.get())) {
        		// 	//Ignore count stack.getCount()
        		// 	//itemPower += ((float)stack.getCount()) / 3.0F;
        		// 	itemPower += ((float)1.0F) / 3.0F; //Three Scrolls to a full enchanted book
        		// }
        	}
        }
        
        //power = (itemPower / SLOTS_IN_BOOKSHELF) * (REAL MINECRAFT SHELVES A FULL SHELF SIMULATES)
        float power = (itemPower / 24.0F) * 8.0F;
    	return power;
    }

    @Override
    public InteractionResult playerInteract(Level level, Player player, BlockPos blockPos, BlockHitResult hitResult,
            ItemStack stack, InteractionHand hand) {
        if (!level.isClientSide) {
	    	BlockEntity be = level.getBlockEntity(blockPos);
	        if (be instanceof EnchantmentBookshelfBE bookshelf) {
	        	IItemHandler inventory = bookshelf.getExternalItemHandler(null);
	        	if(!player.isShiftKeyDown()) {
	        		if(!stack.isEmpty() && inventory !=null) {
                        //TODO Make this a tag
	        			if(stack.is(Items.ENCHANTED_BOOK) || stack.is(Items.WRITTEN_BOOK)) {
	        				ItemStack copy = stack.copyWithCount(1);
	        				ItemStack insert = BlockHelper.insertItemStacked(inventory, copy, false);
	        				if(insert.isEmpty()) {
	        					if(!player.isCreative()){
                                    stack.shrink(1);
                                }
	        					return InteractionResult.SUCCESS_SERVER;
	        				}
	        			}
	        		}

	        		//Open Inventory
                    if (player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.openMenu(bookshelf);
                    }
	        	}
	        }
    	}
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isValidBE(BlockEntity blockEntity) {
        return blockEntity instanceof EnchantmentBookshelfBE;
    }
    
}
