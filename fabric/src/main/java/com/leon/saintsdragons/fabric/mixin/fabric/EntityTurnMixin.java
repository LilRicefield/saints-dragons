package com.leon.saintsdragons.fabric.mixin.fabric;

import com.leon.saintsdragons.client.camera.DragonlordFlightTurnSmoothing;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class EntityTurnMixin {
    @ModifyVariable(method = "turn", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double saintsdragons$smoothDragonlordFlightYaw(double yawDelta) {
        if ((Object) this instanceof LocalPlayer player) {
            return DragonlordFlightTurnSmoothing.smoothYaw(player, yawDelta);
        }
        return yawDelta;
    }

    @ModifyVariable(method = "turn", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double saintsdragons$smoothDragonlordFlightPitch(double pitchDelta) {
        if ((Object) this instanceof LocalPlayer player) {
            return DragonlordFlightTurnSmoothing.smoothPitch(player, pitchDelta);
        }
        return pitchDelta;
    }
}
