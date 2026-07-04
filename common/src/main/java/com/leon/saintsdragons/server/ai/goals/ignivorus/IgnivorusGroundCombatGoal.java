package com.leon.saintsdragons.server.ai.goals.ignivorus;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.goals.base.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.abilities.ignivorus.IgnivorusFireballAbility;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class IgnivorusGroundCombatGoal extends Goal {
    private final Ignivorus dragon;
    private final double meleeEngageRange = 6.0;
    private final double fireBreathMinRange = 32.0;
    private final double chaseSpeed = 1.75D;
    private int attackCooldown = 0;
    private int pathRecalcCooldown = 0;
    private int pathFailureBackoff = 0;
    private double lastTargetX;
    private double lastTargetY;
    private double lastTargetZ;
    private boolean hasUsedUltimateTrigger = false;
    private int breathCooldown = 0;
    private static final int BREATH_COOLDOWN_TICKS = 3600;
    private static final float BREATH_RANDOM_CHANCE = 0.12f;
    private int phase2DecisionCooldown = 0;
    private static final int PHASE2_DECISION_MIN = 60;
    private static final int PHASE2_DECISION_MAX = 120;
    private static final float PHASE2_TOGGLE_ON_CHANCE = 0.85f;
    private static final float PHASE2_TOGGLE_OFF_CHANCE = 0.05f;
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
        if (dragon.areRiderControlsLocked() || dragon.isLeaping() || dragon.isLeapImpactRecovering()) {
            return false;
        }


        if (dragon.isAbilityActive(ModAbilities.IGNIVORUS_FIRE_BREATH)
            || dragon.isAbilityActive(ModAbilities.IGNIVORUS_ULTIMATE)
            || dragon.isAbilityActive(ModAbilities.IGNIVORUS_WING_SWIPE)
            || dragon.isAbilityActive(ModAbilities.IGNIVORUS_STOMP)
            || dragon.isAbilityActive(ModAbilities.IGNIVORUS_FIREBALL)) {
            return true;
        }

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
        dragon.getAIMovement().stop();
        dragon.setAggressive(false);
        cancelFireBreathIfActive();
        pathRecalcCooldown = 8;
        pathFailureBackoff = 0;
        hasUsedUltimateTrigger = false;
    }

    @Override
    public void start() {
        dragon.setAggressive(true);
        dragon.getAIMovement().setGroundRun();
        hasUsedUltimateTrigger = false;
        LivingEntity target = dragon.getTarget();
        if (dragon.isFlying() || dragon.isHovering() || dragon.isTakeoff() || dragon.isLanding()) {
            dragon.getAIMovement().setLandingWaypoint(target, 1.5D);
            return;
        }

        dragon.markLandedNow();
        dragon.setHovering(false);
        dragon.setLanding(false);
        dragon.setTakeoff(false);

        if (target != null) {
            dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);
            pathRecalcCooldown = Math.max(pathRecalcCooldown, 8);
            rememberTargetPosition(target);
        }
    }

    @Override
    public void tick() {
        if (dragon.isLanding()) {
            if (!dragon.getAIMovement().isPathing()) {
                dragon.getAIMovement().setLandingWaypoint(dragon.getTarget(), 1.5D);
            }
            return;
        }

        if (dragon.areRiderControlsLocked() || dragon.isLeaping() || dragon.isLeapImpactRecovering()) {
            dragon.getAIMovement().stop();
            pathRecalcCooldown = 8;
            updateGroundMoveState();
            return;
        }

        if (dragon.isFlying() || dragon.isHovering() || dragon.isTakeoff()) {
            dragon.markLandedNow();
            dragon.setHovering(false);
            dragon.setLanding(false);
            dragon.setTakeoff(false);
        }

        if (dragon.isAiPhase2Locked()) {
            dragon.getAIMovement().stop();
            pathRecalcCooldown = 8;
            updateGroundMoveState();
            return;
        }

        if (attackCooldown > 0) {
            attackCooldown--;
        }

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
            cancelFireBreathIfActive();
            updateGroundMoveState();
            return;
        }

        dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);

        boolean biteOnlyPrey = DragonTargetingHelper.isBiteOnlyPreyTarget(target);

        if (!biteOnlyPrey && !hasUsedUltimateTrigger && shouldTriggerLowHealthUltimate()) {
            if (canUseAiAbility(ModAbilities.IGNIVORUS_ULTIMATE, true)) {
                dragon.combatManager.tryUseAbility(ModAbilities.IGNIVORUS_ULTIMATE);
                dragon.getAiCombatPacing().recordUse(ModAbilities.IGNIVORUS_ULTIMATE, 20, 140, true, 180, 100);
            }
            if (dragon.isAbilityActive(ModAbilities.IGNIVORUS_ULTIMATE)) {
                hasUsedUltimateTrigger = true;
                attackCooldown = 0;
            }
        }

        double gap = getGapToTarget(target);
        boolean hasLineOfSight = dragon.getSensing().hasLineOfSight(target);

        if (dragon.isInWaterOrBubble()) {
            handleWaterCombatChase(target, gap, hasLineOfSight);
            updateGroundMoveState();
            return;
        }

        if (!biteOnlyPrey) {
            maybeTogglePhase2(target, gap, hasLineOfSight);
        }

        if (!biteOnlyPrey && handleFireballActive(target)) {
            updateGroundMoveState();
            return;
        }

        if (!biteOnlyPrey && maybeStartPhase2GapCloseLeap(target, gap)) {
            updateGroundMoveState();
            return;
        }

        if (!biteOnlyPrey && maybeStartFireball(target, gap, hasLineOfSight)) {
            updateGroundMoveState();
            return;
        }

        if (!biteOnlyPrey && tryRandomBreath(target, hasLineOfSight)) {
            updateGroundMoveState();
            return;
        } else if (gap > meleeEngageRange) {
            if (!isCurrentlyAttacking()) {
                updateChasePath(target);
            }
        } else {
            dragon.getAIMovement().stop();
            pathRecalcCooldown = 8;
            tryAttack(target);
        }

        updateGroundMoveState();
    }

    private boolean isCurrentlyAttacking() {
        return dragon.isAbilityActive(ModAbilities.IGNIVORUS_BITE)
            || dragon.isAbilityActive(ModAbilities.IGNIVORUS_BODY_SLAM)
            || dragon.isAbilityActive(ModAbilities.IGNIVORUS_WING_SWIPE)
            || dragon.isAbilityActive(ModAbilities.IGNIVORUS_STOMP)
            || dragon.isAbilityActive(ModAbilities.IGNIVORUS_FIREBALL)
            || dragon.isAbilityActive(ModAbilities.IGNIVORUS_FIRE_BREATH)
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

    private void tryAttack(LivingEntity target) {
        if (attackCooldown > 0 || isCurrentlyAttacking()) {
            return;
        }

        if (!dragon.getSensing().hasLineOfSight(target)) {
            return;
        }

        double gap = getGapToTarget(target);

        if (gap <= meleeEngageRange) {
            if (DragonTargetingHelper.isBiteOnlyPreyTarget(target)) {
                if (!canUseAiAbility(ModAbilities.IGNIVORUS_BITE, false)) {
                    return;
                }
                dragon.combatManager.tryUseAbility(ModAbilities.IGNIVORUS_BITE);
                dragon.getAiCombatPacing().recordUse(ModAbilities.IGNIVORUS_BITE, 30, 30, false, 0, 24);
                attackCooldown = 30;
                return;
            }
            if (dragon.isPhase2Active()) {
                if (dragon.getRandom().nextBoolean()) {
                    if (!canUseAiAbility(ModAbilities.IGNIVORUS_STOMP, false)) {
                        return;
                    }
                    dragon.combatManager.tryUseAbility(ModAbilities.IGNIVORUS_STOMP);
                    dragon.getAiCombatPacing().recordUse(ModAbilities.IGNIVORUS_STOMP, 35, 35, false, 0, 28);
                    attackCooldown = 35;
                } else {
                    if (!canUseAiAbility(ModAbilities.IGNIVORUS_WING_SWIPE, false)) {
                        return;
                    }
                    dragon.combatManager.tryUseAbility(ModAbilities.IGNIVORUS_WING_SWIPE);
                    dragon.getAiCombatPacing().recordUse(ModAbilities.IGNIVORUS_WING_SWIPE, 30, 30, false, 0, 24);
                    attackCooldown = 30;
                }
            } else {
                if (dragon.getRandom().nextBoolean()) {
                    if (!canUseAiAbility(ModAbilities.IGNIVORUS_BODY_SLAM, false)) {
                        return;
                    }
                    dragon.combatManager.tryUseAbility(ModAbilities.IGNIVORUS_BODY_SLAM);
                    dragon.getAiCombatPacing().recordUse(ModAbilities.IGNIVORUS_BODY_SLAM, 35, 35, false, 0, 28);
                    attackCooldown = 35;
                } else {
                    if (!canUseAiAbility(ModAbilities.IGNIVORUS_BITE, false)) {
                        return;
                    }
                    dragon.combatManager.tryUseAbility(ModAbilities.IGNIVORUS_BITE);
                    dragon.getAiCombatPacing().recordUse(ModAbilities.IGNIVORUS_BITE, 30, 30, false, 0, 24);
                    attackCooldown = 30;
                }
            }
        }
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

        dragon.getAIMovement().stop();
        pathRecalcCooldown = 8;
        if (!canUseAiAbility(ModAbilities.IGNIVORUS_FIRE_BREATH, true)) {
            return false;
        }
        dragon.combatManager.tryUseAbility(ModAbilities.IGNIVORUS_FIRE_BREATH);
        dragon.getAiCombatPacing().recordUse(ModAbilities.IGNIVORUS_FIRE_BREATH, 60, BREATH_COOLDOWN_TICKS, true, 180, 80);
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
            dragon.getAIMovement().stop();
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

        if (!dragon.getAIMovement().isPathing() && roll < FIREBALL_STATIONARY_CHANCE) {
            if (canUseAiAbility(ModAbilities.IGNIVORUS_FIREBALL, true)) {
                dragon.combatManager.tryUseAbility(ModAbilities.IGNIVORUS_FIREBALL);
                dragon.getAiCombatPacing().recordUse(ModAbilities.IGNIVORUS_FIREBALL, 24, 120, true, 120, 50);
                fireballMode = FireballMode.STATIONARY;
                fireballDesiredLevel = 3;
                fireballDecisionCooldown = nextFireballDecisionCooldown();
                return true;
            }
        }

        if (roll < FIREBALL_STATIONARY_CHANCE + FIREBALL_MOVING_L2_CHANCE) {
            if (canUseAiAbility(ModAbilities.IGNIVORUS_FIREBALL, true)) {
                dragon.combatManager.tryUseAbility(ModAbilities.IGNIVORUS_FIREBALL);
                dragon.getAiCombatPacing().recordUse(ModAbilities.IGNIVORUS_FIREBALL, 24, 120, true, 120, 50);
                fireballMode = FireballMode.MOVING;
                fireballDesiredLevel = 2;
                fireballDecisionCooldown = nextFireballDecisionCooldown();
                return true;
            }
        }

        if (roll < FIREBALL_STATIONARY_CHANCE + FIREBALL_MOVING_L2_CHANCE + FIREBALL_MOVING_L1_CHANCE) {
            if (canUseAiAbility(ModAbilities.IGNIVORUS_FIREBALL, true)) {
                dragon.combatManager.tryUseAbility(ModAbilities.IGNIVORUS_FIREBALL);
                dragon.getAiCombatPacing().recordUse(ModAbilities.IGNIVORUS_FIREBALL, 24, 120, true, 120, 50);
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
            || (!phase2 && targetMoved && !dragon.getAIMovement().isPathing());

        if (shouldRepath) {
            rememberTargetPosition(target);
            double distance = dragon.distanceTo(target);
            int baseCooldown = phase2
                ? Mth.clamp((int) (distance * 0.7D), 10, 30)
                : Mth.clamp((int) (distance * 0.6D), 5, 20);
            boolean started = dragon.getAIMovement().moveToGroundTarget(target, chaseSpeed, true);

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
        if (!dragon.isFlying() && dragon.onGround() && dragon.getAIMovement().isPathing()) {
            dragon.getAIMovement().setGroundRun();
        } else {
            dragon.getAIMovement().setGroundIdle();
        }
    }

    private void handleWaterCombatChase(LivingEntity target, double gap, boolean hasLineOfSight) {
        dragon.getAIMovement().stop();
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
            ny = Math.max(current.y + 0.16D, 0.42D);
        } else if (yDiff > 0.25D) {
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

    private boolean isTargetAirborne(LivingEntity target) {
        return DragonTargetingHelper.isTargetAirborne(target, 8.0D);
    }

    private boolean canUseAiAbility(DragonAbilityType<?, ?> abilityType, boolean majorAbility) {
        return dragon.combatManager.canStart(abilityType) && dragon.getAiCombatPacing().canUse(abilityType, majorAbility);
    }
}
