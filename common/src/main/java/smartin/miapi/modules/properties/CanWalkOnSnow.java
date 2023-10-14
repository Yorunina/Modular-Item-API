package smartin.miapi.modules.properties;

import net.minecraft.item.ItemStack;
import smartin.miapi.modules.properties.util.ComplexBooleanProperty;

/**
 * This Property allows to designate the Item as Tool or Weapon,
 * mainly affecting durability calculations
 */
public class CanWalkOnSnow extends ComplexBooleanProperty {
    public static final String KEY = "canWalkOnSnow";
    public static CanWalkOnSnow property;

    public CanWalkOnSnow() {
        super(KEY, false);
        property = this;
    }

    public static boolean canSnowWalk(ItemStack stack) {
        return property.isTrue(stack);
    }
}
