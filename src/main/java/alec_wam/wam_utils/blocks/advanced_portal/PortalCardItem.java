package alec_wam.wam_utils.blocks.advanced_portal;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import alec_wam.wam_utils.utils.BlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class PortalCardItem extends Item {

	public static final String NBT_POS = "TeleportPos";
	public static final String NBT_PORTAL = "AdvancedPortalPos";
	
	public PortalCardItem(Properties props) {
		super(props);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if(player.isShiftKeyDown()) {
			if(stack.hasTag()) {
				CompoundTag tag = stack.getTag();
				if(tag.contains(NBT_POS)) {
					stack.removeTagKey(NBT_POS);
					return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
				}
				if(tag.contains(NBT_PORTAL)) {
					stack.removeTagKey(NBT_PORTAL);
					return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
				}
			}
			return InteractionResultHolder.pass(stack);
		}
		
		if(!stack.hasTag() || !stack.getTag().contains(NBT_POS) && !stack.getTag().contains(NBT_PORTAL)) {
			GlobalPos pos = GlobalPos.of(level.dimension(), player.getOnPos().above());
			stack.getOrCreateTag().put(NBT_POS, BlockUtils.writeGlobalPos(pos));
			return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
		}		
		return InteractionResultHolder.pass(stack);
	}
	
	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flags) {
		super.appendHoverText(stack, level, tooltip, flags);
		if(stack.hasTag() && stack.getTag().contains(NBT_POS)) {
			GlobalPos pos = getItemGlobalPos(stack, NBT_POS);
			if(pos !=null) {
				BlockPos blockPos = pos.pos();
				tooltip.add(Component.literal(blockPos.getX() + ", " + blockPos.getY() + ", " + blockPos.getZ()));
				tooltip.add(Component.literal(pos.dimension().location().toString()));
			}
		}
		
		if(stack.hasTag() && stack.getTag().contains(NBT_PORTAL)) {
			UUID portalUUID = stack.getTag().getUUID(NBT_PORTAL);
			if(portalUUID !=null) {
				tooltip.add(Component.literal("Portal:"));
				tooltip.add(Component.literal(portalUUID.toString()));
			}
		}
	}
	
	public static GlobalPos getItemGlobalPos(ItemStack stack, String tag) {
		if(stack.hasTag() && stack.getTag().contains(tag)) {
			return BlockUtils.loadGlobalPos(stack.getTag().getCompound(tag));
		}
		return null;
	}
}
