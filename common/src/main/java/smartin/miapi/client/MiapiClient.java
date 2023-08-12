package smartin.miapi.client;

import com.redpxnda.nucleus.impl.ShaderRegistry;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientReloadShadersEvent;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import smartin.miapi.Miapi;
import smartin.miapi.blocks.ModularWorkBenchRenderer;
import smartin.miapi.client.gui.crafting.CraftingGUI;
import smartin.miapi.client.model.CustomColorProvider;
import smartin.miapi.client.model.ModularModelPredicateProvider;
import smartin.miapi.effects.CryoStatusEffect;
import smartin.miapi.mixin.client.ItemRendererAccessor;
import smartin.miapi.modules.abilities.util.ItemProjectile.ItemProjectileRenderer;
import smartin.miapi.modules.cache.ModularItemCache;
import smartin.miapi.modules.properties.material.PaletteCreators;
import smartin.miapi.registries.RegistryInventory;

import static smartin.miapi.registries.RegistryInventory.Client.glintShader;

public class MiapiClient {

    private MiapiClient() {
    }

    public static void init() {
        registerShaders();
        PaletteCreators.setup();
        ClientLifecycleEvent.CLIENT_SETUP.register(MiapiClient::clientSetup);
        ClientLifecycleEvent.CLIENT_STARTED.register(MiapiClient::clientStart);
        ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register(MiapiClient::clientLevelLoad);
        ClientReloadShadersEvent.EVENT.register((resourceFactory, asd) -> {
            ModularItemCache.discardCache();
        });
        RegistryInventory.modularItems.addCallback((item -> {
            ModularModelPredicateProvider.registerModelOverride(item, new Identifier(Miapi.MOD_ID, "durability"), (stack, world, entity, seed) -> {
                return stack.isDamageable() && stack.getDamage() > 0 ? (stack.getMaxDamage() / stack.getDamage()) : 1.0f;
            });
            ModularModelPredicateProvider.registerModelOverride(item, new Identifier(Miapi.MOD_ID, "damaged"), (stack, world, entity, seed) -> stack.isDamaged() ? 1.0F : 0.0F);
        }));
    }

    protected static void clientSetup(MinecraftClient client) {
        SpriteLoader.setup();
    }

    protected static void clientStart(MinecraftClient client) {
        RegistryInventory.addCallback(RegistryInventory.modularItems, item -> {
            ((ItemRendererAccessor) client.getItemRenderer()).color().register(new CustomColorProvider(), item);
        });
        CryoStatusEffect.setupOnClient();
    }

    protected static void clientLevelLoad(ClientWorld clientWorld) {
        SpriteLoader.clientStart();
    }

    public static void registerScreenHandler() {
        MenuRegistry.registerScreenFactory(RegistryInventory.craftingScreenHandler, CraftingGUI::new);
    }

    public static void registerEntityRenderer() {
        EntityRendererRegistry.register(RegistryInventory.itemProjectileType, ItemProjectileRenderer::new);
    }

    public static void registerBlockEntityRenderer() {
        BlockEntityRendererRegistry.register(RegistryInventory.modularWorkBenchEntityType, ModularWorkBenchRenderer::new);
    }

    public static void registerShaders() {
        ShaderRegistry.register(
                new Identifier(Miapi.MOD_ID, "rendertype_translucent_material"),
                VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL, s -> RegistryInventory.Client.translucentMaterialShader = s);
        ShaderRegistry.register(
                new Identifier(Miapi.MOD_ID, "rendertype_entity_translucent_material"),
                VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, s -> RegistryInventory.Client.entityTranslucentMaterialShader = s);
        ShaderRegistry.register(
                new Identifier(Miapi.MOD_ID, "rendertype_item_glint"),
                VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, s -> glintShader = s);
    }
}
