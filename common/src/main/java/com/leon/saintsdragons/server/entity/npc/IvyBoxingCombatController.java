package com.leon.saintsdragons.server.entity.npc;

import com.leon.saintsdragons.server.entity.effect.volitans.ArrowOfVenomEntity;
import com.leon.saintsdragons.util.animation.AnimationHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class IvyBoxingCombatController {
    private static final RawAnimation ORTHODOX_IDLE = RawAnimation.begin().thenLoop("ivy_oleander.animation.orthodox_idle");
    private static final RawAnimation ORTHODOX_WALK = RawAnimation.begin().thenLoop("ivy_oleander.animation.orthodox_walk");
    private static final RawAnimation ORTHODOX_WALK_BACKWARDS = RawAnimation.begin().thenLoop("ivy_oleander.animation.orthodox_walk_backwards");
    private static final RawAnimation ORTHODOX_FAST_WALK = RawAnimation.begin().thenLoop("ivy_oleander.animation.orthodox_fast_walk");
    private static final RawAnimation ORTHODOX_FAST_WALK_BACKWARDS = RawAnimation.begin().thenLoop("ivy_oleander.animation.orthodox_fast_walk_backwards");

    private static final int STANCE_TRANSITION_TICKS = 10;
    private static final int EXIT_STANCE_TICKS = 10;
    private static final int TAUNT_ACTION_TICKS = 36;
    private static final int JAB_ACTION_TICKS = 10;
    private static final int HOOK_ACTION_TICKS = 13;
    private static final int DODGE_ACTION_TICKS = 9;
    private static final int LIVER_COUNTER_ACTION_TICKS = 13;
    private static final int LEFT_JAB_RIGHT_CROSS_ACTION_TICKS = 20;
    private static final int JAB_JAB_HOOK_ACTION_TICKS = 25;
    private static final int RIGHT_HOOK_UPPERCUT_ACTION_TICKS = 25;
    private static final int DASH_FORWARD_RIGHT_CROSS_ACTION_TICKS = 20;
    private static final int THROW_PROJECTILES_ACTION_TICKS = 26;
    private static final int RETREAT_RECOVERY_ACTION_TICKS = 47;
    private static final int RETREAT_RECOVERY_CONSUME_TICKS = 42;
    private static final int RETREAT_RECOVERY_MAX_RETREAT_TICKS = 28;
    private static final int RETREAT_RECOVERY_BACKSTEP_INTERVAL_TICKS = 10;
    private static final int JAB_IMPACT_TICKS = 4;
    private static final int HOOK_IMPACT_TICKS = 5;
    private static final int LIVER_COUNTER_IMPACT_TICKS = 9;
    private static final int LEFT_JAB_RIGHT_CROSS_FIRST_IMPACT_TICKS = 5;
    private static final int LEFT_JAB_RIGHT_CROSS_SECOND_IMPACT_TICKS = 13;
    private static final int LEFT_JAB_RIGHT_CROSS_RETREAT_TICKS = 15;
    private static final int JAB_JAB_HOOK_FIRST_IMPACT_TICKS = 5;
    private static final int JAB_JAB_HOOK_SECOND_IMPACT_TICKS = 11;
    private static final int JAB_JAB_HOOK_THIRD_IMPACT_TICKS = 19;
    private static final int RIGHT_HOOK_UPPERCUT_FIRST_IMPACT_TICKS = 8;
    private static final int RIGHT_HOOK_UPPERCUT_SECOND_IMPACT_TICKS = 16;
    private static final int DASH_FORWARD_RIGHT_CROSS_NUDGE_TICKS = 3;
    private static final int DASH_FORWARD_RIGHT_CROSS_IMPACT_TICKS = 8;
    private static final int THROW_PROJECTILES_FIRST_THROW_TICKS = 6;
    private static final int THROW_PROJECTILES_SECOND_THROW_TICKS = 14;
    private static final int THROW_PROJECTILES_DASH_TICKS = 19;
    private static final int JAB_COOLDOWN_TICKS = 5;
    private static final int HOOK_COOLDOWN_TICKS = 15;
    private static final int COMBO_COOLDOWN_TICKS = 30;
    private static final int THROW_PROJECTILES_COOLDOWN_TICKS = 100;
    private static final int RETREAT_RECOVERY_COOLDOWN_TICKS = 120;
    private static final int DODGE_COOLDOWN_TICKS = 20;
    private static final float REACTIVE_DODGE_CHANCE = 0.65F;
    private static final float REACTIVE_CRIT_DODGE_CHANCE = 0.92F;
    private static final float REACTIVE_NON_PLAYER_DODGE_CHANCE = 1.0F;
    private static final double ATTACK_RANGE = 2.45D;
    private static final double COUNTER_DODGE_RANGE = 3.25D;
    private static final double HOOK_RANGE = 2.15D;
    private static final double COMBO_MIN_RANGE = 1.65D;
    private static final int HOOK_UPPERCUT_CLOSE_CHANCE = 40;
    private static final double KEEP_DISTANCE = 2.15D;
    private static final double APPROACH_DISTANCE = 3.8D;
    private static final double TARGET_STILL_EPSILON_SQ = 0.0025D;
    private static final int TARGET_STILL_PRESSURE_TICKS = 14;
    private static final int TARGET_MOVING_PRESSURE_TICKS = 10;
    private static final int MOVING_PRESSURE_COMMIT_COOLDOWN_TICKS = 18;
    private static final int DASH_CROSS_COOLDOWN_TICKS = 45;
    private static final int DASH_CROSS_RETREAT_CHANCE = 65;
    private static final double DASH_CROSS_MIN_RANGE = 2.35D;
    private static final double DASH_CROSS_MAX_RANGE = 4.65D;
    private static final double THROW_PROJECTILES_MIN_RANGE = 5.0D;
    private static final double THROW_PROJECTILES_MAX_RANGE = 16.0D;
    private static final double RETREAT_RECOVERY_SAFE_DISTANCE = 5.25D;
    private static final double RETREAT_RECOVERY_FORCED_DISTANCE = 4.0D;
    private static final double THROW_PROJECTILES_DASH_STRENGTH = 0.82D;
    private static final float THROW_PROJECTILES_SPEED = 1.55F;
    private static final float THROW_PROJECTILES_INACCURACY = 2.0F;
    private static final double THROW_PROJECTILES_PREDICT_TICKS = 4.0D;
    private static final int THROW_PROJECTILES_RANGE_PUNISH_TICKS = 35;
    private static final double SKIRMISH_CLOSING_TOLERANCE = 0.2D;
    private static final double RETREAT_DOT_THRESHOLD = 0.45D;
    private static final double RETREAT_DISTANCE_INCREASE_SQ = 0.015D;
    private static final double INTERCEPT_PREDICTION_TICKS = 5.0D;
    private static final float LOCK_LOOK_YAW_SPEED = 45.0F;
    private static final float LOCK_LOOK_PITCH_SPEED = 35.0F;
    private static final float LOCK_BODY_YAW_SPEED = 0.45F;
    private static final float LOCK_BODY_YAW_MAX_DELTA = 28.0F;

    private final IvyTheDragonMerchant ivy;
    private int attackCooldown;
    private int dashCrossCooldown;
    private int throwProjectilesCooldown;
    private int retreatRecoveryCooldown;
    private int throwProjectilesRangePunishTicks;
    private int dodgeCooldown;
    private int hookCooldown;
    private int comboCooldown;
    private int exitTicks;
    private boolean exitAfterTaunt;
    private int impactTicks;
    private int secondImpactTicks;
    private int thirdImpactTicks;
    private int projectileTicks;
    private int secondProjectileTicks;
    private int projectileDashTicks;
    private int approachNudgeTicks;
    private int comboRetreatTicks;
    private int recoveryConsumeTicks;
    private int pendingRecoveryAction;
    private int recoveryRetreatTicks;
    private int recoveryBackstepCooldown;
    private boolean recoveryWaitingForLanding;
    private int impactTargetId = -1;
    private AttackType pendingAttack = AttackType.LEFT_JAB;
    private CounterType pendingCounter = null;
    @Nullable
    private LivingEntity lastCombatTarget;
    @Nullable
    private Pillager committedPillagerTarget;
    private CombatState state = CombatState.RECOVERING;
    private int stateTicks;
    private int lastTargetId = -1;
    private double lastTargetX;
    private double lastTargetZ;
    private double targetMoveX;
    private double targetMoveZ;
    private double previousTargetDistanceSqr;
    private int targetStillTicks;
    private int targetMovingTicks;
    private int movingPressureCommitCooldown;

    public IvyBoxingCombatController(IvyTheDragonMerchant ivy) {
        this.ivy = ivy;
    }

    public Goal createGoal() {
        return new BoxingGoal();
    }

    public void setupMovementController(AnimationController<IvyTheDragonMerchant> controller) {
        controller.triggerableAnim("to_orthodox",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.to_orthodox"));
        controller.triggerableAnim("exit_orthodox",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.exit_orthodox"));
        controller.triggerableAnim("orthodox_taunt",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.orthodox_taunt"));
        controller.triggerableAnim("orthodox_left_jab",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.orthodox_left_jab"));
        controller.triggerableAnim("orthodox_right_hook",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.orthodox_right_hook"));
        controller.triggerableAnim("orthodox_left_jab_right_cross",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.orthodox_left_jab_right_cross"));
        controller.triggerableAnim("orthodox_jab_jab_hook",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.orthodox_jab_jab_hook"));
        controller.triggerableAnim("orthodox_right_hook_uppercut",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.orthodox_right_hook_uppercut"));
        controller.triggerableAnim("orthodox_dash_forward_right_cross",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.orthodox_dash_forward_right_cross"));
        controller.triggerableAnim("orthodox_throw_projectiles",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.orthodox_throw_projectiles"));
        controller.triggerableAnim("orthodox_retreat_to_drink",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.orthodox_retreat_to_drink"));
        controller.triggerableAnim("orthodox_retreat_to_eat",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.orthodox_retreat_to_eat"));
        controller.triggerableAnim("dodge_backwards",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.dodge_backwards"));
        controller.triggerableAnim("dodge_left",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.dodge_left"));
        controller.triggerableAnim("dodge_right",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.dodge_right"));
        controller.triggerableAnim("dodge_left_liver_shot",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.dodge_left_liver_shot"));
        controller.triggerableAnim("dodge_right_liver_shot",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.dodge_right_liver_shot"));
    }

    public <T extends GeoEntity> boolean applyMovementAnimation(AnimationState<T> state) {
        if (!isActive()) {
            return false;
        }
        if (ivy.isBoxingExiting() || ivy.isBoxingTaunting()) {
            return true;
        }
        if (ivy.getBoxingActionTicks() > 0) {
            return true;
        }
        if (ivy.isBoxingBackingUp()) {
            AnimationHelper.setAndContinue(state, ivy.isBoxingFast() ? ORTHODOX_FAST_WALK_BACKWARDS : ORTHODOX_WALK_BACKWARDS);
        } else if (state.isMoving()) {
            AnimationHelper.setAndContinue(state, ivy.isBoxingFast() ? ORTHODOX_FAST_WALK : ORTHODOX_WALK);
        } else {
            AnimationHelper.setAndContinue(state, ORTHODOX_IDLE);
        }
        return true;
    }

    public boolean isActive() {
        return ivy.isBoxingStance() || ivy.isBoxingExiting() || ivy.isBoxingTaunting();
    }

    public boolean shouldHoldGroundAgainstKnockback() {
        return isActive()
                && ivy.getBoxingActionTicks() > 0
                && (state == CombatState.ATTACKING || state == CombatState.DODGING);
    }

    public void onHurt(@NotNull DamageSource source, boolean wasHurt) {
        if (!wasHurt || ivy.level().isClientSide || !(source.getEntity() instanceof LivingEntity attacker) || attacker == ivy) {
            return;
        }
        LivingEntity target = resolveReactiveTarget(attacker);
        ivy.setTarget(target);
        if (!(target instanceof Player)) {
            throwProjectilesRangePunishTicks = THROW_PROJECTILES_RANGE_PUNISH_TICKS;
        }
        if (ivy.isBoxingTaunting()) {
            interruptTaunt(target);
            return;
        }
        beginStance();
    }

    public boolean tryDodgeOnHit(@NotNull DamageSource source, float amount) {
        if (amount <= 0.0F || ivy.level().isClientSide || !(source.getEntity() instanceof LivingEntity attacker) || attacker == ivy) {
            return false;
        }
        if (ivy.isBoxingRecovering()) {
            return false;
        }
        if (!attacker.isAlive()) {
            return false;
        }
        LivingEntity target = resolveReactiveTarget(attacker);
        if (ivy.isBoxingTaunting()) {
            ivy.setTarget(target);
            interruptTaunt(target);
            return false;
        }

        if (isLikelyPlayerCritical(attacker)) {
            boolean dodged = ivy.getRandom().nextFloat() < REACTIVE_CRIT_DODGE_CHANCE;
            if (!dodged) {
                return false;
            }
            ivy.setTarget(target);
            beginStance();
            lockSight(target);
            if (state != CombatState.DODGING) {
                startReactiveDodge(target, true, 100);
            }
            return true;
        }

        float dodgeChance = attacker instanceof Player ? REACTIVE_DODGE_CHANCE : REACTIVE_NON_PLAYER_DODGE_CHANCE;
        if (dodgeCooldown > 0 || ivy.getBoxingActionTicks() > 0 || ivy.getRandom().nextFloat() >= dodgeChance) {
            return false;
        }
        ivy.setTarget(target);
        beginStance();
        lockSight(target);
        startReactiveDodge(target, true, 45);
        return true;
    }

    private boolean isLikelyPlayerCritical(LivingEntity attacker) {
        if (!(attacker instanceof Player player)) {
            return false;
        }
        return player.fallDistance > 0.0F
                && !player.onGround()
                && !player.onClimbable()
                && !player.isInWater()
                && !player.hasEffect(MobEffects.BLINDNESS)
                && !player.isPassenger()
                && !player.isSprinting();
    }

    public void dodgeBlockedHit(@NotNull DamageSource source) {
        if (ivy.level().isClientSide || !(source.getEntity() instanceof LivingEntity attacker) || attacker == ivy || !attacker.isAlive()) {
            return;
        }
        LivingEntity target = resolveReactiveTarget(attacker);
        ivy.setTarget(target);
        if (ivy.isBoxingTaunting()) {
            interruptTaunt(target);
            return;
        }
        beginStance();
        lockSight(target);
        if (ivy.getBoxingActionTicks() <= 0 || state != CombatState.DODGING) {
            startReactiveDodge(target, true, 55);
        }
    }

    public void tick() {
        if (retreatRecoveryCooldown > 0) {
            retreatRecoveryCooldown--;
        }
        if (!isActive()) {
            return;
        }
        if (exitTicks > 0) {
            exitTicks--;
            if (exitTicks <= 0) {
                clear();
            }
            return;
        }
        int actionTicks = ivy.getBoxingActionTicks();
        if (actionTicks > 0) {
            ivy.setBoxingActionTicks(actionTicks - 1);
            if (ivy.isBoxingRecovering()) {
                if (recoveryConsumeTicks > 0 && --recoveryConsumeTicks <= 0) {
                    applyRecoveryConsume();
                }
            }
            if (actionTicks - 1 <= 0 && ivy.isBoxingRecovering()) {
                ivy.setBoxingRecoveryAction(IvyTheDragonMerchant.RECOVERY_NONE);
                if (ivy.getTarget() == null) {
                    startExitStance();
                    return;
                }
            }
            if (actionTicks - 1 <= 0 && ivy.isBoxingTaunting()) {
                ivy.setBoxingTaunting(false);
                if (exitAfterTaunt) {
                    exitAfterTaunt = false;
                    startExitStance();
                }
            }
        }
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (dashCrossCooldown > 0) {
            dashCrossCooldown--;
        }
        if (throwProjectilesCooldown > 0) {
            throwProjectilesCooldown--;
        }
        if (throwProjectilesRangePunishTicks > 0) {
            throwProjectilesRangePunishTicks--;
        }
        if (hookCooldown > 0) {
            hookCooldown--;
        }
        if (comboCooldown > 0) {
            comboCooldown--;
        }
        if (dodgeCooldown > 0) {
            dodgeCooldown--;
        }
        if (movingPressureCommitCooldown > 0) {
            movingPressureCommitCooldown--;
        }
        if (approachNudgeTicks > 0 && --approachNudgeTicks <= 0) {
            applyApproachNudge(pendingAttack.approachNudgeStrength);
        }
        if (impactTicks > 0 && --impactTicks <= 0) {
            applyImpact(pendingCounter != null ? pendingCounter.hit : pendingAttack.firstHit);
            pendingCounter = null;
        }
        if (secondImpactTicks > 0 && --secondImpactTicks <= 0) {
            applyImpact(pendingAttack.secondHit);
        }
        if (thirdImpactTicks > 0 && --thirdImpactTicks <= 0) {
            applyImpact(pendingAttack.thirdHit);
        }
        if (projectileTicks > 0 && --projectileTicks <= 0) {
            throwVenomArrowAtTarget();
        }
        if (secondProjectileTicks > 0 && --secondProjectileTicks <= 0) {
            throwVenomArrowAtTarget();
        }
        if (projectileDashTicks > 0 && --projectileDashTicks <= 0) {
            applyProjectileDash();
        }
        if (comboRetreatTicks > 0 && --comboRetreatTicks <= 0) {
            applyComboRetreat();
        }
        if (stateTicks > 0) {
            stateTicks--;
        }
    }

    private void tickRetreatRecovery(LivingEntity target) {
        lockSight(target);
        ivy.getNavigation().stop();
        ivy.setBoxingMovement(true, true);

        if (ivy.getBoxingActionTicks() > 0) {
            return;
        }

        double distanceSqr = ivy.distanceToSqr(target);
        boolean reachedRecoverySpace = recoveryWaitingForLanding
                || distanceSqr >= RETREAT_RECOVERY_SAFE_DISTANCE * RETREAT_RECOVERY_SAFE_DISTANCE
                || (recoveryRetreatTicks >= RETREAT_RECOVERY_MAX_RETREAT_TICKS
                && distanceSqr >= RETREAT_RECOVERY_FORCED_DISTANCE * RETREAT_RECOVERY_FORCED_DISTANCE);
        if (reachedRecoverySpace) {
            recoveryWaitingForLanding = !ivy.onGround();
            if (recoveryWaitingForLanding) {
                ivy.setBoxingMovement(false, false);
                return;
            }
            startRecoveryAnimation();
            return;
        }

        recoveryRetreatTicks++;
        if (recoveryBackstepCooldown > 0) {
            recoveryBackstepCooldown--;
            return;
        }

        recoveryBackstepCooldown = RETREAT_RECOVERY_BACKSTEP_INTERVAL_TICKS;
        applyRecoveryBackstep(target);
    }

    private void applyRecoveryBackstep(LivingEntity target) {
        Vec3 away;
        if (target != null && target.isAlive()) {
            away = ivy.position().subtract(target.position());
        } else {
            away = Vec3.directionFromRotation(0.0F, ivy.getYRot());
        }
        if (away.horizontalDistanceSqr() < 1.0E-4D) {
            away = Vec3.directionFromRotation(0.0F, ivy.getYRot());
        }

        Vec3 step = away.normalize().scale(1.55D);
        ivy.setDeltaMovement(step.x, ivy.getDeltaMovement().y + 0.08D, step.z);
        ivy.hasImpulse = true;
        ivy.setBoxingMovement(true, true);
        ivy.setBoxingActionTicks(DODGE_ACTION_TICKS);
        setState(CombatState.RETREATING_TO_RECOVER, DODGE_ACTION_TICKS);
        ivy.triggerAnim("movement", "dodge_backwards");
    }

    private void startRecoveryAnimation() {
        if (pendingRecoveryAction == IvyTheDragonMerchant.RECOVERY_NONE) {
            return;
        }

        ivy.getNavigation().stop();
        ivy.setBoxingMovement(false, false);
        ivy.setDeltaMovement(0.0D, ivy.getDeltaMovement().y, 0.0D);
        ivy.setBoxingActionTicks(RETREAT_RECOVERY_ACTION_TICKS);
        ivy.setBoxingRecoveryAction(pendingRecoveryAction);
        recoveryConsumeTicks = RETREAT_RECOVERY_CONSUME_TICKS;
        setState(CombatState.RECOVERING, RETREAT_RECOVERY_ACTION_TICKS);
        ivy.triggerAnim("movement", pendingRecoveryAction == IvyTheDragonMerchant.RECOVERY_DRINK
                ? "orthodox_retreat_to_drink"
                : "orthodox_retreat_to_eat");
        pendingRecoveryAction = IvyTheDragonMerchant.RECOVERY_NONE;
        recoveryRetreatTicks = 0;
        recoveryBackstepCooldown = 0;
        recoveryWaitingForLanding = false;
    }

    private void applyRecoveryConsume() {
        if (ivy.isBoxingDrinking()) {
            ivy.drinkMilkForRecovery();
        } else if (ivy.isBoxingEating()) {
            ivy.eatFoodForRecovery();
        }
    }

    public boolean tryStartRetreatRecovery() {
        if (ivy.level().isClientSide
                || retreatRecoveryCooldown > 0
                || ivy.isTrading()
                || !ivy.isReadyForCombatAnimation()
                || ivy.isBoxingRecovering()
                || ivy.isBoxingExiting()
                || ivy.isBoxingTaunting()
                || pendingRecoveryAction != IvyTheDragonMerchant.RECOVERY_NONE
                || ivy.getBoxingActionTicks() > 0) {
            return false;
        }

        LivingEntity target = selectPriorityTarget(ivy.getTarget());
        if (target == null) {
            return false;
        }
        ivy.setTarget(target);

        if (ivy.hasHarmfulEffect()) {
            pendingRecoveryAction = IvyTheDragonMerchant.RECOVERY_DRINK;
        } else if (ivy.needsRecoveryFood()) {
            pendingRecoveryAction = IvyTheDragonMerchant.RECOVERY_EAT;
        } else {
            return false;
        }

        beginStance();
        ivy.getNavigation().stop();
        ivy.setBoxingMovement(true, true);
        clearAttackTimers();
        recoveryRetreatTicks = 0;
        recoveryBackstepCooldown = 0;
        recoveryWaitingForLanding = false;
        setState(CombatState.RETREATING_TO_RECOVER, RETREAT_RECOVERY_MAX_RETREAT_TICKS);
        retreatRecoveryCooldown = RETREAT_RECOVERY_COOLDOWN_TICKS;
        return true;
    }

    public void tickRotationLock() {
        if (!isActive()) {
            return;
        }
        LivingEntity target = ivy.getTarget();
        if (target != null && target.isAlive()) {
            lockSight(target);
        }
    }

    private void beginStance() {
        if (exitTicks > 0) {
            exitTicks = 0;
            ivy.setBoxingExiting(false);
        } else if (isActive()) {
            return;
        }
        exitAfterTaunt = false;
        ivy.setBoxingTaunting(false);
        ivy.cancelPassiveAnimationsForCombat();
        ivy.setBoxingStance(true);
        ivy.setBoxingActionTicks(STANCE_TRANSITION_TICKS);
        setState(CombatState.RECOVERING, STANCE_TRANSITION_TICKS);
        ivy.triggerAnim("movement", "to_orthodox");
    }

    private void clear() {
        attackCooldown = 0;
        dashCrossCooldown = 0;
        throwProjectilesCooldown = 0;
        throwProjectilesRangePunishTicks = 0;
        dodgeCooldown = 0;
        hookCooldown = 0;
        comboCooldown = 0;
        exitTicks = 0;
        exitAfterTaunt = false;
        impactTicks = 0;
        secondImpactTicks = 0;
        thirdImpactTicks = 0;
        projectileTicks = 0;
        secondProjectileTicks = 0;
        projectileDashTicks = 0;
        approachNudgeTicks = 0;
        comboRetreatTicks = 0;
        recoveryConsumeTicks = 0;
        pendingRecoveryAction = IvyTheDragonMerchant.RECOVERY_NONE;
        recoveryRetreatTicks = 0;
        recoveryBackstepCooldown = 0;
        recoveryWaitingForLanding = false;
        impactTargetId = -1;
        pendingCounter = null;
        lastCombatTarget = null;
        committedPillagerTarget = null;
        lastTargetId = -1;
        targetMoveX = 0.0D;
        targetMoveZ = 0.0D;
        previousTargetDistanceSqr = 0.0D;
        targetStillTicks = 0;
        targetMovingTicks = 0;
        movingPressureCommitCooldown = 0;
        setState(CombatState.RECOVERING, 0);
        ivy.setBoxingMovement(false, false);
        ivy.setBoxingActionTicks(0);
        ivy.setBoxingTaunting(false);
        ivy.setBoxingExiting(false);
        ivy.setBoxingRecoveryAction(IvyTheDragonMerchant.RECOVERY_NONE);
        ivy.setBoxingStance(false);
    }

    private void startExitStance() {
        if (exitTicks > 0) {
            return;
        }
        ivy.setBoxingTaunting(false);
        ivy.getNavigation().stop();
        ivy.setBoxingMovement(false, false);
        ivy.setBoxingActionTicks(0);
        ivy.setBoxingRecoveryAction(IvyTheDragonMerchant.RECOVERY_NONE);
        clearAttackTimers();
        setState(CombatState.EXITING, EXIT_STANCE_TICKS);
        exitTicks = EXIT_STANCE_TICKS;
        ivy.setBoxingStance(false);
        ivy.setBoxingExiting(true);
        ivy.triggerAnim("movement", "exit_orthodox");
    }

    private void startTaunt(LivingEntity target) {
        ivy.getNavigation().stop();
        lockSight(target);
        ivy.setBoxingMovement(false, false);
        ivy.setBoxingActionTicks(TAUNT_ACTION_TICKS);
        ivy.setBoxingRecoveryAction(IvyTheDragonMerchant.RECOVERY_NONE);
        this.exitAfterTaunt = true;
        clearAttackTimers();
        setState(CombatState.TAUNTING, TAUNT_ACTION_TICKS);
        ivy.setBoxingTaunting(true);
        ivy.setBoxingExiting(false);
        ivy.triggerAnim("movement", "orthodox_taunt");
    }

    private void clearAttackTimers() {
        impactTicks = 0;
        secondImpactTicks = 0;
        thirdImpactTicks = 0;
        projectileTicks = 0;
        secondProjectileTicks = 0;
        projectileDashTicks = 0;
        approachNudgeTicks = 0;
        comboRetreatTicks = 0;
        recoveryConsumeTicks = 0;
        recoveryRetreatTicks = 0;
        recoveryBackstepCooldown = 0;
        recoveryWaitingForLanding = false;
        impactTargetId = -1;
        pendingCounter = null;
    }

    private void interruptTaunt(LivingEntity attacker) {
        exitAfterTaunt = false;
        exitTicks = 0;
        ivy.setBoxingTaunting(false);
        ivy.setBoxingExiting(false);
        ivy.setBoxingStance(true);
        ivy.setBoxingActionTicks(0);
        ivy.setBoxingRecoveryAction(IvyTheDragonMerchant.RECOVERY_NONE);
        ivy.setBoxingMovement(false, false);
        clearAttackTimers();
        setState(CombatState.RECOVERING, 0);
        ivy.getNavigation().stop();
        lockSight(attacker);
    }

    private void startAttack(LivingEntity target, AttackType attack) {
        setState(CombatState.ATTACKING, attack.actionTicks);
        ivy.setBoxingMovement(false, false);
        ivy.setBoxingActionTicks(attack.actionTicks);
        ivy.setBoxingRecoveryAction(IvyTheDragonMerchant.RECOVERY_NONE);
        attackCooldown = attack.cooldownTicks;
        if (attack == AttackType.DASH_FORWARD_RIGHT_CROSS) {
            dashCrossCooldown = DASH_CROSS_COOLDOWN_TICKS;
        }
        if (attack == AttackType.THROW_PROJECTILES) {
            throwProjectilesCooldown = THROW_PROJECTILES_COOLDOWN_TICKS;
        }
        if (attack == AttackType.RIGHT_HOOK || attack == AttackType.RIGHT_HOOK_UPPERCUT) {
            hookCooldown = HOOK_COOLDOWN_TICKS;
        }
        if (attack != AttackType.LEFT_JAB && attack != AttackType.RIGHT_HOOK) {
            comboCooldown = COMBO_COOLDOWN_TICKS;
        }
        impactTicks = attack.firstImpactTicks;
        secondImpactTicks = attack.secondImpactTicks;
        thirdImpactTicks = attack.thirdImpactTicks;
        projectileTicks = attack.firstProjectileTicks;
        secondProjectileTicks = attack.secondProjectileTicks;
        projectileDashTicks = attack.dashTicks;
        approachNudgeTicks = attack.approachNudgeTicks;
        comboRetreatTicks = attack.retreatTicks;
        impactTargetId = target.getId();
        pendingAttack = attack;
        pendingCounter = null;
        ivy.triggerAnim("movement", attack.trigger);
    }

    private void startDodge(LivingEntity target) {
        int dodge = canCounterDodge(target) ? ivy.getRandom().nextInt(3) : ivy.getRandom().nextInt(2);
        setState(CombatState.DODGING, DODGE_ACTION_TICKS);
        ivy.setBoxingActionTicks(DODGE_ACTION_TICKS);
        ivy.setBoxingRecoveryAction(IvyTheDragonMerchant.RECOVERY_NONE);
        dodgeCooldown = DODGE_COOLDOWN_TICKS;
        impactTicks = 0;
        secondImpactTicks = 0;
        thirdImpactTicks = 0;
        projectileTicks = 0;
        secondProjectileTicks = 0;
        projectileDashTicks = 0;
        approachNudgeTicks = 0;
        comboRetreatTicks = 0;
        pendingCounter = null;
        ivy.triggerAnim("movement", switch (dodge) {
            case 0 -> "dodge_left";
            case 1 -> "dodge_right";
            default -> "dodge_backwards";
        });

        Vec3 away = ivy.position().subtract(target.position());
        if (away.horizontalDistanceSqr() < 1.0E-4D) {
            away = Vec3.directionFromRotation(0.0F, ivy.getYRot());
        }
        Vec3 side = new Vec3(-away.z, 0.0D, away.x).normalize();
        Vec3 impulse = dodge == 2 ? away.normalize().scale(0.48D) : side.scale(dodge == 0 ? 0.42D : -0.42D);
        ivy.setDeltaMovement(ivy.getDeltaMovement().add(impulse.x, 0.08D, impulse.z));
        ivy.hasImpulse = true;
    }

    private void startReactiveDodge(LivingEntity target, boolean allowCounter, int counterChance) {
        if (allowCounter && canCounterDodge(target) && ivy.getRandom().nextInt(100) < counterChance) {
            startCounterDodge(target);
        } else {
            startDodge(target);
        }
    }

    private boolean canCounterDodge(LivingEntity target) {
        return ivy.distanceToSqr(target) <= COUNTER_DODGE_RANGE * COUNTER_DODGE_RANGE;
    }

    private void startCounterDodge(LivingEntity target) {
        CounterType counter = ivy.getRandom().nextBoolean() ? CounterType.LEFT_LIVER_SHOT : CounterType.RIGHT_LIVER_SHOT;
        setState(CombatState.DODGING, LIVER_COUNTER_ACTION_TICKS);
        ivy.setBoxingMovement(false, false);
        ivy.setBoxingActionTicks(LIVER_COUNTER_ACTION_TICKS);
        ivy.setBoxingRecoveryAction(IvyTheDragonMerchant.RECOVERY_NONE);
        dodgeCooldown = DODGE_COOLDOWN_TICKS + 6;
        impactTicks = LIVER_COUNTER_IMPACT_TICKS;
        secondImpactTicks = 0;
        thirdImpactTicks = 0;
        projectileTicks = 0;
        secondProjectileTicks = 0;
        projectileDashTicks = 0;
        approachNudgeTicks = 0;
        comboRetreatTicks = 0;
        impactTargetId = target.getId();
        pendingCounter = counter;
        ivy.triggerAnim("movement", counter.trigger);
        applyDiagonalCounterStep(target, counter);
    }

    private void applyDiagonalCounterStep(LivingEntity target, CounterType counter) {
        Vec3 away = ivy.position().subtract(target.position());
        if (away.horizontalDistanceSqr() < 1.0E-4D) {
            away = Vec3.directionFromRotation(0.0F, ivy.getYRot());
        }

        Vec3 radial = away.normalize();
        Vec3 tangent = new Vec3(-radial.z, 0.0D, radial.x);
        if (counter == CounterType.RIGHT_LIVER_SHOT) {
            tangent = tangent.scale(-1.0D);
        }

        Vec3 step = tangent.scale(0.44D).add(radial.scale(-0.18D)).normalize().scale(0.54D);
        ivy.setDeltaMovement(step.x, ivy.getDeltaMovement().y + 0.08D, step.z);
        ivy.hasImpulse = true;
    }

    private void applyImpact(AttackHit hit) {
        if (hit == null) {
            return;
        }
        if (impactTargetId < 0 || !(ivy.level().getEntity(impactTargetId) instanceof LivingEntity target)) {
            impactTargetId = -1;
            return;
        }
        if (!target.isAlive() || !isInHitRange(target, hit)) {
            return;
        }
        if (hit.forwardNudge > 0.0D) {
            applyForwardNudge(target, hit.forwardNudge);
        }
        if (hit.resetInvulnerability) {
            target.invulnerableTime = 0;
        }
        target.hurt(ivy.damageSources().mobAttack(ivy), hit.damage);
        Vec3 knockback = target.position().subtract(ivy.position());
        if (knockback.horizontalDistanceSqr() > 1.0E-4D) {
            target.push(knockback.x * hit.knockback, hit.lift, knockback.z * hit.knockback);
        }
    }

    private boolean isInHitRange(LivingEntity target, AttackHit hit) {
        return ivy.getBoundingBox().inflate(hit.range, 1.15D, hit.range).intersects(target.getBoundingBox());
    }

    private void applyForwardNudge(LivingEntity target, double strength) {
        Vec3 toward = target.position().subtract(ivy.position());
        if (toward.horizontalDistanceSqr() < 1.0E-4D) {
            toward = Vec3.directionFromRotation(0.0F, ivy.getYRot());
        }
        Vec3 step = toward.normalize().scale(strength);
        setComboHorizontalImpulse(step, 0.02D);
    }

    private void applyApproachNudge(double strength) {
        if (strength <= 0.0D || impactTargetId < 0 || !(ivy.level().getEntity(impactTargetId) instanceof LivingEntity target) || !target.isAlive()) {
            return;
        }
        applyForwardNudge(target, strength);
    }

    private void applyComboRetreat() {
        if (impactTargetId < 0 || !(ivy.level().getEntity(impactTargetId) instanceof LivingEntity target) || !target.isAlive()) {
            return;
        }

        Vec3 away = ivy.position().subtract(target.position());
        if (away.horizontalDistanceSqr() < 1.0E-4D) {
            away = Vec3.directionFromRotation(0.0F, ivy.getYRot());
        }
        Vec3 step = away.normalize().scale(1.25);
        setComboHorizontalImpulse(step, 0.04D);
    }

    private void throwVenomArrowAtTarget() {
        if (ivy.level().isClientSide || impactTargetId < 0 || !(ivy.level().getEntity(impactTargetId) instanceof LivingEntity target) || !target.isAlive()) {
            return;
        }
        if (!ivy.hasLineOfSight(target)) {
            return;
        }

        Vec3 look = ivy.getLookAngle();
        if (look.horizontalDistanceSqr() < 1.0E-4D) {
            look = target.position().subtract(ivy.position()).normalize();
        }

        Vec3 origin = ivy.position()
                .add(0.0D, ivy.getBbHeight() * 0.62D, 0.0D)
                .add(look.normalize().scale(0.55D));
        Vec3 targetVelocity = target.getDeltaMovement();
        Vec3 aimPoint = target.position()
                .add(targetVelocity.x * THROW_PROJECTILES_PREDICT_TICKS, target.getBbHeight() * 0.55D, targetVelocity.z * THROW_PROJECTILES_PREDICT_TICKS);
        Vec3 direction = aimPoint.subtract(origin);
        if (direction.lengthSqr() < 1.0E-4D) {
            direction = look;
        }

        ArrowOfVenomEntity arrow = new ArrowOfVenomEntity(ivy.level(), ivy);
        arrow.setPos(origin.x, origin.y, origin.z);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.shoot(direction.x, direction.y, direction.z, THROW_PROJECTILES_SPEED, THROW_PROJECTILES_INACCURACY);
        ivy.level().addFreshEntity(arrow);
    }

    private void applyProjectileDash() {
        if (impactTargetId < 0 || !(ivy.level().getEntity(impactTargetId) instanceof LivingEntity target) || !target.isAlive()) {
            return;
        }

        Vec3 toward = target.position().subtract(ivy.position());
        if (toward.horizontalDistanceSqr() < 1.0E-4D) {
            toward = Vec3.directionFromRotation(0.0F, ivy.getYRot());
        }
        Vec3 step = toward.normalize().scale(pendingAttack.dashStrength);
        setComboHorizontalImpulse(step, 0.03D);
    }

    private void setComboHorizontalImpulse(Vec3 step, double lift) {
        Vec3 current = ivy.getDeltaMovement();
        ivy.setDeltaMovement(step.x, current.y + lift, step.z);
        ivy.hasImpulse = true;
    }

    private AttackType chooseAttack(double distanceSqr, boolean targetStill) {
        if (distanceSqr < KEEP_DISTANCE * KEEP_DISTANCE && comboCooldown <= 0) {
            return chooseDistanceKeepingAttack();
        }

        if (comboCooldown <= 0
                && hookCooldown <= 0
                && distanceSqr <= ATTACK_RANGE * ATTACK_RANGE
                && ivy.getRandom().nextInt(100) < HOOK_UPPERCUT_CLOSE_CHANCE) {
            return AttackType.RIGHT_HOOK_UPPERCUT;
        }

        int jabWeight = targetStill ? 25 : 50;
        int hookWeight = hookCooldown <= 0 && distanceSqr <= HOOK_RANGE * HOOK_RANGE ? (targetStill ? 30 : 35) : 0;
        int jabCrossWeight = comboCooldown <= 0 && distanceSqr >= COMBO_MIN_RANGE * COMBO_MIN_RANGE
                ? (targetStill ? 45 : 25) : 0;
        int jabJabHookWeight = comboCooldown <= 0 && distanceSqr >= COMBO_MIN_RANGE * COMBO_MIN_RANGE
                ? (targetStill ? 30 : 18) : 0;

        int totalWeight = jabWeight + hookWeight + jabCrossWeight + jabJabHookWeight;
        int roll = ivy.getRandom().nextInt(totalWeight);
        if (roll < jabJabHookWeight) {
            return AttackType.JAB_JAB_HOOK;
        }
        roll -= jabJabHookWeight;
        if (roll < jabCrossWeight) {
            return AttackType.LEFT_JAB_RIGHT_CROSS;
        }
        roll -= jabCrossWeight;
        if (roll < hookWeight) {
            return AttackType.RIGHT_HOOK;
        }
        return AttackType.LEFT_JAB;
    }

    private AttackType chooseDistanceKeepingAttack() {
        return ivy.getRandom().nextBoolean() ? AttackType.LEFT_JAB_RIGHT_CROSS : AttackType.JAB_JAB_HOOK;
    }

    private boolean shouldCommitToMovingTarget(double distanceSqr, boolean targetStill) {
        return !targetStill
                && targetMovingTicks >= TARGET_MOVING_PRESSURE_TICKS
                && movingPressureCommitCooldown <= 0
                && distanceSqr > ATTACK_RANGE * ATTACK_RANGE;
    }

    private boolean canDashCross(double distanceSqr, DashCrossRead read, int roll) {
        return attackCooldown <= 0
                && comboCooldown <= 0
                && dashCrossCooldown <= 0
                && distanceSqr >= DASH_CROSS_MIN_RANGE * DASH_CROSS_MIN_RANGE
                && distanceSqr <= DASH_CROSS_MAX_RANGE * DASH_CROSS_MAX_RANGE
                && read.retreating()
                && roll < DASH_CROSS_RETREAT_CHANCE;
    }

    private boolean canThrowProjectiles(LivingEntity target, double distanceSqr, DashCrossRead dashRead) {
        if (attackCooldown > 0
                || throwProjectilesCooldown > 0
                || distanceSqr < THROW_PROJECTILES_MIN_RANGE * THROW_PROJECTILES_MIN_RANGE
                || distanceSqr > THROW_PROJECTILES_MAX_RANGE * THROW_PROJECTILES_MAX_RANGE
                || !ivy.hasLineOfSight(target)) {
            return false;
        }
        return isProjectilePressureTarget(target)
                || (target instanceof Player && dashRead.retreating())
                || (!(target instanceof Player) && (throwProjectilesRangePunishTicks > 0 || isSkirmishingTarget(dashRead)));
    }

    private boolean isSkirmishingTarget(DashCrossRead read) {
        if (read.retreating()) {
            return true;
        }
        if (targetMovingTicks < TARGET_MOVING_PRESSURE_TICKS || read.previousDistanceSqr <= 0.0D) {
            return false;
        }
        double previousDistance = Math.sqrt(read.previousDistanceSqr);
        double currentDistance = Math.sqrt(read.currentDistanceSqr);
        return currentDistance >= APPROACH_DISTANCE && currentDistance >= previousDistance - SKIRMISH_CLOSING_TOLERANCE;
    }

    private DashCrossRead readDashCross(LivingEntity target, double distanceSqr) {
        Vec3 awayFromIvy = target.position().subtract(ivy.position());
        double horizontalSpeedSqr = targetMoveX * targetMoveX + targetMoveZ * targetMoveZ;
        if (awayFromIvy.horizontalDistanceSqr() < 1.0E-4D || horizontalSpeedSqr < 1.0E-4D) {
            return new DashCrossRead(0.0D, horizontalSpeedSqr, previousTargetDistanceSqr, distanceSqr, false);
        }

        double retreatDot = awayFromIvy.normalize().dot(new Vec3(targetMoveX, 0.0D, targetMoveZ).normalize());
        boolean increasing = distanceSqr > previousTargetDistanceSqr + RETREAT_DISTANCE_INCREASE_SQ;
        boolean retreating = retreatDot >= RETREAT_DOT_THRESHOLD && increasing;
        return new DashCrossRead(retreatDot, horizontalSpeedSqr, previousTargetDistanceSqr, distanceSqr, retreating);
    }

    private void setState(CombatState state, int ticks) {
        this.state = state;
        this.stateTicks = Math.max(0, ticks);
    }

    private void lockSight(LivingEntity target) {
        double dx = target.getX() - ivy.getX();
        double dz = target.getZ() - ivy.getZ();
        if (dx * dx + dz * dz > 1.0E-4D) {
            float targetYaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
            float lockedYaw = approachYaw(targetYaw, ivy.getYRot());
            ivy.setYRot(lockedYaw);
            ivy.lockBoxingBodyToYaw(lockedYaw, LOCK_BODY_YAW_SPEED);
        }
        ivy.getLookControl().setLookAt(target, LOCK_LOOK_YAW_SPEED, LOCK_LOOK_PITCH_SPEED);
        ivy.lookAt(target, LOCK_LOOK_YAW_SPEED, LOCK_LOOK_PITCH_SPEED);
    }

    private static float approachYaw(float target, float current) {
        float delta = Mth.wrapDegrees(target - current);
        delta = Mth.clamp(delta, -IvyBoxingCombatController.LOCK_BODY_YAW_MAX_DELTA, IvyBoxingCombatController.LOCK_BODY_YAW_MAX_DELTA);
        return current + delta * IvyBoxingCombatController.LOCK_BODY_YAW_SPEED;
    }

    private void closeDistance(LivingEntity target) {
        setState(CombatState.CLOSING_DISTANCE, 8);
        ivy.setBoxingMovement(false, true);
        ivy.getNavigation().moveTo(target, 1.0D);
    }

    private void closeDistanceIntercept(LivingEntity target) {
        setState(CombatState.CLOSING_DISTANCE, 10);
        ivy.setBoxingMovement(false, true);

        Vec3 velocity = target.getDeltaMovement();
        Vec3 predicted = target.position().add(velocity.x * INTERCEPT_PREDICTION_TICKS, 0.0D, velocity.z * INTERCEPT_PREDICTION_TICKS);
        ivy.getNavigation().moveTo(predicted.x, target.getY(), predicted.z, 1.08D);
    }

    private boolean updateTargetStillness(LivingEntity target) {
        if (lastTargetId != target.getId()) {
            lastTargetId = target.getId();
            lastTargetX = target.getX();
            lastTargetZ = target.getZ();
            targetMoveX = 0.0D;
            targetMoveZ = 0.0D;
            previousTargetDistanceSqr = ivy.distanceToSqr(target);
            targetStillTicks = 0;
            targetMovingTicks = 0;
            return false;
        }

        previousTargetDistanceSqr = (lastTargetX - ivy.getX()) * (lastTargetX - ivy.getX())
                + (lastTargetZ - ivy.getZ()) * (lastTargetZ - ivy.getZ());
        double dx = target.getX() - lastTargetX;
        double dz = target.getZ() - lastTargetZ;
        targetMoveX = dx;
        targetMoveZ = dz;
        if (dx * dx + dz * dz <= TARGET_STILL_EPSILON_SQ) {
            targetStillTicks++;
            targetMovingTicks = 0;
        } else {
            targetStillTicks = 0;
            targetMovingTicks++;
            lastTargetX = target.getX();
            lastTargetZ = target.getZ();
        }
        return targetStillTicks >= TARGET_STILL_PRESSURE_TICKS;
    }

    private void keepDistance(LivingEntity target) {
        setState(CombatState.KEEPING_DISTANCE, 6);
        ivy.getNavigation().stop();
        ivy.setBoxingMovement(true, false);
        Vec3 away = ivy.position().subtract(target.position());
        if (away.horizontalDistanceSqr() > 1.0E-4D) {
            Vec3 step = away.normalize().scale(0.17D);
            ivy.setDeltaMovement(ivy.getDeltaMovement().add(step.x, 0.0D, step.z));
            ivy.hasImpulse = true;
        }
    }

    private void beginCircle() {
        setState(ivy.getRandom().nextBoolean() ? CombatState.CIRCLING_LEFT : CombatState.CIRCLING_RIGHT,
                18 + ivy.getRandom().nextInt(18));
    }

    private void circle(LivingEntity target, double distanceSqr) {
        ivy.getNavigation().stop();
        ivy.setBoxingMovement(false, false);

        Vec3 away = ivy.position().subtract(target.position());
        if (away.horizontalDistanceSqr() < 1.0E-4D) {
            away = Vec3.directionFromRotation(0.0F, ivy.getYRot());
        }

        Vec3 radial = away.normalize();
        Vec3 tangent = new Vec3(-radial.z, 0.0D, radial.x);
        if (state == CombatState.CIRCLING_RIGHT) {
            tangent = tangent.scale(-1.0D);
        }

        double preferred = 2.9D;
        double distance = Math.sqrt(distanceSqr);
        double radialCorrection = Mth.clamp(distance - preferred, -0.75D, 0.75D) * -0.035D;
        Vec3 step = tangent.scale(0.13D).add(radial.scale(radialCorrection));
        ivy.setDeltaMovement(ivy.getDeltaMovement().add(step.x, 0.0D, step.z));
        ivy.hasImpulse = true;
    }

    private class BoxingGoal extends Goal {
        BoxingGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = selectPriorityTarget(ivy.getTarget());
            if (!canBox(target)) {
                return false;
            }
            if (target != ivy.getTarget()) {
                ivy.setTarget(target);
            }
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = selectPriorityTarget(ivy.getTarget());
            if (!canBox(target)) {
                return false;
            }
            if (target != ivy.getTarget()) {
                ivy.setTarget(target);
            }
            return true;
        }

        @Override
        public void start() {
            beginStance();
        }

        @Override
        public void stop() {
            ivy.getNavigation().stop();
            if (ivy.isAlive() && ivy.isBoxingStance()) {
                LivingEntity target = ivy.getTarget() != null ? ivy.getTarget() : lastCombatTarget;
                LivingEntity nextTarget = selectPriorityTarget(null);
                if (nextTarget != null) {
                    ivy.setTarget(nextTarget);
                    beginStance();
                    return;
                }
                if (shouldTauntDefeatedEnemy(target)) {
                    startTaunt(target);
                } else {
                    startExitStance();
                }
            } else {
                clear();
            }
        }

        @Override
        public void tick() {
            LivingEntity target = selectPriorityTarget(ivy.getTarget());
            if (target == null) {
                return;
            }
            if (target != ivy.getTarget()) {
                ivy.setTarget(target);
            }
            lastCombatTarget = target;

            lockSight(target);

            if (pendingRecoveryAction != IvyTheDragonMerchant.RECOVERY_NONE) {
                tickRetreatRecovery(target);
                return;
            }

            if (ivy.getBoxingActionTicks() > 0) {
                ivy.getNavigation().stop();
                ivy.setBoxingMovement(false, false);
                return;
            }

            double distanceSqr = ivy.distanceToSqr(target);
            boolean targetStill = updateTargetStillness(target);
            boolean commitReady = shouldCommitToMovingTarget(distanceSqr, targetStill);
            DashCrossRead dashRead = readDashCross(target, distanceSqr);
            if (canThrowProjectiles(target, distanceSqr, dashRead)) {
                ivy.getNavigation().stop();
                ivy.setBoxingMovement(false, false);
                startAttack(target, AttackType.THROW_PROJECTILES);
                return;
            }
            if (dodgeCooldown <= 0 && ivy.getRandom().nextInt(80) == 0 && distanceSqr < 16.0D) {
                ivy.getNavigation().stop();
                startDodge(target);
                return;
            }

            if (commitReady) {
                movingPressureCommitCooldown = MOVING_PRESSURE_COMMIT_COOLDOWN_TICKS;
                int dashRoll = ivy.getRandom().nextInt(100);
                if (canDashCross(distanceSqr, dashRead, dashRoll)) {
                    ivy.getNavigation().stop();
                    ivy.setBoxingMovement(false, false);
                    startAttack(target, AttackType.DASH_FORWARD_RIGHT_CROSS);
                } else {
                    closeDistanceIntercept(target);
                }
                return;
            }

            if (distanceSqr <= ATTACK_RANGE * ATTACK_RANGE) {
                ivy.getNavigation().stop();
                ivy.setBoxingMovement(false, false);
                if (attackCooldown <= 0) {
                    startAttack(target, chooseAttack(distanceSqr, targetStill));
                } else if (distanceSqr < KEEP_DISTANCE * KEEP_DISTANCE) {
                    keepDistance(target);
                }
                return;
            }

            if (isPressureTarget(target)) {
                closeDistance(target);
                return;
            }

            if (targetStill && distanceSqr > ATTACK_RANGE * ATTACK_RANGE) {
                closeDistance(target);
                return;
            }

            if (distanceSqr > APPROACH_DISTANCE * APPROACH_DISTANCE) {
                closeDistance(target);
                return;
            }

            if ((state != CombatState.CIRCLING_LEFT && state != CombatState.CIRCLING_RIGHT) || stateTicks <= 0) {
                beginCircle();
            }
            circle(target, distanceSqr);
        }

        private boolean canBox(@Nullable LivingEntity target) {
            if (target == null || !target.isAlive() || !ivy.isAlive() || ivy.isTrading() || !ivy.isReadyForCombatAnimation()) {
                return false;
            }
            double distanceSqr = ivy.distanceToSqr(target);
            return distanceSqr <= 256.0D && (distanceSqr <= 36.0D || ivy.hasLineOfSight(target) || isTargetingIvy(target));
        }
    }

    private boolean isPressureTarget(LivingEntity target) {
        return target instanceof Pillager || target instanceof Evoker || target instanceof Vex || target instanceof Witch;
    }

    private boolean isProjectilePressureTarget(LivingEntity target) {
        return target instanceof Pillager || target instanceof Evoker || target instanceof Witch || target instanceof Vex;
    }

    @Nullable
    private LivingEntity selectPriorityTarget(@Nullable LivingEntity currentTarget) {
        if (currentTarget instanceof Player || currentTarget instanceof Evoker) {
            return currentTarget;
        }
        Evoker evoker = findNearestEvoker();
        if (evoker != null) {
            return evoker;
        }
        Pillager committedPillager = getValidCommittedPillager();
        if (committedPillager != null) {
            return committedPillager;
        }
        if (currentTarget instanceof Pillager pillager) {
            commitPillager(pillager);
            return pillager;
        }
        Pillager nearestPillager = findNearestPillager();
        if (nearestPillager != null) {
            return nearestPillager;
        }
        if (currentTarget instanceof Witch) {
            return currentTarget;
        }
        Witch witch = findNearestWitch();
        if (witch != null) {
            return witch;
        }
        if (currentTarget instanceof Vex) {
            return currentTarget;
        }
        Vex vex = findNearestVex();
        if (vex != null) {
            return vex;
        }
        LivingEntity aggressor = findNearestAggressorTargetingIvy();
        return aggressor != null ? aggressor : currentTarget;
    }

    @Nullable
    private Evoker findNearestEvoker() {
        Evoker best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Evoker evoker : ivy.level().getEntitiesOfClass(Evoker.class, ivy.getBoundingBox().inflate(16.0D))) {
            if (!evoker.isAlive()) {
                continue;
            }
            double distance = ivy.distanceToSqr(evoker);
            if (distance < bestDistance && (distance <= 36.0D || ivy.hasLineOfSight(evoker))) {
                best = evoker;
                bestDistance = distance;
            }
        }
        return best;
    }

    @Nullable
    private LivingEntity resolveReactiveTarget(LivingEntity attacker) {
        if (attacker instanceof Player || attacker instanceof Evoker) {
            return attacker;
        }
        Evoker evoker = findNearestEvoker();
        if (evoker != null) {
            return evoker;
        }
        Pillager committedPillager = getValidCommittedPillager();
        if (committedPillager != null) {
            return committedPillager;
        }
        if (attacker instanceof Pillager pillager) {
            commitPillager(pillager);
            return pillager;
        }
        if (attacker instanceof Witch) {
            return attacker;
        }
        return attacker;
    }

    private void commitPillager(Pillager pillager) {
        if (isValidTarget(pillager)) {
            committedPillagerTarget = pillager;
        }
    }

    @Nullable
    private Pillager getValidCommittedPillager() {
        if (isValidTarget(committedPillagerTarget)) {
            return committedPillagerTarget;
        }
        committedPillagerTarget = null;
        return null;
    }

    @Nullable
    private Pillager findNearestPillager() {
        Pillager best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Pillager pillager : ivy.level().getEntitiesOfClass(Pillager.class, ivy.getBoundingBox().inflate(16.0D))) {
            if (!isValidTarget(pillager)) {
                continue;
            }
            double distance = ivy.distanceToSqr(pillager);
            if (distance < bestDistance && (distance <= 36.0D || ivy.hasLineOfSight(pillager))) {
                best = pillager;
                bestDistance = distance;
            }
        }
        if (best != null) {
            commitPillager(best);
        }
        return best;
    }

    @Nullable
    private Witch findNearestWitch() {
        Witch best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Witch witch : ivy.level().getEntitiesOfClass(Witch.class, ivy.getBoundingBox().inflate(16.0D))) {
            if (!isValidTarget(witch)) {
                continue;
            }
            double distance = ivy.distanceToSqr(witch);
            if (distance < bestDistance && (distance <= 36.0D || ivy.hasLineOfSight(witch))) {
                best = witch;
                bestDistance = distance;
            }
        }
        return best;
    }

    @Nullable
    private Vex findNearestVex() {
        Vex best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Vex vex : ivy.level().getEntitiesOfClass(Vex.class, ivy.getBoundingBox().inflate(16.0D))) {
            if (!isValidTarget(vex)) {
                continue;
            }
            double distance = ivy.distanceToSqr(vex);
            if (distance < bestDistance && (distance <= 36.0D || ivy.hasLineOfSight(vex))) {
                best = vex;
                bestDistance = distance;
            }
        }
        return best;
    }

    private boolean isValidTarget(@Nullable LivingEntity target) {
        return target != null && target.isAlive() && !target.isRemoved();
    }

    private boolean isTargetingIvy(LivingEntity target) {
        return target instanceof Mob mob && mob.getTarget() == ivy;
    }

    @Nullable
    private LivingEntity findNearestAggressorTargetingIvy() {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Mob mob : ivy.level().getEntitiesOfClass(Mob.class, ivy.getBoundingBox().inflate(16.0D))) {
            if (!isValidTarget(mob) || mob == ivy || mob.getTarget() != ivy) {
                continue;
            }
            double distance = ivy.distanceToSqr(mob);
            if (distance < bestDistance && (distance <= 64.0D || ivy.hasLineOfSight(mob))) {
                best = mob;
                bestDistance = distance;
            }
        }
        return best;
    }

    private boolean shouldTauntDefeatedEnemy(@Nullable LivingEntity target) {
        return target != null && !target.isAlive() && !(target instanceof Player);
    }

    private enum CombatState {
        CLOSING_DISTANCE,
        KEEPING_DISTANCE,
        CIRCLING_LEFT,
        CIRCLING_RIGHT,
        ATTACKING,
        DODGING,
        TAUNTING,
        EXITING,
        RETREATING_TO_RECOVER,
        RECOVERING
    }

    private enum AttackType {
        LEFT_JAB("orthodox_left_jab", JAB_ACTION_TICKS, JAB_IMPACT_TICKS, 0, 0, 0, 0, 0.0D, JAB_COOLDOWN_TICKS,
                new AttackHit(2.5F, 0.015D, 0.0D, 2.35D, false, 0.0D), null, null),
        RIGHT_HOOK("orthodox_right_hook", HOOK_ACTION_TICKS, HOOK_IMPACT_TICKS, 0, 0, 0, 0, 0.0D, HOOK_COOLDOWN_TICKS,
                new AttackHit(6.0F, 0.28D, 0.08D, 2.45D, false, 0.0D), null, null),
        LEFT_JAB_RIGHT_CROSS("orthodox_left_jab_right_cross", LEFT_JAB_RIGHT_CROSS_ACTION_TICKS, LEFT_JAB_RIGHT_CROSS_FIRST_IMPACT_TICKS,
                LEFT_JAB_RIGHT_CROSS_SECOND_IMPACT_TICKS, 0, LEFT_JAB_RIGHT_CROSS_RETREAT_TICKS, 0, 0.0D, JAB_COOLDOWN_TICKS,
                new AttackHit(2.0F, 0.02D, 0.0D, 2.55D, true, 0.0D),
                new AttackHit(4.0F, 0.16D, 0.04D, 2.95D, true, 1), null),
        JAB_JAB_HOOK("orthodox_jab_jab_hook", JAB_JAB_HOOK_ACTION_TICKS,
                JAB_JAB_HOOK_FIRST_IMPACT_TICKS, JAB_JAB_HOOK_SECOND_IMPACT_TICKS,
                JAB_JAB_HOOK_THIRD_IMPACT_TICKS, 0, 0, 0.0D, JAB_COOLDOWN_TICKS,
                new AttackHit(1.8F, 0.0D, 0.0D, 2.45D, true, 0.0D),
                new AttackHit(1.8F, 0.0D, 0.0D, 2.45D, true, 0.0D),
                new AttackHit(5.5F, 0.24D, 0.06D, 3.05D, true, 0.55D)),
        RIGHT_HOOK_UPPERCUT("orthodox_right_hook_uppercut", RIGHT_HOOK_UPPERCUT_ACTION_TICKS,
                RIGHT_HOOK_UPPERCUT_FIRST_IMPACT_TICKS, RIGHT_HOOK_UPPERCUT_SECOND_IMPACT_TICKS,
                0, 0, 0, 0.0D, HOOK_COOLDOWN_TICKS,
                new AttackHit(4.0F, 0.015D, 0.0D, 2.55D, true, 0.0D),
                new AttackHit(6.0F, 0.12D, 0.50D, 3.05D, true, 0.55D),
                null),
        DASH_FORWARD_RIGHT_CROSS("orthodox_dash_forward_right_cross", DASH_FORWARD_RIGHT_CROSS_ACTION_TICKS,
                DASH_FORWARD_RIGHT_CROSS_IMPACT_TICKS, 0, 0, 0,
                DASH_FORWARD_RIGHT_CROSS_NUDGE_TICKS, 1.15D, COMBO_COOLDOWN_TICKS,
                new AttackHit(5.0F, 0.28D, 0.04D, 3.2D, true, 0.2D),
                null,
                null),
        THROW_PROJECTILES("orthodox_throw_projectiles", THROW_PROJECTILES_ACTION_TICKS,
                0, 0, 0, 0, 0, 0.0D, COMBO_COOLDOWN_TICKS,
                THROW_PROJECTILES_FIRST_THROW_TICKS, THROW_PROJECTILES_SECOND_THROW_TICKS, THROW_PROJECTILES_DASH_TICKS, THROW_PROJECTILES_DASH_STRENGTH,
                null,
                null,
                null);

        private final String trigger;
        private final int actionTicks;
        private final int firstImpactTicks;
        private final int secondImpactTicks;
        private final int thirdImpactTicks;
        private final int retreatTicks;
        private final int approachNudgeTicks;
        private final double approachNudgeStrength;
        private final int cooldownTicks;
        private final int firstProjectileTicks;
        private final int secondProjectileTicks;
        private final int dashTicks;
        private final double dashStrength;
        private final AttackHit firstHit;
        private final AttackHit secondHit;
        private final AttackHit thirdHit;

        AttackType(String trigger, int actionTicks, int firstImpactTicks, int secondImpactTicks, int thirdImpactTicks,
                   int retreatTicks, int approachNudgeTicks, double approachNudgeStrength, int cooldownTicks,
                   AttackHit firstHit, @Nullable AttackHit secondHit, @Nullable AttackHit thirdHit) {
            this(trigger, actionTicks, firstImpactTicks, secondImpactTicks, thirdImpactTicks, retreatTicks,
                    approachNudgeTicks, approachNudgeStrength, cooldownTicks, 0, 0, 0, 0.0D,
                    firstHit, secondHit, thirdHit);
        }

        AttackType(String trigger, int actionTicks, int firstImpactTicks, int secondImpactTicks, int thirdImpactTicks,
                   int retreatTicks, int approachNudgeTicks, double approachNudgeStrength, int cooldownTicks,
                   int firstProjectileTicks, int secondProjectileTicks, int dashTicks, double dashStrength,
                   @Nullable AttackHit firstHit, @Nullable AttackHit secondHit, @Nullable AttackHit thirdHit) {
            this.trigger = trigger;
            this.actionTicks = actionTicks;
            this.firstImpactTicks = firstImpactTicks;
            this.secondImpactTicks = secondImpactTicks;
            this.thirdImpactTicks = thirdImpactTicks;
            this.retreatTicks = retreatTicks;
            this.approachNudgeTicks = approachNudgeTicks;
            this.approachNudgeStrength = approachNudgeStrength;
            this.cooldownTicks = cooldownTicks;
            this.firstProjectileTicks = firstProjectileTicks;
            this.secondProjectileTicks = secondProjectileTicks;
            this.dashTicks = dashTicks;
            this.dashStrength = dashStrength;
            this.firstHit = firstHit;
            this.secondHit = secondHit;
            this.thirdHit = thirdHit;
        }
    }

    private enum CounterType {
        LEFT_LIVER_SHOT("dodge_left_liver_shot", new AttackHit(5.0F, 0.09D, 0.0D, 2.55D, true, 0.0D)),
        RIGHT_LIVER_SHOT("dodge_right_liver_shot", new AttackHit(5.0F, 0.09D, 0.0D, 2.55D, true, 0.0D));

        private final String trigger;
        private final AttackHit hit;

        CounterType(String trigger, AttackHit hit) {
            this.trigger = trigger;
            this.hit = hit;
        }
    }

    private record AttackHit(float damage, double knockback, double lift, double range, boolean resetInvulnerability,
                             double forwardNudge) {
    }

    private record DashCrossRead(double retreatDot, double horizontalSpeedSqr, double previousDistanceSqr,
                                 double currentDistanceSqr, boolean retreating) {
    }
}
