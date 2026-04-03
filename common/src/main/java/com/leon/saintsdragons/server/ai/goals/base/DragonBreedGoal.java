package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Generic breed goal for dragons that lay eggs, with configurable partner range and breed distance.
 */
public class DragonBreedGoal<T extends DragonEntity> extends Goal {
    protected final T dragon;
    protected final Level level;
    private final double speedModifier;
    private final double partnerRange;
    private final double breedDistanceSqr;
    private final TargetingConditions partnerTargeting;
    private final Class<T> partnerClass;
    @Nullable
    protected T partner;
    private int loveTime;

    public DragonBreedGoal(T dragon, double speedModifier, Class<T> partnerClass, double partnerRange, double breedDistanceSqr) {
        this.dragon = dragon;
        this.level = dragon.level();
        this.speedModifier = speedModifier;
        this.partnerClass = partnerClass;
        this.partnerRange = partnerRange;
        this.breedDistanceSqr = breedDistanceSqr;
        this.partnerTargeting = TargetingConditions.forNonCombat()
                .range(partnerRange)
                .ignoreLineOfSight();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!isBreedingAllowed()) {
            return false;
        }
        this.partner = findMate();
        return this.partner != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.partner != null
                && this.partner.isAlive()
                && this.partner.isInLove()
                && this.loveTime < 60
                && isBreedingAllowed();
    }

    @Override
    public void stop() {
        this.partner = null;
        this.loveTime = 0;
    }

    @Override
    public void tick() {
        if (this.partner == null) {
            return;
        }

        this.dragon.getLookControl().setLookAt(this.partner, 10.0F, (float) this.dragon.getMaxHeadXRot());
        if (!this.dragon.isFlying()) {
            double speed = this.speedModifier;
            if (this.dragon instanceof com.leon.saintsdragons.server.entity.base.RideableDragonBase rideable) {
                speed = Math.min(speed, 0.35D);
                rideable.setGroundMoveStateFromAI(1);
                rideable.setRunning(false);
            }
            this.dragon.getNavigation().moveTo(this.partner, speed);
        }

        ++this.loveTime;

        if (this.loveTime >= 60 && this.dragon.distanceToSqr(this.partner) < this.breedDistanceSqr) {
            handleBreed();
        }
    }

    protected boolean isBreedingAllowed() {
        return this.dragon.isInLove()
                && !this.dragon.isFlying()
                && !this.dragon.isOrderedToSit();
    }

    @Nullable
    protected T findMate() {
        List<T> list = this.level.getNearbyEntities(
                this.partnerClass,
                partnerTargeting,
                this.dragon,
                this.dragon.getBoundingBox().inflate(this.partnerRange)
        );

        double closestDist = Double.MAX_VALUE;
        T closestMate = null;

        for (T candidate : list) {
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

    protected void handleBreed() {
        if (!(this.level instanceof ServerLevel serverlevel)) {
            return;
        }

        T female = this.dragon.isFemale() ? this.dragon : this.partner;
        T male = this.dragon.isFemale() ? this.partner : this.dragon;

        if (female == null || male == null) {
            return;
        }

        this.dragon.resetLove();
        this.partner.resetLove();
        if (this.dragon.getAge() == 0) {
            this.dragon.setAge(6000);
        }
        if (this.partner.getAge() == 0) {
            this.partner.setAge(6000);
        }

        BlockPos eggPos = findEggLayingPosition(female);
        if (eggPos == null) {
            return;
        }

        BlockState eggState = female.getEggBlockState();
        if (eggState == null) {
            return;
        }

        if (eggState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            eggState = eggState.setValue(BlockStateProperties.WATERLOGGED,
                    serverlevel.getFluidState(eggPos).getType() == Fluids.WATER);
        }

        serverlevel.setBlock(eggPos, eggState, 3);

        BlockEntity blockEntity = serverlevel.getBlockEntity(eggPos);
        if (blockEntity != null) {
            female.configureEggBlockEntity(blockEntity, male);
        }

        serverlevel.playSound(null, eggPos, SoundEvents.TURTLE_LAY_EGG, SoundSource.BLOCKS, 0.8F, 1.0F);

        this.level.broadcastEntityEvent(female, (byte) 18);
        if (this.level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT)) {
            this.level.addFreshEntity(new ExperienceOrb(
                    this.level,
                    female.getX(),
                    female.getY(),
                    female.getZ(),
                    this.dragon.getRandom().nextInt(7) + 1
            ));
        }

        ServerPlayer serverplayer = this.dragon.getLoveCause();
        if (serverplayer == null && this.partner.getLoveCause() != null) {
            serverplayer = this.partner.getLoveCause();
        }

        if (serverplayer != null) {
            serverplayer.awardStat(Stats.ANIMALS_BRED);
            CriteriaTriggers.BRED_ANIMALS.trigger(serverplayer, this.dragon, this.partner, null);
        }
    }

    @Nullable
    private BlockPos findEggLayingPosition(T female) {
        BlockPos startPos = female.blockPosition();

        BlockPos groundPos = this.findGroundBelow(startPos, 5);
        if (groundPos != null && this.isValidEggPosition(groundPos)) {
            return groundPos;
        }

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

        return null;
    }

    @Nullable
    private BlockPos findGroundBelow(BlockPos pos, int maxDepth) {
        for (int i = 0; i <= maxDepth; i++) {
            BlockPos checkPos = pos.below(i);
            BlockState groundState = this.level.getBlockState(checkPos);
            BlockState aboveState = this.level.getBlockState(checkPos.above());

            if (!groundState.isAir() && groundState.isSolidRender(this.level, checkPos)
                    && aboveState.isAir()) {
                return checkPos.above();
            }
        }
        return null;
    }

    private boolean isValidEggPosition(BlockPos pos) {
        BlockState state = this.level.getBlockState(pos);
        return state.isAir() && !this.level.getBlockState(pos.below()).isAir();
    }
}
