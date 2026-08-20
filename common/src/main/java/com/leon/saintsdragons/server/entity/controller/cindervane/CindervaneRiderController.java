package com.leon.saintsdragons.server.entity.controller.cindervane;

import com.leon.saintsdragons.server.entity.controller.DragonRiderControllerHelper;
import com.leon.saintsdragons.server.flight.DragonRiderFlightController;
import com.leon.saintsdragons.server.flight.DragonRiderFlightSettings;
import com.leon.saintsdragons.server.flight.DragonRiderSeat;
import com.leon.saintsdragons.server.flight.DragonRiderSeatOffsets;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record CindervaneRiderController(Cindervane dragon) {

    private static final double AUTO_GRAB_BASE_FACTOR = 0.05D;
    private static final double SEAT0_HEIGHT_ADJUST = 0.00D;
    private static final double SEAT1_HEIGHT_ADJUST = 0.00D;
    private static final double AUTO_GRAB_HEIGHT_ADJUST = 0.00D;
    private static final double BASE_FLIGHT_SPEED_MULT = 3.0;
    private static final double SPRINT_FLIGHT_SPEED_MULT = 4.0;
    private static final double DRAG_NO_INPUT = 0.45;
    private static final double STRAFE_POWER = 0.4;
    private static final double ASCEND_THRUST = 0.45D;
    private static final double DESCEND_THRUST = 0.85D;
    private static final double TERMINAL_VELOCITY = 1.2D;
    private static final double FLIGHT_ACCELERATION = 0.35D;
    private static final double DIVE_SPEED_MULTIPLIER = 2.75D;
    private static final double DIVE_ACCELERATION = 0.30D;

    @Nullable
    public Player getRidingPlayer() {
        return DragonRiderControllerHelper.getRidingPlayer(dragon);
    }

    public Vec3 getRiddenInput(Player player, @SuppressWarnings("unused") Vec3 deltaIn) {
        return DragonRiderControllerHelper.riddenInput(player, dragon.isFlying(), 0.4D, 0.7D, 0.3D, 0.8D);
    }

    public void tickRidden(Player player, @SuppressWarnings("unused") Vec3 travelVector) {
        DragonRiderControllerHelper.clearRiderFallAndTarget(dragon, player);
        DragonRiderControllerHelper.syncYawToRider(dragon, player, 0.30F, 0.25F);
        if (dragon.onGround()) {
            player.fallDistance = 0.0F;
            dragon.fallDistance = 0.0F;
        }
    }


    public float getRiddenSpeed(@SuppressWarnings("unused") Player rider) {
        if (dragon.isFlying()) {
            return (float) dragon.getAttributeValue(Attributes.FLYING_SPEED);
        } else {
            boolean running = dragon.isAccelerating();
            dragon.setRunning(running);
            float base = (float) (running ? Cindervane.RIDER_RUN_SPEED : Cindervane.RIDER_WALK_SPEED);
            return base * dragon.getHappinessSpeedMultiplier();
        }
    }

    public void handleRiderMovement(Player player, Vec3 motion) {
        if (dragon.getNavigation().getPath() != null) {
            dragon.getNavigation().stop();
        }

        if (dragon.isFlying()) {
            DragonRiderFlightController.tick(
                    dragon,
                    player,
                    motion,
                    DragonRiderControllerHelper.resolveRiderPitchRadians(dragon, player, Cindervane.RIDER_KEY_PITCH_DEG),
                    dragon.isRiderPitchKeyMode(),
                    flightSettings(),
                    dragon.getRiderTakeoffTicks() > 0,
                    false,
                    0.45D
            );
        }
    }

    private DragonRiderFlightSettings flightSettings() {
        double baseSpeed = dragon.getAttributeValue(Attributes.FLYING_SPEED);
        return new DragonRiderFlightSettings(
                baseSpeed * BASE_FLIGHT_SPEED_MULT,
                baseSpeed * SPRINT_FLIGHT_SPEED_MULT,
                FLIGHT_ACCELERATION,
                DIVE_SPEED_MULTIPLIER,
                DIVE_ACCELERATION,
                STRAFE_POWER,
                DRAG_NO_INPUT,
                ASCEND_THRUST,
                DESCEND_THRUST,
                TERMINAL_VELOCITY,
                ASCEND_THRUST * 0.85D
        );
    }

    public double getPassengersRidingOffset() {
        return DragonRiderSeatOffsets.cindervane(0).y;
    }

    private double getAutoGrabRidingOffset() {
        return (double) dragon.getBbHeight() * AUTO_GRAB_BASE_FACTOR;
    }
    
    public void positionRider(@NotNull Entity passenger, Entity.@NotNull MoveFunction moveFunction) {
        if (!dragon.hasPassenger(passenger)) return;

        if (dragon.isSlashGrabPassenger(passenger)) {
            final String locatorName = "automountBoneRight";
            Vec3 passengerLoc = dragon.getBonePositionForPassenger(locatorName);

            if (passengerLoc != null) {
                DragonRiderSeat.positionLocatorRider(
                        dragon,
                        passenger,
                        moveFunction,
                        getAutoGrabRidingOffset() + AUTO_GRAB_HEIGHT_ADJUST,
                        passengerLoc,
                        AUTO_GRAB_HEIGHT_ADJUST
                );
            } else {
                float yawRad = (float) Math.toRadians(dragon.getYRot());
                double localX = 0.95D;
                double localZ = -0.15D;
                double worldX = localX * Math.cos(yawRad) - localZ * Math.sin(yawRad);
                double worldZ = localX * Math.sin(yawRad) + localZ * Math.cos(yawRad);

                double x = dragon.getX() + worldX;
                double y = dragon.getY() + getAutoGrabRidingOffset() + AUTO_GRAB_HEIGHT_ADJUST + passenger.getMyRidingOffset();
                double z = dragon.getZ() + worldZ;
                moveFunction.accept(passenger, x, y, z);
            }
            return;
        }

        var passengers = dragon.getPassengers();
        int seatIndex = passengers.indexOf(passenger);

        if (seatIndex == -1) return; // Passenger not found
        final double seatHeightAdjust = seatIndex == 0 ? SEAT0_HEIGHT_ADJUST : SEAT1_HEIGHT_ADJUST;
        Vec3 passengerLoc = null;
        if (dragon.level().isClientSide) {
            passengerLoc = dragon.getClientLocatorPosition(seatIndex == 0 ? "passengerSeat0" : "passengerSeat1");
            if (passengerLoc == null && seatIndex == 0) {
                passengerLoc = dragon.getClientLocatorPosition("passengerLocator");
            }
            if (passengerLoc != null && seatHeightAdjust != 0.0D) {
                passengerLoc = passengerLoc.add(0.0D, seatHeightAdjust, 0.0D);
            }
        }
        DragonRiderSeat.positionAnimatedRider(
                dragon,
                passenger,
                moveFunction,
                DragonRiderSeatOffsets.cindervane(seatIndex).add(0.0D, seatHeightAdjust, 0.0D),
                passengerLoc
        );
    }
    
    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger) {
        Vec3 base = dragon.position();

        if (dragon.isSlashGrabPassenger(passenger)) {
            // Slash-grab release should always dismount to dragon-right, not vanilla safe-spot left drift.
            float yawRad = (float) Math.toRadians(dragon.getYRot());
            double localX = 1.8D;
            double localZ = 0.1D;
            double worldX = localX * Math.cos(yawRad) - localZ * Math.sin(yawRad);
            double worldZ = localX * Math.sin(yawRad) + localZ * Math.cos(yawRad);
            return new Vec3(
                    base.x + worldX,
                    base.y + getAutoGrabRidingOffset() + 0.2D,
                    base.z + worldZ
            );
        }

        return DragonRiderSeat.findRadialGroundDismount(
                passenger,
                dragon,
                new double[] { 2.5D, 3.5D, 1.8D },
                new int[] { 0, 30, -30, 60, -60, 90, -90, 150, -150, 180 },
                6,
                2.0D
        );
    }
    
    @Nullable
    public LivingEntity getControllingPassenger() {
        // Only seat 0 (first passenger) can control, and only if they're the owner
        var passengers = dragon.getPassengers();
        if (passengers.isEmpty()) {
            return null;
        }

        Entity firstPassenger = passengers.get(0);
        if (firstPassenger instanceof Player player && dragon.isTame() && dragon.isOwnedBy(player)) {
            return player;
        }
        return null;
    }
    public void requestRiderTakeoff() {
        dragon.tryRiderTakeoff(getControllingPassenger() instanceof Player player ? player : null);
    }
}
