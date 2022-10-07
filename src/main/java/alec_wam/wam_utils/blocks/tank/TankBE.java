package alec_wam.wam_utils.blocks.tank;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import alec_wam.wam_utils.blocks.INBTItemDrop;
import alec_wam.wam_utils.blocks.WAMUtilsBlockEntity;
import alec_wam.wam_utils.capabilities.BlockFluidStorage;
import alec_wam.wam_utils.capabilities.ItemFluidStorage;
import alec_wam.wam_utils.init.BlockInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

public class TankBE extends WAMUtilsBlockEntity implements INBTItemDrop {

	public static final int FLUID_CAPACITY = 8 * FluidType.BUCKET_VOLUME;
	
    public BlockFluidStorage fluidStorage = createFluidHandler();
    private LazyOptional<BlockFluidStorage> fluid = LazyOptional.of(() -> fluidStorage);
	
	public TankBE(BlockPos pos, BlockState state) {
		super(BlockInit.TANK_BE.get(), pos, state);
	}
	
	public void tickClient() {
	}

	public void tickServer() {		
		
		boolean pushLiquid = true;
		if(pushLiquid && fluidStorage.getFluidAmount() > 0) {
			BlockPos otherPos = worldPosition.below();
			BlockEntity blockEntity = level.getBlockEntity(otherPos);
			if(blockEntity !=null && blockEntity instanceof TankBE) {
				TankBE otherTank = (TankBE)blockEntity;
				if(otherTank.fluidStorage.getFluidAmount() < otherTank.fluidStorage.getCapacity()) {					
					blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).ifPresent(h -> {
						if(fluidStorage.getFluidAmount() > 0) {
							if(otherTank.fluidStorage.forceFill(fluidStorage.getFluid(), FluidAction.SIMULATE) > 0) {
								int output = Math.min(fluidStorage.getFluidAmount(), FluidType.BUCKET_VOLUME);
								FluidStack stack = fluidStorage.getFluid().copy();
								stack.setAmount(output);
								int filled = otherTank.fluidStorage.forceFill(stack, FluidAction.EXECUTE);
								this.fluidStorage.forceDrain(filled, FluidAction.EXECUTE);
							}
						}
					});
				}
			}
		}
	}
	
	public int specialFill(FluidStack fluidStack, FluidAction action) {
		int fillAmount = fluidStorage.forceFill(fluidStack, action);
		
		int remainder = fluidStack.getAmount() - fillAmount;
		//WAMUtilsMod.LOGGER.debug("Remainder: " + remainder);
		if(remainder > 0 && this.fluidStorage.getFluidAmount() >= this.fluidStorage.getCapacity()) {
			//Move to the above tank
			FluidStack copy = fluidStack.copy();
			copy.setAmount(remainder);
			BlockEntity beAbove = level.getBlockEntity(worldPosition.above());
			if(beAbove !=null && beAbove instanceof TankBE tank) {
				return tank.specialFill(copy, action);
			}
		}
		
		return fillAmount;
	}
	
	@Nonnull
	private BlockFluidStorage createFluidHandler() {
		return new BlockFluidStorage(FLUID_CAPACITY) {
			@Override
			protected void onContentsChanged() {
				markBlockForUpdate(null);
			}
			
			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return specialFill(resource, action);
			}
		};
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		fluid.invalidate();
	}

	@Override
	public void writeCustomNBT(CompoundTag tag, boolean descPacket) {
		fluid.ifPresent(h -> tag.put("Fluid", h.serializeNBT()));
	}

	@Override
	public void readCustomNBT(CompoundTag tag, boolean descPacket) {
		fluid.ifPresent(h -> h.deserializeNBT(tag.getCompound("Fluid")));
		if(descPacket)
			this.markBlockForUpdate(null);
	}
	
	@NotNull
	@Override
	public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return fluid.cast();
		}
		return super.getCapability(cap, side);
	}

	@Override
	public void readFromItem(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		if(tag !=null) {
			if(tag.contains("Fluid")) {
				fluid.ifPresent(h -> h.deserializeNBT(tag.getCompound(ItemFluidStorage.NBT_FLUID)));
			}
		}
		setChanged();
		markBlockForUpdate(null);
	}
	
	@Override
	public ItemStack getNBTDrop(Item item) {
		ItemStack stack = new ItemStack(item, 1);
		if(this.fluidStorage != null) {
			CompoundTag stackTag = stack.getOrCreateTag();
			CompoundTag fluidTag = fluidStorage.serializeNBT();
			stackTag.put(ItemFluidStorage.NBT_FLUID, fluidTag);
			stack.setTag(stackTag);
		}
		return stack;
	}

}

