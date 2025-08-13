package alec_wam.wam_utils.client.integration.jei;

import java.util.ArrayList;
import java.util.List;

import alec_wam.wam_utils.common.blocks.conveyor.extractor.menu.ItemExtractorScreen;
import alec_wam.wam_utils.network.FilterSlotPayload;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class ExtractorFakeSlotJEIHandler implements IGhostIngredientHandler<ItemExtractorScreen> {
    @Override
    public <I> List<Target<I>> getTargetsTyped(ItemExtractorScreen gui, ITypedIngredient<I> ingredient, boolean doStart) {
        List<Target<I>> targets = new ArrayList<>();

        Slot slot = gui.getMenu().slots.get(0);
        if (!slot.isActive()) {
            return targets;
        }

        Rect2i bounds = new Rect2i(gui.getGuiLeft() + slot.x, gui.getGuiTop() + slot.y, 16, 16); //RS Had this as 17 17

        if (ingredient.getIngredient() instanceof ItemStack) {
            targets.add(new Target<I>() {
                @Override
                public Rect2i getArea() {
                    return bounds;
                }

                @Override
                public void accept(I ingredient) {
                    slot.setByPlayer((ItemStack) ingredient);
                    ClientPacketDistributor.sendToServer(new FilterSlotPayload(slot.index, (ItemStack) ingredient, ((ItemStack) ingredient).getCount()));
                }
            });
        }
        return targets;
    }

    @Override
    public void onComplete() {
        // NO OP
    }
}
