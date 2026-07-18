package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.GroundPursuitFlightSettings;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.entity.base.DragonLocomotionMode;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public class LandForGroundTargetBehaviour<T extends RideableFlyingDragon> extends DragonBehaviour<T> {
    private final double landingSpeed;
    private final GroundPursuitFlightSettings pursuitSettings;

    public LandForGroundTargetBehaviour(double landingSpeed) {
        this(landingSpeed, GroundPursuitFlightSettings.standard());
    }

    public LandForGroundTargetBehaviour(double landingSpeed,
                                        GroundPursuitFlightSettings pursuitSettings) {
        super(Map.of(DragonMemories.LOCOMOTION_MODE, MemoryStatus.REGISTERED));
        this.landingSpeed = landingSpeed;
        this.pursuitSettings = pursuitSettings;
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        if (!context.dragon().isAerial()) {
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
                .orElse(context.dragon().getLocomotionMode());
        if (mode != DragonLocomotionMode.AIR) {
            return false;
        }
        if (context.dragon().isLanding() && context.dragon().getAIMovement().isPathing()) {
            return false;
        }
        if (!hasTacticalLanding
                && !context.dragon().isLanding()
                && context.memories().get(DragonMemories.TARGET_AIRBORNE).orElse(false)) {
            return false;
        }
        if (context.dragon().isLanding()) {
            return true;
        }
        return context.memories().get(DragonMemories.ATTACK_TARGET)
                .filter(context.dragon()::isTargetValid)
                .isPresent();
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        Vec3 tacticalLanding = context.memories()
                .get(DragonMemories.TACTICAL_LANDING_POSITION)
                .orElse(null);
        if (tacticalLanding != null) {
            context.memories().set(DragonMemories.MOVEMENT_INTENT,
                    DragonMovementIntent.landing(tacticalLanding, landingSpeed));
            return;
        }
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (target != null) {
            Vec3 landingTarget = context.dragon().getAIMovement().findTacticalLandingTarget(
                    target,
                    pursuitSettings.landingSearchRadius(),
                    pursuitSettings.landingMaxVerticalDelta()
            );
            if (landingTarget != null) {
                context.memories().set(DragonMemories.MOVEMENT_INTENT,
                        DragonMovementIntent.landing(landingTarget, landingSpeed));
            } else {
                context.memories().set(DragonMemories.GROUND_ROUTE_ABANDONED, true);
            }
        } else if (context.dragon().isLanding()) {
            context.memories().set(DragonMemories.MOVEMENT_INTENT,
                    DragonMovementIntent.landing(landingSpeed));
        }
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        return false;
    }
}
