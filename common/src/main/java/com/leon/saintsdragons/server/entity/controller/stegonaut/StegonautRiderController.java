package com.leon.saintsdragons.server.entity.controller.stegonaut;

import com.leon.saintsdragons.server.entity.controller.GroundDragonRiderControllerHelper;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;


public record StegonautRiderController(Stegonaut drake) {
    private static final double SEAT_BASE_FACTOR = 0.0D;

    @Nullable
    public Player getRidingPlayer() {
        return GroundDragonRiderControllerHelper.getRidingPlayer(drake);
    }

    public Vec3 getRiddenInput(Player player, @SuppressWarnings("unused") Vec3 deltaIn) {
        return GroundDragonRiderControllerHelper.standardGroundInput(player);
    }

    public void tickRidden(Player player, @SuppressWarnings("unused") Vec3 travelVector) {
        GroundDragonRiderControllerHelper.tickStandardGroundRider(drake, player);
    }

    public float getRiddenSpeed(Player player) {
        double speed = drake.isAccelerating() ? Stegonaut.RIDER_RUN_SPEED : Stegonaut.RIDER_WALK_SPEED;
        return (float) (speed * drake.getHappinessSpeedMultiplier());
    }

    public double getPassengersRidingOffset() {
        return (drake.getBbHeight() * SEAT_BASE_FACTOR);
    }

    public void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        GroundDragonRiderControllerHelper.positionLocatorRider(drake, passenger, moveFunction, getPassengersRidingOffset());
    }

    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return GroundDragonRiderControllerHelper.getDismountLocationForPassenger(drake, passenger);
    }

    @Nullable
    public Player getControllingPassenger() {
        return GroundDragonRiderControllerHelper.getOwnedControllingPassenger(drake);
    }
}
