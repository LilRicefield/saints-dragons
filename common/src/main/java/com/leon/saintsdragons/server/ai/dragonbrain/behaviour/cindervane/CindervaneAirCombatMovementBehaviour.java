package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.cindervane;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.AirCombatMovementBehaviour;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;

public class CindervaneAirCombatMovementBehaviour extends AirCombatMovementBehaviour<Cindervane> {
    private static final double BITE_RANGE = 5.75D;
    private static final double DOUBLE_BITE_RANGE = 5.25D;
    private static final double FIRE_BODY_RANGE = 3.5D;
    private static final double FIRE_BODY_EXIT_RANGE = 11.0D;
    private static final double MELEE_FACING_DOT = 0.25D;
    private static final double BITE_APPROACH_DISTANCE = 3.5D;
    private static final double CHASE_HEIGHT_OFFSET = 0.5D;
    private static final double CHASE_SPEED = 2.0D;
    private static final double DIVE_CHASE_SPEED = 3.1D;
    private static final double DIVE_CHASE_MIN_HEIGHT_ADVANTAGE = 7.0D;
    private static final double DIVE_CHASE_MAX_HORIZONTAL_DISTANCE = 42.0D;
    private static final int DECISION_INTERVAL_TICKS = 6;
    private static final int POST_ABILITY_CHASE_TICKS = 8;
    private static final int FIRE_BODY_MIN_TICKS = 60;
    private static final int FIRE_BODY_MAX_TICKS = 160;

    private int decisionCooldown;
    private int chaseCommitTicks;
    private int fireBodyTicks;
    private boolean wasAbilityActive;
    private CombatAction lastAction = CombatAction.NONE;
    private CombatAction previousAction = CombatAction.NONE;
    private AirState state = AirState.CHASE;
    private String lastDecision = "idle";
    private double lastGap = -1.0D;

    @Override
    protected boolean checkExtraStartConditions(Cindervane dragon, LivingEntity target) {
        return canUseAirCombat(dragon);
    }

    @Override
    protected boolean checkExtraContinueConditions(Cindervane dragon, LivingEntity target) {
        return canUseAirCombat(dragon);
    }

    @Override
    protected void startAirCombat(DragonBrainContext<Cindervane> context) {
        decisionCooldown = 0;
        chaseCommitTicks = 0;
        fireBodyTicks = 0;
        wasAbilityActive = false;
        lastAction = CombatAction.NONE;
        previousAction = CombatAction.NONE;
        state = AirState.CHASE;
        lastDecision = "chase:engaged";
    }

    @Override
    protected void tickAirCombat(DragonBrainContext<Cindervane> context,
                                 LivingEntity target,
                                 boolean hasLineOfSight) {
        Cindervane dragon = context.dragon();
        dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (decisionCooldown > 0) {
            decisionCooldown--;
        }
        if (chaseCommitTicks > 0) {
            chaseCommitTicks--;
        }

        double gap = gapToTarget(dragon, target);
        lastGap = gap;
        updateFireBody(dragon, gap, hasLineOfSight);

        if (dragon.isTakeoff()) {
            state = AirState.CHASE;
            lastDecision = "transition:takeoff";
            return;
        }

        if (dragon.combatManager.getActiveAbility() != null) {
            wasAbilityActive = true;
            state = AirState.ABILITY;
            lastDecision = "ability:committed="
                    + dragon.combatManager.getActiveAbility().getClass().getSimpleName();
            holdPosition(context, "ability-committed");
            return;
        }
        if (wasAbilityActive) {
            wasAbilityActive = false;
            chaseCommitTicks = Math.max(chaseCommitTicks, POST_ABILITY_CHASE_TICKS);
        }

        if (tryActivateFireBody(dragon, gap)) {
            state = AirState.STANCE;
            lastDecision = "stance:fire-body";
            return;
        }

        boolean biteOnlyTarget = DragonTargetingHelper.isBiteOnlyPreyTarget(dragon, target);
        if (hasLineOfSight && decisionCooldown <= 0
                && chaseCommitTicks <= 0
                && dragon.getAiCombatPacing().getCadenceCooldownTicks() <= 0) {
            decisionCooldown = DECISION_INTERVAL_TICKS;
            CombatAction action = selectAction(dragon, target, gap, biteOnlyTarget);
            if (action != CombatAction.NONE && startAction(dragon, action)) {
                rememberAction(action);
                state = AirState.MELEE;
                lastDecision = "started:" + action.debugName;
                holdPosition(context, "started-" + action.debugName);
                return;
            }
        }

        if (gap <= BITE_RANGE && hasLineOfSight) {
            state = AirState.ALIGN;
            lastDecision = "aligning:melee";
            setMeleePositionIntent(context, target, 0.0D, BITE_APPROACH_DISTANCE, 1.2D, 0.7D);
            return;
        }

        state = AirState.CHASE;
        if (!hasLineOfSight) {
            lastDecision = "chase:no-line-of-sight";
        } else if (chaseCommitTicks > 0) {
            lastDecision = "chase:post-ability";
        } else {
            lastDecision = "chase:closing";
        }
        if (shouldDiveChase(
                dragon,
                target,
                DIVE_CHASE_MIN_HEIGHT_ADVANTAGE,
                DIVE_CHASE_MAX_HORIZONTAL_DISTANCE
        )) {
            setPredictedChaseIntent(context, target, 3.0D, -0.25D, 0.08D, 0.12D, DIVE_CHASE_SPEED);
        } else {
            setPredictedChaseIntent(context, target, 4.0D, CHASE_HEIGHT_OFFSET, 0.15D, 0.35D, CHASE_SPEED);
        }
    }

    @Override
    protected void stopAirCombat(DragonBrainContext<Cindervane> context) {
        Cindervane dragon = context.dragon();
        if (dragon.combatManager.getActiveAbility() != null) {
            DragonAbilityType<?, ?> activeType = dragon.combatManager.getActiveAbility().getAbilityType();
            dragon.forceEndAbility(activeType);
        }
        deactivateFireBody(dragon);
        decisionCooldown = 0;
        chaseCommitTicks = 0;
        fireBodyTicks = 0;
        wasAbilityActive = false;
        state = AirState.CHASE;
        lastDecision = "stopped";
    }

    private CombatAction selectAction(Cindervane dragon,
                                      LivingEntity target,
                                      double gap,
                                      boolean biteOnlyTarget) {
        boolean meleeFacing = isFacingTarget(dragon, target, MELEE_FACING_DOT);
        if (biteOnlyTarget) {
            return gap <= BITE_RANGE
                    && meleeFacing
                    && canUse(dragon, ModAbilities.CINDERVANE_BITE, false)
                    ? CombatAction.BITE
                    : CombatAction.NONE;
        }

        AbilityChoice best = new AbilityChoice(CombatAction.NONE, 0.0D);
        double doubleBiteScore = 92.0D + (DOUBLE_BITE_RANGE - gap) * 6.0D;
        best = consider(dragon, best, CombatAction.DOUBLE_BITE, doubleBiteScore,
                gap <= DOUBLE_BITE_RANGE
                        && meleeFacing
                        && canUse(dragon, ModAbilities.CINDERVANE_DOUBLE_BITE, false));

        double biteScore = 58.0D + (BITE_RANGE - gap) * 3.0D;
        best = consider(dragon, best, CombatAction.BITE, biteScore,
                gap <= BITE_RANGE
                        && meleeFacing
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
            case NONE -> false;
        };
    }

    private boolean tryActivateFireBody(Cindervane dragon, double gap) {
        if (gap > FIRE_BODY_RANGE
                || dragon.isAbilityActive(ModAbilities.CINDERVANE_FIRE_BODY)
                || dragon.isFireBodySuppressed()
                || dragon.combatManager.getActiveAbility() != null
                || dragon.getAiCombatPacing().getCadenceCooldownTicks() > 0) {
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

    private boolean canUseAirCombat(Cindervane dragon) {
        return !dragon.isBaby()
                && !dragon.isInWater()
                && !dragon.isInWaterOrBubble()
                && !dragon.isInLava();
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

    private boolean isFacingTarget(Cindervane dragon, LivingEntity target, double threshold) {
        Vec3 toTarget = target.getBoundingBox().getCenter().subtract(dragon.getBoundingBox().getCenter());
        if (toTarget.lengthSqr() <= 1.0E-6D) {
            return true;
        }
        Vec3 look = Vec3.directionFromRotation(dragon.getXRot(), dragon.yHeadRot);
        return look.normalize().dot(toTarget.normalize()) >= threshold;
    }

    private double gapToTarget(Cindervane dragon, LivingEntity target) {
        double radii = (dragon.getBbWidth() + target.getBbWidth()) * 0.5D;
        return Math.max(0.0D, dragon.distanceTo(target) - radii);
    }

    private void holdPosition(DragonBrainContext<Cindervane> context, String reason) {
        context.memories().set(
                DragonMemories.MOVEMENT_INTENT,
                DragonMovementIntent.stop("cindervane-air-combat:" + reason)
        );
        context.memories().erase(DragonMemories.WALK_TARGET);
        context.memories().erase(DragonMemories.PATH);
        context.dragon().getAIMovement().stop();
    }

    private void rememberAction(CombatAction action) {
        previousAction = lastAction;
        lastAction = action;
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("air_state", state.name().toLowerCase());
        details.put("air_decision", lastDecision);
        details.put("air_last_action", lastAction.debugName);
        details.put("air_gap", lastGap < 0.0D ? "none" : String.format("%.2f", lastGap));
        details.put("air_chase_commit_ticks", Integer.toString(chaseCommitTicks));
        details.put("air_fire_body_ticks", Integer.toString(fireBodyTicks));
        return Map.copyOf(details);
    }

    private enum AirState {
        CHASE,
        ALIGN,
        MELEE,
        STANCE,
        ABILITY
    }

    private enum CombatAction {
        NONE("none"),
        BITE("bite"),
        DOUBLE_BITE("double-bite");

        private final String debugName;

        CombatAction(String debugName) {
            this.debugName = debugName;
        }
    }

    private record AbilityChoice(CombatAction action, double score) {
    }
}
