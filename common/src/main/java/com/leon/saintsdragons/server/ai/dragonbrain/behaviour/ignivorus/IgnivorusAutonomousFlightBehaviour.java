package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ignivorus;

import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.AutonomousFlightBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonFlightEligibility;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.DragonFlightBehaviorProfile;
import com.leon.saintsdragons.server.ai.navigation.async.DragonFlightRequest;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public class IgnivorusAutonomousFlightBehaviour extends AutonomousFlightBehaviour<Ignivorus> {
    private static final double LANDING_SPEED = 1.5D;
    private static final int LANDING_RETRY_TICKS = 20;
    private boolean recoveringPhase2Flight;
    private long nextLandingAttempt;
    private String landingRecovery = "inactive";
    private int landingAttempts;
    private int repositionAttempts;

    public IgnivorusAutonomousFlightBehaviour() {
        super(DragonFlightBehaviorProfile.ignivorus(), 2.25D, LANDING_SPEED, Ignivorus.TAKEOFF_ANIMATION_TICKS);
    }

    @Override
    protected boolean canStart(DragonBrainContext<Ignivorus> context) {
        recoveringPhase2Flight = context.dragon().isPhase2Active() && canRecoverIdleFlight(context)
                && !context.dragon().getAIMovement().isPathing()
                && !context.dragon().getAIMovement().hasActiveLandingTransition();
        return recoveringPhase2Flight || super.canStart(context);
    }

    @Override
    protected boolean canContinue(DragonBrainContext<Ignivorus> context) {
        return recoveringPhase2Flight ? canRecoverIdleFlight(context) : super.canContinue(context);
    }

    @Override
    protected void start(DragonBrainContext<Ignivorus> context) {
        if (!recoveringPhase2Flight) {
            super.start(context);
            return;
        }
        nextLandingAttempt = context.gameTime();
        landingAttempts = 0;
        repositionAttempts = 0;
        landingRecovery = "starting";
        context.dragon().getAIMovement().stopAndClearAllMovement();
        tickLandingRecovery(context);
    }

    @Override
    protected void tick(DragonBrainContext<Ignivorus> context) {
        if (recoveringPhase2Flight) tickLandingRecovery(context);
        else super.tick(context);
    }

    private boolean canRecoverIdleFlight(DragonBrainContext<Ignivorus> context) {
        Ignivorus dragon = context.dragon();
        return dragon.isAlive() && !dragon.isRemoved() && dragon.isAerial() && !dragon.onGround()
                && DragonFlightEligibility.movementBlockReason(dragon) == null
                && !dragon.isTakeoff() && !dragon.isInLove() && !hasCommittedAction(dragon)
                && !dragon.isAiSpecialCombatActive() && !dragon.isTamingStunned()
                && !dragon.isTargetValid(dragon.getTarget())
                && !context.memories().has(DragonMemories.ATTACK_TARGET)
                && !context.memories().has(DragonMemories.INVESTIGATION_TARGET)
                && !context.memories().has(DragonMemories.RESCUE_TARGET)
                && !context.memories().has(DragonMemories.BREED_TARGET)
                && !context.memories().has(DragonMemories.MOVEMENT_INTENT);
    }

    private void tickLandingRecovery(DragonBrainContext<Ignivorus> context) {
        Ignivorus dragon = context.dragon();
        dragon.setAccelerating(false);
        var movement = dragon.getAIMovement();
        boolean landing = movement.hasActiveLandingTransition();
        if (movement.isPathing()) {
            landingRecovery = landing ? "landing" : "repositioning";
            return;
        }
        if (context.gameTime() < nextLandingAttempt) return;
        nextLandingAttempt = context.gameTime() + LANDING_RETRY_TICKS;

        boolean failedLanding = dragon.isFlightControllerFailed() && "landing".equals(landingRecovery);
        if (!failedLanding) {
            landingAttempts++;
            if (movement.requestGroundTransition((LivingEntity) null, LANDING_SPEED)) {
                landingRecovery = "landing";
                return;
            }
        }

        repositionAttempts++;
        Vec3 reposition = dragon.findStandardAiFlightTarget(360.0D, 16.0D, 16.0D, 12.0D, true);
        if (reposition == null) {
            landingRecovery = "no-clear-reposition";
        } else if (!isCruiseTargetAllowed(dragon, reposition)) {
            landingRecovery = "reposition-outside-roost";
        } else if (movement.requestFlight(DragonFlightRequest.cruise(reposition, LANDING_SPEED))) {
            dragon.beginAiFlight();
            landingRecovery = "repositioning";
        } else {
            landingRecovery = "reposition-rejected";
        }
    }

    @Override
    protected void stop(DragonBrainContext<Ignivorus> context) {
        if (recoveringPhase2Flight) {
            context.dragon().getAIMovement().stopAndClearAllMovement();
        }
        recoveringPhase2Flight = false;
        landingRecovery = "inactive";
        super.stop(context);
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        return Map.of(
                "phase2_landing_recovery", landingRecovery,
                "phase2_landing_attempts", Integer.toString(landingAttempts),
                "phase2_reposition_attempts", Integer.toString(repositionAttempts)
        );
    }

    @Override
    protected boolean canUseAutonomousFlight(Ignivorus dragon) {
        if (!super.canUseAutonomousFlight(dragon)
                || dragon.isTame()
                || dragon.isAiSpecialCombatActive()
                || dragon.isPhase2Active()
                || dragon.shouldSuspendRoostWandering()
                || dragon.isInWater()
                || dragon.isInWaterOrBubble()
                || dragon.isInLava()) {
            return false;
        }
        LivingEntity target = dragon.getTarget();
        return target == null || !dragon.isTargetValid(target);
    }

    @Override
    protected boolean canContinueAutonomousFlight(Ignivorus dragon) {
        if (!super.canContinueAutonomousFlight(dragon)
                || dragon.isTame()
                || dragon.isAiSpecialCombatActive()
                || dragon.isPhase2Active()
                || dragon.shouldSuspendRoostWandering()
                || dragon.isInWater()
                || dragon.isInWaterOrBubble()
                || dragon.isInLava()) {
            return false;
        }
        LivingEntity target = dragon.getTarget();
        return target == null || !dragon.isTargetValid(target);
    }

    @Override
    protected boolean shouldLandWhenAutonomousFlightBlocked(Ignivorus dragon) {
        return dragon.isTame()
                || dragon.isPhase2Active()
                || dragon.shouldSuspendRoostWandering()
                || dragon.isInWater()
                || dragon.isInWaterOrBubble()
                || dragon.isInLava();
    }

    @Override
    protected double getCruiseMinRange(Ignivorus dragon) {
        return 16.0D;
    }

    @Override
    protected double getCruiseExtraRange(Ignivorus dragon) {
        return Ignivorus.ROOST_WANDER_RADIUS - getCruiseMinRange(dragon);
    }

    @Override
    protected double getMaxHeightAboveGround(Ignivorus dragon) {
        return 60.0D;
    }

    @Override
    protected int getCruiseTargetAttempts(Ignivorus dragon) {
        return 10;
    }

    @Override
    protected boolean isCruiseTargetAllowed(Ignivorus dragon, Vec3 cruiseTarget) {
        return dragon.isWithinRoostWanderArea(cruiseTarget);
    }
}
