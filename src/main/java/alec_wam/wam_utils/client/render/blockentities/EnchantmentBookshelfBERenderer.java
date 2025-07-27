package alec_wam.wam_utils.client.render.blockentities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.math.Axis;

import alec_wam.wam_utils.WAMUtils;
import alec_wam.wam_utils.client.ModClientInit;
import alec_wam.wam_utils.client.model.EnchantedBookModel;
import alec_wam.wam_utils.common.blocks.enchantment.bookshelf.EnchantmentBookshelfBE;
import alec_wam.wam_utils.common.blocks.enchantment.bookshelf.EnchantmentBookshelfBlock;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;

public class EnchantmentBookshelfBERenderer implements BlockEntityRenderer<EnchantmentBookshelfBE> {

    public static final ResourceLocation BOOK_TEXTURE = ResourceLocation.fromNamespaceAndPath(WAMUtils.MODID,
            "entity/book");
    public static final ResourceLocation ENCHANTED_BOOK_TEXTURE = ResourceLocation.fromNamespaceAndPath(WAMUtils.MODID,
            "entity/enchanted_book");

    public static final Material BOOK_LOCATION = new Material(
        TextureAtlas.LOCATION_BLOCKS, BOOK_TEXTURE
    );
    public static final Material ENCHANTED_BOOK_LOCATION = new Material(
        TextureAtlas.LOCATION_BLOCKS, ENCHANTED_BOOK_TEXTURE
    );

    private final EnchantedBookModel bookModel;

    public EnchantmentBookshelfBERenderer(BlockEntityRendererProvider.Context context) {
        this.bookModel = new EnchantedBookModel(context.bakeLayer(ModClientInit.BOOK_LAYER));
    }

    @Override
    public void render(EnchantmentBookshelfBE blockEntity, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay, Vec3 cameraPos) {
        Direction direction = blockEntity.getBlockState().getValue(EnchantmentBookshelfBlock.FACING);
        IItemHandler handler = blockEntity.getItemHandler(null);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        float f = -direction.toYRot() + 180.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(f));

        float scale = 0.4F;// 0.55F;
        float width = 0.65F;
        poseStack.scale(width, scale, scale);
        poseStack.translate(0.60D, -0.4D/*-0.75D*/, 0.85D/* 0.6D */);

        int slot = 0;
        if (handler != null) {
            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 8; x++) {
                    if (slot >= handler.getSlots())
                        continue;
                    ItemStack book = handler.getStackInSlot(slot);
                    if (!book.isEmpty()) {
                        double offsetX = 0.17D;
                        poseStack.pushPose();
                        poseStack.translate(-offsetX * x, -y * 0.8D, 0.0D);
                        // if (book.is(ItemInit.SINGLE_ENCHANTMENT_ITEM.get())) {
                        //     poseStack.scale(0.8F, 0.95F, 1.25F);
                        //     poseStack.translate(0.0D, 0.05D, 0.0D);
                        //     VertexConsumer vertexconsumer = VertexMultiConsumer.create(
                        //             bufferSource.getBuffer(RenderType.entityGlintDirect()),
                        //             SCROLL_LOCATION.buffer(bufferSource, RenderType::entitySolid));
                        //     this.scrollModel.renderToBuffer(poseStack, vertexconsumer, p_112348_,
                        //             OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
                        // } else {
                        boolean enchanted = book.is(Items.ENCHANTED_BOOK);
                        VertexConsumer vertexconsumer = enchanted
                                ? VertexMultiConsumer.create(bufferSource.getBuffer(RenderType.entityGlint()), ENCHANTED_BOOK_LOCATION.buffer(bufferSource, RenderType::entitySolid))
                                : BOOK_LOCATION.buffer(bufferSource, RenderType::entitySolid);
                        this.bookModel.renderToBuffer(poseStack, vertexconsumer, packedLight, OverlayTexture.NO_OVERLAY);
                        // }
                        poseStack.popPose();
                    }
                    slot++;
                }
            }
        }

        poseStack.popPose();
    }

}
