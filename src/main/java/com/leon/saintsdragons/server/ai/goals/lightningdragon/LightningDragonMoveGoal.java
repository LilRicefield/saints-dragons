package com.leon.saintsdragons.server.ai.goals.lightningdragon;

import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;


/**
 * Handles ALL pathfinding and movement without any attack logic interference.
 */
public class LightningDragonMoveGoal extends Goal {
    private final LightningDragonEntity dragon;
    private final boolean followingTargetEvenIfNotSeen;
    private int delayCounter;
    protected final double moveSpeed;

    public LightningDragonMoveGoal(LightningDragonEntity dragon, boolean followingTargetEvenIfNotSeen, double moveSpeed) {
        this.dragon = dragon;
        this.followingTargetEvenIfNotSeen = followingTargetEvenIfNotSeen;
        this.moveSpeed = moveSpeed;
        this.setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.dragon.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (dragon.isInAttackState()) return false;
        return true;
    }

    @Override
    public void start() {
        this.dragon.setAggressive(true);
        this.dragon.setRunning(true); // Set running state for combat
    }

    @Override
    public void stop() {
        dragon.getNavigation().stop();
        LivingEntity livingentity = this.dragon.getTarget();
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(livingentity)) {
            this.dragon.setTarget((LivingEntity) null);
        }
        this.dragon.setAggressive(false);
        this.dragon.setRunning(false); // Stop running when not chasing
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.dragon.getTarget();
        if (target == null) {
            return false;
        } else if (!target.isAlive()) {
            return false;
        } else if (dragon.isInAttackState()) {
            // Stop moving when attacking
            return false;
        } else if (!this.followingTargetEvenIfNotSeen) {
            return !this.dragon.getNavigation().isDone();
        } else if (!this.dragon.isWithinRestriction(target.blockPosition())) {
            return false;
        } else {
            return !(target instanceof Player) || !target.isSpectator() && !((Player) target).isCreative();
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.dragon.getTarget();
        if (target != null && !dragon.isInAttackState()) {
            // Only move when not attacking
            dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distSq = this.dragon.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ());
            
            // Intelligent path recalculation with delays (Cataclysm-style)
            if (--this.delayCounter <= 0) {
                this.delayCounter = 4 + this.dragon.getRandom().nextInt(7); // 4-10 tick delays (balanced)
                
                if (distSq > Math.pow(this.dragon.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE), 2.0D)) {
                    if (!this.dragon.isPathFinding()) {
                        if (!this.dragon.getNavigation().moveTo(target, 1.0D)) {
                            this.delayCounter += 5;
                        }
                    }
                } else {
                    // Use the dragon's current navigation
                    if (this.dragon.isFlying()) {
                        this.dragon.getNavigation().moveTo(target, this.moveSpeed * 1.5); // Faster when flying
                    } else {
                        this.dragon.getNavigation().moveTo(target, this.moveSpeed);
                    }
                }
            }
        }
    }
}
