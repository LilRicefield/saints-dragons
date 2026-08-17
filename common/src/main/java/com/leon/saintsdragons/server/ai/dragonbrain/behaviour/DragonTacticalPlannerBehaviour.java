package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonSensoryObservation;
import com.leon.saintsdragons.server.ai.dragonbrain.tactical.DragonTactic;
import com.leon.saintsdragons.server.ai.dragonbrain.tactical.DragonTacticalCommitment;
import com.leon.saintsdragons.server.ai.dragonbrain.tactical.DragonTacticalProfile;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonLocomotionMode;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.interfaces.DragonMovementCapability;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class DragonTacticalPlannerBehaviour<T extends DragonEntity> extends DragonBehaviour<T> {
    private long nextEvaluationTick;
    private long lastGameTime;
    private DragonTacticalCommitment lastCommitment;

    public DragonTacticalPlannerBehaviour() {
        super(false);
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        return true;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        return true;
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        lastGameTime = context.gameTime();
        if (context.gameTime() < nextEvaluationTick) {
            return;
        }

        DragonTacticalProfile profile = DragonTacticalProfile.forDragon(context.dragon());
        nextEvaluationTick = context.gameTime() + profile.evaluationIntervalTicks();
        Evaluation evaluation = evaluate(context, profile);
        DragonTacticalCommitment current = context.memories()
                .get(DragonMemories.TACTICAL_COMMITMENT)
                .orElse(null);
        DragonTacticalCommitment decided = decide(
                current,
                evaluation,
                profile,
                context.gameTime()
        );
        context.memories().set(DragonMemories.TACTICAL_COMMITMENT, decided);
        lastCommitment = decided;
    }

    private Evaluation evaluate(DragonBrainContext<T> context, DragonTacticalProfile profile) {
        T dragon = context.dragon();
        Evaluation evaluation = new Evaluation();
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (target == null) {
            target = dragon.getTarget();
        }
        if (target != null && !dragon.isTargetValid(target)) {
            target = null;
        }
        DragonSensoryObservation investigation = context.memories()
                .get(DragonMemories.INVESTIGATION_TARGET)
                .orElse(null);

        String inactiveReason = inactiveReason(dragon);
        if (inactiveReason != null) {
            evaluation.add(DragonTactic.NONE, 100, null, null, inactiveReason);
            return evaluation;
        }

        evaluation.add(DragonTactic.NONE, 0, null, null, "no-pressure");
        boolean targetVisible = context.memories().get(DragonMemories.TARGET_VISIBLE).orElse(false);
        DragonSensoryObservation evidence = investigation;
        if (evidence == null && target != null && !targetVisible) {
            evidence = latestMatching(
                    target,
                    context.memories().get(DragonMemories.LAST_SEEN_TARGET).orElse(null),
                    context.memories().get(DragonMemories.HEARD_TARGET).orElse(null)
            );
        }
        if (evidence != null && !targetVisible) {
            int score = 70 + Math.round(evidence.confidence() * 20.0F);
            if (target != null && target.getUUID().equals(evidence.sourceUuid())) {
                score += 10;
            }
            evaluation.add(
                    DragonTactic.INVESTIGATE,
                    score,
                    evidence.sourceUuid(),
                    evidence.position(),
                    "fresh-evidence"
            );
        }

        if (target != null) {
            Vec3 focus = targetFocus(context, target);
            if (focus != null) {
                addCombatPlans(context, evaluation, target, focus, targetVisible, profile);
            }
        }

        boolean sleepIntent = context.memories().get(DragonMemories.SLEEP_INTENT).orElse(false);
        if (sleepIntent && target == null && investigation == null) {
            evaluation.add(DragonTactic.NONE, 60, null, null, "sleep-intent");
        }
        return evaluation;
    }

    private void addCombatPlans(DragonBrainContext<T> context,
                                Evaluation evaluation,
                                LivingEntity target,
                                Vec3 focus,
                                boolean targetVisible,
                                DragonTacticalProfile profile) {
        T dragon = context.dragon();
        UUID targetUuid = target.getUUID();
        double distance = dragon.position().distanceTo(focus);
        boolean targetAirborne = context.memories().get(DragonMemories.TARGET_AIRBORNE).orElse(false);
        boolean targetInWater = targetVisible && target.isInWaterOrBubble();
        boolean groundRouteAbandoned = context.memories()
                .get(DragonMemories.GROUND_ROUTE_ABANDONED)
                .orElse(false);
        DragonLocomotionMode locomotion = dragon.getLocomotionMode();

        float healthRatio = dragon.getMaxHealth() <= 0.0F
                ? 1.0F
                : dragon.getHealth() / dragon.getMaxHealth();
        if (healthRatio <= profile.retreatHealthRatio()) {
            int deficit = Math.round(
                    (profile.retreatHealthRatio() - healthRatio)
                            / profile.retreatHealthRatio() * 20.0F
            );
            evaluation.add(
                    DragonTactic.RETREAT,
                    95 + Math.max(0, deficit),
                    targetUuid,
                    focus,
                    "critical-health"
            );
        }

        if (!targetVisible) {
            return;
        }

        if (!targetAirborne && !targetInWater && !groundRouteAbandoned
                && dragon.movementCapabilities().contains(DragonMovementCapability.WALK)) {
            int score = 55;
            if (locomotion == DragonLocomotionMode.GROUND) score += 15;
            if (distance <= 16.0D) score += 10;
            evaluation.add(
                    DragonTactic.GROUND_PURSUIT,
                    score,
                    targetUuid,
                    focus,
                    "ground-target"
            );
        }

        if ((targetInWater || locomotion == DragonLocomotionMode.WATER)
                && dragon.movementCapabilities().contains(DragonMovementCapability.SWIM)) {
            int score = 55;
            if (targetInWater) score += 20;
            if (locomotion == DragonLocomotionMode.WATER) score += 15;
            evaluation.add(
                    DragonTactic.WATER_PURSUIT,
                    score,
                    targetUuid,
                    focus,
                    "water-contact"
            );
        }

        boolean canFly = dragon.movementCapabilities().contains(DragonMovementCapability.FLY);
        if (canFly) {
            int score = 50;
            if (targetAirborne) score += 30;
            if (groundRouteAbandoned) score += 35;
            if (dragon.isAerial()) score += 10;
            if (distance >= 16.0D) score += 10;
            evaluation.add(
                    DragonTactic.AERIAL_PURSUIT,
                    score,
                    targetUuid,
                    focus,
                    targetAirborne ? "airborne-target" : "flight-option"
            );

            if (dragon.isAerial() && !targetAirborne && !targetInWater) {
                int landingScore = 60;
                if (distance <= 24.0D) landingScore += 10;
                if (context.memories().has(DragonMemories.TACTICAL_LANDING_POSITION)) {
                    landingScore += 20;
                }
                evaluation.add(
                        DragonTactic.LANDING_APPROACH,
                        landingScore,
                        targetUuid,
                        focus,
                        "grounded-target"
                );
            }
        }
    }

    private DragonTacticalCommitment decide(@Nullable DragonTacticalCommitment current,
                                             Evaluation evaluation,
                                             DragonTacticalProfile profile,
                                             long gameTime) {
        Plan candidate = evaluation.best();
        if (current == null) {
            return start(candidate, candidate, evaluation, profile, gameTime, "initial:" + candidate.reason());
        }

        Plan currentPlan = evaluation.plan(current.tactic());
        if (candidate.tactic() == DragonTactic.NONE && candidate.score() >= 100) {
            return start(candidate, candidate, evaluation, profile, gameTime, "interrupt:" + candidate.reason());
        }
        if (candidate.tactic() == DragonTactic.RETREAT && current.tactic() != DragonTactic.RETREAT) {
            return start(candidate, candidate, evaluation, profile, gameTime, "interrupt:critical-health");
        }
        if ((current.tactic() == DragonTactic.NONE || current.tactic() == DragonTactic.GUARD)
                && candidate.tactic() != current.tactic()) {
            return start(candidate, candidate, evaluation, profile, gameTime, "interrupt:new-pressure");
        }
        if (currentPlan == null) {
            return start(candidate, candidate, evaluation, profile, gameTime, "invalid:" + current.tactic());
        }

        boolean sameSubject = current.tactic() == candidate.tactic()
                && Objects.equals(current.targetUuid(), candidate.targetUuid());
        if (current.tactic() == candidate.tactic() && !sameSubject) {
            return start(candidate, candidate, evaluation, profile, gameTime,
                    "target-changed:" + candidate.reason());
        }
        if (gameTime >= current.expiresAt()) {
            return start(candidate, candidate, evaluation, profile, gameTime,
                    "expired:" + candidate.reason());
        }
        if (!sameSubject && gameTime < current.minimumEndsAt()) {
            return retain(current, currentPlan, candidate, evaluation,
                    "locked:" + candidate.reason());
        }
        if (!sameSubject
                && candidate.score() >= currentPlan.score() + profile.switchMargin()) {
            return start(candidate, candidate, evaluation, profile, gameTime,
                    "switch-margin:" + candidate.reason());
        }
        return retain(
                current,
                currentPlan,
                candidate,
                evaluation,
                (sameSubject ? "maintain:" : "hysteresis:") + candidate.reason()
        );
    }

    private DragonTacticalCommitment start(Plan selected,
                                            Plan candidate,
                                            Evaluation evaluation,
                                            DragonTacticalProfile profile,
                                            long gameTime,
                                            String reason) {
        return new DragonTacticalCommitment(
                selected.tactic(),
                selected.score(),
                candidate.tactic(),
                candidate.score(),
                gameTime,
                gameTime + profile.minimumTicks(selected.tactic()),
                gameTime + profile.maximumTicks(selected.tactic()),
                selected.targetUuid(),
                selected.focus(),
                reason,
                evaluation.scores()
        );
    }

    private DragonTacticalCommitment retain(DragonTacticalCommitment current,
                                             Plan selected,
                                             Plan candidate,
                                             Evaluation evaluation,
                                             String reason) {
        return new DragonTacticalCommitment(
                current.tactic(),
                selected.score(),
                candidate.tactic(),
                candidate.score(),
                current.startedAt(),
                current.minimumEndsAt(),
                current.expiresAt(),
                selected.targetUuid(),
                selected.focus(),
                reason,
                evaluation.scores()
        );
    }

    @Nullable
    private String inactiveReason(T dragon) {
        if (dragon.isDying() || !dragon.isAlive()) return "unavailable";
        if (dragon.isVehicle()) return "ridden";
        if (dragon.isPassenger()) return "passenger";
        if (dragon.isOrderedToSit()) return "ordered-sit";
        if (dragon.isSleeping() || dragon.isSleepTransitioning()) return "sleeping";
        if (dragon instanceof RideableDragonBase rideable
                && DragonFollowOwnerBehaviour.hasOwnerFollowPriority(rideable)) {
            return "following-owner";
        }
        return null;
    }

    private @Nullable Vec3 targetFocus(DragonBrainContext<T> context, LivingEntity target) {
        if (context.memories().get(DragonMemories.TARGET_VISIBLE).orElse(false)) {
            return target.getBoundingBox().getCenter();
        }
        DragonSensoryObservation investigation = context.memories()
                .get(DragonMemories.INVESTIGATION_TARGET)
                .orElse(null);
        if (investigation != null && target.getUUID().equals(investigation.sourceUuid())) {
            return investigation.position();
        }
        DragonSensoryObservation latest = latestMatching(
                target,
                context.memories().get(DragonMemories.LAST_SEEN_TARGET).orElse(null),
                context.memories().get(DragonMemories.HEARD_TARGET).orElse(null)
        );
        return latest == null ? null : latest.position();
    }

    @Nullable
    private DragonSensoryObservation latestMatching(LivingEntity target,
                                                     DragonSensoryObservation... observations) {
        DragonSensoryObservation latest = null;
        for (DragonSensoryObservation observation : observations) {
            if (observation == null || !target.getUUID().equals(observation.sourceUuid())) {
                continue;
            }
            if (latest == null || observation.observedAt() > latest.observedAt()) {
                latest = observation;
            }
        }
        return latest;
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        if (lastCommitment == null) {
            return Map.of("mode", "shadow", "commitment", "none");
        }
        Map<String, String> details = new LinkedHashMap<>();
        details.put("mode", "shadow");
        details.put("commitment", lastCommitment.tactic().name());
        details.put("score", Integer.toString(lastCommitment.score()));
        details.put("candidate", lastCommitment.candidate().name());
        details.put("candidate_score", Integer.toString(lastCommitment.candidateScore()));
        details.put("decision", lastCommitment.reason());
        details.put("age", Math.max(0L, lastGameTime - lastCommitment.startedAt()) + "t");
        details.put("minimum_remaining",
                Math.max(0L, lastCommitment.minimumEndsAt() - lastGameTime) + "t");
        details.put("expiry_remaining",
                Math.max(0L, lastCommitment.expiresAt() - lastGameTime) + "t");
        details.put("scores", lastCommitment.scoresSummary());
        return Map.copyOf(details);
    }

    private record Plan(DragonTactic tactic,
                        int score,
                        @Nullable UUID targetUuid,
                        @Nullable Vec3 focus,
                        String reason) {
    }

    private static final class Evaluation {
        private final EnumMap<DragonTactic, Plan> plans = new EnumMap<>(DragonTactic.class);

        void add(DragonTactic tactic,
                 int score,
                 @Nullable UUID targetUuid,
                 @Nullable Vec3 focus,
                 String reason) {
            Plan existing = plans.get(tactic);
            if (existing == null || score > existing.score()) {
                plans.put(tactic, new Plan(tactic, score, targetUuid, focus, reason));
            }
        }

        @Nullable
        Plan plan(DragonTactic tactic) {
            return plans.get(tactic);
        }

        Plan best() {
            Plan best = plans.get(DragonTactic.NONE);
            for (Plan candidate : plans.values()) {
                if (best == null
                        || candidate.score() > best.score()
                        || (candidate.score() == best.score()
                        && candidate.tactic().tiePriority() > best.tactic().tiePriority())) {
                    best = candidate;
                }
            }
            if (best == null) {
                throw new IllegalStateException("Tactical evaluation produced no plans");
            }
            return best;
        }

        Map<DragonTactic, Integer> scores() {
            EnumMap<DragonTactic, Integer> scores = new EnumMap<>(DragonTactic.class);
            plans.forEach((tactic, plan) -> scores.put(tactic, plan.score()));
            return scores;
        }
    }
}
