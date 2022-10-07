package alec_wam.wam_utils.blocks.bookshelf;

import javax.annotation.Nullable;

import alec_wam.wam_utils.blocks.WAMUtilsBlockEntityBlock;
import alec_wam.wam_utils.init.ItemInit;
import alec_wam.wam_utils.utils.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

public class EnchantmentBookshelfBlock extends WAMUtilsBlockEntityBlock implements SimpleWaterloggedBlock {
	public static final String SCREEN_NAME = "screen.wam_utils.enchantment_bookshelf";
    protected static final VoxelShape SHAPE_NORTH = Block.box(0.0D, 0.0D, 12.0D, 16.0D, 16.0D, 16.0D);
	protected static final VoxelShape SHAPE_SOUTH = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 4.0D);
	protected static final VoxelShape SHAPE_EAST = Block.box(0.0D, 0.0D, 0.0D, 4.0D, 16.0D, 16.0D);
	protected static final VoxelShape SHAPE_WEST = Block.box(12.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    
    public EnchantmentBookshelfBlock() {
    	//TODO Map color
    	super(Properties.of(Material.WOOD)
                .sound(SoundType.WOOD)
                .strength(1.5f)
                .noOcclusion()
            );
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

    @SuppressWarnings("deprecation")
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
    public RenderShape getRenderShape(BlockState p_51307_) {
       return RenderShape.MODEL;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnchantmentBookshelfBE(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return (lvl, pos, stt, te) -> {
                if (te instanceof EnchantmentBookshelfBE shelf) shelf.tickServer();
            };
        }
        else {
        	return (lvl, pos, stt, te) -> {
                if (te instanceof EnchantmentBookshelfBE shelf) shelf.tickClient();
            };
        }
    }
    
    //136 Unique Scrolls to equal a level 30 enchantment
    //45 Enchanted Books to equal level 30 enchantment
    @Override
    public float getEnchantPowerBonus(BlockState state, LevelReader level, BlockPos pos)
    {
        float itemPower = 0.0F;
        
        BlockEntity be = level.getBlockEntity(pos);
        if(be !=null && be instanceof EnchantmentBookshelfBE shelf) {
        	for(int i = 0; i < shelf.bookItems.getSlots(); i++) {
        		ItemStack stack = shelf.bookItems.getStackInSlot(i);
        		if(stack.is(Items.ENCHANTED_BOOK)) {
        			//Three books per minecraft bookshelf
        			itemPower += 1.0F;
        		}
        		if(stack.is(ItemInit.SINGLE_ENCHANTMENT_ITEM.get())) {
        			//Ignore count stack.getCount()
        			//itemPower += ((float)stack.getCount()) / 3.0F;
        			itemPower += ((float)1.0F) / 3.0F; //Three Scrolls to a full enchanted book
        		}
        	}
        }
        
        //power = (itemPower / SLOTS_IN_BOOKSHELF) * (REAL MINECRAFT SHELVES A FULL SHELF SIMULATES)
        float power = (itemPower / 24.0F) * 8.0F;
    	return power;
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult trace) {
    	if (!level.isClientSide) {
	    	BlockEntity be = level.getBlockEntity(pos);
	        if (be instanceof EnchantmentBookshelfBE) {
	        	EnchantmentBookshelfBE shelf = (EnchantmentBookshelfBE)be;
	        	
	        	if(!player.isShiftKeyDown()) {
	        		ItemStack heldItem = player.getItemInHand(hand);
	        		if(!heldItem.isEmpty()) {
	        			if(heldItem.is(Items.ENCHANTED_BOOK) || heldItem.is(Items.WRITTEN_BOOK) || heldItem.is(ItemInit.SINGLE_ENCHANTMENT_ITEM.get())) {
	        				ItemStack copy = heldItem.copy();
	        				copy.setCount(1);
	        				ItemStack insert = InventoryUtils.putStackInInventoryAllSlots(shelf.bookItems, copy);
	        				if(insert.isEmpty()) {
	        					heldItem.shrink(1);
	        					return InteractionResult.SUCCESS;
	        				}
	        			}
	        		}

	        		//TODO Handle book interaction with extract with hand
	        		MenuProvider containerProvider = new MenuProvider() {
	        			@Override
	        			public Component getDisplayName() {
	        				return Component.translatable(SCREEN_NAME);
	        			}

	        			@Override
	        			public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player playerEntity) {
	        				return new EnchantmentBookshelfContainer(windowId, pos, playerInventory, playerEntity);
	        			}
	        		};
	        		NetworkHooks.openScreen((ServerPlayer) player, containerProvider, be.getBlockPos());
	        	}
	        }
    	}
        return InteractionResult.SUCCESS;
    }
}
