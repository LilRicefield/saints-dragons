package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Custom breed goal for Raevyx dragons that handles their unique requirements.
 * Based on vanilla BreedGoal but adapted for flying tamed dragons.
 */
public class RaevyxBreedGoal extends Goal {
    private static final TargetingConditions PARTNER_TARGETING = TargetingConditions.forNonCombat().range(8.0D).ignoreLineOfSight();
    private final Raevyx dragon;
    private final Level level;
    private final double speedModifier;
    @Nullable
    private Raevyx partner;
    private int loveTime;

    public RaevyxBreedGoal(Raevyx dragon, double speedModifier) {
        this.dragon = dragon;
        this.level = dragon.level();
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Don't breed if not in love
        if (!this.dragon.isInLove()) {
            return false;
        }
        // Don't breed while flying
        if (this.dragon.isFlying()) {
            return false;
        }
        // Don't breed while sitting
        if (this.dragon.isOrderedToSit()) {
            return false;
        }
        // Find a partner
        this.partner = this.findMate();
        return this.partner != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.partner.isAlive()
            && this.partner.isInLove()
            && this.loveTime < 60
            && !this.dragon.isFlying()
            && !this.dragon.isOrderedToSit();
    }

    @Override
    public void stop() {
        this.partner = null;
        this.loveTime = 0;
    }

    @Override
    public void tick() {
        this.dragon.getLookControl().setLookAt(this.partner, 10.0F, (float)this.dragon.getMaxHeadXRot());

        // Move towards partner if on ground
        if (!this.dragon.isFlying()) {
            this.dragon.getNavigation().moveTo(this.partner, this.speedModifier);
        }

        ++this.loveTime;

        // Breed when close enough - dragons are large, so use bigger radius
        if (this.loveTime >= 60 && this.dragon.distanceToSqr(this.partner) < 600.0D) {
            this.breed();
        }
    }

    @Nullable
    private Raevyx findMate() {
        List<Raevyx> list = this.level.getNearbyEntities(
            Raevyx.class,
            PARTNER_TARGETING,
            this.dragon,
            this.dragon.getBoundingBox().inflate(8.0D)
        );

        double closestDist = Double.MAX_VALUE;
        Raevyx closestMate = null;

        for (Raevyx candidate : list) {
            if (this.dragon.canMate(candidate)) {
                double dist = this.dragon.distanceToSqr(candidate);
                if (dist < closestDist) {
                    closestMate = candidate;
                    closestDist = dist;
                }
            }
        }

        return closestMate;
    }

    private void breed() {
        ServerLevel serverlevel = (ServerLevel)this.level;
        Raevyx baby = (Raevyx) this.dragon.getBreedOffspring(serverlevel, this.partner);

        if (baby == null) {
            return;
        }

        // Reset love state
        this.dragon.resetLove();
        this.partner.resetLove();

        // Set baby position between parents
        baby.setBaby(true);
        baby.moveTo(
            (this.dragon.getX() + this.partner.getX()) / 2.0D,
            this.dragon.getY(),
            (this.dragon.getZ() + this.partner.getZ()) / 2.0D,
            0.0F, 0.0F
        );

        // Add baby to world
        serverlevel.addFreshEntity(baby);

        // Spawn experience and trigger advancements
        this.level.broadcastEntityEvent(this.dragon, (byte)18);
        if (this.level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT)) {
            this.level.addFreshEntity(new ExperienceOrb(
                this.level,
                this.dragon.getX(),
                this.dragon.getY(),
                this.dragon.getZ(),
                this.dragon.getRandom().nextInt(7) + 1
            ));
        }

        // Trigger advancement for breeding
        ServerPlayer serverplayer = this.dragon.getLoveCause();
        if (serverplayer == null && this.partner.getLoveCause() != null) {
            serverplayer = this.partner.getLoveCause();
        }

        if (serverplayer != null) {
            serverplayer.awardStat(Stats.ANIMALS_BRED);
            CriteriaTriggers.BRED_ANIMALS.trigger(serverplayer, this.dragon, this.partner, baby);
        }
    }
}
