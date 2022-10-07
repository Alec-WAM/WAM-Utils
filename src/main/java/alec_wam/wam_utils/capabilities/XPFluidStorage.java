package alec_wam.wam_utils.capabilities;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import alec_wam.wam_utils.init.FluidInit;
import alec_wam.wam_utils.utils.XPUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class XPFluidStorage implements IFluidHandler, IFluidTank, INBTSerializable<CompoundTag>  {

	private int experienceLevel;
	private float experience;
	private int experienceTotal;
	private final int maxXp;
	
	public XPFluidStorage(int capacity) {
		this.maxXp = capacity;
	}

	public int getMaximumExperiance() {    
		return maxXp;
	}

	public int getExperienceLevel() {
		return experienceLevel;
	}

	public float getExperience() {
		return experience;
	}

	public int getExperienceTotal() {
		return experienceTotal;
	}

	public void set(XPFluidStorage xpCon) {
		experienceTotal = xpCon.experienceTotal;
		experienceLevel = xpCon.experienceLevel;
		experience = xpCon.experience;    
		onContentsChanged();
	}

	public void setExperience(int xp){
		experienceTotal = xp;
		experienceLevel = XPUtil.getLevelForExperience(experienceTotal);    
		experience = (experienceTotal - XPUtil.getExperienceForLevel(experienceLevel)) / (float)getXpBarCapacity();
		onContentsChanged();
	}

	public int addExperience(int xpToAdd) {
		int j = maxXp - experienceTotal;
		if(xpToAdd > j) {
			xpToAdd = j;
		}

		experienceTotal += xpToAdd;
		experienceLevel = XPUtil.getLevelForExperience(experienceTotal);    
		experience = (experienceTotal - XPUtil.getExperienceForLevel(experienceLevel)) / (float)getXpBarCapacity();
		onContentsChanged();
		return xpToAdd;
	}

	public int getXpBarCapacity() {
		return XPUtil.getXpBarCapacity(experienceLevel);
	}
	
	public int getXpBarScaled(int scale) {
		int result = (int) (experience * scale);
		return result;

	}

	public void givePlayerXp(Player player, int levels) {
		for (int i = 0; i < levels && experienceTotal > 0; i++) {
			givePlayerXpLevel(player);
		}
	}

	public void givePlayerXpLevel(Player player) {
		int currentXP = XPUtil.getPlayerXP(player);
		int nextLevelXP = XPUtil.getExperienceForLevel(player.experienceLevel + 1);
		int requiredXP = nextLevelXP - currentXP;

		requiredXP = Math.min(experienceTotal, requiredXP);
		XPUtil.addPlayerXP(player, requiredXP);

		int newXp = experienceTotal - requiredXP;
		experience = 0;
		experienceLevel = 0;
		experienceTotal = 0;
		addExperience(newXp);
	}


	public void drainPlayerXpToReachContainerLevel(Player player, int level) {    
		int targetXP = XPUtil.getExperienceForLevel(level);
		int requiredXP = targetXP - experienceTotal;
		if(requiredXP <= 0) {
			return;
		}
		int drainXP = Math.min(requiredXP, XPUtil.getPlayerXP(player));
		addExperience(drainXP);
		XPUtil.addPlayerXP(player, -drainXP);    
	}

	public void drainPlayerXpToReachPlayerLevel(Player player, int level) {    
		int targetXP = XPUtil.getExperienceForLevel(level);
		int drainXP = XPUtil.getPlayerXP(player) - targetXP;
		if(drainXP <= 0) {
			return;
		}    
		drainXP = addExperience(drainXP);
		if(drainXP > 0) {
			XPUtil.addPlayerXP(player, -drainXP);
		}
	}
	
	@Override
	public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
		if (resource.isEmpty() || !resource.isFluidEqual(getFluid()) || !canDrain()) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
	}
	
	protected boolean canDrain() {
		return true;
	}

	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		/*if(FluidInit.XP.getSourceFluid() == null) {
			return FluidStack.EMPTY;
		}*/
		int available = getFluidAmount();
		int toDrain = Math.min(available, maxDrain);
		final int xpAskedToExtract = XPUtil.liquidToExperience(toDrain);
		// only return multiples of 1 XP (20mB) to avoid duping XP when being asked
		// for low values (like 10mB/t)
		final int fluidToExtract = XPUtil.experienceToLiquid(xpAskedToExtract);
		final int xpToExtract = XPUtil.liquidToExperience(fluidToExtract);
		if(action == FluidAction.EXECUTE) {      
			int newXp = experienceTotal - xpToExtract;
			experience = 0;
			experienceLevel = 0;
			experienceTotal = 0;
			addExperience(newXp);
		}
		return new FluidStack(FluidInit.XP.getSourceFluid(), fluidToExtract);
	}
	
	@Override
	public boolean isFluidValid(FluidStack stack) {
		return stack.getFluid() == FluidInit.XP.getSourceFluid();
	}
	
	@Override
	public int fill(FluidStack resource, FluidAction action) {
		if(resource == null) {
			return 0;
		}
		if(resource.getAmount() <= 0) {
			return 0;
		}
		if(!canFill(resource)) {
			return 0;
		}
		//need to do these calcs in XP instead of fluid space to avoid type overflows
		int xp = XPUtil.liquidToExperience(resource.getAmount());
		int xpSpace = getMaximumExperiance() - getExperienceTotal();
		int canFillXP = Math.min(xp, xpSpace);
		if(canFillXP <= 0) {
			return 0;
		}
		if(action.execute()) {
			addExperience(canFillXP);
		}
		return XPUtil.experienceToLiquid(canFillXP);
	}

	@Override
	public int getCapacity() {
		if(maxXp == Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		return XPUtil.experienceToLiquid(maxXp);
	}

	@Override
	public int getFluidAmount() {
		return XPUtil.experienceToLiquid(experienceTotal);
	}
	
	@Override
	public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("experienceLevel", experienceLevel);
		tag.putInt("experienceTotal", experienceTotal);
		tag.putFloat("experience", experience);
		return tag;
	}
	
	@Override
	public void deserializeNBT(CompoundTag nbt) {
		experienceLevel = nbt.getInt("experienceLevel");
		experienceTotal = nbt.getInt("experienceTotal");
		experience = nbt.getFloat("experience");
	}
	
	@Override
	public FluidStack getFluid() {
		return new FluidStack(FluidInit.XP.getSourceFluid(), getFluidAmount());
	}

	public void setFluid(@Nullable FluidStack fluid) {
		experience = 0;
		experienceLevel = 0;
		experienceTotal = 0;
		if (fluid != null && fluid.getFluid() != null && fluid.getFluid() != Fluids.EMPTY) {
			if (FluidInit.XP.getSourceFluid() == fluid.getFluid()) {
				addExperience(XPUtil.liquidToExperience(fluid.getAmount()));
			} 
		}
		onContentsChanged();
	}
	
	protected void onContentsChanged() {
		
	}
	
	public boolean canFill(FluidStack resource) {
		return isFluidValid(resource);
	}

	@Override
	public int getTanks() {
		return 1;
	}

	@Override
	public @NotNull FluidStack getFluidInTank(int tank) {
		return getFluid();
	}

	@Override
	public int getTankCapacity(int tank) {
		return getCapacity();
	}

	@Override
	public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
		return canFill(stack);
	}

}
