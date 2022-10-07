package alec_wam.wam_utils.blocks.advanced_beehive;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;

import alec_wam.wam_utils.init.BlockInit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkHooks;

public class AdvancedBeehiveBlock extends BeehiveBlock {
	public static final String SCREEN_BLOCK = "screen.wam_utils.advanced_beehive";
	    
	public AdvancedBeehiveBlock() {
		super(Properties.of(Material.WOOD)
				.strength(1.0F)
				.sound(SoundType.WOOD));
	}
	
	@Override
	public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack stack) {
		player.awardStat(Stats.BLOCK_MINED.get(this));
		player.causeFoodExhaustion(0.005F);
		dropResources(state, level, pos, blockEntity, player, stack);
	}
	
	@Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player)
    {
    	BlockEntity blockentity = level.getBlockEntity(pos);
        if (blockentity instanceof AdvancedBeehiveBE be) {
        	return saveToItem(state, (AdvancedBeehiveBE)blockentity);
        }
        return super.getCloneItemStack(state, target, level, pos, player);
    }
    
    @Override
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
    	Entity entity = builder.getOptionalParameter(LootContextParams.THIS_ENTITY);
    	BlockEntity blockentity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
    	if (entity instanceof PrimedTnt || entity instanceof Creeper || entity instanceof WitherSkull || entity instanceof WitherBoss || entity instanceof MinecartTNT) {
    		if (blockentity instanceof BeehiveBlockEntity) {
    			BeehiveBlockEntity beehiveblockentity = (BeehiveBlockEntity)blockentity;
    			beehiveblockentity.emptyAllLivingFromHive((Player)null, state, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
    		}
        	return super.getDrops(state, builder);
    	}
    	
    	if(blockentity !=null) {
    		if (blockentity instanceof AdvancedBeehiveBE be) {
            	return Lists.newArrayList(saveToItem(state, be));
            }
    	}
    	return super.getDrops(state, builder);
    }
    
    public ItemStack saveToItem(BlockState state, AdvancedBeehiveBE hive) {
    	ItemStack itemstack = new ItemStack(this);
        int i = state.getValue(HONEY_LEVEL);
        boolean flag = !hive.isEmpty();
        if (flag || i > 0) {
           if (flag) {
        	  CompoundTag compoundtag = new CompoundTag();
              compoundtag.put("Bees", hive.writeBees());
              BlockItem.setBlockEntityData(itemstack, BlockInit.ADVANCED_BEEHIVE_BE.get(), compoundtag);
           }

           CompoundTag compoundtag1 = new CompoundTag();
           compoundtag1.putInt("honey_level", i);
           itemstack.addTagElement("BlockStateTag", compoundtag1);
        }
        return itemstack;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flags) {
    	super.appendHoverText(stack, level, tooltip, flags);
    	
    	if(stack.hasTag()) {
    		CompoundTag tag = stack.getTag();
    		if(tag.contains("BlockEntityTag")) {
    			CompoundTag blockData = tag.getCompound("BlockEntityTag");
    			if(blockData.contains("Bees")) {
    				ListTag listtag = blockData.getList("Bees", 10);
    				tooltip.add(Component.translatable("gui.wam_utils.advanced_beehive.bees", ""+listtag.size()));
    			}
    		}
    		if(tag.contains("BlockStateTag")) {
    			CompoundTag blockData = tag.getCompound("BlockStateTag");
    			if(blockData.contains("honey_level")) {
    				int honeyLevel = blockData.getInt("honey_level");
    				float honeyRatio = (float)(honeyLevel) / (float)BeehiveBlock.MAX_HONEY_LEVELS;
    	    		int honeyPercentage = (int)(honeyRatio * 100.0F);
    				tooltip.add(Component.translatable("gui.wam_utils.advanced_beehive.honey", ""+honeyPercentage));
    			}
    		}
    	}
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AdvancedBeehiveBE(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        else {
        	return (lvl, pos, stt, te) -> {
                if (te instanceof AdvancedBeehiveBE be) be.customServerTick(level, pos, state);
            };
        }
    }
    
    @SuppressWarnings("deprecation")
	@Override
    public void onRemove(BlockState p_49076_, Level p_49077_, BlockPos p_49078_, BlockState p_49079_, boolean p_49080_) {
        if (!p_49076_.is(p_49079_.getBlock())) {
           BlockEntity blockentity = p_49077_.getBlockEntity(p_49078_);
           if (blockentity instanceof AdvancedBeehiveBE be) {
        	  LazyOptional<IItemHandler> handler = be.getCapability(ForgeCapabilities.ITEM_HANDLER).cast();
        	  
        	  handler.ifPresent(h -> {
                 for(int i = 0; i < h.getSlots(); i++) {
                	 ItemStack stack = h.getStackInSlot(i);
                	 Containers.dropItemStack(p_49077_, (double)p_49078_.getX(), (double)p_49078_.getY(), (double)p_49078_.getZ(), stack);
                 }
              });
              p_49077_.updateNeighbourForOutputSignal(p_49078_, this);
           }

           super.onRemove(p_49076_, p_49077_, p_49078_, p_49079_, p_49080_);
        }
     }

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		ItemStack itemstack = player.getItemInHand(hand);
		
		boolean isHarvestItem = itemstack.is(Items.GLASS_BOTTLE) || itemstack.canPerformAction(net.minecraftforge.common.ToolActions.SHEARS_HARVEST);
		
		if(!isHarvestItem) {
			if(!player.isShiftKeyDown()) {
				if(!level.isClientSide) {
					BlockEntity be = level.getBlockEntity(pos);
		            if (be instanceof AdvancedBeehiveBE) {
		            	MenuProvider containerProvider = new MenuProvider() {
		                    @Override
		                    public Component getDisplayName() {
		                        return Component.translatable(SCREEN_BLOCK);
		                    }
	
		                    @Override
		                    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player playerEntity) {
		                        return new AdvancedBeehiveContainer(windowId, pos, playerInventory, playerEntity);
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
		
		return super.use(state, level, pos, player, hand, hitResult);
	}

}
