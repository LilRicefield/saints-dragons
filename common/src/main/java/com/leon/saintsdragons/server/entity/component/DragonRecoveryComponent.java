package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.util.Mth;


public final class DragonRecoveryComponent {
    private static final int RECOVERY_DELAY_AFTER_COMBAT_TICKS = 20 * 12;
    private static final int RECOVERY_INTERVAL_TICKS = 20;
    private static final float RECOVERY_AMOUNT = 1.0F;

    private final DragonEntity dragon;
    private int recoveryTickCounter = 0;

    public DragonRecoveryComponent(DragonEntity dragon) {
        this.dragon = dragon;
    }

    public void tick() {
        if (!canRecoverNow()) {
            recoveryTickCounter = 0;
            return;
        }

        recoveryTickCounter++;
        if (recoveryTickCounter >= RECOVERY_INTERVAL_TICKS) {
            recoveryTickCounter = 0;
            float targetHealth = dragon.getHealth() + RECOVERY_AMOUNT;
            dragon.setHealth(Mth.clamp(targetHealth, 0.0F, dragon.getMaxHealth()));
        }
    }

    private boolean canRecoverNow() {
        if (!dragon.isAlive() || dragon.isRemoved() || dragon.isDying()) {
            return false;
        }
        if (dragon.isOnFire() || dragon.getRemainingFireTicks() > 0 || dragon.isInLava()) {
            return false;
        }
        if (dragon.isTame()) {
            return false;
        }
        if (dragon.getHealth() >= dragon.getMaxHealth()) {
            return false;
        }
        if (dragon.getTarget() != null && dragon.isTargetValid(dragon.getTarget())) {
            return false;
        }
        if (dragon.getActiveAbility() != null) {
            return false;
        }
        if (dragon.isVehicle() || dragon.isPassenger()) {
            return false;
        }
        int recentCombatTick = Math.max(dragon.getLastDamagerTimestamp(), dragon.getLastHurtByMobTimestamp());
        return dragon.tickCount - recentCombatTick >= RECOVERY_DELAY_AFTER_COMBAT_TICKS;
    }
}
