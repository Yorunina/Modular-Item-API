package smartin.miapi.modules.properties;

import com.mojang.serialization.Codec;
import dev.architectury.event.EventResult;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import smartin.miapi.Miapi;
import smartin.miapi.events.MiapiProjectileEvents;
import smartin.miapi.item.modular.ModularItem;
import smartin.miapi.item.modular.items.ModularCrossbow;
import smartin.miapi.modules.ModuleInstance;
import smartin.miapi.modules.properties.util.DoubleProperty;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

public class RapidfireCrossbowProperty extends DoubleProperty {
    public static String KEY = "rapid_fire_crossbow";
    public static String NBTKEY = Miapi.MOD_ID + KEY;
    public static RapidfireCrossbowProperty property;
    public static DataComponentType<List<ItemStack>> ADDITIONAL_PROJECTILES_COMPONENT = DataComponentType.<List<ItemStack>>builder().persistent(Codec.list(ItemStack.CODEC)).build();

    public RapidfireCrossbowProperty() {
        super(KEY);
        property = this;
        MiapiProjectileEvents.MODULAR_CROSSBOW_LOAD_AFTER.register(context -> {
            int shotCount = getShotCount(context.crossbow);
            List<ItemStack> projectiles = new ArrayList<>(getSavedProjectilesOnCrossbow(context.crossbow));
            for (int i = projectiles.size(); i < shotCount; i++) {
                ItemStack otherAmmo = context.player.getProjectile(context.crossbow);
                Player player = null;
                if (
                        !otherAmmo.isEmpty() &&
                        context.player instanceof Player player2 &&
                        !player2.isCreative()) {
                    player = player2;
                } else {
                    otherAmmo = otherAmmo.copy();
                }
                otherAmmo = otherAmmo.split(1);
                if (!otherAmmo.isEmpty()) {
                    if (otherAmmo.isDamageableItem()) {
                        //otherAmmo.hurtAndBreak(1, context.player, context.crossbow);
                        //TODO:check for durability dmg
                    }
                    projectiles.add(otherAmmo.copy());
                    if (player != null) {
                        player.getInventory().removeItem(otherAmmo);
                    }
                }
            }
            writeProjectileListToCrossbow(context.crossbow, projectiles);
            return EventResult.pass();
        });
        MiapiProjectileEvents.MODULAR_CROSSBOW_POST_SHOT.register((player, crossbow) -> {
            if ((crossbow.getItem() instanceof ModularCrossbow) && (player.level() instanceof ServerLevel serverLevel)) {
                List<ItemStack> projectiles = getSavedProjectilesOnCrossbow(crossbow);
                if (!projectiles.isEmpty()) {
                    ItemStack itemStack = projectiles.remove(0);
                    ModularCrossbow.putProjectile(crossbow, itemStack);
                    int projectileCount = EnchantmentHelper.processProjectileCount(serverLevel, crossbow, player, 1);
                    for (int i = 1; i < projectileCount; i++) {
                        ModularCrossbow.putProjectile(crossbow, itemStack.copy());
                    }
                    writeProjectileListToCrossbow(crossbow, projectiles);
                    //ModularCrossbow.setCharged(crossbow, true);
                    if (player instanceof ServerPlayer serverPlayerEntity) {
                        serverPlayerEntity.getCooldowns().addCooldown(
                                crossbow.getItem(),
                                MagazineCrossbowShotDelay.property.getValue(crossbow).orElse(0.0).intValue());
                    }
                }
            }
            return EventResult.pass();
        });
        LoreProperty.loreSuppliers.add((stack, tooltip, context, info) -> {
            if (stack.getItem() instanceof ModularItem) {
                List<ItemStack> projectiles = getSavedProjectilesOnCrossbow(stack);
                for (ItemStack projectile : projectiles) {
                    tooltip.add(Component.translatable(Miapi.MOD_ID + ".crossbow.ammo.addition", projectile.getHoverName()));
                }
            }
        });
    }

    public static void writeProjectileListToCrossbow(ItemStack crossbow, List<ItemStack> projectiles) {
        crossbow.getComponents().get(ADDITIONAL_PROJECTILES_COMPONENT);
        crossbow.set(ADDITIONAL_PROJECTILES_COMPONENT, projectiles);
    }

    public static List<ItemStack> getSavedProjectilesOnCrossbow(ItemStack crossbow) {
        return crossbow.getComponents().getOrDefault(ADDITIONAL_PROJECTILES_COMPONENT, new ArrayList<>())
    }

    public static int getShotCount(ItemStack itemStack) {
        return property.getValue(itemStack).orElse(0.0).intValue();
    }

}
