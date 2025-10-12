package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;


/**
 * Handles ALL pathfinding and movement without any attack logic interference.
 */
public class RaevyxMoveGoal extends Goal {
    private final Raevyx wyvern;
    private final boolean followingTargetEvenIfNotSeen;
    private int delayCounter;
    protected final double moveSpeed;

    public RaevyxMoveGoal(Raevyx wyvern, boolean followingTargetEvenIfNotSeen, double moveSpeed) {
        this.wyvern = wyvern;
        this.followingTargetEvenIfNotSeen = followingTargetEvenIfNotSeen;
        this.moveSpeed = moveSpeed;
        this.setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.wyvern.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (wyvern.isInAttackState()) return false;
        return true;
    }

    @Override
    public void start() {
        this.wyvern.setAggressive(true);
        this.wyvern.setRunning(true); // Set running state for combat
    }

    @Override
    public void stop() {
        wyvern.getNavigation().stop();
        LivingEntity livingentity = this.wyvern.getTarget();
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(livingentity)) {
            this.wyvern.setTarget((LivingEntity) null);
        }
        this.wyvern.setAggressive(false);
        this.wyvern.setRunning(false); // Stop running when not chasing
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.wyvern.getTarget();
        if (target == null) {
            return false;
        } else if (!target.isAlive()) {
            return false;
        } else if (wyvern.isInAttackState()) {
            // Stop moving when attacking
            return false;
        } else if (!this.followingTargetEvenIfNotSeen) {
            return !this.wyvern.getNavigation().isDone();
        } else if (!this.wyvern.isWithinRestriction(target.blockPosition())) {
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
        LivingEntity target = this.wyvern.getTarget();
        if (target != null && !wyvern.isInAttackState()) {
            // Only move when not attacking
            wyvern.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distSq = this.wyvern.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ());
            
            // Intelligent path recalculation with delays (Cataclysm-style)
            if (--this.delayCounter <= 0) {
                this.delayCounter = 4 + this.wyvern.getRandom().nextInt(7); // 4-10 tick delays (balanced)
                
                if (distSq > Math.pow(this.wyvern.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE), 2.0D)) {
                    if (!this.wyvern.isPathFinding()) {
                        if (!this.wyvern.getNavigation().moveTo(target, 1.0D)) {
                            this.delayCounter += 5;
                        }
                    }
                } else {
                    // Use the wyvern's current navigation
                    if (this.wyvern.isFlying()) {
                        this.wyvern.getNavigation().moveTo(target, this.moveSpeed * 1.5); // Faster when flying
                    } else {
                        this.wyvern.getNavigation().moveTo(target, this.moveSpeed);
                    }
                }
            }
        }
    }
}
