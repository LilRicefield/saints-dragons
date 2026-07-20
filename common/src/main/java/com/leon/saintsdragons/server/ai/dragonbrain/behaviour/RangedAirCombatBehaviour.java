package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.DragonAirCombatSettingsProvider;
import com.leon.saintsdragons.server.ai.RangedAirCombatSettings;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public abstract class RangedAirCombatBehaviour<
        T extends RideableFlyingDragon & DragonAirCombatSettingsProvider>
        extends AirCombatMovementBehaviour<T> {
    private static final int REPOSITION_INTERVAL_TICKS = 20;
    private static final int SHOT_FROM_BELOW_THRESHOLD = 3;

    private final RangedAirCombatSettings combatSettings;
    private int attackCooldown;
    private int repositionCooldown;
    private int rangedCooldown;
    private int shotFromBelowCounter;
    private long lastDamageTick;

    protected RangedAirCombatBehaviour(RangedAirCombatSettings combatSettings) {
        this.combatSettings = combatSettings;
    }

    @Override
    protected final void tickAirCombat(DragonBrainContext<T> context,
                                       LivingEntity target,
                                       boolean hasLineOfSight) {
        tickCooldowns();
        if (checkEmergencyLanding(context, target)) {
            return;
        }

        T dragon = context.dragon();
        double distance = dragon.distanceTo(target);
        if (distance <= combatSettings.meleeRange() && hasLineOfSight) {
            if (!isCurrentlyAttacking(dragon) && attackCooldown <= 0 && tryStartMeleeAttack(dragon, target)) {
                attackCooldown = combatSettings.meleeAttackCooldownTicks();
            }
            setMeleePositionIntent(
                    context,
                    target,
                    0.0D,
                    combatSettings.meleeApproachDistance(),
                    1.2D,
                    0.6D
            );
            return;
        }

        if (canUseRangedWindow(dragon, target, distance, hasLineOfSight) && rangedCooldown <= 0) {
            if (!isCurrentlyAttacking(dragon) && attackCooldown <= 0 && tryStartRangedAttack(dragon, target)) {
                attackCooldown = combatSettings.rangedAttackCooldownTicks();
                rangedCooldown = combatSettings.rangedCooldownTicks();
            }
            if (isRangedAttackActive(dragon)) {
                context.memories().set(DragonMemories.MOVEMENT_INTENT, DragonMovementIntent.holdPosition());
            } else {
                setCombatPositionIntent(context, target);
            }
            return;
        }

        if (shouldDiveChase(
                dragon,
                target,
                combatSettings.diveMinHeightAdvantage(),
                combatSettings.diveMaxHorizontalDistance()
        )) {
            setPredictedChaseIntent(
                    context,
                    target,
                    3.0D,
                    -0.25D,
                    0.08D,
                    0.12D,
                    combatSettings.diveChaseSpeed()
            );
        } else {
            setPredictedChaseIntent(
                    context,
                    target,
                    5.0D,
                    0.5D,
                    0.15D,
                    0.5D,
                    combatSettings.directChaseSpeed()
            );
        }
    }

    @Override
    protected void stopAirCombat(DragonBrainContext<T> context) {
        attackCooldown = 0;
        repositionCooldown = 0;
    }

    protected abstract boolean isRangedAttackActive(T dragon);

    protected abstract boolean tryStartMeleeAttack(T dragon, LivingEntity target);

    protected abstract boolean tryStartRangedAttack(T dragon, LivingEntity target);

    protected boolean canUseRangedAttack(T dragon, LivingEntity target) {
        return true;
    }

    protected boolean isAdditionalAttackActive(T dragon) {
        return false;
    }

    private void tickCooldowns() {
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (rangedCooldown > 0) {
            rangedCooldown--;
        }
        if (repositionCooldown > 0) {
            repositionCooldown--;
        }
    }

    private boolean canUseRangedWindow(T dragon,
                                       LivingEntity target,
                                       double distance,
                                       boolean hasLineOfSight) {
        return canUseRangedAttack(dragon, target)
                && distance >= combatSettings.rangedMinRange()
                && distance <= combatSettings.rangedMaxRange()
                && hasLineOfSight;
    }

    private boolean isCurrentlyAttacking(T dragon) {
        return isRangedAttackActive(dragon) || isMeleeAttackActive(dragon) || isAdditionalAttackActive(dragon);
    }

    protected abstract boolean isMeleeAttackActive(T dragon);

    private void setCombatPositionIntent(DragonBrainContext<T> context, LivingEntity target) {
        if (repositionCooldown > 0) {
            return;
        }

        T dragon = context.dragon();
        double angle = dragon.tickCount * 0.05D % (Math.PI * 2.0D);
        double targetY = target.getY() + target.getBbHeight() * 0.5D;
        double verticalOffset = Math.sin(dragon.tickCount * 0.1D);
        Vec3 destination = new Vec3(
                target.getX() + Math.cos(angle) * combatSettings.engagementDistance(),
                targetY + verticalOffset,
                target.getZ() + Math.sin(angle) * combatSettings.engagementDistance()
        );
        context.memories().set(DragonMemories.MOVEMENT_INTENT, DragonMovementIntent.auto(destination, 1.0D));
        repositionCooldown = REPOSITION_INTERVAL_TICKS;
    }

    private boolean checkEmergencyLanding(DragonBrainContext<T> context, LivingEntity target) {
        if (context.memories().get(DragonMemories.GROUND_ROUTE_ABANDONED).orElse(false)) {
            shotFromBelowCounter = 0;
            return false;
        }
        T dragon = context.dragon();
        long currentTick = dragon.level().getGameTime();
        if (dragon.hurtTime > 0 && currentTick != lastDamageTick) {
            lastDamageTick = currentTick;
            if (target.getY() < dragon.getY() - 5.0D
                    && ++shotFromBelowCounter >= SHOT_FROM_BELOW_THRESHOLD) {
                context.memories().set(
                        DragonMemories.MOVEMENT_INTENT,
                        DragonMovementIntent.transitionToGround(
                                target,
                                dragon.getAiAirCombatSettings().landingSpeed()
                        )
                );
                shotFromBelowCounter = 0;
                return true;
            }
        }
        if (currentTick - lastDamageTick > 100L) {
            shotFromBelowCounter = 0;
        }
        return false;
    }
}
