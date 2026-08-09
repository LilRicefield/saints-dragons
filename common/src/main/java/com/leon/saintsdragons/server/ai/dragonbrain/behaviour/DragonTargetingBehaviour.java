package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonTargetLifecycle;
import com.leon.saintsdragons.server.ai.DragonAirCombatSettingsProvider;
import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonInvestigation;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonAwarenessMemory;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonPerceptionProfile;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonSensoryObservation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class DragonTargetingBehaviour<T extends RideableDragonBase> extends DragonBehaviour<T> {
    private static final String PROJECTILE_THREAT_SOURCE = "projectile_threat";
    private static final int PROJECTILE_THREAT_PRIORITY = 4;

    private final DragonPursuitSafety pursuitSafety = new DragonPursuitSafety();
    private String source = "none";
    private int sourcePriority = Integer.MAX_VALUE;

    protected DragonTargetingBehaviour() {
        super(false);
    }

    @Override
    protected final boolean canStart(DragonBrainContext<T> context) {
        return true;
    }

    @Override
    protected final boolean canContinue(DragonBrainContext<T> context) {
        return true;
    }

    @Override
    protected final void tick(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        pursuitSafety.beginTick(context.gameTime());
        LivingEntity wakeTarget = context.memories().get(DragonMemories.WAKE_TARGET).orElse(null);
        if (wakeTarget != null && !isUsableTarget(dragon, wakeTarget)) {
            context.memories().erase(DragonMemories.WAKE_TARGET);
            wakeTarget = null;
        }

        if (!canAcquireTargets(dragon)) {
            if (dragon.isSleepingExiting()) {
                return;
            }
            clearTarget(context);
            pursuitSafety.resetTracking();
            return;
        }

        if (wakeTarget != null
                && dragon.isWildAggressionEnabled()
                && dragon.getSensing().hasLineOfSight(wakeTarget)
                && pursuitSafety.canReacquire(dragon, wakeTarget, context.gameTime())) {
            setTarget(context, wakeTarget, "heard_intruder", 3);
            context.memories().erase(DragonMemories.WAKE_TARGET);
            return;
        }

        LivingEntity current = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (isUsableTarget(dragon, current)) {
            String abandonmentReason = pursuitSafety.abandonmentReason(context, current);
            if (abandonmentReason != null) {
                if (DragonPursuitSafety.isNavigationFailure(abandonmentReason)
                        && retainsVisibleTargetThroughNavigationFailure(
                        context,
                        current,
                        source,
                        abandonmentReason
                )) {
                    pursuitSafety.recoverVisiblePursuit(
                            context.gameTime(),
                            current,
                            dragon.distanceTo(current)
                    );
                } else {
                    abandonTarget(context, current, abandonmentReason);
                    return;
                }
            }
        } else {
            pursuitSafety.resetTracking();
        }

        LivingEntity assignedTarget = dragon.getTarget();
        if (current == null
                && assignedTarget != null
                && !pursuitSafety.canReacquire(dragon, assignedTarget, context.gameTime())) {
            DragonTargetLifecycle.clearCombatTarget(context.memories(), dragon, false);
        }
        if (current == null && pursuitSafety.shouldThrottleAcquisition(dragon, context.gameTime())) {
            return;
        }

        TargetChoice choice = findPriorityTarget(context);
        TargetChoice projectileThreat = findProjectileThreat(context);
        if (projectileThreat != null
                && (choice == null || projectileThreat.priority() < choice.priority())) {
            choice = projectileThreat;
        }
        if (choice != null
                && !pursuitSafety.canReacquire(dragon, choice.target(), context.gameTime())) {
            choice = null;
        }
        if (choice != null) {
            if (isUsableTarget(dragon, current) && canRetainTarget(dragon, current, source)) {
                boolean keepProjectileCommitment = PROJECTILE_THREAT_SOURCE.equals(source)
                        && PROJECTILE_THREAT_SOURCE.equals(choice.source())
                        && choice.target() != current;
                if (keepProjectileCommitment || choice.priority() > sourcePriority) {
                    syncTarget(context, current);
                    return;
                }
            }
            setTarget(context, choice.target(), choice.source(), choice.priority());
            return;
        }
        if (suppressesTargetRetention(context)) {
            clearTarget(context);
            return;
        }

        current = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (isUsableTarget(dragon, current) && canRetainTarget(dragon, current, source)) {
            syncTarget(context, current);
            return;
        }
        clearTarget(context);
    }

    protected abstract boolean canAcquireTargets(T dragon);

    @Nullable
    protected abstract TargetChoice findPriorityTarget(DragonBrainContext<T> context);

    protected boolean isUsableTarget(T dragon, @Nullable LivingEntity target) {
        return DragonTargetLifecycle.isValidTarget(dragon, target) && dragon.canTarget(target);
    }

    protected abstract boolean canRetainTarget(T dragon, LivingEntity target, String source);

    protected boolean suppressesTargetRetention(DragonBrainContext<T> context) {
        return false;
    }

    protected boolean retainsVisibleTargetThroughNavigationFailure(DragonBrainContext<T> context,
                                                                   LivingEntity target,
                                                                   String source,
                                                                   String reason) {
        return false;
    }

    protected void prepareTargetChange(T dragon,
                                       @Nullable LivingEntity oldTarget,
                                       LivingEntity newTarget,
                                       String oldSource,
                                       String newSource) {
    }

    protected void targetChanged(T dragon,
                                 @Nullable LivingEntity oldTarget,
                                 LivingEntity newTarget,
                                 String oldSource,
                                 String newSource) {
    }

    protected void targetCleared(T dragon, @Nullable LivingEntity oldTarget, String oldSource) {
    }

    protected Map<String, String> additionalDebugDetails() {
        return Map.of();
    }

    protected final TargetChoice targetChoice(LivingEntity target, String source) {
        return targetChoice(target, source, 0);
    }

    protected final TargetChoice targetChoice(LivingEntity target, String source, int priority) {
        return new TargetChoice(target, source, priority);
    }

    @Nullable
    private TargetChoice findProjectileThreat(DragonBrainContext<T> context) {
        DragonAwarenessMemory awareness = DragonAwarenessMemory.get(context.dragon());
        java.util.UUID sourceUuid = awareness.projectileThreatSource(context.gameTime());
        if (sourceUuid == null) {
            return null;
        }
        Entity sourceEntity = context.level().getEntity(sourceUuid);
        if (!(sourceEntity instanceof LivingEntity source)
                || !isUsableTarget(context.dragon(), source)) {
            awareness.consumeProjectileThreat(sourceUuid);
            return null;
        }
        return targetChoice(source, PROJECTILE_THREAT_SOURCE, PROJECTILE_THREAT_PRIORITY);
    }

    private void setTarget(DragonBrainContext<T> context,
                           LivingEntity target,
                           String newSource,
                           int newPriority) {
        T dragon = context.dragon();
        LivingEntity oldTarget = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        String oldSource = source;
        boolean changed = oldTarget != target || !oldSource.equals(newSource);
        prepareTargetChange(dragon, oldTarget, target, oldSource, newSource);
        source = newSource;
        sourcePriority = newPriority;
        context.memories().set(DragonMemories.ATTACK_TARGET, target);
        if (changed) {
            DragonSensoryObservation investigation = context.memories()
                    .get(DragonMemories.INVESTIGATION_TARGET)
                    .orElse(null);
            boolean investigationMatchesTarget = investigation != null
                    && target.getUUID().equals(investigation.sourceUuid());
            if (!investigationMatchesTarget) {
                context.memories().erase(DragonMemories.INVESTIGATION_TARGET);
            }
            context.memories().erase(DragonMemories.HEARD_TARGET);
            boolean visible = dragon.getSensing().hasLineOfSight(target);
            context.memories().set(DragonMemories.TARGET_VISIBLE, visible, 3);
            if (visible) {
                context.memories().set(
                        DragonMemories.LAST_SEEN_TARGET,
                        new DragonSensoryObservation(
                                target.getBoundingBox().getCenter(),
                                target.getUUID(),
                                DragonSensoryObservation.Kind.SIGHT,
                                1.0F,
                                context.gameTime()
                        ),
                        DragonPerceptionProfile.forDragon(dragon).targetMemoryTicks()
                );
            } else {
                context.memories().erase(DragonMemories.LAST_SEEN_TARGET);
            }
        }
        syncTarget(context, target);
        if (changed) {
            targetChanged(dragon, oldTarget, target, oldSource, newSource);
        }
    }

    private void syncTarget(DragonBrainContext<T> context, LivingEntity target) {
        T dragon = context.dragon();
        if (dragon.getTarget() != target) {
            dragon.setTarget(target);
        }
        if (dragon instanceof DragonAirCombatSettingsProvider provider) {
            context.memories().set(
                    DragonMemories.TARGET_AIRBORNE,
                    DragonTargetingHelper.isTargetAirborne(
                            target,
                            provider.getAiTargetAirborneHeight(target)
                    ) && !target.isInWaterOrBubble()
            );
        } else {
            context.memories().erase(DragonMemories.TARGET_AIRBORNE);
        }
    }

    private void clearTarget(DragonBrainContext<T> context) {
        clearTarget(context, true);
    }

    private void clearTarget(DragonBrainContext<T> context, boolean rememberEvidence) {
        T dragon = context.dragon();
        LivingEntity oldTarget = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        LivingEntity entityTarget = dragon.getTarget();
        String oldSource = source;
        LivingEntity clearedTarget = oldTarget != null ? oldTarget : entityTarget;
        if (rememberEvidence && isUsableTarget(dragon, clearedTarget)) {
            rememberTargetEvidence(context, clearedTarget);
        }
        source = "none";
        sourcePriority = Integer.MAX_VALUE;
        DragonTargetLifecycle.clearCombatTarget(context.memories(), dragon, false);
        if (oldTarget != null || !"none".equals(oldSource)) {
            targetCleared(dragon, oldTarget, oldSource);
        }
    }

    private void abandonTarget(DragonBrainContext<T> context,
                               LivingEntity target,
                               String reason) {
        pursuitSafety.recordAbandonment(context.gameTime(), target, reason);
        clearTarget(context, false);
        context.memories().erase(DragonMemories.INVESTIGATION_TARGET);
        context.memories().erase(DragonMemories.WALK_TARGET);
        context.memories().erase(DragonMemories.PATH);
        context.memories().erase(DragonMemories.CANT_REACH_WALK_TARGET_SINCE);
        context.memories().erase(DragonMemories.MOVEMENT_INTENT);
        context.memories().erase(DragonMemories.GROUND_ROUTE_ABANDONED);
        context.memories().erase(DragonMemories.TACTICAL_LANDING_POSITION);
        context.memories().erase(DragonMemories.TACTICAL_COMMITMENT);
        context.dragon().getAIMovement().stopAndClearAllMovement();
    }

    public final String getPursuitDebugSummary() {
        return pursuitSafety.debugSummary();
    }

    private void rememberTargetEvidence(DragonBrainContext<T> context, LivingEntity target) {
        DragonSensoryObservation lastSeen = context.memories()
                .get(DragonMemories.LAST_SEEN_TARGET)
                .filter(observation -> target.getUUID().equals(observation.sourceUuid()))
                .orElse(null);
        DragonSensoryObservation heard = context.memories()
                .get(DragonMemories.HEARD_TARGET)
                .filter(observation -> target.getUUID().equals(observation.sourceUuid()))
                .orElse(null);
        DragonSensoryObservation freshest = lastSeen;
        if (heard != null && (freshest == null || heard.observedAt() > freshest.observedAt())) {
            freshest = heard;
        }
        if (freshest != null) {
            DragonInvestigation.remember(context.dragon(), freshest);
        }
    }

    @Override
    public final Map<String, String> getDragonBrainDebugDetails() {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("source", source);
        details.put("priority", sourcePriority == Integer.MAX_VALUE ? "none" : Integer.toString(sourcePriority));
        details.put("pursuit", pursuitSafety.stateDebugSummary());
        details.put("abandoned", pursuitSafety.abandonedDebugSummary());
        details.putAll(additionalDebugDetails());
        return Map.copyOf(details);
    }

    protected record TargetChoice(LivingEntity target, String source, int priority) {
    }
}
