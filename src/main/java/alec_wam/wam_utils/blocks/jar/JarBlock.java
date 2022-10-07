package alec_wam.wam_utils.blocks.jar;

import javax.annotation.Nullable;

import alec_wam.wam_utils.blocks.WAMUtilsBlockEntityBlock;
import alec_wam.wam_utils.blocks.jar.JarBE.JarContents;
import alec_wam.wam_utils.utils.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class JarBlock extends WAMUtilsBlockEntityBlock implements SimpleWaterloggedBlock {
	private static final VoxelShape JAR_GLASS_SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 13.0D, 13.0D);
    private static final VoxelShape JARK_CORK_SHAPE = Block.box(5.0D, 13.0D, 5.0D, 11.0D, 15.0D, 11.0);
    private static final VoxelShape JAR_SHAPE = Shapes.or(JAR_GLASS_SHAPE, JARK_CORK_SHAPE);
    
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    
    public JarBlock() {
    	super(Properties.of(Material.GLASS)
                .sound(SoundType.GLASS)
                .strength(2.0f)
                .noOcclusion()
            );
    	this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, Boolean.valueOf(false)));
	}
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_153711_) {
       FluidState fluidstate = p_153711_.getLevel().getFluidState(p_153711_.getClickedPos());
       boolean flag = fluidstate.getType() == Fluids.WATER;
       return this.defaultBlockState().setValue(WATERLOGGED, Boolean.valueOf(flag));
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
    public VoxelShape getShape(BlockState p_54105_, BlockGetter p_54106_, BlockPos p_54107_, CollisionContext p_54108_) {
    	return JAR_SHAPE;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter world, BlockPos pos) {
        BlockEntity blockentity = world.getBlockEntity(pos);
        if (blockentity instanceof JarBE jar) {
        	if(jar.getContents() == JarContents.SHULKER || jar.getContents() == JarContents.LIGHTNING) {
        		return 15;
        	}
        }
    	return super.getLightEmission(state, world, pos);
    }   
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new JarBE(pos, state);
    }
    
    @Override
    public boolean hasAnalogOutputSignal(BlockState p_151986_) {
    	return true;
    }
    
    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
    	int comparatorValue = 0;
    	BlockEntity blockentity = level.getBlockEntity(pos);
        if (blockentity !=null && blockentity instanceof JarBE be) {
        	if(be.getContents() == JarContents.SHULKER) {
        		return 1;
        	}
        	comparatorValue += be.getBottleCount();
        }
    	return comparatorValue;
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (lvl, pos, stt, te) -> {
            if (te instanceof JarBE jar) jar.tickClient();
        };
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult trace) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof JarBE) {
            	JarBE jar = (JarBE)be;
            	if (!player.isShiftKeyDown()) {
	            	ItemStack heldStack = player.getItemInHand(hand);
	            	if(heldStack.is(Items.POTION)){
	    				Potion type = PotionUtils.getPotion(heldStack);
	    				//Only allow single potions
	    				if(type.getEffects().size() == 1 && (jar.getContents() == JarContents.EMPTY || jar.getContents() == JarContents.POTION) && (jar.getPotion() == type || jar.getPotion() == Potions.EMPTY)){
	    					if(jar.getBottleCount() < JarBE.BOTTLE_CAPACITY){
	    						if(jar.getPotion() == Potions.EMPTY){
	    							jar.setPotionType(type);
	    							jar.setContents(JarContents.POTION);
	    						}
	    						jar.setBottleCount(jar.getBottleCount() + 1);
	    						if(!player.isCreative()) {
	    							heldStack.shrink(1);	   
	    							ItemStack returnStack = new ItemStack(Items.GLASS_BOTTLE);
	    							if (heldStack.isEmpty()) {
	    	    						player.setItemInHand(hand, returnStack);
	    	    					} else {
	    	    						InventoryUtils.givePlayerItem(player, returnStack);
	    	    					}
	    						}
	    						level.playSound((Player)null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
	    						jar.setChanged();
	    						jar.markBlockForUpdate(null);
	    						return InteractionResult.SUCCESS;
	    					}
	    				}
	    			} 
	            	else if(heldStack.is(Items.HONEY_BOTTLE)){
	    				if((jar.getContents() == JarContents.EMPTY || jar.getContents() == JarContents.HONEY)){
	    					if(jar.getBottleCount() < JarBE.BOTTLE_CAPACITY){
	    						if(jar.getContents() == JarContents.EMPTY){
	    							jar.setContents(JarContents.HONEY);
	    						}
	    						jar.setBottleCount(jar.getBottleCount() + 1);
	    						if(!player.isCreative()) {
	    							heldStack.shrink(1);	   
	    							ItemStack returnStack = new ItemStack(Items.GLASS_BOTTLE);
	    							if (heldStack.isEmpty()) {
	    	    						player.setItemInHand(hand, returnStack);
	    	    					} else {
	    	    						InventoryUtils.givePlayerItem(player, returnStack);
	    	    					}
	    						}
	    						level.playSound((Player)null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
	    						jar.setChanged();
	    						jar.markBlockForUpdate(null);
	    						return InteractionResult.SUCCESS;
	    					}
	    				}
	    			} 
	            	else if(heldStack.is(Items.SHULKER_SHELL)) {
	            		if(jar.getContents() == JarContents.EMPTY){
	            			if(!player.isCreative()) {
            					heldStack.shrink(1);
            				}
	            			
	            			jar.setContents(JarContents.SHULKER);
	            			jar.setChanged();
    						jar.markBlockForUpdate(null);
    						level.getLightEngine().checkBlock(pos);
    						return InteractionResult.SUCCESS;
	            		}
	            	}
	            	else if(heldStack.is(Items.ITEM_FRAME)) {
	            		Direction dir = trace.getDirection();
	            		if(dir.getAxis() != Axis.Y) {
	            			if(!jar.hasLabel(dir)) {
	            				if(!player.isCreative()) {
	            					heldStack.shrink(1);
	            				}
	            				jar.setHasLabel(dir, true);
	            				jar.setChanged();
	    						jar.markBlockForUpdate(null);
	    						return InteractionResult.SUCCESS;
	            			}
	            			else {
	            				jar.setHasLabel(dir, false);
	            				jar.setChanged();
	    						jar.markBlockForUpdate(null);
	    						Block.popResourceFromFace(level, pos, dir, new ItemStack(Items.ITEM_FRAME));
	    						return InteractionResult.SUCCESS;
	            			}
	            		}
	            	}
	            	else if(heldStack.getItem() instanceof JarBlockItem) {
	            		
	            	}
	            	else if(heldStack.is(Items.GLASS_BOTTLE)){
	    				if(jar.getContents() == JarContents.POTION && jar.getPotion() != Potions.EMPTY && jar.getBottleCount() > 0){
	    					ItemStack returnStack = PotionUtils.setPotion(new ItemStack(Items.POTION), jar.getPotion());
	    					heldStack.shrink(1);
	    					if (heldStack.isEmpty()) {
	    						player.setItemInHand(hand, returnStack);
	    					} else {
	    						InventoryUtils.givePlayerItem(player, returnStack);
	    					}
	    					
	    					jar.setBottleCount(jar.getBottleCount() - 1);	    					
	    					if(jar.getBottleCount() <= 0){
    							jar.setPotionType(Potions.EMPTY);
    							jar.setContents(JarContents.EMPTY);
    						}
    						level.playSound((Player)null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
    						jar.setChanged();
    						jar.markBlockForUpdate(null);
    						return InteractionResult.SUCCESS;
	    				}
	    				else if(jar.getContents() == JarContents.HONEY && jar.getPotion() == Potions.EMPTY && jar.getBottleCount() > 0){
	    					ItemStack returnStack = new ItemStack(Items.HONEY_BOTTLE);
	    					heldStack.shrink(1);
	    					if (heldStack.isEmpty()) {
	    						player.setItemInHand(hand, returnStack);
	    					} else {
	    						InventoryUtils.givePlayerItem(player, returnStack);
	    					}
	    					
	    					jar.setBottleCount(jar.getBottleCount() - 1);	    					
	    					if(jar.getBottleCount() <= 0){
    							jar.setContents(JarContents.EMPTY);
    						}
    						level.playSound((Player)null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
    						jar.setChanged();
    						jar.markBlockForUpdate(null);
    						return InteractionResult.SUCCESS;
	    				}
	    			} 
            	}
            } 
            return InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }
}
