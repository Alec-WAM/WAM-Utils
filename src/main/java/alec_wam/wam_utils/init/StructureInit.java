package alec_wam.wam_utils.init;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.level.PylonFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraftforge.common.world.BiomeGenerationSettingsBuilder;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo.BiomeInfo.Builder;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class StructureInit {

	public static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIERS = DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, WAMUtilsMod.MODID);
	
	public static final RegistryObject<Codec<PylonBiomeModifier>> PYLON_BIOME_MODIFIER = BIOME_MODIFIERS.register("pylon_biome_modifier", () -> {
		return PylonBiomeModifier.CODEC;
	});
	
	public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, WAMUtilsMod.MODID);
	public static final DeferredRegister<ConfiguredFeature<?, ?>> CONFIGURED_FEATURES = DeferredRegister.create(Registry.CONFIGURED_FEATURE_REGISTRY, WAMUtilsMod.MODID);
	public static final DeferredRegister<PlacedFeature> PLACED_FEATURES = DeferredRegister.create(Registry.PLACED_FEATURE_REGISTRY, WAMUtilsMod.MODID);
	
	public static final RegistryObject<Feature<NoneFeatureConfiguration>> EARTH_PYLON = FEATURES.register("earth_pylon", () -> new PylonFeature(NoneFeatureConfiguration.CODEC, BlockInit.EARTH_PYLON_BLOCK.get()));
	public static final RegistryObject<ConfiguredFeature<?, ?>> EARTH_PYLON_CONFIGURED = CONFIGURED_FEATURES.register("earth_pylon", () -> {
		return new ConfiguredFeature<>(EARTH_PYLON.get(), FeatureConfiguration.NONE);
	});
	public static final RegistryObject<PlacedFeature> EARTH_PYLON_PLACEMENT = PLACED_FEATURES.register("earth_pylon", () -> {
		return new PlacedFeature(EARTH_PYLON_CONFIGURED.getHolder().orElseThrow(NullPointerException::new), List.of(RarityFilter.onAverageOnceEvery(64), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome()));
	});
	
	public static final RegistryObject<Feature<NoneFeatureConfiguration>> EVIL_PYLON = FEATURES.register("evil_pylon", () -> new PylonFeature(NoneFeatureConfiguration.CODEC, BlockInit.EVIL_PYLON_BLOCK.get(), BuiltInLootTables.SIMPLE_DUNGEON));
	public static final RegistryObject<ConfiguredFeature<?, ?>> EVIL_PYLON_CONFIGURED = CONFIGURED_FEATURES.register("evil_pylon", () -> {
		return new ConfiguredFeature<>(EVIL_PYLON.get(), FeatureConfiguration.NONE);
	});
	public static final RegistryObject<PlacedFeature> EVIL_PYLON_PLACEMENT = PLACED_FEATURES.register("evil_pylon", () -> {
		return new PlacedFeature(EVIL_PYLON_CONFIGURED.getHolder().orElseThrow(NullPointerException::new), List.of(RarityFilter.onAverageOnceEvery(64), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome()));
	});
	
	public static final RegistryObject<Feature<NoneFeatureConfiguration>> WATER_PYLON = FEATURES.register("water_pylon", () -> new PylonFeature(NoneFeatureConfiguration.CODEC, BlockInit.WATER_PYLON_BLOCK.get(), BuiltInLootTables.SHIPWRECK_TREASURE) {
		@Override
		public BlockPos findSurfacePos(WorldGenLevel level, BlockPos origin) {
			int j = level.getHeight(Heightmap.Types.OCEAN_FLOOR, origin.getX(), origin.getZ());
			return new BlockPos(origin.getX(), j, origin.getZ());
		}
	});
	public static final RegistryObject<ConfiguredFeature<?, ?>> WATER_PYLON_CONFIGURED = CONFIGURED_FEATURES.register("water_pylon", () -> {
		return new ConfiguredFeature<>(WATER_PYLON.get(), FeatureConfiguration.NONE);
	});
	public static final RegistryObject<PlacedFeature> WATER_PYLON_PLACEMENT = PLACED_FEATURES.register("water_pylon", () -> {
		return new PlacedFeature(WATER_PYLON_CONFIGURED.getHolder().orElseThrow(NullPointerException::new), List.of(RarityFilter.onAverageOnceEvery(64), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_OCEAN_FLOOR, BiomeFilter.biome()));
	});
	
	public static final RegistryObject<Feature<NoneFeatureConfiguration>> ENDER_PYLON = FEATURES.register("ender_pylon", () -> new PylonFeature(NoneFeatureConfiguration.CODEC, BlockInit.ENDER_PYLON_BLOCK.get(), BuiltInLootTables.END_CITY_TREASURE));
	public static final RegistryObject<ConfiguredFeature<?, ?>> ENDER_PYLON_CONFIGURED = CONFIGURED_FEATURES.register("ender_pylon", () -> {
		return new ConfiguredFeature<>(ENDER_PYLON.get(), FeatureConfiguration.NONE);
	});
	public static final RegistryObject<PlacedFeature> ENDER_PYLON_PLACEMENT = PLACED_FEATURES.register("ender_pylon", () -> {
		return new PlacedFeature(ENDER_PYLON_CONFIGURED.getHolder().orElseThrow(NullPointerException::new), List.of(RarityFilter.onAverageOnceEvery(64), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome()));
	});

	public static void registerConfiguredFeatures() {}
	
	public record PylonBiomeModifier(HolderSet<Biome> overworldBiomes, HolderSet<PlacedFeature> overworldFeatures, HolderSet<Biome> oceanBiomes, HolderSet<PlacedFeature> oceanFeatures, HolderSet<Biome> endBiomes, 
			HolderSet<PlacedFeature> endFeatures) implements BiomeModifier {

		public static final Codec<PylonBiomeModifier> CODEC = RecordCodecBuilder.create((instance) -> {
			return instance.group(Biome.LIST_CODEC.fieldOf("overworld_biomes").forGetter((biomeModifier) -> {
				return biomeModifier.overworldBiomes();
			}), PlacedFeature.LIST_CODEC.fieldOf("overworld_features").forGetter((biomeModifier) -> {
				return biomeModifier.overworldFeatures();
			}), Biome.LIST_CODEC.fieldOf("ocean_biomes").forGetter((biomeModifier) -> {
				return biomeModifier.oceanBiomes();
			}), PlacedFeature.LIST_CODEC.fieldOf("ocean_features").forGetter((biomeModifier) -> {
				return biomeModifier.oceanFeatures();
			}), Biome.LIST_CODEC.fieldOf("end_biomes").forGetter((biomeModifier) -> {
				return biomeModifier.endBiomes();
			}), PlacedFeature.LIST_CODEC.fieldOf("end_features").forGetter((biomeModifier) -> {
				return biomeModifier.endFeatures();
			})).apply(instance, PylonBiomeModifier::new);
		});

		@Override
		public void modify(Holder<Biome> biome, Phase phase, Builder builder) {
			if (phase == Phase.ADD) {
				BiomeGenerationSettingsBuilder generationBuilder = builder.getGenerationSettings();
				if (this.oceanBiomes.contains(biome)) {
					for (Holder<PlacedFeature> holder : this.oceanFeatures) {
						generationBuilder.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, holder);
					}
				}
				if (this.overworldBiomes.contains(biome)) {
					for (Holder<PlacedFeature> holder : this.overworldFeatures) {
						generationBuilder.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, holder);
					}
				} else if (this.endBiomes.contains(biome)) {
					for (Holder<PlacedFeature> holder : this.endFeatures) {
						generationBuilder.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, holder);
					}
				}
			}
		}

		@Override
		public Codec<PylonBiomeModifier> codec() {
			return PYLON_BIOME_MODIFIER.get();
		}

	}
}
