package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.common.block.RaevyxEggBlock;
import com.leon.saintsdragons.common.block.RaevyxEggBlockEntity;
import com.leon.saintsdragons.common.registry.ModBlocks;
import com.leon.saintsdragons.server.entity.base.DragonGender;
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
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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

        // Determine which dragon is female (the one that will lay the egg)
        Raevyx female = this.dragon.isFemale() ? this.dragon : this.partner;
        Raevyx male = this.dragon.isFemale() ? this.partner : this.dragon;

        // Reset love state
        this.dragon.resetLove();
        this.partner.resetLove();

        // Find ground position at female's location
        BlockPos eggPos = this.findEggLayingPosition(female);
        if (eggPos == null) {
            return; // No valid position found
        }

        // Generate baby gender randomly (for when egg hatches)
        DragonGender babyGender = this.dragon.getRandom().nextBoolean() ? DragonGender.MALE : DragonGender.FEMALE;

        // Place egg block
        BlockState eggState = ModBlocks.RAEVYX_EGG.get().defaultBlockState();
        serverlevel.setBlock(eggPos, eggState, 3);

        // Set block entity data
        BlockEntity blockEntity = serverlevel.getBlockEntity(eggPos);
        if (blockEntity instanceof RaevyxEggBlockEntity eggEntity) {
            // Store owner UUID if female is tamed
            if (female.isTame() && female.getOwnerUUID() != null) {
                eggEntity.setOwnerUUID(female.getOwnerUUID());
            }
            // Store baby gender
            eggEntity.setBabyGender(babyGender);
        }

        // Play egg laying sound
        serverlevel.playSound(null, eggPos, SoundEvents.TURTLE_LAY_EGG, SoundSource.BLOCKS, 0.8F, 1.0F);

        // Spawn experience and trigger advancements
        this.level.broadcastEntityEvent(female, (byte)18);
        if (this.level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT)) {
            this.level.addFreshEntity(new ExperienceOrb(
                this.level,
                female.getX(),
                female.getY(),
                female.getZ(),
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
            // Note: Can't trigger BRED_ANIMALS advancement without baby entity, but breeding still counted
        }
    }

    /**
     * Find a suitable ground position for egg laying at the female's location
     */
    @Nullable
    private BlockPos findEggLayingPosition(Raevyx female) {
        BlockPos startPos = female.blockPosition();

        // Try the current position first
        BlockPos groundPos = this.findGroundBelow(startPos, 5);
        if (groundPos != null && this.isValidEggPosition(groundPos)) {
            return groundPos;
        }

        // Try nearby positions in a small radius
        for (int xOffset = -1; xOffset <= 1; xOffset++) {
            for (int zOffset = -1; zOffset <= 1; zOffset++) {
                if (xOffset == 0 && zOffset == 0) continue;

                BlockPos offsetPos = startPos.offset(xOffset, 0, zOffset);
                groundPos = this.findGroundBelow(offsetPos, 5);
                if (groundPos != null && this.isValidEggPosition(groundPos)) {
                    return groundPos;
                }
            }
        }

        return null; // No valid position found
    }

    /**
     * Find ground block below the given position
     */
    @Nullable
    private BlockPos findGroundBelow(BlockPos pos, int maxDepth) {
        for (int i = 0; i <= maxDepth; i++) {
            BlockPos checkPos = pos.below(i);
            BlockState groundState = this.level.getBlockState(checkPos);
            BlockState aboveState = this.level.getBlockState(checkPos.above());

            // Found solid ground with air above
            if (!groundState.isAir() && groundState.isSolidRender(this.level, checkPos) &&
                aboveState.isAir()) {
                return checkPos.above();
            }
        }
        return null;
    }

    /**
     * Check if this is a valid position for an egg
     */
    private boolean isValidEggPosition(BlockPos pos) {
        BlockState state = this.level.getBlockState(pos);
        return state.isAir() && !this.level.getBlockState(pos.below()).isAir();
    }
}
