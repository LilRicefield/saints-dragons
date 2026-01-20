package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;

import java.util.EnumSet;
import java.util.List;

/**
 * Makes adult dragons protect nearby babies of the same species.
 * - Prevents flight when babies are nearby (adults stay grounded)
 * - Attacks entities that hurt babies
 * - Does NOT follow babies around (babies follow adults instead)
 *
 * @param <T> The dragon type (e.g., Raevyx, Ignivorus, etc.)
 */
public class DragonProtectBabiesGoal<T extends DragonEntity> extends TargetGoal {
    private final T dragon;
    private final Class<T> dragonClass;
    private LivingEntity attacker;
    private int timestamp;

    // Track which baby is being protected (for threat detection)
    private T targetBaby;
    private static final double MAX_DISTANCE_SQ = 256.0D; // 16 blocks - max protective range

    // Track if any babies are nearby (for flight prevention)
    private boolean babiesNearby = false;

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

        // Don't interfere if sitting or being ridden
        if (this.dragon.isOrderedToSit() || this.dragon.isVehicle()) {
            return false;
        }

        // Look for nearby babies
        List<T> nearbyBabies = this.dragon.level().getEntitiesOfClass(
                dragonClass,
                this.dragon.getBoundingBox().inflate(16.0D),
                baby -> baby != null && baby.isBaby() && baby.isAlive()
        );

        if (nearbyBabies.isEmpty()) {
            this.babiesNearby = false;
            return false;
        }

        // Babies are nearby - activate to prevent flight
        this.babiesNearby = true;

        // Also check for threats
        for (T baby : nearbyBabies) {
            LivingEntity babyAttacker = baby.getLastDamager();
            int attackerTimestamp = baby.getLastDamagerTimestamp();
            if (babyAttacker == null) {
                babyAttacker = baby.getLastHurtByMob();
                attackerTimestamp = baby.getLastHurtByMobTimestamp();
            }
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
                this.timestamp = attackerTimestamp;
                this.targetBaby = baby;
                return true;
            }
        }

        // No threats, but babies are nearby - still activate for flight prevention
        this.attacker = null;
        this.targetBaby = nearbyBabies.get(0); // Track any baby for distance checks
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if ordered to sit or being ridden
        if (this.dragon.isOrderedToSit() || this.dragon.isVehicle()) {
            return false;
        }

        // Check if any babies are still nearby
        List<T> nearbyBabies = this.dragon.level().getEntitiesOfClass(
                dragonClass,
                this.dragon.getBoundingBox().inflate(16.0D),
                baby -> baby != null && baby.isBaby() && baby.isAlive()
        );

        if (nearbyBabies.isEmpty()) {
            this.babiesNearby = false;
            return false;
        }

        this.babiesNearby = true;

        // If there's a threat, check if it's still valid
        if (this.attacker != null) {
            if (!this.attacker.isAlive() || this.dragon.distanceToSqr(this.attacker) > MAX_DISTANCE_SQ) {
                // Threat gone, clear it but continue for flight prevention
                this.attacker = null;
                this.dragon.setTarget(null);
            }
        }

        // Continue as long as babies are nearby (for flight prevention)
        return true;
    }

    @Override
    public void start() {
        // Set the attacker as target if there is one
        if (this.attacker != null) {
            this.dragon.setTarget(this.attacker);
        }

        super.start();
    }

    @Override
    public void tick() {
        // Prevent flight when babies are nearby
        preventFlight();

        // Continuously check for new threats
        checkForThreats();

        // Combat is handled by the TargetGoal system (setTarget in checkForThreats)
        // No pathfinding toward babies - they follow the adult instead
    }

    @Override
    public void stop() {
        this.attacker = null;
        this.targetBaby = null;
        this.dragon.getNavigation().stop();
    }

    /**
     * Continuously checks for threats to nearby babies.
     * This is called every tick to detect new attacks, not just when the goal first activates.
     */
    private void checkForThreats() {
        // Look for nearby babies
        List<T> nearbyBabies = this.dragon.level().getEntitiesOfClass(
                dragonClass,
                this.dragon.getBoundingBox().inflate(16.0D),
                baby -> baby != null && baby.isBaby() && baby.isAlive()
        );

        if (nearbyBabies.isEmpty()) {
            return;
        }

        // Check if any baby has been recently attacked
        for (T baby : nearbyBabies) {
            LivingEntity babyAttacker = baby.getLastDamager();
            int attackerTimestamp = baby.getLastDamagerTimestamp();
            if (babyAttacker == null) {
                babyAttacker = baby.getLastHurtByMob();
                attackerTimestamp = baby.getLastHurtByMobTimestamp();
            }

            // Check if this is a new/recent attack
            if (babyAttacker != null && babyAttacker.isAlive()) {
                // Don't attack other dragons of the same species or the owner
                if (dragonClass.isInstance(babyAttacker)) {
                    continue;
                }
                if (this.dragon.isTame() && babyAttacker == this.dragon.getOwner()) {
                    continue;
                }

                // Found a threat! Update target if it's new or more recent
                if (this.attacker != babyAttacker || attackerTimestamp > this.timestamp) {
                    this.attacker = babyAttacker;
                    this.timestamp = attackerTimestamp;
                    this.targetBaby = baby;
                    this.dragon.setTarget(babyAttacker);
                    return;
                }
            }
        }
    }

    /**
     * Prevents the dragon from flying while babies are nearby.
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
