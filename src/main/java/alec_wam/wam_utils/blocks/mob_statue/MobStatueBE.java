package alec_wam.wam_utils.blocks.mob_statue;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import alec_wam.wam_utils.blocks.INBTItemDrop;
import alec_wam.wam_utils.blocks.WAMUtilsBlockEntity;
import alec_wam.wam_utils.init.BlockInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

public class MobStatueBE extends WAMUtilsBlockEntity implements INBTItemDrop {

	public static final int DISPLAY_ITEM_SLOTS = 6; //Hands + Armor
	
	private LivingEntity displayEntity;
	private int rotation = 0;
	private float scale = 1.0F;
	
	protected final ItemStackHandler displayItems = createDisplayItemHandler();
	public final LazyOptional<IItemHandler> displayItemHandler = LazyOptional.of(() -> displayItems);
	public final LazyOptional<IItemHandler> externalItemHandler = LazyOptional.of(this::createExternalItemHandler);
	
	
	public MobStatueBE(BlockPos p_155229_, BlockState p_155230_) {
		super(BlockInit.MOB_STATUE_BE.get(), p_155229_, p_155230_);
	}

	public int getRotation() {
		return rotation;
	}

	public void setRotation(int rotation) {
		this.rotation = rotation;
	}

	public float getScale() {
		return scale;
	}

	public void setScale(float scale) {
		this.scale = scale;
	}

	public LivingEntity getDisplayEntity() {
		return displayEntity;
	}

	@Override
	public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
		if (nbt.contains("Inventory")) {
			displayItems.deserializeNBT(nbt.getCompound("Inventory"));
		}
		this.rotation = nbt.getInt("Display.Rotation");
		this.scale = nbt.getFloat("Display.Scale");
		//if(descPacket) {
			if(nbt.contains("DisplayEntity")) {
				loadDisplayEntity(nbt.getCompound("DisplayEntity"));
			}
		//}
	}
	
	@SuppressWarnings("deprecation")
	public void setEntityFromStack(ItemStack stack) {
		if(stack.getItem() instanceof SpawnEggItem) {
			EntityType<?> entitytype = ((SpawnEggItem) stack.getItem()).getType(stack.getTag());
			
			CompoundTag tag = null;
			CompoundTag stackTag = stack.getTag() !=null ? stack.getTag().copy() : new CompoundTag();
			if(!stackTag.contains("EntityTag", 10)) {
				CompoundTag entityTag = new CompoundTag();
				entityTag.putString("id", Registry.ENTITY_TYPE.getKey(entitytype).toString());
				tag = entityTag;
			}
			else {
				tag = stackTag.getCompound("EntityTag");					
			}	
			
			Entity entity = EntityType.loadEntityRecursive(tag, level, (p_151310_) -> {
				// p_151310_.moveTo(d0, d1, d2, p_151310_.getYRot(), p_151310_.getXRot());
				return p_151310_;
			});
			if (entity !=null && entity instanceof LivingEntity) {
				this.displayEntity = (LivingEntity) entity;
				BlockPos pos = worldPosition;
				this.displayEntity.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
				updateEntityDisplayItems();
				this.markBlockForUpdate(null);
			}
		}
	}
	
	public static final String TAG_ENTITY_DATA = "EntityData";
	public static ItemStack createStatueItem(EntityType<?> entityType) {
		ItemStack stack = new ItemStack(BlockInit.MOB_STATUE_BLOCK.get(), 1);
		CompoundTag tag = new CompoundTag();
		ResourceLocation id = EntityType.getKey(entityType);
		CompoundTag entityData = new CompoundTag();
		entityData.putString("id", id.toString());
		tag.put(TAG_ENTITY_DATA, entityData);
		stack.setTag(tag);
		return stack;
	}
	
	@Override
	public ItemStack getNBTDrop(Item item) {
		ItemStack stack = new ItemStack(BlockInit.MOB_STATUE_BLOCK.get(), 1);
		if(this.displayEntity != null) {
			EntityType<?> type = this.displayEntity.getType();
			CompoundTag tag = new CompoundTag();
			ResourceLocation id = EntityType.getKey(type);
			CompoundTag entityData = new CompoundTag();
			entityData.putString("id", id.toString());
			tag.put(TAG_ENTITY_DATA, entityData);
			stack.setTag(tag);
		}
		return stack;
	}
	
	public static Optional<EntityType<?>> loadTypeFromStatueItem(ItemStack stack){
		CompoundTag tag = stack.getTag();
		if(tag != null) {
			CompoundTag entityData = tag.getCompound(TAG_ENTITY_DATA);
			return EntityType.by(entityData);
		}
		return Optional.empty();
	}
	
	@Override
	public void readFromItem(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		if(tag != null) {
			CompoundTag entityData = tag.getCompound(TAG_ENTITY_DATA);
			Entity entity = EntityType.loadEntityRecursive(entityData, level, (p_151310_) -> {
				// p_151310_.moveTo(d0, d1, d2, p_151310_.getYRot(), p_151310_.getXRot());
				return p_151310_;
			});
			if (entity !=null && entity instanceof LivingEntity) {
				this.displayEntity = (LivingEntity) entity;
				BlockPos pos = worldPosition;
				this.displayEntity.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
				updateEntityDisplayItems();
				this.markBlockForUpdate(null);
			}
		}
	}

	public void loadDisplayEntity(CompoundTag tag) {
		this.displayEntity = null;
		Optional<EntityType<?>> optional = EntityType.by(tag);
        if(optional.isPresent()) {        	
        	Entity entity = EntityType.loadEntityRecursive(tag, level, (p_151310_) -> {
        		return p_151310_;
        	});
    		if(entity instanceof LivingEntity) {
    			this.displayEntity = (LivingEntity)entity;
				BlockPos pos = worldPosition;
				this.displayEntity.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
				this.markBlockForUpdate(null);
    		}
        }
	}

	@Override
	public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
		nbt.put("Inventory", displayItems.serializeNBT());
		nbt.putInt("Display.Rotation", this.rotation);
		nbt.putFloat("Display.Scale", this.scale);
		//if(descPacket) {
			if(displayEntity != null) {
				nbt.put("DisplayEntity", displayEntity.serializeNBT());
				//TODO Maybe update display items after load?
			}
		//}
	}

	@Override
	public void receiveMessageFromClient(CompoundTag nbt) {
		if(nbt.contains("Scale")) {
			this.setScale((float)nbt.getDouble("Scale"));
			this.setChanged();
			this.markBlockForUpdate(null);
		}
		if(nbt.contains("Rotation")) {
			this.setRotation((int)nbt.getDouble("Rotation"));
			this.setChanged();
			this.markBlockForUpdate(null);
		}
	}
	
	public boolean isValidDisplayItem(int slot, ItemStack stack) {
		EquipmentSlot equipmentType = getEquipmentTypeBySlot(slot);
		if(equipmentType != null) {
			if(equipmentType.getType() == EquipmentSlot.Type.HAND) {
				return true;
			}
			else if(equipmentType.getType() == EquipmentSlot.Type.ARMOR) {
				if(!stack.isEmpty()) {
					EquipmentSlot itemSlot = LivingEntity.getEquipmentSlotForItem(stack);
					boolean sameSlot = itemSlot == equipmentType;
					if(sameSlot) {
						if(displayEntity !=null) {
							return stack.canEquip(equipmentType, displayEntity);
						}
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public int getSlotIndex(EquipmentSlot slot) {
		int slotIndex = -1;
		if(slot == EquipmentSlot.MAINHAND) {
			slotIndex = 0;
		}
		else if(slot == EquipmentSlot.OFFHAND) {
			slotIndex = 1;
		}
		else if(slot == EquipmentSlot.HEAD) {
			slotIndex = 2;
		}
		else if(slot == EquipmentSlot.CHEST) {
			slotIndex = 3;
		}
		else if(slot == EquipmentSlot.LEGS) {
			slotIndex = 4;
		}
		else if(slot == EquipmentSlot.FEET) {
			slotIndex = 5;
		}
		return slotIndex;
	}
	
	public EquipmentSlot getEquipmentTypeBySlot(int slot) {
		if(slot == 0) {
			return EquipmentSlot.MAINHAND;
		}
		else if(slot == 1) {
			return EquipmentSlot.OFFHAND;
		}
		else if(slot == 2) {
			return EquipmentSlot.HEAD;
		}
		else if(slot == 3) {
			return EquipmentSlot.CHEST;
		}
		else if(slot == 4) {
			return EquipmentSlot.LEGS;
		}
		else if(slot == 5) {
			return EquipmentSlot.FEET;
		}
		return null;
	}
	
	public ItemStack getEquipment(EquipmentSlot slot) {
		int slotIndex = getSlotIndex(slot);
		if(slotIndex >= 0) {
			return this.displayItems.getStackInSlot(slotIndex);
		}
		return ItemStack.EMPTY;
	}

	public void updateEntityDisplayItems() {
		//TODO Handle onEquip
		boolean update = false;
		if(this.displayEntity !=null) {
			for(EquipmentSlot slot : EquipmentSlot.values()) {
				ItemStack stack = getEquipment(slot);
				if(stack.canEquip(slot, displayEntity)) {
					this.displayEntity.setItemSlot(slot, stack);
					update = true;
				}
				else {
					ItemStack other = displayEntity.getItemBySlot(slot);
					if(!other.isEmpty()) {
						this.displayEntity.setItemSlot(slot, ItemStack.EMPTY);		
						update = true;
					}
				}
			}
		}
		
		if(update) {
			this.setChanged();
			this.markBlockForUpdate(null);
		}
		//p_39708_.onEquipItem(equipmentslot, itemstack, p_219985_);
	}
	
	@Override
	public void setRemoved() {
		super.setRemoved();
		displayItemHandler.invalidate();
		externalItemHandler.invalidate();
	}
	
	protected ItemStackHandler createDisplayItemHandler() {
		return new ItemStackHandler(DISPLAY_ITEM_SLOTS) {
			@Override
			protected void onContentsChanged(int slot) {
				updateEntityDisplayItems();
				setChanged();
			}

			@Override
			public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
				return isValidDisplayItem(slot, stack);
			}

			@Nonnull
			@Override
			public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
				if (!isValidDisplayItem(slot, stack)) {
					return stack;
				}
				return super.insertItem(slot, stack, simulate);
			}
		};
	}
	
	@Nonnull
	protected IItemHandler createExternalItemHandler() {
		return new CombinedInvWrapper(displayItems) {
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
				return externalItemHandler.cast();
			} 
			else if (side != Direction.UP) {
				return displayItemHandler.cast();
			} 
		} 
		return super.getCapability(cap, side);
	}

	public void tickServer() {
		if(!level.hasNeighborSignal(worldPosition)) {
			return;
		}
		
		if(this.displayEntity !=null) {
			this.displayEntity.tickCount++;
			
			if(!this.displayEntity.isSilent()) {
				this.displayEntity.setSilent(true);
			}
			
			//this.displayEntity.aiStep();
		}
	}

	public void tickClient() {
		if(!level.hasNeighborSignal(worldPosition)) {
			return;
		}
		
		if(this.displayEntity !=null) {
			this.displayEntity.tickCount++;
			if(!this.displayEntity.isSilent()) {
				this.displayEntity.setSilent(true);
			}
			//this.displayEntity.aiStep();
		}
	}
	
	@Override
	public AABB getRenderBoundingBox() {
		AABB bb = new AABB(worldPosition, worldPosition);
		if(this.displayEntity !=null) {
			AABB entityBoundingBox = this.displayEntity.getBoundingBox().move(worldPosition);
			return bb.minmax(entityBoundingBox);
		}
		return bb;
	}

}
