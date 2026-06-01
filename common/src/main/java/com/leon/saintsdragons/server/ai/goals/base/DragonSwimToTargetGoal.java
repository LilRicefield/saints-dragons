package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.ai.navigation.async.AsyncSwimController;
import com.leon.saintsdragons.server.entity.interfaces.DragonMovementCapable;
import com.leon.saintsdragons.server.entity.interfaces.SemiAquaticDragon;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;

public class DragonSwimToTargetGoal extends Goal {
    private static final double DEFAULT_FOLLOW_START_DISTANCE = 20.0D;
    private static final double DEFAULT_FOLLOW_STOP_DISTANCE = 16.0D;
    private static final double BABY_FOLLOW_START_DISTANCE_SQR = 8.0D * 8.0D;
    private static final double BABY_FOLLOW_STOP_DISTANCE_SQR = 6.0D * 6.0D;

    private final Mob mob;
    private final Supplier<AsyncSwimController> swimController;
    private final float turnSpeed;
    private final double swimSpeed;
    private final boolean aggressive;
    private final double followStartDistanceSqr;
    private final double followStopDistanceSqr;

    public DragonSwimToTargetGoal(Mob mob, Supplier<AsyncSwimController> swimController, float turnSpeedDegrees, double swimSpeed, boolean aggressive) {
        this(mob, swimController, turnSpeedDegrees, swimSpeed, aggressive, DEFAULT_FOLLOW_START_DISTANCE, DEFAULT_FOLLOW_STOP_DISTANCE);
    }

    public DragonSwimToTargetGoal(Mob mob, Supplier<AsyncSwimController> swimController, float turnSpeedDegrees, double swimSpeed,
                                  boolean aggressive, double followStartDistance, double followStopDistance) {
        this.mob = mob;
        this.swimController = swimController;
        this.turnSpeed = turnSpeedDegrees;
        this.swimSpeed = swimSpeed;
        this.aggressive = aggressive;
        this.followStartDistanceSqr = followStartDistance * followStartDistance;
        this.followStopDistanceSqr = followStopDistance * followStopDistance;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
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
                    : followStartDistanceSqr;
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
                    : followStopDistanceSqr;
            return mob.distanceToSqr(target) > stopDistance;
        }

        return true;
    }

    @Override
    public void start() {
        mob.getNavigation().stop();
    }

    @Override
    public void stop() {
        AsyncSwimController controller = swimController.get();
        if (controller != null) {
            controller.stop();
        }
    }

    @Override
    public void tick() {
        LivingEntity target = resolveTarget();
        if (target == null) {
            return;
        }

        mob.getNavigation().stop();

        Vec3 targetPos = target.position().add(0.0D, target.getEyeHeight() * 0.5D, 0.0D);
        double speed = swimSpeed;
        if (mob instanceof SemiAquaticDragon dragon) {
            speed = dragon.getSwimSpeed() * swimSpeed;
        }
        if (mob.distanceToSqr(target) > 15.0D * 15.0D) {
            speed *= 1.5D;
        }

        AsyncSwimController controller = swimController.get();
        if (controller == null) {
            return;
        }
        if (!controller.trackTarget(targetPos, speed, turnSpeed)) {
            return;
        }
        controller.serverTick();
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
        if (!mob.isBaby() || target == null || !(target instanceof Mob targetMob)) {
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
