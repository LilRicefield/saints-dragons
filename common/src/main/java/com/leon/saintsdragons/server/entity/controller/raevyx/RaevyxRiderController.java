package com.leon.saintsdragons.server.entity.controller.raevyx;

import com.leon.saintsdragons.server.entity.controller.DragonRiderControllerHelper;
import com.leon.saintsdragons.server.flight.DragonRiderFlightController;
import com.leon.saintsdragons.server.flight.DragonRiderFlightSettings;
import com.leon.saintsdragons.server.flight.DragonRiderSeat;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record RaevyxRiderController(Raevyx wyvern) {
    private static final double SEAT_BASE_FACTOR = 3.50D;
    private static final double SEAT_HEIGHT_ADJUST = 0.00D;
    private static final double BASE_FLIGHT_SPEED_MULT = 4.0;
    private static final double SPRINT_FLIGHT_SPEED_MULT = 6.0;
    private static final double DRAG_NO_INPUT = 0.5;
    private static final double STRAFE_POWER = 0.5;
    private static final double ASCEND_THRUST = 1.2D;
    private static final double DESCEND_THRUST = 1.0D;
    private static final double TERMINAL_VELOCITY = 1.5D;
    private static final double FLIGHT_ACCELERATION = 0.45D;
    private static final double DIVE_SPEED_MULTIPLIER = 3.0D;
    private static final double DIVE_ACCELERATION = 0.35D;

    @Nullable
    public Player getRidingPlayer() {
        return DragonRiderControllerHelper.getRidingPlayer(wyvern);
    }

    public Vec3 getRiddenInput(Player player, @SuppressWarnings("unused") Vec3 deltaIn) {
        return DragonRiderControllerHelper.riddenInput(player, wyvern.isFlying(), 0.5D, 0.9D, 0.4D, 1.0D);
    }

    public void tickRidden(Player player, @SuppressWarnings("unused") Vec3 travelVector) {
        DragonRiderControllerHelper.clearRiderFallAndTarget(wyvern, player);
        DragonRiderControllerHelper.syncYawToRider(wyvern, player, 0.35F, 0.28F);
        if (wyvern.onGround()) {
            player.fallDistance = 0.0F;
            wyvern.fallDistance = 0.0F;
        }
    }

    public float getRiddenSpeed(@SuppressWarnings("unused") Player rider) {
        if (wyvern.isFlying()) {
            return (float) getMountedFlightBaseSpeed();
        } else {
            boolean isMoving = wyvern.getDeltaMovement().horizontalDistanceSqr() > 0.0001;
            boolean running = wyvern.isAccelerating() && isMoving;
            wyvern.setRunning(running);
            float base = (float) (running ? Raevyx.RIDER_RUN_SPEED : Raevyx.RIDER_WALK_SPEED);
            return base * wyvern.getHappinessSpeedMultiplier();
        }
    }

    public void handleRiderMovement(Player player, Vec3 motion) {
        if (wyvern.getNavigation().getPath() != null) {
            wyvern.getNavigation().stop();
        }
        if (wyvern.isFlying()) {
            DragonRiderFlightController.tick(
                    wyvern,
                    player,
                    motion,
                    DragonRiderControllerHelper.resolveRiderPitchRadians(
                            wyvern,
                            player,
                            Raevyx.RIDER_KEY_PITCH_DEG
                    ),
                    wyvern.isRiderPitchKeyMode(),
                    flightSettings(),
                    wyvern.getRiderTakeoffTicks() > 0,
                    false,
                    Double.NEGATIVE_INFINITY
            );
        }
    }

    private DragonRiderFlightSettings flightSettings() {
        double baseSpeed = getMountedFlightBaseSpeed();
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
                ASCEND_THRUST
        );
    }

    private double getMountedFlightBaseSpeed() {
        return wyvern.getFlightSpeed();
    }
    
    public double getPassengersRidingOffset() {
        return (double) wyvern.getBbHeight() * SEAT_BASE_FACTOR;
    }
    
    public void positionRider(@NotNull Entity passenger, Entity.@NotNull MoveFunction moveFunction) {
        DragonRiderSeat.positionLocatorRider(
                wyvern,
                passenger,
                moveFunction,
                getPassengersRidingOffset() + SEAT_HEIGHT_ADJUST,
                wyvern.getClientLocatorPosition("passengerLocator")
        );
    }
    
    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger) {
        return DragonRiderSeat.findRadialGroundDismount(
                passenger,
                wyvern,
                new double[] { 2.5D, 3.5D, 1.8D },
                new int[] { 0, 30, -30, 60, -60, 90, -90, 150, -150, 180 },
                6,
                2.0D
        );
    }
    
    @Nullable 
    public LivingEntity getControllingPassenger() {
        return DragonRiderControllerHelper.getOwnerControllingPassenger(wyvern);
    }

    public void requestRiderTakeoff() {
        wyvern.tryRiderTakeoff(getControllingPassenger() instanceof Player player ? player : null);
    }
}
