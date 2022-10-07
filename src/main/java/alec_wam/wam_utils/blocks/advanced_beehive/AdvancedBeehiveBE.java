package alec_wam.wam_utils.blocks.advanced_beehive;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.utils.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

public class AdvancedBeehiveBE extends BeehiveBlockEntity {
	
	public static final int INPUT_SLOTS = 1;
	public static final int OUTPUT_SLOTS = 5;
	protected final ItemStackHandler inputItems = createInputItemHandler();	
	public final LazyOptional<IItemHandler> inputItemHandler = LazyOptional.of(() -> inputItems);
	protected final ItemStackHandler outputItems = createOutputItemHandler();	
	public final LazyOptional<IItemHandler> outputItemHandler = LazyOptional.of(() -> outputItems);
	public final LazyOptional<IItemHandler> combinedAllItemHandler = LazyOptional.of(this::createCombinedAllItemHandler);
	
	public int clientBeeCount = 0;
	
	public AdvancedBeehiveBE(BlockPos p_155134_, BlockState p_155135_) {
		super(p_155134_, p_155135_);
	}

	@Override
	public BlockEntityType<?> getType() {
		return BlockInit.ADVANCED_BEEHIVE_BE.get();
	}
	
	@Override
	public boolean isFull() {
		return getOccupantCount() == 6;
	}

	public void customServerTick(Level level, BlockPos pos, BlockState state) {
		final int beeCount = getOccupantCount();
		BeehiveBlockEntity.serverTick(level, pos, state, this);
		if(getOccupantCount() != beeCount) {
			super.setChanged();
			BlockState currentState = level.getBlockState(pos);
			level.sendBlockUpdated(pos, currentState, currentState, 3);
		}
		
		if(getHoneyLevel(state) >= BeehiveBlock.MAX_HONEY_LEVELS) {
			ItemStack input = this.inputItems.getStackInSlot(0);
			ItemStack output = ItemStack.EMPTY;
			boolean damageItem = false;
			SoundEvent sound = null;
			if(input.is(Items.GLASS_BOTTLE)) {
				output = new ItemStack(Items.HONEY_BOTTLE);
				sound = SoundEvents.BOTTLE_FILL;
			}
			else if(input.canPerformAction(ToolActions.SHEARS_HARVEST)) {
				output = new ItemStack(Items.HONEYCOMB, 3);
				damageItem = true;
				sound = SoundEvents.BEEHIVE_SHEAR;
			}
			
			if(!output.isEmpty() && InventoryUtils.simulateForceStackInInventoryAllSlots(outputItems, output).isEmpty()) {	
				InventoryUtils.forceStackInInventoryAllSlots(outputItems, output, (slot) -> {
					AdvancedBeehiveBE.super.setChanged();
				});
				
				if(damageItem) {
					if(input.hurt(INPUT_SLOTS, level.getRandom(), null)) {
						input.shrink(1);
						input.setDamageValue(0);
						this.inputItems.setStackInSlot(0, input.isEmpty() ? ItemStack.EMPTY : input);
					}
				}
				else {
					input.shrink(1);
					this.inputItems.setStackInSlot(0, input.isEmpty() ? ItemStack.EMPTY : input);
				}
				if(sound !=null)level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), sound, SoundSource.BLOCKS, 1.0F, 1.0F);	            
				level.setBlock(pos, state.setValue(BeehiveBlock.HONEY_LEVEL, Integer.valueOf(0)), 3);
			}
		}
	}
	
	@Override
	public void addOccupantWithPresetTicks(Entity p_58745_, boolean p_58746_, int p_58747_) {
		boolean update = false;
		if(this.getOccupantCount() < 3) {
			//Do original add occupent for the first 3 bees to save flower pos
			super.addOccupantWithPresetTicks(p_58745_, p_58746_, p_58747_);
			update = true;
		}
		else if (!this.isFull()) {
			p_58745_.stopRiding();
			p_58745_.ejectPassengers();
			CompoundTag compoundtag = new CompoundTag();
			p_58745_.save(compoundtag);
			this.storeBee(compoundtag, p_58747_, p_58746_);
			if (this.level != null) {
				BlockPos blockpos = this.getBlockPos();
				this.level.playSound((Player)null, (double)blockpos.getX(), (double)blockpos.getY(), (double)blockpos.getZ(), SoundEvents.BEEHIVE_ENTER, SoundSource.BLOCKS, 1.0F, 1.0F);
				this.level.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(p_58745_, this.getBlockState()));
			}

			p_58745_.discard();
			super.setChanged();
			update = true;
		}
		
		if(update) {
			BlockPos pos = getBlockPos();
			BlockState state = level.getBlockState(pos);
			level.sendBlockUpdated(pos, state, state, 3);
		}
	}
	
	@Override
	public void emptyAllLivingFromHive(@Nullable Player player, BlockState state, BeehiveBlockEntity.BeeReleaseStatus status) {
		final int oldBeeCount = getOccupantCount();
		super.emptyAllLivingFromHive(player, state, status);
		if(getOccupantCount() != oldBeeCount) {
			super.setChanged();
			BlockPos pos = getBlockPos();
			level.sendBlockUpdated(pos, state, state, 3);
		}
	}
	
	@Override
	public void setRemoved() {
		super.setRemoved();
		inputItemHandler.invalidate();
		outputItemHandler.invalidate();
		combinedAllItemHandler.invalidate();
	}
	
	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		if (tag.contains("Inventory.Input")) {
			inputItems.deserializeNBT(tag.getCompound("Inventory.Input"));
		}
		if (tag.contains("Inventory.Output")) {
			outputItems.deserializeNBT(tag.getCompound("Inventory.Output"));
		}
	}
	
	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.put("Inventory.Input", inputItems.serializeNBT());
		tag.put("Inventory.Output", outputItems.serializeNBT());		
	}
	
	public void writePacketNBT(CompoundTag tag) {
		saveAdditional(tag);
		tag.putInt("OccupantCount", getOccupantCount());
	}
	
	public void readPacketNBT(CompoundTag tag) {
		load(tag);
		this.clientBeeCount = tag.getInt("OccupantCount");
	}
	
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this, be -> {
			CompoundTag nbttagcompound = new CompoundTag();
			this.writePacketNBT(nbttagcompound);
			return nbttagcompound;
		});
	}

	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
		CompoundTag nonNullTag = pkt.getTag() != null ? pkt.getTag() : new CompoundTag();
		this.readPacketNBT(nonNullTag);
	}

	@Override
	public void handleUpdateTag(CompoundTag tag) {
		this.readPacketNBT(tag);
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag nbt = new CompoundTag();
		writePacketNBT(nbt);
		return nbt;
	}
	
	public boolean isItemValidInput(ItemStack stack, int slot) {
		if(!stack.isEmpty()) {
			return stack.is(Items.GLASS_BOTTLE) || stack.canPerformAction(ToolActions.SHEARS_HARVEST);
		}
		return false;
	}
	
	private ItemStackHandler createInputItemHandler() {
		return new ItemStackHandler(INPUT_SLOTS) {
			@Override
			protected void onContentsChanged(int slot) {
				AdvancedBeehiveBE.super.setChanged();
			}

			@Override
			public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
				return isItemValidInput(stack, slot);
			}

			@Nonnull
			@Override
			public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
				if (!isItemValidInput(stack, slot)) {
					return stack;
				}
				return super.insertItem(slot, stack, simulate);
			}
		};
	}
	
	private ItemStackHandler createOutputItemHandler() {
		return new ItemStackHandler(OUTPUT_SLOTS) {
			@Override
			protected void onContentsChanged(int slot) {
				setChanged();
			}

			@Nonnull
			@Override
			public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
				return stack;
			}
		};
	}
	
	@Nonnull
	private IItemHandler createCombinedAllItemHandler() {
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
		if(cap == ForgeCapabilities.ITEM_HANDLER) {
			if(side == null) {
				return combinedAllItemHandler.cast();
			}
			else if(side == Direction.UP) {
				return inputItemHandler.cast();
			}
			return outputItemHandler.cast();
		}
		return super.getCapability(cap, side);
	}
}
