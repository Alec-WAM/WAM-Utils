package alec_wam.wam_utils.blocks.machine.stone_factory;

import java.util.List;

import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.init.ContainerInit;
import alec_wam.wam_utils.init.RecipeInit;
import alec_wam.wam_utils.recipe.StoneFactoryRecipe;
import alec_wam.wam_utils.server.container.GenericContainerData;
import alec_wam.wam_utils.server.container.WAMContainerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

public class StoneFactoryContainer extends WAMContainerMenu {

	public StoneFactoryBE blockEntity;
    private Player playerEntity;
    private IItemHandler playerInventory;
    private List<StoneFactoryRecipe> recipeCache;
	
	public StoneFactoryContainer(int windowId, BlockPos pos, Inventory playerInventory, Player player) {
		super(ContainerInit.STONE_FACTORY_CONTAINER.get(), windowId);
		blockEntity = (StoneFactoryBE) player.getCommandSenderWorld().getBlockEntity(pos);
        this.playerEntity = player;
        this.playerInventory = new InvWrapper(playerInventory);

        recipeCache = this.playerEntity.level.getRecipeManager().getAllRecipesFor(RecipeInit.STONE_FACTORY_TYPE.get());
        
        if (blockEntity != null) {
            addSlot(new SlotItemHandler(blockEntity.upgradeItems, 0, 152, 33));
            addSlot(new SlotItemHandler(blockEntity.outputItems, 0, 125, 32));
            trackFluidAndEnergyProgress();
        }
        layoutPlayerInventorySlots(this.playerInventory, 8, 84);
	}
	
	private void trackFluidAndEnergyProgress() {
		addGenericData(GenericContainerData.int32(this.blockEntity::getCraftingTime, this.blockEntity::setCraftingTime));
		addGenericData(GenericContainerData.int32(this.blockEntity::getMaxCraftingTime, this.blockEntity::setMaxCraftingTime));		
		addGenericData(GenericContainerData.energy(this.blockEntity.energyStorage));
		addGenericData(GenericContainerData.fluid(this.blockEntity.fluidStorageWater));
		addGenericData(GenericContainerData.fluid(this.blockEntity.fluidStorageLava));
    }

	@Override
    public boolean stillValid(Player playerIn) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), playerEntity, BlockInit.STONE_FACTORY_BLOCK.get());
    }

	public List<StoneFactoryRecipe> getAllRecipes(){
		return recipeCache;
	}
	
	public boolean isValidIndex(int index) {
		return index >=0 && index < getAllRecipes().size();
	}
	
	public StoneFactoryRecipe getCurrentRecipe() {
		return this.blockEntity.getSelectedRecipe();
	}
	
	public int getCurrentRecipeIndex() {
		return getCurrentRecipe() == null ? -1 : getAllRecipes().indexOf(getCurrentRecipe());
	}
	
	public StoneFactoryRecipe getPrevRecipe() {
		int index = getCurrentRecipeIndex();
		index--;
		if(!isValidIndex(index)) {
			index = getAllRecipes().size() - 1;
		}
		return getAllRecipes().get(index);
	}
	
	public StoneFactoryRecipe getNextRecipe() {
		int index = getCurrentRecipeIndex();
		index++;
		if(!isValidIndex(index)) {
			index = 0;
		}
		return getAllRecipes().get(index);
	}
	
    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if(index < 2) {
            	//Chest Area
            	if(!this.moveItemStackTo(stack, 29, 38, false)) {
	            	if (!this.moveItemStackTo(stack, 2, 30, false)) {
	                  return ItemStack.EMPTY;
	            	}
            	}
            }
            else {
            	if (!this.moveItemStackTo(stack, 0, 1, false)) {
            		if (index < 29 && !this.moveItemStackTo(stack, 29, 38, false)) {
                      return ItemStack.EMPTY;
            		}
            		else if (!this.moveItemStackTo(stack, 2, 29, false)) {
                        return ItemStack.EMPTY;
              		}
            	}
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, stack);
        }

        return itemstack;
    }

}
