package alec_wam.wam_utils.blocks.machine.stone_factory;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.blocks.RedstoneMode;
import alec_wam.wam_utils.client.GuiUtils;
import alec_wam.wam_utils.client.widgets.RedstoneModeButton;
import alec_wam.wam_utils.recipe.StoneFactoryRecipe;
import alec_wam.wam_utils.server.network.MessageBlockEntityUpdate;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

public class StoneFactoryScreen extends AbstractContainerScreen<StoneFactoryContainer> {

    private final ResourceLocation GUI = new ResourceLocation(WAMUtilsMod.MODID, "textures/gui/stone_factory.png");

    private StoneFactoryBE factory;
    
    public StoneFactoryScreen(StoneFactoryContainer container, Inventory inv, Component name) {
        super(container, inv, name);
        
        this.factory = container.blockEntity;
    }

    @Override
    public void init() {
    	super.init();
    	int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        
        RedstoneModeButton redstoneButton = new RedstoneModeButton(this, relX + 152, relY + 6, 16, 16, factory.getRedstoneMode()){

			@Override
			public void onPress() {
				super.onPress();
				sendRedstoneModeUpdate(getRestoneMode());
			}
        	
        };
        this.addRenderableWidget(redstoneButton);
        
        int renderStackX = relX + 124;
		int renderStackY = relY + 58;
        
        Button prevRecipeButton = new Button(renderStackX - 12, renderStackY, 10, 20, Component.literal("<"), new Button.OnPress() {
			
			@Override
			public void onPress(Button p_93751_) {
				StoneFactoryRecipe recipe = menu.getPrevRecipe();
				sendRecipeUpdate(recipe);
			}
		});
        this.addRenderableWidget(prevRecipeButton);
        
        Button nextRecipeButton = new Button(renderStackX + 20, renderStackY, 10, 20, Component.literal(">"), new Button.OnPress() {
			
			@Override
			public void onPress(Button p_93751_) {
				StoneFactoryRecipe recipe = menu.getNextRecipe();
				sendRecipeUpdate(recipe);
			}
		});
        this.addRenderableWidget(nextRecipeButton);
    }
    
    public void sendRedstoneModeUpdate(RedstoneMode mode)
	{
    	factory.setRedstoneMode(mode);;
		CompoundTag message = new CompoundTag();
		message.putInt("RedstoneMode", mode.ordinal());
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(factory, message));
	}
    
    public void sendRecipeUpdate(StoneFactoryRecipe recipe)
	{
    	factory.setSelectedRecipe(recipe);
		CompoundTag message = new CompoundTag();
		message.putString("RecipeID", recipe.getId().toString());
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(factory, message));
	}
    
    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
        
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        int tankX = relX + 51;
    	int tankY = relY + 9;
        if(mouseX >= tankX && mouseX <= tankX + 16 && mouseY >= tankY && mouseY <= tankY + 68) {
        	List<Component> textComponents = new ArrayList<>();
        	
        	FluidStack fluid = factory.fluidStorageWater.getFluid();        	
        	if(!fluid.isEmpty()) {
    			textComponents.add(fluid.getDisplayName());
    		}
        	String fluidValue = String.format("%,d", factory.fluidStorageWater.getFluidAmount());
        	String maxFluidValue = String.format("%,d", factory.fluidStorageWater.getCapacity());
    		textComponents.add(Component.literal(fluidValue + " / " + maxFluidValue + " mB"));
        	Optional<TooltipComponent> tooltipComponent = java.util.Optional.empty();
        	this.renderTooltip(matrixStack, textComponents, tooltipComponent, mouseX, mouseY);
        }
        
        tankX = relX + 71;
    	tankY = relY + 9;
        if(mouseX >= tankX && mouseX <= tankX + 16 && mouseY >= tankY && mouseY <= tankY + 68) {
        	List<Component> textComponents = new ArrayList<>();
        	
        	FluidStack fluid = factory.fluidStorageLava.getFluid();        	
        	if(!fluid.isEmpty()) {
    			textComponents.add(fluid.getDisplayName());
    		}
        	String fluidValue = String.format("%,d", factory.fluidStorageLava.getFluidAmount());
        	String maxFluidValue = String.format("%,d", factory.fluidStorageLava.getCapacity());
    		textComponents.add(Component.literal(fluidValue + " / " + maxFluidValue + " mB"));
        	Optional<TooltipComponent> tooltipComponent = java.util.Optional.empty();
        	this.renderTooltip(matrixStack, textComponents, tooltipComponent, mouseX, mouseY);
        }
        
        int energyBarX = relX + 17;
        int energyBarY = relY + 9;
        if(mouseX >= energyBarX && mouseX <= energyBarX + 16 && mouseY >= energyBarY && mouseY <= energyBarY + 68) {
        	List<Component> textComponents = new ArrayList<>();
        	String energyValue = String.format("%,d", factory.energyStorage.getEnergyStored());
        	String maxEnergyValue = String.format("%,d", factory.getEnergyCapacity());
        	textComponents.add(Component.literal(energyValue + " / " + maxEnergyValue));
        	Optional<TooltipComponent> tooltipComponent = java.util.Optional.empty();
        	this.renderTooltip(matrixStack, textComponents, tooltipComponent, mouseX, mouseY);
        }
        int renderStackX = relX + 124 + 1;
		int renderStackY = relY + 58 + 2;
        if(menu.getCurrentRecipe() != null) {
        	ItemStack output = menu.getCurrentRecipe().getResultItem();
        	int waterAmount = menu.getCurrentRecipe().getWaterAmount();
        	int lavaAmount = menu.getCurrentRecipe().getLavaAmount();
        	if(!output.isEmpty()) {
        		this.itemRenderer.renderGuiItem(output, renderStackX, renderStackY);
        		if(mouseX > renderStackX && mouseX < renderStackX + 16 && mouseY > renderStackY && mouseY < renderStackY + 16) {
        			List<Component> textComponents = this.getTooltipFromItem(output);
        			
        			textComponents.add(Component.empty());
        			textComponents.add(Component.literal("Recipe:"));
        			
        			String translatedWater = Component.translatable(Fluids.WATER.getFluidType().getDescriptionId()).getString();
        			textComponents.add(Component.literal(translatedWater + ": " + waterAmount + "mB"));
        			String translatedLava = Component.translatable(Fluids.LAVA.getFluidType().getDescriptionId()).getString();
        			textComponents.add(Component.literal(translatedLava + ": " + lavaAmount + "mB"));
        			Optional<TooltipComponent> tooltipComponent = output.getTooltipImage();
        			
        			this.renderTooltip(matrixStack, textComponents, tooltipComponent, mouseX, mouseY, null, output);
        		}
        	}
        }
        else {
        	this.font.draw(matrixStack, "?", renderStackX + 5.0F, renderStackY + 4.5F, 0);
        }
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
    	
        int craftingProgressX = relX + 92;
    	int craftingProgressY = relY + 32;
    	float tradeProgress = (float)(factory.getCraftingTime()) / (float)factory.getMaxCraftingTime();
        int tradeWidth = (int)(tradeProgress * 24.0F);
        if(tradeProgress > 0.0F) {
        	this.blit(matrixStack, craftingProgressX, craftingProgressY, 176, 0, tradeWidth, 17);
        }
        
        int energyBarX = relX + 17;
        int energyBarY = relY + 9;
        float energyRatio = (float)(factory.energyStorage.getEnergyStored()) / (float)factory.getEnergyCapacity();
        int energyHeight = (int)(energyRatio * 68.0F);
        if(energyRatio > 0) {
        	Screen.fill(matrixStack, energyBarX, energyBarY + 68, energyBarX + 16, energyBarY + 68 - energyHeight, Color.red.getRGB());
        }
        
    	int tankX = relX + 51;
    	int tankY = relY + 9;        
        float fluidRatio = (float)(factory.fluidStorageWater.getFluidAmount()) / (float)factory.fluidStorageWater.getCapacity();
        int tankFluidHeight = (int)(fluidRatio * 68.0F);
        if(factory.fluidStorageWater.getFluidAmount() > 0) {
        	FluidStack fluid = factory.fluidStorageWater.getFluid();    	
        	matrixStack.pushPose();
        	MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        	GuiUtils.drawRepeatedFluidSprite(matrixStack, fluid, tankX, tankY + 68 - tankFluidHeight, 16, tankFluidHeight);
        	buffer.endBatch();
        	matrixStack.popPose();
        }
        
        tankX = relX + 71;
    	tankY = relY + 9;        
        fluidRatio = (float)(factory.fluidStorageLava.getFluidAmount()) / (float)factory.fluidStorageLava.getCapacity();
        tankFluidHeight = (int)(fluidRatio * 68.0F);
        if(factory.fluidStorageLava.getFluidAmount() > 0) {
        	FluidStack fluid = factory.fluidStorageLava.getFluid();    	
        	matrixStack.pushPose();
        	MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        	GuiUtils.drawRepeatedFluidSprite(matrixStack, fluid, tankX, tankY + 68 - tankFluidHeight, 16, tankFluidHeight);
        	buffer.endBatch();
        	matrixStack.popPose();
        }
    }
}
