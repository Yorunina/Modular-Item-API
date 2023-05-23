package smartin.miapi.mixin;

import dev.architectury.event.EventResult;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import smartin.miapi.Events.Event;
import smartin.miapi.attributes.AttributeRegistry;
import smartin.miapi.item.modular.ModularItem;
import smartin.miapi.modules.properties.EquipmentSlotProperty;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {

    @Shadow public abstract boolean removeStatusEffect(StatusEffect type);

    @Shadow @Nullable public abstract StatusEffectInstance removeStatusEffectInternal(@Nullable StatusEffect type);

    @Shadow protected abstract int getNextAirUnderwater(int air);

    @Inject(method = "getPreferredEquipmentSlot", at = @At("HEAD"), cancellable = true)
    private static void onGetPreferredEquipmentSlot(ItemStack stack, CallbackInfoReturnable<EquipmentSlot> cir) {
        if (stack.getItem() instanceof ModularItem) {
            EquipmentSlot slot = EquipmentSlotProperty.getSlot(stack);
            if (slot != null) {
                cir.setReturnValue(slot);
            }
        }
    }

    @Inject(method = "createLivingAttributes", at = @At("TAIL"), cancellable = true)
    private static void addAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        DefaultAttributeContainer.Builder builder = cir.getReturnValue();
        if(builder!=null){
            builder.add(AttributeRegistry.DAMAGE_RESISTANCE);
        }
    }

    private float storedValue;
    private DamageSource storedDamageSource;

    @Inject(method = "damage", at = @At(value = "HEAD"))
    private void damageEvent(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Event.LivingHurtEvent livingHurtEvent = new Event.LivingHurtEvent((LivingEntity) (Object) this, source, amount);
        EventResult result = Event.LIVING_HURT.invoker().hurt(livingHurtEvent);
        if(result.interruptsFurtherEvaluation()){
            cir.setReturnValue(false);
        }
        storedValue = livingHurtEvent.amount;
        storedDamageSource = livingHurtEvent.damageSource;

    }

    @ModifyVariable(method = "damage", at = @At(value = "HEAD"), ordinal = 0)
    private float damageEventValue(float value) {
        return storedValue;
    }

    @ModifyVariable(method = "damage", at = @At(value = "HEAD"), ordinal = 0)
    private DamageSource damageEventSource(DamageSource value) {
        return storedDamageSource;
    }
}
