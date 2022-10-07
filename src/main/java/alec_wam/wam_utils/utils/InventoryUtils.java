package alec_wam.wam_utils.utils;

import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import alec_wam.wam_utils.capabilities.BlockFluidStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

public class InventoryUtils {

	public static int findItem(IItemHandler inv, Item item) {
		for(int i = 0; i < inv.getSlots(); i++) {
			ItemStack stack = inv.getStackInSlot(i);
			if(!stack.isEmpty() && stack.getItem().equals(item)) {
				return i;
			}
		}
		return -1;
	}

	public static int findItem(IItemHandler inv, ItemStack item) {
		for(int i = 0; i < inv.getSlots(); i++) {
			ItemStack stack = inv.getStackInSlot(i);
			if(areItemsEqualIgnoreCount(stack, item, false)) {
				return i;
			}
		}
		return -1;
	}
	
	public static int countItem(IItemHandler inv, Item item) {
		int count = 0;
		for(int i = 0; i < inv.getSlots(); i++) {
			ItemStack stack = inv.getStackInSlot(i);
			if(!stack.isEmpty() && stack.getItem().equals(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}
	
	public static int countItem(IItemHandler inv, ItemStack item) {
		int count = 0;
		for(int i = 0; i < inv.getSlots(); i++) {
			ItemStack stack = inv.getStackInSlot(i);
			if(areItemsEqualIgnoreCount(stack, item, false)) {
				count += stack.getCount();
			}
		}
		return count;
	}
	
	public static int consumeItem(IItemHandler inv, ItemStack item, int count, boolean simualte) {
		int remaining = count;
		for(int i = 0; i < inv.getSlots(); i++) {
			ItemStack stack = inv.getStackInSlot(i);
			if(areItemsEqualIgnoreCount(stack, item, false)) {
				int realCount = Math.min(remaining, stack.getCount());
				remaining -= realCount;
				if(!simualte)stack.shrink(realCount);
				if(remaining <= 0) {
					break;
				}
			}
		}
		return remaining;
	}
	
	public static boolean areItemsEqualIgnoreCount(ItemStack stackA, ItemStack stackB, boolean limitTags)
    {
        if (stackA.isEmpty())
            return stackB.isEmpty();
        else
            return !stackB.isEmpty() && stackA.getItem() == stackB.getItem() &&
            (limitTags ? stackA.areShareTagsEqual(stackB) : ItemStack.tagMatches(stackA, stackB));
    }

	public static boolean isEmpty(IItemHandler handler) {
		for (int slot = 0; slot < handler.getSlots(); slot++)
        {
			ItemStack stack = handler.getStackInSlot(slot);
			if(!stack.isEmpty()) {
				return false;
			}
        }
		return true;		
	}
	
	public static boolean isFull(IItemHandler handler) {
		for (int slot = 0; slot < handler.getSlots(); slot++)
        {
			ItemStack stack = handler.getStackInSlot(slot);
			if(stack.isEmpty()) {
				return false;
			}
        }
		return true;		
	}
	
	public static  ItemStack putStackInInventoryAllSlots(IItemHandler handler, ItemStack stack)
    {
        for (int slot = 0; slot < handler.getSlots() && !stack.isEmpty(); slot++)
        {
            stack = insertStack(handler, stack, slot);
        }
        return stack;
    }

    /**
     * Copied from TileEntityHopper#insertStack and added capability support
     */
    public static ItemStack insertStack(IItemHandler handler, ItemStack stack, int slot)
    {
        ItemStack itemstack = handler.getStackInSlot(slot);

        if (handler.insertItem(slot, stack, true).isEmpty())
        {
            if (itemstack.isEmpty())
            {
            	handler.insertItem(slot, stack, false);
                stack = ItemStack.EMPTY;
            }
            else if (ItemHandlerHelper.canItemStacksStack(itemstack, stack))
            {
                stack = handler.insertItem(slot, stack, false);
            }
        }

        return stack;
    }
    
    public static ItemStack simulateForceStackInInventoryAllSlots(ItemStackHandler handler, ItemStack stack)
    {
        for (int slot = 0; slot < handler.getSlots() && !stack.isEmpty(); slot++)
        {
            stack = forceInsertItem(handler, stack, slot, true, null);
        }
        return stack;
    }
    
    public static ItemStack forceStackInInventoryAllSlots(ItemStackHandler handler, ItemStack stack, Consumer<Integer> onChange)
    {
        for (int slot = 0; slot < handler.getSlots() && !stack.isEmpty(); slot++)
        {
            stack = forcheInsertStack(handler, stack, slot, onChange);
        }
        return stack;
    }
    
    public static ItemStack forcheInsertStack(ItemStackHandler handler, ItemStack stack, int slot, Consumer<Integer> onChange)
    {
        ItemStack itemstack = handler.getStackInSlot(slot);

        if (forceInsertItem(handler, stack, slot, true, onChange).isEmpty())
        {
            if (itemstack.isEmpty())
            {
            	forceInsertItem(handler, stack, slot, false, onChange);
                stack = ItemStack.EMPTY;
            }
            else if (ItemHandlerHelper.canItemStacksStack(itemstack, stack))
            {
                stack = forceInsertItem(handler, stack, slot, false, onChange);
            }
        }

        return stack;
    }
    
    public static ItemStack forceInsertItem(ItemStackHandler handler, ItemStack stack, int slot, boolean simulate, @Nullable Consumer<Integer> onChange)
    {
    	if (stack.isEmpty())
            return ItemStack.EMPTY;

        if (!handler.isItemValid(slot, stack))
            return stack;

        ItemStack existing = handler.getStackInSlot(slot);

        int limit = Math.min(handler.getSlotLimit(slot), stack.getMaxStackSize());

        if (!existing.isEmpty())
        {
            if (!ItemHandlerHelper.canItemStacksStack(stack, existing))
                return stack;

            limit -= existing.getCount();
        }

        if (limit <= 0)
            return stack;

        boolean reachedLimit = stack.getCount() > limit;

        if (!simulate)
        {
            if (existing.isEmpty())
            {
                handler.setStackInSlot(slot, reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, limit) : stack);
            }
            else
            {
                existing.grow(reachedLimit ? limit : stack.getCount());
            }
            if(onChange !=null)onChange.accept(slot);
        }

        return reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, stack.getCount()- limit) : ItemStack.EMPTY;
    }

	public static void givePlayerItem(Player player, ItemStack stack) {
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false);
		}
		/*ItemStack remainder = putStackInInventoryAllSlots(new InvWrapper(player.getInventory()), stack);
		if(!remainder.isEmpty()) {
			double x = player.getX() + 0.5D;
			double y = player.getY() + 0.5D;
			double z = player.getZ() + 0.5D;
			ItemEntity itemEntity = new ItemEntity(player.level, x, y, z, stack);
			itemEntity.setNoPickUpDelay();
			player.level.addFreshEntity(itemEntity);
		}*/
	}
	
	//FLUID
	@SuppressWarnings("deprecation")
	public static boolean handleTankInteraction(Player player, InteractionHand hand, ItemStack stack, BlockFluidStorage tank, Level level, BlockPos pos) {
		ItemStack copyStack = stack.copy();
        copyStack.setCount(1);
        
        Optional<IFluidHandlerItem> fluidHandlerItem = FluidUtil.getFluidHandler(copyStack).resolve();
        if (fluidHandlerItem.isPresent()) {
            IFluidHandlerItem handler = fluidHandlerItem.get();
            FluidStack fluidInItem;
            if (tank.isEmpty()) {
                //If we don't have a fluid stored try draining in general
                fluidInItem = handler.drain(Integer.MAX_VALUE, FluidAction.SIMULATE);
            } else {
                //Otherwise, try draining the same type of fluid we have stored
                // We do this to better support multiple tanks in case the fluid we have stored we could pull out of a block's
                // second tank but just asking to drain a specific amount
                fluidInItem = handler.drain(new FluidStack(tank.getFluid(), Integer.MAX_VALUE), FluidAction.SIMULATE);
            }
            if (fluidInItem.isEmpty()) {
                if (!tank.isEmpty()) {
                    int filled = handler.fill(tank.getFluid(), player.isCreative() ? FluidAction.SIMULATE : FluidAction.EXECUTE);
                    ItemStack container = handler.getContainer();
                    if (filled > 0) {
                        if (stack.getCount() == 1) {
                            player.setItemInHand(hand, container);
                        } else if (stack.getCount() > 1 && player.getInventory().add(container)) {
                        	stack.shrink(1);
                        } else {
                            player.drop(container, false, true);
                            stack.shrink(1);
                        }
                        
                        SoundEvent soundevent = tank.getFluid().getFluid().getFluidType().getSound(player, level, pos, net.minecraftforge.common.SoundActions.BUCKET_FILL);
                        if(soundevent == null) soundevent = tank.getFluid().getFluid().is(FluidTags.LAVA) ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL;
                        level.playSound((Player)null, pos, soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
                        
                        tank.forceDrain(filled, FluidAction.EXECUTE);
                        return true;
                    }
                }
            } else {
            	int testFillAmount = tank.fill(fluidInItem, FluidAction.SIMULATE);            	
                int storedAmount = fluidInItem.getAmount();
                int remainder = storedAmount - testFillAmount;
                if (remainder < storedAmount) {                	
                    boolean filled = false;
                    FluidStack drained = handler.drain(new FluidStack(fluidInItem, storedAmount - remainder), player.isCreative() ? FluidAction.SIMULATE : FluidAction.EXECUTE);
                    if (!drained.isEmpty()) {
                        ItemStack container = handler.getContainer();
                        if (player.isCreative()) {
                            filled = true;
                        } else if (!container.isEmpty()) {
                            if (stack.getCount() == 1) {
                                player.setItemInHand(hand, container);
                                filled = true;
                            } else if (player.getInventory().add(container)) {
                            	stack.shrink(1);
                                filled = true;
                            }
                        } else {
                        	stack.shrink(1);
                            if (stack.isEmpty()) {
                                player.setItemInHand(hand, ItemStack.EMPTY);
                            }
                            filled = true;
                        }
                        if (filled) {
                        	SoundEvent soundevent = drained.getFluid().getFluidType().getSound(player, level, pos, net.minecraftforge.common.SoundActions.BUCKET_EMPTY);
                            if(soundevent == null) soundevent = drained.getFluid().is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
                            level.playSound((Player)null, pos, soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
                        	
                            tank.fill(fluidInItem, FluidAction.EXECUTE);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
	
	@SuppressWarnings("deprecation")
	public static boolean handleFillTankInteraction(Player player, InteractionHand hand, ItemStack stack, BlockFluidStorage tank, Level level, BlockPos pos) {
		ItemStack copyStack = stack.copy();
        copyStack.setCount(1);
        
        Optional<IFluidHandlerItem> fluidHandlerItem = FluidUtil.getFluidHandler(copyStack).resolve();
        if (fluidHandlerItem.isPresent()) {
            IFluidHandlerItem handler = fluidHandlerItem.get();
            FluidStack fluidInItem;
            if (tank.isEmpty()) {
                //If we don't have a fluid stored try draining in general
                fluidInItem = handler.drain(Integer.MAX_VALUE, FluidAction.SIMULATE);
            } else {
                //Otherwise, try draining the same type of fluid we have stored
                // We do this to better support multiple tanks in case the fluid we have stored we could pull out of a block's
                // second tank but just asking to drain a specific amount
                fluidInItem = handler.drain(new FluidStack(tank.getFluid(), Integer.MAX_VALUE), FluidAction.SIMULATE);
            }
            
            if (!fluidInItem.isEmpty()) {                
            	int testFillAmount = tank.fill(fluidInItem, FluidAction.SIMULATE);            	
                int storedAmount = fluidInItem.getAmount();
                int remainder = storedAmount - testFillAmount;
                if (remainder < storedAmount) {                	
                    boolean filled = false;
                    FluidStack drained = handler.drain(new FluidStack(fluidInItem, storedAmount - remainder), player.isCreative() ? FluidAction.SIMULATE : FluidAction.EXECUTE);
                    if (!drained.isEmpty()) {
                        ItemStack container = handler.getContainer();
                        if (player.isCreative()) {
                            filled = true;
                        } else if (!container.isEmpty()) {
                            if (stack.getCount() == 1) {
                                player.setItemInHand(hand, container);
                                filled = true;
                            } else if (player.getInventory().add(container)) {
                            	stack.shrink(1);
                                filled = true;
                            }
                        } else {
                        	stack.shrink(1);
                            if (stack.isEmpty()) {
                                player.setItemInHand(hand, ItemStack.EMPTY);
                            }
                            filled = true;
                        }
                        if (filled) {
                        	SoundEvent soundevent = drained.getFluid().getFluidType().getSound(player, level, pos, net.minecraftforge.common.SoundActions.BUCKET_EMPTY);
                            if(soundevent == null) soundevent = drained.getFluid().is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
                            level.playSound((Player)null, pos, soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
                        	
                            tank.fill(fluidInItem, FluidAction.EXECUTE);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
	
}
