package alec_wam.wam_utils.blocks.bookshelf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import alec_wam.wam_utils.WAMUtilsMod;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ScrollModel extends Model {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(WAMUtilsMod.MODID, "scroll"), "main");
	private final ModelPart scroll;
	private final ModelPart root;

	public ScrollModel(EntityModelSet entityModelSet) {
		this(entityModelSet.bakeLayer(LAYER_LOCATION));
	}
	
	public ScrollModel(ModelPart root) {
		super(RenderType::entitySolid);
		this.root = root;
		this.scroll = root.getChild("scroll");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition scroll = partdefinition.addOrReplaceChild("scroll", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -10.0F, -1.0F, 2.0F, 10.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 16, 16);
	}

	@Override
	public void renderToBuffer(PoseStack p_102298_, VertexConsumer p_102299_, int p_102300_, int p_102301_, float p_102302_, float p_102303_, float p_102304_, float p_102305_) {
		this.render(p_102298_, p_102299_, p_102300_, p_102301_, p_102302_, p_102303_, p_102304_, p_102305_);
	}

	public void render(PoseStack p_102317_, VertexConsumer p_102318_, int p_102319_, int p_102320_, float p_102321_, float p_102322_, float p_102323_, float p_102324_) {
		this.root.render(p_102317_, p_102318_, p_102319_, p_102320_, p_102321_, p_102322_, p_102323_, p_102324_);
	}

	public void setupAnim(float p_102293_, float p_102294_, float p_102295_, float p_102296_) {

	}
}
