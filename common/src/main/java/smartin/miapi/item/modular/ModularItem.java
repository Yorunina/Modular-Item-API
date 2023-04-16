package smartin.miapi.item.modular;

import com.google.gson.JsonElement;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import smartin.miapi.Miapi;
import smartin.miapi.item.modular.cache.ModularItemCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModularItem extends Item {
    public static final String moduleKey = "modules";
    public static final String propertyKey = "rawProperties";

    public ModularItem() {
        super(new Item.Settings());
    }

    public static ItemModule.ModuleInstance getModules(ItemStack stack) {
        ItemModule.ModuleInstance moduleInstance = (ItemModule.ModuleInstance) ModularItemCache.get(stack, moduleKey);
        if (moduleInstance == null || moduleInstance.module == null) {
            Miapi.LOGGER.warn("Item has Invalid Module setup - treating it like it has no modules");
            return new ItemModule.ModuleInstance(new ItemModule("empty", new HashMap<>()));
        }
        return moduleInstance;
    }

    public static Map<String, List<JsonElement>> getUnmergedProperties(ItemModule.ModuleInstance modules) {
        Map<String, List<JsonElement>> unmergedProperties = new HashMap<>();
        for (ItemModule.ModuleInstance module : modules.subModules.values()) {
            module.getProperties().forEach((property, data) -> {
                String key = Miapi.moduleRegistry.findKey(property);
                unmergedProperties.getOrDefault(key, new ArrayList<>()).add(data);
            });
        }
        return unmergedProperties;
    }
}
