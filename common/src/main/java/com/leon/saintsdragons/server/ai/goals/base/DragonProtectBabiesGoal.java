package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;

import java.util.EnumSet;
import java.util.List;

/**
 * Makes adult dragons attack entities that hurt nearby babies of the same species.
 * Protective parent behavior - mama dragon doesn't let you hurt her babies!
 *
 * @param <T> The dragon type (e.g., Raevyx, Ignivorus, etc.)
 */
public class DragonProtectBabiesGoal<T extends DragonEntity> extends TargetGoal {
    private final T dragon;
    private final Class<T> dragonClass;
    private LivingEntity attacker;
    private int timestamp;

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

        // Don't interfere if sitting or already has a target
        if (this.dragon.isOrderedToSit()) {
            return false;
        }

        // Look for nearby babies that have been hurt recently
        List<T> nearbyBabies = this.dragon.level().getEntitiesOfClass(
                dragonClass,
                this.dragon.getBoundingBox().inflate(16.0D),
                baby -> baby != null && baby.isBaby() && baby.isAlive()
        );

        // Check if any baby has a recent attacker
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
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if the attacker is dead or too far away
        if (this.attacker == null || !this.attacker.isAlive()) {
            return false;
        }

        // Stop if too far away from the attacker
        if (this.dragon.distanceToSqr(this.attacker) > 256.0D) { // 16 blocks
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        // Set the attacker as target
        this.dragon.setTarget(this.attacker);
        super.start();
    }

    @Override
    public void stop() {
        this.attacker = null;
    }
}
