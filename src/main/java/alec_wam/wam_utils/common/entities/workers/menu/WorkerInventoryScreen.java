package alec_wam.wam_utils.common.entities.workers.menu;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import alec_wam.wam_utils.WAMUtils;
import alec_wam.wam_utils.client.util.GuiHelper;
import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import alec_wam.wam_utils.common.entities.workers.WorkerFoodData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;

public class WorkerInventoryScreen extends AbstractContainerScreen<WorkerInventoryMenu> {
    
    public static final ResourceLocation WORKER_INVENTORY_TEXTURE = ResourceLocation.fromNamespaceAndPath(WAMUtils.MODID, "textures/gui/worker_inventory.png");

    /**
     * The old x position of the mouse pointer
     */
    private float xMouse;
    /**
     * The old y position of the mouse pointer
     */
    private float yMouse;

    private final WorkerEntity workerEntity;
    private final RandomSource random = RandomSource.create();
    private int tickCount = 0;

    public WorkerInventoryScreen(WorkerInventoryMenu menu, Inventory playerInventory, Component title) {
        super(
            menu,
            playerInventory,
            Component.translatable("wamutils.container.worker.title")
        );
        this.workerEntity = menu.workerEntity;
        this.titleLabelX = 80;
        this.titleLabelY = 10;
        this.inventoryLabelY = 104;
        this.imageHeight = 198;
    }

    @Override
    public void containerTick(){
        super.containerTick();
        this.tickCount++;
    }

    @Override
    public void render(GuiGraphics p_283246_, int p_98876_, int p_98877_, float p_98878_) {
        super.render(p_283246_, p_98876_, p_98877_, p_98878_);
        
        int heartX = this.leftPos + 100;
        int heartY = this.topPos + 58;

        p_283246_.pose().pushMatrix();
        p_283246_.pose().translate(heartX, heartY);
        p_283246_.pose().scale(0.8f);
        // TODO add ticking effects to hearts
        GuiHelper.renderHearts(p_283246_, workerEntity, 0, 0, 10.0F, this.random);
        GuiHelper.renderArmor(p_283246_, workerEntity, 0, -1, 1, 0);
        
        WorkerFoodData foodData = workerEntity.getFoodData();
        if(foodData != null){
            GuiHelper.renderFood(p_283246_, workerEntity, 82, 11, foodData.getFoodLevel(), foodData.getSaturationLevel(), this.tickCount, random);
        }
        
        p_283246_.pose().popMatrix();
        
        this.xMouse = p_98876_;
        this.yMouse = p_98877_;
        this.renderTooltip(p_283246_, p_98876_, p_98877_);
    }

    @Override
    protected void renderBg(GuiGraphics p_281500_, float p_281299_, int p_283481_, int p_281831_) {
        int i = this.leftPos;
        int j = this.topPos;
        p_281500_.blit(RenderPipelines.GUI_TEXTURED, WORKER_INVENTORY_TEXTURE, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        if(this.workerEntity !=null){
            InventoryScreen.renderEntityInInventoryFollowsMouse(p_281500_, i + 26, j + 8, i + 75, j + 78, 30, 0.0625F * 8.0F, this.xMouse, this.yMouse, this.workerEntity);
        }
    }

    public static void renderEntityInInventoryFollowsMouse(
        GuiGraphics graphics,
        int x1,
        int y1,
        int x2,
        int y2,
        int scaleInt,
        float yOffset,
        float mouseX,
        float mouseY,
        LivingEntity entity,
        float entityScale
    ) {
        float f = (x1 + x2) / 2.0F;
        float f1 = (y1 + y2) / 2.0F;
        float angleXComponent = (float)Math.atan((f - mouseX) / 40.0F);
        float angleYComponent = (float)Math.atan((f1 - mouseY) / 40.0F);

        // float f = (x1 + x2) / 2.0F;
        // float f1 = (y1 + y2) / 2.0F;
        graphics.enableScissor(x1, y1, x2, y2);
        float f2 = angleXComponent;
        float f3 = angleYComponent;
        Quaternionf quaternionf = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf quaternionf1 = new Quaternionf().rotateX(f3 * 20.0F * (float) (Math.PI / 180.0));
        quaternionf.mul(quaternionf1);
        float f4 = entity.yBodyRot;
        float f5 = entity.getYRot();
        float f6 = entity.getXRot();
        float f7 = entity.yHeadRotO;
        float f8 = entity.yHeadRot;
        entity.yBodyRot = 180.0F + f2 * 20.0F;
        entity.setYRot(180.0F + f2 * 40.0F);
        entity.setXRot(-f3 * 20.0F);
        entity.yHeadRot = entity.getYRot();
        entity.yHeadRotO = entity.getYRot();
        float f9 = entityScale;
        Vector3f vector3f = new Vector3f(0.0F, entity.getBbHeight() / 2.0F + yOffset * f9, 0.0F);
        float f10 = scaleInt / f9;
        InventoryScreen.renderEntityInInventory(graphics, x1, y1, x2, y2, f10, vector3f, quaternionf, quaternionf1, entity);
        entity.yBodyRot = f4;
        entity.setYRot(f5);
        entity.setXRot(f6);
        entity.yHeadRotO = f7;
        entity.yHeadRot = f8;
        graphics.disableScissor();
    }

}
