package com.leon.saintsdragons.server.ai.goals.ignivorus;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ignivorus.IgnivorusAbilities;
import com.leon.saintsdragons.server.ai.goals.base.DragonAggroLandingHelper;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.ignivorus.IgnivorusFireballAbility;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class IgnivorusGroundCombatGoal extends Goal {
    private final Ignivorus dragon;

    // Combat ranges
    private final double meleeEngageRange = 6.0;      // Chase until this close, then stop and melee
    private final double fireBreathMinRange = 32.0;   // Only use fire breath when target is this far

    private final double chaseSpeed = 1.75D;
    private int attackCooldown = 0;
    private int pathRecalcCooldown = 0;
    private int pathFailureBackoff = 0;
    private double lastTargetX;
    private double lastTargetY;
    private double lastTargetZ;

    // One-time low-health ultimate trigger per combat encounter
    private boolean hasUsedUltimateTrigger = false;

    // Fire breath cooldown mechanic (AI only - 3 minute cooldown)
    private int breathCooldown = 0;
    private static final int BREATH_COOLDOWN_TICKS = 3600; // 3 minutes (60 seconds * 20 ticks * 3)
    private static final float BREATH_RANDOM_CHANCE = 0.12f; // 12% chance per attack window

    // Phase 2 stance switching (AI only)
    private int phase2DecisionCooldown = 0;
    private static final int PHASE2_DECISION_MIN = 60;
    private static final int PHASE2_DECISION_MAX = 120;
    private static final float PHASE2_TOGGLE_ON_CHANCE = 0.85f;
    private static final float PHASE2_TOGGLE_OFF_CHANCE = 0.05f;

    // Fireball AI (Phase 2 stance)
    private int fireballDecisionCooldown = 0;
    private int fireballPostCooldown = 0;
    private FireballMode fireballMode = FireballMode.NONE;
    private int fireballDesiredLevel = 0;
    private static final int FIREBALL_DECISION_MIN = 120;
    private static final int FIREBALL_DECISION_MAX = 200;
    private static final int FIREBALL_POST_COOLDOWN_TICKS = 200;
    private static final float FIREBALL_STATIONARY_CHANCE = 0.12f;
    private static final float FIREBALL_MOVING_L1_CHANCE = 0.12f;
    private static final float FIREBALL_MOVING_L2_CHANCE = 0.08f;
    private static final double FIREBALL_MIN_GAP = 8.0;
    private static final double FIREBALL_MAX_GAP = 48.0;
    private static final double AI_PHASE2_LEAP_TRIGGER_GAP = 20.0;
    private static final int AI_PHASE2_LEAP_POST_COOLDOWN = 30;

    public IgnivorusGroundCombatGoal(Ignivorus dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (dragon.isBaby()) {
            return false;
        }
        LivingEntity target = dragon.getTarget();

        if (!dragon.isTargetValid(target)) {
            return false;
        }

        // Don't attack creative/spectator players
        if (target instanceof net.minecraft.world.entity.player.Player player) {
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
        if (dragon.areRiderControlsLocked() || dragon.isLeaping() || dragon.isLeapImpactRecovering()) {
            return false;
        }

        // Don't use ground combat if target is airborne (let air combat goal handle it)
        if (isTargetAirborne(target)) {
            return false;
        }

        if (dragon.distanceToSqr(target) > getMaxAggroDistanceSqr()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (dragon.isLanding()) {
            return !dragon.onGround();
        }

        if (dragon.isBaby()) {
            return false;
        }
        LivingEntity target = dragon.getTarget();

        if (!dragon.isTargetValid(target)) {
            return false;
        }

        // Stop attacking if player switches to creative/spectator
        if (target instanceof net.minecraft.world.entity.player.Player player) {
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
        if (dragon.areRiderControlsLocked() || dragon.isLeaping() || dragon.isLeapImpactRecovering()) {
            return false;
        }

        // IMPORTANT: If currently breathing fire or using ultimate, don't stop the goal even if target goes out of range
        // This prevents abilities from being cancelled mid-animation when the player runs away
        if (dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH)
            || dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_ULTIMATE)
            || dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_WING_SWIPE)
            || dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_STOMP)
            || dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_FIREBALL)) {
            return true; // Keep goal active to finish the ability
        }

        // Stop ground combat if target becomes airborne (switch to air combat goal)
        if (isTargetAirborne(target)) {
            return false;
        }

        if (dragon.distanceToSqr(target) > getMaxAggroDistanceSqr()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        dragon.getNavigation().stop();
        // Don't modify running state - let other systems handle it
        dragon.setAggressive(false);
        dragon.setGroundMoveStateFromAI(0);
        cancelFireBreathIfActive();
        pathRecalcCooldown = 8;
        pathFailureBackoff = 0;

        // Reset one-time ultimate trigger for next combat encounter
        hasUsedUltimateTrigger = false;
    }

    @Override
    public void start() {
        // Don't set running to avoid speed boost - just use chaseSpeed multiplier
        dragon.setAggressive(true);
        dragon.setGroundMoveStateFromAI(2);

        hasUsedUltimateTrigger = false;

        LivingEntity target = dragon.getTarget();
        if (dragon.isFlying() || dragon.isHovering() || dragon.isTakeoff() || dragon.isLanding()) {
            DragonAggroLandingHelper.beginAggroLanding(dragon, target, 2.0D);
            return;
        }

        dragon.markLandedNow();
        dragon.setHovering(false);
        dragon.setLanding(false);
        dragon.setTakeoff(false);

        if (target != null) {
            dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);
            // Avoid forcing an immediate expensive repath on every goal (re)start.
            pathRecalcCooldown = Math.max(pathRecalcCooldown, 8);
            rememberTargetPosition(target);
        }
    }

    @Override
    public void tick() {
        if (dragon.isLanding()) {
            if (!dragon.getNavigation().isInProgress()) {
                DragonAggroLandingHelper.beginAggroLanding(dragon, dragon.getTarget(), 2.0D);
            }
            return;
        }

        if (dragon.areRiderControlsLocked() || dragon.isLeaping() || dragon.isLeapImpactRecovering()) {
            dragon.getNavigation().stop();
            pathRecalcCooldown = 8;
            updateGroundMoveState();
            return;
        }

        // Keep dragon grounded during combat - prevent flight AI from interfering
        if (dragon.isFlying() || dragon.isHovering() || dragon.isTakeoff()) {
            dragon.markLandedNow();
            dragon.setHovering(false);
            dragon.setLanding(false);
            dragon.setTakeoff(false);
        }

        if (dragon.isAiPhase2Locked()) {
            dragon.getNavigation().stop();
            pathRecalcCooldown = 8;
            updateGroundMoveState();
            return;
        }

        if (attackCooldown > 0) {
            attackCooldown--;
        }

        // Tick down breath cooldown
        if (breathCooldown > 0) {
            breathCooldown--;
        }
        if (phase2DecisionCooldown > 0) {
            phase2DecisionCooldown--;
        }
        if (fireballDecisionCooldown > 0) {
            fireballDecisionCooldown--;
        }
        if (fireballPostCooldown > 0) {
            fireballPostCooldown--;
        }

        LivingEntity target = dragon.getTarget();
        if (!dragon.isTargetValid(target)) {
            // Target died or disappeared - immediately cancel fire breath
            cancelFireBreathIfActive();
            updateGroundMoveState();
            return;
        }

        dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (!hasUsedUltimateTrigger && shouldTriggerLowHealthUltimate()) {
            if (canUseAiAbility(IgnivorusAbilities.IGNIVORUS_ULTIMATE, true)) {
                dragon.combatManager.tryUseAbility(IgnivorusAbilities.IGNIVORUS_ULTIMATE);
                dragon.getAiCombatPacing().recordUse(IgnivorusAbilities.IGNIVORUS_ULTIMATE, 20, 140, true, 180, 100);
            }
            if (dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_ULTIMATE)) {
                hasUsedUltimateTrigger = true;
                attackCooldown = 0;
            }
        }

        double gap = getGapToTarget(target);
        boolean hasLineOfSight = dragon.getSensing().hasLineOfSight(target);

        // Water combat: swim-chase target directly (ground navigation is unreliable in water).
        if (dragon.isInWaterOrBubble()) {
            handleWaterCombatChase(target, gap, hasLineOfSight);
            updateGroundMoveState();
            return;
        }

        maybeTogglePhase2(target, gap, hasLineOfSight);

        if (handleFireballActive(target)) {
            updateGroundMoveState();
            return;
        }

        if (maybeStartPhase2GapCloseLeap(target, gap)) {
            updateGroundMoveState();
            return;
        }

        if (maybeStartFireball(target, gap, hasLineOfSight)) {
            updateGroundMoveState();
            return;
        }

        if (tryRandomBreath(target, hasLineOfSight)) {
            updateGroundMoveState();
            return;
        } else if (gap > meleeEngageRange) {
            // Medium-long range (6-32 blocks) OR breath on cooldown - chase to get closer
            if (!isCurrentlyAttacking()) {
                updateChasePath(target);
            }
        } else {
            // In melee range (0-6 blocks) - stop moving and attack
            dragon.getNavigation().stop();
            pathRecalcCooldown = 8;
            tryAttack(target);
        }

        updateGroundMoveState();
    }

    /**
     * Check if dragon is currently executing an attack ability
     */
    private boolean isCurrentlyAttacking() {
        return dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_BITE)
            || dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_BODY_SLAM)
            || dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_WING_SWIPE)
            || dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_STOMP)
            || dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_FIREBALL)
            || dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH)
            || dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_ULTIMATE)
            || dragon.isLeaping()
            || dragon.isLeapImpactRecovering();
    }

    /**
     * Immediately cancels fire breath if active (e.g., when target dies or switches to creative)
     */
    private void cancelFireBreathIfActive() {
        // Don't interfere if being ridden (let rider control abilities)
        if (dragon.isVehicle()) {
            return;
        }

        if (dragon.isAbilityActive(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH)) {
            dragon.forceEndAbility(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH);
        }
    }

    /**
     * Attack selection: MELEE-FOCUSED with unpredictable randomization.
     * Fire breath ONLY at long range (AI can't aim well).
     * Bite and body slam randomized for unpredictability - NOT range-based.
     */
    private void tryAttack(LivingEntity target) {
        if (attackCooldown > 0 || isCurrentlyAttacking()) {
            return;
        }

        if (!dragon.getSensing().hasLineOfSight(target)) {
            return;
        }

        double gap = getGapToTarget(target);

        if (gap <= meleeEngageRange) {
            // Melee attacks ONLY in melee range (<6 blocks)
            // Phase 2 swaps to wing swipe / stomp instead of bite / body slam
            if (dragon.isPhase2Active()) {
                if (dragon.getRandom().nextBoolean()) {
                    if (!canUseAiAbility(IgnivorusAbilities.IGNIVORUS_STOMP, false)) {
                        return;
                    }
                    dragon.combatManager.tryUseAbility(IgnivorusAbilities.IGNIVORUS_STOMP);
                    dragon.getAiCombatPacing().recordUse(IgnivorusAbilities.IGNIVORUS_STOMP, 35, 35, false, 0, 28);
                    attackCooldown = 35;
                } else {
                    if (!canUseAiAbility(IgnivorusAbilities.IGNIVORUS_WING_SWIPE, false)) {
                        return;
                    }
                    dragon.combatManager.tryUseAbility(IgnivorusAbilities.IGNIVORUS_WING_SWIPE);
                    dragon.getAiCombatPacing().recordUse(IgnivorusAbilities.IGNIVORUS_WING_SWIPE, 30, 30, false, 0, 24);
                    attackCooldown = 30;
                }
            } else {
                // Randomly choose between bite and body slam for unpredictability
                if (dragon.getRandom().nextBoolean()) {
                    if (!canUseAiAbility(IgnivorusAbilities.IGNIVORUS_BODY_SLAM, false)) {
                        return;
                    }
                    dragon.combatManager.tryUseAbility(IgnivorusAbilities.IGNIVORUS_BODY_SLAM);
                    dragon.getAiCombatPacing().recordUse(IgnivorusAbilities.IGNIVORUS_BODY_SLAM, 35, 35, false, 0, 28);
                    attackCooldown = 35; // Moderate cooldown for body slam
                } else {
                    if (!canUseAiAbility(IgnivorusAbilities.IGNIVORUS_BITE, false)) {
                        return;
                    }
                    dragon.combatManager.tryUseAbility(IgnivorusAbilities.IGNIVORUS_BITE);
                    dragon.getAiCombatPacing().recordUse(IgnivorusAbilities.IGNIVORUS_BITE, 30, 30, false, 0, 24);
                    attackCooldown = 30; // Slightly faster cooldown for bite
                }
            }
        }
        // No attack in 6-32 block range - just chase
    }

    private boolean tryRandomBreath(LivingEntity target, boolean hasLineOfSight) {
        if (attackCooldown > 0 || isCurrentlyAttacking()) {
            return false;
        }
        if (!hasLineOfSight || breathCooldown > 0) {
            return false;
        }
        if (dragon.getRandom().nextFloat() >= BREATH_RANDOM_CHANCE) {
            return false;
        }

        dragon.getNavigation().stop();
        pathRecalcCooldown = 8;
        if (!canUseAiAbility(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH, true)) {
            return false;
        }
        dragon.combatManager.tryUseAbility(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH);
        dragon.getAiCombatPacing().recordUse(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH, 60, BREATH_COOLDOWN_TICKS, true, 180, 80);
        attackCooldown = 60;
        breathCooldown = BREATH_COOLDOWN_TICKS;
        return true;
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

        if (fireballMode == FireballMode.STATIONARY) {
            dragon.getNavigation().stop();
            pathRecalcCooldown = 8;
        } else if (fireballMode == FireballMode.MOVING) {
            updateChasePath(target);
        }

        int level = dragon.getFireballChargeLevel();
        if (level >= fireballDesiredLevel && fireballDesiredLevel > 0) {
            fireball.requestRelease();
        }

        return true;
    }

    private boolean maybeStartFireball(LivingEntity target, double gap, boolean hasLineOfSight) {
        if (!dragon.isPhase2Active()) {
            return false;
        }
        if (fireballDecisionCooldown > 0 || fireballPostCooldown > 0) {
            return false;
        }
        if (isCurrentlyAttacking()) {
            return false;
        }
        if (!hasLineOfSight) {
            return false;
        }
        if (gap < FIREBALL_MIN_GAP || gap > FIREBALL_MAX_GAP) {
            return false;
        }

        float roll = dragon.getRandom().nextFloat();

        if (!dragon.getNavigation().isInProgress() && roll < FIREBALL_STATIONARY_CHANCE) {
            if (canUseAiAbility(IgnivorusAbilities.IGNIVORUS_FIREBALL, true)) {
                dragon.combatManager.tryUseAbility(IgnivorusAbilities.IGNIVORUS_FIREBALL);
                dragon.getAiCombatPacing().recordUse(IgnivorusAbilities.IGNIVORUS_FIREBALL, 24, 120, true, 120, 50);
                fireballMode = FireballMode.STATIONARY;
                fireballDesiredLevel = 3;
                fireballDecisionCooldown = nextFireballDecisionCooldown();
                return true;
            }
        }

        if (roll < FIREBALL_STATIONARY_CHANCE + FIREBALL_MOVING_L2_CHANCE) {
            if (canUseAiAbility(IgnivorusAbilities.IGNIVORUS_FIREBALL, true)) {
                dragon.combatManager.tryUseAbility(IgnivorusAbilities.IGNIVORUS_FIREBALL);
                dragon.getAiCombatPacing().recordUse(IgnivorusAbilities.IGNIVORUS_FIREBALL, 24, 120, true, 120, 50);
                fireballMode = FireballMode.MOVING;
                fireballDesiredLevel = 2;
                fireballDecisionCooldown = nextFireballDecisionCooldown();
                return true;
            }
        }

        if (roll < FIREBALL_STATIONARY_CHANCE + FIREBALL_MOVING_L2_CHANCE + FIREBALL_MOVING_L1_CHANCE) {
            if (canUseAiAbility(IgnivorusAbilities.IGNIVORUS_FIREBALL, true)) {
                dragon.combatManager.tryUseAbility(IgnivorusAbilities.IGNIVORUS_FIREBALL);
                dragon.getAiCombatPacing().recordUse(IgnivorusAbilities.IGNIVORUS_FIREBALL, 24, 120, true, 120, 50);
                fireballMode = FireballMode.MOVING;
                fireballDesiredLevel = 1;
                fireballDecisionCooldown = nextFireballDecisionCooldown();
                return true;
            }
        }

        fireballDecisionCooldown = nextFireballDecisionCooldown();
        return false;
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
        if (attackCooldown > 0) {
            return false;
        }
        if (isCurrentlyAttacking()) {
            return false;
        }
        if (!dragon.onGround()) {
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

    private int nextFireballDecisionCooldown() {
        return FIREBALL_DECISION_MIN
            + dragon.getRandom().nextInt(FIREBALL_DECISION_MAX - FIREBALL_DECISION_MIN + 1);
    }

    private void maybeTogglePhase2(LivingEntity target, double gap, boolean hasLineOfSight) {
        if (phase2DecisionCooldown > 0) {
            return;
        }
        if (dragon.isVehicle() || dragon.getControllingPassenger() != null) {
            return;
        }
        if (!dragon.onGround()) {
            return;
        }
        if (isCurrentlyAttacking()) {
            return;
        }
        if (!hasLineOfSight) {
            return;
        }
        if (gap > meleeEngageRange) {
            return;
        }

        boolean enable = !dragon.isPhase2Active();
        float chance = enable ? getPhase2ToggleOnChance() : getPhase2ToggleOffChance();
        if (dragon.getRandom().nextFloat() < chance) {
            if (!dragon.tryTogglePhase2ForAI(enable)) {
                phase2DecisionCooldown = 80;
                return;
            }
        }

        int minTicks = getPhase2DecisionMinTicks();
        int maxTicks = getPhase2DecisionMaxTicks();
        phase2DecisionCooldown = minTicks
            + dragon.getRandom().nextInt(maxTicks - minTicks + 1);
    }

    private float getPhase2ToggleOnChance() {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
            .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
        return Mth.clamp((float) config.extraDouble("phase2_toggle_on_chance", PHASE2_TOGGLE_ON_CHANCE), 0.0F, 1.0F);
    }

    private float getPhase2ToggleOffChance() {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
            .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
        return Mth.clamp((float) config.extraDouble("phase2_toggle_off_chance", PHASE2_TOGGLE_OFF_CHANCE), 0.0F, 1.0F);
    }

    private int getPhase2DecisionMinTicks() {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
            .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
        return Math.max(1, Mth.floor(config.extraDouble("phase2_decision_min_ticks", PHASE2_DECISION_MIN)));
    }

    private int getPhase2DecisionMaxTicks() {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
            .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
        int minTicks = getPhase2DecisionMinTicks();
        int maxTicks = Math.max(1, Mth.floor(config.extraDouble("phase2_decision_max_ticks", PHASE2_DECISION_MAX)));
        return Math.max(minTicks, maxTicks);
    }

    private boolean shouldTriggerLowHealthUltimate() {
        if (dragon.isTame() && dragon.getOwner() != null) {
            return false;
        }
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
            .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
        double healthFraction = Mth.clamp(
            config.extraDouble("ultimate_trigger_health_fraction", 0.5D),
            0.0D,
            1.0D
        );
        if (healthFraction <= 0.0D) {
            return false;
        }
        return dragon.getHealth() <= dragon.getMaxHealth() * healthFraction;
    }

    /**
     * Get the gap between entity edges (not centers)
     */
    private double getGapToTarget(LivingEntity target) {
        double centerDistance = this.dragon.distanceTo(target);
        double combinedRadii = (this.dragon.getBbWidth() + target.getBbWidth()) * 0.5;
        return Math.max(0.0, centerDistance - combinedRadii);
    }

    private double getMaxAggroDistanceSqr() {
        double followRange = this.dragon.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) {
            followRange = 32.0D;
        }
        return followRange * followRange;
    }

    private void updateChasePath(LivingEntity target) {
        if (pathFailureBackoff > 0) {
            pathFailureBackoff--;
            return;
        }

        if (pathRecalcCooldown > 0) {
            pathRecalcCooldown--;
        }

        boolean phase2 = dragon.isPhase2Active();
        boolean targetMoved = targetMovedSignificantly(target);
        boolean shouldRepath = pathRecalcCooldown <= 0
            || (!phase2 && targetMoved && !dragon.getNavigation().isInProgress());

        if (shouldRepath) {
            rememberTargetPosition(target);
            double distance = dragon.distanceTo(target);
            int baseCooldown = phase2
                ? Mth.clamp((int) (distance * 0.7D), 10, 30)
                : Mth.clamp((int) (distance * 0.6D), 5, 20);
            boolean started = dragon.getNavigation().moveTo(target, chaseSpeed);

            if (started) {
                pathRecalcCooldown = baseCooldown;
                pathFailureBackoff = 0;
            } else {
                // Back off after failed path attempts to prevent pathfinding storms.
                pathRecalcCooldown = Math.max(baseCooldown, 20);
                pathFailureBackoff = 20 + dragon.getRandom().nextInt(21);
            }
        }
    }

    private void rememberTargetPosition(LivingEntity target) {
        this.lastTargetX = target.getX();
        this.lastTargetY = target.getY();
        this.lastTargetZ = target.getZ();
    }

    private boolean targetMovedSignificantly(LivingEntity target) {
        double dx = target.getX() - this.lastTargetX;
        double dy = target.getY() - this.lastTargetY;
        double dz = target.getZ() - this.lastTargetZ;
        return dx * dx + dy * dy + dz * dz > 4.0D;
    }

    private void updateGroundMoveState() {
        if (!dragon.isFlying() && dragon.onGround() && dragon.getNavigation().isInProgress()) {
            dragon.setGroundMoveStateFromAI(2);
        } else {
            dragon.setGroundMoveStateFromAI(0);
        }
    }

    private void handleWaterCombatChase(LivingEntity target, double gap, boolean hasLineOfSight) {
        dragon.getNavigation().stop();
        pathRecalcCooldown = 8;

        Vec3 current = dragon.getDeltaMovement();
        Vec3 toTarget = target.position().subtract(dragon.position());
        Vec3 horizontal = new Vec3(toTarget.x, 0.0, toTarget.z);
        Vec3 desiredHorizontal = horizontal.lengthSqr() > 1.0E-4
                ? horizontal.normalize().scale(0.34D)
                : Vec3.ZERO;

        double nx = current.x + (desiredHorizontal.x - current.x) * 0.30D;
        double nz = current.z + (desiredHorizontal.z - current.z) * 0.30D;

        double targetY = target.getY() + (target.getBbHeight() * 0.5D);
        double yDiff = targetY - dragon.getY();
        double ny = current.y;
        if (dragon.horizontalCollision) {
            // Shore lip / bank collision: force a stronger upward pop for this large body.
            ny = Math.max(current.y + 0.16D, 0.42D);
        } else if (yDiff > 0.25D) {
            // Start climbing earlier when the target is even slightly above waterline.
            ny = Math.max(current.y + 0.09D, 0.14D);
        } else if (yDiff < -1.1D) {
            ny = Math.min(current.y - 0.05D, -0.11D);
        } else {
            ny = current.y + 0.03D;
        }

        dragon.setDeltaMovement(nx, ny, nz);
        dragon.getMoveControl().setWantedPosition(target.getX(), targetY, target.getZ(), 1.2D);

        if (!isCurrentlyAttacking() && hasLineOfSight && gap <= meleeEngageRange) {
            tryAttack(target);
        } else if (!isCurrentlyAttacking()) {
            tryRandomBreath(target, hasLineOfSight);
        }
    }

    private enum FireballMode {
        NONE,
        STATIONARY,
        MOVING
    }

    /**
     * Check if target is airborne (flying, riding flying mount, or off ground)
     */
    private boolean isTargetAirborne(LivingEntity target) {
        // Check if target is on ground
        if (target.onGround()) {
            return false;
        }

        // Check if riding something (might be a flying dragon)
        if (target.isPassenger() && target.getVehicle() != null) {
            return true; // Assume mounted targets are valid air targets
        }

        // Check if significantly off ground (more than 8 blocks up for elytra/flight stability)
        // Increased from 3 to prevent low elytra gliding from triggering constant takeoff
        double groundY = dragon.level().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, target.blockPosition()).getY();
        if (target.getY() - groundY > 8.0) {
            return true;
        }

        return false;
    }

    private boolean canUseAiAbility(com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> abilityType, boolean majorAbility) {
        return dragon.combatManager.canStart(abilityType) && dragon.getAiCombatPacing().canUse(abilityType, majorAbility);
    }

}
