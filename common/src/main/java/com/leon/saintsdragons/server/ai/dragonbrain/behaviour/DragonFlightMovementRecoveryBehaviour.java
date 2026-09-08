package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.DragonAirCombatSettingsProvider;
import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.ai.dragonbrain.*;
import com.leon.saintsdragons.server.ai.dragonbrain.tactical.DragonTactic;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/** Last FIGHT producer: only repairs an otherwise unowned, visible-target flight handoff. */
public final class DragonFlightMovementRecoveryBehaviour<T extends DragonEntity> extends DragonBehaviour<T> {
    private final FlightMovementWatchdog watchdog = new FlightMovementWatchdog();
    private String state = "idle";
    private int recoveries;

    public DragonFlightMovementRecoveryBehaviour() {
        super(false);
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        return context.dragon() instanceof RideableFlyingDragon
                && context.dragon() instanceof DragonAirCombatSettingsProvider;
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        RideableFlyingDragon dragon = (RideableFlyingDragon) context.dragon();
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        state = blockedReason(context, dragon, target);
        if (!watchdog.tick(context.gameTime(), state == null)) return;

        // No executor accepted this reservation. Release it before restarting pursuit.
        context.memories().erase(DragonMemories.TACTICAL_LANDING_POSITION);
        context.memories().erase(DragonMemories.TACTICAL_COMMITMENT);
        context.memories().set(DragonMemories.GROUND_ROUTE_ABANDONED, true);
        dragon.setLanding(false);
        dragon.beginAiFlight();
        LivingEntity anchor = DragonTargetingHelper.livingMovementAnchor(target);
        Vec3 destination = anchor.position().add(0, anchor.getBbHeight() + 2.0D, 0);
        double speed = ((DragonAirCombatSettingsProvider) dragon).getAiAirCombatSettings().landingSpeed();
        context.memories().set(DragonMemories.MOVEMENT_INTENT, DragonMovementIntent.strictAir(destination, speed));
        recoveries++;
        state = "recovered:missing-flight-command";
    }

    private String blockedReason(DragonBrainContext<T> context, RideableFlyingDragon dragon, LivingEntity target) {
        if (!dragon.isAerial() || dragon.onGround()) return "not-airborne";
        String blocked = DragonFlightEligibility.movementBlockReason(dragon);
        if (blocked != null) return blocked;
        if (dragon.isTakeoff()) return "takeoff-animation";
        if (dragon.getActiveAbility() != null) return "ability:" + dragon.getActiveAbility().getClass().getSimpleName();
        if (context.memories().has(DragonMemories.RESCUE_TARGET)
                || context.memories().has(DragonMemories.INTERCEPT_PROJECTILE)) return "reserved-action";
        if (target == null || !context.memories().get(DragonMemories.TARGET_VISIBLE).orElse(false)) return "no-visible-target";
        blocked = DragonFlightEligibility.pursuitBlockReason(dragon, target, true, true, false);
        if (blocked != null) return blocked;
        DragonTactic tactic = context.memories().get(DragonMemories.TACTICAL_COMMITMENT)
                .map(commitment -> commitment.tactic()).orElse(DragonTactic.NONE);
        if (tactic != DragonTactic.AERIAL_PURSUIT && tactic != DragonTactic.LANDING_APPROACH) return "other-tactic";
        if (context.memories().has(DragonMemories.MOVEMENT_INTENT)) return "pending-intent";
        var movement = dragon.getAIMovement();
        if (movement.isPathing()) return "path-active";
        if (movement.hasActiveLandingTransition() && dragon.getDeltaMovement().lengthSqr() > 0.0025D) return "landing-descent";
        if (movement.brainMovement().hasRecentHold(movement.getMovementCommandGeneration(), context.gameTime())) return "intentional-hold";
        return null;
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        watchdog.tick(context.gameTime(), false);
        state = "inactive";
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        return Map.of("flight_execution", state == null ? "missing-command" : state,
                "missing_ticks", Integer.toString(watchdog.missingTicks()), "recoveries", Integer.toString(recoveries));
    }
}
