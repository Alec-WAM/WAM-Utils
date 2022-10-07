package alec_wam.wam_utils.blocks.machine.flower_generator;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.Maps;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.blocks.RedstoneMode;
import alec_wam.wam_utils.blocks.WAMUtilsBlockEntity;
import alec_wam.wam_utils.entities.WAMUtilsFakePlayer;
import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.utils.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.data.worldgen.placement.NetherPlacements;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

public class FlowerSimulatorBE extends WAMUtilsBlockEntity {

	public static final int OUTPUT_SLOTS = 5;
	protected final ItemStackHandler outputItems = createOutputItemHandler();
	public final LazyOptional<IItemHandler> externalItemHandler = LazyOptional.of(this::createExternalOutputItemHandler);
	
	private int generateTime = 0;
	
	protected final RandomSource random = RandomSource.create();
	
	private FakeFlowerLevel fakeLevel;
	private ChunkGenerator fakeChunkGenerator;
	
	public FlowerSimulatorBE(BlockPos p_155229_, BlockState p_155230_) {
		super(BlockInit.FLOWER_SIMULATOR_BE.get(), p_155229_, p_155230_);
	}

	@Override
	public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
		if (nbt.contains("Inventory.Output")) {
			outputItems.deserializeNBT(nbt.getCompound("Inventory.Output"));
		}
		this.generateTime = nbt.getInt("GenerateTime");		
	}

	@Override
	public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
		nbt.put("Inventory.Output", outputItems.serializeNBT());
		nbt.putInt("GenerateTime", generateTime);
	}
	
	public void tickServer() {
		ServerLevel serverlevel = (ServerLevel)this.level;
		
		if(this.fakeLevel == null) {
			this.fakeLevel = new FakeFlowerLevel(
					serverlevel,
					this::getFakeBiome,
					this::setFakeBlock,
					this::getFakeBlock,
					this::getFakeFluid,
					this::getFakeHeight,
					-16,
					16
			);
		}
		if(this.fakeChunkGenerator == null) {
			this.fakeChunkGenerator = new FakeFlowerChunkGenerator(
					((ServerLevel)this.level).getChunkSource().getGenerator().getBiomeSource(),
					this::getFakeBiomeSettings
			);
		}
		
		if(InventoryUtils.isFull(outputItems)) {
			return;
		}
		
		if(!RedstoneMode.ON.isMet(serverlevel, worldPosition)) {
			return;
		}
		
		if(generateTime > 0) {
			generateTime--;
		}
		else {
			Holder<Biome> biomeHolder = getSimulationBiome();
			if(biomeHolder !=null) {
				Biome biome = biomeHolder.get();
				BiomeGenerationSettings settings = biome.getGenerationSettings();
				List<ConfiguredFeature<?, ?>> flowerFeatures = settings.getFlowerFeatures();
				//WAMUtilsMod.LOGGER.debug("Features: " + flowerFeatures.size());
				ChunkGenerator generator = fakeChunkGenerator == null ? ((ServerLevel)this.level).getChunkSource().getGenerator() : fakeChunkGenerator;
				
				int spread = 16;
				int randomX = this.random.nextIntBetweenInclusive(-spread, spread);
				int randomZ = this.random.nextIntBetweenInclusive(-spread, spread);
				
				BlockPos fakeGeneratePosition = new BlockPos(randomX, 1, randomZ);
				
				Holder<PlacedFeature> holder;
	            if (!flowerFeatures.isEmpty() /*&& this.random.nextInt(8) == 0*/) {
	               //1 in 8 chance for flower
	               holder = ((RandomPatchConfiguration)flowerFeatures.get(0).config()).feature();
	            } else {
	            	if(biomeHolder.is(Biomes.WARPED_FOREST)) {
						holder = NetherPlacements.WARPED_FOREST_VEGETATION;
					}
	            	else if(biomeHolder.is(Biomes.CRIMSON_FOREST)) {
						holder = NetherPlacements.CRIMSON_FOREST_VEGETATION;
					}
					else {
						holder = VegetationPlacements.GRASS_BONEMEAL;
					}
	            }
				
	            if(holder !=null) {
	            	PlacedFeature feature = holder.value();
	            	ConfiguredFeature<?, ?> configuredfeature = feature.feature().value();
	            	try {
	            		if(configuredfeature.place(this.fakeLevel, generator, this.level.random, fakeGeneratePosition)) {
							harvestFakeBlocks();
							generateTime = 10 * 20;
						}
						else {
							if(!this.fakeBlockCache.isEmpty()) {
								this.fakeBlockCache.clear();
							}
						}
	            	}
					catch(FakeFlowerLevelException e) {
						e.printStackTrace();
					}
					catch(Exception e) {
						e.printStackTrace();
					}
	            }
			}
		}
		
	}
	
	@SuppressWarnings("deprecation")
	public Holder<Biome> getSimulationBiome() {
		Optional<Holder<Biome>> biomeHolder = BuiltinRegistries.BIOME.getHolder(Biomes.WARPED_FOREST);
		//Optional<Holder<Biome>> biomeHolder = Optional.of(level.getBiome(worldPosition));
		return biomeHolder.isPresent() ? biomeHolder.get() : null;
	}
	
	public Holder<Biome> getFakeBiome(BlockPos pos) {
		Holder<Biome> holder = getSimulationBiome();
		return holder == null ? this.level.getBiome(pos) : holder;
	}
	
	public BiomeGenerationSettings getFakeBiomeSettings(Holder<Biome> holder) {
		return holder.value().getGenerationSettings();
	}
	
	private Map<BlockPos, BlockState> fakeBlockCache = Maps.newHashMap();
	
	public void setFakeBlock(BlockPos pos, BlockState state) {
		//WAMUtilsMod.LOGGER.debug("Set Block: " + pos + " " + state);
		fakeBlockCache.put(pos, state);
	}
	
	public BlockState getFakeBlock(BlockPos pos) {
		Holder<Biome> holder = getSimulationBiome();
		
		if(pos.getY() == 0) {
			if(holder !=null) {
				if(holder.is(BiomeTags.IS_END)) {
					return Blocks.END_STONE.defaultBlockState();
				}
				if(holder.is(BiomeTags.IS_NETHER)) {
					if(holder.is(Biomes.WARPED_FOREST)) {
						return Blocks.WARPED_NYLIUM.defaultBlockState();
					}
					if(holder.is(Biomes.CRIMSON_FOREST)) {
						return Blocks.CRIMSON_NYLIUM.defaultBlockState();
					}
					return Blocks.NETHERRACK.defaultBlockState();
				}
			}
			return Blocks.GRASS_BLOCK.defaultBlockState();
		}
		if(pos.getY() < 0) {
			if(holder !=null) {
				if(holder.is(BiomeTags.IS_END)) {
					return Blocks.END_STONE.defaultBlockState();
				}
				if(holder.is(BiomeTags.IS_NETHER)) {
					return Blocks.NETHERRACK.defaultBlockState();
				}
			}
			return Blocks.STONE.defaultBlockState();
		}
		return fakeBlockCache.getOrDefault(pos, Blocks.AIR.defaultBlockState());
	}
	
	public FluidState getFakeFluid(BlockPos pos) {
		return Fluids.EMPTY.defaultFluidState();
	}
	
	public void harvestFakeBlocks() {
		WAMUtilsFakePlayer fakePlayer = WAMUtilsFakePlayer.get((ServerLevel)this.level).get();
		for(Entry<BlockPos, BlockState> harvestBlock : fakeBlockCache.entrySet()) {
			BlockPos pos = harvestBlock.getKey();
			BlockState state = harvestBlock.getValue();
			//WAMUtilsMod.LOGGER.debug("Harvest Block: " + pos + " " + state);
			
			ItemStack tool = new ItemStack(Items.SHEARS);//ItemStack.EMPTY; //TODO Maybe shears?
			LootContext.Builder builder = new LootContext.Builder((ServerLevel) level)
	                .withRandom(level.random)
	                .withParameter(LootContextParams.ORIGIN, new Vec3(pos.getX(), pos.getY(), pos.getZ()))
	                .withParameter(LootContextParams.TOOL, tool)
	                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, null)
	                .withOptionalParameter(LootContextParams.THIS_ENTITY, fakePlayer);
	        /*if (fortune > 0) {
	            builder.withLuck(fortune);
	        }*/
	        List<ItemStack> drops = state.getDrops(builder);
	        
	        for(ItemStack drop : drops) {
	        	ItemStack insertStack = drop.copy();
	        	InventoryUtils.forceStackInInventoryAllSlots(outputItems, insertStack, null);
	        }
		}
		fakeBlockCache.clear();
	}
	
	public int getFakeHeight(Types type) {
		WAMUtilsMod.LOGGER.debug("getFakeHeight: ");
		return 1;
	}
	
	public void tickClient() {
		
	}
	
	@Override
	public void setRemoved() {
		super.setRemoved();
		externalItemHandler.invalidate();
	}

	@Nonnull
	private ItemStackHandler createOutputItemHandler() {
		return new ItemStackHandler(OUTPUT_SLOTS) {
			@Override
			protected void onContentsChanged(int slot) {
				setChanged();
				markBlockForUpdate(null);
			}

			@Nonnull
			@Override
			public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
				return super.insertItem(slot, stack, simulate);
			}
		};
	}

	@Nonnull
	protected IItemHandler createExternalOutputItemHandler() {
		return new CombinedInvWrapper(outputItems) {
			@NotNull
			@Override
			public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
				return stack;
			}
		};
	}
	
	@NotNull
	@Override
	public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER) {
			return externalItemHandler.cast();
		} 
		return super.getCapability(cap, side);
	}

}
