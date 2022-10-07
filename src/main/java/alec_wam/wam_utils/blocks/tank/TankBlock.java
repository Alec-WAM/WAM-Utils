package alec_wam.wam_utils.blocks.tank;

import javax.annotation.Nullable;

import alec_wam.wam_utils.blocks.WAMUtilsBlockEntityBlock;
import alec_wam.wam_utils.utils.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidStack;

public class TankBlock extends WAMUtilsBlockEntityBlock {
    public static enum TankTowerPos  implements StringRepresentable {
    	NONE("none"),
    	BOTTOM("bottom"),
    	MIDDLE("middle"),
    	TOP("top");

    	private final String name;

    	private TankTowerPos(String p_61311_) {
    		this.name = p_61311_;
    	}

    	public String getSerializedName() {
    		return this.name;
    	}
    }
    
    public static final EnumProperty<TankTowerPos> TOWER_POS = EnumProperty.create("tower", TankTowerPos.class);
    
	public TankBlock() {
    	super(Properties.of(Material.METAL)
                .sound(SoundType.GLASS)
                .strength(2.0f)
                .noOcclusion()
                .requiresCorrectToolForDrops()
            );
    	this.registerDefaultState(this.stateDefinition.any().setValue(TOWER_POS, TankTowerPos.NONE));
	}

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_48725_) {
    	p_48725_.add(TOWER_POS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
    	Level level = context.getLevel();
    	BlockPos pos = context.getClickedPos();
    	return this.defaultBlockState().setValue(TOWER_POS, getTowerPos(level, pos));
    }
    
    @SuppressWarnings("deprecation")
	@Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block otherBlock, BlockPos otherPos, boolean p_60514_) {
    	super.neighborChanged(state, level, pos, otherBlock, otherPos, p_60514_);
    	
    	TankTowerPos newPos = getTowerPos(level, pos);
		if(newPos != state.getValue(TOWER_POS)) {
			BlockState newState = this.defaultBlockState().setValue(TOWER_POS, newPos);
			level.setBlock(pos, newState, 3);
		}
    }
    
    public TankTowerPos getTowerPos(Level level, BlockPos pos) {
    	TankTowerPos towerPos = TankTowerPos.NONE;
    	BlockState belowState = level.getBlockState(pos.below());
    	BlockState aboveState = level.getBlockState(pos.above());
    	boolean aboveIsTank = aboveState.getBlock() instanceof TankBlock;
    	boolean belowIsTank = belowState.getBlock() instanceof TankBlock;
    	if(aboveIsTank && belowIsTank) {
    		towerPos = TankTowerPos.MIDDLE;
    	}
    	else if(belowIsTank) {
    		towerPos = TankTowerPos.TOP;    		
    	}
    	else if(aboveIsTank) {
    		towerPos = TankTowerPos.BOTTOM;    		
    	}
    	return towerPos;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter world, BlockPos pos) {
        int ambientLight = super.getLightEmission(state, world, pos);
        if (ambientLight == 15) {
            //If we are already at the max light value don't bother looking up the tile to see if it has a fluid that gives off light
            return ambientLight;
        }
        BlockEntity be = world.getBlockEntity(pos);
        if (be != null && be instanceof TankBE tank) {
            FluidStack fluid = tank.fluidStorage.getFluid();
            if (!fluid.isEmpty()) {
                ambientLight = Math.max(ambientLight, fluid.getFluid().getFluidType().getLightLevel(fluid));
            }
        }
        return ambientLight;
    }  
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TankBE(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return (lvl, pos, stt, te) -> {
                if (te instanceof TankBE tank) tank.tickServer();
            };
        }
        else {
        	return (lvl, pos, stt, te) -> {
                if (te instanceof TankBE tank) tank.tickClient();
            };
        }
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult trace) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TankBE) {
            	TankBE tank = (TankBE)be;
            	if (!player.isShiftKeyDown()) {
	            	ItemStack heldStack = player.getItemInHand(hand);
	            	if(!heldStack.isEmpty() && InventoryUtils.handleTankInteraction(player, hand, heldStack, tank.fluidStorage, level, pos)) {
	            		player.getInventory().setChanged();
	                    return InteractionResult.SUCCESS;
	            	}
            	}
            } 
            return InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }
}
