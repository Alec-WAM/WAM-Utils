package alec_wam.wam_utils.blocks.fishing_net;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import alec_wam.wam_utils.blocks.WAMUtilsBlockEntity;
import alec_wam.wam_utils.entities.WAMUtilsFakePlayer;
import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.utils.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

public class FishingNetBE extends WAMUtilsBlockEntity {

	public static final int FISH_SLOTS = 4;
	protected final ItemStackHandler fishItems = createFishItemHandler();
	public final LazyOptional<IItemHandler> externalFishItemHandler = LazyOptional.of(this::createExternalOutputItemHandler);
	public final LazyOptional<IItemHandler> combinedAllItemHandler = LazyOptional.of(this::createCombinedAllItemHandler);
	
	private int fishingTime = 0;
	private int maxFishingTime = 0;
	private float fishAngle;
	
	protected final RandomSource random = RandomSource.create();
	private ItemStack fishingRod;
	
	public FishingNetBE(BlockPos p_155229_, BlockState p_155230_) {
		super(BlockInit.FISHINGNET_BE.get(), p_155229_, p_155230_);
		fishingRod = new ItemStack(Items.FISHING_ROD);
	}

	@Override
	public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
		if (nbt.contains("Inventory.Fish")) {
			fishItems.deserializeNBT(nbt.getCompound("Inventory.Fish"));
		}
		this.fishingTime = nbt.getInt("FishingTime");	
		this.maxFishingTime = nbt.getInt("MaxFishingTime");		
	}

	@Override
	public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
		nbt.put("Inventory.Fish", fishItems.serializeNBT());
		nbt.putInt("FishingTime", fishingTime);
		nbt.putInt("MaxFishingTime", maxFishingTime);
	}
	
	public boolean hasFish() {
		return !InventoryUtils.isEmpty(fishItems);
	}

	public void dropFishItems() {
		boolean needsUpdate = false;
		for(int i = 0; i < this.fishItems.getSlots(); i++) {
			ItemStack stack = this.fishItems.getStackInSlot(i);
			if(!stack.isEmpty()) {
				double spawnX = this.worldPosition.getX() + 0.5D;
				double spawnY = this.worldPosition.getY() + 0.1D;
				double spawnZ = this.worldPosition.getZ() + 0.5D;
				Containers.dropItemStack(level, spawnX, spawnY, spawnZ, stack);
				this.fishItems.setStackInSlot(i, ItemStack.EMPTY);
				needsUpdate = true;
			}
		}
		if(needsUpdate) {
			markBlockForUpdate(null);
		}
	}
	
	public void tickServer() {
		ServerLevel serverlevel = (ServerLevel)this.level;
		
		//TODO Look into caching this
		if(InventoryUtils.isFull(fishItems)) {
			return;
		}
		
		FluidState fluidstate = level.getFluidState(worldPosition.below());
		
		if(!fluidstate.is(FluidTags.WATER) || !fluidstate.isSource()) {
			return;
		}
		
		if(this.maxFishingTime <= 0) {
			this.maxFishingTime = Mth.nextInt(this.random, 100, 600);
		}
		
		if(this.fishingTime < this.maxFishingTime) {
			
			int i = 1;
			BlockPos blockpos = worldPosition.above();
			if (this.random.nextFloat() < 0.25F && this.level.isRainingAt(blockpos)) {
				++i;
			}

			if (this.random.nextFloat() < 0.5F && !this.level.canSeeSky(blockpos)) {
				--i;
			}
			
			this.fishingTime += i;
			if(this.fishingTime < this.maxFishingTime) {
				int remainingTime = this.maxFishingTime - this.fishingTime;
				this.fishAngle += (float)this.random.triangle(0.0D, 9.188D);
	            float f = this.fishAngle * ((float)Math.PI / 180F);
	            float f1 = Mth.sin(f);
	            float f2 = Mth.cos(f);
	            double centerX = worldPosition.getX() + 0.5D;
	            double centerY = worldPosition.getY() - 1.0D;
	            double centerZ = worldPosition.getZ() + 0.5D;
	            double d0 = centerX + (double)(f1 * (float)remainingTime * 0.1F);
	            double d1 = (double)((float)Mth.floor(centerY) + 1.0F);
	            double d2 = centerZ + (double)(f2 * (float)remainingTime * 0.1F);
	            if (serverlevel.getBlockState(new BlockPos((int)d0, (int)d1 - 1, (int)d2)).getMaterial() == net.minecraft.world.level.material.Material.WATER) {
	               if (this.random.nextFloat() < 0.15F) {
	                  serverlevel.sendParticles(ParticleTypes.BUBBLE, d0, d1 - (double)0.1F, d2, 1, (double)f1, 0.1D, (double)f2, 0.0D);
	               }

	               float f3 = f1 * 0.04F;
	               float f4 = f2 * 0.04F;
	               serverlevel.sendParticles(ParticleTypes.FISHING, d0, d1, d2, 0, (double)f4, 0.01D, (double)(-f3), 1.0D);
	               serverlevel.sendParticles(ParticleTypes.FISHING, d0, d1, d2, 0, (double)(-f4), 0.01D, (double)f3, 1.0D);
	            }
			}
		}
		else {
			BlockPos fishingPosition = this.worldPosition.below();
			Vec3 fishingVec = new Vec3(fishingPosition.getX() + 0.5, fishingPosition.getY() + 0.5, fishingPosition.getZ() + 0.5);
			WAMUtilsFakePlayer player = WAMUtilsFakePlayer.get((ServerLevel)level, fishingVec.x, fishingVec.y, fishingVec.z).get();
			FishingHook hook = new FishingHook(player, level, 0, 0);
			hook.setPos(fishingVec);
			
			LootContext.Builder lootcontext$builder = 
					(new LootContext.Builder((ServerLevel)this.level))
					.withParameter(LootContextParams.ORIGIN, fishingVec)
					.withParameter(LootContextParams.TOOL, fishingRod)
					.withParameter(LootContextParams.THIS_ENTITY, hook)
					.withRandom(this.random)
					.withLuck((float)player.getLuck());
            lootcontext$builder.withParameter(LootContextParams.KILLER_ENTITY, player)
            .withParameter(LootContextParams.THIS_ENTITY, hook);
            LootTable loottable = this.level.getServer().getLootTables().get(BuiltInLootTables.FISHING);
            List<ItemStack> list = loottable.getRandomItems(lootcontext$builder.create(LootContextParamSets.FISHING));
            net.minecraftforge.event.entity.player.ItemFishedEvent event = new net.minecraftforge.event.entity.player.ItemFishedEvent(list, 1, hook);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
            if (event.isCanceled()) {
               return;
            }
            
            for(ItemStack stack : list) {
            	ItemStack remainder = stack.copy();
            	
            	//Intentionally don't stack items
        		for(int i = 0; i < fishItems.getSlots(); i++) {
            		ItemStack invStack = fishItems.getStackInSlot(i);
            		if(invStack.isEmpty()) {
            			fishItems.setStackInSlot(i, remainder);
            			remainder = ItemStack.EMPTY;
            			break;
            		}
            	}
            	
            	if(!remainder.isEmpty()) {
            		double spawnX = this.worldPosition.getX() + 0.5D;
            		double spawnY = this.worldPosition.getY() + 0.1D;
            		double spawnZ = this.worldPosition.getZ() + 0.5D;
            		Containers.dropItemStack(level, spawnX, spawnY, spawnZ, remainder);                    
            	}
            }
            
            if(!list.isEmpty()) {
            	level.playSound((Player)null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.BLOCKS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
                this.fishingTime = 0;
                //Add 5 Second Delay between "Casts"
                this.maxFishingTime = Mth.nextInt(this.random, 100, 600) + (5 * 20);
            }
		}
	}
	
	public void tickClient() {
		
	}
	
	@Override
	public void setRemoved() {
		super.setRemoved();
		externalFishItemHandler.invalidate();
		combinedAllItemHandler.invalidate();
	}

	@Nonnull
	private ItemStackHandler createFishItemHandler() {
		return new ItemStackHandler(FISH_SLOTS) {
			@Override
			protected void onContentsChanged(int slot) {
				setChanged();
				markBlockForUpdate(null);
			}

			@Nonnull
			@Override
			public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
				return stack;
			}
		};
	}

	@Nonnull
	protected IItemHandler createExternalOutputItemHandler() {
		return new CombinedInvWrapper(fishItems) {
			@NotNull
			@Override
			public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
				return stack;
			}
		};
	}
	
	@Nonnull
	private IItemHandler createCombinedAllItemHandler() {
		return new CombinedInvWrapper(fishItems) {
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
			} else if (side != Direction.DOWN) {
				return externalFishItemHandler.cast();
			} 
		} 
		return super.getCapability(cap, side);
	}

}
