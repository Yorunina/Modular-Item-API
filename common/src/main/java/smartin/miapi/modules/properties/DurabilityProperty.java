package smartin.miapi.modules.properties;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import smartin.miapi.modules.properties.util.ComponentApplyProperty;
import smartin.miapi.modules.properties.util.DoubleProperty;

/**
 * This property manages the final durabiltiy of the item
 */
public class DurabilityProperty extends DoubleProperty implements ComponentApplyProperty {
    public static final String KEY = "durability";
    public static DurabilityProperty property;

    public DurabilityProperty() {
        super(KEY);
        property = this;
        allowVisualOnly = true;
    }

    @Override
    public void updateComponent(ItemStack itemStack) {
        itemStack.set(DataComponents.MAX_DAMAGE,getValue(itemStack).orElse(50.0).intValue());
    }
}
