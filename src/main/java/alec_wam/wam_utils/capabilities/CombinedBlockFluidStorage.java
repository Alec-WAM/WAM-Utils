package alec_wam.wam_utils.capabilities;

import org.jetbrains.annotations.NotNull;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class CombinedBlockFluidStorage implements IFluidHandler {

	private final BlockFluidStorage[] tanks;
	
	public CombinedBlockFluidStorage(BlockFluidStorage... tanks) {
		this.tanks = tanks;
	}

	@Override
	public int getTanks() {
		return tanks.length;
	}

	@Override
	public @NotNull FluidStack getFluidInTank(int tank) {
		return tanks[tank].fluid;
	}

	@Override
	public int getTankCapacity(int tank) {
		return tanks[tank].getCapacity();
	}

	@Override
	public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
		return tanks[tank].isFluidValid(stack);
	}
	
	@Override
	public int fill(FluidStack resource, FluidAction action) {
		int amount = 0;
		for(BlockFluidStorage tank : tanks) {
			if(tank.canFill(resource)) {
				amount += tank.fill(resource, action);
			}
		}
		return amount;
	}
	
	@Override
	public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
		FluidStack returnStack = FluidStack.EMPTY;
		for(BlockFluidStorage tank : tanks) {
			if(tank.canDrain()) {
				FluidStack drain = tank.drain(resource, action);
				if(!drain.isEmpty()) {
					if(returnStack.isEmpty()) {
						returnStack = drain;
					}
					else if(returnStack.isFluidEqual(drain)) {
						returnStack.grow(drain.getAmount());
					}
				}
			}
		}
        return returnStack;
	}
	
	//THIS IS NOT HANDLED THE BEST WAY AND WILL JUST GRAB THE FIRST TANK
	@Override
	public @NotNull FluidStack drain(int amount, FluidAction action) {
		FluidStack returnStack = FluidStack.EMPTY;
		for(BlockFluidStorage tank : tanks) {
			if(tank.canDrain()) {
				return tank.drain(amount, action);
			}
		}
        return returnStack;
	}

}
