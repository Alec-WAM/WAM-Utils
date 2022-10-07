package alec_wam.wam_utils.utils;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.entities.WAMUtilsFakePlayer;
import alec_wam.wam_utils.server.network.MessageDestroyBlockEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BambooBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.ChorusPlantBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FungusBlock;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.StemGrownBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.PacketDistributor;

public class BlockUtils {

	public static void playSound(Level worldObj, SoundEvent soundName, double x, double y, double z, double volume, double pitch) {
        ClientboundSoundPacket soundEffect = new ClientboundSoundPacket(soundName, SoundSource.BLOCKS, x, y, z, (float) volume, (float) pitch, 0);

        for (int j = 0; j < worldObj.players().size(); ++j) {
            ServerPlayer player = (ServerPlayer)worldObj.players().get(j);
            BlockPos chunkcoordinates = player.blockPosition();
            double xx = x - chunkcoordinates.getX();
            double yy = y - chunkcoordinates.getY();
            double zz = z - chunkcoordinates.getZ();
            double sqDist = xx * xx + yy * yy + zz * zz;

            if (sqDist <= 256.0D) {
                player.connection.send(soundEffect);
            }
        }
	}
	
	public static BlockPos loadBlockPos(CompoundTag tag, String name) {
		ListTag listtag = loadIntegerList(tag, name);
        return new BlockPos(listtag.getInt(0), listtag.getInt(1), listtag.getInt(2));
	}
	
	public static Vec3i loadVec3i(CompoundTag tag, String name) {
		ListTag listtag = loadIntegerList(tag, name);
        return new Vec3i(listtag.getInt(0), listtag.getInt(1), listtag.getInt(2));
	}
	
	public static ListTag loadIntegerList(CompoundTag tag, String name) {
		ListTag listtag = tag.getList(name, 3);
        return listtag;
	}
	
	public static ListTag saveBlockPos(BlockPos pos) {
		return newIntegerList(pos.getX(), pos.getY(), pos.getZ());
	}
	
	public static ListTag saveVec3i(Vec3i pos) {
		return newIntegerList(pos.getX(), pos.getY(), pos.getZ());
	}
	
	public static CompoundTag writeGlobalPos(GlobalPos pos) {
		CompoundTag tag = new CompoundTag();
		ResourceLocation dimLocation = pos.dimension().location();
		tag.putInt("X", pos.pos().getX());
		tag.putInt("Y", pos.pos().getY());
		tag.putInt("Z", pos.pos().getZ());
		tag.putString("Dim", dimLocation.toString());
		return tag;
	}
	
	public static GlobalPos loadGlobalPos(CompoundTag tag) {
		ResourceLocation dimType = new ResourceLocation(tag.getString("Dim"));
		ResourceKey<Level> dim = ResourceKey.create(Registry.DIMENSION_REGISTRY, dimType);
		BlockPos blockPos = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
		return GlobalPos.of(dim, blockPos);
	}

	public static ListTag newIntegerList(int... p_74626_) {
		ListTag listtag = new ListTag();

		for(int i : p_74626_) {
			listtag.add(IntTag.valueOf(i));
		}

		return listtag;
	}
	
	//CROPS	
	public static boolean isStackingCrop(BlockState state) {
		Block block = state.getBlock();
		return block instanceof SugarCaneBlock || block instanceof CactusBlock;
	}

	public static boolean isSeedItem(ItemStack stack) {
		if(stack.isEmpty()) return false;
		if(stack.is(Tags.Items.SEEDS))return true;
		if(stack.is(Tags.Items.CROPS))return true;
		if(stack.getItem() instanceof BlockItem) {
			BlockItem blockItem = ((BlockItem)stack.getItem());
			Block block = blockItem.getBlock();
			if(block.defaultBlockState().is(BlockTags.CROPS))return true;
			if(block instanceof CropBlock
				|| block instanceof StemBlock
				|| block instanceof NetherWartBlock
				|| block instanceof SugarCaneBlock || block instanceof CactusBlock
				|| block instanceof CocoaBlock
				|| block instanceof SweetBerryBushBlock) {
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean validPlantingLocation(Level level, BlockPos blockPos, ItemStack stack) {
		if(stack.getItem() instanceof BlockItem) {
			BlockItem blockItem = (BlockItem)stack.getItem();
			ItemStack copy = stack.copy();
			Block block = blockItem.getBlock();
			
			if(block instanceof SugarCaneBlock || block instanceof CactusBlock) {
				BlockState belowState = level.getBlockState(blockPos.below());
				if(belowState.getBlock().equals(block)) {
					return false;
				}
			}			
			
			WAMUtilsFakePlayer player = WAMUtilsFakePlayer.get((ServerLevel)level, blockPos).get();
			
			BlockPos offsetDir = blockPos.below();
			BlockHitResult hit = new BlockHitResult(Vec3.ZERO, Direction.UP, offsetDir, false);
			BlockPlaceContext context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, copy, hit);
			BlockState blockstate = block.getStateForPlacement(context);
			if(blockstate !=null && canPlace(blockItem, context, blockstate)) {
				return true;
			}
			//TODO Handle Things Like Cocoa
			/*for(Direction dir : Direction.values()) {
				BlockPos offsetDir = pos.relative(dir.getOpposite());
				BlockHitResult hit = new BlockHitResult(Vec3.ZERO, dir, offsetDir, false);
				BlockPlaceContext context = new BlockPlaceContext(level, null, InteractionHand.MAIN_HAND, copy, hit);
				BlockState blockstate = block.getStateForPlacement(context);
				if(blockstate !=null && canPlace(blockItem, context, blockstate)) {
					return true;
				}
			}*/
		}
		return false;
	}
	
	public static boolean canPlace(BlockItem blockItem, BlockPlaceContext p_40611_, BlockState p_40612_) {
	      Player player = p_40611_.getPlayer();
	      CollisionContext collisioncontext = player == null ? CollisionContext.empty() : CollisionContext.of(player);
	      return (p_40612_.canSurvive(p_40611_.getLevel(), p_40611_.getClickedPos())) && p_40611_.getLevel().isUnobstructed(p_40612_, p_40611_.getClickedPos(), collisioncontext);
	}
	
	public static boolean placeSeed(Level level, BlockPos pos, ItemStack seed) {
		if(seed.getItem() instanceof BlockItem) {
			BlockItem blockItem = (BlockItem)seed.getItem();
			ItemStack copy = seed.copy();
			Block block = blockItem.getBlock();
			WAMUtilsFakePlayer player = WAMUtilsFakePlayer.get((ServerLevel)level, pos).get();
			player.setItemInHand(InteractionHand.MAIN_HAND, copy);
			
			BlockPos offsetDir = pos.below();
			BlockHitResult hit = new BlockHitResult(Vec3.ZERO, Direction.UP, offsetDir, false);
			BlockPlaceContext context = new BlockPlaceContext(level, null, InteractionHand.MAIN_HAND, copy, hit);
			BlockState blockstate = block.getStateForPlacement(context);
			if(blockstate !=null) {
				InteractionResult result = blockItem.place(context);
				//WAMUtilsMod.LOGGER.debug("Place: " + result);
				if(result.consumesAction()) {
					player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
					return true;
				}
			}
			
			//TODO Handle Cocoa
//			for(Direction dir : Direction.values()) {
//				BlockPos offsetDir = pos.relative(dir.getOpposite());
//				BlockHitResult hit = new BlockHitResult(Vec3.ZERO, dir, offsetDir, false);
//				BlockPlaceContext context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, copy, hit);
//				BlockState blockstate = block.getStateForPlacement(context);
//				if(blockstate !=null) {
//					InteractionResult result = blockItem.place(context);
//					//WAMUtilsMod.LOGGER.debug("Place: " + result);
//					if(result.consumesAction()) {
//						int count = InventoryUtils.consumeItem(seedItems, seed, 1, false);
//						//WAMUtilsMod.LOGGER.debug("Consume: " + count + " " + seed);
//						player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
//						return true;
//					}
//				}
//			}
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		}
		return false;
	}
	
	public static boolean randomTickBlock(ServerLevel level, BlockPos pos) {
		final BlockState state = level.getBlockState(pos);
		if(state.isRandomlyTicking() && !state.hasBlockEntity()) {
			state.randomTick(level, pos, level.random);
			level.sendBlockUpdated(pos, state, state, 3);
			return true;
		}
		return false;
	}
	
	public static boolean canHarvestCrop(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		Block block = state.getBlock();
		if(block instanceof CropBlock crop) {
			return crop.isMaxAge(state);
		}
		if(block instanceof NetherWartBlock wart) {
			return state.getValue(NetherWartBlock.AGE) == 3;
		}
		if(BlockUtils.isStackingCrop(state)) {
			BlockState stateBelow = level.getBlockState(pos.below());
			//Make sure not to harvest the base
			return stateBelow.getBlock().equals(block);
		}
		if(block instanceof CocoaBlock) {
			return state.getValue(CocoaBlock.AGE) == CocoaBlock.MAX_AGE;
		}
		if(block instanceof StemGrownBlock) {
			return true;
		}
		if(block instanceof SweetBerryBushBlock) {
			return state.getValue(SweetBerryBushBlock.AGE) == SweetBerryBushBlock.MAX_AGE;
		}
		return false;
	}	
	
	public static boolean allowedToBreak(Level level, BlockState state, BlockPos pos, Player player) {
        if (!state.getBlock().canEntityDestroy(state, level, pos, player)) {
            return false;
        }
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, player);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return false;
        }
        return true;
    }
	
	public static List<ItemStack> breakBlock(Level level, BlockPos pos){
		return breakBlock(level, pos, true, true); 
	}
	
	public static List<ItemStack> breakBlock(Level level, BlockPos pos, boolean playSound, boolean displayParticles){
		return breakBlock(level, pos, null, ItemStack.EMPTY, 0, playSound, displayParticles);
	}
	
	public static List<ItemStack> breakBlock(Level level, BlockPos pos, WAMUtilsFakePlayer player, ItemStack tool){
		return breakBlock(level, pos, player, tool, 0, true, true);
	}
	
	public static List<ItemStack> breakBlock(Level level, BlockPos pos, ItemStack tool, int fortune){
		return breakBlock(level, pos, null, tool, fortune, true, true);
	}
	
	public static List<ItemStack> breakBlock(Level level, BlockPos pos, WAMUtilsFakePlayer player, ItemStack tool, int fortune, boolean playSound, boolean displayParticles){
		BlockState state = level.getBlockState(pos);
		LootContext.Builder builder = new LootContext.Builder((ServerLevel) level)
                .withRandom(level.random)
                .withParameter(LootContextParams.ORIGIN, new Vec3(pos.getX(), pos.getY(), pos.getZ()))
                .withParameter(LootContextParams.TOOL, tool)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, level.getBlockEntity(pos))
                .withOptionalParameter(LootContextParams.THIS_ENTITY, player);
        if (fortune > 0) {
            builder.withLuck(fortune);
        }
        List<ItemStack> drops = state.getDrops(builder);
        
        if(playSound && !displayParticles) {
	        try {
	        	SoundType soundType = state.getBlock().getSoundType(state, level, pos, null);
	        	BlockUtils.playSound(level, soundType.getBreakSound(), (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
	        } 
	        catch(Exception e) {
	        	
	        }        
        }
        else if(displayParticles && !playSound) {
        	WAMUtilsMod.packetHandler.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)), new MessageDestroyBlockEffect(pos, state));
        }
        else if(playSound && displayParticles){
        	level.levelEvent(null, 2001, pos, Block.getId(state));
        }
        
        FluidState fluidstate = level.getFluidState(pos);
        level.setBlock(pos, fluidstate.createLegacyBlock(), Block.UPDATE_CLIENTS);
        
        return drops;
	}
	
	public static int harvestCrop(Level level, BlockPos pos, ItemStack tool, int fortune, ItemStackHandler primaryInv, ItemStackHandler secondayInv, Consumer<Integer> onChanged) {
		int harvestCount = 0;
		WAMUtilsFakePlayer player = WAMUtilsFakePlayer.get((ServerLevel)level, pos).get();
		BlockState state = level.getBlockState(pos);
		Block block = state.getBlock();
		if(block instanceof SweetBerryBushBlock) {
			int i = state.getValue(SweetBerryBushBlock.AGE);
			boolean flag = i == 3;
			int j = 1 + level.random.nextInt(2);
			ItemStack drop = new ItemStack(Items.SWEET_BERRIES, j + (flag ? 1 : 0));
			
			ItemStack remainder = drop;
        	remainder = InventoryUtils.forceStackInInventoryAllSlots(primaryInv, remainder, onChanged);
        	
        	if(!remainder.isEmpty() && secondayInv !=null) {
        		remainder = InventoryUtils.forceStackInInventoryAllSlots(secondayInv, remainder, onChanged);
        	}
			
			BlockUtils.playSound(level, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), 1.0F, 0.8F + level.random.nextFloat() * 0.4F);
			BlockState blockstate = state.setValue(SweetBerryBushBlock.AGE, Integer.valueOf(1));
			level.setBlock(pos, blockstate, 2);
			level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, blockstate));
			return 1;
		}
		
		if(isStackingCrop(state)) {
			BlockPos above = pos.above();
			BlockState aboveState = level.getBlockState(above);
			if(aboveState.getBlock() == block && allowedToBreak(level, state, pos, player)) {
				harvestCount += harvestCrop(level, above, tool, fortune, primaryInv, secondayInv, onChanged);
			}
		}
		
		if(allowedToBreak(level, state, pos, player)) {
			List<ItemStack> drops = breakBlock(level, pos, player, tool, fortune, true, true);
	        for(ItemStack drop : drops) {
	        	ItemStack insertStack = drop.copy();
	        	ItemStack remainder = insertStack;
	        	remainder = InventoryUtils.forceStackInInventoryAllSlots(primaryInv, remainder, onChanged);
	        	
	        	if(!remainder.isEmpty() && secondayInv !=null) {
	        		//TODO Handle Full
	        		remainder = InventoryUtils.forceStackInInventoryAllSlots(secondayInv, remainder, onChanged);
	        	}
	        }
            harvestCount++;
		}
		
		return harvestCount;
	}
	
	//TREES
	public static boolean isSaplingItem(ItemStack stack) {
		if(stack.isEmpty()) return false;
		if(stack.is(ItemTags.SAPLINGS))return true;
		
		if(stack.getItem() instanceof BlockItem) {
			BlockItem blockItem = ((BlockItem)stack.getItem());
			Block block = blockItem.getBlock();
			if(block instanceof BambooBlock
				|| block instanceof MushroomBlock || block instanceof FungusBlock
				|| block == Blocks.CHORUS_FLOWER) {
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean isMegaSapling(Item item) {
		if(item != null && item instanceof BlockItem block) {
			return isMegaSapling(block.getBlock());
		}
		return false;
	}
	
	public static boolean isMegaSapling(Block block) {
		return block == Blocks.JUNGLE_SAPLING || block == Blocks.SPRUCE_SAPLING;
	}
	
	public static boolean isLog(BlockState state) {
		return state.is(BlockTags.LOGS);
	}
	
	public static boolean isLeaf(BlockState state) {
		return state.is(BlockTags.LEAVES);
	}
	
	public static boolean canHarvestTree(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		Block block = state.getBlock();
		if(isLog(state)) {
			return true;
		}
		if(block instanceof HugeMushroomBlock) {
			return true;
		}
		if(block instanceof ChorusPlantBlock) {
			return true;
		}
		if(block instanceof BambooBlock) {
			return true;
		}
		return false;
	}	
	
	public static int harvestTree(Level level, BlockPos pos, ItemStackHandler primaryInv, ItemStackHandler secondayInv, Consumer<Integer> onChanged, Predicate<BlockState> shouldHarvestBlock, List<BlockPos> scannedBlocks) {
		int harvestCount = 0;
		WAMUtilsFakePlayer player = WAMUtilsFakePlayer.get((ServerLevel)level, pos).get();
		BlockState state = level.getBlockState(pos);
		
		if(scannedBlocks.contains(pos))return harvestCount;
		
		scannedBlocks.add(pos);
		
		if(!shouldHarvestBlock.test(state)) {
			return harvestCount;
		}
		
		//Diagonal to handle Acacia Trees
		for(int x = -1; x <= 1; x++) {
			for(int y = -1; y <= 1; y++) {
				for(int z = -1; z <= 1; z++) {
					BlockPos otherPos = pos.offset(x, y, z);
					if(scannedBlocks.contains(otherPos))continue;
					BlockState otherState = level.getBlockState(otherPos);
					if((shouldHarvestBlock.test(otherState))) {
						harvestCount += harvestTree(level, otherPos, primaryInv, secondayInv, onChanged, shouldHarvestBlock, scannedBlocks);
					}
				}
			}
		}
		
		if(allowedToBreak(level, state, pos, player)) {
			List<ItemStack> drops = breakBlock(level, pos);            
            for(ItemStack drop : drops) {
            	ItemStack insertStack = drop.copy();
            	ItemStack remainder = insertStack;
            	remainder = InventoryUtils.forceStackInInventoryAllSlots(primaryInv, remainder, onChanged);
            	
            	if(!remainder.isEmpty() && secondayInv !=null) {
            		//TODO Handle Full
            		remainder = InventoryUtils.forceStackInInventoryAllSlots(secondayInv, remainder, onChanged);
            	}
            }
            harvestCount++;
		}
		
		return harvestCount;
	}
	
	public static boolean allChorusFlowersGrown(Level level, BlockPos pos, List<BlockPos> scannedBlocks) {
		BlockState state = level.getBlockState(pos);
		
		if(scannedBlocks.contains(pos))return true;
		
		scannedBlocks.add(pos);
		
		if(state.getBlock() == Blocks.CHORUS_PLANT) {
			for(Direction dir : Direction.values()) {
				BlockPos otherPos = pos.relative(dir);
				if(scannedBlocks.contains(otherPos))continue;
				if(!allChorusFlowersGrown(level, otherPos, scannedBlocks)) {
					return false;
				}
			}
		}
		
		if(state.getBlock() == Blocks.CHORUS_FLOWER) {
			int age = state.getValue(ChorusFlowerBlock.AGE);
			return age == ChorusFlowerBlock.DEAD_AGE;
		}
		
		return true;		
	}
	
	public static void findAllChorusFlowers(Level level, BlockPos pos, List<BlockPos> scannedBlocks, List<BlockPos> chorusFlowers, boolean ignoreGrown) {
		BlockState state = level.getBlockState(pos);
		
		if(scannedBlocks.contains(pos))return;
		
		scannedBlocks.add(pos);
		
		if(state.getBlock() == Blocks.CHORUS_PLANT) {
			for(Direction dir : Direction.values()) {
				BlockPos otherPos = pos.relative(dir);
				if(scannedBlocks.contains(otherPos))continue;
				BlockState otherState = level.getBlockState(otherPos);
				if(otherState.getBlock() == Blocks.CHORUS_PLANT || otherState.getBlock() == Blocks.CHORUS_FLOWER) {
					findAllChorusFlowers(level, otherPos, scannedBlocks, chorusFlowers, ignoreGrown);
				}
			}
		}
		
		if(state.getBlock() == Blocks.CHORUS_FLOWER) {
			int age = state.getValue(ChorusFlowerBlock.AGE);
			if(!ignoreGrown || age != ChorusFlowerBlock.DEAD_AGE) {
				chorusFlowers.add(pos);
			}
		}		
	}
	
	public static void buildHarvestMap(Level level, BlockPos pos, Predicate<BlockState> shouldHarvestBlock, List<BlockPos> scannedBlocks, List<HarvestBlockData> harvestData){
		BlockState state = level.getBlockState(pos);
		
		if(scannedBlocks.contains(pos)) {
			return;
		}
		
		scannedBlocks.add(pos);
		
		if(shouldHarvestBlock.test(state)) {
			harvestData.add(new HarvestBlockData(pos, state));
		}
		
		//Diagonal to handle Acacia Trees
		for(int x = -1; x <= 1; x++) {
			for(int y = -1; y <= 1; y++) {
				for(int z = -1; z <= 1; z++) {
					BlockPos otherPos = pos.offset(x, y, z);
					if(scannedBlocks.contains(otherPos))continue;
					BlockState otherState = level.getBlockState(otherPos);
					if((shouldHarvestBlock.test(otherState))) {
						buildHarvestMap(level, otherPos, shouldHarvestBlock, scannedBlocks, harvestData);
					}
				}
			}
		}		
	}
	
	public static class HarvestBlockData implements INBTSerializable<CompoundTag> {

		private BlockPos pos;
		private BlockState blockState;
		
		public HarvestBlockData(BlockPos pos, BlockState state) {
			this.pos = pos;
			this.blockState = state;
		}
		
		public HarvestBlockData(CompoundTag tag) {
			deserializeNBT(tag);
		}
		
		public BlockPos getPos() {
			return pos;
		}

		public BlockState getBlockState() {
			return blockState;
		}
		
		@Override
		public CompoundTag serializeNBT() {
			CompoundTag tag = new CompoundTag();
			tag.put("BlockPos", BlockUtils.saveBlockPos(pos));
			tag.put("BlockState", NbtUtils.writeBlockState(this.blockState));
			return tag;
		}

		@Override
		public void deserializeNBT(CompoundTag nbt) {
			this.pos = BlockUtils.loadBlockPos(nbt, "BlockPos");
			this.blockState = NbtUtils.readBlockState(nbt.getCompound("BlockState"));
		}
		
		@Override
		public boolean equals(Object obj) {
			if(obj == null || !(obj instanceof HarvestBlockData)) {
				return false;
			}
			HarvestBlockData data = (HarvestBlockData)obj;
			return data.pos.equals(pos) && data.blockState.equals(blockState);
		}
		
	}
	
	//FLUIDS
	
	public static boolean isFluidBlock(Block block) {
		return block instanceof LiquidBlock;
    }

	public static int getFluidLevel(BlockState srcState) {
        if (srcState.getBlock() instanceof LiquidBlock) {
            return srcState.getValue(LiquidBlock.LEVEL);
        }
        return -1;
    }
	
	public static boolean isFluidSource(Level level, BlockPos pos) {
		BlockState blockstate = level.getBlockState(pos);
		if(blockstate.hasProperty(BlockStateProperties.WATERLOGGED)
				&& blockstate.getValue(BlockStateProperties.WATERLOGGED)) {
			return true;
		}
		if (blockstate.getFluidState()
				.getType() != Fluids.EMPTY && blockstate.getCollisionShape(level, pos, CollisionContext.empty())
					.isEmpty()) {
			return true;
		}
		if(getFluidLevel(blockstate) == 0) {
			return true;
		}
		return false;
	}
	
	public static FluidStack getFluidFromSource(Level level, BlockPos pos) {
		BlockState blockstate = level.getBlockState(pos);
		FluidState fluidstate = level.getFluidState(pos);
		Material material = blockstate.getMaterial();
		Fluid fluid = fluidstate.getType();
		if (blockstate.getBlock() instanceof LiquidBlock && getFluidLevel(blockstate) == 0) {
			FluidStack stack = new FluidStack(fluid, FluidType.BUCKET_VOLUME);
			return stack;
		}
		else if (blockstate.getBlock() instanceof BucketPickup && fluid != Fluids.EMPTY) {
			FluidStack stack = new FluidStack(fluid, FluidType.BUCKET_VOLUME);
			return stack;
		}
		else if (blockstate.hasProperty(BlockStateProperties.WATERLOGGED)
				&& blockstate.getValue(BlockStateProperties.WATERLOGGED)) {
			FluidStack stack = new FluidStack(Fluids.WATER, FluidType.BUCKET_VOLUME);
			return stack;
		}
		else if (material == Material.WATER_PLANT || material == Material.REPLACEABLE_WATER_PLANT) {
			FluidStack stack = new FluidStack(Fluids.WATER, FluidType.BUCKET_VOLUME);
			return stack;
		}
		return FluidStack.EMPTY;
	}
	
	public static FluidStack consumeFluidSource(Level level, BlockPos pos, BlockState replacementBlock) {
		BlockState blockstate = level.getBlockState(pos);
		FluidState fluidstate = level.getFluidState(pos);
		Material material = blockstate.getMaterial();
		Fluid fluid = fluidstate.getType();
		if (blockstate.getBlock() instanceof LiquidBlock) {
			FluidStack stack = new FluidStack(fluid, FluidType.BUCKET_VOLUME);
			level.setBlock(pos, replacementBlock, Block.UPDATE_CLIENTS);
			return stack;
		}
		else if (blockstate.getBlock() instanceof BucketPickup && fluid != Fluids.EMPTY) {
			ItemStack fluidBucket = ((BucketPickup) blockstate.getBlock()).pickupBlock(level, pos, blockstate);
			return FluidUtil.getFluidContained(fluidBucket).map(f -> new FluidStack(f, FluidType.BUCKET_VOLUME)).orElse(FluidStack.EMPTY);
		}
		else if (material == Material.WATER_PLANT || material == Material.REPLACEABLE_WATER_PLANT) {
			FluidStack stack = new FluidStack(Fluids.WATER, FluidType.BUCKET_VOLUME);
			BlockEntity tileentity = blockstate.getBlock() instanceof EntityBlock ? level.getBlockEntity(pos) : null;
			Block.dropResources(blockstate, level, pos, tileentity);
			level.setBlock(pos, replacementBlock, Block.UPDATE_CLIENTS);
			return stack;
		}
		return FluidStack.EMPTY;
	}
	
	public static ImmutableSet<BlockState> getAllBlockStates(Block... blocks) {
		return Stream.of(blocks)
        .mapMulti((Block block, Consumer<BlockState> mapper) -> block.getStateDefinition().getPossibleStates().forEach(mapper))
        .collect(ImmutableSet.toImmutableSet());
	}
	
	public static void spawnHappyParticles(ServerLevel level, BlockPos pos, RandomSource random) {
		spawnHappyParticles(level, pos, random, 15);
	}
	
	public static void spawnHappyParticles(ServerLevel level, BlockPos pos, RandomSource random, int count) {
		double d0 = 0.5D;
		double d1 = 1.0D;
		for(int i = 0; i < count; ++i) {
			double d2 = random.nextGaussian() * 0.02D;
			double d3 = random.nextGaussian() * 0.02D;
			double d4 = random.nextGaussian() * 0.02D;
			double d5 = 0.5D;
			double d6 = (double)pos.getX() + 0.5D - d5 + random.nextDouble() * d0 * 2.0D;
			double d7 = (double)pos.getY() + random.nextDouble() * d1;
			double d8 = (double)pos.getZ() + 0.5D - d5 + random.nextDouble() * d0 * 2.0D;
			((ServerLevel)level).sendParticles(ParticleTypes.HAPPY_VILLAGER, d6, d7, d8, 0, d2, d3, d4, 1.0D);
		}
	}
	
}
