package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.DragonAirCombatSettingsProvider;
import com.leon.saintsdragons.server.ai.GroundPursuitFlightSettings;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.DragonAirCombatHelper;
import com.leon.saintsdragons.server.entity.base.DragonLocomotionMode;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.function.Predicate;

public class GroundPursuitFlightTransitionBehaviour<
        T extends RideableFlyingDragon & DragonAirCombatSettingsProvider> extends DragonBehaviour<T> {
    private final GroundPursuitFlightSettings settings;
    private final Predicate<T> transitionLocked;
    private final Predicate<T> groundFallbackFlightAllowed;

    private int airPursuitTicks;
    private int landingSearchCooldown;
    private int landingFailureTicks;

    public GroundPursuitFlightTransitionBehaviour(GroundPursuitFlightSettings settings) {
        this(settings, dragon -> dragon.getActiveAbility() != null, dragon -> true);
    }

    public GroundPursuitFlightTransitionBehaviour(GroundPursuitFlightSettings settings,
                                                   Predicate<T> transitionLocked) {
        this(settings, transitionLocked, dragon -> true);
    }

    public GroundPursuitFlightTransitionBehaviour(GroundPursuitFlightSettings settings,
                                                   Predicate<T> transitionLocked,
                                                   Predicate<T> groundFallbackFlightAllowed) {
        super(Map.of(DragonMemories.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT), false);
        this.settings = settings;
        this.transitionLocked = transitionLocked;
        this.groundFallbackFlightAllowed = groundFallbackFlightAllowed;
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        return isTransitionContext(context);
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        return isTransitionContext(context);
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        resetProgressTracking();
        if (isGroundRouteAbandoned(context)) {
            airPursuitTicks = settings.minAirPursuitTicks();
        }
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (target == null) {
            clearTransition(context);
            return;
        }
        if (isGroundRouteAbandoned(context)) {
            tickAbandonedGroundRoute(context, target);
        } else {
            tickGroundProgress(context, target);
        }
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        clearTransition(context);
    }

    private void tickGroundProgress(DragonBrainContext<T> context, LivingEntity target) {
        T dragon = context.dragon();
        if (dragon.getLocomotionMode() != DragonLocomotionMode.GROUND
                || dragon.isInWaterOrBubble()) {
            resetProgressTracking();
            return;
        }
        if (transitionLocked.test(dragon)) {
            context.memories().erase(DragonMemories.CANT_REACH_WALK_TARGET_SINCE);
            resetProgressTracking();
            return;
        }
        double distance = dragon.distanceTo(target);
        double horizontalDistance = horizontalDistance(dragon, target);
        double upwardSeparation = target.getY() - dragon.getY();
        if (upwardSeparation >= settings.highGroundMinVerticalSeparation()
                && horizontalDistance <= settings.highGroundMaxHorizontalDistance()) {
            abandonGroundRoute(context);
            return;
        }
        if (distance <= settings.minPursuitDistance()) {
            context.memories().erase(DragonMemories.CANT_REACH_WALK_TARGET_SINCE);
            resetProgressTracking();
            return;
        }
        if (dragon.getAIMovement().hasRepeatedGroundPathFailures()) {
            abandonGroundRoute(context);
            return;
        }

        long unreachableSince = context.memories()
                .get(DragonMemories.CANT_REACH_WALK_TARGET_SINCE)
                .orElse(Long.MAX_VALUE);
        if (unreachableSince != Long.MAX_VALUE
                && context.gameTime() - unreachableSince >= settings.stallTicks()) {
            abandonGroundRoute(context);
        }
    }

    private void abandonGroundRoute(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        if (!groundFallbackFlightAllowed.test(dragon)
                || !DragonAirCombatHelper.canTriggerAiFlight(dragon)) {
            context.memories().erase(DragonMemories.CANT_REACH_WALK_TARGET_SINCE);
            resetProgressTracking();
            return;
        }
        context.memories().set(DragonMemories.GROUND_ROUTE_ABANDONED, true);
        context.memories().erase(DragonMemories.WALK_TARGET);
        context.memories().erase(DragonMemories.PATH);
        context.memories().erase(DragonMemories.TACTICAL_LANDING_POSITION);
        context.memories().erase(DragonMemories.MOVEMENT_INTENT);
        dragon.getAIMovement().stopAndClearAllMovement();
        dragon.getAIMovement().clearGroundPathFailureHistory();
        DragonAirCombatHelper.startOrResumeFlight(
                dragon,
                dragon.getAiAirCombatSettings().takeoffAnimationTicks()
        );
        resetProgressTracking();
    }

    private void tickAbandonedGroundRoute(DragonBrainContext<T> context, LivingEntity target) {
        T dragon = context.dragon();
        boolean targetAirborne = context.memories().get(DragonMemories.TARGET_AIRBORNE).orElse(false);
        Vec3 landingPosition = context.memories()
                .get(DragonMemories.TACTICAL_LANDING_POSITION)
                .orElse(null);

        if (!dragon.isAerial()
                && !dragon.isTakeoff()
                && !groundFallbackFlightAllowed.test(dragon)) {
            clearTransition(context);
            return;
        }

        if (targetAirborne) {
            if (landingPosition != null || dragon.isLanding()) {
                abandonTacticalLanding(context);
            } else if (!dragon.isAerial()
                    && !dragon.isTakeoff()
                    && DragonAirCombatHelper.canTriggerAiFlight(dragon)) {
                DragonAirCombatHelper.startOrResumeFlight(
                        dragon,
                        dragon.getAiAirCombatSettings().takeoffAnimationTicks()
                );
            }
            return;
        }

        if (!dragon.isAerial() && !dragon.isTakeoff()) {
            if (dragon.onGround() && landingPosition != null) {
                clearTransition(context);
                return;
            }
            if (DragonAirCombatHelper.canTriggerAiFlight(dragon)) {
                DragonAirCombatHelper.startOrResumeFlight(
                        dragon,
                        dragon.getAiAirCombatSettings().takeoffAnimationTicks()
                );
            }
            return;
        }

        if (landingPosition != null) {
            if (!dragon.getAIMovement().isTacticalGroundTransitionTargetValid(
                    landingPosition,
                    target,
                    settings.landingSearchRadius(),
                    settings.landingMaxVerticalDelta()
            )) {
                abandonTacticalLanding(context);
                return;
            }
            if (dragon.isLanding()) {
                if (dragon.isFlightControllerStuck() || !dragon.getAIMovement().isPathing()) {
                    landingFailureTicks++;
                    if (landingFailureTicks >= settings.landingFailureTimeoutTicks()) {
                        abandonTacticalLanding(context);
                    }
                } else {
                    landingFailureTicks = 0;
                }
            }
            return;
        }

        landingFailureTicks = 0;
        if (dragon.isLanding()) {
            dragon.getAIMovement().clearAllWaypoints();
            dragon.setLanding(false);
            dragon.beginAiFlight();
        }
        if (dragon.isTakeoff()) {
            return;
        }
        if (transitionLocked.test(dragon)) {
            return;
        }

        airPursuitTicks++;
        if (airPursuitTicks < settings.minAirPursuitTicks()) {
            return;
        }
        if (landingSearchCooldown > 0) {
            landingSearchCooldown--;
            return;
        }
        landingSearchCooldown = settings.landingSearchIntervalTicks();

        Vec3 candidate = dragon.getAIMovement().findTacticalGroundTransitionTarget(
                target,
                settings.landingSearchRadius(),
                settings.landingMaxVerticalDelta()
        );
        if (candidate != null) {
            context.memories().set(DragonMemories.TACTICAL_LANDING_POSITION, candidate);
        }
    }

    private void abandonTacticalLanding(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        context.memories().erase(DragonMemories.TACTICAL_LANDING_POSITION);
        context.memories().erase(DragonMemories.MOVEMENT_INTENT);
        dragon.getAIMovement().clearAllWaypoints();
        dragon.setLanding(false);
        dragon.beginAiFlight();
        landingFailureTicks = 0;
        landingSearchCooldown = settings.landingSearchIntervalTicks();
    }

    private boolean isTransitionContext(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (target == null
                || !DragonAirCombatHelper.isValidCombatTarget(dragon, target)
                || dragon.isBaby()
                || dragon.isVehicle()
                || dragon.isPassenger()
                || dragon.isOrderedToSit()
                || dragon.isInWaterOrBubble()) {
            return false;
        }
        if (isGroundRouteAbandoned(context)) {
            return true;
        }
        if (context.memories().get(DragonMemories.TARGET_AIRBORNE).orElse(false)) {
            return false;
        }
        return !dragon.isAerial() && dragon.getLocomotionMode() == DragonLocomotionMode.GROUND;
    }

    private boolean isGroundRouteAbandoned(DragonBrainContext<T> context) {
        return context.memories().get(DragonMemories.GROUND_ROUTE_ABANDONED).orElse(false);
    }

    private void clearTransition(DragonBrainContext<T> context) {
        context.memories().erase(DragonMemories.GROUND_ROUTE_ABANDONED);
        context.memories().erase(DragonMemories.TACTICAL_LANDING_POSITION);
        context.memories().erase(DragonMemories.CANT_REACH_WALK_TARGET_SINCE);
        context.memories().erase(DragonMemories.WALK_TARGET);
        context.memories().erase(DragonMemories.PATH);
        resetProgressTracking();
    }

    private void resetProgressTracking() {
        airPursuitTicks = 0;
        landingSearchCooldown = 0;
        landingFailureTicks = 0;
    }

    private static double horizontalDistance(RideableFlyingDragon dragon, LivingEntity target) {
        double dx = target.getX() - dragon.getX();
        double dz = target.getZ() - dragon.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }
}
