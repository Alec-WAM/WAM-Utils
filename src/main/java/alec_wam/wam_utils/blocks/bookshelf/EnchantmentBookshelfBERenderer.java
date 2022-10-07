package alec_wam.wam_utils.blocks.bookshelf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.math.Vector3f;

import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.init.ItemInit;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.IItemHandler;

@OnlyIn(Dist.CLIENT)
public class EnchantmentBookshelfBERenderer implements BlockEntityRenderer<EnchantmentBookshelfBE> {
	public static final ResourceLocation BOOK_TEXTURE = new ResourceLocation("wam_utils:entities/book");
	public static final ResourceLocation ENCHANTED_BOOK_TEXTURE = new ResourceLocation("wam_utils:entities/enchanted_book");
	public static final Material BOOK_LOCATION = new Material(TextureAtlas.LOCATION_BLOCKS, BOOK_TEXTURE);
	public static final Material ENCHANTED_BOOK_LOCATION = new Material(TextureAtlas.LOCATION_BLOCKS, ENCHANTED_BOOK_TEXTURE);
	public static final ResourceLocation SCROLL_TEXTURE = new ResourceLocation("wam_utils:entities/scroll");
	public static final Material SCROLL_LOCATION = new Material(TextureAtlas.LOCATION_BLOCKS, SCROLL_TEXTURE);
	
	private final ItemRenderer itemRenderer;
	private final BlockRenderDispatcher blockRenderer;
	private final EnchantedBookModel bookModel;
	private final ScrollModel scrollModel;
	
   public EnchantmentBookshelfBERenderer(BlockEntityRendererProvider.Context p_173602_) {
      this.itemRenderer = p_173602_.getItemRenderer();
      this.blockRenderer = p_173602_.getBlockRenderDispatcher();
      this.bookModel = new EnchantedBookModel(p_173602_.getModelSet());
      this.scrollModel = new ScrollModel(p_173602_.getModelSet());
   }

   @Override
   public void render(EnchantmentBookshelfBE bookshelf, float p_112345_, PoseStack p_112346_, MultiBufferSource p_112347_, int p_112348_, int p_112349_) {
      Direction direction = bookshelf.getBlockState().getValue(EnchantmentBookshelfBlock.FACING);
      IItemHandler handler = bookshelf.bookItems;
      
      p_112346_.pushPose();
      p_112346_.translate(0.5D, 0.5D, 0.5D);
      float f = -direction.toYRot() + 180.0F;
      p_112346_.mulPose(Vector3f.YP.rotationDegrees(f));

      float scale = 0.4F;//0.55F;
      float width = 0.65F;
      p_112346_.scale(width, scale, scale);      
      p_112346_.translate(0.60D, -0.4D/*-0.75D*/, 0.85D/*0.6D*/);      
      
      int slot = 0;
      if(handler != null) {
    	  for(int y = 0; y < 3; y++) {
    		  for(int x = 0; x < 8; x++) {
    			  if(slot >= handler.getSlots())continue;
    			  ItemStack book = handler.getStackInSlot(slot);
    			  if(!book.isEmpty()) {
    				  double offsetX = 0.17D;
    				  p_112346_.pushPose();
    				  p_112346_.translate(-offsetX * x, -y * 0.8D, 0.0D);
    				  if(book.is(ItemInit.SINGLE_ENCHANTMENT_ITEM.get())) {
    					  p_112346_.scale(0.8F, 0.95F, 1.25F);
    					  p_112346_.translate(0.0D, 0.05D, 0.0D);
    					  VertexConsumer vertexconsumer = VertexMultiConsumer.create(p_112347_.getBuffer(RenderType.entityGlintDirect()), SCROLL_LOCATION.buffer(p_112347_, RenderType::entitySolid)); 
	    				  this.scrollModel.renderToBuffer(p_112346_, vertexconsumer, p_112348_, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    				  }
    				  else {
	    				  //VertexConsumer vertexconsumer = enchanted ? VertexMultiConsumer.create(p_112347_.getBuffer(RenderType.entityGlintDirect()), BOOK_LOCATION.buffer(p_112347_, RenderType::entitySolid)) : BOOK_LOCATION.buffer(p_112347_, RenderType::entitySolid);
	    				  //this.bookModel.renderToBuffer(p_112346_, vertexconsumer, p_112348_, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
	    				  //this.bookModel.render(p_112346_, vertexconsumer, p_112348_, p_112349_, 1.0F, 1.0F, 1.0F, 1.0F);
	    				  boolean enchanted = book.is(Items.ENCHANTED_BOOK);
	    				  VertexConsumer vertexconsumer = enchanted ? VertexMultiConsumer.create(p_112347_.getBuffer(RenderType.entityGlintDirect()), ENCHANTED_BOOK_LOCATION.buffer(p_112347_, RenderType::entitySolid)) : BOOK_LOCATION.buffer(p_112347_, RenderType::entitySolid); 
	    				  this.bookModel.renderToBuffer(p_112346_, vertexconsumer, p_112348_, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    				  }
    				  p_112346_.popPose();
    			  }
    			  slot++;
    		  }
    	  }
      }
      
      p_112346_.popPose();
   }

   public static void register() {
       BlockEntityRenderers.register(BlockInit.ENCHANTMENT_BOOKSHELF_BE.get(), EnchantmentBookshelfBERenderer::new);
   }
}
