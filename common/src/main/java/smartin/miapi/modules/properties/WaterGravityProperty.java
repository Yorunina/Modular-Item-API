package smartin.miapi.modules.properties;

import dev.architectury.event.EventResult;
import smartin.miapi.events.MiapiEvents;
import smartin.miapi.modules.properties.util.DoubleProperty;

/**
 * This property allows for armor penetration, so weapons can igonre some armor
 */
public class WaterGravityProperty extends DoubleProperty {
    public static final String KEY = "water_gravity";
    public static WaterGravityProperty property;

    public WaterGravityProperty() {
        super(KEY);
        property = this;
        MiapiEvents.PLAYER_TICK_START.register((player -> {
            if (player.isUnderWater() && player.isControlledByLocalInstance()) {
                double speed = property.getForItems(player.getAllSlots()) / 100;
                if (player.getDeltaMovement().y < speed) {
                    player.push(0, -speed / 20, 0);
                }
            }
            return EventResult.pass();
        }));
    }
}
