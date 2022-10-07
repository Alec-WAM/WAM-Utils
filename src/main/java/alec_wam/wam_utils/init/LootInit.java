package alec_wam.wam_utils.init;

import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import alec_wam.wam_utils.WAMUtilsMod;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@EventBusSubscriber(modid = WAMUtilsMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class LootInit {
	
	public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> GLM = DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, WAMUtilsMod.MODID);
	
	public static final RegistryObject<Codec<WitherBonesConverterModifier>> WITHER_BONES = GLM.register("wither_bones", WitherBonesConverterModifier.CODEC);
    	
	private static class WitherBonesConverterModifier extends LootModifier {
        public static final Supplier<Codec<WitherBonesConverterModifier>> CODEC = Suppliers.memoize(() -> RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, WitherBonesConverterModifier::new)));

        public WitherBonesConverterModifier(LootItemCondition[] conditionsIn) {
            super(conditionsIn);
        }

        @Override
        public ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        	ObjectArrayList<ItemStack> ret = new ObjectArrayList<ItemStack>();
        	//TODO Make this a config option
        	generatedLoot.forEach((stack) -> {
            	if(stack.getItem() == Items.BONE) {
            		ret.add(new ItemStack(ItemInit.WITHER_BONE.get(), stack.getCount()));
            	}
            	else {
            		ret.add(stack);
            	}
            });
            return ret;
        }

        @Override
        public Codec<? extends IGlobalLootModifier> codec() {
            return CODEC.get();
        }
    }
}
