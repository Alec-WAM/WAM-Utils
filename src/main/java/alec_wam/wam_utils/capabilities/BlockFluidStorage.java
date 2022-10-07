package alec_wam.wam_utils.capabilities;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class BlockFluidStorage implements IFluidHandler, IFluidTank, INBTSerializable<CompoundTag> {
	@Nonnull
    protected FluidStack fluid = FluidStack.EMPTY;
    protected int capacity;

    public BlockFluidStorage(int capacity) {
    	this.capacity = capacity;
    }
    
    public BlockFluidStorage setCapacity(int capacity) {
    	this.capacity = capacity;
    	return this;
    }

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		fluid.writeToNBT(tag);
		return tag;
	}

	@Override
	public void deserializeNBT(CompoundTag nbt) {
		FluidStack fluid = FluidStack.loadFluidStackFromNBT(nbt);
        setFluid(fluid);
	}

	@Override
	public @NotNull FluidStack getFluid() {
		return fluid;
	}

	@Override
	public int getFluidAmount() {
		return fluid.getAmount();
	}

	@Override
	public int getCapacity() {
		return capacity;
	}

	@Override
	public boolean isFluidValid(FluidStack stack) {
		return true;
	}

	@Override
	public int getTanks() {
		return 1;
	}

	@Override
	public @NotNull FluidStack getFluidInTank(int tank) {
		return fluid;
	}

	@Override
	public int getTankCapacity(int tank) {
		return getCapacity();
	}

	@Override
	public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
		return true;
	}
	
	public boolean canFill(FluidStack resource) {
		return isFluidValid(resource);
	}

	public int forceFill(FluidStack resource, FluidAction action) {
		if (resource.isEmpty() || !isFluidValid(resource)) {
            return 0;
        }
        if (action.simulate()) {
            if (fluid.isEmpty()) {
                return Math.min(getCapacity(), resource.getAmount());
            }
            if (!fluid.isFluidEqual(resource)) {
                return 0;
            }
            return Math.min(getCapacity() - fluid.getAmount(), resource.getAmount());
        }
        if (fluid.isEmpty()) {
            onContentsChanged();
            fluid = new FluidStack(resource, Math.min(getCapacity(), resource.getAmount()));
            return fluid.getAmount();
        }
        if (!fluid.isFluidEqual(resource)) {
            return 0;
        }
        int filled = getCapacity() - fluid.getAmount();

        if (resource.getAmount() < filled) {
            onContentsChanged();
            fluid.grow(resource.getAmount());
            filled = resource.getAmount();
        } else {
            onContentsChanged();
            fluid.setAmount(getCapacity());
        }
        return filled;
	}
	
	@Override
	public int fill(FluidStack resource, FluidAction action) {
		if (resource.isEmpty() || !isFluidValid(resource) || !canFill(resource)) {
            return 0;
        }
        if (action.simulate()) {
            if (fluid.isEmpty()) {
                return Math.min(getCapacity(), resource.getAmount());
            }
            if (!fluid.isFluidEqual(resource)) {
                return 0;
            }
            return Math.min(getCapacity() - fluid.getAmount(), resource.getAmount());
        }
        if (fluid.isEmpty()) {
            onContentsChanged();
            fluid = new FluidStack(resource, Math.min(getCapacity(), resource.getAmount()));
            return fluid.getAmount();
        }
        if (!fluid.isFluidEqual(resource)) {
            return 0;
        }
        int filled = getCapacity() - fluid.getAmount();

        if (resource.getAmount() < filled) {
            onContentsChanged();
            fluid.grow(resource.getAmount());
            filled = resource.getAmount();
        } else {
            onContentsChanged();
            fluid.setAmount(getCapacity());
        }
        return filled;
	}
	
	public boolean canDrain() {
		return true;
	}

	@Override
	public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
		if (resource.isEmpty() || !resource.isFluidEqual(getFluid()) || !canDrain()) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
	}

	public FluidStack forceDrain(int maxDrain, FluidAction action) {
		int drained = maxDrain;
		if (getFluid().getAmount() < drained) {
			drained = getFluid().getAmount();
		}
		FluidStack stack = new FluidStack(getFluid(), drained);
		if (action.execute()) {
			onContentsChanged();
			getFluid().shrink(drained);
		}
		if (getFluid().getAmount() <= 0) {
			setFluid(FluidStack.EMPTY);
		}
		return stack;
	}
	
	@Override
	public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
		if(!canDrain()) {
			return FluidStack.EMPTY;
		}
		int drained = maxDrain;
		if (fluid.getAmount() < drained) {
			drained = fluid.getAmount();
		}
		FluidStack stack = new FluidStack(fluid, drained);
		if (action.execute()) {
			onContentsChanged();
			fluid.shrink(drained);
		}
		if (fluid.getAmount() <= 0) {
			fluid = FluidStack.EMPTY;
		}
		return stack;
	}
	
	protected void onContentsChanged() {

    }
	
	public void setFluid(@Nonnull FluidStack stack) {
        this.fluid = stack;
    }

    public boolean isEmpty() {
        return fluid.isEmpty();
    }

    public int getSpace() {
        return Math.max(0, getCapacity() - fluid.getAmount());
    }
}
