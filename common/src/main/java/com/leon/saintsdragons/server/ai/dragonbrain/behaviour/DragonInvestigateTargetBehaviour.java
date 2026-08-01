package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonPerceptionProfile;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonSensoryObservation;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DragonInvestigateTargetBehaviour<T extends DragonEntity> extends DragonBehaviour<T> {
    private int repathCooldown;
    private int searchTicks;
    private boolean issuedMovement;
    private Vec3 destination;
    private String investigationKind = "none";
    private long observationTick = Long.MIN_VALUE;

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        return canInvestigate(context);
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        return canInvestigate(context);
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        repathCooldown = 0;
        searchTicks = 0;
        issuedMovement = false;
        destination = null;
        investigationKind = "none";
        observationTick = Long.MIN_VALUE;
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        if (!(context.dragon() instanceof RideableDragonBase dragon)) {
            return;
        }
        DragonSensoryObservation observation = context.memories()
                .get(DragonMemories.INVESTIGATION_TARGET)
                .orElse(null);
        if (observation == null) {
            return;
        }
        investigationKind = observation.kind().name().toLowerCase(java.util.Locale.ROOT);

        DragonPerceptionProfile profile = DragonPerceptionProfile.forDragon(dragon);
        Vec3 observedPosition = observation.position();
        boolean refreshed = observation.observedAt() != observationTick;
        observationTick = observation.observedAt();
        if (destination == null || destination.distanceToSqr(observedPosition) > 1.0D) {
            destination = observedPosition;
            repathCooldown = 0;
            searchTicks = 0;
        } else if (refreshed) {
            searchTicks = 0;
        }

        dragon.getLookControl().setLookAt(
                destination.x,
                destination.y,
                destination.z,
                10.0F,
                dragon.getMaxHeadXRot()
        );

        if (issuedMovement && dragon.getAIMovement().hasFailed()) {
            abandonInvestigation(context, dragon);
            return;
        }

        double arrivalDistance = profile.arrivalDistance();
        if (dragon.position().distanceToSqr(destination) > arrivalDistance * arrivalDistance) {
            searchTicks = 0;
            if (repathCooldown-- <= 0) {
                issuedMovement = dragon.getAIMovement().setWaypoint(
                        destination,
                        profile.investigationSpeed()
                ) || issuedMovement;
                repathCooldown = 10;
            }
            return;
        }

        if (issuedMovement) {
            dragon.getAIMovement().stop();
            issuedMovement = false;
        }
        searchTicks++;
        double angle = Math.toRadians((context.gameTime() * 9L) % 360L);
        dragon.getLookControl().setLookAt(
                destination.x + Math.cos(angle) * 4.0D,
                destination.y + 1.0D,
                destination.z + Math.sin(angle) * 4.0D,
                12.0F,
                dragon.getMaxHeadXRot()
        );
        if (searchTicks >= profile.searchTicks()) {
            clearInvestigationMemories(context);
        }
    }

    private void abandonInvestigation(DragonBrainContext<T> context, RideableDragonBase dragon) {
        dragon.getAIMovement().stop();
        issuedMovement = false;
        clearInvestigationMemories(context);
    }

    private void clearInvestigationMemories(DragonBrainContext<T> context) {
        context.memories().erase(DragonMemories.INVESTIGATION_TARGET);
        context.memories().erase(DragonMemories.LAST_SEEN_TARGET);
        context.memories().erase(DragonMemories.HEARD_TARGET);
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        if (issuedMovement && context.dragon() instanceof RideableDragonBase dragon) {
            dragon.getAIMovement().stop();
        }
        issuedMovement = false;
        destination = null;
        investigationKind = "none";
        observationTick = Long.MIN_VALUE;
        searchTicks = 0;
    }

    private boolean canInvestigate(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        boolean targetVisible = target != null
                && context.memories().get(DragonMemories.TARGET_VISIBLE).orElse(false);
        return dragon instanceof RideableDragonBase
                && (target == null || target.isAlive())
                && !targetVisible
                && context.memories().has(DragonMemories.INVESTIGATION_TARGET)
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && !dragon.isOrderedToSit()
                && !dragon.isSleepLocked()
                && !dragon.isDying();
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("destination", destination == null ? "none" : destination.toString());
        details.put("kind", investigationKind);
        details.put("search_ticks", Integer.toString(searchTicks));
        details.put("movement_issued", Boolean.toString(issuedMovement));
        return Map.copyOf(details);
    }
}
