package alec_wam.wam_utils.blocks.village_manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import alec_wam.wam_utils.blocks.WAMUtilsBlockEntity;
import alec_wam.wam_utils.init.BlockInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

//public class VillageManagerBE extends WAMUtilsBlockEntity {
//
//	private List<VillagerInfo> villagerInfo = new ArrayList<VillagerInfo>();
//	
//	public VillageManagerBE(BlockPos p_155229_, BlockState p_155230_) {
//		super(BlockInit.VILLAGE_MANAGER_BE.get(), p_155229_, p_155230_);
//	}
//
//	@Override
//	public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
//		
//	}
//
//	@Override
//	public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
//		
//	}
//	
//	public static class VillagerInfo {
//		public GlobalPos homePos;
//		public GlobalPos workStation;
//		public VillagerData data;
//		public boolean showRestockTime;
//		public long lastRestockTime;
//	}
//	
//	public void tickServer() {
//		ServerLevel serverlevel = (ServerLevel)this.level;
//		double range = 20.0D;
//		AABB aabb = new AABB(worldPosition).inflate(range, range, range);
//		
//		List<Villager> villagers = this.level.getEntitiesOfClass(Villager.class, aabb);
//		villagers.stream().forEach((villager) -> {
//			VillagerInfo info = new VillagerInfo();
//			Optional<GlobalPos> homePos = villager.getBrain().getMemory(MemoryModuleType.HOME);
//			if(homePos.isPresent()) {
//				info.homePos = homePos.get();
//			}
//			Optional<GlobalPos> jobPos = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
//			if(jobPos.isPresent()) {
//				info.workStation = jobPos.get();
//			}
//			info.data = villager.getVillagerData();
//			info.showRestockTime = needsToRestock(villager) && !
//		});
//	}
//	
//	private boolean needsToRestock(Villager villager) {
//		for(MerchantOffer merchantoffer : villager.getOffers()) {
//			if (merchantoffer.needsRestock()) {
//				return true;
//			}
//		}
//
//		return false;
//	}
//	
//	private boolean allowedToRestock(Villager villager) {
//		return villager.numberOfRestocksToday == 0 || villager.numberOfRestocksToday < 2 && this.level.getGameTime() > villager.lastRestockGameTime + 2400L;
//	}
//	
//	@Override
//	public void setRemoved() {
//		super.setRemoved();
//	}
//
//}
