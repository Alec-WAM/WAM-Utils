package alec_wam.wam_utils.entities.minecarts;

import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import alec_wam.wam_utils.entities.chest_boats.EnderChestBoat.EnderChestContainerWrapper;
import alec_wam.wam_utils.init.EntityInit;
import alec_wam.wam_utils.init.ItemInit;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class EnderChestMinecart extends AbstractMinecart implements HasCustomInventoryScreen, MenuProvider {
	private static final Component CONTAINER_TITLE = Component.translatable("container.enderchest");
	
	public EnderChestMinecart(EntityType<? extends AbstractMinecart> p_219869_, Level p_219870_) {
		super(p_219869_, p_219870_);
	}

	public EnderChestMinecart(Level p_219872_, double p_219873_, double p_219874_, double p_219875_) {
		this(EntityInit.ENDER_CHEST_MINECART.get(), p_219872_);
		this.setPos(p_219873_, p_219874_, p_219875_);
		this.xo = p_219873_;
		this.yo = p_219874_;
		this.zo = p_219875_;
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
	public InteractionResult interact(Player p_219898_, InteractionHand p_219899_) {
		return this.interactWithChestVehicle(this::gameEvent, p_219898_);
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

	@Override
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
	
	@Nullable
	public AbstractContainerMenu createMenu(int p_219910_, Inventory p_219911_, Player p_219912_) {
		EnderChestContainerWrapper wrapper = new EnderChestContainerWrapper(p_219912_, this);
		return ChestMenu.threeRows(p_219910_, p_219911_, wrapper);
	}
	
	@Override
	public Component getDisplayName() {
		return CONTAINER_TITLE;
	}

	@Override
	protected Item getDropItem() {
		return ItemInit.ENDERCHEST_MINECART.get();
	}
	
	@Override
	public BlockState getDefaultDisplayBlockState() {
		return Blocks.ENDER_CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH);
	}

	@Override
	public int getDefaultDisplayOffset() {
		return 8;
	}

	@Override
	public Type getMinecartType() {
		return Type.CHEST;
	}

}
