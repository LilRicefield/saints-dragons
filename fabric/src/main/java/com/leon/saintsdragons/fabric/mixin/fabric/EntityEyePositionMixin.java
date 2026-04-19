package com.leon.saintsdragons.fabric.mixin.fabric;

import com.leon.saintsdragons.fabric.client.camera.NulljawFirstPersonCamera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityEyePositionMixin {
    @Inject(method = "getEyePosition()Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"), cancellable = true)
    private void saintsdragons$offsetNulljawEyePosition(CallbackInfoReturnable<Vec3> cir) {
        Entity entity = (Entity) (Object) this;
        if (NulljawFirstPersonCamera.isActive(entity)) {
            cir.setReturnValue(cir.getReturnValue().add(0.0D, NulljawFirstPersonCamera.Y_OFFSET, 0.0D));
        }
    }

    @Inject(method = "getEyePosition(F)Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"), cancellable = true)
    private void saintsdragons$offsetNulljawEyePositionLerped(float partialTick, CallbackInfoReturnable<Vec3> cir) {
        Entity entity = (Entity) (Object) this;
        if (NulljawFirstPersonCamera.isActive(entity)) {
            cir.setReturnValue(cir.getReturnValue().add(0.0D, NulljawFirstPersonCamera.Y_OFFSET, 0.0D));
        }
    }
}
