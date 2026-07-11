package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.phys.Vec3;

/**
 * Vanilla floating with a small size-aware lift when a dragon presses into a bank.
 */
public class DragonFloatGoal extends FloatGoal {
    private final Mob dragon;

    public DragonFloatGoal(Mob dragon) {
        super(dragon);
        this.dragon = dragon;
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity target = dragon.getTarget();
        boolean targetOnLand = target != null && !target.isInWaterOrBubble();
        boolean controllerStalled = targetOnLand
                && dragon instanceof DragonEntity dragonEntity
                && !dragonEntity.getAiSwimController().isMoving();
        double terminalRange = Math.max(12.0D, Math.max(dragon.getBbWidth(), dragon.getBbHeight()) * 3.0D);
        boolean terminalShoreNudge = controllerStalled
                && dragon.distanceToSqr(target) <= terminalRange * terminalRange;

        if ((!dragon.horizontalCollision && !terminalShoreNudge) || !dragon.isInWaterOrBubble()) {
            return;
        }

        Vec3 velocity = dragon.getDeltaMovement();
        double bodyScale = Math.max(dragon.getBbWidth(), dragon.getBbHeight());
        double minimumLift = Mth.clamp(0.10D + bodyScale * 0.025D, 0.14D, 0.36D);
        double nextX = velocity.x;
        double nextZ = velocity.z;
        if (targetOnLand) {
            Vec3 towardTarget = new Vec3(target.getX() - dragon.getX(), 0.0D, target.getZ() - dragon.getZ());
            if (towardTarget.lengthSqr() > 1.0E-4D) {
                Vec3 direction = towardTarget.normalize();
                double minimumForward = Mth.clamp(0.08D + bodyScale * 0.015D, 0.12D, 0.24D);
                double currentForward = nextX * direction.x + nextZ * direction.z;
                if (currentForward < minimumForward) {
                    double correction = minimumForward - currentForward;
                    nextX += direction.x * correction;
                    nextZ += direction.z * correction;
                }
            }
        }

        if (velocity.y < minimumLift || nextX != velocity.x || nextZ != velocity.z) {
            dragon.setDeltaMovement(nextX, Math.max(velocity.y, minimumLift), nextZ);
            dragon.hasImpulse = true;
        }
    }
}
