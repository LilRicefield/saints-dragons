package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ignivorus;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.ai.goals.base.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.abilities.ignivorus.IgnivorusFireballAbility;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

public class IgnivorusGroundCombatBehaviour extends DragonBehaviour<Ignivorus> {
    public static final double MELEE_ENGAGE_RANGE = 6.0D;
    public static final float CHASE_SPEED = 1.75F;
    private Ignivorus dragon;
    private DragonBrainContext<Ignivorus> currentContext;
    private int attackCooldown = 0;
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

    public IgnivorusGroundCombatBehaviour() {
        super(Map.of(DragonMemories.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
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
        if (dragon.areRiderControlsLocked() || dragon.isLeaping() || dragon.isLeapImpactRecovering()) {
            return false;
        }

        if (!dragon.isInWaterOrBubble() && dragon.isAerial()) {
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
    protected void stop(DragonBrainContext<Ignivorus> context) {
        dragon.setAggressive(false);
        cancelFireBreathIfActive();
        hasUsedUltimateTrigger = false;
        currentContext = null;
    }

    @Override
    protected void start(DragonBrainContext<Ignivorus> context) {
        this.dragon = context.dragon();
        dragon.setAggressive(true);
        hasUsedUltimateTrigger = false;
    }

    @Override
    protected void tick(DragonBrainContext<Ignivorus> context) {
        this.currentContext = context;

        if (dragon.areRiderControlsLocked() || dragon.isLeaping() || dragon.isLeapImpactRecovering()) {
            stopMovement("ignivorus-combat:mobility-locked");
            return;
        }

        if (dragon.isAiPhase2Locked()) {
            stopMovement("ignivorus-combat:phase-locked");
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

        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (!dragon.isTargetValid(target)) {
            cancelFireBreathIfActive();
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
            handleWaterCombat(target, gap, hasLineOfSight);
            return;
        }

        if (!biteOnlyPrey) {
            maybeTogglePhase2(target, gap, hasLineOfSight);
        }

        if (!biteOnlyPrey && handleFireballActive(target)) {
            return;
        }

        if (!biteOnlyPrey && maybeStartPhase2GapCloseLeap(target, gap)) {
            return;
        }

        if (!biteOnlyPrey && maybeStartFireball(target, gap, hasLineOfSight)) {
            return;
        }

        if (!biteOnlyPrey && tryRandomBreath(target, hasLineOfSight)) {
            return;
        } else if (gap > MELEE_ENGAGE_RANGE) {
            return;
        } else {
            stopMovement("ignivorus-combat:melee-range");
            tryAttack(target);
        }
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

        if (gap <= MELEE_ENGAGE_RANGE) {
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

        stopMovement("ignivorus-combat:fire-breath");
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
            stopMovement("ignivorus-combat:stationary-fireball");
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

        if (dragon.getNavigation().isDone() && roll < FIREBALL_STATIONARY_CHANCE) {
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
        if (gap > MELEE_ENGAGE_RANGE) {
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

    private void handleWaterCombat(LivingEntity target, double gap, boolean hasLineOfSight) {
        if (!isCurrentlyAttacking() && hasLineOfSight && gap <= MELEE_ENGAGE_RANGE) {
            tryAttack(target);
        } else if (!isCurrentlyAttacking()) {
            tryRandomBreath(target, hasLineOfSight);
        }
    }

    public boolean isGroundMovementLocked() {
        if (dragon == null) {
            return false;
        }
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
