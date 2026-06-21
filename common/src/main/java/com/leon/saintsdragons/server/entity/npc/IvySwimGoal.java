package com.leon.saintsdragons.server.entity.npc;

import com.leon.saintsdragons.server.ai.navigation.async.AsyncSwimController;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

final class IvySwimGoal extends Goal {
    private static final double FOLLOW_START_DISTANCE_SQR = 6.25D;
    private static final double FOLLOW_STOP_DISTANCE_SQR = 2.25D;
    private static final double FOLLOW_RUN_DISTANCE_SQR = 20.25D;
    private static final double FOLLOW_SWIM_ARRIVAL_DISTANCE = 1.75D;
    private static final double BREATHE_SWIM_ARRIVAL_DISTANCE = 0.75D;
    private static final double TARGET_FAST_DISTANCE_SQR = 15.0D * 15.0D;
    private static final float TURN_SPEED = 8.0F;
    private static final float BREATHE_TURN_SPEED = 12.0F;
    private static final double BREATHE_SWIM_SPEED = 0.22D;
    private static final double FOLLOW_SWIM_SPEED = 0.18D;
    private static final double FOLLOW_FAST_SWIM_SPEED = 0.26D;
    private static final double TARGET_SWIM_SPEED = 0.30D;
    private static final double TARGET_FAST_SWIM_SPEED = 0.42D;

    private final IvyTheDragonMerchant ivy;
    private final Mode mode;
    private LivingEntity target;

    private IvySwimGoal(IvyTheDragonMerchant ivy, Mode mode) {
        this.ivy = ivy;
        this.mode = mode;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    static IvySwimGoal target(IvyTheDragonMerchant ivy) {
        return new IvySwimGoal(ivy, Mode.TARGET);
    }

    static IvySwimGoal breathe(IvyTheDragonMerchant ivy) {
        return new IvySwimGoal(ivy, Mode.BREATHE);
    }

    static IvySwimGoal followOwner(IvyTheDragonMerchant ivy) {
        return new IvySwimGoal(ivy, Mode.FOLLOW_OWNER);
    }

    @Override
    public boolean canUse() {
        if (mode == Mode.BREATHE) {
            return canSwimNow() && shouldSurfaceForAir();
        }
        if (!canSwimNow()) {
            return false;
        }
        LivingEntity resolvedTarget = resolveTarget();
        if (resolvedTarget == null) {
            return false;
        }
        if (mode == Mode.FOLLOW_OWNER && ivy.distanceToSqr(resolvedTarget) < FOLLOW_START_DISTANCE_SQR && !shouldSurfaceForAir()) {
            return false;
        }
        target = resolvedTarget;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (mode == Mode.BREATHE) {
            return canSwimNow() && shouldSurfaceForAir();
        }
        if (!canSwimNow() || target == null || !isValidTarget(target)) {
            return false;
        }
        if (mode == Mode.FOLLOW_OWNER && ivy.distanceToSqr(target) <= FOLLOW_STOP_DISTANCE_SQR && !shouldSurfaceForAir()) {
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        ivy.getNavigation().stop();
    }

    @Override
    public void stop() {
        target = null;
        ivy.setRunning(false);
        AsyncSwimController controller = ivy.getAsyncSwimController();
        if (controller != null) {
            controller.stop();
        }
    }

    @Override
    public void tick() {
        if (mode == Mode.BREATHE) {
            tickBreathe();
            return;
        }
        if (target == null) {
            return;
        }
        ivy.getNavigation().stop();
        ivy.getLookControl().setLookAt(target, 30.0F, 30.0F);

        double distanceSqr = ivy.distanceToSqr(target);
        boolean fast = mode == Mode.TARGET
                ? distanceSqr > TARGET_FAST_DISTANCE_SQR
                : distanceSqr > FOLLOW_RUN_DISTANCE_SQR;
        ivy.setRunning(fast);

        Vec3 targetPos = mode == Mode.FOLLOW_OWNER ? followOwnerTarget(target) : target.position().add(0.0D, target.getEyeHeight() * 0.5D, 0.0D);
        double speed = mode == Mode.TARGET
                ? fast ? TARGET_FAST_SWIM_SPEED : TARGET_SWIM_SPEED
                : fast ? FOLLOW_FAST_SWIM_SPEED : FOLLOW_SWIM_SPEED;

        AsyncSwimController controller = ivy.getAsyncSwimController();
        boolean accepted = controller != null && (mode == Mode.FOLLOW_OWNER
                ? controller.trackMovingTarget(targetPos, speed, TURN_SPEED, FOLLOW_SWIM_ARRIVAL_DISTANCE)
                : controller.trackTarget(targetPos, speed, TURN_SPEED));
        if (accepted) {
            controller.serverTick();
        }
    }

    private void tickBreathe() {
        ivy.getNavigation().stop();
        ivy.setRunning(false);

        Vec3 targetPos = surfaceTarget();
        AsyncSwimController controller = ivy.getAsyncSwimController();
        boolean accepted = controller != null
                && controller.trackMovingTarget(targetPos, BREATHE_SWIM_SPEED, BREATHE_TURN_SPEED, BREATHE_SWIM_ARRIVAL_DISTANCE);
        if (accepted) {
            controller.serverTick();
        }
    }

    private Vec3 followOwnerTarget(LivingEntity owner) {
        Vec3 look = owner.getLookAngle();
        Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);
        if (horizontalLook.lengthSqr() < 1.0E-4D) {
            horizontalLook = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            horizontalLook = horizontalLook.normalize();
        }

        double targetY = owner.isInWaterOrBubble()
                ? owner.getY() + owner.getEyeHeight() * 0.35D
                : owner.getY() + 0.2D;
        if (shouldSurfaceForAir()) {
            targetY = Math.max(targetY, surfaceTarget().y);
        }
        return new Vec3(
                owner.getX() - horizontalLook.x * 1.5D,
                targetY,
                owner.getZ() - horizontalLook.z * 1.5D
        );
    }

    private Vec3 surfaceTarget() {
        double surfaceY = ivy.getY() + Math.max(1.0D, ivy.getFluidDepthUp() + 0.35D);
        return new Vec3(ivy.getX(), surfaceY, ivy.getZ());
    }

    private boolean shouldSurfaceForAir() {
        return ivy.getAirSupply() < ivy.getMaxAirSupply() - 80;
    }

    private boolean canSwimNow() {
        return ivy.isAlive()
                && ivy.isInWaterOrBubble()
                && !ivy.isInShallowWaterForWading()
                && !ivy.isVehicle()
                && !ivy.isDownedOrArising()
                && !ivy.isTrading()
                && !ivy.isInDialogue();
    }

    private LivingEntity resolveTarget() {
        return switch (mode) {
            case BREATHE -> null;
            case TARGET -> {
                LivingEntity combatTarget = ivy.getTarget();
                yield isValidTarget(combatTarget) ? combatTarget : null;
            }
            case FOLLOW_OWNER -> {
                if (!ivy.isTame()
                        || ivy.getCompanionCommand() != IvyTheDragonMerchant.CompanionCommand.FOLLOW
                        || ivy.getTarget() != null) {
                    yield null;
                }
                LivingEntity owner = ivy.getOwner();
                yield isValidTarget(owner) ? owner : null;
            }
        };
    }

    private boolean isValidTarget(LivingEntity candidate) {
        return candidate != null
                && candidate.isAlive()
                && candidate.level().dimension() == ivy.level().dimension();
    }

    private enum Mode {
        BREATHE,
        TARGET,
        FOLLOW_OWNER
    }
}
