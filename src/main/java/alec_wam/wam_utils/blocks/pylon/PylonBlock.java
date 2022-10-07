package alec_wam.wam_utils.blocks.pylon;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;

public abstract class PylonBlock extends Block implements EntityBlock {
	public static enum PylonLevel implements StringRepresentable {
		BOTTOM("bottom"), MIDDLE("middle"), TOP("top");

		final String name;
		PylonLevel(String name){
			this.name = name;
		}
		
		@Override
		public String getSerializedName() {
			return name;
		}
	}
	
	public static final EnumProperty<PylonLevel> PYLON_LEVEL = EnumProperty.create("pylon_level", PylonLevel.class);
    
    public PylonBlock() {
    	super(Properties.of(Material.STONE)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops()
                .strength(1.5F, 6.0F)
            );
    	this.registerDefaultState(this.stateDefinition.any().setValue(PYLON_LEVEL, PylonLevel.BOTTOM));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_48689_) {
    	return this.defaultBlockState();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_48725_) {
    	p_48725_.add(PYLON_LEVEL);
    }
    
    @Override
    public void tick(BlockState p_222543_, ServerLevel p_222544_, BlockPos p_222545_, RandomSource p_222546_) {
        if (!p_222543_.canSurvive(p_222544_, p_222545_)) {
           p_222544_.destroyBlock(p_222545_, true);
        }
    }
    
    @SuppressWarnings({ "deprecation" })
	@Override
    public BlockState updateShape(BlockState p_57179_, Direction p_57180_, BlockState p_57181_, LevelAccessor p_57182_, BlockPos p_57183_, BlockPos p_57184_) {
        if (!p_57179_.canSurvive(p_57182_, p_57183_)) {
           p_57182_.scheduleTick(p_57183_, this, 1);
        }

        return super.updateShape(p_57179_, p_57180_, p_57181_, p_57182_, p_57183_, p_57184_);
     }
    
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
    	boolean structureCheck = true;
    	if(structureCheck) {
          	PylonLevel pylonLevel = state.getValue(PYLON_LEVEL);
	    	BlockState below = level.getBlockState(pos.below());
	    	BlockState above = level.getBlockState(pos.above());
	    	if(pylonLevel == PylonLevel.BOTTOM) {
	    		boolean isMiddlePylonAbove = above.is(this) && above.getValue(PYLON_LEVEL) == PylonLevel.MIDDLE;
	    		boolean solidBelow = !below.isAir() && below.isFaceSturdy(level, pos.below(), Direction.UP);
	    		return solidBelow && isMiddlePylonAbove;
	    	}
	    	else if(pylonLevel == PylonLevel.MIDDLE) {
	    		boolean isBottomPylonBelow = below.is(this) && below.getValue(PYLON_LEVEL) == PylonLevel.BOTTOM;
	    		boolean isTopPylonAbove = above.is(this) && above.getValue(PYLON_LEVEL) == PylonLevel.TOP;
	    		return isBottomPylonBelow && isTopPylonAbove;
	    	}
	    	else if(pylonLevel == PylonLevel.TOP) {
	    		boolean isMiddlePylonBelow = below.is(this) && below.getValue(PYLON_LEVEL) == PylonLevel.MIDDLE;
	    		return isMiddlePylonBelow;
	    	}  
	    	return false;
    	}
    	return true;
     }
    

    //Cleanup to prevent unneeded drops in Creative Mode
    @Override
    public void playerWillDestroy(Level p_52755_, BlockPos p_52756_, BlockState p_52757_, Player p_52758_) {
       if (!p_52755_.isClientSide && p_52758_.isCreative()) {
    	   PylonLevel pylonLevel = p_52757_.getValue(PYLON_LEVEL);
    	   if (pylonLevel == PylonLevel.BOTTOM) {
    		   BlockPos blockpos = p_52756_.above();
    		   BlockState blockstate = p_52755_.getBlockState(blockpos);
    		   if (blockstate.is(p_52757_.getBlock()) && blockstate.getValue(PYLON_LEVEL) == PylonLevel.MIDDLE) {
    			   BlockState blockstate1 = Blocks.AIR.defaultBlockState();
    			   p_52755_.setBlock(blockpos, blockstate1, 35);
    			   p_52755_.levelEvent(p_52758_, 2001, blockpos, Block.getId(blockstate));
    		   }
    		   blockpos = p_52756_.above(2);
    		   blockstate = p_52755_.getBlockState(blockpos);
    		   if (blockstate.is(p_52757_.getBlock()) && blockstate.getValue(PYLON_LEVEL) == PylonLevel.TOP) {
    			   BlockState blockstate1 = Blocks.AIR.defaultBlockState();
    			   p_52755_.setBlock(blockpos, blockstate1, 35);
    			   p_52755_.levelEvent(p_52758_, 2001, blockpos, Block.getId(blockstate));
    		   }
    	   }
    	   if (pylonLevel == PylonLevel.MIDDLE) {
    		   BlockPos blockpos = p_52756_.below();
    		   BlockState blockstate = p_52755_.getBlockState(blockpos);
    		   if (blockstate.is(p_52757_.getBlock()) && blockstate.getValue(PYLON_LEVEL) == PylonLevel.BOTTOM) {
    			   BlockState blockstate1 = Blocks.AIR.defaultBlockState();
    			   p_52755_.setBlock(blockpos, blockstate1, 35);
    			   p_52755_.levelEvent(p_52758_, 2001, blockpos, Block.getId(blockstate));
    		   }
    		   blockpos = p_52756_.above();
    		   blockstate = p_52755_.getBlockState(blockpos);
    		   if (blockstate.is(p_52757_.getBlock()) && blockstate.getValue(PYLON_LEVEL) == PylonLevel.TOP) {
    			   BlockState blockstate1 = Blocks.AIR.defaultBlockState();
    			   p_52755_.setBlock(blockpos, blockstate1, 35);
    			   p_52755_.levelEvent(p_52758_, 2001, blockpos, Block.getId(blockstate));
    		   }
    	   }
    	   if (pylonLevel == PylonLevel.TOP) {
    		   BlockPos blockpos = p_52756_.below(2);
    		   BlockState blockstate = p_52755_.getBlockState(blockpos);
    		   if (blockstate.is(p_52757_.getBlock()) && blockstate.getValue(PYLON_LEVEL) == PylonLevel.BOTTOM) {
    			   BlockState blockstate1 = Blocks.AIR.defaultBlockState();
    			   p_52755_.setBlock(blockpos, blockstate1, 35);
    			   p_52755_.levelEvent(p_52758_, 2001, blockpos, Block.getId(blockstate));
    		   }
    		   blockpos = p_52756_.below();
    		   blockstate = p_52755_.getBlockState(blockpos);
    		   if (blockstate.is(p_52757_.getBlock()) && blockstate.getValue(PYLON_LEVEL) == PylonLevel.MIDDLE) {
    			   BlockState blockstate1 = Blocks.AIR.defaultBlockState();
    			   p_52755_.setBlock(blockpos, blockstate1, 35);
    			   p_52755_.levelEvent(p_52758_, 2001, blockpos, Block.getId(blockstate));
    		   }
    	   }
       }

       super.playerWillDestroy(p_52755_, p_52756_, p_52757_, p_52758_);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if(state.getValue(PYLON_LEVEL) == PylonLevel.BOTTOM) {
        	return createPylonBlockEntity(pos, state);
        }
        return null;
    }
    
    public abstract AbstractPylonBE createPylonBlockEntity(BlockPos pos, BlockState state);
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return (lvl, pos, stt, te) -> {
                if (te instanceof AbstractPylonBE pylon) pylon.tickServer();
            };
        }
        else {
        	return (lvl, pos, stt, te) -> {
                if (te instanceof AbstractPylonBE pylon) pylon.tickClient();
            };
        }
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult trace) {
        if (!level.isClientSide) {
        	
        	if(player.isShiftKeyDown()) {
        		PylonLevel pylonLevel = state.getValue(PYLON_LEVEL);
        		if(pylonLevel == PylonLevel.BOTTOM) {
        			AbstractPylonBE pylon = (AbstractPylonBE) level.getBlockEntity(pos);
	        		if(pylon.getMobCategory() == null) {
	        			return InteractionResult.PASS;
	        		}
        			int diameter = 10;
	        		int radius = diameter / 2;
	        		MutableBlockPos currentPos = new MutableBlockPos();
	        		int y = pos.getY();
	        		List<Biome> biomes = new ArrayList<Biome>();
	        		for(int x = -radius; x < radius + 1; x++) {
	        			for(int z = -radius; z < radius + 1; z++) {
	        				currentPos.set(x, y, z);
	        				Holder<Biome> biomeHolder = level.getBiome(currentPos);
	        				if(biomeHolder.value() !=null) {
	        					Biome biome = biomeHolder.value();
	        					if(!biomes.contains(biome)) {
	        						biomes.add(biome);
	        					}
	        				}
	            		}
	        		}
	        		
	        		List<EntityType<?>> entities = new ArrayList<EntityType<?>>();
	        		for(Biome biome : biomes) {
	        			biome.getMobSettings().getMobs(pylon.getMobCategory()).unwrap().forEach(data -> {
	        				if(!entities.contains(data.type)) {
	        					entities.add(data.type);
	        				}
	        			});
	        		}
	        		String message = "\nSpawn List:";
	        		for(EntityType<?> type : entities) {
	        			String id = EntityType.getKey(type).toString();
	        			message += "\n" + type.getDescription().getString() + " (" + id + ")";
	        		}
	        		((ServerPlayer)player).sendSystemMessage(Component.literal(message));
	        		return InteractionResult.SUCCESS;
        		}
        	}
        }
        return InteractionResult.PASS;
    }
}
