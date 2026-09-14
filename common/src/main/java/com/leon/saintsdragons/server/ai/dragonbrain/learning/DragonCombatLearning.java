package com.leon.saintsdragons.server.ai.dragonbrain.learning;

import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.UUID;

public final class DragonCombatLearning {
    public enum Attack { BEAM, BREATH, PROJECTILE, MELEE }
    public enum Response { NONE, CLOSE, RETREAT, LEFT, RIGHT }
    public enum Outcome { COMPLETED, RESPONSE_ONLY, BLOCKED, CANCELLED }

    public record Profile(Attack primaryAttack, int sampleTicks, int memoryTicks, int maximumTargets,
                          double maximumObservedSpeed, double maximumPredictionTicks, double maximumLead) {
        public Profile {
            if (primaryAttack == null || sampleTicks < 1 || memoryTicks < sampleTicks || maximumTargets < 1
                    || !Double.isFinite(maximumObservedSpeed) || maximumObservedSpeed <= 0
                    || !Double.isFinite(maximumPredictionTicks) || maximumPredictionTicks < 0
                    || !Double.isFinite(maximumLead) || maximumLead < 0) {
                throw new IllegalArgumentException("Invalid combat learning profile");
            }
        }

        public static Profile standard() { return standard(Attack.BEAM); }

        public static Profile standard(Attack attack) { return new Profile(attack, 2, 1200, 4, 6.0D, 8.0D, 12.0D); }
    }

    public record Expectation(Response response, double confidence, int observations,
                              double successRate, int outcomes, double predictionAccuracy, int predictions) {
        private static final Expectation UNKNOWN = new Expectation(Response.NONE, 0, 0, 0.5D, 0, 0.5D, 0);

        public double spacingBonus() {
            return response == Response.CLOSE ? 8.0D * confidence : 0.0D;
        }

        public double attackWeight() {
            return outcomes < 3 ? 1.0D : Mth.clamp(0.8D + successRate * 0.4D, 0.8D, 1.2D);
        }
    }

    private final DragonEntity dragon;
    private final Profile profile;
    private final LinkedHashMap<UUID, TargetMemory> targets = new LinkedHashMap<>(4, 0.75F, true);
    private UUID currentTarget;
    private UUID movementAnchor;
    private boolean visible;
    private long lastTick = Long.MIN_VALUE;
    private long sampleTick = Long.MIN_VALUE;
    private Vec3 position;
    private Vec3 velocity = Vec3.ZERO;
    private int samples;
    private double motionConfidence;
    private long nextToken;
    private Trial trial;
    private final LinkedHashMap<Long, PendingResult> pendingResults = new LinkedHashMap<>();
    private String lastResult = "none";

    public DragonCombatLearning(DragonEntity dragon, Profile profile) {
        this.dragon = dragon;
        this.profile = profile;
    }

    private long now() { return dragon.level().getGameTime(); }

    public Attack primaryAttack() { return profile.primaryAttack(); }

    private boolean available() {
        return !dragon.level().isClientSide && dragon.isAlive() && !dragon.isDying()
                && !dragon.isVehicle() && !dragon.isPassenger() && !dragon.isOrderedToSit()
                && !dragon.isSleeping() && !dragon.isSleepTransitioning()
                && (!(dragon instanceof DragonCombatLearner learner) || learner.canLearnCombat());
    }

    public void observe(@Nullable LivingEntity target, boolean canSee) {
        long now = now();
        if (now == lastTick) return;
        lastTick = now;
        targets.values().removeIf(memory -> now - memory.lastSeen > profile.memoryTicks());
        if (!available()) {
            clear();
            return;
        }
        for (var iterator = pendingResults.values().iterator(); iterator.hasNext();) {
            PendingResult pending = iterator.next();
            if (!canSee || target == null || target != dragon.getTarget()
                    || !pending.trial.target.equals(target.getUUID())) pending.trial.obscured = true;
            if (now >= pending.deadline) {
                iterator.remove();
                finishTrial(pending.trial, pending.outcome);
            }
        }
        if (target == null || target != dragon.getTarget() || !dragon.isTargetValid(target)) {
            resetTracking();
            return;
        }
        UUID id = target.getUUID();
        if (!id.equals(currentTarget)) {
            resetTracking();
            currentTarget = id;
        }
        visible = canSee;
        if (!visible) {
            if (trial != null) trial.obscured = true;
            resetMotion();
            return;
        }
        TargetMemory memory = targets.computeIfAbsent(id, ignored -> new TargetMemory());
        memory.lastSeen = now;
        while (targets.size() > profile.maximumTargets()) targets.remove(targets.keySet().iterator().next());
        if (trial != null && !trial.released && target.hurtTime > 0) trial.responseConfounded = true;
        if (sampleTick != Long.MIN_VALUE && now - sampleTick < profile.sampleTicks()) return;

        var anchor = DragonTargetingHelper.movementAnchor(target);
        // Track the combatant's hitbox; a non-living vehicle is only used to detect mount changes.
        Vec3 observed = target.getBoundingBox().getCenter();
        if (!finite(observed)) {
            resetTracking();
            return;
        }
        boolean anchorChanged = !anchor.getUUID().equals(movementAnchor);
        if (anchorChanged) {
            if (trial != null) finishAttack(trial.token, Outcome.CANCELLED);
            pendingResults.values().forEach(pending -> pending.trial.obscured = true);
            resetMotion();
            movementAnchor = anchor.getUUID();
        }
        long elapsed = sampleTick == Long.MIN_VALUE ? 0 : now - sampleTick;
        if (position == null || elapsed > profile.sampleTicks() * 2L) {
            if (trial != null) trial.obscured = true;
            resetMotion();
        } else {
            Vec3 measured = observed.subtract(position).scale(1.0D / Math.max(1, elapsed));
            if (measured.length() > profile.maximumObservedSpeed()) {
                if (trial != null) finishAttack(trial.token, Outcome.CANCELLED);
                pendingResults.values().forEach(pending -> pending.trial.obscured = true);
                resetMotion();
            } else {
                double error = measured.subtract(velocity).length();
                double agreement = 1.0D - Mth.clamp(error / (0.15D + measured.length()), 0, 1);
                motionConfidence = samples < 2 ? 0 : Mth.lerp(0.4D, motionConfidence, agreement);
                velocity = samples < 2 ? measured : velocity.lerp(measured, 0.65D);
            }
        }
        position = observed;
        sampleTick = now;
        samples = Math.min(samples + 1, 100);
        if (trial != null && !trial.released && !trial.obscured) {
            trial.lastPosition = observed;
            trial.lastObservation = now;
            if (now - trial.startedAt >= trial.windupTicks) captureResponse(trial);
        }
    }

    public boolean hasVisibleObservation(LivingEntity target) {
        return available() && visible && target != null && target == dragon.getTarget()
                && target.isAlive() && target.level() == dragon.level() && lastTick == now()
                && target.getUUID().equals(currentTarget) && position != null
                && sampleTick != Long.MIN_VALUE && now() - sampleTick <= profile.sampleTicks();
    }

    public @Nullable Vec3 predictCenter(LivingEntity target, double ticksAhead, double maximumLead) {
        if (!hasVisibleObservation(target) || !Double.isFinite(ticksAhead) || !Double.isFinite(maximumLead)) return null;
        // Sampling and confidence limit reaction speed. Extrapolate only observations already seen.
        double confidence = motionConfidence * Math.min(1.0D, samples / 4.0D);
        double horizon = Mth.clamp(ticksAhead, 0, profile.maximumPredictionTicks());
        Vec3 lead = velocity.scale((now() - sampleTick + horizon) * confidence);
        double limit = Math.min(profile.maximumLead(), Math.max(0, maximumLead));
        if (lead.lengthSqr() > limit * limit) lead = lead.normalize().scale(limit);
        return position.add(lead);
    }

    public Expectation expectation(LivingEntity target, Attack attack, boolean aerial) {
        if (!hasVisibleObservation(target)) return Expectation.UNKNOWN;
        TargetMemory memory = targets.get(currentTarget);
        Habits habits = memory == null ? null : memory.get(attack, aerial, false);
        return habits == null ? Expectation.UNKNOWN : habits.expectation(now());
    }

    public long beginAttack(Attack attack, LivingEntity target, int windupTicks) {
        trial = null;
        if (!hasVisibleObservation(target) || samples < 3) return 0;
        Vec3 forward = position.subtract(dragon.position()).multiply(1, 0, 1).normalize();
        if (forward.lengthSqr() < 1.0E-6D) return 0;
        trial = new Trial(++nextToken, currentTarget, attack, dragon.isAerial(), now(),
                Math.max(1, windupTicks), position, velocity, forward,
                expectation(target, attack, dragon.isAerial()));
        trial.responseConfounded = target.hurtTime > 0;
        return trial.token;
    }

    public void releaseAttack(long token) {
        if (!matches(token)) return;
        captureResponse(trial);
        trial.released = true;
    }

    public void recordHit(long token, LivingEntity target) {
        Trial attack = findTrial(token);
        if (attack != null && target != null && attack.target.equals(target.getUUID()) && available()) attack.hit = true;
    }

    public void recordContact(long token) {
        Trial attack = findTrial(token);
        if (attack != null) attack.contact = true;
    }

    public void recordContact(long token, LivingEntity target) {
        Trial attack = findTrial(token);
        if (attack != null && target != null && attack.target.equals(target.getUUID())) attack.contact = true;
    }

    public void deferAttackResult(long token, Outcome outcome, int waitTicks) {
        if (!matches(token)) return;
        captureResponse(trial);
        pendingResults.put(token, new PendingResult(trial, outcome, now() + Mth.clamp(waitTicks, 1, 240)));
        trial = null;
        // A released stream or projectile can outlive its ability, including another attack starting.
        while (pendingResults.size() > 4) {
            finishAttack(pendingResults.keySet().iterator().next(), Outcome.CANCELLED);
        }
    }

    private @Nullable Trial findTrial(long token) {
        if (token == 0) return null;
        if (matches(token)) return trial;
        PendingResult pending = pendingResults.get(token);
        return pending == null ? null : pending.trial;
    }

    public void finishAttack(long token, Outcome outcome) {
        Trial ended = findTrial(token);
        if (ended == null) return;
        if (matches(token)) trial = null;
        else pendingResults.remove(token);
        finishTrial(ended, outcome);
    }

    private void finishTrial(Trial ended, Outcome outcome) {
        if (!available()) return;
        captureResponse(ended);
        if (outcome == Outcome.CANCELLED && !ended.hit) {
            lastResult = ended.attack.name().toLowerCase(Locale.ROOT) + ":cancelled";
            return;
        }
        TargetMemory memory = targets.get(ended.target);
        if (memory == null) return;
        Habits habits = memory.get(ended.attack, ended.aerial, true);
        habits.decay(now());
        boolean learnedResponse = outcome != Outcome.CANCELLED && ended.response != null
                && !ended.obscured && !ended.responseConfounded;
        if (learnedResponse) {
            habits.responses[ended.response.ordinal()]++;
            habits.observations++;
            if (ended.expected.confidence() > 0) {
                habits.predictions++;
                habits.predictionWeight++;
                if (ended.response == ended.expected.response()) habits.correctPredictions++;
            }
        }
        boolean scored = ended.hit || (outcome == Outcome.COMPLETED && ended.released
                && !ended.obscured && !ended.contact);
        if (scored) {
            habits.outcomes++;
            if (ended.hit) habits.hits++;
            else habits.misses++;
        }
        lastResult = ended.attack.name().toLowerCase(Locale.ROOT) + ":"
                + (ended.hit ? "hit" : scored ? "miss" : "unscored")
                + ",end=" + outcome.name().toLowerCase(Locale.ROOT)
                + ",response=" + (learnedResponse ? ended.response.name().toLowerCase(Locale.ROOT) : "unknown");
    }

    private boolean matches(long token) { return token != 0 && trial != null && trial.token == token; }

    private void captureResponse(Trial attack) {
        // Recent damage can force movement; do not learn that knockback as a deliberate response.
        if (attack.response != null || attack.obscured || attack.responseConfounded || attack.lastPosition == null
                || attack.lastObservation - attack.startedAt < Math.min(6, attack.windupTicks)) return;
        double elapsed = attack.lastObservation - attack.startedAt;
        Vec3 movement = attack.lastPosition.subtract(attack.startPosition);
        Vec3 change = movement.subtract(attack.startVelocity.scale(elapsed));
        double forward = movement.dot(attack.forward);
        double forwardChange = change.dot(attack.forward);
        double side = movement.dot(attack.side);
        double sideChange = change.dot(attack.side);
        attack.response = Response.NONE;
        // Movement already underway before the tell is not automatically labelled a dodge.
        if (forward < -2.0D && forwardChange < -1.0D) attack.response = Response.CLOSE;
        else if (forward > 2.0D && forwardChange > 1.0D) attack.response = Response.RETREAT;
        else if (Math.abs(side) > 1.25D && Math.abs(sideChange) > 1.25D && side * sideChange > 0)
            attack.response = side > 0 ? Response.LEFT : Response.RIGHT;
    }

    public String debugSummary() {
        if (!available()) return "inactive";
        LivingEntity target = dragon.getTarget();
        Expectation expectation = expectation(target, primaryAttack(), dragon.isAerial());
        return String.format(Locale.ROOT,
                "sight=%s,samples=%d,motion=%.2f,targets=%d,attack=%s,expected=%s:%.2f,trials=%d,success=%.2f,outcomes=%d,prediction=%.2f/%d,pending=%d,last=%s",
                hasVisibleObservation(target), samples, motionConfidence, targets.size(), primaryAttack(),
                expectation.response(), expectation.confidence(),
                expectation.observations(), expectation.successRate(), expectation.outcomes(),
                expectation.predictionAccuracy(), expectation.predictions(), pendingResults.size(), lastResult);
    }

    private void resetMotion() {
        position = null;
        velocity = Vec3.ZERO;
        sampleTick = Long.MIN_VALUE;
        motionConfidence = 0;
        samples = 0;
    }

    private void resetTracking() {
        if (trial != null) finishAttack(trial.token, Outcome.CANCELLED);
        currentTarget = movementAnchor = null;
        visible = false;
        trial = null;
        resetMotion();
    }

    public void clear() {
        targets.clear();
        pendingResults.clear();
        resetTracking();
        lastResult = "none";
    }

    private static boolean finite(Vec3 value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }

    private record PendingResult(Trial trial, Outcome outcome, long deadline) { }

    private static final class TargetMemory {
        long lastSeen;
        final EnumMap<Attack, Habits[]> attacks = new EnumMap<>(Attack.class);

        Habits get(Attack attack, boolean aerial, boolean create) {
            Habits[] contexts = attacks.get(attack);
            if (contexts == null && create) {
                contexts = new Habits[2];
                attacks.put(attack, contexts);
            }
            if (contexts == null) return null;
            int index = aerial ? 1 : 0;
            if (contexts[index] == null && create) contexts[index] = new Habits();
            return contexts[index];
        }
    }

    private static final class Habits {
        final double[] responses = new double[Response.values().length];
        int observations;
        int outcomes;
        int predictions;
        double predictionWeight;
        double correctPredictions;
        double hits;
        double misses;
        long decayedAt;

        void decay(long now) {
            if (now == decayedAt) return;
            double factor = Math.pow(0.5D, Math.max(0, now - decayedAt) / 600.0D);
            for (int i = 0; i < responses.length; i++) responses[i] *= factor;
            hits *= factor;
            misses *= factor;
            predictionWeight *= factor;
            correctPredictions *= factor;
            decayedAt = now;
        }

        Expectation expectation(long now) {
            decay(now);
            double total = 0;
            double best = 0;
            double runnerUp = 0;
            Response response = Response.NONE;
            for (Response candidate : Response.values()) {
                double count = responses[candidate.ordinal()];
                total += count;
                if (count > best) {
                    runnerUp = best;
                    best = count;
                    response = candidate;
                } else runnerUp = Math.max(runnerUp, count);
            }
            double confidence = observations >= 3 && total >= 1.5D && best >= total * 0.6D
                    ? Math.min(0.8D, (best - runnerUp) / (total + 2.0D)) : 0;
            double accuracy = (correctPredictions + 1.0D) / (predictionWeight + 2.0D);
            confidence *= 0.5D + 0.5D * accuracy;
            return new Expectation(response, confidence, observations, (hits + 1.0D) / (hits + misses + 2.0D),
                    outcomes, accuracy, predictions);
        }
    }

    private static final class Trial {
        final long token;
        final UUID target;
        final Attack attack;
        final boolean aerial;
        final long startedAt;
        final int windupTicks;
        final Vec3 startPosition;
        final Vec3 startVelocity;
        final Vec3 forward;
        final Vec3 side;
        final Expectation expected;
        Vec3 lastPosition;
        long lastObservation;
        Response response;
        boolean obscured;
        boolean responseConfounded;
        boolean released;
        boolean hit;
        boolean contact;

        Trial(long token, UUID target, Attack attack, boolean aerial, long startedAt, int windupTicks,
              Vec3 position, Vec3 velocity, Vec3 forward, Expectation expected) {
            this.token = token;
            this.target = target;
            this.attack = attack;
            this.aerial = aerial;
            this.startedAt = startedAt;
            this.windupTicks = windupTicks;
            this.startPosition = this.lastPosition = position;
            this.startVelocity = velocity;
            this.forward = forward;
            this.side = new Vec3(-forward.z, 0, forward.x);
            this.expected = expected;
            this.lastObservation = startedAt;
        }
    }
}
