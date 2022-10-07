package alec_wam.wam_utils.capabilities;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

public class ItemFluidStorage implements IFluidHandlerItem, ICapabilityProvider {
	private final LazyOptional<IFluidHandlerItem> holder = LazyOptional.of(() -> this);
	public static final String NBT_FLUID = "Fluid";
	
	@NotNull
    protected ItemStack container;
	protected int capacity;
	
	public ItemFluidStorage(ItemStack stack, int capacity) {
		this.container = stack;
		this.capacity = capacity;
	}
    
    public ItemFluidStorage setCapacity(int capacity) {
    	this.capacity = capacity;
    	return this;
    }

	public @NotNull FluidStack getFluid() {
		if(!container.isEmpty()) {
			if(container.hasTag()) {
				FluidStack fluid = FluidStack.loadFluidStackFromNBT(container.getTag().getCompound(NBT_FLUID));
				return fluid;
			}
		}
		return FluidStack.EMPTY;
	}
	
	public void updateContainerFluid(FluidStack fluid) {
		CompoundTag tag = container.getOrCreateTag();
		tag.put(NBT_FLUID, fluid.writeToNBT(new CompoundTag()));
		container.setTag(tag);
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
		return capacity;
	}

	@Override
	public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
		return true;
	}
	
	public boolean canFill(FluidStack resource) {
		return isFluidValid(0, resource);
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		if (resource.isEmpty() || !isFluidValid(0, resource) || !canFill(resource)) {
            return 0;
        }
		FluidStack fluid = getFluid();
        if (action.simulate()) {
            if (fluid.isEmpty()) {
                return Math.min(capacity, resource.getAmount());
            }
            if (!fluid.isFluidEqual(resource)) {
                return 0;
            }
            return Math.min(capacity - fluid.getAmount(), resource.getAmount());
        }
        if (fluid.isEmpty()) {
            fluid = new FluidStack(resource, Math.min(capacity, resource.getAmount()));
            updateContainerFluid(fluid);
            return fluid.getAmount();
        }
        if (!fluid.isFluidEqual(resource)) {
            return 0;
        }
        int filled = capacity - fluid.getAmount();

        if (resource.getAmount() < filled) {
            fluid.grow(resource.getAmount());
            updateContainerFluid(fluid);
            filled = resource.getAmount();
        } else {
            fluid.setAmount(capacity);
            updateContainerFluid(fluid);
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

	@Override
	public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
		if(!canDrain()) {
			return FluidStack.EMPTY;
		}
		int drained = maxDrain;
		FluidStack fluid = getFluid();
		if (fluid.getAmount() < drained) {
			drained = fluid.getAmount();
		}
		FluidStack stack = new FluidStack(fluid, drained);
		if (action.execute()) {
			fluid.shrink(drained);
			updateContainerFluid(fluid);
		}
		if (fluid.getAmount() <= 0) {
			fluid = FluidStack.EMPTY;
		}
		return stack;
	}

	@Override
	public @NotNull ItemStack getContainer() {
		return container;
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
		return ForgeCapabilities.FLUID_HANDLER_ITEM.orEmpty(cap, holder);
	}
	
}
