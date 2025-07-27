package alec_wam.wam_utils.client.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;

public class EnchantedBookModel extends Model {

    private static final String BOOK_MAIN = "book";
    private final ModelPart root;
    private final ModelPart book;
    // private final ModelPart book_main;
    // private final ModelPart page;

    public EnchantedBookModel(ModelPart root) {
        super(root, RenderType::entitySolid);
        this.root = root;
        this.book = root.getChild("book");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition book = partdefinition.addOrReplaceChild("book", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1.0F, -10.0F, -3.0F, 2.0F, 10.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 16, 16);
    }
}
