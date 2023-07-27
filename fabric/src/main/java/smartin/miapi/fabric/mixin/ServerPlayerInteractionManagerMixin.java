package smartin.miapi.fabric.mixin;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import smartin.miapi.attributes.ReachEntityAttributes;

@Mixin(ServerPlayerInteractionManager.class)
abstract class ServerPlayerInteractionManagerMixin {
    @Shadow @Final protected ServerPlayerEntity player;

    @Redirect(
        method = "processBlockBreakingAction",
        at = @At(value = "FIELD", target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;MAX_BREAK_SQUARED_DISTANCE:D", opcode = Opcodes.GETSTATIC))
    private double miapi$getActualReachDistance() {
        return ReachEntityAttributes.getSquaredReachDistance(this.player, ServerPlayNetworkHandler.MAX_BREAK_SQUARED_DISTANCE);
    }
}
