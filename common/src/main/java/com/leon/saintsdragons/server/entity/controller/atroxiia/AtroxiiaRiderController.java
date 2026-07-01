package com.leon.saintsdragons.server.entity.controller.atroxiia;

import com.leon.saintsdragons.server.entity.controller.GroundDragonRiderControllerHelper;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public record AtroxiiaRiderController(Atroxiia dragon) {
    private static final double SEAT_BASE_FACTOR = 0.0D;

    @Nullable
    public Player getRidingPlayer() {
        return GroundDragonRiderControllerHelper.getRidingPlayer(dragon);
    }

    public Vec3 getRiddenInput(Player player, @SuppressWarnings("unused") Vec3 deltaIn) {
        return GroundDragonRiderControllerHelper.standardGroundInput(player);
    }

    public void tickRidden(Player player, @SuppressWarnings("unused") Vec3 travelVector) {
        GroundDragonRiderControllerHelper.tickStandardGroundRider(dragon, player);
    }

    public float getRiddenSpeed(Player player) {
        double speed = dragon.isAccelerating() ? Atroxiia.RIDER_RUN_SPEED : Atroxiia.RIDER_WALK_SPEED;
        return (float) (speed * dragon.getHappinessSpeedMultiplier());
    }

    public double getPassengersRidingOffset() {
        return dragon.getBbHeight() * SEAT_BASE_FACTOR;
    }

    public void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        GroundDragonRiderControllerHelper.positionLocatorRider(dragon, passenger, moveFunction, getPassengersRidingOffset());
    }

    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return GroundDragonRiderControllerHelper.getDismountLocationForPassenger(dragon, passenger);
    }

    @Nullable
    public Player getControllingPassenger() {
        return GroundDragonRiderControllerHelper.getOwnedControllingPassenger(dragon);
    }
}
