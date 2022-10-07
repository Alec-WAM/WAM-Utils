package alec_wam.wam_utils.init;

import java.util.function.Supplier;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.entities.chest_boats.EnderChestBoat;
import alec_wam.wam_utils.utils.BlockUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EntityInit {
	public static final DeferredRegister<EntityType<?>> ENTITIES = 
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, WAMUtilsMod.MODID);
	public static final DeferredRegister<PoiType> POI = 
            DeferredRegister.create(ForgeRegistries.POI_TYPES, WAMUtilsMod.MODID);
	
	public static final RegistryObject<EntityType<EnderChestBoat>> ENDER_CHEST_BOAT = register("enderchest_boat", () -> EntityType.Builder.<EnderChestBoat>of(EnderChestBoat::new, MobCategory.MISC).sized(1.375F, 0.5625F).clientTrackingRange(10));
	
	public static final RegistryObject<PoiType> WANDERING_TRADER_SIGN_POI = POI.register("wandering_trader_sign", () -> new PoiType(BlockUtils.getAllBlockStates(BlockInit.WANDERING_TRADER_SIGN_BLOCK.get()), 0, 1));
	public static final RegistryObject<PoiType> PILLAGER_SIGN_POI = POI.register("pillager_sign", () -> new PoiType(BlockUtils.getAllBlockStates(BlockInit.PILLAGER_SIGN_BLOCK.get()), 0, 1));
	public static final RegistryObject<PoiType> ENDERMAN_SIGN_POI = POI.register("enderman_sign", () -> new PoiType(BlockUtils.getAllBlockStates(BlockInit.ENDERMAN_SIGN_BLOCK.get()), 0, 1));
	public static final RegistryObject<PoiType> PHANTOM_SIGN_POI = POI.register("phantom_sign", () -> new PoiType(BlockUtils.getAllBlockStates(BlockInit.PHANTOM_SIGN_BLOCK.get()), 0, 1));

	public static final RegistryObject<PoiType> ADVANCED_BEEHIVE_POI = POI.register("advanced_beehive", () -> new PoiType(BlockUtils.getAllBlockStates(BlockInit.ADVANCED_BEEHIVE_BLOCK.get()), 0, 1));

	private static <T extends Entity> RegistryObject<EntityType<T>> register(String name,
			Supplier<EntityType.Builder<T>> prepare) {
		return ENTITIES.register(name, () -> prepare.get().build(WAMUtilsMod.MODID + ":" + name));
	}
}
