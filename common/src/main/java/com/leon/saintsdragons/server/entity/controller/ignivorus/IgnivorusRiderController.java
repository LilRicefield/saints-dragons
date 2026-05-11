package com.leon.saintsdragons.server.entity.controller.ignivorus;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.ignivorus.IgnivorusFireBreathAbility;
import com.leon.saintsdragons.server.entity.controller.DragonRiderControllerHelper;
import com.leon.saintsdragons.server.flight.DragonRiderSeat;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record IgnivorusRiderController(Ignivorus dragon) {

    private static final double SEAT_BASE_FACTOR = 0.50D;
    private static final double LANDING_HEIGHT_TRIGGER = 4.0D;
    private static final int MAX_GROUND_CHECK_DISTANCE = 10;
    private static final double CRUISE_SPEED_MULT = 3.95;
    private static final double SPRINT_SPEED_MULT = 4.75;
    private static final double DRAG_NO_INPUT = 0.5;
    private static final double STRAFE_POWER = 0.5;
    private static final double ASCEND_THRUST = 0.45D;
    private static final double DESCEND_THRUST = 1.0D;
    private static final double TERMINAL_VELOCITY = 1.5D;

    @Nullable
    public Player getRidingPlayer() {
        return DragonRiderControllerHelper.getRidingPlayer(dragon);
    }

    public Vec3 getRiddenInput(Player player, Vec3 deltaIn) {
        return DragonRiderControllerHelper.riddenInput(player, dragon.isFlying(), 0.5D, 0.9D, 0.4D, 1.0D);
    }

    public void tickRidden(Player player, Vec3 travelVector) {
        DragonRiderControllerHelper.clearRiderFallAndTarget(dragon, player);
        DragonRiderControllerHelper.syncYawToRider(dragon, player, 0.35F, 0.28F);
        if (dragon.isFlying() && !isTakeoffWindowActive()) {
            double distanceToGround = getDistanceToGround();
            boolean nearGround = distanceToGround >= 0 && distanceToGround <= LANDING_HEIGHT_TRIGGER;

            if (nearGround && dragon.isGoingDown() && !dragon.isLanding()) {
                dragon.setLanding(true);
            }

            if (dragon.onGround()) {
                dragon.setFlying(false);
                dragon.setLanding(false);
                dragon.setTakeoff(false);
            }
        }

        if (dragon.onGround()) {
            player.fallDistance = 0.0F;
            dragon.fallDistance = 0.0F;
        }
    }

    private double getDistanceToGround() {
        var level = dragon.level();
        if (level == null) return -1;

        final net.minecraft.world.phys.AABB box = dragon.getBoundingBox();
        final int minBuildY = level.getMinBuildHeight();
        final double[] sampleX = {dragon.getX(), box.minX + 0.25D, box.maxX - 0.25D};
        final double[] sampleZ = {dragon.getZ(), box.minZ + 0.25D, box.maxZ - 0.25D};

        double bestDistance = Double.POSITIVE_INFINITY;
        boolean foundGround = false;

        for (double sx : sampleX) {
            for (double sz : sampleZ) {
                int x = net.minecraft.util.Mth.floor(sx);
                int z = net.minecraft.util.Mth.floor(sz);
                int startY = net.minecraft.util.Mth.floor(box.minY);
                int stopY = Math.max(minBuildY, startY - MAX_GROUND_CHECK_DISTANCE);

                for (int y = startY; y >= stopY; y--) {
                    net.minecraft.core.BlockPos checkPos = new net.minecraft.core.BlockPos(x, y, z);
                    if (!level.hasChunkAt(checkPos)) {
                        continue;
                    }

                    net.minecraft.world.level.block.state.BlockState state = level.getBlockState(checkPos);
                    if (!state.getFluidState().isEmpty()) {
                        return -1;
                    }

                    net.minecraft.world.phys.shapes.VoxelShape shape = state.getCollisionShape(level, checkPos);
                    if (shape.isEmpty()) {
                        continue;
                    }

                    double topY = y + shape.max(net.minecraft.core.Direction.Axis.Y);
                    double distance = box.minY - topY;
                    if (distance < bestDistance) {
                        bestDistance = distance;
                    }
                    foundGround = true;
                    break;
                }
            }
        }

        if (!foundGround) {
            return -1;
        }
        return Math.max(0.0D, bestDistance);
    }

    public float getRiddenSpeed(Player rider) {
        if (dragon.isFlying()) {
            return (float) dragon.getAttributeValue(Attributes.FLYING_SPEED);
        }

        boolean isMoving = dragon.getDeltaMovement().horizontalDistanceSqr() > 0.0001;

        boolean isPhase2 = dragon.getEntityData().get(Ignivorus.DATA_PHASE2);

        if (dragon.isAccelerating() && isMoving) {
            dragon.setRunning(true);
            float base = (float) (isPhase2 ? Ignivorus.RIDER_PHASE2_RUN_SPEED : Ignivorus.RIDER_RUN_SPEED);
            return base * dragon.getHappinessSpeedMultiplier();
        } else {
            dragon.setRunning(false);
            float base = (float) (isPhase2 ? Ignivorus.RIDER_PHASE2_WALK_SPEED : Ignivorus.RIDER_WALK_SPEED);
            return base * dragon.getHappinessSpeedMultiplier();
        }
    }

    public void handleRiderMovement(Player player, Vec3 motion) {
        if (dragon.getNavigation().getPath() != null) {
            dragon.getNavigation().stop();
        }

        if (dragon.isFlying()) {
            Vec3 finalVelocity = DragonRiderControllerHelper.computeFlightVelocity(
                    dragon,
                    player,
                    motion,
                    getEffectivePitchRadians(player),
                    dragon.isRiderPitchKeyMode(),
                    dragon.getAttributeValue(Attributes.FLYING_SPEED),
                    CRUISE_SPEED_MULT,
                    SPRINT_SPEED_MULT,
                    STRAFE_POWER,
                    DRAG_NO_INPUT,
                    ASCEND_THRUST,
                    DESCEND_THRUST,
                    TERMINAL_VELOCITY,
                    ASCEND_THRUST * 0.65D,
                    isTakeoffWindowActive(),
                    true,
                    0.20D
            );
            dragon.move(MoverType.SELF, finalVelocity);
            dragon.setDeltaMovement(finalVelocity);
            dragon.calculateEntityAnimation(true);

            player.fallDistance = 0.0F;
            dragon.fallDistance = 0.0F;
        }
    }

    private float getEffectivePitchRadians(Player player) {
        DragonAbility<?> ability = dragon.getActiveAbility();
        boolean lockPitch = dragon.isBreathingFire()
                || (ability instanceof IgnivorusFireBreathAbility && ability.isUsing());
        if (lockPitch) {
            return 0.0f;
        }
        return DragonRiderControllerHelper.resolveRiderPitchRadians(dragon, player, Ignivorus.RIDER_KEY_PITCH_DEG);
    }

    public double getPassengersRidingOffset() {
        return (double) dragon.getBbHeight() * SEAT_BASE_FACTOR;
    }

    public void positionRider(@NotNull Entity passenger, Entity.@NotNull MoveFunction moveFunction) {
        if (!dragon.hasPassenger(passenger)) return;
        DragonRiderSeat.positionLocatorRider(
                dragon,
                passenger,
                moveFunction,
                getPassengersRidingOffset(),
                dragon.level().isClientSide ? dragon.getClientLocatorPosition("passengerLocator") : null
        );
    }

    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger) {
        return DragonRiderSeat.forwardDismount(passenger, dragon, 2.0D);
    }

    @Nullable
    public LivingEntity getControllingPassenger() {
        return DragonRiderControllerHelper.getOwnerControllingPassenger(dragon);
    }

    public void requestRiderTakeoff() {
        dragon.tryRiderTakeoff(getControllingPassenger() instanceof Player player ? player : null);
    }

    private boolean isTakeoffWindowActive() {
        return dragon.isTakeoff() || dragon.timeFlying < Ignivorus.TAKEOFF_ANIMATION_TICKS;
    }
}
