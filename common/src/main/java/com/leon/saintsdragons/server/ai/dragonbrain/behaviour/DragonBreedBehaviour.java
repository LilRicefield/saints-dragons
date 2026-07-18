package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonLocomotionMode;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.dragons.util.DragonBreedingRules;
import com.leon.saintsdragons.server.entity.interfaces.SemiAquaticDragon;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class DragonBreedBehaviour<T extends DragonEntity> extends DragonBehaviour<T> {
    private static final double RIDEABLE_BREED_MOVE_SPEED = 0.55D;
    private static final double CONTACT_BREED_DISTANCE = 2.0D;
    private static final double MAX_CENTER_BREED_DISTANCE_SQR = 16.0D;
    private static final int EGG_SEARCH_RADIUS = 4;
    private static final int EGG_SEARCH_DEPTH = 8;

    protected final double speedModifier;
    private final Class<T> partnerClass;
    private final double partnerRange;
    private final double breedDistanceSqr;
    @Nullable
    protected T partner;
    protected int loveTime;
    private boolean suspendedSitCommand;
    private int landingRetryTicks;
    private String startState = "not_checked";
    private String mateState = "not_checked";
    private int nearbyCandidates;
    private double nearestCandidateDistance = -1.0D;

    public DragonBreedBehaviour(double speedModifier,
                                Class<T> partnerClass,
                                double partnerRange,
                                double breedDistanceSqr) {
        this.speedModifier = speedModifier;
        this.partnerClass = partnerClass;
        this.partnerRange = partnerRange;
        this.breedDistanceSqr = breedDistanceSqr;
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        if (!breedingAllowed(dragon)) {
            startState = selfRejection(dragon);
            partner = null;
            return false;
        }
        startState = "eligible";
        partner = findMate(context.level(), dragon);
        if (partner == null) {
            startState = "no_eligible_mate";
        }
        return partner != null;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        return partner != null && partner.isAlive() && partner.isInLove()
                && breedingAllowed(partner)
                && breedingAllowed(context.dragon())
                && (!context.dragon().isOrderedToSit() || suspendedSitCommand);
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        suspendedSitCommand = dragon.isOrderedToSit() && dragon.getCommand() == 1;
        if (suspendedSitCommand) {
            dragon.setOrderedToSit(false);
        }
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        if (partner == null) {
            return;
        }
        dragon.getLookControl().setLookAt(partner, 10.0F, dragon.getMaxHeadXRot());
        boolean close = closeEnough(dragon);
        if (close && dragon instanceof RideableDragonBase rideable && rideable.isAerial()) {
            if (landingRetryTicks > 0) {
                landingRetryTicks--;
            }
            if (!rideable.getAIMovement().isPathing() && landingRetryTicks <= 0) {
                rideable.getAIMovement().trySetLandingWaypoint(partner, speedModifier);
                landingRetryTicks = 20;
            }
        } else if (close) {
            stopMovement(dragon);
        } else {
            double speed = speedModifier;
            if (dragon instanceof RideableDragonBase rideable) {
                if (dragon.getLocomotionMode() == DragonLocomotionMode.GROUND) {
                    speed = Math.min(speed, RIDEABLE_BREED_MOVE_SPEED);
                } else if (dragon.getLocomotionMode() == DragonLocomotionMode.WATER
                        && dragon instanceof SemiAquaticDragon swimmer) {
                    speed *= swimmer.getSwimSpeed();
                }
                rideable.getAIMovement().setWaypoint(partner, speed, false);
            } else {
                dragon.getNavigation().moveTo(partner, speed);
            }
        }
        loveTime = Math.min(60, loveTime + 1);
        if (loveTime >= 60 && close && readyToBreed(dragon)) {
            breed(context.level(), dragon);
        }
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        stopMovement(dragon);
        if (partner != null) {
            stopMovement(partner);
        }
        if (suspendedSitCommand && dragon.getCommand() == 1) {
            dragon.setOrderedToSit(true);
        }
        suspendedSitCommand = false;
        landingRetryTicks = 0;
        partner = null;
        loveTime = 0;
    }

    protected boolean breedingAllowed(T dragon) {
        return DragonBreedingRules.isEnabled()
                && dragon.isTame()
                && dragon.isInLove()
                && (!dragon.isInWaterOrBubble() || canBreedInWater(dragon));
    }

    protected boolean canBreedInWater(T dragon) {
        return false;
    }

    protected boolean readyToBreed(T dragon) {
        if (partner == null) {
            return false;
        }
        if (dragon instanceof RideableDragonBase rideable && rideable.isAerial()) {
            return false;
        }
        return !(partner instanceof RideableDragonBase rideablePartner) || !rideablePartner.isAerial();
    }

    @Nullable
    protected T findMate(ServerLevel level, T dragon) {
        List<T> candidates = level.getEntitiesOfClass(
                partnerClass,
                dragon.getBoundingBox().inflate(partnerRange),
                candidate -> candidate != dragon && candidate.isAlive()
        );
        nearbyCandidates = candidates.size();
        nearestCandidateDistance = -1.0D;
        mateState = candidates.isEmpty() ? "none_in_range" : "no_eligible_mate";
        T closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (T candidate : candidates) {
            double distance = dragon.distanceToSqr(candidate);
            double candidateDistance = Math.sqrt(distance);
            if (nearestCandidateDistance < 0.0D || candidateDistance < nearestCandidateDistance) {
                nearestCandidateDistance = candidateDistance;
                mateState = mateRejection(dragon, candidate);
            }
            if (dragon.canMate(candidate) && breedingAllowed(candidate)) {
                if (distance < closestDistance) {
                    closest = candidate;
                    closestDistance = distance;
                    mateState = "eligible";
                }
            }
        }
        return closest;
    }

    private String selfRejection(T dragon) {
        if (!DragonBreedingRules.isEnabled()) {
            return "breeding_disabled";
        }
        if (!dragon.isTame()) {
            return "not_tamed";
        }
        if (!dragon.isInLove()) {
            return "not_in_love";
        }
        if (dragon.isInWaterOrBubble() && !canBreedInWater(dragon)) {
            return "wrong_medium";
        }
        return "ineligible";
    }

    private String mateRejection(T dragon, T candidate) {
        if (!candidate.isTame()) {
            return "mate_not_tamed";
        }
        if (!candidate.isInLove()) {
            return "mate_not_in_love";
        }
        if (candidate.isBaby()) {
            return "mate_is_baby";
        }
        if (candidate.getAge() != 0) {
            return "mate_age=" + candidate.getAge();
        }
        if (dragon.isFemale() == candidate.isFemale()) {
            return "same_sex";
        }
        if (!breedingAllowed(candidate)) {
            return "mate_ineligible";
        }
        if (!dragon.canMate(candidate)) {
            return "can_mate_false";
        }
        return "eligible";
    }

    protected boolean closeEnough(T dragon) {
        if (partner == null) {
            return false;
        }
        if (dragon.distanceToSqr(partner) <= Math.min(breedDistanceSqr, MAX_CENTER_BREED_DISTANCE_SQR)) {
            return true;
        }
        return dragon.getBoundingBox().inflate(CONTACT_BREED_DISTANCE, 0.5D, CONTACT_BREED_DISTANCE)
                .intersects(partner.getBoundingBox());
    }

    protected void breed(ServerLevel level, T dragon) {
        if (partner == null) {
            return;
        }
        if (!DragonBreedingRules.isEnabled()) {
            dragon.resetLove();
            partner.resetLove();
            return;
        }
        T female = dragon.isFemale() ? dragon : partner;
        T male = dragon.isFemale() ? partner : dragon;
        BlockPos eggPos = findEggLayingPosition(level, female);
        BlockState eggState = female.getEggBlockState();
        if (eggPos == null || eggState == null) {
            return;
        }
        dragon.resetLove();
        partner.resetLove();
        if (dragon.getAge() == 0) dragon.setAge(6000);
        if (partner.getAge() == 0) partner.setAge(6000);
        if (eggState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            eggState = eggState.setValue(BlockStateProperties.WATERLOGGED,
                    level.getFluidState(eggPos).getType() == Fluids.WATER);
        }
        level.setBlock(eggPos, eggState, 3);
        BlockEntity blockEntity = level.getBlockEntity(eggPos);
        if (blockEntity != null) {
            female.configureEggBlockEntity(blockEntity, male);
        }
        level.playSound(null, eggPos, SoundEvents.TURTLE_LAY_EGG, SoundSource.BLOCKS, 0.8F, 1.0F);
        level.broadcastEntityEvent(female, (byte)18);
        if (level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            level.addFreshEntity(new ExperienceOrb(level, female.getX(), female.getY(), female.getZ(),
                    dragon.getRandom().nextInt(7) + 1));
        }
        ServerPlayer player = dragon.getLoveCause();
        if (player == null) player = partner.getLoveCause();
        if (player != null) {
            player.awardStat(Stats.ANIMALS_BRED);
            CriteriaTriggers.BRED_ANIMALS.trigger(player, dragon, partner, null);
        }
        stopMovement(female);
        stopMovement(male);
    }

    @Nullable
    protected BlockPos findEggLayingPosition(ServerLevel level, T female) {
        BlockPos midpoint = partner == null ? female.blockPosition() : BlockPos.containing(
                (female.getX() + partner.getX()) * 0.5D,
                Math.min(female.getY(), partner.getY()),
                (female.getZ() + partner.getZ()) * 0.5D);
        BlockPos result = findNearestEggPosition(level, midpoint);
        return result != null ? result : findNearestEggPosition(level, female.blockPosition());
    }

    @Nullable
    private BlockPos findNearestEggPosition(ServerLevel level, BlockPos center) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int radius = 0; radius <= EGG_SEARCH_RADIUS; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (radius > 0 && Math.max(Math.abs(x), Math.abs(z)) != radius) continue;
                    BlockPos ground = findGroundBelow(level, center.offset(x, 0, z));
                    if (ground == null || !validEggPosition(level, ground)) continue;
                    double distance = ground.distSqr(center);
                    if (distance < bestDistance) {
                        best = ground;
                        bestDistance = distance;
                    }
                }
            }
        }
        return best;
    }

    @Nullable
    private BlockPos findGroundBelow(ServerLevel level, BlockPos position) {
        for (int depth = 0; depth <= EGG_SEARCH_DEPTH; depth++) {
            BlockPos check = position.below(depth);
            BlockState ground = level.getBlockState(check);
            if (!ground.isAir() && ground.isSolidRender(level, check) && level.getBlockState(check.above()).isAir()) {
                return check.above();
            }
        }
        return null;
    }

    private boolean validEggPosition(ServerLevel level, BlockPos position) {
        return level.getBlockState(position).isAir() && !level.getBlockState(position.below()).isAir();
    }

    protected void stopMovement(DragonEntity dragon) {
        if (dragon instanceof RideableDragonBase rideable) {
            rideable.getAIMovement().stop();
            rideable.setGroundMoveStateFromAI(0);
            rideable.setRunning(false);
        } else {
            dragon.getNavigation().stop();
        }
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        return Map.of(
                "partner", partner == null ? "none" : partner.getName().getString(),
                "love_time", Integer.toString(loveTime),
                "start_state", startState,
                "mate_state", mateState,
                "nearby_candidates", Integer.toString(nearbyCandidates),
                "nearest_candidate_distance", nearestCandidateDistance < 0.0D
                        ? "none"
                        : Double.toString(nearestCandidateDistance)
        );
    }
}
