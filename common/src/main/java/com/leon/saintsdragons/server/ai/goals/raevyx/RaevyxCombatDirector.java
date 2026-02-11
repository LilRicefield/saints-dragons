package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Lightweight combat director for Raevyx ground combat.
 * Keeps behavior contextual while limiting action spam.
 */
public final class RaevyxCombatDirector {
    private static final int MODE_REEVALUATE_TICKS = 6;
    private static final int DAMAGE_MEMORY_TICKS = 30;

    private static final double DASH_MIN_RANGE = 8.0D;
    private static final double DASH_MAX_RANGE = 26.0D;
    private static final double DODGE_MAX_RANGE = 16.0D;

    private static final float BUDGET_REGEN_PER_TICK = 0.025f;
    private static final float DODGE_COST = 0.38f;
    private static final float DASH_COST = 0.62f;
    private static final float BEAM_COST = 0.25f;

    private static final int MOBILITY_LOCK_TICKS = 24;
    private static final int BEAM_LOCK_TICKS = 30;

    private Mode mode = Mode.PRESSURE;
    private int modeReevaluateCooldown = 0;
    private int recentDamageTicks = 0;
    private long lastDamageGameTime = Long.MIN_VALUE;
    private int mobilityLockTicks = 0;
    private int beamLockTicks = 0;
    private float mobilityBudget = 1.0f;

    public void reset() {
        mode = Mode.PRESSURE;
        modeReevaluateCooldown = 0;
        recentDamageTicks = 0;
        lastDamageGameTime = Long.MIN_VALUE;
        mobilityLockTicks = 0;
        beamLockTicks = 0;
        mobilityBudget = 1.0f;
    }

    public void tick(Raevyx dragon, LivingEntity target, double gap, boolean hasLineOfSight, boolean beamReady) {
        if (mobilityLockTicks > 0) mobilityLockTicks--;
        if (beamLockTicks > 0) beamLockTicks--;
        if (recentDamageTicks > 0) recentDamageTicks--;
        mobilityBudget = Math.min(1.0f, mobilityBudget + BUDGET_REGEN_PER_TICK);

        long gameTime = dragon.level().getGameTime();
        if (dragon.hurtTime > 0 && gameTime != lastDamageGameTime) {
            lastDamageGameTime = gameTime;
            recentDamageTicks = DAMAGE_MEMORY_TICKS;
        }

        if (modeReevaluateCooldown > 0) {
            modeReevaluateCooldown--;
            return;
        }
        modeReevaluateCooldown = MODE_REEVALUATE_TICKS;
        mode = decideMode(target, gap, hasLineOfSight, beamReady);
    }

    public boolean shouldTryDodge(Raevyx dragon, LivingEntity target, double gap, boolean currentlyAttacking) {
        if (currentlyAttacking || dragon.isBeaming() || dragon.isDodging() || dragon.isDashing()) return false;
        if (mobilityLockTicks > 0 || gap > DODGE_MAX_RANGE || mobilityBudget < DODGE_COST) return false;

        float chance = switch (mode) {
            case EVADE -> 0.32f;
            case REPOSITION -> 0.18f;
            default -> 0.08f;
        };

        if (recentDamageTicks > 0) chance += 0.08f;
        if (isLikelyMeleeThreat(target, gap)) chance += 0.14f;

        if (dragon.getRandom().nextFloat() >= chance) return false;
        mobilityBudget = Math.max(0.0f, mobilityBudget - DODGE_COST);
        mobilityLockTicks = MOBILITY_LOCK_TICKS;
        beamLockTicks = Math.max(beamLockTicks, 8);
        return true;
    }

    public boolean shouldTryDash(Raevyx dragon, double gap, boolean currentlyAttacking) {
        if (currentlyAttacking || dragon.isBeaming() || dragon.isDodging() || dragon.isDashing()) return false;
        if (mobilityLockTicks > 0 || gap < DASH_MIN_RANGE || gap > DASH_MAX_RANGE || mobilityBudget < DASH_COST) return false;

        float chance = switch (mode) {
            case SPACE -> 0.20f;
            case PRESSURE -> 0.14f;
            default -> 0.06f;
        };
        if (recentDamageTicks > 0) chance *= 0.75f;

        if (dragon.getRandom().nextFloat() >= chance) return false;
        mobilityBudget = Math.max(0.0f, mobilityBudget - DASH_COST);
        mobilityLockTicks = MOBILITY_LOCK_TICKS;
        beamLockTicks = Math.max(beamLockTicks, 12);
        return true;
    }

    public boolean shouldTryBeam(Raevyx dragon, double gap, boolean hasLineOfSight, boolean beamReady, boolean currentlyAttacking, int attackCooldown) {
        if (currentlyAttacking || attackCooldown > 0) return false;
        if (!beamReady || !hasLineOfSight || beamLockTicks > 0 || mobilityBudget < BEAM_COST) return false;

        float chance = switch (mode) {
            case PRESSURE -> 0.20f;
            case SPACE -> 0.10f;
            default -> 0.04f;
        };
        if (gap < 8.0D || gap > 32.0D) chance *= 0.45f;
        if (recentDamageTicks > 0) chance *= 0.8f;

        if (dragon.getRandom().nextFloat() >= chance) return false;
        mobilityBudget = Math.max(0.0f, mobilityBudget - BEAM_COST);
        beamLockTicks = BEAM_LOCK_TICKS;
        return true;
    }

    private Mode decideMode(LivingEntity target, double gap, boolean hasLineOfSight, boolean beamReady) {
        if (recentDamageTicks > 0 && gap <= 14.0D) {
            return Mode.EVADE;
        }
        if (gap <= 5.0D) {
            return Mode.REPOSITION;
        }
        if (gap > 12.0D) {
            return Mode.SPACE;
        }
        if (hasLineOfSight && beamReady && gap >= 8.0D && gap <= 28.0D) {
            return Mode.PRESSURE;
        }
        if (isLikelyMeleeThreat(target, gap)) {
            return Mode.EVADE;
        }
        return Mode.PRESSURE;
    }

    private boolean isLikelyMeleeThreat(LivingEntity target, double gap) {
        if (gap > 6.0D) return false;
        if (target instanceof Player player) {
            return player.getAttackStrengthScale(0.0f) > 0.85f;
        }
        return target.swingTime > 0;
    }

    private enum Mode {
        PRESSURE,
        SPACE,
        EVADE,
        REPOSITION
    }
}

