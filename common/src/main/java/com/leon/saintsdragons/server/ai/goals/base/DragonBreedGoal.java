package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.dragons.util.DragonBreedingRules;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
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
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;

public class DragonBreedGoal<T extends DragonEntity> extends Goal {
    private static final double RIDEABLE_BREED_MOVE_SPEED = 0.55D;
    private static final double CONTACT_BREED_DISTANCE = 2.0D;
    private static final double MAX_CENTER_BREED_DISTANCE_SQR = 16.0D;
    private static final int EGG_SEARCH_RADIUS = 4;
    private static final int EGG_SEARCH_DEPTH = 8;
    protected final T dragon;
    protected final Level level;
    protected final double speedModifier;
    private final double partnerRange;
    protected final double breedDistanceSqr;
    private final TargetingConditions partnerTargeting;
    private final Class<T> partnerClass;
    @Nullable
    protected T partner;
    protected int loveTime;

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
        prepareBreedingPosture(this.dragon);
        this.partner = findMate();
        if (this.partner != null) {
            prepareBreedingPosture(this.partner);
        }
        return this.partner != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.partner != null) {
            prepareBreedingPosture(this.dragon);
            prepareBreedingPosture(this.partner);
        }
        boolean result = this.partner != null
                && this.partner.isAlive()
                && this.partner.isInLove()
                && this.loveTime < 60
                && isBreedingAllowed();
        return result;
    }

    @Override
    public void stop() {
        stopBreedingMovement(this.dragon);
        if (this.partner != null) {
            stopBreedingMovement(this.partner);
        }
        this.partner = null;
        this.loveTime = 0;
    }

    @Override
    public void tick() {
        if (this.partner == null) {
            return;
        }

        this.dragon.getLookControl().setLookAt(this.partner, 10.0F, (float) this.dragon.getMaxHeadXRot());
        boolean closeEnough = isCloseEnoughToBreed();
        if (closeEnough) {
            stopBreedingMovement(this.dragon);
        } else if (!this.dragon.isFlying()) {
            double speed = this.speedModifier;
            if (this.dragon instanceof RideableDragonBase rideable) {
                speed = Math.min(speed, RIDEABLE_BREED_MOVE_SPEED);
                rideable.setGroundMoveStateFromAI(1);
                rideable.setRunning(false);
                rideable.getAIMovement().moveToGroundTarget(this.partner, speed, false);
            } else {
                this.dragon.getNavigation().moveTo(this.partner, speed);
            }
        }

        ++this.loveTime;

        if (this.loveTime >= 60 && closeEnough) {
            handleBreed();
        }
    }

    protected boolean isCloseEnoughToBreed() {
        if (this.partner == null) {
            return false;
        }

        double configuredDistanceSqr = Math.min(this.breedDistanceSqr, MAX_CENTER_BREED_DISTANCE_SQR);
        if (this.dragon.distanceToSqr(this.partner) <= configuredDistanceSqr) {
            return true;
        }

        return this.dragon.getBoundingBox()
                .inflate(CONTACT_BREED_DISTANCE, 0.5D, CONTACT_BREED_DISTANCE)
                .intersects(this.partner.getBoundingBox());
    }

    protected boolean isBreedingAllowed() {
        return DragonBreedingRules.isEnabled()
                && this.dragon.isInLove()
                && !this.dragon.isFlying();
    }

    private void prepareBreedingPosture(DragonEntity dragon) {
        if (dragon.isOrderedToSit()) {
            dragon.setOrderedToSit(false);
            if (dragon.getCommand() == 1) {
                dragon.setCommand(0);
            }
        }
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
            boolean canMate = this.dragon.canMate(candidate);
            if (canMate) {
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
        if (!DragonBreedingRules.isEnabled()) {
            this.dragon.resetLove();
            if (this.partner != null) {
                this.partner.resetLove();
            }
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
        if (this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
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

        stopBreedingMovement(female);
        stopBreedingMovement(male);
    }

    protected void stopBreedingMovement(DragonEntity dragon) {
        if (dragon instanceof RideableDragonBase rideable) {
            rideable.getAIMovement().stop();
            rideable.setGroundMoveStateFromAI(0);
            rideable.setRunning(false);
        } else {
            dragon.getNavigation().stop();
        }
    }

    @Nullable
    protected BlockPos findEggLayingPosition(T female) {
        BlockPos femalePos = female.blockPosition();
        BlockPos midpoint = this.partner != null
                ? BlockPos.containing(
                (female.getX() + this.partner.getX()) * 0.5D,
                Math.min(female.getY(), this.partner.getY()),
                (female.getZ() + this.partner.getZ()) * 0.5D
        )
                : femalePos;

        BlockPos midpointCandidate = findNearestEggPosition(midpoint);
        if (midpointCandidate != null) {
            return midpointCandidate;
        }

        return findNearestEggPosition(femalePos);
    }

    @Nullable
    private BlockPos findNearestEggPosition(BlockPos center) {
        BlockPos bestPos = null;
        double bestDistSq = Double.MAX_VALUE;

        for (int radius = 0; radius <= EGG_SEARCH_RADIUS; radius++) {
            for (int xOffset = -radius; xOffset <= radius; xOffset++) {
                for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                    if (radius > 0 && Math.max(Math.abs(xOffset), Math.abs(zOffset)) != radius) {
                        continue;
                    }

                    BlockPos offsetPos = center.offset(xOffset, 0, zOffset);
                    BlockPos groundPos = this.findGroundBelow(offsetPos, EGG_SEARCH_DEPTH);
                    if (groundPos == null || !this.isValidEggPosition(groundPos)) {
                        continue;
                    }

                    double distSq = groundPos.distSqr(center);
                    if (distSq < bestDistSq) {
                        bestDistSq = distSq;
                        bestPos = groundPos;
                    }
                }
            }
        }

        return bestPos;
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
