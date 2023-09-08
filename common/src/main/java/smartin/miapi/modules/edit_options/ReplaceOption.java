package smartin.miapi.modules.edit_options;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import smartin.miapi.Miapi;
import smartin.miapi.client.gui.InteractAbleWidget;
import smartin.miapi.client.gui.crafting.crafter.CraftEditOption;
import smartin.miapi.client.gui.crafting.CraftingScreen;
import smartin.miapi.craft.CraftAction;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ReplaceOption implements EditOption {
    @Override
    public ItemStack preview(PacketByteBuf buffer, EditContext editContext) {
        CraftAction action = new CraftAction(buffer, editContext.getWorkbench());
        action.setItem(editContext.getLinkedInventory().getStack(0));
        action.linkInventory(editContext.getLinkedInventory(), 1);
        return action.getPreview();
    }

    @Override
    public ItemStack execute(PacketByteBuf buffer, EditContext editContext) {
        CraftAction action = new CraftAction(buffer, editContext.getWorkbench());
        action.setItem(editContext.getLinkedInventory().getStack(0));
        action.linkInventory(editContext.getLinkedInventory(), 1);
        if (action.canPerform()) {
            return action.perform();
        } else {
            Miapi.LOGGER.warn("Could not previewStack Craft Action. This might indicate an exploit by " + editContext.getPlayer().getUuidAsString());
            return editContext.getItemstack();
        }
    }

    @Override
    public boolean isVisible(EditContext editContext) {
        return editContext.getSlot() != null;
    }

    @Override
    public InteractAbleWidget getGui(int x, int y, int width, int height, EditContext editContext) {
        return new CraftEditOption(x, y, width, height, editContext);
    }

    @Override
    public InteractAbleWidget getIconGui(int x, int y, int width, int height, Consumer<EditOption> select, Supplier<EditOption> getSelected) {
        return new EditOptionIcon(x, y, width, height, select, getSelected, CraftingScreen.BACKGROUND_TEXTURE, 339 + 32, 25, 512, 512, this);
    }
}
