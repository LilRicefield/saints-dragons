package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.interfaces.SemiAquaticDragon;
import com.leon.saintsdragons.server.entity.interfaces.DragonMovementCapable;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

public class DirectSwimToTargetGoal extends Goal {

    private static final double FOLLOW_START_DISTANCE_SQR = 20.0D * 20.0D;
    private static final double FOLLOW_STOP_DISTANCE_SQR = 16.0D * 16.0D;
    private static final double BABY_FOLLOW_START_DISTANCE_SQR = 8.0D * 8.0D;
    private static final double BABY_FOLLOW_STOP_DISTANCE_SQR = 6.0D * 6.0D;

    private final Mob mob;
    private final float turnSpeed;
    private final double swimSpeed;
    private final boolean aggressive; // true = chase targets, false = follow owner

    private double currentYaw;
    private double targetYaw;
    private double currentPitch;

    public DirectSwimToTargetGoal(Mob mob, float turnSpeedDegrees, double swimSpeed, boolean aggressive) {
        this.mob = mob;
        this.turnSpeed = turnSpeedDegrees;
        this.swimSpeed = swimSpeed;
        this.aggressive = aggressive;
        this.currentYaw = mob.getYRot();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Only use when in water and has a target
        if (!canUseSwimMovement() || !mob.isInWaterOrBubble() || mob.isVehicle()) {
            return false;
        }

        LivingEntity target = resolveTarget();
        if (target == null) {
            return false;
        }

        if (!aggressive) {
            double startDistance = isBabyParentTarget(target)
                    ? BABY_FOLLOW_START_DISTANCE_SQR
                    : FOLLOW_START_DISTANCE_SQR;
            return mob.distanceToSqr(target) > startDistance;
        }

        return target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        if (!canUseSwimMovement() || !mob.isInWaterOrBubble() || mob.isVehicle()) {
            return false;
        }

        LivingEntity target = resolveTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        if (!aggressive) {
            double stopDistance = isBabyParentTarget(target)
                    ? BABY_FOLLOW_STOP_DISTANCE_SQR
                    : FOLLOW_STOP_DISTANCE_SQR;
            return mob.distanceToSqr(target) > stopDistance;
        }

        return true;
    }

    @Override
    public void start() {
        this.currentYaw = mob.getYRot();
        this.currentPitch = 0.0;
        this.mob.getNavigation().stop();
    }

    @Override
    public void stop() {
        // Gradually slow down
        Vec3 vel = mob.getDeltaMovement();
        mob.setDeltaMovement(vel.x * 0.8, vel.y * 0.8, vel.z * 0.8);
    }

    @Override
    public void tick() {
        LivingEntity target = resolveTarget();
        if (target == null) {
            return;
        }

        this.mob.getNavigation().stop();

        // Calculate direction to target
        double dx = target.getX() - mob.getX();
        double dy = (target.getY() + target.getEyeHeight() * 0.5) - (mob.getY() + mob.getEyeHeight() * 0.5);
        double dz = target.getZ() - mob.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        // Calculate target yaw (horizontal rotation)
        targetYaw = (Math.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0;

        // Calculate target pitch (vertical rotation)
        double targetPitch = -(Math.atan2(dy, horizontalDist) * Mth.RAD_TO_DEG);
        targetPitch = Mth.clamp(targetPitch, -85.0, 85.0);

        // Smooth rotation toward target
        double yawDelta = Mth.wrapDegrees(targetYaw - currentYaw);
        yawDelta = Mth.clamp(yawDelta, -turnSpeed, turnSpeed);
        currentYaw = Mth.wrapDegrees(currentYaw + yawDelta);

        // Smooth pitch changes
        double pitchDelta = targetPitch - currentPitch;
        pitchDelta = Mth.clamp(pitchDelta, -turnSpeed * 0.5, turnSpeed * 0.5);
        currentPitch += pitchDelta;

        // Apply rotation
        mob.setYRot((float) currentYaw);
        mob.yBodyRot = (float) currentYaw;
        mob.yHeadRot = (float) currentYaw;
        mob.setXRot((float) currentPitch);

        // Calculate velocity direction from current rotation
        double yawRad = currentYaw * Mth.DEG_TO_RAD;
        double pitchRad = currentPitch * Mth.DEG_TO_RAD;

        double dirX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double dirY = -Math.sin(pitchRad);
        double dirZ = Math.cos(yawRad) * Math.cos(pitchRad);

        // Apply velocity directly
        double speed = swimSpeed;
        if (mob instanceof SemiAquaticDragon dragon) {
            speed = dragon.getSwimSpeed() * swimSpeed;
        }

        // Boost speed if far from target
        if (horizontalDist > 15.0) {
            speed *= 1.5;
        }

        mob.setDeltaMovement(dirX * speed, dirY * speed, dirZ * speed);
    }

    private LivingEntity resolveTarget() {
        if (aggressive) {
            return mob.getTarget();
        }

        if (mob.getTarget() != null) {
            return null;
        }

        LivingEntity parent = resolveParentForBaby();
        if (parent != null) {
            return parent;
        }

        if (mob instanceof TamableAnimal tamable) {
            if (!tamable.isTame() || tamable.isOrderedToSit()) {
                return null;
            }
            LivingEntity owner = tamable.getOwner();
            if (owner != null && owner.isAlive() && owner.level() == mob.level()) {
                return owner;
            }
        }
        return null;
    }

    private boolean canUseSwimMovement() {
        return !(mob instanceof DragonMovementCapable dragon) || dragon.canSwim();
    }

    private boolean isBabyParentTarget(LivingEntity target) {
        if (!mob.isBaby() || target == null) {
            return false;
        }
        if (!(target instanceof Mob targetMob)) {
            return false;
        }
        return target.getClass() == mob.getClass() && !targetMob.isBaby();
    }

    private LivingEntity resolveParentForBaby() {
        if (!mob.isBaby()) {
            return null;
        }

        if (mob instanceof TamableAnimal tamable && (tamable.isTame() || tamable.getOwner() != null)) {
            return null;
        }

        List<? extends Mob> nearby = mob.level().getEntitiesOfClass(
                mob.getClass(),
                mob.getBoundingBox().inflate(12.0D, 6.0D, 12.0D),
                candidate -> candidate != mob && candidate.isAlive() && !candidate.isBaby()
        );

        Mob closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Mob candidate : nearby) {
            double dist = mob.distanceToSqr(candidate);
            if (dist < closestDistance) {
                closestDistance = dist;
                closest = candidate;
            }
        }

        return closest;
    }
}
