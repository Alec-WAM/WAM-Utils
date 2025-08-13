package alec_wam.wam_utils.client.integration.jei;

import javax.annotation.Nonnull;

import alec_wam.wam_utils.WAMUtils;
import alec_wam.wam_utils.common.blocks.conveyor.extractor.menu.ItemExtractorScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class JEIIntegration implements IModPlugin {

    public JEIIntegration(){
        
    }


    @Nonnull
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(WAMUtils.MODID, "jei_plugin");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(ItemExtractorScreen.class, new ExtractorFakeSlotJEIHandler());
    }
}