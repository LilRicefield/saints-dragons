package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;

import java.util.EnumSet;
import java.util.List;

/**
 * Makes adult dragons protect and stay with nearby babies of the same species.
 * - Attacks entities that hurt babies
 * - Stays grounded and near babies (doesn't fly away)
 * - Follows babies to stay within protective range
 *
 * @param <T> The dragon type (e.g., Raevyx, Ignivorus, etc.)
 */
public class DragonProtectBabiesGoal<T extends DragonEntity> extends TargetGoal {
    private final T dragon;
    private final Class<T> dragonClass;
    private LivingEntity attacker;
    private int timestamp;

    // Stay-with-babies behavior
    private T targetBaby;
    private int pathRecalcTime;
    private static final double MIN_DISTANCE_SQ = 25.0D; // 5 blocks - comfortable distance
    private static final double MAX_DISTANCE_SQ = 256.0D; // 16 blocks - max protective range
    private static final double SPEED_MODIFIER = 1.0D;

    public DragonProtectBabiesGoal(T dragon, Class<T> dragonClass) {
        super(dragon, false);
        this.dragon = dragon;
        this.dragonClass = dragonClass;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        // Only adults protect babies
        if (this.dragon.isBaby()) {
            return false;
        }

        // Don't interfere if sitting
        if (this.dragon.isOrderedToSit()) {
            return false;
        }

        // Look for nearby babies
        List<T> nearbyBabies = this.dragon.level().getEntitiesOfClass(
                dragonClass,
                this.dragon.getBoundingBox().inflate(16.0D),
                baby -> baby != null && baby.isBaby() && baby.isAlive()
        );

        if (nearbyBabies.isEmpty()) {
            return false;
        }

        // Priority 1: Check if any baby has a recent attacker
        for (T baby : nearbyBabies) {
            LivingEntity babyAttacker = baby.getLastHurtByMob();
            if (babyAttacker != null && babyAttacker.isAlive()) {
                // Don't attack other dragons of the same species or the owner
                if (dragonClass.isInstance(babyAttacker)) {
                    continue;
                }
                if (this.dragon.isTame() && babyAttacker == this.dragon.getOwner()) {
                    continue;
                }

                // Found a valid threat!
                this.attacker = babyAttacker;
                this.timestamp = baby.getLastHurtByMobTimestamp();
                this.targetBaby = baby;
                return true;
            }
        }

        // Priority 2: No threats, but stay near babies anyway
        // Find the closest baby to stay with
        double closestDist = Double.MAX_VALUE;
        T closestBaby = null;
        for (T baby : nearbyBabies) {
            double dist = this.dragon.distanceToSqr(baby);
            if (dist < closestDist) {
                closestDist = dist;
                closestBaby = baby;
            }
        }

        // Only activate if baby is beyond minimum distance (not already close)
        if (closestBaby != null && closestDist > MIN_DISTANCE_SQ) {
            this.targetBaby = closestBaby;
            this.attacker = null; // No threat, just staying nearby
            return true;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if ordered to sit
        if (this.dragon.isOrderedToSit()) {
            return false;
        }

        // Stop if target baby is gone
        if (this.targetBaby == null || !this.targetBaby.isAlive() || !this.targetBaby.isBaby()) {
            return false;
        }

        double distToBaby = this.dragon.distanceToSqr(this.targetBaby);

        // Stop if baby is too far away
        if (distToBaby > MAX_DISTANCE_SQ) {
            return false;
        }

        // If there's an attacker, check if it's still valid
        if (this.attacker != null) {
            if (!this.attacker.isAlive() || this.dragon.distanceToSqr(this.attacker) > MAX_DISTANCE_SQ) {
                this.attacker = null; // Threat gone, but continue staying with baby
            }
        }

        // Stop only if baby is very close and there's no threat
        return distToBaby >= MIN_DISTANCE_SQ || this.attacker != null;
    }

    @Override
    public void start() {
        // Set the attacker as target if there is one
        if (this.attacker != null) {
            this.dragon.setTarget(this.attacker);
        }
        this.pathRecalcTime = 0;

        // Prevent flight when protecting babies
        preventFlight();

        super.start();
    }

    @Override
    public void tick() {
        if (this.targetBaby == null) {
            return;
        }

        // Keep preventing flight while protecting
        preventFlight();

        // If there's an attacker, prioritize combat (handled by TargetGoal)
        if (this.attacker != null && this.attacker.isAlive()) {
            return;
        }

        // Otherwise, stay near the baby
        if (--this.pathRecalcTime <= 0) {
            this.pathRecalcTime = this.adjustedTickDelay(10);

            double distToBaby = this.dragon.distanceToSqr(this.targetBaby);

            // Only path towards baby if beyond minimum distance
            if (distToBaby > MIN_DISTANCE_SQ) {
                this.dragon.getNavigation().moveTo(this.targetBaby, SPEED_MODIFIER);
            } else {
                // Close enough, stop moving
                this.dragon.getNavigation().stop();
            }
        }
    }

    @Override
    public void stop() {
        this.attacker = null;
        this.targetBaby = null;
        this.dragon.getNavigation().stop();
    }

    /**
     * Prevents the dragon from flying while protecting babies.
     * Flying dragons should stay grounded with their young.
     */
    private void preventFlight() {
        if (this.dragon instanceof DragonFlightCapable flightCapable) {
            // If dragon is currently flying, make it land
            if (flightCapable.isFlying()) {
                flightCapable.setFlying(false);
            }
        }
    }
}
