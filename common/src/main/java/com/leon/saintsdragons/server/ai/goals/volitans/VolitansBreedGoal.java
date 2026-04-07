package com.leon.saintsdragons.server.ai.goals.volitans;

import com.leon.saintsdragons.server.ai.goals.base.DragonBreedGoal;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class VolitansBreedGoal extends DragonBreedGoal<Volitans> {
    private static final int NEST_SEARCH_RADIUS = 8;
    private static final int NEST_SEARCH_DEPTH = 12;

    private double currentYaw;
    private double currentPitch;

    public VolitansBreedGoal(Volitans dragon, double speedModifier, double partnerRange, double breedDistanceSqr) {
        super(dragon, speedModifier, Volitans.class, partnerRange, breedDistanceSqr);
        this.currentYaw = dragon.getYRot();
        this.currentPitch = dragon.getXRot();
    }

    @Override
    public void start() {
        super.start();
        this.currentYaw = dragon.getYRot();
        this.currentPitch = dragon.getXRot();
    }

    @Override
    public void stop() {
        super.stop();
        Vec3 vel = dragon.getDeltaMovement();
        if (dragon.isInWaterOrBubble()) {
            dragon.setDeltaMovement(vel.x * 0.8D, vel.y * 0.8D, vel.z * 0.8D);
        }
    }

    @Override
    public void tick() {
        if (this.partner == null) {
            return;
        }

        ++this.loveTime;

        if (this.dragon.isInWaterOrBubble() && this.partner.isInWaterOrBubble()) {
            swimTowardPartner();
        } else if (!this.dragon.isFlying()) {
            double speed = this.speedModifier;
            this.dragon.setGroundMoveStateFromAI(1);
            this.dragon.setRunning(false);
            this.dragon.getNavigation().moveTo(this.partner, speed);
        }

        if (this.loveTime >= 60
                && this.dragon.distanceToSqr(this.partner) < this.breedDistanceSqr) {
            Volitans female = this.dragon.isFemale() ? this.dragon : this.partner;
            BlockPos eggPos = findEggLayingPosition(female);
            if (eggPos == null) {
                return;
            }
            handleBreed();
        }
    }

    @Override
    protected boolean isBreedingAllowed() {
        return super.isBreedingAllowed()
                && dragon.isInWaterOrBubble()
                && !dragon.isBurrowing()
                && !dragon.isSleepLocked()
                && !dragon.isVehicle();
    }

    @Override
    protected @Nullable Volitans findMate() {
        Volitans mate = super.findMate();
        return mate != null && mate.isInWaterOrBubble() ? mate : null;
    }

    @Override
    protected @Nullable BlockPos findEggLayingPosition(Volitans female) {
        if (!female.isInWaterOrBubble()) {
            return null;
        }

        BlockPos midpoint = BlockPos.containing(
                (female.getX() + this.partner.getX()) * 0.5D,
                (female.getY() + this.partner.getY()) * 0.5D,
                (female.getZ() + this.partner.getZ()) * 0.5D
        );
        BlockPos aroundMidpoint = findUnderwaterEggLayingPosition(midpoint);
        if (aroundMidpoint != null) {
            return aroundMidpoint;
        }

        return findUnderwaterEggLayingPosition(female.blockPosition());
    }

    private void swimTowardPartner() {
        swimTowardPosition(partner.position().add(0.0D, partner.getBbHeight() * 0.35D, 0.0D));
    }

    private void swimTowardPosition(Vec3 targetPos) {
        dragon.getNavigation().stop();

        double dx = targetPos.x - dragon.getX();
        double dy = targetPos.y - (dragon.getY() + dragon.getBbHeight() * 0.35D);
        double dz = targetPos.z - dragon.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        double targetYaw = Math.atan2(dz, dx) * Mth.RAD_TO_DEG - 90.0D;
        double targetPitch = -(Math.atan2(dy, horizontalDist) * Mth.RAD_TO_DEG);
        targetPitch = Mth.clamp(targetPitch, -60.0D, 60.0D);

        double yawDelta = Mth.wrapDegrees(targetYaw - currentYaw);
        yawDelta = Mth.clamp(yawDelta, -8.0D, 8.0D);
        currentYaw = Mth.wrapDegrees(currentYaw + yawDelta);

        double pitchDelta = targetPitch - currentPitch;
        pitchDelta = Mth.clamp(pitchDelta, -4.0D, 4.0D);
        currentPitch += pitchDelta;

        dragon.setYRot((float) currentYaw);
        dragon.yBodyRot = (float) currentYaw;
        dragon.yHeadRot = (float) currentYaw;
        dragon.setXRot((float) currentPitch);

        double yawRad = currentYaw * Mth.DEG_TO_RAD;
        double pitchRad = currentPitch * Mth.DEG_TO_RAD;
        double dirX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double dirY = -Math.sin(pitchRad);
        double dirZ = Math.cos(yawRad) * Math.cos(pitchRad);

        double speed = dragon.getSwimSpeed() * Math.min(this.speedModifier, 0.20D);
        if (dragon.distanceToSqr(partner) < 25.0D) {
            speed *= 0.7D;
        }

        dragon.setDeltaMovement(dirX * speed, dirY * speed, dirZ * speed);
    }

    private @Nullable BlockPos findUnderwaterEggLayingPosition(BlockPos startPos) {
        BlockPos bestPos = null;
        double bestDistance = Double.MAX_VALUE;

        for (int xOffset = -NEST_SEARCH_RADIUS; xOffset <= NEST_SEARCH_RADIUS; xOffset++) {
            for (int zOffset = -NEST_SEARCH_RADIUS; zOffset <= NEST_SEARCH_RADIUS; zOffset++) {
                BlockPos columnStart = startPos.offset(xOffset, 0, zOffset);
                BlockPos pos = findUnderwaterFloor(columnStart, NEST_SEARCH_DEPTH);
                if (pos != null) {
                    double distance = pos.distSqr(startPos);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestPos = pos;
                    }
                }
            }
        }

        return bestPos;
    }

    private @Nullable BlockPos findUnderwaterFloor(BlockPos startPos, int maxDepth) {
        for (int depth = 0; depth <= maxDepth; depth++) {
            BlockPos floorPos = startPos.below(depth);
            BlockPos eggPos = floorPos.above();
            BlockState floorState = level.getBlockState(floorPos);
            BlockState eggState = level.getBlockState(eggPos);

            if (!level.hasChunkAt(floorPos)) {
                continue;
            }

            if (!floorState.isAir()
                    && floorState.isSolidRender(level, floorPos)
                    && eggState.getFluidState().is(FluidTags.WATER)) {
                return eggPos;
            }
        }
        return null;
    }
}
