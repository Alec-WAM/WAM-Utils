package alec_wam.wam_utils.init;

import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.init.ItemInit.ModCreativeTab;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.wrappers.FluidBucketWrapper;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class FluidInit {

	public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, WAMUtilsMod.MODID);
	public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(
			ForgeRegistries.Keys.FLUID_TYPES, WAMUtilsMod.MODID
	);
	
	public static final RegistryObject<FluidType> XP_FLUID_TYPE = FLUID_TYPES.register("xp", () -> makeTypeWithTextures(FluidType.Properties.create().density(800).viscosity(1500).lightLevel(10),
			new ResourceLocation("wam_utils:blocks/fluid/xp_still"), new ResourceLocation("wam_utils:blocks/fluid/xp_flow")));
	public static final FluidInstance XP = new FluidInstance("xp", XP_FLUID_TYPE);
	
	public static class FluidInstance {

	    private RegistryObject<Fluid> flowingFluid;
	    private RegistryObject<Fluid> sourceFluid;
	    private RegistryObject<Item> bucketFluid;
	    private RegistryObject<Block> blockFluid;
	    private String fluid;

	    public FluidInstance(String fluid, RegistryObject<FluidType> fluidType) {
	        this.fluid = fluid;
	        this.sourceFluid = FLUIDS.register(fluid, () -> new CustomFluid.Source(this, fluidType));
	        this.flowingFluid = FLUIDS.register(fluid + "_flowing", () -> new CustomFluid.Flowing(this, fluidType));
	        this.bucketFluid = ItemInit.ITEMS.register("xp_bucket", () -> makeBucket(sourceFluid));
	        this.blockFluid = BlockInit.BLOCKS.register(fluid + "_fluid", () -> new LiquidBlock(() -> (FlowingFluid) sourceFluid.get(), Block.Properties.of(Material.WATER).noCollission().strength(100.0F).noLootTable()));
	        BlockInit.autoItemBlockBlacklist.add(blockFluid);
	    }

	    public Fluid getFlowingFluid() {
	        return flowingFluid.get();
	    }

	    public Fluid getSourceFluid() {
	        return sourceFluid.get();
	    }

	    public Item getBucketFluid() {
	        return bucketFluid.get();
	    }

	    public Block getBlockFluid() {
	        return blockFluid.get();
	    }

	    public String getFluid() {
	        return fluid;
	    }
	}
	
	private static FluidType makeTypeWithTextures(
			FluidType.Properties builder, ResourceLocation stillTex, ResourceLocation flowingTex
	)
	{
		return new FluidType(builder)
		{
			@Override
			public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer)
			{
				consumer.accept(new IClientFluidTypeExtensions()
				{
					@Override
					public ResourceLocation getStillTexture()
					{
						return stillTex;
					}

					@Override
					public ResourceLocation getFlowingTexture()
					{
						return flowingTex;
					}
				});
			}
		};
	}
	
	private static BucketItem makeBucket(RegistryObject<Fluid> still)
	{
		return new BucketItem(
				still, new Item.Properties()
				.stacksTo(1)
				.tab(ModCreativeTab.instance)
				.craftRemainder(Items.BUCKET))
		{
			@Override
			public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt)
			{
				return new FluidBucketWrapper(stack);
			}
		};
	}
	
	public static class CustomFluid extends FlowingFluid {

	    private FluidInstance instance;
	    private RegistryObject<FluidType> fluidType;

	    public CustomFluid(FluidInstance instance, RegistryObject<FluidType> fluidType) {
	        this.instance = instance;
	        this.fluidType = fluidType;
	    }

	    @Override
	    @Nonnull
	    public Fluid getFlowing() {
	        return instance.getFlowingFluid();
	    }

	    @Override
	    @Nonnull
	    public Fluid getSource() {
	        return instance.getSourceFluid();
	    }

	    @Override
	    protected boolean canConvertToSource() {
	        return false;
	    }

	    @Override
	    protected void beforeDestroyingBlock(LevelAccessor worldIn, BlockPos pos, BlockState state) {
	        // copied from the WaterFluid implementation
	        BlockEntity tileentity = state.hasBlockEntity() ? worldIn.getBlockEntity(pos) : null;
	        Block.dropResources(state, worldIn, pos, tileentity);
	    }

	    @Override
	    protected int getSlopeFindDistance(@Nonnull LevelReader world) {
	        return 4;
	    }

	    @Override
	    protected int getDropOff(@Nonnull LevelReader world) {
	        return 1;
	    }

	    @Override
	    @Nonnull
	    public Item getBucket() {
	        return instance.getBucketFluid();
	    }

	    @SuppressWarnings("deprecation")
		@Override
	    @ParametersAreNonnullByDefault
	    protected boolean canBeReplacedWith(FluidState p_215665_1_, BlockGetter p_215665_2_, BlockPos p_215665_3_, Fluid p_215665_4_, Direction p_215665_5_) {
	        return p_215665_5_ == Direction.DOWN && !p_215665_4_.is(FluidTags.WATER);
	    }

	    @Override
	    public int getTickDelay(@Nonnull LevelReader p_205569_1_) {
	        return 5;
	    }

	    @Override
	    protected float getExplosionResistance() {
	        return 1;
	    }

	    @Override
	    @Nonnull
	    protected BlockState createLegacyBlock(@Nonnull FluidState state) {
	        return instance.getBlockFluid().defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
	    }

	    @Override
	    public boolean isSource(@Nonnull FluidState state) {
	        return false;
	    }

	    @Override
	    public int getAmount(@Nonnull FluidState p_207192_1_) {
	        return 0;
	    }

	    @Override
	    public boolean isSame(Fluid fluidIn) {
	        return fluidIn == instance.getFlowingFluid() || fluidIn == instance.getSourceFluid();
	    }
	    
	    @Override
	    public net.minecraftforge.fluids.FluidType getFluidType() {
	    	return fluidType.get();
	    }
	    
	    @Override
	    public Optional<SoundEvent> getPickupSound() {
	    	return Optional.of(SoundEvents.BUCKET_FILL);
	    }

	    public static class Flowing extends CustomFluid {
	        {
	            registerDefaultState(getStateDefinition().any().setValue(LEVEL, 7));
	        }

	        public Flowing(FluidInstance instance, RegistryObject<FluidType> fluidType) {
	            super(instance, fluidType);
	        }

	        @Override
	        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
	            super.createFluidStateDefinition(builder);
	            builder.add(LEVEL);
	        }

	        @Override
	        public int getAmount(@Nonnull FluidState p_207192_1_) {
	            return p_207192_1_.getValue(LEVEL);
	        }

	        @Override
	        public boolean isSource(@Nonnull FluidState state) {
	            return false;
	        }
	    }

	    public static class Source extends CustomFluid {

	        public Source(FluidInstance instance, RegistryObject<FluidType> fluidType) {
	            super(instance, fluidType);
	        }

	        @Override
	        public int getAmount(@Nonnull FluidState p_207192_1_) {
	            return 8;
	        }

	        @Override
	        public boolean isSource(@Nonnull FluidState state) {
	            return true;
	        }
	    }
	}
}
