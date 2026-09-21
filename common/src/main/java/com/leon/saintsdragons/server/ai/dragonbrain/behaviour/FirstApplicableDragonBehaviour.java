package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviourInterruption;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.world.entity.ai.behavior.Behavior;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class FirstApplicableDragonBehaviour<T extends RideableDragonBase> extends DragonBehaviour<T> {
    private static final int INTERRUPTION_CHECK_INTERVAL_TICKS = 10;
    private final List<DragonBehaviour<T>> behaviours;
    private long nextInterruptionCheckAt;
    private String decision = "none";
    @Nullable
    private DragonBehaviour<T> running;

    @SafeVarargs
    public FirstApplicableDragonBehaviour(DragonBehaviour<T>... behaviours) {
        List<DragonBehaviour<T>> ordered = new ArrayList<>(behaviours.length + 1);
        ordered.add(new DragonMaintainPersonalSpaceBehaviour<>());
        ordered.addAll(Arrays.asList(behaviours));
        this.behaviours = List.copyOf(ordered);
    }

    public List<DragonBehaviour<T>> childBehaviours() {
        return behaviours;
    }

    @Nullable
    public DragonBehaviour<T> runningBehaviour() {
        return running;
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        if (hasCommittedAction(context.dragon())) {
            return false;
        }
        if (context.memories().has(DragonMemories.BREED_TARGET)) {
            return !controlReservedByState(context) && tryStartReservedBreeding(context);
        }
        if (controlReserved(context)) {
            return false;
        }
        long gameTime = context.gameTime();
        for (DragonBehaviour<T> behaviour : behaviours) {
            if (behaviour.tryStart(context.level(), context.dragon(), gameTime)) {
                running = behaviour;
                recordDecision(context, "started:" + behaviour.getClass().getSimpleName());
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        return !controlReserved(context)
                && running != null
                && running.getStatus() == Behavior.Status.RUNNING;
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        if (running == null) {
            return;
        }
        if (controlReserved(context)) {
            recordDecision(context, "interrupted:" + reservationReason(context));
            relinquishControl(context);
            return;
        }
        if (hasCommittedAction(context.dragon())) {
            if (!"paused:committed-action".equals(decision)) {
                recordDecision(context, "paused:committed-action");
            }
            return;
        }
        if ("paused:committed-action".equals(decision)) {
            recordDecision(context, "resumed:" + running.getClass().getSimpleName());
        }
        if (tryInterrupt(context)) {
            if (running == null) {
                doStop(context.level(), context.dragon(), context.gameTime());
            }
            return;
        }
        running.tickOrStop(context.level(), context.dragon(), context.gameTime());
        if (running.getStatus() == Behavior.Status.STOPPED) {
            recordDecision(context, "ended:" + running.getClass().getSimpleName());
            running = null;
            doStop(context.level(), context.dragon(), context.gameTime());
        }
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        if (running != null) {
            String reason = reservationReason(context);
            if (reason == null) {
                reason = activity() != null && !context.dragon().getBrain().getActiveActivities().contains(activity())
                        ? "activity-changed" : "child-ended";
            }
            recordDecision(context, "interrupted:" + reason + ":" + running.getClass().getSimpleName());
        }
        relinquishControl(context);
    }

    private boolean tryInterrupt(DragonBrainContext<T> context) {
        if (context.gameTime() < nextInterruptionCheckAt) {
            return false;
        }
        nextInterruptionCheckAt = context.gameTime() + INTERRUPTION_CHECK_INTERVAL_TICKS;
        DragonBehaviour<T> replacement = null;
        DragonBehaviourInterruption requested = DragonBehaviourInterruption.NONE;
        for (DragonBehaviour<T> behaviour : behaviours) {
            DragonBehaviourInterruption interruption = behaviour.interruptionType();
            if (behaviour != running && interruption.outranks(requested)
                    && running.canYieldTo(context, interruption)
                    && behaviour.cooldownRemaining(context.gameTime()) == 0L
                    && behaviour.requestsInterruption(context)) {
                replacement = behaviour;
                requested = interruption;
            }
        }
        if (replacement == null) {
            return false;
        }
        String previous = running.getClass().getSimpleName();
        relinquishControl(context);
        if (replacement.tryStart(context.level(), context.dragon(), context.gameTime())) {
            running = replacement;
            recordDecision(context, "handoff:" + requested.reason() + ":" + previous
                    + "->" + replacement.getClass().getSimpleName());
        } else {
            recordDecision(context, "handoff-unavailable:" + requested.reason() + ":from=" + previous);
        }
        return true;
    }

    private void recordDecision(DragonBrainContext<T> context, String reason) {
        decision = reason;
        context.dragon().combatManager.recordAiDecision("behaviour", reason);
    }

    private boolean controlReserved(DragonBrainContext<T> context) {
        return reservationReason(context) != null;
    }

    @Nullable
    private String reservationReason(DragonBrainContext<T> context) {
        String stateReason = stateReservationReason(context);
        if (stateReason != null) {
            return stateReason;
        }
        return context.memories().has(DragonMemories.BREED_TARGET)
                && !(running instanceof DragonBreedBehaviour<?>)
                && !hasCommittedAction(context.dragon()) ? "breeding-reserved" : null;
    }

    private boolean controlReservedByState(DragonBrainContext<T> context) {
        return stateReservationReason(context) != null;
    }

    @Nullable
    private String stateReservationReason(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        if (!dragon.isAlive() || dragon.isDying()) return "dying";
        if (dragon.isVehicle() || dragon.isPassenger()) return "rider-control";
        dragon.clearStaleSittingPoseForMovement();
        boolean ownerFollowPriority = DragonFollowOwnerBehaviour.hasOwnerFollowPriority(dragon);
        boolean unseenInvestigation = context.memories().has(DragonMemories.INVESTIGATION_TARGET)
                && !ownerFollowPriority
                && (!dragon.isInLove() && !context.memories().has(DragonMemories.BREED_TARGET)
                || context.memories().has(DragonMemories.ATTACK_TARGET))
                && !context.memories().get(DragonMemories.TARGET_VISIBLE).orElse(false);
        if (dragon.isSleeping() || dragon.isSleepTransitioning()) return "sleep";
        if (dragon.isOrderedToSit() || dragon.isInSittingPose() || dragon.isInSitTransition()) return "sit-command";
        if (dragon.isHuntFoodPursuitActive()) return "food-pursuit";
        if (hasCommittedAction(dragon)) return null;
        if (context.memories().has(DragonMemories.SCENT_CANDIDATE)) return "scent-assessment";
        return unseenInvestigation ? "investigation" : null;
    }

    private boolean tryStartReservedBreeding(DragonBrainContext<T> context) {
        long gameTime = context.gameTime();
        for (DragonBehaviour<T> behaviour : behaviours) {
            if (behaviour instanceof DragonBreedBehaviour<?>
                    && behaviour.tryStart(context.level(), context.dragon(), gameTime)) {
                running = behaviour;
                recordDecision(context, "started:reserved-breeding");
                return true;
            }
        }
        DragonBreedBehaviour.releaseReservation(context.dragon());
        return false;
    }

    private void relinquishControl(DragonBrainContext<T> context) {
        if (running == null) {
            return;
        }
        DragonMovementIntent reservedIntent = context.dragon().isHuntFoodPursuitActive()
                ? context.memories().get(DragonMemories.MOVEMENT_INTENT).orElse(null)
                : null;
        running.doStop(context.level(), context.dragon(), context.gameTime());
        running = null;
        if (reservedIntent != null) {
            context.memories().set(DragonMemories.MOVEMENT_INTENT, reservedIntent);
        }
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        if (running == null) {
            return Map.of("active_child", "none", "decision", decision);
        }
        return Map.of(
                "active_child", running.getClass().getSimpleName(),
                "decision", decision,
                "child_details", running.getDragonBrainDebugDetails().toString()
        );
    }
}
