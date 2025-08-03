package alec_wam.wam_utils.common.helpers;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueOutput.TypedOutputList;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.items.ContainerOrHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

public class BlockHelper {
	
	public static final Vec3 CENTER_OF_ORIGIN = new Vec3(.5, .5, .5);
	
	public static Vec3 getCenterOf(Vec3i pos) {
		if (pos.equals(Vec3i.ZERO))
			return CENTER_OF_ORIGIN;
		return Vec3.atLowerCornerOf(pos)
			.add(.5f, .5f, .5f);
	}
	
	public static double distToBottomCenterSqr(BlockPos pos, Entity entity) {
		double d0 = (double) pos.getX() + 0.5D - entity.position().x;
		double d1 = (double) pos.getY() - entity.position().y;
		double d2 = (double) pos.getZ() + 0.5D - entity.position().z;
		return d0 * d0 + d1 * d1 + d2 * d2;
	}
	
	public static boolean canBreak(BlockState stateToBreak, float blockHardness) {
		return isBreakable(stateToBreak, blockHardness);
	}

	public static boolean isBreakable(BlockState stateToBreak, float blockHardness) {
		return !(stateToBreak.getBlock() instanceof AirBlock || blockHardness == -1);
	}
	
	@SuppressWarnings("deprecation")
	public static float getBasicDigSpeed(LivingEntity entity, ItemStack stack, BlockState state) {
		float f = stack.getDestroySpeed(state);
		if (f > 1.0F) {
			int i = ItemHelper.getEnchantmentLevel(entity.level(), stack, Enchantments.EFFICIENCY);
			if (i > 0 && !stack.isEmpty()) {
				f += (float) (i * i + 1);
			}
		}

        if (MobEffectUtil.hasDigSpeed(entity)) {
            f *= 1.0F + (MobEffectUtil.getDigSpeedAmplification(entity) + 1) * 0.2F;
        }

        if (entity.hasEffect(MobEffects.MINING_FATIGUE)) {
            float f1 = switch (entity.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.0027F;
                default -> 8.1E-4F;
            };
            f *= f1;
        }

        if (entity.isEyeInFluid(FluidTags.WATER)) {
        	f *= (float)entity.getAttribute(Attributes.SUBMERGED_MINING_SPEED).getValue();
        }

        if (!entity.onGround()) {
            f /= 5.0F;
        }
		return f;
	}	
	
	public static int getBlockXP(ItemStack stack, Level level, BlockPos pos, BlockState state) {
		BlockEntity be = level.getBlockEntity(pos);
        return state.getExpDrop(level, pos, be, null, stack);
	}
	
	public static void destroyAndPickUpBlock(Level world, BlockPos pos, float effectChance, ItemStack toDamage, @Nullable LivingEntity entity, BiConsumer<BlockPos, ItemStack> drop) {
		ItemStack usedTool = toDamage.copy();
		BlockHelper.destroyBlockAs(world, pos, entity, toDamage, effectChance,
			stack -> drop.accept(pos, stack));
		Player playerEntity = entity instanceof Player ? ((Player) entity) : null;
		if (toDamage.isEmpty() && !usedTool.isEmpty() && playerEntity !=null)
			EventHooks.onPlayerDestroyItem(playerEntity, usedTool, InteractionHand.MAIN_HAND);
	}
	
	public static void destroyBlock(Level world, BlockPos pos, float effectChance) {
		destroyBlock(world, pos, effectChance, stack -> Block.popResource(world, pos, stack));
	}

	public static void destroyBlock(Level world, BlockPos pos, float effectChance,
		Consumer<ItemStack> droppedItemCallback) {
		destroyBlockAs(world, pos, null, ItemStack.EMPTY, effectChance, droppedItemCallback);
	}

	public static List<ItemEntity> getBlockDrops(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, @Nullable Entity entity, ItemStack tool){
		List<ItemEntity> capturedItems = new ArrayList<>();
        Block.getDrops(state, level, pos, blockEntity, entity, tool).forEach(p_49944_ -> popResource(capturedItems, level, pos, p_49944_));
        return capturedItems;
	}
	
	public static void popResource(List<ItemEntity> capturedItems, Level level, BlockPos pos, ItemStack stack) {
        double d0 = EntityType.ITEM.getHeight() / 2.0;
        double d1 = pos.getX() + 0.5 + Mth.nextDouble(level.random, -0.25, 0.25);
        double d2 = pos.getY() + 0.5 + Mth.nextDouble(level.random, -0.25, 0.25) - d0;
        double d3 = pos.getZ() + 0.5 + Mth.nextDouble(level.random, -0.25, 0.25);
        capturedItems.add(new ItemEntity(level, d1, d2, d3, stack));
    }
	
	@SuppressWarnings("deprecation")
	public static void destroyBlockAs(Level world, BlockPos pos, @Nullable LivingEntity entity, ItemStack usedTool,
		float effectChance, Consumer<ItemStack> droppedItemCallback) {
		FluidState fluidState = world.getFluidState(pos);
		BlockState state = world.getBlockState(pos);
		
		if (world.random.nextFloat() < effectChance)
			world.levelEvent(2001, pos, Block.getId(state));
		BlockEntity tileentity = state.hasBlockEntity() ? world.getBlockEntity(pos) : null;
		
		Player player = entity !=null && entity instanceof Player ? (Player) entity : null;
		
		if (entity != null) {
			if(player !=null) {
				usedTool.mineBlock(world, state, pos, player);
				player.awardStat(Stats.BLOCK_MINED.get(state.getBlock()));
			}
			else {
				usedTool.getItem().mineBlock(usedTool, world, state, pos, entity);
			}			
		}

		if (world instanceof ServerLevel serverWorld && serverWorld.getGameRules()
			.getBoolean(GameRules.RULE_DOBLOCKDROPS) && !serverWorld.restoringBlockSnapshots
			&& (player == null || !player.isCreative())) {
			
			List<ItemEntity> defaultDrops = getBlockDrops(serverWorld, pos, state, tileentity, entity, usedTool);			
			
			BlockDropsEvent event = new BlockDropsEvent(serverWorld, pos, state, tileentity, defaultDrops, entity, usedTool);
			NeoForge.EVENT_BUS.post(event);
			
			if (event.isCanceled())
				return;
			
			for (ItemEntity itemEntity : event.getDrops())
				droppedItemCallback.accept(itemEntity.getItem());

			int xp = event.getDroppedExperience();

			if (xp > 0 && world instanceof ServerLevel)
				state.getBlock()
					.popExperience((ServerLevel) world, pos, xp);
			
			// Simulating IceBlock#playerDestroy. Not calling method directly as it would drop item
			// entities as a side-effect
			if (state.getBlock() instanceof IceBlock && ItemHelper.getEnchantmentLevel(world, usedTool, Enchantments.SILK_TOUCH) == 0) {
				if (world.dimensionType()
					.ultraWarm())
					return;

//				Material material = world.getBlockState(pos.below())
//					.getMaterial();
//				if (material.blocksMotion() || material.isLiquid())
//					world.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
				BlockState iceState = world.getBlockState(pos.below());
				if(iceState.blocksMotion() || !world.getFluidState(pos.below()).isEmpty())
					world.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
				return;
			}

			state.spawnAfterBreak((ServerLevel) world, pos, ItemStack.EMPTY, true);
		}
		
		world.setBlockAndUpdate(pos, fluidState.createLegacyBlock());
	}

	public static BlockHitResult getHitResult(BlockPos pos, Direction face) {
		return new BlockHitResult(new Vec3((double)pos.getX() + 0.5D + (double)face.getStepX() * 0.5D, (double)pos.getY() + 0.5D + (double)face.getStepY() * 0.5D, (double)pos.getZ() + 0.5D + (double)face.getStepZ() * 0.5D), face, pos, false);
	}
	
	public static boolean canPlace(BlockPlaceContext p_40611_, BlockState p_40612_) {
      Player player = p_40611_.getPlayer();
      CollisionContext collisioncontext = player == null ? CollisionContext.empty() : CollisionContext.of(player);
      return (p_40612_.canSurvive(p_40611_.getLevel(), p_40611_.getClickedPos())) && p_40611_.getLevel().isUnobstructed(p_40612_, p_40611_.getClickedPos(), collisioncontext);
   }
	
	public static boolean placeBlock(Level level, BlockPos pos, ItemStack stack, @Nullable Entity entity, InteractionHand hand, Direction face, boolean sendEvent, boolean playSound) {
		Item item = stack.getItem();
        if (item instanceof BlockItem) {
           BlockItem blockitem = (BlockItem)item;
           BlockPlaceContext context = new BlockPlaceContext(level, null, hand, stack, getHitResult(pos.relative(face.getOpposite()), face));
           BlockPlaceContext blockplacecontext = blockitem.updatePlacementContext(context);
           BlockState blockstate = blockitem.getBlock().getStateForPlacement(blockplacecontext);
           BlockPos realPos = blockplacecontext.getClickedPos();
           if(blockplacecontext.canPlace() && canPlace(blockplacecontext, blockstate) && level.setBlockAndUpdate(realPos, blockstate)) {
        	   if(sendEvent)level.gameEvent(GameEvent.BLOCK_PLACE, realPos, GameEvent.Context.of(entity, blockstate));
        	   
        	   if(playSound) {
        		   SoundType sound = blockstate.getSoundType(level, realPos, entity);
        		   level.playSound((Player)null, (double)realPos.getX(), (double)realPos.getY(), (double)realPos.getZ(), sound.getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        	   }        	   
        	   stack.shrink(1);        	   
        	   return true;
           }
        }        
        return false;
	}

	public static BlockState getBlockStateForPlacement(Level level, BlockPos pos, ItemStack stack, @Nullable Entity entity, InteractionHand hand, Direction face) {
		Item item = stack.getItem();
		if (item instanceof BlockItem) {
			BlockItem blockitem = (BlockItem) item;
			BlockPlaceContext context = new BlockPlaceContext(level, null, hand, stack, getHitResult(pos.relative(face.getOpposite()), face));
			BlockPlaceContext blockplacecontext = blockitem.updatePlacementContext(context);
			if (blockplacecontext != null) {
				return blockitem.getBlock().getStateForPlacement(blockplacecontext);
			}
		}
		return null;
	}
	
	//CROPS
	public static boolean isValidCrop(Level world, BlockPos pos, BlockState state, boolean replant) {
		
		if(state.getBlock() instanceof StemBlock) {
			return false;
		}
		
		if (state.getBlock() instanceof CropBlock) {
			CropBlock crop = (CropBlock) state.getBlock();
			return crop.isMaxAge(state);
		}

		if (state.getCollisionShape(world, pos)
			.isEmpty() || state.getBlock() instanceof CocoaBlock) {
			for (Property<?> property : state.getProperties()) {
				if (!(property instanceof IntegerProperty))
					continue;
				IntegerProperty ageProperty = (IntegerProperty) property;
				if (!property.getName()
					.equals(BlockStateProperties.AGE_1.getName()))
					continue;
				int age = state.getValue(ageProperty)
					.intValue();
				if (state.getBlock() instanceof SweetBerryBushBlock && age <= 1 && replant)
					continue;
				if (age == 0 && replant || (ageProperty.getPossibleValues().size() - 1 != age))
					continue;
				return true;
			}
		}

		return false;
	}
	
	public static boolean isValidOtherCrop(Level world, BlockPos pos, BlockState state) {
		if (state.getBlock() instanceof CropBlock)
			return false;
		if(state.getBlock() instanceof StemBlock) 
			return false;
		if (TreeCutter.isVerticalPlant(state)) {
			BlockState stateAbove = world.getBlockState(pos.above());
			return TreeCutter.isVerticalPlant(stateAbove);
		}
		if (state.getBlock() instanceof CocoaBlock)
			return state.getValue(CocoaBlock.AGE) == CocoaBlock.MAX_AGE;
		if (state.is(Blocks.LARGE_AMETHYST_BUD))
			return true;
		if(state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN))
			return true;

		if (state.getCollisionShape(world, pos)
			.isEmpty()) {
			if (state.getBlock() instanceof GrowingPlantBlock)
				return true;

			for (Property<?> property : state.getProperties()) {
				if (!(property instanceof IntegerProperty))
					continue;
				if (!property.getName()
					.equals(BlockStateProperties.AGE_1.getName()))
					continue;
				return false;
			}

//			TODO Figure out IPlantable in 1.21.5
//			if (state.getBlock() instanceof IPlantable)
//				return true;
		}

		return false;
	}
	
	public static BlockState harvestCrop(Level world, BlockPos pos, BlockState state, boolean replant) {
		if (!replant) {
			if (state.getFluidState()
				.isEmpty())
				return Blocks.AIR.defaultBlockState();
			return state.getFluidState()
				.createLegacyBlock();
		}

		Block block = state.getBlock();
		if (block instanceof CropBlock) {
			CropBlock crop = (CropBlock) block;
			return crop.getStateForAge(0);
		}
		if (block == Blocks.SWEET_BERRY_BUSH) {
			return state.setValue(BlockStateProperties.AGE_3, Integer.valueOf(1));
		}
		if (block == Blocks.SUGAR_CANE || block instanceof GrowingPlantBlock) {
			if (state.getFluidState()
				.isEmpty())
				return Blocks.AIR.defaultBlockState();
			return state.getFluidState()
				.createLegacyBlock();
		}
		if (state.getCollisionShape(world, pos)
			.isEmpty() || block instanceof CocoaBlock) {
			for (Property<?> property : state.getProperties()) {
				if (!(property instanceof IntegerProperty))
					continue;
				if (!property.getName()
					.equals(BlockStateProperties.AGE_1.getName()))
					continue;
				return state.setValue((IntegerProperty) property, Integer.valueOf(0));
			}
		}

		if (state.getFluidState()
			.isEmpty())
			return Blocks.AIR.defaultBlockState();
		return state.getFluidState()
			.createLegacyBlock();
	}

	public static boolean isStackingCrop(BlockState state) {
		return state.getBlock() instanceof SugarCaneBlock || state.getBlock() instanceof CactusBlock;
	}
	
	public static void saveBlockPosList(ValueOutput valueOutput, String key, List<BlockPos> list) {
		TypedOutputList<BlockPos> outputList = valueOutput.list(key, BlockPos.CODEC);
		list.forEach(outputList::add);
		// return BlockPos.CODEC.listOf().encodeStart(NbtOps.INSTANCE, list).getOrThrow();
	}

	public static List<BlockPos> loadBlockPosList(ValueInput valueInput, String key){
		List<BlockPos> list = new LinkedList<BlockPos>();
		valueInput.listOrEmpty(key, BlockPos.CODEC).forEach(pos -> {
			list.add(pos);
		});
		return list;
	}
	
	public static Pair<BlockPos, BlockPos> getMinAndMaxPos(List<BlockPos> posList){
		if (posList.isEmpty()) return null;

	    int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
	    int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

	    for (BlockPos pos : posList) {
	        minX = Math.min(minX, pos.getX());
	        minY = Math.min(minY, pos.getY());
	        minZ = Math.min(minZ, pos.getZ());

	        maxX = Math.max(maxX, pos.getX());
	        maxY = Math.max(maxY, pos.getY());
	        maxZ = Math.max(maxZ, pos.getZ());
	    }
	    
	    return new Pair<>(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
	}
	
	public static Optional<IItemHandler> getItemHandler(Level worldIn, BlockPos pos, @Nullable Direction side)
    {
        ContainerOrHandler inventory = HopperBlockEntity.getContainerOrHandlerAt(worldIn, pos, side);
		IItemHandler invHandler = null;
		if(inventory !=null) {
			if(inventory.itemHandler() != null) {
				invHandler = inventory.itemHandler();
			}
			else if(inventory.container() != null) {
				if(inventory.container() instanceof WorldlyContainer worldlyContainer){
					invHandler = new SidedInvWrapper(worldlyContainer, side);
				}
				else {
					invHandler = new InvWrapper(inventory.container());
				}
			}
		}
        return Optional.ofNullable(invHandler);
    }
	
	//Copied from VanillaInventoryHooks
	public static boolean isFull(IItemHandler itemHandler)
    {
        for (int slot = 0; slot < itemHandler.getSlots(); slot++)
        {
            ItemStack stackInSlot = itemHandler.getStackInSlot(slot);
            if (stackInSlot.isEmpty() || stackInSlot.getCount() < itemHandler.getSlotLimit(slot))
            {
                return false;
            }
        }
        return true;
    }

    public static boolean isEmpty(IItemHandler itemHandler)
    {
        for (int slot = 0; slot < itemHandler.getSlots(); slot++)
        {
            ItemStack stackInSlot = itemHandler.getStackInSlot(slot);
            if (stackInSlot.getCount() > 0)
            {
                return false;
            }
        }
        return true;
    }
	
	@NotNull
    public static ItemStack insertItemStacked(IItemHandler inventory, @NotNull ItemStack stack, boolean simulate)
    {
        if (inventory == null || stack.isEmpty())
            return stack;

        // not stackable -> just insert into a new slot
        if (!stack.isStackable())
        {
            return ItemHandlerHelper.insertItem(inventory, stack, simulate);
        }

        int sizeInventory = inventory.getSlots();

        // go through the inventory and try to fill up already existing items
        for (int i = 0; i < sizeInventory; i++)
        {
            ItemStack slot = inventory.getStackInSlot(i);
            if (ItemStack.isSameItemSameComponents(slot, stack))
            {
                stack = inventory.insertItem(i, stack, simulate);

                if (stack.isEmpty())
                {
                    break;
                }
            }
        }

        // insert remainder into empty slots
        if (!stack.isEmpty())
        {
            // find empty slot
            for (int i = 0; i < sizeInventory; i++)
            {
                if (inventory.getStackInSlot(i).isEmpty())
                {
                    stack = inventory.insertItem(i, stack, simulate);
                    if (stack.isEmpty())
                    {
                        break;
                    }
                }
            }
        }

        return stack;
    }

	public static int insertSingleItemStacked(IItemHandler inventory, ItemStack stack, boolean simulate) {
		if (inventory == null || stack.isEmpty())
            return -1;

		int sizeInventory = inventory.getSlots();

		if(stack.isStackable()) {
			// go through the inventory and try to fill up already existing items
			for (int i = 0; i < sizeInventory; i++)
			{
				ItemStack slot = inventory.getStackInSlot(i);
				if (ItemStack.isSameItemSameComponents(slot, stack))
				{
					ItemStack result = inventory.insertItem(i, stack, simulate);
					if(result.isEmpty()){
						return i;
					}
				}
			}
		}

        // insert remainder into empty slots
        // find empty slot
		for (int i = 0; i < sizeInventory; i++)
		{
			if (inventory.getStackInSlot(i).isEmpty())
			{
				ItemStack result = inventory.insertItem(i, stack, simulate);
				if(result.isEmpty()){
					return i;
				}
			}
		}

		return -1;
	}

	public static boolean shouldSolidify(BlockGetter level, BlockPos pos, BlockState state, net.minecraft.world.level.material.FluidState fluidState) {
        return state.canBeHydrated(level, pos, fluidState, pos) || touchesLiquid(level, pos, state);
    }

    public static boolean shouldSolidify(BlockGetter level, BlockPos pos, BlockState state) {
        return shouldSolidify(level, pos, state, level.getFluidState(pos));
    }

    public static boolean touchesLiquid(BlockGetter level, BlockPos pos, BlockState state) {
        boolean flag = false;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = pos.mutable();

        for (Direction direction : Direction.values()) {
            BlockState blockstate = level.getBlockState(blockpos$mutableblockpos);
            if (direction != Direction.DOWN || state.canBeHydrated(level, pos, blockstate.getFluidState(), blockpos$mutableblockpos)) {
                blockpos$mutableblockpos.setWithOffset(pos, direction);
                blockstate = level.getBlockState(blockpos$mutableblockpos);
                if (state.canBeHydrated(level, pos, blockstate.getFluidState(), blockpos$mutableblockpos) && !blockstate.isFaceSturdy(level, pos, direction.getOpposite())) {
                    flag = true;
                    break;
                }
            }
        }

        return flag;
    }

	public static boolean isWater(Level level, BlockPos pos) {
		return level.getFluidState(pos).is(FluidTags.WATER) || level.getBlockState(pos).getFluidState().is(FluidTags.WATER);
	}
	
	public static final Predicate<BlockState> IS_FULL_BEEHIVE = (state) -> {
		return state.hasProperty(BeehiveBlock.HONEY_LEVEL) && state.getValue(BeehiveBlock.HONEY_LEVEL) >= BeehiveBlock.MAX_HONEY_LEVELS;
	};
	
}
