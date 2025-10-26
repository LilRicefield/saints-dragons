package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

import java.util.EnumSet;
import java.util.List;

/**
 * Makes adult Raevyx attack entities that hurt nearby babies.
 * Protective parent behavior - mama dragon doesn't let you hurt her babies!
 */
public class RaevyxProtectBabiesGoal extends TargetGoal {
    private final Raevyx dragon;
    private LivingEntity attacker;
    private int timestamp;

    public RaevyxProtectBabiesGoal(Raevyx dragon) {
        super(dragon, false);
        this.dragon = dragon;
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
        List<Raevyx> nearbyBabies = this.dragon.level().getEntitiesOfClass(
                Raevyx.class,
                this.dragon.getBoundingBox().inflate(16.0D),
                baby -> baby != null && baby.isBaby() && baby.isAlive()
        );

        // Check if any baby has a recent attacker
        for (Raevyx baby : nearbyBabies) {
            LivingEntity babyAttacker = baby.getLastHurtByMob();
            if (babyAttacker != null && babyAttacker.isAlive()) {
                // Don't attack other Raevyx or the owner
                if (babyAttacker instanceof Raevyx) {
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
