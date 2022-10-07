package alec_wam.wam_utils.entities.chest_boats;

import java.util.List;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import alec_wam.wam_utils.init.EntityInit;
import alec_wam.wam_utils.init.ItemInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class EnderChestBoat extends Boat implements HasCustomInventoryScreen, MenuProvider {
	private static final Component CONTAINER_TITLE = Component.translatable("container.enderchest");
	
	public EnderChestBoat(EntityType<? extends Boat> p_219869_, Level p_219870_) {
		super(p_219869_, p_219870_);
	}

	public EnderChestBoat(Level p_219872_, double p_219873_, double p_219874_, double p_219875_) {
		this(EntityInit.ENDER_CHEST_BOAT.get(), p_219872_);
		this.setPos(p_219873_, p_219874_, p_219875_);
		this.xo = p_219873_;
		this.yo = p_219874_;
		this.zo = p_219875_;
	}

	@Override
	protected float getSinglePassengerXOffset() {
		return 0.15F;
	}

	@Override
	protected int getMaxPassengers() {
		return 1;
	}

	@Override
	public void tick() {
		super.tick();
//		if(level.isClientSide) {
//			if(level.random.nextInt(10) < 2) {
//				for(int i = 0; i < 3; ++i) {
//					int j = level.random.nextInt(2) * 2 - 1;
//					int k = level.random.nextInt(2) * 2 - 1;
//					double d0 = (double)getX() + 0.5D + 0.25D * (double)j;
//					double d1 = (double)((float)getY() + level.random.nextFloat());
//					double d2 = (double)getZ() + 0.5D + 0.25D * (double)k;
//					double d3 = (double)(level.random.nextFloat() * (float)j);
//					double d4 = ((double)level.random.nextFloat() - 0.5D) * 0.125D;
//					double d5 = (double)(level.random.nextFloat() * (float)k);
//					level.addParticle(ParticleTypes.PORTAL, d0, d1, d2, d3, d4, d5);
//				}
//			}
//		}
	}
	
	@Override
	public void destroy(DamageSource p_219892_) {
		super.destroy(p_219892_);
		this.chestVehicleDestroyed(p_219892_, this.level, this);
	}

	private void chestVehicleDestroyed(DamageSource p_219928_, Level p_219929_, Entity p_219930_) {
		if (p_219929_.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
			if (!p_219929_.isClientSide) {
				Entity entity = p_219928_.getDirectEntity();
				if (entity != null && entity.getType() == EntityType.PLAYER) {
					PiglinAi.angerNearbyPiglins((Player) entity, true);
				}
			}

		}
	}

	@Override
	public void remove(Entity.RemovalReason p_219894_) {
		super.remove(p_219894_);
	}

	@Override
	public InteractionResult interact(Player p_219898_, InteractionHand p_219899_) {
		return this.canAddPassenger(p_219898_) && !p_219898_.isSecondaryUseActive()
				? super.interact(p_219898_, p_219899_)
				: this.interactWithChestVehicle(this::gameEvent, p_219898_);
	}
	
	private InteractionResult interactWithChestVehicle(BiConsumer<GameEvent, Entity> p_219932_, Player p_219933_) {
		p_219933_.openMenu(this);
		if (!p_219933_.level.isClientSide) {
			p_219932_.accept(GameEvent.CONTAINER_OPEN, p_219933_);
			PiglinAi.angerNearbyPiglins(p_219933_, true);
			return InteractionResult.CONSUME;
		} else {
			return InteractionResult.SUCCESS;
		}
	}

	public void openCustomInventoryScreen(Player p_219906_) {
		p_219906_.openMenu(new SimpleMenuProvider((p_53124_, p_53125_, p_53126_) -> {
            return this.createMenu(p_53124_, p_53125_, p_219906_);
         }, CONTAINER_TITLE));
		if (!p_219906_.level.isClientSide) {
			p_219906_.awardStat(Stats.OPEN_ENDERCHEST);
			this.gameEvent(GameEvent.CONTAINER_OPEN, p_219906_);
			PiglinAi.angerNearbyPiglins(p_219906_, true);
		}
	}

	@Override
	public Item getDropItem() {
		Item item;
		switch (this.getBoatType()) {
		case SPRUCE:
			item = ItemInit.SPRUCE_ENDERCHEST_BOAT.get();
			break;
		case BIRCH:
			item = ItemInit.BIRCH_ENDERCHEST_BOAT.get();
			break;
		case JUNGLE:
			item = ItemInit.JUNGLE_ENDERCHEST_BOAT.get();
			break;
		case ACACIA:
			item = ItemInit.ACACIA_ENDERCHEST_BOAT.get();
			break;
		case DARK_OAK:
			item = ItemInit.DARK_OAK_ENDERCHEST_BOAT.get();
			break;
		case MANGROVE:
			item = ItemInit.MANGROVE_ENDERCHEST_BOAT.get();
			break;
		default:
			item = ItemInit.OAK_ENDERCHEST_BOAT.get();
		}

		return item;
	}

	public boolean isChestVehicleStillValid(Player p_219955_) {
		return !this.isRemoved() && this.position().closerThan(p_219955_.position(), 8.0D);
	}
	
	@Nullable
	public AbstractContainerMenu createMenu(int p_219910_, Inventory p_219911_, Player p_219912_) {
		EnderChestContainerWrapper wrapper = new EnderChestContainerWrapper(p_219912_, this);
		return ChestMenu.threeRows(p_219910_, p_219911_, wrapper);
	}
	
	@Override
	public Component getDisplayName() {
		return CONTAINER_TITLE;
	}
	
	public class EnderChestContainerWrapper extends SimpleContainer {
		private Player player;
		private PlayerEnderChestContainer enderChest;
		private EnderChestBoat boat;
		public EnderChestContainerWrapper(Player player, EnderChestBoat boat) {
			super(27);
			this.player = player;
			this.boat = boat;
			this.enderChest = player.getEnderChestInventory();
		}
		
		@Override
		public ItemStack getItem(int p_19157_) {
			return enderChest.getItem(p_19157_);
		}
		
		@Override
		public List<ItemStack> removeAllItems() {
			return this.enderChest.removeAllItems();
		}
		
		@Override
		public ItemStack removeItem(int p_19159_, int p_19160_) {
			return this.enderChest.removeItem(p_19159_, p_19160_);
		}
		
		@Override
		public ItemStack removeItemType(Item p_19171_, int p_19172_) {
			return this.enderChest.removeItemType(p_19171_, p_19172_);
		}
		
		@Override
		public ItemStack addItem(ItemStack p_19174_) {
			return this.enderChest.addItem(p_19174_);
		}
		
		@Override
		public boolean canAddItem(ItemStack p_19184_) {
			return this.enderChest.canAddItem(p_19184_);
		}
		
		@Override
		public ItemStack removeItemNoUpdate(int p_19180_) {
			return this.enderChest.removeItemNoUpdate(p_19180_);
		}
		
		@Override
		public void setItem(int p_19162_, ItemStack p_19163_) {
			this.enderChest.setItem(p_19162_, p_19163_);
		}
		
		@Override
		public int getContainerSize() {
			return this.enderChest.getContainerSize();
		}
		
		@Override
		public boolean isEmpty() {
			return this.enderChest.isEmpty();
		}
		
		@Override
		public void setChanged() {
			this.enderChest.setChanged();
		}
		
		@Override
		public boolean stillValid(Player p_19167_) {
			return boat.isChestVehicleStillValid(p_19167_);
		}
		
		@Override
		public void clearContent() {
			this.enderChest.clearContent();
		}
		
		@Override
		public void fillStackedContents(StackedContents p_19169_) {
			this.enderChest.fillStackedContents(p_19169_);
		}
		
		@Override
		public String toString() {
			return this.enderChest.toString();
		}
		
		@Override
		public void fromTag(ListTag p_19178_) {
			this.enderChest.fromTag(p_19178_);
		}
		
		@Override
		public ListTag createTag() {
			return this.enderChest.createTag();
		}
	}
}
