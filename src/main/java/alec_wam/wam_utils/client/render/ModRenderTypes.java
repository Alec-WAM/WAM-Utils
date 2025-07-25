package alec_wam.wam_utils.client.render;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

public class ModRenderTypes {

//    public static final RenderType SolidBoxArea = create("SolidBoxArea",
//            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, false,
//            RenderType.CompositeState.builder()
//                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
//                    .setLayeringState(VIEW_OFFSET_Z_LAYERING) // view_offset_z_layering
//                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
//                    .setTextureState(NO_TEXTURE)
//                    .setDepthTestState(LEQUAL_DEPTH_TEST)
//                    .setCullState(NO_CULL)
//                    .setLightmapState(NO_LIGHTMAP)
//                    .setWriteMaskState(COLOR_WRITE)
//                    .createCompositeState(false));
//
//    public static final RenderType SolidBoxAreaOpaque = create("SolidBoxArea",
//            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, false,
//            RenderType.CompositeState.builder()
//                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
//                    .setLayeringState(VIEW_OFFSET_Z_LAYERING) // view_offset_z_layering
//                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
//                    .setTextureState(NO_TEXTURE)
//                    .setDepthTestState(LEQUAL_DEPTH_TEST)
//                    .setCullState(NO_CULL)
//                    .setLightmapState(NO_LIGHTMAP)
//                    .setWriteMaskState(COLOR_DEPTH_WRITE)
//                    .createCompositeState(false));
//
    public static final RenderType TRANSPARENT_BOX = RenderType.create(
    		"transparent_box",
            256, false, true,
            RenderPipelines.ENTITY_TRANSLUCENT,
            RenderType.CompositeState.builder()
            .setTextureState(RenderStateShard.NO_TEXTURE)
            .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
            .createCompositeState(true)
    );
    
    
//    public static final RenderType TRANSPARENT_BOX = create("transparent_box",
//            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true,
//            RenderType.CompositeState.builder()
//                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)  // Use the translucent shader
//                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)  // View offset Z layering
//                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)  // Enable translucent transparency
//                    .setTextureState(NO_TEXTURE)  // No texture state
//                    .setDepthTestState(LEQUAL_DEPTH_TEST)  // Depth test state
//                    .setCullState(NO_CULL)  // No cull state
//                    .setLightmapState(NO_LIGHTMAP)  // No lightmap state
//                    .setWriteMaskState(COLOR_WRITE)  // Only write color
//                    .createCompositeState(true));  // Enable sort on transparency
    
//
//    public static final RenderPipeline TRANSPARENT_BOX_PIPELINE = register(
//        RenderPipeline.builder(GUI_TEXTURED_SNIPPET)
//            .withLocation("pipeline/fire_screen_effect")
//            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
//            .withDepthWrite(false)
//            .build()
//    );
}