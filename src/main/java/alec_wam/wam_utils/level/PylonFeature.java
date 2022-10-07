package alec_wam.wam_utils.level;

import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;

import com.mojang.serialization.Codec;

import alec_wam.wam_utils.blocks.pylon.PylonBlock;
import alec_wam.wam_utils.blocks.pylon.PylonBlock.PylonLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

public class PylonFeature extends Feature<NoneFeatureConfiguration> {
   public static final Predicate<BlockState> IS_SURFACE = ((blockstate) -> {
	   return blockstate.is(BlockTags.DIRT) || blockstate.is(BlockTags.BASE_STONE_OVERWORLD) || blockstate.is(BlockTags.SAND);
   });

   public final Block pylonBlock;
   @Nullable
   public final ResourceLocation lootTable;
   
   public PylonFeature(Codec<NoneFeatureConfiguration> p_65599_, Block pylonBlock) {
	   this(p_65599_, pylonBlock, null);
   }
   
   public PylonFeature(Codec<NoneFeatureConfiguration> p_65599_, Block pylonBlock, ResourceLocation lootTable) {
      super(p_65599_);
      this.pylonBlock = pylonBlock;
      this.lootTable = lootTable;
   }

   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> p_159571_) {
      WorldGenLevel worldgenlevel = p_159571_.level();
      
      BlockPos surface = findSurfacePos(worldgenlevel, p_159571_.origin());           
      
      return placePylon(worldgenlevel, p_159571_.random(), surface, pylonBlock, lootTable);
   }
   
   public BlockPos findSurfacePos(WorldGenLevel level, BlockPos origin) {
	   /*BlockPos blockpos = origin;

	   for(blockpos = blockpos.above(); level.isEmptyBlock(blockpos) && blockpos.getY() > level.getMinBuildHeight() + 1; blockpos = blockpos.below()) {
	   }
	   return blockpos;*/
	   return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin);
   }
   
   public boolean placePylon(WorldGenLevel level, RandomSource random, BlockPos pos, Block block, @Nullable ResourceLocation lootTable) {
	   for(int i = 0; i < 3; i++) {		   
		   BlockPos otherPos = pos.above(i);
		   
		   if(level.isEmptyBlock(otherPos) || level.getBlockState(otherPos).getCollisionShape(level, otherPos).isEmpty()) {
			   continue;
		   }
		   
		   if(!Feature.isReplaceable(BlockTags.FEATURES_CANNOT_REPLACE).test(level.getBlockState(otherPos))) {
			   return false;
		   }
	   }
	   
	   BlockState below = level.getBlockState(pos.below());
	   if(!below.isFaceSturdy(level, pos.below(), Direction.UP)) {
		   return false;
	   }
	   
	   level.setBlock(pos, block.defaultBlockState().setValue(PylonBlock.PYLON_LEVEL, PylonLevel.BOTTOM), 2);
	   level.setBlock(pos.above(), block.defaultBlockState().setValue(PylonBlock.PYLON_LEVEL, PylonLevel.MIDDLE), 2);
	   level.setBlock(pos.above(2), block.defaultBlockState().setValue(PylonBlock.PYLON_LEVEL, PylonLevel.TOP), 2);
	   
	   if(lootTable !=null) {
		   List<Direction> validSides = Lists.newArrayList();
		   for(Direction dir : Direction.Plane.HORIZONTAL) {
			   BlockPos otherPos = pos.relative(dir);
			   
			   if(level.isEmptyBlock(otherPos) || level.getBlockState(otherPos).getCollisionShape(level, otherPos).isEmpty()) {
				   BlockState belowState = level.getBlockState(otherPos.below());
				   if(belowState.isFaceSturdy(level, otherPos.below(), Direction.UP)) {
					   validSides.add(dir);
				   }
			   }
		   }
		   
		   if(validSides.isEmpty()) {
			   for(Direction dir : Direction.Plane.HORIZONTAL) {
				   BlockPos otherPos = pos.relative(dir);
				   
				   if(Feature.isReplaceable(BlockTags.FEATURES_CANNOT_REPLACE).test(level.getBlockState(otherPos))) {
					   BlockState belowState = level.getBlockState(otherPos.below());
					   if(belowState.isFaceSturdy(level, otherPos.below(), Direction.UP)) {
						   validSides.add(dir);
					   }
				   }
			   }
		   }
		   
		   if(!validSides.isEmpty()) {
			   Direction side = validSides.get(random.nextInt(validSides.size()));
			   BlockPos chestPos = pos.relative(side);
			   
			   BlockState chestState = StructurePiece.reorient(level, chestPos, Blocks.CHEST.defaultBlockState());
			   level.setBlock(chestPos, chestState, 2);
			   RandomizableContainerBlockEntity.setLootTable(level, random, chestPos, lootTable);
		   }
		   else {
			   return false;
		   }
	   }
	   return true;
   }
}
