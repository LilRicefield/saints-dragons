package com.leon.saintsdragons.server.entity.controller.varasuchus;

import com.leon.saintsdragons.server.entity.controller.GroundDragonRiderControllerHelper;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public record VarasuchusRiderController(Varasuchus drake) {
    private static final double SEAT_BASE_FACTOR = 0.45D; // 0.0..1.0 of bbHeight

    @Nullable
    public Player getRidingPlayer() {
        return GroundDragonRiderControllerHelper.getRidingPlayer(drake);
    }

    public Vec3 getRiddenInput(Player player, @SuppressWarnings("unused") Vec3 deltaIn) {
        float f = player.zza < 0.0F ? 0.5F : 1.0F;

        if (drake.isInWater()) {
            // Aquatic movement - enhanced responsiveness in water
            return new Vec3(player.xxa * 0.6F, 0.0F, player.zza * 1.0F * f);
        } else {
            return GroundDragonRiderControllerHelper.standardGroundInput(player);
        }
    }

    public void tickRidden(Player player, @SuppressWarnings("unused") Vec3 travelVector) {
        GroundDragonRiderControllerHelper.tickStandardGroundRider(drake, player);
    }
    public void handleRiderMovement(Player player, Vec3 motion) {
        throw new UnsupportedOperationException("handleRiderMovement should not be called for ground-based dragons");
    }

    public float getRiddenSpeed(Player player) {
        if (drake.isInWater()) {
            double baseSpeed = drake.getSwimSpeed();
            double speed = drake.isAccelerating() ? baseSpeed * 1.3D : baseSpeed;
            return (float) speed;
        } else {
            double speed = drake.isAccelerating() ? Varasuchus.RIDER_RUN_SPEED : Varasuchus.RIDER_WALK_SPEED;
            return (float) (speed * drake.getHappinessSpeedMultiplier());
        }
    }
    public double getPassengersRidingOffset() {
        return drake.getBbHeight() * SEAT_BASE_FACTOR;
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
