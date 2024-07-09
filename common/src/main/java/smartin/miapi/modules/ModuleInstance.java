package smartin.miapi.modules;

import com.google.gson.Gson;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import smartin.miapi.item.modular.PropertyResolver;
import smartin.miapi.modules.cache.DataCache;
import smartin.miapi.modules.cache.ModularItemCache;
import smartin.miapi.modules.properties.util.MergeType;
import smartin.miapi.modules.properties.util.ModuleProperty;
import smartin.miapi.registries.RegistryInventory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A class representing a single module instance that belongs to an item.
 */
public class ModuleInstance {
    public static Codec<ModuleInstance> CODEC;
    public static DataComponentType<ModuleInstance> componentType;

    public Map<String, Object> cachedData = new ConcurrentHashMap<>();
    public Map<String, Object> itemStackCache = new ConcurrentHashMap<>();

    static {
        Codec<Map<String, ModuleInstance>> mapCodec =
                Codec.unboundedMap(Codec.STRING, CODEC).xmap((i) -> i, Function.identity());
        Codec<Map<String, String>> dataCodec = Codec.unboundedMap(Codec.STRING, Codec.STRING).xmap((i) -> i, Function.identity());


        CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(
                        Codec.STRING.fieldOf("key").forGetter((moduleInstance) -> moduleInstance.module.name()),
                        mapCodec.fieldOf("child").forGetter((moduleInstance) -> moduleInstance.subModules),
                        dataCodec.fieldOf("data").forGetter((moduleInstance) -> moduleInstance.moduleData)
                ).apply(instance, (module, children, data) -> {
                    ModuleInstance moduleInstance = new ModuleInstance(RegistryInventory.modules.get(module));
                    moduleInstance.moduleData = data;
                    moduleInstance.subModules = children;
                    moduleInstance.subModules.values().forEach(childInstance -> childInstance.parent = moduleInstance);
                    return moduleInstance;
                }));
        componentType = DataComponentType.<ModuleInstance>builder().persistent(CODEC).build();
    }

    /**
     * The item module represented by this module instance.
     */
    public ItemModule module;
    /**
     * The parent module instance of this module instance, if any.
     */
    @Nullable
    public ModuleInstance parent;
    /**
     * A map of child module instances to their respective module IDs.
     */
    public Map<String, ModuleInstance> subModules = new HashMap<>();
    /**
     * A map of module data keys to their respective values.
     */
    public Map<String, String> moduleData = new HashMap<>();


    /**
     * A map of the raw properties.
     * Only access this when you know what you are doing.
     * Use {@link ModuleInstance#getProperty(ModuleProperty)} instead to trigger the Property resolver
     */
    public Map<ModuleProperty<?>, Object> properties = null;

    /**
     * A map of the raw properties.
     * Only access this when you know what you are doing.
     * Use {@link ModuleInstance#getProperty(ModuleProperty)} instead to trigger the Property resolver
     */
    public Map<ModuleProperty<?>, Object> initializedProperties = new ConcurrentHashMap<>();

    /**
     * A map of the raw properties.
     * Only access this when you know what you are doing.
     * Use {@link ModuleInstance#getProperty(ModuleProperty)} instead to trigger the Property resolver
     */
    public Map<ModuleProperty<?>, Object> itemMergedProperties = new ConcurrentHashMap<>();

    /**
     * Constructs a new module instance with the given item module.
     *
     * @param module the item module for the module instance
     */
    public ModuleInstance(ItemModule module) {
        this.module = module;
    }

    /**
     * Returns a flat list of all sub-modules, including the current module instance.
     *
     * @return a list of all sub-modules
     */
    public List<ModuleInstance> allSubModules() {
        return ItemModule.createFlatList(this);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getProperty(ModuleProperty<T> property) {
        Object propertyData = initializedProperties.get(property);
        if (propertyData != null) {
            return (T) propertyData;
        }

        if (properties == null) {
            PropertyResolver.resolve(this.getRoot());
        }

        Object propertyDataRaw = properties.get(property);
        if (propertyDataRaw != null) {
            T data = (T) propertyDataRaw;
            data = property.initialize(data, this);
            this.initializedProperties.put(property, data);
            return data;
        }
        return null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getPropertyItemStack(ModuleProperty<T> property) {
        if (itemMergedProperties.containsKey(property)) {
            return (T) itemMergedProperties.get(property);
        }
        T propertyData = null;
        ModuleInstance lastDataOwner = null;
        ModuleInstance root = getRoot();
        for (ModuleInstance moduleInstance : root.allSubModules()) {
            T toMergeData = moduleInstance.getProperty(property);
            if (toMergeData != null) {
                if (propertyData == null) {
                    propertyData = toMergeData;
                    lastDataOwner = moduleInstance;
                } else {
                    propertyData = property.merge(propertyData, lastDataOwner, toMergeData, moduleInstance, MergeType.SMART);
                }
            }
        }
        itemMergedProperties.put(property, propertyData);
        return propertyData;
    }

    /**
     * Returns the root module instance, i.e., the module instance that has no parent.
     *
     * @return the root module instance
     */
    public ModuleInstance getRoot() {
        ModuleInstance root = this;
        while (root.parent != null) {
            root = root.parent;
        }
        return root;
    }

    /**
     * Creates a copy of this module instance and all its parents and children.
     *
     * @return The copied module instance.
     */
    public ModuleInstance copy() {
        List<Integer> position = new ArrayList<>();
        calculatePosition(position);

        ModuleInstance root = this.getRoot().deepCopy();

        return root.getPosition(position);
    }

    /**
     * Recursively calculates the position of this module instance in its hierarchy.
     *
     * @param position The list to store the position.
     */
    public void calculatePosition(List<Integer> position) {
        if (parent != null) {
            parent.calculatePosition(position);
            position.add(this.getId());
        }
    }

    /**
     * Retrieves the module instance at the specified position in the hierarchy.
     *
     * @param position The position of the module instance.
     * @return The module instance at the specified position.
     */
    public ModuleInstance getPosition(List<Integer> position) {
        if (!position.isEmpty()) {
            int pos = position.removeFirst();
            ModuleInstance subModule = subModules.get(pos);
            if (subModule != null) {
                return subModule.getPosition(position);
            }
        }
        return this;
    }

    /**
     * Retrieves the ID of this module instance.
     *
     * @return The ID of the module instance, or null if not found.
     */
    @Nullable
    public String getId() {
        if (parent != null) {
            for (Map.Entry<String, ModuleInstance> entry : parent.subModules.entrySet()) {
                if (entry.getValue() == this) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    /**
     * Creates a deep copy of this module instance and its submodules.
     *
     * @return The copied module instance.
     */
    private ModuleInstance deepCopy() {
        ModuleInstance copy = new ModuleInstance(this.module);
        copy.moduleData = new HashMap<>(this.moduleData);
        this.subModules.forEach(((id, subModule) -> {
            ModuleInstance subModuleCopy = subModule.deepCopy();
            subModuleCopy.parent = copy;
            copy.subModules.put(id, subModuleCopy);
        }));
        return copy;
    }

    /**
     * Writes the module to the item using the current module.
     *
     * @param stack The ItemStack to write the module to.
     */
    public void writeToItem(ItemStack stack) {
        this.clearCaches();
        writeToItem(stack, true);
    }

    /**
     * Writes the module to the item using the current module.
     *
     * @param stack      The ItemStack to write the module to.
     * @param clearCache Determines whether to clear the cache after writing the module.
     */
    public void writeToItem(ItemStack stack, boolean clearCache) {
        if (clearCache) {
            this.clearCaches();
        }
        stack.update(ModuleInstance.componentType, this, (component) -> component);
    }

    /**
     * Clears all cached data of this ModuleInstance and all its related ModulesInstances
     * should be used in case of changes to the item
     */
    public void clearCaches() {
        this.getRoot().allSubModules().forEach(ModuleInstance::clearCachesOnlyThis);
    }

    /**
     * Clears all cached data of this ModuleInstance
     * this should not be called directly.
     * Calling this directly will cause issues,
     * as the {@link PropertyResolver} will partially trigger if not all {@link ModuleInstance}
     * are cleared. {@link ModuleInstance#clearCaches()} should be used
     */
    public void clearCachesOnlyThis() {
        properties = null;
        itemMergedProperties.clear();
        cachedData.clear();
        itemStackCache.clear();
        initializedProperties.clear();
    }

    /**
     * Returns a JSON string representation of this module instance.
     *
     * @return a JSON string representation of this module instance
     */
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    /**
     * Returns a module instance constructed from the given JSON string representation.
     *
     * @param string the JSON string representation of a module instance
     * @return a module instance constructed from the given JSON string representation
     */
    public static ModuleInstance fromString(String string) {
        Gson gson = new Gson();
        ModuleInstance moduleInstance = gson.fromJson(string, ModuleInstance.class);
        if (moduleInstance.module == null) {
            moduleInstance.module = ItemModule.empty;
        }
        return moduleInstance;
    }

    public Optional<ModuleInstance> parseTo(String[] data) {
        if (data.length == 0) {
            return Optional.of(this);
        }
        String[] newArray = Arrays.copyOfRange(data, 1, data.length);
        if ("parent".equals(data[0])) {
            if (this.parent != null) {
                return parent.parseTo(newArray);
            }
        } else {
            try {
                int id = Integer.parseInt(data[0]);
                if (subModules.containsKey(id)) {
                    subModules.get(0).parseTo(newArray);
                }
            } catch (NumberFormatException ignored) {

            }
        }
        return Optional.empty();
    }

    /**
     * this function is meant to be used {@link ModularItemCache#MODULE_CACHE_SUPPLIER}
     * to have on demand caching on a per {@link ModuleInstance} level
     * if Itemstack Level caching is desired, {@link ModularItemCache#get(ItemStack, String, Object)} should be looked at
     *
     * @param key      the Key for the stored data. Common Practice is to use {@link net.minecraft.resources.ResourceLocation} stringifies for this
     * @param fallback a supplier of a fallback in case this cant be resolved. Stuff cannot be resolved during reloads or other invalid stats.
     * @param <T>      the Type of the data in question, used to avoid casting
     * @return Returns the Cached data if available, otherwise uses the registered {@link ModularItemCache#MODULE_CACHE_SUPPLIER} to supply and then cache the data
     */
    @SuppressWarnings("unchecked")
    public <T> T getFromCache(String key, Supplier<T> fallback) {
        T data = (T) cachedData.get(key);
        if (data != null) {
            return data;
        }
        DataCache.ModuleCacheSupplier cacheSupplier = ModularItemCache.MODULE_CACHE_SUPPLIER.get(key);
        if (cacheSupplier != null) {
            data = (T) cacheSupplier.apply(this);
            if (data != null) {
                cachedData.put(key, data);
                return data;
            }
        }
        return fallback.get();
    }

    /**
     * returns the Item-level Cache for this. Itemstack is required as context
     *
     * @param key       the key under {@link ModularItemCache#setSupplier(String, ModularItemCache.CacheObjectSupplier)} the supplier was registered
     * @param itemStack the Context Itemstack
     * @param fallback  fallback value in case the state was invalid or the supplier returned null
     * @param <T>       the type inside the cache
     * @return the cached value
     */
    public <T> T getFromCache(String key, ItemStack itemStack, T fallback) {
        return ModularItemCache.get(itemStack, key, fallback);
    }

    /**
     * returns the Item-level Cache for this. Itemstack is required as context
     *
     * @param key       the key under {@link ModularItemCache#setSupplier(String, ModularItemCache.CacheObjectSupplier)} the supplier was registered
     * @param itemStack the Context Itemstack
     * @param fallback  fallback value in case the state was invalid or the supplier returned null
     * @param <T>       the type inside the cache
     * @return the cached value
     */
    public <T> T getFromCache(String key, ItemStack itemStack, Supplier<T> fallback) {
        return ModularItemCache.get(itemStack, key, fallback);
    }

    /**
     * This function shouldnt be used directly, instead check {@link ModularItemCache#get(ItemStack, String, Object)} for this functionality
     * alternativly {@link ModuleInstance#getFromCache(String, ItemStack, Supplier)} can also be used
     */
    @SuppressWarnings("unchecked")
    public <T> T getFromCache(String key, ItemStack itemStack, Map<String, ModularItemCache.CacheObjectSupplier> supplierMap, Supplier<T> fallback) {
        T data = (T) cachedData.get(key);
        if (data != null) {
            return data;
        }
        ModularItemCache.CacheObjectSupplier cacheSupplier = supplierMap.get(key);
        if (cacheSupplier != null) {
            data = (T) cacheSupplier.apply(itemStack);
            if (data != null) {
                cachedData.put(key, data);
                return data;
            }
        }
        return fallback.get();
    }
}
