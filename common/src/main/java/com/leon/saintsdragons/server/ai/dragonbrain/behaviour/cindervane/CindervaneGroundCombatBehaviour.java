package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.cindervane;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.DragonAirCombatHelper;
import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.base.DragonLocomotionMode;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CindervaneGroundCombatBehaviour extends DragonBehaviour<Cindervane> {
    public static final float CHASE_SPEED = 1.15F;
    public static final double MELEE_STOP_RANGE = 5.0D;

    private static final double BITE_RANGE = 4.5D;
    private static final double DOUBLE_BITE_RANGE = 4.75D;
    private static final double SLASH_GRAB_MIN_RANGE = 2.0D;
    private static final double SLASH_GRAB_MAX_RANGE = 6.25D;
    private static final double BOMBARDMENT_MIN_RANGE = 8.0D;
    private static final double BOMBARDMENT_MAX_RANGE = 32.0D;
    private static final double FIRE_BODY_POINT_BLANK_RANGE = 3.25D;
    private static final double FIRE_BODY_GROUP_RANGE = 6.5D;
    private static final double FIRE_BODY_EXIT_RANGE = 11.0D;
    private static final double CLOSE_FACING_DOT = 0.35D;
    private static final double BOMBARDMENT_FACING_DOT = 0.55D;
    private static final int DECISION_INTERVAL_TICKS = 6;
    private static final int INITIAL_CHASE_COMMIT_TICKS = 12;
    private static final int RETREAT_CHASE_COMMIT_TICKS = 18;
    private static final int POST_ABILITY_CHASE_COMMIT_TICKS = 8;
    private static final int FIRE_BODY_MIN_TICKS = 60;
    private static final int FIRE_BODY_MAX_TICKS = 180;
    private static final float COMBAT_TURN_DEGREES_PER_TICK = 12.0F;

    private int decisionCooldown;
    private int chaseCommitTicks;
    private int fireBodyTicks;
    private boolean wasAbilityActive;
    private double previousGap = Double.NaN;
    private CombatAction lastAction = CombatAction.NONE;
    private CombatAction previousAction = CombatAction.NONE;
    private CombatState state = CombatState.CHASE;
    private String lastDecision = "idle";
    private double lastGap = -1.0D;
    private double lastAwaySpeed;
    private int lastNearbyEnemies;

    public CindervaneGroundCombatBehaviour() {
        super(Map.of(DragonMemories.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean canStart(DragonBrainContext<Cindervane> context) {
        return isGroundCombatContext(context);
    }

    @Override
    protected boolean canContinue(DragonBrainContext<Cindervane> context) {
        return isGroundCombatContext(context) || isMovementCommitted(context.dragon());
    }

    @Override
    protected void start(DragonBrainContext<Cindervane> context) {
        context.dragon().setAggressive(true);
        decisionCooldown = 0;
        chaseCommitTicks = INITIAL_CHASE_COMMIT_TICKS;
        fireBodyTicks = 0;
        wasAbilityActive = false;
        previousGap = Double.NaN;
        lastAction = CombatAction.NONE;
        previousAction = CombatAction.NONE;
        state = CombatState.CHASE;
        lastDecision = "chase:engaged";
    }

    @Override
    protected void tick(DragonBrainContext<Cindervane> context) {
        Cindervane dragon = context.dragon();
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (!dragon.isTargetValid(target)) {
            deactivateFireBody(dragon);
            if (dragon.combatManager.getActiveAbility() != null) {
                state = CombatState.ABILITY;
                lastDecision = "ability:finishing-without-target";
                claimMovement(context, "ability-without-target");
            } else {
                lastDecision = "idle:invalid-target";
            }
            return;
        }

        dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (decisionCooldown > 0) {
            decisionCooldown--;
        }

        boolean hasLineOfSight = dragon.getSensing().hasLineOfSight(target);
        double gap = gapToTarget(dragon, target);
        TargetMotion motion = updateTargetMotion(dragon, target, gap);
        int nearbyEnemies = countEngagedEnemies(dragon, target, FIRE_BODY_GROUP_RANGE);
        lastGap = gap;
        lastAwaySpeed = motion.awaySpeed();
        lastNearbyEnemies = nearbyEnemies;

        updateFireBody(dragon, gap, hasLineOfSight);

        if (dragon.combatManager.getActiveAbility() != null) {
            wasAbilityActive = true;
            state = CombatState.ABILITY;
            lastDecision = "ability:committed=" + activeAbilityName(dragon);
            claimMovement(context, "ability-committed");
            return;
        }
        if (wasAbilityActive) {
            wasAbilityActive = false;
            chaseCommitTicks = Math.max(chaseCommitTicks, POST_ABILITY_CHASE_COMMIT_TICKS);
        }

        if (dragon.isInWaterOrBubble()) {
            deactivateFireBody(dragon);
            handleWaterMelee(context, dragon, target, gap, hasLineOfSight);
            return;
        }

        boolean biteOnlyTarget = DragonTargetingHelper.isBiteOnlyPreyTarget(dragon, target);
        if (biteOnlyTarget) {
            handleBiteOnlyTarget(context, dragon, target, gap, hasLineOfSight);
            return;
        }

        if (hasLineOfSight && gap <= SLASH_GRAB_MAX_RANGE) {
            turnTowardTarget(dragon, target);
        }

        if (tryActivateFireBody(dragon, gap, nearbyEnemies)) {
            state = CombatState.STANCE;
            lastDecision = "stance:fire-body";
            return;
        }

        if (!hasLineOfSight) {
            commitToChase(RETREAT_CHASE_COMMIT_TICKS, "chase:no-line-of-sight");
            return;
        }
        if (motion.openingDistance() && gap > SLASH_GRAB_MAX_RANGE) {
            commitToChase(RETREAT_CHASE_COMMIT_TICKS, "chase:target-retreating");
        }
        if (chaseCommitTicks > 0 && gap > SLASH_GRAB_MAX_RANGE) {
            chaseCommitTicks--;
            state = CombatState.CHASE;
            if (!motion.openingDistance()) {
                lastDecision = "chase:committed";
            }
            return;
        }

        if (decisionCooldown > 0 || dragon.getAiCombatPacing().getCadenceCooldownTicks() > 0) {
            state = gap <= SLASH_GRAB_MAX_RANGE ? CombatState.ALIGN : CombatState.CHASE;
            lastDecision = "pacing";
            return;
        }

        decisionCooldown = DECISION_INTERVAL_TICKS;
        CombatSnapshot snapshot = new CombatSnapshot(
                gap,
                isFacingTarget(dragon, target),
                motion.awaySpeed(),
                motion.targetSpeed(),
                nearbyEnemies,
                target.getHealth() / Math.max(1.0F, target.getMaxHealth())
        );
        CombatAction selected = selectAction(dragon, snapshot);
        if (selected != CombatAction.NONE && startAction(dragon, selected)) {
            rememberAction(selected);
            state = selected == CombatAction.MAGMA_VOLLEY ? CombatState.PRESSURE : CombatState.MELEE;
            lastDecision = "started:" + selected.debugName;
            claimMovement(context, "started-" + selected.debugName);
            return;
        }

        state = gap <= SLASH_GRAB_MAX_RANGE ? CombatState.ALIGN : CombatState.CHASE;
        lastDecision = state == CombatState.ALIGN ? "aligning" : "chase:no-opening";
        if (state == CombatState.CHASE) {
            chaseCommitTicks = Math.max(chaseCommitTicks, 6);
        }
    }

    @Override
    protected void stop(DragonBrainContext<Cindervane> context) {
        Cindervane dragon = context.dragon();
        if (!context.memories().has(DragonMemories.ATTACK_TARGET)) {
            dragon.setAggressive(false);
        }
        deactivateFireBody(dragon);
        decisionCooldown = 0;
        chaseCommitTicks = 0;
        fireBodyTicks = 0;
        wasAbilityActive = false;
        previousGap = Double.NaN;
        state = CombatState.CHASE;
        lastDecision = "stopped";
    }

    public static double groundStopRange(Cindervane dragon, LivingEntity target) {
        return MELEE_STOP_RANGE + (dragon.getBbWidth() + target.getBbWidth()) * 0.5D;
    }

    public static boolean isMovementCommitted(Cindervane dragon) {
        return dragon != null && dragon.combatManager.getActiveAbility() != null;
    }

    private boolean isGroundCombatContext(DragonBrainContext<Cindervane> context) {
        Cindervane dragon = context.dragon();
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (target == null
                || context.memories().get(DragonMemories.GROUND_ROUTE_ABANDONED).orElse(false)
                || context.memories().get(DragonMemories.TARGET_AIRBORNE).orElse(false)
                || !DragonAirCombatHelper.isValidCombatTarget(dragon, target)
                || dragon.isBaby()
                || dragon.isVehicle()
                || dragon.isPassenger()
                || dragon.isOrderedToSit()
                || dragon.isAerial()
                || dragon.distanceToSqr(target) > DragonAirCombatHelper.maxAggroDistanceSqr(dragon, 32.0D)) {
            return false;
        }
        DragonLocomotionMode mode = context.memories()
                .get(DragonMemories.LOCOMOTION_MODE)
                .orElse(dragon.getLocomotionMode());
        return mode == DragonLocomotionMode.GROUND || mode == DragonLocomotionMode.WATER;
    }

    private void handleWaterMelee(DragonBrainContext<Cindervane> context,
                                  Cindervane dragon,
                                  LivingEntity target,
                                  double gap,
                                  boolean hasLineOfSight) {
        if (gap <= BITE_RANGE && hasLineOfSight && canDecide(dragon)
                && startAbility(dragon, ModAbilities.CINDERVANE_BITE, false, 20, 24, 0, 16)) {
            rememberAction(CombatAction.BITE);
            state = CombatState.MELEE;
            lastDecision = "water:bite";
            claimMovement(context, "water-bite");
            return;
        }
        state = CombatState.CHASE;
        lastDecision = "water:closing";
    }

    private void handleBiteOnlyTarget(DragonBrainContext<Cindervane> context,
                                      Cindervane dragon,
                                      LivingEntity target,
                                      double gap,
                                      boolean hasLineOfSight) {
        if (gap <= BITE_RANGE && hasLineOfSight && canDecide(dragon)
                && startAbility(dragon, ModAbilities.CINDERVANE_BITE, false, 20, 24, 0, 16)) {
            rememberAction(CombatAction.BITE);
            state = CombatState.MELEE;
            lastDecision = "prey:bite";
            claimMovement(context, "prey-bite");
            return;
        }
        state = CombatState.CHASE;
        lastDecision = hasLineOfSight ? "prey:closing" : "prey:no-line-of-sight";
    }

    private CombatAction selectAction(Cindervane dragon, CombatSnapshot snapshot) {
        AbilityChoice best = new AbilityChoice(CombatAction.NONE, 0.0D);

        double doubleBiteScore = 92.0D
                + (DOUBLE_BITE_RANGE - snapshot.gap()) * 6.0D
                + Math.max(0.0D, snapshot.awaySpeed()) * 18.0D
                + snapshot.targetHealthRatio() * 8.0D;
        best = consider(dragon, best, CombatAction.DOUBLE_BITE, doubleBiteScore,
                snapshot.facing()
                        && snapshot.gap() <= DOUBLE_BITE_RANGE
                        && canUse(dragon, ModAbilities.CINDERVANE_DOUBLE_BITE, false));

        double slashScore = 84.0D
                + snapshot.targetHealthRatio() * 12.0D
                + (snapshot.gap() >= 3.0D ? 8.0D : -8.0D)
                - Math.max(0.0D, snapshot.awaySpeed()) * 10.0D;
        best = consider(dragon, best, CombatAction.SLASH_GRAB, slashScore,
                snapshot.facing()
                        && snapshot.gap() >= SLASH_GRAB_MIN_RANGE
                        && snapshot.gap() <= SLASH_GRAB_MAX_RANGE
                        && canUse(dragon, ModAbilities.CINDERVANE_SLASH_GRAB, true));

        double bombScore = 68.0D
                + Math.min(18.0D, (snapshot.gap() - BOMBARDMENT_MIN_RANGE) * 0.8D)
                + Math.max(0, snapshot.nearbyEnemies() - 1) * 10.0D
                + (snapshot.targetSpeed() < 0.25D ? 10.0D : 0.0D);
        best = consider(dragon, best, CombatAction.MAGMA_VOLLEY, bombScore,
                snapshot.facing()
                        && snapshot.gap() >= BOMBARDMENT_MIN_RANGE
                        && snapshot.gap() <= BOMBARDMENT_MAX_RANGE
                        && canUse(dragon, ModAbilities.CINDERVANE_MAGMA_VOLLEY, true));

        double biteScore = 56.0D + (BITE_RANGE - snapshot.gap()) * 3.0D;
        best = consider(dragon, best, CombatAction.BITE, biteScore,
                snapshot.facing()
                        && snapshot.gap() <= BITE_RANGE
                        && canUse(dragon, ModAbilities.CINDERVANE_BITE, false));

        return best.action();
    }

    private AbilityChoice consider(Cindervane dragon,
                                   AbilityChoice current,
                                   CombatAction action,
                                   double score,
                                   boolean eligible) {
        if (!eligible) {
            return current;
        }
        if (action == lastAction) {
            score -= 26.0D;
        } else if (action == previousAction) {
            score -= 10.0D;
        }
        score += (dragon.getRandom().nextDouble() - 0.5D) * 4.0D;
        return score > current.score() ? new AbilityChoice(action, score) : current;
    }

    private boolean startAction(Cindervane dragon, CombatAction action) {
        return switch (action) {
            case BITE -> startAbility(dragon, ModAbilities.CINDERVANE_BITE,
                    false, 20, 24, 0, 16);
            case DOUBLE_BITE -> startAbility(dragon, ModAbilities.CINDERVANE_DOUBLE_BITE,
                    false, 28, 80, 0, 70);
            case SLASH_GRAB -> startAbility(dragon, ModAbilities.CINDERVANE_SLASH_GRAB,
                    true, 40, 140, 90, 120);
            case MAGMA_VOLLEY -> startAbility(dragon, ModAbilities.CINDERVANE_MAGMA_VOLLEY,
                    true, 55, 200, 140, 180);
            case NONE -> false;
        };
    }

    private boolean tryActivateFireBody(Cindervane dragon, double gap, int nearbyEnemies) {
        if (dragon.isAbilityActive(ModAbilities.CINDERVANE_FIRE_BODY)
                || dragon.isFireBodySuppressed()
                || dragon.combatManager.getActiveAbility() != null
                || dragon.getAiCombatPacing().getCadenceCooldownTicks() > 0) {
            return false;
        }
        boolean pointBlank = gap <= FIRE_BODY_POINT_BLANK_RANGE;
        boolean surrounded = nearbyEnemies >= 2 && gap <= FIRE_BODY_GROUP_RANGE;
        if (!pointBlank && !surrounded) {
            return false;
        }
        if (!startAbility(dragon, ModAbilities.CINDERVANE_FIRE_BODY, true, 10, 240, 100, 200)) {
            return false;
        }
        fireBodyTicks = 0;
        return true;
    }

    private void updateFireBody(Cindervane dragon, double gap, boolean hasLineOfSight) {
        if (!dragon.isAbilityActive(ModAbilities.CINDERVANE_FIRE_BODY)) {
            fireBodyTicks = 0;
            return;
        }
        fireBodyTicks++;
        boolean unsafe = dragon.isInWaterOrBubble() || dragon.isFireBodySuppressed();
        boolean spent = fireBodyTicks >= FIRE_BODY_MAX_TICKS;
        boolean disengaged = fireBodyTicks >= FIRE_BODY_MIN_TICKS
                && (gap > FIRE_BODY_EXIT_RANGE || !hasLineOfSight);
        boolean attackInProgress = dragon.combatManager.getActiveAbility() != null;
        if (unsafe || !attackInProgress && (spent || disengaged)) {
            deactivateFireBody(dragon);
        }
    }

    private void deactivateFireBody(Cindervane dragon) {
        if (dragon.isAbilityActive(ModAbilities.CINDERVANE_FIRE_BODY)) {
            dragon.forceEndAbility(ModAbilities.CINDERVANE_FIRE_BODY);
        }
        fireBodyTicks = 0;
    }

    private boolean canDecide(Cindervane dragon) {
        return decisionCooldown <= 0
                && dragon.getAiCombatPacing().getCadenceCooldownTicks() <= 0
                && dragon.combatManager.getActiveAbility() == null;
    }

    private boolean canUse(Cindervane dragon,
                           DragonAbilityType<?, ?> abilityType,
                           boolean majorAbility) {
        return dragon.combatManager.canStart(abilityType)
                && dragon.getAiCombatPacing().canUse(abilityType, majorAbility);
    }

    private boolean startAbility(Cindervane dragon,
                                 DragonAbilityType<?, ?> abilityType,
                                 boolean majorAbility,
                                 int cadenceTicks,
                                 int abilityCooldownTicks,
                                 int majorCooldownTicks,
                                 int repeatLockoutTicks) {
        return dragon.combatManager.tryUseAiAbility(
                abilityType,
                majorAbility,
                cadenceTicks,
                abilityCooldownTicks,
                majorCooldownTicks,
                repeatLockoutTicks
        );
    }

    private TargetMotion updateTargetMotion(Cindervane dragon, LivingEntity target, double gap) {
        double gapDelta = Double.isFinite(previousGap) ? gap - previousGap : 0.0D;
        previousGap = gap;
        Vec3 toTarget = target.position().subtract(dragon.position()).multiply(1.0D, 0.0D, 1.0D);
        Vec3 velocity = target.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
        double targetSpeed = velocity.length();
        double awaySpeed = toTarget.lengthSqr() > 1.0E-6D
                ? velocity.dot(toTarget.normalize())
                : 0.0D;
        return new TargetMotion(gapDelta, awaySpeed, targetSpeed,
                gapDelta > 0.08D || awaySpeed > 0.16D);
    }

    private int countEngagedEnemies(Cindervane dragon, LivingEntity target, double radius) {
        return dragon.level().getEntitiesOfClass(
                LivingEntity.class,
                dragon.getBoundingBox().inflate(radius, 4.0D, radius),
                entity -> entity != dragon
                        && entity.isAlive()
                        && entity.attackable()
                        && !dragon.isAlly(entity)
                        && (entity == target
                        || entity.getLastHurtMob() == dragon
                        || entity.getLastHurtByMob() == dragon)
        ).size();
    }

    private boolean isFacingTarget(Cindervane dragon, LivingEntity target) {
        Vec3 toTarget = target.getBoundingBox().getCenter().subtract(dragon.getBoundingBox().getCenter());
        Vec3 horizontal = new Vec3(toTarget.x, 0.0D, toTarget.z);
        if (horizontal.lengthSqr() <= 1.0E-6D) {
            return true;
        }
        Vec3 facing = Vec3.directionFromRotation(0.0F, dragon.getYRot());
        double threshold = lastGap >= BOMBARDMENT_MIN_RANGE
                ? BOMBARDMENT_FACING_DOT
                : CLOSE_FACING_DOT;
        return facing.dot(horizontal.normalize()) >= threshold;
    }

    private void turnTowardTarget(Cindervane dragon, LivingEntity target) {
        double dx = target.getX() - dragon.getX();
        double dz = target.getZ() - dragon.getZ();
        if (dx * dx + dz * dz <= 1.0E-6D) {
            return;
        }
        float targetYaw = (float)(Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float yaw = Mth.approachDegrees(dragon.getYRot(), targetYaw, COMBAT_TURN_DEGREES_PER_TICK);
        dragon.setYRot(yaw);
        dragon.yBodyRot = yaw;
    }

    private double gapToTarget(Cindervane dragon, LivingEntity target) {
        double radii = (dragon.getBbWidth() + target.getBbWidth()) * 0.5D;
        return Math.max(0.0D, dragon.distanceTo(target) - radii);
    }

    private String activeAbilityName(Cindervane dragon) {
        return dragon.combatManager.getActiveAbility() == null
                ? "none"
                : dragon.combatManager.getActiveAbility().getClass().getSimpleName();
    }

    private void commitToChase(int ticks, String reason) {
        chaseCommitTicks = Math.max(chaseCommitTicks, ticks);
        state = CombatState.CHASE;
        lastDecision = reason;
    }

    private void rememberAction(CombatAction action) {
        previousAction = lastAction;
        lastAction = action;
    }

    private void claimMovement(DragonBrainContext<Cindervane> context, String reason) {
        context.memories().set(
                DragonMemories.MOVEMENT_INTENT,
                DragonMovementIntent.stop("cindervane-combat:" + reason)
        );
        context.memories().erase(DragonMemories.WALK_TARGET);
        context.memories().erase(DragonMemories.PATH);
        context.dragon().getAIMovement().stop();
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("state", state.name().toLowerCase());
        details.put("decision", lastDecision);
        details.put("last_action", lastAction.debugName);
        details.put("gap", lastGap < 0.0D ? "none" : String.format("%.2f", lastGap));
        details.put("target_away_speed", String.format("%.3f", lastAwaySpeed));
        details.put("nearby_enemies", Integer.toString(lastNearbyEnemies));
        details.put("chase_commit_ticks", Integer.toString(chaseCommitTicks));
        details.put("fire_body_ticks", Integer.toString(fireBodyTicks));
        return Map.copyOf(details);
    }

    private enum CombatState {
        CHASE,
        ALIGN,
        MELEE,
        PRESSURE,
        STANCE,
        ABILITY
    }

    private enum CombatAction {
        NONE("none"),
        BITE("bite"),
        DOUBLE_BITE("double-bite"),
        SLASH_GRAB("slash-grab"),
        MAGMA_VOLLEY("magma-volley");

        private final String debugName;

        CombatAction(String debugName) {
            this.debugName = debugName;
        }
    }

    private record AbilityChoice(CombatAction action, double score) {
    }

    private record CombatSnapshot(double gap,
                                  boolean facing,
                                  double awaySpeed,
                                  double targetSpeed,
                                  int nearbyEnemies,
                                  float targetHealthRatio) {
    }

    private record TargetMotion(double gapDelta,
                                double awaySpeed,
                                double targetSpeed,
                                boolean openingDistance) {
    }
}
