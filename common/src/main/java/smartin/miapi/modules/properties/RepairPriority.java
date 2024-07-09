package smartin.miapi.modules.properties;

import net.minecraft.world.item.ItemStack;
import smartin.miapi.modules.ItemModule;
import smartin.miapi.modules.ModuleInstance;
import smartin.miapi.modules.cache.ModularItemCache;
import smartin.miapi.modules.material.Material;
import smartin.miapi.modules.material.MaterialProperty;
import smartin.miapi.modules.properties.util.DoubleProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * controls the repair material
 */
public class RepairPriority extends DoubleProperty {
    public static RepairPriority property;
    public static final String KEY = "repairPriority";

    public RepairPriority() {
        super(KEY);
        property = this;
        allowVisualOnly = true;
        ModularItemCache.setSupplier(KEY + "_materials", this::getRepairMaterialsPrivate);
    }

    public List<Material> getRepairMaterials(ItemStack itemStack) {
        return ModularItemCache.getVisualOnlyCache(itemStack, KEY + "_materials", new ArrayList<>());
    }

    public static double getRepairValue(ItemStack tool, ItemStack material) {
        double highestValue = 0;
        for (Material material1 : property.getRepairMaterials(tool)) {
            highestValue = Math.max(highestValue, material1.getValueOfItem(material));
        }
        return highestValue;
    }


    private List<Material> getRepairMaterialsPrivate(ItemStack itemStack) {
        double lowest = Double.MAX_VALUE;
        List<Material> materials = new ArrayList<>();
        for (ModuleInstance moduleInstance : ItemModule.getModules(itemStack).allSubModules()) {
            Optional<Double> optional = getValue(itemStack);
            if (optional.isPresent()) {
                Material material = MaterialProperty.getMaterial(moduleInstance);
                if (material != null && lowest > optional.get()) {
                    lowest = optional.get();
                }
            }
        }
        for (ModuleInstance moduleInstance : ItemModule.getModules(itemStack).allSubModules()) {
            Optional<Double> optional = getValue(itemStack);
            if (optional.isPresent()) {
                Material material = MaterialProperty.getMaterial(moduleInstance);
                if (material != null && Math.abs(lowest - optional.get()) < 0.001) {
                    materials.add(material);
                }
            }
        }
        return materials;
    }
}
