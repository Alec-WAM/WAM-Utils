package alec_wam.wam_utils.client.util;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class GuiHelper {
    private static final ResourceLocation ARMOR_EMPTY_SPRITE = ResourceLocation.withDefaultNamespace("hud/armor_empty");
    private static final ResourceLocation ARMOR_HALF_SPRITE = ResourceLocation.withDefaultNamespace("hud/armor_half");
    private static final ResourceLocation ARMOR_FULL_SPRITE = ResourceLocation.withDefaultNamespace("hud/armor_full");
    private static final ResourceLocation FOOD_EMPTY_HUNGER_SPRITE = ResourceLocation.withDefaultNamespace("hud/food_empty_hunger");
    private static final ResourceLocation FOOD_HALF_HUNGER_SPRITE = ResourceLocation.withDefaultNamespace("hud/food_half_hunger");
    private static final ResourceLocation FOOD_FULL_HUNGER_SPRITE = ResourceLocation.withDefaultNamespace("hud/food_full_hunger");
    private static final ResourceLocation FOOD_EMPTY_SPRITE = ResourceLocation.withDefaultNamespace("hud/food_empty");
    private static final ResourceLocation FOOD_HALF_SPRITE = ResourceLocation.withDefaultNamespace("hud/food_half");
    private static final ResourceLocation FOOD_FULL_SPRITE = ResourceLocation.withDefaultNamespace("hud/food_full");
    
    public static void renderHearts(
        GuiGraphics guiGraphics,
        LivingEntity entity,
        int x,
        int y,
        float heartCount,
        RandomSource random
    ) {
        int i = Mth.ceil(entity.getHealth());
        float f = Math.max((float)entity.getAttributeValue(Attributes.MAX_HEALTH), (float)i);
        int k1 = Mth.ceil(entity.getAbsorptionAmount());
        int l1 = Mth.ceil((f + k1) / 2.0F / heartCount);
        int i2 = Math.max(10 - (l1 - 2), 3);

        renderHearts(guiGraphics, entity, x, y, i2, -1, f, i, i, k1, false, random);            
    }

    public static void renderHearts(
        GuiGraphics guiGraphics,
        LivingEntity entity,
        int x,
        int y,
        int height,
        int offsetHeartIndex,
        float maxHealth,
        int currentHealth,
        int displayHealth,
        int absorptionAmount,
        boolean renderHighlight,
        RandomSource random
    ) {
        Gui.HeartType gui$hearttype = getHeartType(entity);
        boolean isHardcore = false;
        int i = Mth.ceil(maxHealth / 2.0);
        int j = Mth.ceil(absorptionAmount / 2.0);
        int k = i * 2;

        for (int l = i + j - 1; l >= 0; l--) {
            int i1 = l / 10;
            int j1 = l % 10;
            int k1 = x + j1 * 8;
            int l1 = y - i1 * height;
            if (currentHealth + absorptionAmount <= 4) {
                l1 += random.nextInt(2);
            }

            if (l < i && l == offsetHeartIndex) {
                l1 -= 2;
            }

            renderHeart(guiGraphics, Gui.HeartType.CONTAINER, k1, l1, isHardcore, renderHighlight, false);
            int i2 = l * 2;
            boolean flag1 = l >= i;
            if (flag1) {
                int j2 = i2 - k;
                if (j2 < absorptionAmount) {
                    boolean flag2 = j2 + 1 == absorptionAmount;
                    renderHeart(guiGraphics, gui$hearttype == Gui.HeartType.WITHERED ? gui$hearttype : Gui.HeartType.ABSORBING, k1, l1, isHardcore, false, flag2);
                }
            }

            if (renderHighlight && i2 < displayHealth) {
                boolean flag3 = i2 + 1 == displayHealth;
                renderHeart(guiGraphics, gui$hearttype, k1, l1, isHardcore, true, flag3);
            }

            if (i2 < currentHealth) {
                boolean flag4 = i2 + 1 == currentHealth;
                renderHeart(guiGraphics, gui$hearttype, k1, l1, isHardcore, false, flag4);
            }
        }
    }

    public static Gui.HeartType getHeartType(LivingEntity entity) {
        Gui.HeartType gui$hearttype;
        if (entity.hasEffect(MobEffects.POISON)) {
            gui$hearttype = Gui.HeartType.POISIONED;
        } else if (entity.hasEffect(MobEffects.WITHER)) {
            gui$hearttype = Gui.HeartType.WITHERED;
        } else if (entity.isFullyFrozen()) {
            gui$hearttype = Gui.HeartType.FROZEN;
        } else {
            gui$hearttype = Gui.HeartType.NORMAL;
        }

        return gui$hearttype;
    }

    public static void renderHeart(
        GuiGraphics guiGraphics, Gui.HeartType heartType, int x, int y, boolean hardcore, boolean halfHeart, boolean blinking
    ) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, heartType.getSprite(hardcore, blinking, halfHeart), x, y, 9, 9);
    }

    public static void renderArmor(GuiGraphics guiGraphics, LivingEntity entity, int x, int y, int heartRows, int height) {
        int i = entity.getArmorValue();
        if (i > 0) {
            int j = y - (heartRows - 1) * height - 10;

            for (int k = 0; k < 10; k++) {
                int l = x + k * 8;
                if (k * 2 + 1 < i) {
                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARMOR_FULL_SPRITE, l, j, 9, 9);
                }

                if (k * 2 + 1 == i) {
                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARMOR_HALF_SPRITE, l, j, 9, 9);
                }

                if (k * 2 + 1 > i) {
                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARMOR_EMPTY_SPRITE, l, j, 9, 9);
                }
            }
        }
    }

    public static void renderFood(GuiGraphics guiGraphics, LivingEntity entity, int x, int y, int foodLevel, float saturation, int tickCount, RandomSource random) {
        for (int j = 0; j < 10; j++) {
            int k = y;
            ResourceLocation resourcelocation;
            ResourceLocation resourcelocation1;
            ResourceLocation resourcelocation2;
            if (entity.hasEffect(MobEffects.HUNGER)) {
                resourcelocation = FOOD_EMPTY_HUNGER_SPRITE;
                resourcelocation1 = FOOD_HALF_HUNGER_SPRITE;
                resourcelocation2 = FOOD_FULL_HUNGER_SPRITE;
            } else {
                resourcelocation = FOOD_EMPTY_SPRITE;
                resourcelocation1 = FOOD_HALF_SPRITE;
                resourcelocation2 = FOOD_FULL_SPRITE;
            }

            if (saturation <= 0.0F && tickCount % (foodLevel * 3 + 1) == 0) {
                k = y + (random.nextInt(3) - 1);
            }

            int l = x - j * 8 - 9;
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, resourcelocation, l, k, 9, 9);
            if (j * 2 + 1 < foodLevel) {
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, resourcelocation2, l, k, 9, 9);
            }

            if (j * 2 + 1 == foodLevel) {
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, resourcelocation1, l, k, 9, 9);
            }
        }
    }

}
