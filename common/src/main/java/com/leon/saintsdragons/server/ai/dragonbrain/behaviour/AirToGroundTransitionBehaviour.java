package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.DragonAirCombatSettingsProvider;
import com.leon.saintsdragons.server.ai.GroundPursuitFlightSettings;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonOneShotBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonTargetLifecycle;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonLocomotionMode;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public final class AirToGroundTransitionBehaviour<T extends DragonEntity> extends DragonOneShotBehaviour<T> {
    private final GroundPursuitFlightSettings pursuitSettings = GroundPursuitFlightSettings.standard();

    public AirToGroundTransitionBehaviour() {
        super(Map.of(DragonMemories.LOCOMOTION_MODE, MemoryStatus.REGISTERED));
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        TransitionDragon transition = transitionDragon(context.dragon());
        if (transition == null || !transition.dragon().isAerial()) {
            return false;
        }

        boolean groundRouteAbandoned = context.memories()
                .get(DragonMemories.GROUND_ROUTE_ABANDONED)
                .orElse(false);
        boolean hasTacticalLanding = context.memories().has(DragonMemories.TACTICAL_LANDING_POSITION);
        if (groundRouteAbandoned && !hasTacticalLanding) {
            return false;
        }
        DragonLocomotionMode mode = context.memories()
                .get(DragonMemories.LOCOMOTION_MODE)
                .orElse(transition.dragon().getLocomotionMode());
        if (mode != DragonLocomotionMode.AIR) {
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
    protected void start(DragonBrainContext<T> context) {
        TransitionDragon transition = transitionDragon(context.dragon());
        if (transition == null) {
            return;
        }

        double landingSpeed = transition.settings().getAiAirCombatSettings().landingSpeed();
        Vec3 tacticalLanding = context.memories()
                .get(DragonMemories.TACTICAL_LANDING_POSITION)
                .orElse(null);
        if (tacticalLanding != null) {
            context.memories().set(
                    DragonMemories.MOVEMENT_INTENT,
                    DragonMovementIntent.transitionToGround(tacticalLanding, landingSpeed)
            );
            return;
        }

        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (target != null) {
            Vec3 landingTarget = transition.dragon().getAIMovement().findTacticalGroundTransitionTarget(
                    target,
                    pursuitSettings.landingSearchRadius(),
                    pursuitSettings.landingMaxVerticalDelta()
            );
            if (landingTarget != null) {
                context.memories().set(
                        DragonMemories.MOVEMENT_INTENT,
                        DragonMovementIntent.transitionToGround(landingTarget, landingSpeed)
                );
            } else {
                context.memories().set(DragonMemories.GROUND_ROUTE_ABANDONED, true);
            }
        } else if (transition.dragon().isLanding()) {
            context.memories().set(
                    DragonMemories.MOVEMENT_INTENT,
                    DragonMovementIntent.transitionToGround(landingSpeed)
            );
        }
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
