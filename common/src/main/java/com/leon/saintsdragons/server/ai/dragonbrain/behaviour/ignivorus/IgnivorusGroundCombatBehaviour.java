package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ignivorus;

import com.leon.saintsdragons.common.config.dragon.profile.IgnivorusStatProfile;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.ai.dragonbrain.learning.DragonCombatLearning;
import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.abilities.ignivorus.IgnivorusFireballAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.ignivorus.IgnivorusFireBreathAbility;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public class IgnivorusGroundCombatBehaviour extends DragonBehaviour<Ignivorus> {
    public static final double MELEE_ENGAGE_RANGE = IgnivorusStatProfile.GroundCombatBehaviour.MELEE_ENGAGE_RANGE;
    public static final float CHASE_SPEED = IgnivorusStatProfile.GroundCombatBehaviour.CHASE_SPEED;
    private Ignivorus dragon;
    private DragonBrainContext<Ignivorus> currentContext;
    private int attackCooldown = 0;
    private int decisionCooldown = 0;
    private int fireballDecisionCooldown = 0;
    private int fireballPostCooldown = 0;
    private long lastCooldownTick = -1;
    private FireballMode fireballMode = FireballMode.NONE;
    private int fireballDesiredLevel = 0;
    private CombatAction lastAction = CombatAction.NONE;
    private CombatAction previousAction = CombatAction.NONE;
    private static final int DECISION_INTERVAL_TICKS = 8;
    private static final int FIREBALL_DECISION_COOLDOWN_TICKS = IgnivorusStatProfile.GroundCombatBehaviour.FIREBALL_DECISION_COOLDOWN_TICKS;
    private static final int FIREBALL_POST_COOLDOWN_TICKS = IgnivorusStatProfile.GroundCombatBehaviour.FIREBALL_POST_COOLDOWN_TICKS;
    private static final double FIREBALL_MIN_GAP = 8.0;
    private static final double FIREBALL_MAX_GAP = 48.0;
    private static final double BODY_SLAM_POINT_BLANK_GAP = 2.5D;
    private static final double AI_PHASE2_LEAP_TRIGGER_GAP = IgnivorusStatProfile.GroundCombatBehaviour.AI_PHASE2_LEAP_TRIGGER_GAP;
    private static final double AI_PHASE2_LEAP_MAX_GAP = IgnivorusStatProfile.GroundCombatBehaviour.AI_PHASE2_LEAP_MAX_GAP;
    private static final int AI_PHASE2_LEAP_POST_COOLDOWN = IgnivorusStatProfile.GroundCombatBehaviour.AI_PHASE2_LEAP_POST_COOLDOWN;
    private static final double MIN_ABILITY_SCORE = 50.0D;

    public IgnivorusGroundCombatBehaviour() {
        super(Map.of(DragonMemories.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
    }

    public double getPreferredStopDistance(Ignivorus dragon, LivingEntity target) {
        double gap = MELEE_ENGAGE_RANGE;
        if (dragon.shouldFavorRangedCombat(target) && dragon.getSensing().hasLineOfSight(target)) {
            boolean breathReady = dragon.isAiAirBreathReady()
                    && IgnivorusFireBreathAbility.canStartAiBreath(dragon, target);
            boolean fireballReady = dragon.isPhase2Active()
                    && (dragon.isAbilityActive(ModAbilities.IGNIVORUS_FIREBALL)
                        || fireballDecisionCooldown <= 0 && fireballPostCooldown <= 0
                        && dragon.combatManager.canStartAiAbility(ModAbilities.IGNIVORUS_FIREBALL, true))
                    && dragon.hasAiFireballShot(target, 64.0D);
            if (breathReady || fireballReady) gap = 22.0D + (breathReady ? dragon.getAiBreathSpacingBonus(target) : 0);
        }
        return gap + (dragon.getBbWidth() + target.getBbWidth()) * 0.5D;
    }

    @Override
    protected boolean canStart(DragonBrainContext<Ignivorus> context) {
        this.dragon = context.dragon();
        if (dragon.isBaby()) {
            return false;
        }
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);

        if (!dragon.isTargetValid(target)) {
            return false;
        }

        if (target instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
        }

        if (dragon.isVehicle() || dragon.isOrderedToSit()) {
            return false;
        }
        if (dragon.isAiSpecialCombatActive()) {
            return false;
        }
        if (dragon.areRiderControlsLocked() || dragon.isLeaping() || dragon.isLeapImpactRecovering()
                || dragon.isWaitingForWildPhase2Entry()) {
            return false;
        }

        if (dragon.getCombatFlightState().wantsFlight()) {
            return false;
        }

        if (!dragon.isInWaterOrBubble() && dragon.isAerial()) {
            return false;
        }

        if (dragon.distanceToSqr(target) > getMaxAggroDistanceSqr()) {
            return false;
        }

        return true;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<Ignivorus> context) {
        this.dragon = context.dragon();
        if (dragon.isBaby()) {
            return false;
        }
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);

        if (!dragon.isTargetValid(target)) {
            return false;
        }

        if (target instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
        }

        if (dragon.isVehicle() || dragon.isOrderedToSit()) {
            return false;
        }
        if (dragon.isAiSpecialCombatActive()) {
            return false;
        }
        if (dragon.areRiderControlsLocked() || dragon.isLeaping() || dragon.isLeapImpactRecovering()
                || dragon.isWaitingForWildPhase2Entry()) {
            return false;
        }

        if (!dragon.isInWaterOrBubble() && dragon.isAerial()) {
            return false;
        }


        if (dragon.isAbilityActive(ModAbilities.IGNIVORUS_FIRE_BREATH)
            || dragon.isAbilityActive(ModAbilities.IGNIVORUS_ULTIMATE)
            || dragon.isAbilityActive(ModAbilities.IGNIVORUS_WING_SWIPE)
            || dragon.isAbilityActive(ModAbilities.IGNIVORUS_STOMP)
            || dragon.isAbilityActive(ModAbilities.IGNIVORUS_FIREBALL)
            || dragon.isAbilityActive(ModAbilities.IGNIVORUS_BODY_SLAM)
            || dragon.isAbilityActive(ModAbilities.IGNIVORUS_BITE)
            || dragon.isAbilityActive(ModAbilities.IGNIVORUS_ROAR)) {
            return true;
        }

        if (dragon.getCombatFlightState().wantsFlight()) {
            return false;
        }

        if (dragon.distanceToSqr(target) > getMaxAggroDistanceSqr()) {
            return false;
        }

        return true;
    }

    @Override
    protected void stop(DragonBrainContext<Ignivorus> context) {
        dragon.setAggressive(false);
        cancelFireBreathIfActive();
        currentContext = null;
    }

    @Override
    protected void start(DragonBrainContext<Ignivorus> context) {
        this.dragon = context.dragon();
        dragon.setAggressive(true);
    }

    @Override
    protected void tick(DragonBrainContext<Ignivorus> context) {
        this.currentContext = context;

        if (dragon.areRiderControlsLocked() || dragon.isLeaping() || dragon.isLeapImpactRecovering()
                || dragon.isWaitingForWildPhase2Entry()) {
            stopMovement("ignivorus-combat:mobility-locked");
            return;
        }

        // Ground cooldowns also elapse during flight and phase transitions.
        long now = dragon.level().getGameTime();
        int elapsed = lastCooldownTick < 0 ? 1
                : (int) Math.min(Integer.MAX_VALUE, Math.max(0, now - lastCooldownTick));
        lastCooldownTick = now;
        attackCooldown = Math.max(0, attackCooldown - elapsed);
        decisionCooldown = Math.max(0, decisionCooldown - elapsed);
        fireballDecisionCooldown = Math.max(0, fireballDecisionCooldown - elapsed);
        fireballPostCooldown = Math.max(0, fireballPostCooldown - elapsed);

        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (!dragon.isTargetValid(target)) {
            cancelFireBreathIfActive();
            return;
        }

        dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);

        boolean biteOnlyPrey = DragonTargetingHelper.isBiteOnlyPreyTarget(dragon, target);

        if (!biteOnlyPrey && shouldTriggerLowHealthUltimate()) {
            tryUseAbility(ModAbilities.IGNIVORUS_ULTIMATE, true, 20, 140, 180, 100);
            if (dragon.isAbilityActive(ModAbilities.IGNIVORUS_ULTIMATE)) {
                attackCooldown = 0;
                return;
            }
        }

        double gap = getGapToTarget(target);
        boolean hasLineOfSight = dragon.getSensing().hasLineOfSight(target);

        if (!biteOnlyPrey && handleFireballActive(target)) {
            return;
        }

        if (dragon.isInWaterOrBubble()) {
            handleWaterCombat(target, gap, hasLineOfSight);
            return;
        }

        if (biteOnlyPrey && !dragon.isPhase2Active()) {
            handleBiteOnlyTarget(target, gap, hasLineOfSight);
            return;
        }

        if (gap <= MELEE_ENGAGE_RANGE) {
            stopMovement("ignivorus-combat:close-range-hold");
        }

        if (isCurrentlyAttacking()
                || attackCooldown > 0
                || decisionCooldown > 0
                || dragon.getAiCombatPacing().getCadenceCooldownTicks() > 0) {
            return;
        }

        decisionCooldown = DECISION_INTERVAL_TICKS;
        CombatSnapshot snapshot = createCombatSnapshot(target, gap, hasLineOfSight);
        CombatAction action = selectAbility(snapshot);
        startSelectedAbility(action, target, snapshot);
    }

    private boolean isCurrentlyAttacking() {
        return dragon.isAbilityActive(ModAbilities.IGNIVORUS_BITE)
            || dragon.isAbilityActive(ModAbilities.IGNIVORUS_BODY_SLAM)
            || dragon.isAbilityActive(ModAbilities.IGNIVORUS_WING_SWIPE)
            || dragon.isAbilityActive(ModAbilities.IGNIVORUS_STOMP)
            || dragon.isAbilityActive(ModAbilities.IGNIVORUS_FIREBALL)
            || dragon.isAbilityActive(ModAbilities.IGNIVORUS_FIRE_BREATH)
            || dragon.isAbilityActive(ModAbilities.IGNIVORUS_ROAR)
            || dragon.isAbilityActive(ModAbilities.IGNIVORUS_ULTIMATE)
            || dragon.isLeaping()
            || dragon.isLeapImpactRecovering();
    }

    private void cancelFireBreathIfActive() {
        // Don't interfere if being ridden (let rider control abilities)
        if (dragon.isVehicle()) {
            return;
        }

        if (dragon.isAbilityActive(ModAbilities.IGNIVORUS_FIRE_BREATH)) {
            dragon.forceEndAbility(ModAbilities.IGNIVORUS_FIRE_BREATH);
        }
    }

    private void handleBiteOnlyTarget(LivingEntity target, double gap, boolean hasLineOfSight) {
        if (gap > MELEE_ENGAGE_RANGE || !hasLineOfSight) {
            return;
        }
        stopMovement("ignivorus-combat:bite-only");
        if (attackCooldown > 0 || isCurrentlyAttacking()) {
            return;
        }
        if (tryUseAbility(ModAbilities.IGNIVORUS_BITE, false, 30, 30, 0, 24)) {
            attackCooldown = 30;
            rememberAction(CombatAction.BITE);
        }
    }

    private boolean handleFireballActive(LivingEntity target) {
        DragonAbility<?> active = dragon.getActiveAbility();
        if (!(active instanceof IgnivorusFireballAbility fireball)) {
            if (fireballMode != FireballMode.NONE) {
                fireballMode = FireballMode.NONE;
                fireballDesiredLevel = 0;
                fireballPostCooldown = FIREBALL_POST_COOLDOWN_TICKS;
            }
            return false;
        }

        dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (fireball.updateAiAim().needsAlignment() || fireballMode == FireballMode.STATIONARY) {
            stopMovement("ignivorus-combat:stationary-fireball");
        }

        int level = dragon.getFireballChargeLevel();
        if (level >= fireballDesiredLevel && fireballDesiredLevel > 0) {
            fireball.requestRelease();
        }

        return true;
    }

    private CombatSnapshot createCombatSnapshot(LivingEntity target, double gap, boolean hasLineOfSight) {
        Vec3 horizontalToTarget = target.position().subtract(dragon.position()).multiply(1.0D, 0.0D, 1.0D);
        boolean hasHorizontalDirection = horizontalToTarget.lengthSqr() > 1.0E-6D;
        Vec3 directionToTarget = hasHorizontalDirection
                ? horizontalToTarget.normalize()
                : Vec3.ZERO;
        Vec3 relativeVelocity = target.getDeltaMovement().subtract(dragon.getDeltaMovement());
        double closingSpeed = hasHorizontalDirection ? -relativeVelocity.dot(directionToTarget) : 0.0D;

        Vec3 horizontalLook = dragon.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        double facingDot = horizontalLook.lengthSqr() > 1.0E-6D && hasHorizontalDirection
                ? horizontalLook.normalize().dot(directionToTarget)
                : 1.0D;

        return new CombatSnapshot(
                gap,
                hasLineOfSight,
                Math.max(0.0D, closingSpeed),
                target.getDeltaMovement().horizontalDistance(),
                Mth.clamp(facingDot, -1.0D, 1.0D),
                countEnemiesAround(dragon.position(), 18.0D),
                countEnemiesAround(target.position(), 8.0D),
                target.onGround(),
                dragon.canStartLeapSlamForAI(target)
        );
    }

    private int countEnemiesAround(Vec3 center, double radius) {
        AABB bounds = new AABB(center, center).inflate(radius);
        return dragon.level().getEntitiesOfClass(
                LivingEntity.class,
                bounds,
                entity -> entity != dragon
                        && entity.isAlive()
                        && entity.attackable()
                        && !dragon.isAlly(entity)
                        && entity.getBoundingBox().getCenter().distanceToSqr(center) <= radius * radius
        ).size();
    }

    private CombatAction selectAbility(CombatSnapshot snapshot) {
        AbilityChoice best = new AbilityChoice(CombatAction.NONE, Double.NEGATIVE_INFINITY);
        boolean favorRanged = dragon.shouldFavorRangedCombat(dragon.getTarget());
        int extraNearbyEnemies = Math.max(0, snapshot.nearbyEnemies() - 1);
        int extraClusteredEnemies = Math.max(0, snapshot.targetClusterSize() - 1);

        if (!dragon.isPhase2Active()
                && snapshot.hasLineOfSight()
                && snapshot.gap() <= MELEE_ENGAGE_RANGE) {
            double biteScore = 66.0D
                    + snapshot.gap() * 2.0D
                    - (snapshot.gap() <= BODY_SLAM_POINT_BLANK_GAP ? 12.0D : 0.0D)
                    - extraNearbyEnemies * 8.0D;
            if (favorRanged) biteScore = Math.max(MIN_ABILITY_SCORE, biteScore - 16.0D);
            best = consider(best, CombatAction.BITE, biteScore,
                    canUseAiAbility(ModAbilities.IGNIVORUS_BITE, false));
        }

        if (!dragon.isPhase2Active()) {
            double bodySlamScore = 82.0D
                    + extraNearbyEnemies * 22.0D
                    + extraClusteredEnemies * 10.0D
                    + (BODY_SLAM_POINT_BLANK_GAP - snapshot.gap()) * 6.0D
                    + snapshot.closingSpeed() * 45.0D;
            best = consider(best, CombatAction.BODY_SLAM, bodySlamScore,
                    dragon.isGroundedForAction()
                            && snapshot.gap() <= BODY_SLAM_POINT_BLANK_GAP
                            && canUseAiAbility(ModAbilities.IGNIVORUS_BODY_SLAM, false));
        } else {
            double stompScore = 48.0D
                    + extraNearbyEnemies * 20.0D
                    + extraClusteredEnemies * 8.0D
                    + (snapshot.gap() <= 4.0D ? 30.0D : 0.0D)
                    + (snapshot.gap() >= 7.0D && snapshot.gap() <= 18.0D ? 16.0D : 0.0D)
                    + snapshot.closingSpeed() * 45.0D;
            best = consider(best, CombatAction.STOMP, stompScore,
                    dragon.isGroundedForAction()
                            && snapshot.gap() <= 18.0D
                            && canUseAiAbility(ModAbilities.IGNIVORUS_STOMP, false));

            double wingSwipeScore = 50.0D
                    + extraNearbyEnemies * 14.0D
                    + (snapshot.gap() <= 6.0D ? 22.0D : 0.0D)
                    + (snapshot.gap() >= 5.0D && snapshot.gap() <= 20.0D ? 10.0D : 0.0D)
                    + (snapshot.facingDot() < 0.75D ? 12.0D : 0.0D)
                    + snapshot.closingSpeed() * 35.0D;
            best = consider(best, CombatAction.WING_SWIPE, wingSwipeScore,
                    dragon.isGroundedForAction()
                            && snapshot.gap() <= 22.0D
                            && canUseAiAbility(ModAbilities.IGNIVORUS_WING_SWIPE, false));

            double fireballScore = 54.0D
                    + (favorRanged ? 22.0D : 0.0D)
                    + Mth.clamp((snapshot.gap() - 12.0D) * 0.9D, 0.0D, 24.0D)
                    + (snapshot.targetSpeed() < 0.22D ? 10.0D : 0.0D)
                    + extraClusteredEnemies * 8.0D
                    - snapshot.closingSpeed() * 30.0D;
            best = consider(best, CombatAction.FIREBALL, fireballScore,
                    snapshot.hasLineOfSight()
                            && snapshot.gap() >= FIREBALL_MIN_GAP
                            && snapshot.gap() <= FIREBALL_MAX_GAP
                            && fireballDecisionCooldown <= 0
                            && fireballPostCooldown <= 0
                            && canUseAiAbility(ModAbilities.IGNIVORUS_FIREBALL, true));

            double leapScore = 50.0D
                    + Mth.clamp((snapshot.gap() - AI_PHASE2_LEAP_TRIGGER_GAP) * 1.3D, 0.0D, 30.0D)
                    + (snapshot.targetGrounded() ? 8.0D : -40.0D);
            best = consider(best, CombatAction.LEAP, leapScore,
                    snapshot.hasLineOfSight()
                            && snapshot.targetGrounded()
                            && snapshot.leapReady()
                            && snapshot.gap() >= AI_PHASE2_LEAP_TRIGGER_GAP
                            && snapshot.gap() <= AI_PHASE2_LEAP_MAX_GAP);
        }

        double breathScore = 52.0D
                + (favorRanged ? 28.0D : 0.0D)
                + (snapshot.gap() >= 10.0D && snapshot.gap() <= 20.0D ? 14.0D : 0.0D)
                + extraClusteredEnemies * 12.0D
                + (snapshot.targetSpeed() < 0.18D ? 8.0D : 0.0D)
                - snapshot.closingSpeed() * 20.0D;
        best = consider(best, CombatAction.FIRE_BREATH, breathScore,
                snapshot.hasLineOfSight()
                        && snapshot.gap() >= 10.0D
                        && snapshot.gap() <= (favorRanged ? 48.0D : 24.0D)
                        && canUseAiAbility(ModAbilities.IGNIVORUS_FIRE_BREATH, true)
                        && IgnivorusFireBreathAbility.canStartAiBreath(dragon, dragon.getTarget())
                        && dragon.getRandom().nextFloat() < (favorRanged ? 0.90F : 0.65F));

        double roarScore = 52.0D
                + Mth.clamp((snapshot.gap() - 16.0D) * 0.75D, 0.0D, 20.0D)
                + (snapshot.targetSpeed() < 0.25D ? 10.0D : 0.0D)
                + extraClusteredEnemies * 10.0D
                + snapshot.facingDot() * 8.0D;
        best = consider(best, CombatAction.ROAR, roarScore,
                dragon.isGroundedForAction()
                        && snapshot.targetGrounded()
                        && snapshot.gap() >= 16.0D
                        && snapshot.gap() <= 48.0D
                        && snapshot.facingDot() >= 0.45D
                        && canUseAiAbility(ModAbilities.IGNIVORUS_ROAR, true));

        return best.action();
    }

    private AbilityChoice consider(AbilityChoice current,
                                   CombatAction action,
                                   double score,
                                   boolean eligible) {
        // Suitability is checked before repetition penalties. Those penalties rank valid
        // attacks; they must not leave a stationary melee target with no selectable attack.
        if (!eligible || score < MIN_ABILITY_SCORE) {
            return current;
        }
        DragonCombatLearning.Attack learnedAttack = switch (action) {
            case FIRE_BREATH -> DragonCombatLearning.Attack.BREATH;
            case FIREBALL -> DragonCombatLearning.Attack.PROJECTILE;
            default -> null;
        };
        if (learnedAttack != null) score *= dragon.getCombatLearning()
                .expectation(dragon.getTarget(), learnedAttack, false).attackWeight();
        if (action == lastAction) {
            score -= 28.0D;
        } else if (action == previousAction) {
            score -= 12.0D;
        }
        score += (dragon.getRandom().nextDouble() - 0.5D) * 4.0D;
        return score > current.score() ? new AbilityChoice(action, score) : current;
    }

    private boolean startSelectedAbility(CombatAction action,
                                         LivingEntity target,
                                         CombatSnapshot snapshot) {
        boolean started = switch (action) {
            case BITE -> {
                int cooldown = dragon.shouldFavorRangedCombat(target) ? 45 : 30;
                yield startStandardAbility(ModAbilities.IGNIVORUS_BITE,
                        false, cooldown, cooldown, 0, 24, "bite");
            }
            case BODY_SLAM -> startStandardAbility(
                    ModAbilities.IGNIVORUS_BODY_SLAM,
                    false, 35, 35, 0, 28, "body-slam");
            case STOMP -> startStandardAbility(
                    ModAbilities.IGNIVORUS_STOMP,
                    false, 35, 35, 0, 28, "stomp");
            case WING_SWIPE -> startStandardAbility(
                    ModAbilities.IGNIVORUS_WING_SWIPE,
                    false, 30, 30, 0, 24, "wing-swipe");
            case FIRE_BREATH -> startFireBreath();
            case FIREBALL -> startFireball(snapshot);
            case ROAR -> startStandardAbility(
                    ModAbilities.IGNIVORUS_ROAR,
                    true, 60, 320, 240, 180, "magma-pillars");
            case LEAP -> startLeap(target, snapshot.gap());
            case NONE -> false;
        };
        if (started && action != CombatAction.NONE) {
            rememberAction(action);
        }
        return started;
    }

    private boolean startStandardAbility(DragonAbilityType<?, ?> abilityType,
                                         boolean majorAbility,
                                         int cadenceTicks,
                                         int abilityCooldownTicks,
                                         int majorCooldownTicks,
                                         int repeatLockoutTicks,
                                         String movementReason) {
        if (!tryUseAbility(
                abilityType,
                majorAbility,
                cadenceTicks,
                abilityCooldownTicks,
                majorCooldownTicks,
                repeatLockoutTicks)) {
            return false;
        }
        stopMovement("ignivorus-combat:" + movementReason);
        attackCooldown = cadenceTicks;
        return true;
    }

    private boolean startFireBreath() {
        if (!tryUseAbility(
                ModAbilities.IGNIVORUS_FIRE_BREATH,
                true,
                12,
                0,
                30,
                0)) {
            return false;
        }
        stopMovement("ignivorus-combat:fire-breath");
        attackCooldown = 12;
        return true;
    }

    private boolean startFireball(CombatSnapshot snapshot) {
        if (!tryUseAbility(ModAbilities.IGNIVORUS_FIREBALL, true, 24, 120, 120, 50)) {
            return false;
        }

        if (snapshot.gap() >= 30.0D && snapshot.targetSpeed() < 0.22D) {
            fireballMode = FireballMode.STATIONARY;
            fireballDesiredLevel = 3;
            stopMovement("ignivorus-combat:stationary-fireball");
        } else {
            fireballMode = FireballMode.MOVING;
            fireballDesiredLevel = snapshot.gap() >= 18.0D && snapshot.closingSpeed() < 0.25D ? 2 : 1;
        }
        attackCooldown = 24;
        fireballDecisionCooldown = FIREBALL_DECISION_COOLDOWN_TICKS;
        return true;
    }

    private boolean startLeap(LivingEntity target, double gap) {
        if (!maybeStartPhase2GapCloseLeap(target, gap)) {
            return false;
        }
        return true;
    }

    private boolean tryUseAbility(DragonAbilityType<?, ?> abilityType,
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

    private void rememberAction(CombatAction action) {
        previousAction = lastAction;
        lastAction = action;
    }

    private boolean maybeStartPhase2GapCloseLeap(LivingEntity target, double gap) {
        if (dragon.isTame()) {
            return false;
        }
        if (!dragon.isPhase2Active()) {
            return false;
        }
        if (gap < AI_PHASE2_LEAP_TRIGGER_GAP) {
            return false;
        }
        if (gap > AI_PHASE2_LEAP_MAX_GAP) {
            return false;
        }
        if (attackCooldown > 0) {
            return false;
        }
        if (isCurrentlyAttacking()) {
            return false;
        }
        if (!dragon.isGroundedForAction()) {
            return false;
        }
        if (!dragon.getSensing().hasLineOfSight(target)) {
            return false;
        }
        if (dragon.getAiCombatPacing().getCadenceCooldownTicks() > 0) {
            return false;
        }
        if (dragon.tryStartLeapSlamForAI(target)) {
            dragon.getAiCombatPacing().setCadenceCooldownMin(AI_PHASE2_LEAP_POST_COOLDOWN);
            attackCooldown = AI_PHASE2_LEAP_POST_COOLDOWN;
            return true;
        }
        return false;
    }

    private boolean shouldTriggerLowHealthUltimate() {
        return dragon.isGroundedForAction()
                && dragon.shouldTriggerWildUltimateAtCurrentHealth();
    }

    private double getGapToTarget(LivingEntity target) {
        double centerDistance = this.dragon.distanceTo(target);
        double combinedRadii = (this.dragon.getBbWidth() + target.getBbWidth()) * 0.5;
        return Math.max(0.0, centerDistance - combinedRadii);
    }

    private double getMaxAggroDistanceSqr() {
        double followRange = this.dragon.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) {
            followRange = Ignivorus.BASE_FOLLOW_RANGE;
        }
        return followRange * followRange;
    }

    private void handleWaterCombat(LivingEntity target, double gap, boolean hasLineOfSight) {
        handleBiteOnlyTarget(target, gap, hasLineOfSight);
    }

    public boolean isGroundMovementLocked() {
        if (dragon == null) {
            return false;
        }
        if (dragon.isWaitingForWildPhase2Entry() || dragon.isSkyfallIdlePause()) return true;
        if (dragon.isAbilityActive(ModAbilities.IGNIVORUS_FIREBALL)) {
            return fireballMode == FireballMode.STATIONARY;
        }
        return isCurrentlyAttacking();
    }

    private void stopMovement(String reason) {
        if (currentContext != null) {
            currentContext.memories().set(DragonMemories.MOVEMENT_INTENT, DragonMovementIntent.stop(reason));
        }
    }

    private enum CombatAction {
        NONE,
        BITE,
        BODY_SLAM,
        FIRE_BREATH,
        ROAR,
        LEAP,
        STOMP,
        WING_SWIPE,
        FIREBALL
    }

    private record AbilityChoice(CombatAction action, double score) {
    }

    private record CombatSnapshot(double gap,
                                  boolean hasLineOfSight,
                                  double closingSpeed,
                                  double targetSpeed,
                                  double facingDot,
                                  int nearbyEnemies,
                                  int targetClusterSize,
                                  boolean targetGrounded,
                                  boolean leapReady) {
    }

    private enum FireballMode {
        NONE,
        STATIONARY,
        MOVING
    }

    private boolean canUseAiAbility(DragonAbilityType<?, ?> abilityType, boolean majorAbility) {
        return dragon.combatManager.canStartAiAbility(abilityType, majorAbility);
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        return Map.of("breath_decision", dragon == null ? "idle" : dragon.getAiFireBreathDecision(),
                "last_action", lastAction.name(),
                "attack_cooldown", Integer.toString(attackCooldown));
    }
}
