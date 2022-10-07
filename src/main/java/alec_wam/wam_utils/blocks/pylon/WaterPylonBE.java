package alec_wam.wam_utils.blocks.pylon;

import java.util.List;

import alec_wam.wam_utils.blocks.pylon.PylonBlock.PylonLevel;
import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.utils.EntityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

public class WaterPylonBE extends AbstractPylonBE {

	private int spawnDelay = 0;
	
	public WaterPylonBE(BlockPos p_155229_, BlockState p_155230_) {
		super(BlockInit.WATER_PYLON_BE.get(), p_155229_, p_155230_);
	}
	
	@Override
	public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
		this.spawnDelay = nbt.getInt("SpawnDelay");
	}

	@Override
	public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
		nbt.putInt("SpawnDelay", spawnDelay);
	}

	@Override
	public void tickServer() {
		if(this.getBlockState().getValue(PylonBlock.PYLON_LEVEL) != PylonLevel.BOTTOM) {
			this.setRemoved();
			return;
		}
		
		//TODO Remove this
		if(level.hasNeighborSignal(worldPosition)) {
			return;
		}
		
		if(spawnDelay > 0) {
			spawnDelay --;
			return;
		}
		int diameter = 10;
		int radius = diameter / 2;
		AABB aabb = new AABB(worldPosition).inflate(radius + 1);
		List<Mob> mobs = level.getEntitiesOfClass(Mob.class, aabb);
		MobCategory category = getMobCategory();
		if(mobs.size() < category.getMaxInstancesPerChunk()) {		
			RandomSource random = this.level.random;
			int x = random.nextInt(-radius, radius + 1);
			int y = random.nextInt(radius + 1);
			int z = random.nextInt(-radius, radius + 1);
			BlockPos spawnBlockPos = this.worldPosition.offset(x, y, z);
			
			if(level.isLoaded(spawnBlockPos)) {
				if(level.getFluidState(spawnBlockPos) != Fluids.EMPTY.defaultFluidState()) {
					if(EntityUtils.spawnRandomCategoryEntity((ServerLevel)level, spawnBlockPos, category, MobSpawnType.NATURAL)) {
						spawnDelay = 10 * 20;
					}
				}
			}
		}
		else {
			spawnDelay = 5 * 20; //Wait 5 seconds to prevent constant AABB checking
		}
	}	

	@Override
	public void tickClient() {
		if(this.getBlockState().getValue(PylonBlock.PYLON_LEVEL) != PylonLevel.BOTTOM) {
			this.setRemoved();
			return;
		}
		
		if(level.hasNeighborSignal(worldPosition)) {
			return;
		}
		
		RandomSource random = this.level.random;
		if(random.nextInt(10) < 5) {
			double spread = 2.5D;
			double d2 = random.nextGaussian() * spread;
            double d3 = random.nextGaussian() * spread;
            double d4 = random.nextGaussian() * spread;
            double spawnX = (double)this.worldPosition.getX() + 0.5D + d2;
            double spawnY = (double)this.worldPosition.getY() + 0.5D + d3;
            double spawnZ = (double)this.worldPosition.getZ() + 0.5D + d4;
            BlockPos spawnPos = new BlockPos(spawnX, spawnY, spawnZ);
            if(level.getFluidState(spawnPos) != Fluids.EMPTY.defaultFluidState()) {
            	level.addParticle(ParticleTypes.HAPPY_VILLAGER, spawnX, spawnY, spawnZ, 0.0D, 0.0D, 0.0D);
            }
	    }
	}

	@Override
	public MobCategory getMobCategory() {
		return MobCategory.WATER_CREATURE;
	}

}

