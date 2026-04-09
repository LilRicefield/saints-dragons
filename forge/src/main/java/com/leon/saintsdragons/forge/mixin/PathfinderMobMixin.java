package com.leon.saintsdragons.forge.mixin;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PathfinderMob.class)
public abstract class PathfinderMobMixin {
    @Inject(
            method = "tickLeash",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Mob;tickLeash()V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void saintsdragons$ignoreDragonLeashPull(CallbackInfo ci) {
        PathfinderMob mob = (PathfinderMob) (Object) this;
        if (!(mob instanceof DragonEntity dragon) || !dragon.ignoresLeashPull()) {
            return;
        }

        Entity leashHolder = mob.getLeashHolder();
        if (leashHolder != null && leashHolder.level() == mob.level()) {
            mob.restrictTo(BlockPos.containing(leashHolder.position()), 5);
            float distance = mob.distanceTo(leashHolder);

            if (mob instanceof TamableAnimal tamable && tamable.isInSittingPose()) {
                if (distance > dragon.getLeashBreakDistance()) {
                    mob.dropLeash(true, true);
                }
                ci.cancel();
                return;
            }

            if (distance > dragon.getLeashBreakDistance()) {
                mob.dropLeash(true, true);
            }
        }

        ci.cancel();
    }
}
