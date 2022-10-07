package alec_wam.wam_utils.blocks.machine.flower_generator;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import alec_wam.wam_utils.WAMUtilsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.ScheduledTick;

public class FakeFlowerLevel implements WorldGenLevel {

	public class FakeLevelTickAccess<T> implements LevelTickAccess<T> {

		@Override
		public void schedule(ScheduledTick<T> p_193428_) {
			
		}

		@Override
		public boolean hasScheduledTick(BlockPos p_193429_, T p_193430_) {
			return false;
		}

		@Override
		public int count() {
			return 0;
		}

		@Override
		public boolean willTickThisTick(BlockPos p_193197_, T p_193198_) {
			return false;
		}
		
	}
	
	private final FakeLevelTickAccess<Block> fakeBlockTicks = new FakeLevelTickAccess<Block>();
	private final FakeLevelTickAccess<Fluid> fakeFluidTicks = new FakeLevelTickAccess<Fluid>();
	
	private ServerLevel realLevel;
	public Function<BlockPos, Holder<Biome>> getBiomeFunction;
	public BiConsumer<BlockPos, BlockState> setBlockConsumer;
	public Function<BlockPos, BlockState> getBlockFunction;
	public Function<BlockPos, FluidState> getFluidFunction;
	public final int minHeight;
	public final int maxHeight;
	public Function<Types, Integer> getHeight;
	
	public FakeFlowerLevel(ServerLevel realLevel, 
			Function<BlockPos, Holder<Biome>> getBiomeFunction, 
			BiConsumer<BlockPos, BlockState> setBlockConsumer, 
			Function<BlockPos, BlockState> getBlockFunction, 
			Function<BlockPos, FluidState> getFluidFunction, 
			Function<Types, Integer> getHeight,
			int minHeight, int maxHeight) {
		this.realLevel = realLevel;
		this.getBiomeFunction = getBiomeFunction;
		this.setBlockConsumer = setBlockConsumer;
		this.getBlockFunction = getBlockFunction;
		this.getFluidFunction = getFluidFunction;
		this.getHeight = getHeight;
		this.minHeight = minHeight;
		this.maxHeight = maxHeight;
	}

	@Override
	public ServerLevel getLevel() {
		throw new FakeFlowerLevelException("FakeFlowerLevel does not have a level source");
	}

	@Override
	public long nextSubTickCount() {
		return this.realLevel.nextSubTickCount();
	}

	@Override
	public LevelTickAccess<Block> getBlockTicks() {
		return fakeBlockTicks;
	}

	@Override
	public LevelTickAccess<Fluid> getFluidTicks() {
		return fakeFluidTicks;
	}

	@Override
	public LevelData getLevelData() {
		return this.realLevel.getLevelData();
	}

	@Override
	public DifficultyInstance getCurrentDifficultyAt(BlockPos p_46800_) {
		return this.realLevel.getCurrentDifficultyAt(p_46800_);
	}

	@Override
	public MinecraftServer getServer() {
		return this.realLevel.getServer();
	}

	@Override
	public ChunkSource getChunkSource(){
		throw new FakeFlowerLevelException("FakeFlowerLevel does not have a chunk source");
	}
	
	@Override
	public boolean hasChunk(int p_46794_, int p_46795_) {
		return true;
	}

	@Override
	public RandomSource getRandom() {
		return this.realLevel.getRandom();
	}

	@Override
	public void playSound(Player p_46775_, BlockPos p_46776_, SoundEvent p_46777_, SoundSource p_46778_, float p_46779_,
			float p_46780_) {
	}

	@Override
	public void addParticle(ParticleOptions p_46783_, double p_46784_, double p_46785_, double p_46786_,
			double p_46787_, double p_46788_, double p_46789_) {
	}

	@Override
	public void levelEvent(Player p_46771_, int p_46772_, BlockPos p_46773_, int p_46774_) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gameEvent(GameEvent p_220404_, Vec3 p_220405_, Context p_220406_) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public RegistryAccess registryAccess() {
		return this.realLevel.registryAccess();
	}

	@Override
	public List<Entity> getEntities(Entity p_45936_, AABB p_45937_, Predicate<? super Entity> p_45938_) {
		return Collections.emptyList();
	}

	@Override
	public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> p_151464_, AABB p_151465_,
			Predicate<? super T> p_151466_) {
		return Collections.emptyList();
	}

	@Override
	public List<? extends Player> players() {
		return Collections.emptyList();
	}
	
	@Nullable
	public Player getNearestPlayer(double p_9501_, double p_9502_, double p_9503_, double p_9504_, Predicate<Entity> p_9505_) {
		return null;
	}

	@Override
	public boolean addFreshEntity(Entity p_9580_) {
		return false;
	}
	
	@Override
	public ChunkAccess getChunk(int p_46823_, int p_46824_, ChunkStatus p_46825_, boolean p_46826_) {
		WAMUtilsMod.LOGGER.debug("FakeFlowerWorld: get Chunk");
		
		return null;
	}

	@Override
	public int getHeight(Types p_46827_, int p_46828_, int p_46829_) {
		return this.getHeight.apply(p_46827_);
	}

	@Override
	public int getSkyDarken() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public BiomeManager getBiomeManager() {
		return this.realLevel.getBiomeManager();
	}

	@Override
	public Holder<Biome> getUncachedNoiseBiome(int p_204159_, int p_204160_, int p_204161_) {
		return this.realLevel.getUncachedNoiseBiome(p_204159_, p_204160_, p_204161_);
	}

	@Override
	public boolean isClientSide() {
		return false;
	}

	@Override
	public int getSeaLevel() {
		return this.realLevel.getSeaLevel();
	}

	@Override
	public DimensionType dimensionType() {
		return this.realLevel.dimensionType();
	}

	@Override
	public float getShade(Direction p_45522_, boolean p_45523_) {
		return 1.0F;
	}

	@Override
	public LevelLightEngine getLightEngine() {
		return this.realLevel.getLightEngine();
	}
	
	@Override
	public Holder<Biome> getBiome(BlockPos pos) {
		return getBiomeFunction.apply(pos);
	}

	@Override
	public BlockEntity getBlockEntity(BlockPos p_45570_) {
		return null;
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		//WAMUtilsMod.LOGGER.debug("FakeFlowerWorld: get Block" + pos);
		return getBlockFunction.apply(pos);
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return getFluidFunction.apply(pos);
	}

	@Override
	public WorldBorder getWorldBorder() {
		return this.realLevel.getWorldBorder();
	}

	@Override
	public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> test) {
		BlockState fakeState = getBlockFunction.apply(pos);
		return test.test(fakeState);
	}

	@Override
	public boolean isFluidAtPosition(BlockPos pos, Predicate<FluidState> test) {
		FluidState fakeState = getFluidFunction.apply(pos);
		return test.test(fakeState);
	}

	@Override
	public boolean setBlock(BlockPos pos, BlockState state, int p_46949_, int p_46950_) {
		//WAMUtilsMod.LOGGER.debug("FakeFlowerWorld: Set Block" + pos + " " + state);
		setBlockConsumer.accept(pos, state);
		return true;
	}

	@Override
	public boolean removeBlock(BlockPos p_46951_, boolean p_46952_) {
		return this.setBlock(p_46951_, Blocks.AIR.defaultBlockState(), 3);
	}

	@Override
	public boolean destroyBlock(BlockPos pos, boolean p_46958_, Entity p_46959_, int p_46960_) {
		BlockState blockstate = this.getBlockState(pos);
		if (blockstate.isAir()) {
			return false;
		} else {
			/*if (p_9551_) {
				BlockEntity blockentity = blockstate.hasBlockEntity() ? this.getBlockEntity(p_9550_) : null;
				Block.dropResources(blockstate, this.level, p_9550_, blockentity, p_9552_, ItemStack.EMPTY);
			}*/

			return this.setBlock(pos, Blocks.AIR.defaultBlockState(), 3, p_46960_);
		}
	}

	@Override
	public long getSeed() {
		return this.realLevel.getSeed();
	}
	
	@Override
	public int getMinBuildHeight() {
		return this.minHeight;
	}

	@Override
	public int getHeight() {
		return this.maxHeight;
	}

}
