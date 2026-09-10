package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.navigation.async.DragonFlightRequest;

import com.leon.saintsdragons.server.ai.DragonAirCombatSettings;
import com.leon.saintsdragons.server.ai.DragonAirCombatSettingsProvider;
import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonFlightEligibility;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonInvestigation;
import com.leon.saintsdragons.server.ai.DragonAirCombatHelper;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public abstract class AirCombatMovementBehaviour<T extends RideableFlyingDragon & DragonAirCombatSettingsProvider>
        extends DragonBehaviour<T> {
    private int lostSightTicks;
    private String blockedReason = "not-evaluated";

    protected AirCombatMovementBehaviour() {
        super(Map.of(DragonMemories.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected final boolean canStart(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        prepareStartConditions(dragon, target);
        return isValidAirTarget(context, target) && checkExtraStartConditions(dragon, target);
    }

    @Override
    protected final boolean canContinue(DragonBrainContext<T> context) {
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        return isValidAirTarget(context, target)
                && context.dragon().isAerial()
                && checkExtraContinueConditions(context.dragon(), target);
    }

    @Override
    protected final void start(DragonBrainContext<T> context) {
        lostSightTicks = 0;
        T dragon = context.dragon();
        DragonAirCombatHelper.startAirCombat(dragon, settings(dragon).takeoffAnimationTicks());
        startAirCombat(context);
    }

    @Override
    protected final void tick(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        DragonAirCombatSettings settings = settings(dragon);
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (target == null) {
            return;
        }
        if (context.memories().has(DragonMemories.TACTICAL_LANDING_POSITION)) {
            return;
        }

        if (dragon.isLanding()) {
            return;
        }

        if (dragon.isTakeoff() && dragon.isFlying() && !dragon.onGround()) {
            dragon.beginAiFlight();
        }

        boolean hasLineOfSight = dragon.getSensing().hasLineOfSight(target);
        if (!hasLineOfSight && DragonInvestigation.shouldPreserveAirbornePursuit(dragon)) {
            lostSightTicks = 0;
            return;
        }
        int lostSightLandingTicks = settings.lostSightLandingTicks();
        if (lostSightLandingTicks > 0 && !isGroundRouteAbandoned(context)) {
            lostSightTicks = hasLineOfSight ? 0 : lostSightTicks + 1;
            if (lostSightTicks >= lostSightLandingTicks) {
                context.memories().set(
                        DragonMemories.MOVEMENT_INTENT,
                        DragonMovementIntent.transitionToGround(
                                DragonTargetingHelper.livingMovementAnchor(target),
                                settings.landingSpeed()
                        )
                );
                return;
            }
        }

        tickAirCombat(context, target, hasLineOfSight);
    }

    @Override
    protected final void stop(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        DragonAirCombatSettings settings = settings(dragon);
        lostSightTicks = 0;
        stopAirCombat(context);
        if (DragonInvestigation.shouldPreserveAirbornePursuit(dragon)) {
            return;
        }
        if (isGroundRouteAbandoned(context)) {
            // The shared transition behaviour owns accepting/rejecting the landing.
            return;
        }
        DragonAirCombatHelper.stopAirCombatAndLandWhenTargetLost(
                dragon,
                dragon.getTarget(),
                settings.landingSpeed(),
                target -> DragonAirCombatHelper.isTargetAirborne(
                        dragon,
                        target,
                        dragon.getAiTargetAirborneHeight(target)
                ),
                DragonAirCombatHelper.maxAggroDistanceSqr(dragon, settings.fallbackFollowRange())
        );
    }

    protected abstract void tickAirCombat(DragonBrainContext<T> context,
                                          LivingEntity target,
                                          boolean hasLineOfSight);

    protected void startAirCombat(DragonBrainContext<T> context) {
    }

    protected void stopAirCombat(DragonBrainContext<T> context) {
    }

    protected boolean checkExtraStartConditions(T dragon, LivingEntity target) {
        return true;
    }

    protected void prepareStartConditions(T dragon, LivingEntity target) {
    }

    protected boolean checkExtraContinueConditions(T dragon, LivingEntity target) {
        return true;
    }

    protected final boolean shouldDiveChase(T dragon,
                                            LivingEntity target,
                                            double minHeightAdvantage,
                                            double maxHorizontalDistance) {
        return DragonAirCombatHelper.shouldDiveChase(
                dragon,
                target,
                dragon.getAiTargetAirborneHeight(target),
                minHeightAdvantage,
                maxHorizontalDistance
        );
    }

    protected final void setMeleePositionIntent(DragonBrainContext<T> context,
                                                LivingEntity target,
                                                double targetHeightOffset,
                                                double approachDistance,
                                                double farSpeed,
                                                double nearSpeed) {
        T dragon = context.dragon();
        Entity movementAnchor = DragonTargetingHelper.movementAnchor(target);
        double targetY = movementAnchor.getY()
                + movementAnchor.getBbHeight() * 0.5D
                + targetHeightOffset;
        Vec3 toTarget = new Vec3(
                movementAnchor.getX() - dragon.getX(),
                targetY - dragon.getY(),
                movementAnchor.getZ() - dragon.getZ()
        );
        double distance = toTarget.length();
        if (distance < 1.0E-4D) {
            return;
        }
        Vec3 direction = toTarget.scale(1.0D / distance);
        Vec3 destination = new Vec3(movementAnchor.getX(), targetY, movementAnchor.getZ())
                .subtract(direction.scale(approachDistance));
        double speed = distance > approachDistance ? farSpeed : nearSpeed;
        context.memories().set(DragonMemories.MOVEMENT_INTENT, DragonMovementIntent.flight(DragonFlightRequest.chase(destination, speed)));
    }

    protected final void setPredictedChaseIntent(DragonBrainContext<T> context,
                                                 LivingEntity target,
                                                 double predictionTicks,
                                                 double heightOffset,
                                                 double bobFrequency,
                                                 double bobAmplitude,
                                                 double speed) {
        context.memories().set(DragonMemories.MOVEMENT_INTENT, DragonMovementIntent.flight(
                DragonFlightRequest.chase(
                        predictedChaseDestination(context, target, predictionTicks, heightOffset, bobFrequency, bobAmplitude),
                        speed)));
    }

    protected final void setDivingChaseIntent(DragonBrainContext<T> context,
                                              LivingEntity target,
                                              double predictionTicks,
                                              double heightOffset,
                                              double bobFrequency,
                                              double bobAmplitude,
                                              double speed) {
        T dragon = context.dragon();
        Vec3 base = predictedChaseDestination(context, target, predictionTicks, heightOffset, bobFrequency, bobAmplitude);
        // Intercept the target's altitude; forcing a dive angle can aim far below it.
        double targetY = base.y + DragonTargetingHelper.movementAnchor(target).getDeltaMovement().y * predictionTicks;
        Vec3 destination = new Vec3(base.x, targetY, base.z);
        var clearance = dragon.getAIMovement().flightSpace().observe(destination);
        if (clearance != null && clearance.floorKnown()) {
            destination = new Vec3(base.x, Math.max(targetY, clearance.floor() + 6.0D), base.z);
        }
        context.memories().set(DragonMemories.MOVEMENT_INTENT,
                DragonMovementIntent.flight(destination.y < dragon.getY() - 2.0D
                        ? DragonFlightRequest.dive(destination, speed)
                        : DragonFlightRequest.chase(destination, speed)));
    }

    private Vec3 predictedChaseDestination(DragonBrainContext<T> context,
                                           LivingEntity target,
                                           double predictionTicks,
                                           double heightOffset,
                                           double bobFrequency,
                                           double bobAmplitude) {
        T dragon = context.dragon();
        Entity movementAnchor = DragonTargetingHelper.movementAnchor(target);
        Vec3 velocity = movementAnchor.getDeltaMovement();
        return new Vec3(
                movementAnchor.getX() + velocity.x * predictionTicks,
                movementAnchor.getY() + movementAnchor.getBbHeight() + heightOffset
                        + Math.sin(dragon.tickCount * bobFrequency) * bobAmplitude,
                movementAnchor.getZ() + velocity.z * predictionTicks
        );
    }

    private boolean isValidAirTarget(DragonBrainContext<T> context, LivingEntity target) {
        T dragon = context.dragon();
        blockedReason = target == null ? "no-target" : DragonFlightEligibility.pursuitBlockReason(
                dragon, target, context.memories().get(DragonMemories.TARGET_AIRBORNE).orElse(false),
                isGroundRouteAbandoned(context), context.memories().has(DragonMemories.TACTICAL_LANDING_POSITION));
        return blockedReason == null;
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        return Map.of("flight_block", blockedReason == null ? "none" : blockedReason);
    }

    private boolean isGroundRouteAbandoned(DragonBrainContext<T> context) {
        return context.memories().get(DragonMemories.GROUND_ROUTE_ABANDONED).orElse(false);
    }

    private DragonAirCombatSettings settings(T dragon) {
        return dragon.getAiAirCombatSettings();
    }
}
