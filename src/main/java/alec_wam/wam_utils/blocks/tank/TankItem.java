package alec_wam.wam_utils.blocks.tank;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import alec_wam.wam_utils.capabilities.ItemFluidStorage;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.data.loading.DatagenModLoader;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

public class TankItem extends BlockItem {

	public TankItem(Block block, Properties props) {
		super(block, props);
	}
	
	@Override
	public void initializeClient(@Nonnull Consumer<IClientItemExtensions> consumer)
	{
		if (!DatagenModLoader.isRunningDataGen())
			consumer.accept(TankItemRenderer.CLIENT_RENDERER);
	}

	@Override
	public ItemStack getCraftingRemainingItem(ItemStack stack) {
		if(stack.hasTag()) {
			if(stack.getTag().contains(ItemFluidStorage.NBT_FLUID)) {
				return new ItemStack(this);
			}
		}
		return ItemStack.EMPTY;
	}
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flags) {
    	if(stack.hasTag()) {
    		CompoundTag tag = stack.getTag();
    		boolean empty = true;
        	String maxFluidValue = String.format("%,d", TankBE.FLUID_CAPACITY);
    		if(tag.contains("Fluid")) {
    			FluidStack fluid = FluidStack.loadFluidStackFromNBT(tag.getCompound(ItemFluidStorage.NBT_FLUID));
    			if(fluid.isEmpty()) {
    				empty = true;
    			}
    			else {
    				empty = false;
    				
    				Component displayName = fluid.getDisplayName();
    				tooltip.add(displayName);
    				String fluidValue = String.format("%,d", fluid.getAmount());
    	        	tooltip.add(Component.literal(fluidValue + " / " + maxFluidValue + " mB"));
    			}
    			//tooltip.add(Component.literal(entityID.toString()));
    		}
    		if(empty) {
    			tooltip.add(Component.translatable("gui.wam_utils.tooltip.empty"));
	        	tooltip.add(Component.literal(0 + " / " + maxFluidValue + " mB"));
    		}
    	}
    }
    
    @Override
    public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> items) {
        super.fillItemCategory(group, items);
        if (allowedIn(group)) {
        	for (Fluid fluid : ForgeRegistries.FLUIDS.getValues()) {
        		//Only add sources
        		if (fluid.isSource(fluid.defaultFluidState())) {
        			ItemStack stack = new ItemStack(this);
        			CompoundTag tag = stack.getOrCreateTag();
        			FluidStack fluidStack = new FluidStack(fluid, TankBE.FLUID_CAPACITY);
        			tag.put(ItemFluidStorage.NBT_FLUID, fluidStack.writeToNBT(new CompoundTag()));
        			stack.setTag(tag);
        			items.add(stack);
        		}
        	}
        }
    }
	
	@Nullable
	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
		return new ItemFluidStorage(stack, TankBE.FLUID_CAPACITY);
	}

}
