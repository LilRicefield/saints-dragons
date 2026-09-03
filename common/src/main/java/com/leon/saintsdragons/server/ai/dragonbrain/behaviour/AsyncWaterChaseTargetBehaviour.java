package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncSwimController;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.interfaces.SemiAquaticDragon;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.ToDoubleBiFunction;

public class AsyncWaterChaseTargetBehaviour<T extends RideableDragonBase> extends DragonBehaviour<T> {
    private static final int SHORE_EXIT_STABLE_GROUND_TICKS = 8;
    private static final int SHORE_EXIT_MAX_TICKS = 80;

    private final ToDoubleBiFunction<T, LivingEntity> speedModifier;
    private final BiPredicate<T, LivingEntity> movementLocked;
    private final float turnSpeed;
    private boolean shoreExitActive;
    private int shoreExitTicks;
    private int stableDryGroundTicks;
    private boolean shoreExitMadeLandContact;
    private Vec3 shoreExitOrigin;
    private Vec3 shoreExitDirection;

    public AsyncWaterChaseTargetBehaviour(double speedModifier, float turnSpeed) {
        this((dragon, target) -> speedModifier, turnSpeed, (dragon, target) -> false);
    }

    public AsyncWaterChaseTargetBehaviour(ToDoubleBiFunction<T, LivingEntity> speedModifier,
                                          float turnSpeed) {
        this(speedModifier, turnSpeed, (dragon, target) -> false);
    }

    public AsyncWaterChaseTargetBehaviour(ToDoubleBiFunction<T, LivingEntity> speedModifier,
                                          float turnSpeed,
                                          BiPredicate<T, LivingEntity> movementLocked) {
        super(Map.of(DragonMemories.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
        this.speedModifier = speedModifier;
        this.turnSpeed = turnSpeed;
        this.movementLocked = movementLocked;
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        return isWaterCombatContext(context);
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        if (shoreExitActive) {
            T dragon = context.dragon();
            LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
            return shoreExitTicks < SHORE_EXIT_MAX_TICKS
                    && !dragon.isVehicle()
                    && !dragon.isAerial()
                    && target != null
                    && !DragonTargetingHelper.isMovementAnchorInWater(target)
                    && dragon.isTargetValid(target);
        }
        return isWaterCombatContext(context);
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        resetShoreExit();
        context.dragon().getNavigation().stop();
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (target == null) {
            return;
        }

        AsyncSwimController controller = dragon.getAiSwimController();
        if (shoreExitActive) {
            tickShoreExit(context, controller);
            return;
        }
        if (context.memories().has(DragonMemories.MOVEMENT_INTENT)) {
            return;
        }
        if (movementLocked.test(dragon, target)) {
            controller.pause();
            return;
        }
        Entity movementAnchor = DragonTargetingHelper.movementAnchor(target);
        Vec3 targetPosition = movementAnchor.getBoundingBox().getCenter();
        double speed = speedModifier.applyAsDouble(dragon, target);
        if (dragon instanceof SemiAquaticDragon swimmer) {
            speed *= swimmer.getSwimSpeed();
        }
        if (dragon.distanceToSqr(movementAnchor) > 225.0D) {
            speed *= 1.5D;
        }
        controller.trackTarget(targetPosition, speed, turnSpeed);
        controller.serverTick();
        Vec3 shoreExit = findCombatShoreExitDirection(dragon, target, controller);
        if (shoreExit != null) {
            beginShoreExit(context, controller, shoreExit);
        }
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        resetShoreExit();
        context.dragon().getAiSwimController().stop();
    }

    private boolean isWaterCombatContext(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        return dragon.isInWaterOrBubble()
                && !dragon.isVehicle()
                && target != null
                && dragon.isTargetValid(target);
    }

    @Nullable
    private Vec3 findCombatShoreExitDirection(T dragon,
                                              LivingEntity target,
                                              AsyncSwimController controller) {
        Entity movementAnchor = DragonTargetingHelper.movementAnchor(target);
        if (!dragon.isInWaterOrBubble() || movementAnchor.isInWaterOrBubble()) {
            return null;
        }

        boolean routeNeedsExit = dragon.horizontalCollision
                || controller.hasReachedPathEnd()
                || !controller.isMoving();
        if (!routeNeedsExit) {
            return null;
        }

        Vec3 endpoint = controller.getPathEndpoint();
        boolean pathComplete = controller.hasReachedPathEnd();
        Vec3 towardExit = pathComplete || endpoint == null
                ? movementAnchor.position().subtract(dragon.position())
                : endpoint.subtract(dragon.position());
        towardExit = new Vec3(towardExit.x, 0.0D, towardExit.z);
        if (towardExit.lengthSqr() < 1.0E-4D) {
            towardExit = new Vec3(
                    movementAnchor.getX() - dragon.getX(), 0.0D,
                    movementAnchor.getZ() - dragon.getZ());
        }
        if (towardExit.lengthSqr() < 1.0E-4D) {
            return null;
        }

        Vec3 direction = towardExit.normalize();
        int referenceY = endpoint == null
                ? Mth.floor(dragon.getBoundingBox().minY)
                : Math.max(
                        Mth.floor(dragon.getBoundingBox().minY),
                        Mth.floor(endpoint.y) + 1
                );
        return hasDryShoreImmediatelyAhead(dragon, direction, referenceY) ? direction : null;
    }

    private void beginShoreExit(DragonBrainContext<T> context,
                                AsyncSwimController controller,
                                Vec3 direction) {
        T dragon = context.dragon();
        shoreExitActive = true;
        shoreExitTicks = 0;
        stableDryGroundTicks = 0;
        shoreExitMadeLandContact = false;
        shoreExitOrigin = dragon.position();
        shoreExitDirection = direction;
        controller.clear();
        dragon.getAIMovement().stop();
        tickShoreExit(context, controller);
    }

    private boolean hasDryShoreImmediatelyAhead(T dragon, Vec3 direction, int referenceY) {
        double halfWidth = dragon.getBbWidth() * 0.5D;
        double frontEdge = halfWidth * (Math.abs(direction.x) + Math.abs(direction.z));
        double lateralReach = Math.min(2.0D, Math.max(0.75D, halfWidth * 0.65D));
        Vec3 lateral = new Vec3(-direction.z, 0.0D, direction.x);

        for (int forwardSample = 0; forwardSample < 3; forwardSample++) {
            double forwardDistance = frontEdge + 0.3D + forwardSample * 0.5D;
            for (int lateralSample = -1; lateralSample <= 1; lateralSample++) {
                Vec3 sample = dragon.position()
                        .add(direction.scale(forwardDistance))
                        .add(lateral.scale(lateralReach * lateralSample));
                if (hasStandableDryLand(dragon, Mth.floor(sample.x), Mth.floor(sample.z), referenceY)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasStandableDryLand(T dragon, int x, int z, int referenceY) {
        for (int offset = 0; offset <= 3; offset++) {
            if (isStandableDryLand(dragon, new BlockPos(x, referenceY + offset, z))) {
                return true;
            }
            if (offset > 0
                    && isStandableDryLand(dragon, new BlockPos(x, referenceY - offset, z))) {
                return true;
            }
        }
        return false;
    }

    private boolean isStandableDryLand(T dragon, BlockPos feet) {
        if (!dragon.level().hasChunkAt(feet)
                || !dragon.level().getFluidState(feet).isEmpty()
                || dragon.level().getBlockState(feet.below())
                .getCollisionShape(dragon.level(), feet.below()).isEmpty()) {
            return false;
        }

        int requiredHeight = Math.max(2, Mth.ceil(dragon.getBbHeight()));
        for (int dy = 0; dy < requiredHeight; dy++) {
            BlockPos clearance = feet.above(dy);
            if (!dragon.level().getFluidState(clearance).isEmpty()
                    || !dragon.level().getBlockState(clearance)
                    .getCollisionShape(dragon.level(), clearance).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void tickShoreExit(DragonBrainContext<T> context,
                               AsyncSwimController controller) {
        T dragon = context.dragon();
        if (shoreExitOrigin == null || shoreExitDirection == null) {
            resetShoreExit();
            return;
        }

        shoreExitTicks++;
        context.memories().erase(DragonMemories.MOVEMENT_INTENT);
        context.memories().erase(DragonMemories.WALK_TARGET);
        context.memories().erase(DragonMemories.PATH);
        context.memories().erase(DragonMemories.CANT_REACH_WALK_TARGET_SINCE);
        dragon.getNavigation().stop();
        controller.pause();

        boolean inWater = dragon.isInWaterOrBubble();
        if (!inWater) {
            shoreExitMadeLandContact = true;
        }
        applyShoreExitMovement(dragon, inWater, shoreExitMadeLandContact);

        Vec3 displacement = dragon.position().subtract(shoreExitOrigin);
        double forwardProgress = displacement.x * shoreExitDirection.x
                + displacement.z * shoreExitDirection.z;
        double requiredClearance = Math.max(2.5D, dragon.getBbWidth() * 0.85D);
        if (!inWater
                && dragon.isGroundedForAction()
                && forwardProgress >= requiredClearance) {
            stableDryGroundTicks++;
        } else {
            stableDryGroundTicks = 0;
        }

        if (stableDryGroundTicks >= SHORE_EXIT_STABLE_GROUND_TICKS
                || shoreExitTicks >= SHORE_EXIT_MAX_TICKS) {
            resetShoreExit();
        }
    }

    private void applyShoreExitMovement(T dragon,
                                        boolean inWater,
                                        boolean madeLandContact) {
        Vec3 direction = shoreExitDirection;
        if (direction == null) {
            return;
        }

        double bodyScale = Math.max(dragon.getBbWidth(), dragon.getBbHeight());
        Vec3 velocity = dragon.getDeltaMovement();
        double minimumForward = Mth.clamp(0.12D + bodyScale * 0.018D, 0.16D, 0.26D);
        double currentForward = velocity.x * direction.x + velocity.z * direction.z;
        double correction = Math.max(0.0D, minimumForward - currentForward);
        double verticalVelocity = velocity.y;
        if (inWater) {
            double minimumLift = madeLandContact
                    ? 0.04D
                    : Mth.clamp(0.10D + bodyScale * 0.025D, 0.14D, 0.36D);
            verticalVelocity = Math.max(verticalVelocity, minimumLift);
        } else if (dragon.horizontalCollision) {
            verticalVelocity = Math.max(verticalVelocity, 0.12D);
        }

        dragon.setDeltaMovement(
                velocity.x + direction.x * correction,
                verticalVelocity,
                velocity.z + direction.z * correction
        );
        dragon.hasImpulse = true;
    }

    private void resetShoreExit() {
        shoreExitActive = false;
        shoreExitTicks = 0;
        stableDryGroundTicks = 0;
        shoreExitMadeLandContact = false;
        shoreExitOrigin = null;
        shoreExitDirection = null;
    }
}
