package alec_wam.wam_utils.blocks.item_analyzer;

import java.util.stream.Stream;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.blocks.item_analyzer.ItemAnalyzerBE.ItemAnalysisResult;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public class ItemAnalyzerScreen extends AbstractContainerScreen<ItemAnalyzerContainer> {

    private final ResourceLocation GUI = new ResourceLocation(WAMUtilsMod.MODID, "textures/gui/item_analyzer.png");

    private ItemAnalyzerBE blockEntity;    
    private int textListOffset = 0;
    
    public ItemAnalyzerScreen(ItemAnalyzerContainer container, Inventory inv, Component name) {
        super(container, inv, name);
        this.imageWidth = 176;
        this.imageHeight = 222;
        
        this.blockEntity = container.blockEntity;
    }

    @Override
    protected void init() {
    	super.init();
    	
    	int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        
        /*RedstoneModeButton redstoneButton = new RedstoneModeButton(this, relX + 152, relY + 6, 16, 16, remover.getRedstoneMode()){

			@Override
			public void onPress() {
				super.onPress();
				sendRedstoneModeUpdate(getRestoneMode());
			}
        	
        };
        this.addRenderableWidget(redstoneButton);*/
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
    	String finalString = buildItemTextList();        
        String[] lines = finalString.split("\n");
        int itemCount = lines.length;
		if(scroll < 0.0F) {
			if(this.textListOffset < itemCount - 1) {
				this.textListOffset++;
			}
		}
		if(scroll > 0.0F) {
			if(this.textListOffset > 0) {
				this.textListOffset--;
			}
		}
    	return super.mouseScrolled(mouseX, mouseY, scroll);
    }

    @Override
    public boolean keyPressed(int p_97765_, int p_97766_, int p_97767_) {
    	
    	
    	return super.keyPressed(p_97765_, p_97766_, p_97767_);
    }
    
    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
        
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        
        String finalString = buildItemTextList();
        
        String[] lines = finalString.split("\n");
        int itemCount = lines.length;
    	int maxIndex = Math.min(this.textListOffset + 11, itemCount);
    	int realLines = 0;
        for(int l = this.textListOffset; l < maxIndex; l++) {
        	matrixStack.pushPose();
        	Component itemText = Component.literal(lines[l]);
        	float strWidth = 138.0F / this.font.getSplitter().stringWidth(lines[l]);       
        	float scale = Math.min(strWidth, 1.0F);
        	float scaleOffset = scale > 1.0F ? this.font.lineHeight * scale : 0.0F;
        	float renderX = relX + 28;
        	float renderY = relY + 20 + (this.font.lineHeight * realLines) + scaleOffset;
        	matrixStack.translate(renderX, renderY, 0.0F);
        	matrixStack.scale(scale, scale, 1.0F);
        	this.font.draw(matrixStack, itemText, 0, 0, 0x000000);
        	matrixStack.popPose();
        	realLines++;
        }
    }
    
    @SuppressWarnings("deprecation")
	private String buildItemTextList() {
    	 StringBuilder builder = new StringBuilder();
         boolean showNBT = true;
         boolean showTags = true;
         if(blockEntity.itemResult != null && blockEntity.itemResult != ItemAnalysisResult.EMPTY) {
         	ItemAnalysisResult result = blockEntity.itemResult;
         	ItemStack stack = result.getItemStack();
         	builder.append(result.getItemID().toString() + "\n");
         	builder.append("\n");
         	builder.append("Properties:");
         	builder.append("\n");        	
         	builder.append(convertItemPropsToString(result.getItemStack()));
         	builder.append("\n");        	
         	if(result.getItemNBT() !=null) {
         		builder.append("NBT:");
         		if(showNBT) {
         			builder.append("\n");
         			builder.append(NbtUtils.prettyPrint(result.getItemNBT(), true)+"\n");
         		}
             	builder.append("\n");
         	}
         	builder.append("Tags:");
         	builder.append("\n");
         	Stream<TagKey<Item>> tags = stack.isEmpty() ? null : stack.getItem().builtInRegistryHolder().tags();
         	if(tags !=null) {
         		if(showTags) {
         			tags.forEach((tag) -> {
 	        			builder.append("- " + tag.location().toString() + "\n");
 	        		});
         		}
         		else {
         			builder.append("Hidden" + "\n");
         		}
         	}
         	else {
         		builder.append("Empty" + "\n");
         	}
         }
         return builder.toString();
    }
    
    private String convertItemPropsToString(ItemStack stack) {
    	Item item = stack.getItem();
    	String string = "";
    	if(item.getItemCategory() !=null) {
    		string += "- Creative Tab: " + item.getItemCategory().getDisplayName().getString() + "\n";
    	}
    	Rarity rarity = item.getRarity(stack);
    	if(rarity !=null) {
    		string += "- Rarity: " + Component.literal(rarity.name()).withStyle(rarity.getStyleModifier()).getString() + "\n";
    	}
    	ItemStack craftingRemainder = item.getCraftingRemainingItem(stack);
    	if(!craftingRemainder.isEmpty()) {
    		string += "- Crafting Remainder: " + craftingRemainder.getItem().getDescriptionId(craftingRemainder) + "\n";
    	}
    	if(item.isDamageable(stack)) {
    		string += "- Durability: " + (item.getMaxDamage(stack) - item.getDamage(stack)) + "\n";
    	}
    	if(item.isDamageable(stack)) {
    		string += "- Damage: " + item.getDamage(stack) + " / " + item.getMaxDamage(stack) + "\n";
    	}
    	string += "- Max Stack Size: " + item.getMaxStackSize(stack) + "\n";
    	if(item.isFireResistant()) {
    		string += "- Fire Resistant\n";
    	}
    	if(item.isRepairable(stack)) {
    		string += "- Repairable\n";
    	}
    	//TODO Add Food Props
    	return string;
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        //drawString(matrixStack, Minecraft.getInstance().font, "Energy: " + menu.getEnergy(), 10, 10, 0xffffff);
    }

    @Override
    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, GUI);
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        this.blit(matrixStack, relX, relY, 0, 0, this.imageWidth, this.imageHeight); 
    }
}
