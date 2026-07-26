package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.atroxiia;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public final class AtroxiiaGroundCombatBehaviour extends DragonBehaviour<Atroxiia> {
    public static final float CHASE_SPEED = 1.45F;
    public static final double MELEE_STOP_RANGE = 6.0D;

    private static final double SLAM_RANGE = 2.75D;
    private static final double SWIPE_RANGE = 6.0D;
    private static final double PRECISE_STRIKE_MIN_RANGE = 3.0D;
    private static final double PRECISE_STRIKE_MAX_RANGE = 6.5D;
    private static final double DEVASTATING_SWEEP_POINT_BLANK_RANGE = 2.5D;
    private static final double QUAKE_RANGE = 18.0D;
    private static final int KITE_PRESSURE_TRIGGER_TICKS = 50;
    private static final int DECISION_INTERVAL_TICKS = 6;
    private static final double FACING_DOT = 0.42D;
    private static final float COMBAT_TURN_DEGREES_PER_TICK = 10.0F;

    private int decisionCooldown;
    private int kitePressureTicks;
    private CombatAction lastAction = CombatAction.NONE;
    private String lastDecision = "idle";
    private double lastGap = -1.0D;
    private int lastEngagedEnemies;

    public AtroxiiaGroundCombatBehaviour() {
        super(Map.of(DragonMemories.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean canStart(DragonBrainContext<Atroxiia> context) {
        return isGroundCombatContext(context);
    }

    @Override
    protected boolean canContinue(DragonBrainContext<Atroxiia> context) {
        return isGroundCombatContext(context);
    }

    @Override
    protected void start(DragonBrainContext<Atroxiia> context) {
        context.dragon().setAggressive(true);
    }

    @Override
    protected void tick(DragonBrainContext<Atroxiia> context) {
        Atroxiia dragon = context.dragon();
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (target == null) {
            return;
        }

        dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (dragon.getActiveAbility() != null) {
            lastDecision = "committed:" + lastAction.debugName;
            claimStationaryMovement(context, "committed:" + lastAction.debugName);
            return;
        }

        if (decisionCooldown > 0) {
            decisionCooldown--;
        }

        boolean hasLineOfSight = dragon.getSensing().hasLineOfSight(target);
        double gap = gapToTarget(dragon, target);
        lastGap = gap;
        updateKitePressure(gap, hasLineOfSight);
        if (hasLineOfSight && gap <= SWIPE_RANGE) {
            turnTowardTarget(dragon, target);
        }

        if (!hasLineOfSight) {
            lastDecision = "closing:no-line-of-sight";
            return;
        }
        if (decisionCooldown > 0
                || dragon.getAiCombatPacing().getCadenceCooldownTicks() > 0) {
            lastDecision = "pacing";
            return;
        }

        decisionCooldown = DECISION_INTERVAL_TICKS;
        CombatSnapshot snapshot = new CombatSnapshot(
                target,
                gap,
                isFacingTarget(dragon, target),
                countEngagedEnemies(dragon, target, QUAKE_RANGE),
                target.getHealth() / Math.max(1.0F, target.getMaxHealth()),
                kitePressureTicks
        );
        lastEngagedEnemies = snapshot.engagedEnemies;
        CombatAction action = selectAction(dragon, snapshot);
        if (action != CombatAction.NONE && startAction(dragon, action)) {
            lastAction = action;
            lastDecision = "started:" + action.debugName;
            claimStationaryMovement(context, "started:" + action.debugName);
        } else {
            lastDecision = gap > SWIPE_RANGE ? "closing:range" : "aligning";
        }
    }

    @Override
    protected void stop(DragonBrainContext<Atroxiia> context) {
        if (!context.memories().has(DragonMemories.ATTACK_TARGET)) {
            context.dragon().setAggressive(false);
        }
        decisionCooldown = 0;
        kitePressureTicks = 0;
        lastAction = CombatAction.NONE;
        lastDecision = "stopped";
        lastGap = -1.0D;
        lastEngagedEnemies = 0;
    }

    public static double meleeStopRange(Atroxiia dragon, LivingEntity target) {
        return MELEE_STOP_RANGE + (dragon.getBbWidth() + target.getBbWidth()) * 0.5D;
    }

    public static boolean isMovementCommitted(Atroxiia dragon) {
        return dragon.getActiveAbility() != null;
    }

    private boolean isGroundCombatContext(DragonBrainContext<Atroxiia> context) {
        Atroxiia dragon = context.dragon();
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        return target != null
                && dragon.isTargetValid(target)
                && dragon.canTarget(target)
                && !dragon.isBaby()
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && !dragon.isOrderedToSit()
                && dragon.isGroundedForAction();
    }

    private CombatAction selectAction(Atroxiia dragon, CombatSnapshot snapshot) {
        CombatAction selected = CombatAction.NONE;
        double bestScore = 0.0D;

        if (snapshot.gap <= DEVASTATING_SWEEP_POINT_BLANK_RANGE
                && canUse(dragon, ModAbilities.ATROXIIA_DEVASTATING_SWEEP, true)) {
            double score = 150.0D
                    + snapshot.engagedEnemies * 12.0D
                    + (DEVASTATING_SWEEP_POINT_BLANK_RANGE - snapshot.gap) * 10.0D;
            if (score > bestScore) {
                selected = CombatAction.DEVASTATING_SWEEP;
                bestScore = score;
            }
        }

        boolean quakePressure = snapshot.engagedEnemies >= 3
                || snapshot.kitePressureTicks >= KITE_PRESSURE_TRIGGER_TICKS;
        if (quakePressure
                && snapshot.gap <= QUAKE_RANGE
                && canUse(dragon, ModAbilities.ATROXIIA_HELHEIM_QUAKE, true)) {
            double score = 125.0D + snapshot.engagedEnemies * 10.0D
                    + Math.min(30.0D, snapshot.kitePressureTicks * 0.4D);
            if (score > bestScore) {
                selected = CombatAction.HELHEIM_QUAKE;
                bestScore = score;
            }
        }

        boolean healthyTarget = snapshot.target.getHealth() >= 18.0F && snapshot.healthRatio >= 0.35F;
        if (snapshot.engagedEnemies == 1
                && healthyTarget
                && snapshot.facing
                && snapshot.gap >= PRECISE_STRIKE_MIN_RANGE
                && snapshot.gap <= PRECISE_STRIKE_MAX_RANGE
                && canUse(dragon, ModAbilities.ATROXIIA_PRECISE_STRIKE, true)) {
            double score = 96.0D + snapshot.healthRatio * 20.0D;
            if (score > bestScore) {
                selected = CombatAction.PRECISE_STRIKE;
                bestScore = score;
            }
        }

        if (snapshot.facing
                && snapshot.gap <= SLAM_RANGE
                && canUse(dragon, ModAbilities.ATROXIIA_SLAM, false)) {
            double score = 82.0D + (SLAM_RANGE - snapshot.gap) * 8.0D;
            if (score > bestScore) {
                selected = CombatAction.SLAM;
                bestScore = score;
            }
        }

        if (snapshot.facing
                && snapshot.gap <= SWIPE_RANGE
                && canUse(dragon, ModAbilities.ATROXIIA_SWIPE, false)) {
            double score = 58.0D + (SWIPE_RANGE - snapshot.gap) * 2.0D;
            if (score > bestScore) {
                selected = CombatAction.SWIPE;
            }
        }

        return selected;
    }

    private boolean startAction(Atroxiia dragon, CombatAction action) {
        return switch (action) {
            case DEVASTATING_SWEEP -> startAbility(
                    dragon, ModAbilities.ATROXIIA_DEVASTATING_SWEEP, true, 32, 240, 120, 240);
            case HELHEIM_QUAKE -> startAbility(
                    dragon, ModAbilities.ATROXIIA_HELHEIM_QUAKE, true, 50, 600, 240, 600);
            case PRECISE_STRIKE -> startAbility(
                    dragon, ModAbilities.ATROXIIA_PRECISE_STRIKE, true, 110, 360, 180, 360);
            case SLAM -> startAbility(
                    dragon, ModAbilities.ATROXIIA_SLAM, false, 28, 32, 0, 22);
            case SWIPE -> startAbility(
                    dragon, ModAbilities.ATROXIIA_SWIPE, false, 22, 24, 0, 12);
            case NONE -> false;
        };
    }

    private boolean canUse(Atroxiia dragon, DragonAbilityType<?, ?> abilityType, boolean majorAbility) {
        return dragon.combatManager.canStart(abilityType)
                && dragon.getAiCombatPacing().canUse(abilityType, majorAbility);
    }

    private boolean startAbility(Atroxiia dragon,
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

    private void updateKitePressure(double gap, boolean hasLineOfSight) {
        if (hasLineOfSight && gap > SWIPE_RANGE + 1.5D) {
            kitePressureTicks = Math.min(KITE_PRESSURE_TRIGGER_TICKS * 2, kitePressureTicks + 1);
        } else {
            kitePressureTicks = Math.max(0, kitePressureTicks - 2);
        }
    }

    private int countEngagedEnemies(Atroxiia dragon, LivingEntity target, double radius) {
        return dragon.level().getEntitiesOfClass(
                LivingEntity.class,
                dragon.getBoundingBox().inflate(radius, 6.0D, radius),
                entity -> isEngagedEnemy(dragon, target, entity)
        ).size();
    }

    private boolean isEngagedEnemy(Atroxiia dragon, LivingEntity target, LivingEntity entity) {
        if (entity == dragon || !entity.isAlive() || !entity.attackable() || dragon.isAlly(entity)) {
            return false;
        }
        return entity == target
                || entity.getLastHurtMob() == dragon
                || entity.getLastHurtByMob() == dragon;
    }

    private boolean isFacingTarget(Atroxiia dragon, LivingEntity target) {
        Vec3 toTarget = target.getBoundingBox().getCenter().subtract(dragon.getBoundingBox().getCenter());
        Vec3 horizontalTarget = new Vec3(toTarget.x, 0.0D, toTarget.z);
        if (horizontalTarget.lengthSqr() < 1.0E-6D) {
            return true;
        }
        Vec3 look = Vec3.directionFromRotation(0.0F, dragon.getYRot());
        return look.dot(horizontalTarget.normalize()) >= FACING_DOT;
    }

    private void turnTowardTarget(Atroxiia dragon, LivingEntity target) {
        double dx = target.getX() - dragon.getX();
        double dz = target.getZ() - dragon.getZ();
        if (dx * dx + dz * dz <= 1.0E-6D) {
            return;
        }

        float targetYaw = (float)(Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float yaw = Mth.approachDegrees(
                dragon.getYRot(),
                targetYaw,
                COMBAT_TURN_DEGREES_PER_TICK
        );
        dragon.setYRot(yaw);
        dragon.yBodyRot = yaw;
    }

    private double gapToTarget(Atroxiia dragon, LivingEntity target) {
        double combinedRadii = (dragon.getBbWidth() + target.getBbWidth()) * 0.5D;
        return Math.max(0.0D, dragon.distanceTo(target) - combinedRadii);
    }

    private void claimStationaryMovement(DragonBrainContext<Atroxiia> context, String reason) {
        context.memories().set(
                DragonMemories.MOVEMENT_INTENT,
                DragonMovementIntent.stop("atroxiia-combat:" + reason)
        );
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        return Map.of(
                "decision", lastDecision,
                "last_action", lastAction.debugName,
                "gap", lastGap < 0.0D ? "none" : Double.toString(Math.round(lastGap * 10.0D) / 10.0D),
                "engaged", Integer.toString(lastEngagedEnemies),
                "kite_ticks", Integer.toString(kitePressureTicks)
        );
    }

    private record CombatSnapshot(LivingEntity target,
                                  double gap,
                                  boolean facing,
                                  int engagedEnemies,
                                  float healthRatio,
                                  int kitePressureTicks) {
    }

    private enum CombatAction {
        NONE("none"),
        SWIPE("swipe"),
        SLAM("slam"),
        PRECISE_STRIKE("precise-strike"),
        DEVASTATING_SWEEP("devastating-sweep"),
        HELHEIM_QUAKE("helheim-quake");

        private final String debugName;

        CombatAction(String debugName) {
            this.debugName = debugName;
        }
    }
}
