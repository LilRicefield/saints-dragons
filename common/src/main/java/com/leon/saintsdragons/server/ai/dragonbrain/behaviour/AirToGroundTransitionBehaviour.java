package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.DragonAirCombatSettingsProvider;
import com.leon.saintsdragons.server.ai.GroundPursuitFlightSettings;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonFlightEligibility;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonTargetLifecycle;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonInvestigation;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonLocomotionMode;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public final class AirToGroundTransitionBehaviour<T extends DragonEntity> extends DragonBehaviour<T> {
    private final GroundPursuitFlightSettings pursuitSettings = GroundPursuitFlightSettings.standard();
    private long nextAttemptTick;
    private String handoff = "idle";

    public AirToGroundTransitionBehaviour() {
        super(Map.of(DragonMemories.LOCOMOTION_MODE, MemoryStatus.REGISTERED), false);
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        return transitionDragon(context.dragon()) != null;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        return canStart(context);
    }

    private boolean needsLanding(DragonBrainContext<T> context) {
        TransitionDragon transition = transitionDragon(context.dragon());
        if (transition == null || !transition.dragon().isAerial()
                || DragonFlightEligibility.movementBlockReason(transition.dragon()) != null
                || transition.dragon().getActiveAbility() != null
                || DragonInvestigation.shouldPreserveAirbornePursuit(context.dragon())) {
            return false;
        }

        boolean groundRouteAbandoned = context.memories()
                .get(DragonMemories.GROUND_ROUTE_ABANDONED)
                .orElse(false);
        boolean hasTacticalLanding = context.memories().has(DragonMemories.TACTICAL_LANDING_POSITION);
        if (groundRouteAbandoned && !hasTacticalLanding) {
            return false;
        }
        if (transition.dragon().getLocomotionMode() != DragonLocomotionMode.AIR) {
            return false;
        }
        if (transition.dragon().isLanding() && transition.dragon().getAIMovement().isPathing()) {
            return false;
        }
        if (!hasTacticalLanding
                && !transition.dragon().isLanding()
                && context.memories().get(DragonMemories.TARGET_AIRBORNE).orElse(false)) {
            return false;
        }
        if (transition.dragon().isLanding()) {
            return true;
        }
        return context.memories().get(DragonMemories.ATTACK_TARGET)
                .filter(target -> DragonTargetLifecycle.isValidTarget(transition.dragon(), target))
                .isPresent();
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        TransitionDragon current = transitionDragon(context.dragon());
        if (current != null && !current.dragon().isAerial() && current.dragon().onGround()) {
            if (context.memories().has(DragonMemories.TACTICAL_LANDING_POSITION)) {
                context.memories().erase(DragonMemories.TACTICAL_LANDING_POSITION);
                context.memories().erase(DragonMemories.GROUND_ROUTE_ABANDONED);
                context.memories().erase(DragonMemories.CANT_REACH_WALK_TARGET_SINCE);
            }
            return;
        }
        if (!needsLanding(context) || context.gameTime() < nextAttemptTick) {
            return;
        }
        TransitionDragon transition = transitionDragon(context.dragon());
        if (transition == null) {
            return;
        }

        double landingSpeed = transition.settings().getAiAirCombatSettings().landingSpeed();
        Vec3 tacticalLanding = context.memories()
                .get(DragonMemories.TACTICAL_LANDING_POSITION)
                .orElse(null);
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (tacticalLanding == null && target != null) {
            tacticalLanding = transition.dragon().getAIMovement().findTacticalGroundTransitionTarget(
                    target,
                    pursuitSettings.landingSearchRadius(),
                    pursuitSettings.landingMaxVerticalDelta()
            );
        }
        // A reservation is not an accepted movement command. Keep the handoff here,
        // so an outgoing combat behaviour cannot leave an unconsumed landing intent.
        boolean accepted = tacticalLanding != null
                ? transition.dragon().getAIMovement().requestGroundTransition(tacticalLanding, landingSpeed)
                : target == null && transition.dragon().getAIMovement().requestGroundTransition((LivingEntity) null, landingSpeed);
        if (accepted) {
            if (tacticalLanding != null) {
                context.memories().set(DragonMemories.TACTICAL_LANDING_POSITION, tacticalLanding);
            }
            handoff = "landing-accepted";
        } else {
            context.memories().erase(DragonMemories.TACTICAL_LANDING_POSITION);
            context.memories().set(DragonMemories.GROUND_ROUTE_ABANDONED, true);
            transition.dragon().setLanding(false);
            transition.dragon().beginAiFlight();
            handoff = "landing-rejected:resume-pursuit";
        }
        nextAttemptTick = context.gameTime() + pursuitSettings.landingSearchIntervalTicks();
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        return Map.of("handoff", handoff);
    }

    private static TransitionDragon transitionDragon(DragonEntity dragon) {
        if (dragon instanceof RideableFlyingDragon flyingDragon
                && dragon instanceof DragonAirCombatSettingsProvider settings) {
            return new TransitionDragon(flyingDragon, settings);
        }
        return null;
    }

    private record TransitionDragon(RideableFlyingDragon dragon,
                                    DragonAirCombatSettingsProvider settings) {
    }
}
