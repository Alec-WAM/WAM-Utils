package alec_wam.wam_utils.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public abstract class WAMUtilsCustomItemRenderer extends BlockEntityWithoutLevelRenderer {

    protected WAMUtilsCustomItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    protected EntityModelSet getEntityModels() {
        //Just have this method as a helper for what we pass as entity models rather than bothering to
        // use an AT to access it directly
        return Minecraft.getInstance().getEntityModels();
    }
    
    protected BlockEntityRenderDispatcher getBlockEntityRenderDisbatcher() {
        //Just have this method as a helper for what we pass as entity models rather than bothering to
        // use an AT to access it directly
        return Minecraft.getInstance().getBlockEntityRenderDispatcher();
    }

    @Override
    public abstract void onResourceManagerReload(ResourceManager resourceManager);

    @Override
    public abstract void renderByItem(ItemStack stack, TransformType transformType, PoseStack matrix, MultiBufferSource renderer,
          int light, int overlayLight);
}
