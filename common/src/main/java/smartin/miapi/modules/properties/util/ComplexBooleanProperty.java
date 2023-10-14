package smartin.miapi.modules.properties.util;

import net.minecraft.item.ItemStack;

public abstract class ComplexBooleanProperty extends DoubleProperty {
    boolean defaultValue;

    protected ComplexBooleanProperty(String key, boolean defaultValue) {
        super(key);
        this.defaultValue = defaultValue;
    }

    public boolean isTrue(ItemStack itemStack) {
        Double value = getValueRaw(itemStack);
        return value != null ? value > 0 : defaultValue;
    }


    public Double getValue(ItemStack stack) {
        return getValueRaw(stack);
    }

    public double getValueSafe(ItemStack stack) {
        return getValueSafeRaw(stack);
    }
}
