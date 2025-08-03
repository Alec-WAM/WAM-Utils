package alec_wam.wam_utils.common.entities.workers.jobs.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import com.mojang.datafixers.util.Pair;

import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager.JobType;
import alec_wam.wam_utils.common.entities.workers.jobs.MultiBlockPosWorkerJob;
import alec_wam.wam_utils.common.entities.workers.jobs.WorkerJob;
import alec_wam.wam_utils.common.helpers.BlockHelper;
import alec_wam.wam_utils.common.helpers.EntityHelper;
import alec_wam.wam_utils.common.helpers.ItemHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.items.IItemHandler;

public class PotionBrewJob extends MultiBlockPosWorkerJob {
	private static final Predicate<ItemStack> IS_GLASS_BOTTLE = (stack) -> {
		return stack.is(Items.GLASS_BOTTLE);
	};

    private static final Predicate<ItemStack> IS_BREWING_FUEL = (stack) -> {
        return stack.is(ItemTags.BREWING_FUEL);
    };
    
    public static enum BrewingTask implements StringRepresentable{
        NONE,
        FILLING,
        FUELING,
        BREWING,
        WAITING,
        EMPTYING;

        public static final StringRepresentable.EnumCodec<BrewingTask> CODEC = StringRepresentable.fromEnum(BrewingTask::values);
        public static final StreamCodec<ByteBuf, BrewingTask> STREAM_CODEC = ByteBufCodecs.stringUtf8(24)
        																	.map(BrewingTask::valueOf, BrewingTask::name);

        @Override
        public String getSerializedName() {
            return this.name();
        }
    } 
	
	private BrewingTask currentTask = BrewingTask.NONE;
    private boolean hasFuel = false;
    private boolean hasGlassBottles = false;
    private Pair<ItemStack, ItemStack> currentPotion = null;
    private int inventoryScanDelay = 0;
    private int waitingForPotionDelay = 0;

	public PotionBrewJob(WorkerEntity worker, ResourceKey<Level> dimension, List<BlockPos> blockPosList) {
		super(worker, dimension, blockPosList);
	}
	
	public PotionBrewJob(WorkerEntity worker, ResourceKey<Level> dimension, BlockPos posA, BlockPos posB) {
		super(worker, dimension, posA, posB);
	}
	
	@Override
	public void save(ValueOutput valueOutput) {
		super.save(valueOutput);
        ValueOutput brewingOutput = valueOutput.child("BrewingData");
        brewingOutput.store("CurrentTask", BrewingTask.CODEC, this.currentTask);
        brewingOutput.putBoolean("HasGlassBottles", this.hasGlassBottles);
        brewingOutput.putBoolean("HasFuel", this.hasFuel);
        if(this.currentPotion != null) {
            ValueOutput potionOutput = brewingOutput.child("CurrentPotion");
            potionOutput.store("Ingredient", ItemStack.CODEC, this.currentPotion.getFirst());
            potionOutput.store("Input", ItemStack.CODEC, this.currentPotion.getSecond());
        }
        brewingOutput.putInt("InventoryScanDelay", this.inventoryScanDelay);
        brewingOutput.putInt("WaitingForPotionDelay", this.waitingForPotionDelay);
	}
	
	@Override
	public void load(ValueInput valueInput) {
		super.load(valueInput);
        ValueInput brewingInput = valueInput.childOrEmpty("BrewingData");
        this.currentTask = brewingInput.read("CurrentTask", BrewingTask.CODEC).orElse(BrewingTask.NONE);
        this.hasGlassBottles = brewingInput.getBooleanOr("HasGlassBottles", false);
        this.hasFuel = brewingInput.getBooleanOr("HasFuel", false);
        
        Optional<ValueInput> potionInputOpt = brewingInput.child("CurrentPotion");
        if(potionInputOpt.isPresent()) {
            ValueInput potionInput = potionInputOpt.get();
            ItemStack ingredient = potionInput.read("Ingredient", ItemStack.CODEC).orElse(ItemStack.EMPTY);
            ItemStack input = potionInput.read("Input", ItemStack.CODEC).orElse(ItemStack.EMPTY);
            this.currentPotion = Pair.of(ingredient, input);
        }
        else {
            this.currentPotion = null;
        }
        this.inventoryScanDelay = brewingInput.getIntOr("InventoryScanDelay", 0);
        this.waitingForPotionDelay = brewingInput.getIntOr("WaitingForPotionDelay", 0);
	}

	public static PotionBrewJob createJob(WorkerEntity worker, ItemStack stack, Player player) {
		return JobManager.createJobFromSelection(
            worker, stack, player,
            posList -> new PotionBrewJob(worker, player.level().dimension(), posList),
            (pos1, pos2) -> new PotionBrewJob(worker, player.level().dimension(), pos1, pos2)
        );
	}
	
	@Override
	public boolean needsItem(ItemStack stack) {
        if(this.currentTask == BrewingTask.FILLING) {
            if(IS_GLASS_BOTTLE.test(stack)){
                return true;
            }
        }
        return super.needsItem(stack);
	}
	
	@Override
	public void run() {
		super.run();
		if(!worker.level().isClientSide) {
			brewPotions();
		}
		
		//Unload Inventory if idling for 5 seconds
		if (this.idleTimer >= 5 * 20) {
			if (worker.getExternalInventorySettings() != null) {
				// Only check every whole second to prevent unneeded inventory scans
				if ((this.idleTimer % 20 == 0) && worker.getExternalInventorySettings().hasInputFace()
						&& worker.hasItemsToUnload()) {
					this.idleTimer = 0;
					worker.startUnloadingInventory(false);
				}
			}
		}
	}
	
	@Override	
	public double getInteractionRange(BlockState state) {
		return 2.0D;
	}

	@Override
	public boolean canInteractWithBlock(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
        if(this.currentTask == BrewingTask.NONE){
            if(this.currentPotion == null && this.hasGlassBottles){
                if(BlockHelper.isWater(level, pos)){
                    return true;
                }
                // TODO Investigate how to allow both brewing and filling
                return false;
            }
            if(state.is(Blocks.BREWING_STAND)){
                return this.currentPotion != null;
            }
        }
        if(this.currentTask == BrewingTask.FILLING){
            return BlockHelper.isWater(level, pos);
        }
        if(this.currentTask == BrewingTask.BREWING 
            || this.currentTask == BrewingTask.EMPTYING
            || this.currentTask == BrewingTask.WAITING){
            return state.is(Blocks.BREWING_STAND);
        }
		return false;
	}
	
	public void brewPotions() {			
		if(this.currentTask == BrewingTask.NONE){
            if(inventoryScanDelay > 0){
                inventoryScanDelay--;
            }
            else {
                this.buildIngredientList();
                // TODO increase this delay
                this.inventoryScanDelay = 10 * 20; // 30 seconds
                return;
            }
        }

		if(this.getWorkingPos() == null)return;

		Level level = worker.level();
        BlockPos pos = getWorkingPos();
		if (!level.isClientSide) {				
			ItemStack handItem = worker.getMainHandItem();

			lookAtBlock();
			moveToBlock(1);

            BlockState blockState = level.getBlockState(pos);
            BrewingStandBlockEntity brewingStand = null;
            boolean isWater = BlockHelper.isWater(level, pos);
            boolean isBrewingStand = blockState.is(Blocks.BREWING_STAND);
            if(isWater) {
                if(this.currentTask == BrewingTask.NONE) {
                    this.currentTask = BrewingTask.FILLING;
                }
            }
            else if(isBrewingStand) {
                // System.out.println("Is Brewing Stand");
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if(blockEntity instanceof BrewingStandBlockEntity) {
                    brewingStand = (BrewingStandBlockEntity)blockEntity;

                    //TODO Figure out fuel
                    if(this.currentTask == BrewingTask.NONE) {
                        if(this.hasFuel){
                            // IItemHandler itemHandler = BlockHelper.getItemHandler(level, pos, Direction.UP);
                        }
                        if(this.currentPotion != null){
                            this.currentTask = BrewingTask.BREWING;
                            // System.out.println("Setting Task to Brewing");
                        }
                    }
                }
            }

            if(this.currentTask == BrewingTask.FILLING){                
                if(worker.swapToItem(IS_GLASS_BOTTLE)){
                    return;
                }
            }

            if(this.currentTask == BrewingTask.BREWING){
                if(this.currentPotion == null){
                    this.currentTask = BrewingTask.NONE;
                    return;
                }
                if(!worker.requireItem(this::isCurrentPotionIngredient, Optional.of(1))){
                    // ItemStack foundItem = ItemHelper.findItem(worker, this::isCurrentPotionIngredient);
                    // System.out.println("Couldn't find ingredient: " + foundItem + " " + this.currentPotion);
                    return;
                }
                if(!worker.requireItem(this::isCurrentPotionInput, Optional.of(3))){
                    // System.out.println("Couldn't find input");
                    return;
                }

                if(ItemHelper.findItem(worker, this::isCurrentPotionIngredient).isEmpty() 
                    || ItemHelper.findItem(worker, this::isCurrentPotionInput).isEmpty()){
                    System.out.println("Couldn't find ingredients. Clearing");
                    this.currentTask = BrewingTask.NONE;
                    this.currentPotion = null;
                    this.invalidateWorkingPos();
                    return;
                }
            }
            
			
			if (this.isCloseToBlockPos()) {
				if(EntityHelper.isLookingAtHorizontally(worker, pos, 20)){
                    if(canInteractWithBlock(level, pos)) {
                        if(this.currentTask == BrewingTask.FILLING) {
                            if(isWater && this.hasGlassBottles){
                                if(handItem.isEmpty() || !IS_GLASS_BOTTLE.test(handItem)){
                                    this.currentTask = BrewingTask.NONE;
                                    this.hasGlassBottles = false;
                                    this.invalidateWorkingPos();
                                    return;
                                }
                                
                                this.fillWaterBottle(handItem, pos);
                            }
                        
                            if(!this.hasGlassBottles){
                                this.finishWorking();
                            }
                        }
                        else if(this.currentTask == BrewingTask.BREWING) {
                            // System.out.println("Brewing");
                            
                            if(brewingStand !=null){
                                if(this.currentPotion != null){
                                    this.brewPotion(pos, brewingStand);
                                }
                            }
                        }
                        else if(this.currentTask == BrewingTask.WAITING) {                            
                            if(brewingStand !=null){
                                if(this.currentPotion != null){
                                    this.waitForPotion(pos, brewingStand);
                                }
                            }
                        }
                        else if(this.currentTask == BrewingTask.EMPTYING) {
                            // System.out.println("Emptying");
                            
                            if(brewingStand !=null){
                                if(this.currentPotion != null){
                                    this.emptyBrewingStand(pos, brewingStand);
                                }
                            }
                        }
					}
                    else {
                        System.out.println("Invalidate Working Pos");
                        this.invalidateWorkingPos();
                    }
				}
			}
		}
	}

    public boolean isCurrentPotionIngredient(ItemStack stack){
        if(this.currentPotion == null){
            return false;
        }
        return ItemStack.isSameItemSameComponents(stack, this.currentPotion.getFirst());
    }

    public boolean isCurrentPotionInput(ItemStack stack){
        if(this.currentPotion == null)return false;
        return ItemStack.isSameItemSameComponents(stack, this.currentPotion.getSecond());
    }

    public void fillWaterBottle(ItemStack handItem, BlockPos pos){
        ItemStack filledBottle = PotionContents.createItemStack(Items.POTION, Potions.WATER);

        if(worker.canAddToInventory(filledBottle) || handItem.getCount() == 1)  {
            worker.swing(InteractionHand.MAIN_HAND);
            Level level = worker.level();
            level.playSound(
                worker, worker.getX(), worker.getY(), worker.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F
            );
            level.gameEvent(worker, GameEvent.FLUID_PICKUP, pos);

            handItem.shrink(1);
            worker.addToInventory(filledBottle);
            //TODO Add a delay to the filling
        }
        else {
            this.worker.startUnloadingInventory(true);
        }
    }

    public void brewPotion(BlockPos pos, BrewingStandBlockEntity brewingStand){
        Level level = worker.level();
        Optional<IItemHandler> itemHandler = BlockHelper.getItemHandler(level, pos, Direction.DOWN);
        if(itemHandler.isPresent()) {
            IItemHandler inv = itemHandler.get();
            ItemStack ingStack = ItemHelper.findItem(worker, this::isCurrentPotionIngredient);
            boolean insertedIngredient = false;
            if(!ingStack.isEmpty() && inv.insertItem(3, ingStack.copyWithCount(1), true).isEmpty()){
                inv.insertItem(3, ingStack.copyWithCount(1), false);
                ingStack.shrink(1);
                insertedIngredient = true;
            }

            boolean insertedInputs = false;
            for(int i = 0; i < 3; i++){
                ItemStack inputStack = ItemHelper.findItem(worker, this::isCurrentPotionInput);
                if(!inputStack.isEmpty() && inv.insertItem(i, inputStack.copyWithCount(1), true).isEmpty()){
                    inv.insertItem(i, inputStack.copyWithCount(1), false);
                    inputStack.shrink(1);
                    insertedInputs = true;
                }
            }

            if(insertedIngredient && insertedInputs){
                this.currentTask = BrewingTask.WAITING;
            }
        }
    }

    public void waitForPotion(BlockPos pos, BrewingStandBlockEntity brewingStand){
        if(waitingForPotionDelay > 0){
            waitingForPotionDelay--;
            return;
        }
        // System.out.println("Waiting for brewing");

        if(this.currentPotion == null){
            this.currentTask = BrewingTask.NONE;
            this.invalidateWorkingPos();
            return;
        }
        
        Level level = worker.level();
        Optional<IItemHandler> itemHandler = BlockHelper.getItemHandler(level, pos, Direction.DOWN);
        ItemStack originalInput = this.currentPotion.getSecond();
        if(itemHandler.isPresent()) {
            IItemHandler inv = itemHandler.get();

            for(int i = 0; i < 3; i++){
                ItemStack stack = inv.getStackInSlot(i);
                if(ItemStack.isSameItemSameComponents(stack, originalInput)){
                    this.waitingForPotionDelay = 100;
                    // System.out.println("Brewing not done"); 
                    return;
                }
            }

            // System.out.println("Brewing done"); 
            this.currentTask = BrewingTask.EMPTYING;
        }
    }

    public void emptyBrewingStand(BlockPos pos, BrewingStandBlockEntity brewingStand){
        Level level = worker.level();
        Optional<IItemHandler> itemHandler = BlockHelper.getItemHandler(level, pos, Direction.DOWN);
        if(itemHandler.isPresent()) {
            IItemHandler inv = itemHandler.get();
            // Extract all items (potions and ingredient left overs)
            for(int i = 0; i < 4; i++){
                ItemStack stack = inv.getStackInSlot(i);
                ItemStack extracted = inv.extractItem(i, stack.getCount(), true);
                if(worker.canAddToInventory(extracted)) {
                    inv.extractItem(i, stack.getCount(), false);
                    worker.addToInventory(extracted);
                }
            }
            this.worker.startUnloadingInventory(false);
            this.currentPotion = null;
            this.currentTask = BrewingTask.NONE;
            this.finishWorking();
        }
    }

    @SuppressWarnings("deprecation")
    public void buildIngredientList(){
        Level level = worker.level();
        if(level == null || !(level instanceof ServerLevel))return;
        System.out.println("Building Ingredient List");
        ServerLevel serverLevel = (ServerLevel)level;
        PotionBrewing brewing = serverLevel.potionBrewing();
        // TODO Handle multiple brewing stands
        List<ItemStack> externalItems = worker.getExternalInventoryContents();
        boolean foundGlassBottles = false;
        boolean foundFuel = false;
        List<ItemStack> ingredients = new ArrayList<>();
        List<ItemStack> inputs = new ArrayList<>();
        for(ItemStack stack : externalItems) {
            if(IS_GLASS_BOTTLE.test(stack)) {
                foundGlassBottles = true;
                continue;
            }
            if(IS_BREWING_FUEL.test(stack)) {
                foundFuel = true;
                continue;
            }
            if(this.currentPotion != null && foundFuel && foundGlassBottles) {
                //We have checked for everything we need
                // Don't go looking for potion items if we are already brewing a potion
                break;
            }           
            if(brewing.isIngredient(stack)) {
                ingredients.add(stack);
            }
            if(brewing.isInput(stack)) {
                inputs.add(stack);
            }
        }

        this.hasGlassBottles = foundGlassBottles;
        this.hasFuel = foundFuel;
        // System.out.println("Has Glass Bottles: " + this.hasGlassBottles);

        if(ingredients.isEmpty() || inputs.isEmpty()) {
            // No Ingredients
            // System.out.println("No Ingredients");
            return;
        }

        ItemStack bestIngredient = ItemStack.EMPTY;
        ItemStack bestInput = ItemStack.EMPTY;
        long bestCount = 0;
        int bestPriority = Integer.MAX_VALUE;

        for (ItemStack ingredient : ingredients) {
            // Count occurrences of exact matching input stacks
            Map<ItemStack, Long> matchingInputCounts = new HashMap<>();
            int priority = getIngredientPriority(ingredient);
            inputs: for (ItemStack input : inputs) {
                Optional<Holder<Potion>> mixedPotion = ItemHelper.getPotionFromItems(brewing, input, ingredient);
                if (mixedPotion.isPresent()) {
                    Holder<Potion> potion = mixedPotion.get();

                    if(potion.is(Potions.MUNDANE) || potion.is(Potions.THICK)) {
                        continue inputs;
                    }
                    boolean foundMatch = false;

                    entries: for (Map.Entry<ItemStack, Long> entry : matchingInputCounts.entrySet()) {
                        if (ItemStack.isSameItemSameComponents(entry.getKey(), input)) {
                            matchingInputCounts.put(entry.getKey(), entry.getValue() + 1);
                            foundMatch = true;
                            break entries;
                        }
                    }

                    if (!foundMatch) {
                        matchingInputCounts.put(input.copy(), 1L); // store a copy
                    }
                }
            }

            // Now find the input with the most matches for this ingredient
            for (Map.Entry<ItemStack, Long> entry : matchingInputCounts.entrySet()) {
                if (priority < bestPriority || (priority == bestPriority && entry.getValue() > bestCount)) {
                    bestCount = entry.getValue();
                    bestIngredient = ingredient.copy();
                    bestInput = entry.getKey().copy();
                    bestPriority = priority;
                }
            }
        }
        if(!bestIngredient.isEmpty() && !bestInput.isEmpty()) {
            System.out.println("Found Recipe");
            System.out.println("bestIngredient: " + bestIngredient + " bestInput: " + bestInput);
            this.currentPotion = Pair.of(bestIngredient, bestInput);
        }
    }

    private int getIngredientPriority(ItemStack ingredient) {
        Item item = ingredient.getItem();

        // Makes level higher
        if (item == Items.GLOWSTONE_DUST) return 1;
        // Makes length longer
        if (item == Items.REDSTONE) return 2;
        // Corrupt potion after upgrade
        if (item == Items.FERMENTED_SPIDER_EYE) return 3;
        // Splash potion last
        if (item == Items.GUNPOWDER) return 4;
        return 0; // Base ingredients first (lowest priority number)
    }
	
	@Override
	public void stop() {
		
	}

	@Override
	public boolean isSame(WorkerJob job) {
		return job instanceof PotionBrewJob && super.isSame(job);
	}

	@Override
	public JobType getJobType() {
        return JobType.BREWING;
	}

}
