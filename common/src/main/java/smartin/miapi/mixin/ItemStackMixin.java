package smartin.miapi.mixin;

import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import smartin.miapi.item.FakeItemstackReferenceProvider;
import smartin.miapi.item.modular.ModularItem;
import smartin.miapi.item.modular.VisualModularItem;
import smartin.miapi.modules.properties.FakeItemTagProperty;
import smartin.miapi.modules.properties.enchanment.FakeEnchantmentManager;

@Mixin(value = ItemStack.class, priority = 2000)
abstract class ItemStackMixin {

    @Inject(method = "is(Lnet/minecraft/tags/TagKey;)Z", at = @At("TAIL"), cancellable = true)
    public void miapi$injectItemTag(TagKey<Item> tag, CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.getItem() instanceof ModularItem) {
            if (!cir.getReturnValue()) {
                cir.setReturnValue(FakeItemTagProperty.hasTag(tag.location(), stack));
            }
        }
    }

    @Inject(method = "getItem", at = @At("TAIL"))
    public void miapi$capturePotentialItemstack(CallbackInfoReturnable<Item> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (cir.getReturnValue() instanceof ModularItem) {
            FakeItemstackReferenceProvider.setReference(cir.getReturnValue(), stack);
        }
    }

    @Inject(method = "<init>(Lnet/minecraft/world/level/ItemLike;ILnet/minecraft/core/component/PatchedDataComponentMap;)V", at = @At("TAIL"))
    public void miapi$capturePotentialItemstack(ItemLike item, int count, PatchedDataComponentMap components, CallbackInfo ci) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.getItem() instanceof VisualModularItem) {
            FakeEnchantmentManager.initOnItemStack(stack);
        }
    }
}
