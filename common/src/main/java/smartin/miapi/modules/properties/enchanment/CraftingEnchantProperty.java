package smartin.miapi.modules.properties.enchanment;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import dev.architectury.event.EventResult;
import org.jetbrains.annotations.Nullable;
import smartin.miapi.Miapi;
import smartin.miapi.blocks.ModularWorkBenchEntity;
import smartin.miapi.craft.CraftAction;
import smartin.miapi.events.MiapiEvents;
import smartin.miapi.modules.ItemModule;
import smartin.miapi.modules.ModuleInstance;
import smartin.miapi.modules.properties.util.CraftingProperty;
import smartin.miapi.modules.properties.util.MergeType;
import smartin.miapi.modules.properties.util.ModuleProperty;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class CraftingEnchantProperty implements ModuleProperty, CraftingProperty {
    public static final String KEY = "crafting_enchants";
    public static CraftingEnchantProperty property;
    private static final Type type = new TypeToken<Map<String, Integer>>() {
    }.getType();

    public CraftingEnchantProperty() {
        property = this;
        MiapiEvents.SMITHING_EVENT.register((listener) -> {
            listener.itemStack = applyEnchants(listener.itemStack);
            return EventResult.pass();
        });
    }

    private static List<Pair<Holder<Enchantment>, Integer>> getEnchantsCache(ItemStack itemStack) {
        List<Pair<Holder<Enchantment>, Integer>> enchants = new ArrayList<>();

        JsonElement list = ItemModule.getMergedProperty(itemStack, property, MergeType.SMART);
        ItemModule.getMergedProperty(ItemModule.getModules(itemStack), property);
        Map<String, Integer> map = Miapi.gson.fromJson(list, type);
        if (map != null) {
            map.forEach((id, level) -> {
                JsonObject object = new JsonObject();
                object.addProperty("string", id);
                try {
                    Holder<Enchantment> entry = Enchantment.CODEC.parse(JsonOps.INSTANCE, object.get("string")).getOrThrow();
                    if (entry.value().canEnchant(itemStack)) {
                        enchants.add(new Pair<>(entry, level));
                    }
                } catch (Exception e) {
                    Miapi.LOGGER.info("Unkown enchantment : " + id);
                }
            });
        }
        return enchants;
    }

    public JsonElement merge(JsonElement old, JsonElement toMerge, MergeType mergeType) {
        if (old != null && toMerge != null) {
            Map<String, Integer> mapOld = Miapi.gson.fromJson(old, type);
            Map<String, Integer> mapToMerge = Miapi.gson.fromJson(toMerge, type);
            if (mergeType.equals(MergeType.OVERWRITE)) {
                return toMerge;
            }
            mapOld.forEach((key, level) -> {
                if (mapToMerge.containsKey(key)) {
                    mapToMerge.put(key, Math.max(mapOld.get(key), mapToMerge.get(key)));
                } else {
                    mapToMerge.put(key, level);
                }
            });
            return Miapi.gson.toJsonTree(mapToMerge);
        }
        if (old == null && toMerge != null) {
            return toMerge;
        }
        return old;
    }

    @Override
    public boolean load(String moduleKey, JsonElement data) throws Exception {
        Miapi.gson.fromJson(data, type);
        return true;
    }

    @Override
    public boolean shouldExecuteOnCraft(@Nullable ModuleInstance module, ModuleInstance root, ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack preview(ItemStack old, ItemStack crafting, Player player, ModularWorkBenchEntity bench, CraftAction craftAction, ItemModule module, List<ItemStack> inventory, Map<String, String> data) {
        return applyEnchants(crafting);
    }

    public ItemStack applyEnchants(ItemStack crafting) {
        getEnchantsCache(crafting).forEach((enchantmentIntegerPair -> {
            Holder<Enchantment> enchantment = enchantmentIntegerPair.getFirst();
            int level = enchantmentIntegerPair.getSecond();
            if (enchantment.value().canEnchant(crafting)) {
                int prevLevel = EnchantmentHelper.getItemEnchantmentLevel(enchantment, crafting);
                if (level > prevLevel) {
                    if (prevLevel > 0) {
                        removeEnchant(enchantment, crafting);
                    }
                    crafting.enchant(enchantment, level);
                }
            }
        }));
        return crafting;
    }

    public static int removeEnchant(Holder<Enchantment> enchantment, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        } else {
            EnchantmentHelper.getEnchantmentsForCrafting(stack).keySet().remove(enchantment);
            return 0;
        }
    }
}