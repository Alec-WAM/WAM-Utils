package alec_wam.wam_utils.blocks.entity_pod;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import alec_wam.wam_utils.blocks.WAMUtilsBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

public abstract class AbstractEntityPodBE<E extends LivingEntity> extends WAMUtilsBlockEntity implements MenuProvider {

	private int tradeTime;
	private int maxTradeTime;
	protected E tradeEntity;
	
	public final ItemStackHandler inputItems = createInputItemHandler();
	public final LazyOptional<IItemHandler> inputItemHandler = LazyOptional.of(() -> inputItems);
	public final ItemStackHandler outputItems = createOutputItemHandler();
	public final LazyOptional<IItemHandler> externalOutputItemHandler = LazyOptional.of(this::createExternalOutputItemHandler);
	public final LazyOptional<IItemHandler> combinedAllItemHandler = LazyOptional.of(this::createCombinedAllItemHandler);
	
	public AbstractEntityPodBE(BlockEntityType<?> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
		super(p_155228_, p_155229_, p_155230_);
	}

	public abstract E createNewTradeEntity();
	
	@Override
	public void setRemoved() {
		super.setRemoved();
		inputItemHandler.invalidate();
		externalOutputItemHandler.invalidate();
		combinedAllItemHandler.invalidate();
	}
	
	@Override
	public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
		tradeTime = nbt.getInt("tradeTime");
		maxTradeTime = nbt.getInt("maxTradeTime");
		if (nbt.contains("Inventory.Input")) {
			inputItems.deserializeNBT(nbt.getCompound("Inventory.Input"));
		}
		if (nbt.contains("Inventory.Output")) {
			outputItems.deserializeNBT(nbt.getCompound("Inventory.Output"));
		}
		if(descPacket) {
			if(nbt.contains("TradeEntity")) {
				loadTradeEntity(nbt.getCompound("TradeEntity"));
			}
		}
	}

	public void loadTradeEntity(CompoundTag tag) {
//		E newTradeEntity = createNewTradeEntity();
//		newTradeEntity.deserializeNBT(tag);
//		return newTradeEntity;
		Optional<EntityType<?>> optional = EntityType.by(tag);
        if(optional.isPresent()) {        	
        	try {
        		Entity entity = EntityType.loadEntityRecursive(tag, level, (p_151310_) -> {
            		return p_151310_;
            	});
        		this.tradeEntity = (E) entity;
        	}catch(Exception e) {
        		this.tradeEntity = null;
        	}
        }
	}
	
	@Override
	public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
		nbt.putInt("tradeTime", tradeTime);
		nbt.putInt("maxTradeTime", maxTradeTime);		
		nbt.put("Inventory.Input", inputItems.serializeNBT());
		nbt.put("Inventory.Output", outputItems.serializeNBT());
		if(descPacket) {
			if(tradeEntity != null) {
				nbt.put("TradeEntity", saveTradeEntity());
			}
		}
	}

	public CompoundTag saveTradeEntity() {
		return tradeEntity.serializeNBT();
	}

	public int getTradeTime() {
		return tradeTime;
	}

	public void setTradeTime(int tradeTime) {
		this.tradeTime = tradeTime;
	}

	public int getMaxTradeTime() {
		return maxTradeTime;
	}

	public void setMaxTradeTime(int maxTradeTime) {
		this.maxTradeTime = maxTradeTime;
	}

	public E getTradeEntity() {
		return tradeEntity;
	}

	public abstract boolean isValidInputItem(ItemStack stack);
	
	@Nonnull
	protected abstract ItemStackHandler createInputItemHandler();
	
	public void updateOutputItems() {
		
	}
	
	@Nonnull
	protected abstract ItemStackHandler createOutputItemHandler();

	@Nonnull
	protected IItemHandler createExternalOutputItemHandler() {
		return new CombinedInvWrapper(outputItems) {
			@NotNull
			@Override
			public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
				return stack;
			}
		};
	}
	
	@Nonnull
	protected IItemHandler createCombinedAllItemHandler() {
		return new CombinedInvWrapper(inputItems, outputItems) {
			@NotNull
			@Override
			public ItemStack extractItem(int slot, int amount, boolean simulate) {
				return ItemStack.EMPTY;
			}

			@NotNull
			@Override
			public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
				return stack;
			}
		};
	}
	
	@NotNull
	@Override
	public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER) {
			if (side == null) {
				return combinedAllItemHandler.cast();
			} 
			else if (side == Direction.DOWN) {
				return externalOutputItemHandler.cast();
			} 
			else if (side.getAxis() != Axis.Y) {
				return inputItemHandler.cast();
			} 
		} 
		return super.getCapability(cap, side);
	}

	public void tickServer() {
	}
	
	public void tickClient() {
		
	}

}
