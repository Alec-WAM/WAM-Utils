package alec_wam.wam_utils.blocks.pylon;

import java.util.List;

import alec_wam.wam_utils.blocks.pylon.PylonBlock.PylonLevel;
import alec_wam.wam_utils.init.BlockInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class EnderPylonBE extends AbstractPylonBE {

	private int spawnDelay = 0;
	private int convertDelay = 0;
	
	public EnderPylonBE(BlockPos p_155229_, BlockState p_155230_) {
		super(BlockInit.ENDER_PYLON_BE.get(), p_155229_, p_155230_);
	}
	
	@Override
	public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
		this.spawnDelay = nbt.getInt("SpawnDelay");
		this.convertDelay = nbt.getInt("ConvertDelay");
	}

	@Override
	public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
		nbt.putInt("SpawnDelay", spawnDelay);
		nbt.putInt("ConvertDelay", convertDelay);
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
		
		if(this.convertDelay > 0) {
			this.convertDelay--;
		}
		else {
			int diameter = 16;
			int radius = diameter / 2;
			RandomSource random = this.level.random;
			int x = random.nextInt(-radius, radius + 1);
			int z = random.nextInt(-radius, radius + 1);
			BlockPos convertBlockPos = this.worldPosition.offset(x, -1, z);
			if(level.isLoaded(convertBlockPos)) {
				BlockState state = level.getBlockState(convertBlockPos);
				if(state.is(BlockTags.BASE_STONE_OVERWORLD)) {
					level.levelEvent(null, 2001, convertBlockPos, Block.getId(state));
					level.setBlock(convertBlockPos, Blocks.END_STONE.defaultBlockState(), 3);
					convertDelay = 5 * 20;
				}
			}
		}
		
		if(spawnDelay > 0) {
			spawnDelay --;
		}
		else {
			int diameter = 10;
			int radius = diameter / 2;
			AABB aabb = new AABB(worldPosition).inflate(radius + 1);
			List<EnderMan> mobs = level.getEntitiesOfClass(EnderMan.class, aabb);
			if(mobs.size() < 8) {		
				RandomSource random = this.level.random;
				int x = random.nextInt(-radius, radius + 1);
				int z = random.nextInt(-radius, radius + 1);
				BlockPos spawnBlockPos = this.worldPosition.offset(x, 0, z);
				
				if(level.isLoaded(spawnBlockPos)) {
					if(Monster.isDarkEnoughToSpawn((ServerLevel)level, spawnBlockPos, random)) {
						if(spawnEnderman((ServerLevel)level, spawnBlockPos, random)) {
		//						double particleX = (double)spawnBlockPos.getX() + 0.5D;
		//						double particleY = (double)spawnBlockPos.getY() + 0.5D;
		//						double particleZ = (double)spawnBlockPos.getZ() + 0.5D;
		//						//level.addParticle(ParticleTypes.POOF, particleX, particleY, particleZ, 0.0D, 0.0D, 0.0D);
		//						
		//						for(int i = 0; i < 20; ++i) {
		//							double d0 = random.nextGaussian() * 0.02D;
		//							double d1 = random.nextGaussian() * 0.02D;
		//							double d2 = random.nextGaussian() * 0.02D;
		//							double spawnX = particleX + (2.0D * random.nextDouble() - 1.0D);
		//							double spawnY = particleX + (random.nextDouble());
		//							double spawnZ = particleZ + (2.0D * random.nextDouble() - 1.0D);
		//				         	//this.level.addParticle(ParticleTypes.POOF, spawnX, spawnY, spawnZ, d0, d1, d2);
		//				         	((ServerLevel)this.level).addParticle(ParticleTypes.POOF, spawnX, spawnY, spawnZ, d0, d1, d2);
		//						}
							
							spawnDelay = 10 * 20;
						}
					}
				}
			}
			else {
				spawnDelay = 5 * 20; //Wait 5 seconds to prevent constant AABB checking
			}
		}
	}	
	
	
	public boolean spawnEnderman(ServerLevel level, BlockPos pos, RandomSource random) {
		EntityType<?> entityType = EntityType.ENDERMAN;
		MobSpawnType spawnType = MobSpawnType.NATURAL;
		if(entityType.canSummon()) {
			SpawnPlacements.Type spawnPlacementType = SpawnPlacements.getPlacementType(entityType);
			boolean validPlacement = false;
			if(spawnPlacementType == SpawnPlacements.Type.NO_RESTRICTIONS) {
				validPlacement = true;
			}
			else if (level.getWorldBorder().isWithinBounds(pos)) {
				validPlacement = spawnPlacementType.canSpawnAt(level, pos, entityType);
			}
			if(validPlacement) {
				if(SpawnPlacements.checkSpawnRules(entityType, level, spawnType, pos, random)) {
					double spawnPosX = (double)pos.getX() + 0.5D;
					double spawnPosY = (double)pos.getY();
					double spawnPosZ = (double)pos.getZ() + 0.5D;
					AABB aabb = entityType.getAABB(spawnPosX, spawnPosY, spawnPosZ);
					if(level.noCollision(aabb)) {
						Entity entity = entityType.create(level);
						if(entity instanceof Mob mob) {
							mob.moveTo(spawnPosX, spawnPosY, spawnPosZ, random.nextFloat() * 360.0F, 0.0F);
							int canSpawn = net.minecraftforge.common.ForgeHooks.canEntitySpawn(mob, level, spawnPosX, spawnPosY, spawnPosZ, null, spawnType);
	                        if(canSpawn != -1 && (canSpawn == 1 || mob.checkSpawnRules(level, spawnType) && mob.checkSpawnObstruction(level))){
	                        	SpawnGroupData spawngroupdata = null;
	                        	if (!net.minecraftforge.event.ForgeEventFactory.doSpecialSpawn(mob, level, (float)spawnPosX, (float)spawnPosY, (float)spawnPosZ, null, spawnType))
	                        		spawngroupdata = mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), spawnType, spawngroupdata, (CompoundTag)null);
	                        	level.addFreshEntityWithPassengers(mob);   
	                        	return true;
	                        }
						}
					}
				}
			}
		}
		return false;
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
		/*if(random.nextInt(10) < 5) {
			double spread = 2.5D;
			double d2 = random.nextGaussian() * spread;
            double d3 = random.nextGaussian() * spread;
            double d4 = random.nextGaussian() * spread;
            double spawnX = (double)this.worldPosition.getX() + 0.5D + d2;
            double spawnY = (double)this.worldPosition.getY() + 0.5D + d3;
            double spawnZ = (double)this.worldPosition.getZ() + 0.5D + d4;
            if(level.getBlockState(new BlockPos(spawnX, spawnY, spawnZ)).isAir()) {
            	level.addParticle(ParticleTypes.PORTAL, spawnX, spawnY, spawnZ, 0.0D, 0.0D, 0.0D);
            }
	    }*/
		if(random.nextInt(10) < 5) {
			for(int i = 0; i < 3; i++) {
				int j = (random.nextInt(2) * 2 - 1) * 3;
				int randY = (random.nextInt(2) * 2 - 1) * 3;
				int k = (random.nextInt(2) * 2 - 1) * 3;
				double d0 = (double)this.worldPosition.getX() + 0.5D + 0.25D * (double)j;
				double d1 = (double)((float)this.worldPosition.getY() + 2 + random.nextFloat());
				double d2 = (double)this.worldPosition.getZ() + 0.5D + 0.25D * (double)k;
				double d3 = (double)(random.nextFloat() * (float)j);
				double d4 = ((double)random.nextFloat() - 0.5D) * randY;
				double d5 = (double)(random.nextFloat() * (float)k);
				level.addParticle(ParticleTypes.PORTAL, d0, d1, d2, d3, d4, d5);
			}
		}
	}

	@Override
	public MobCategory getMobCategory() {
		return MobCategory.MONSTER;
	}

}
