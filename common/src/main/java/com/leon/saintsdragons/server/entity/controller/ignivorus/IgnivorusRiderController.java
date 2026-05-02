package com.leon.saintsdragons.server.entity.controller.ignivorus;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.ignivorus.IgnivorusFireBreathAbility;
import com.leon.saintsdragons.server.flight.DragonRiderFlightPhysics;
import com.leon.saintsdragons.server.flight.DragonRiderSeat;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.util.Mth;
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
        if (dragon.getControllingPassenger() instanceof Player player) {
            return player;
        }
        return null;
    }

    public Vec3 getRiddenInput(Player player, Vec3 deltaIn) {
        float f = player.zza < 0.0F ? 0.5F : 1.0F;
        if (dragon.isFlying()) {
            return new Vec3(player.xxa * 0.4F, 0.0F, player.zza * 1.0F * f);
        } else {
            return new Vec3(player.xxa * 0.5F, 0.0D, player.zza * 0.9F * f);
        }
    }

    public void tickRidden(Player player, Vec3 travelVector) {
        player.fallDistance = 0.0F;
        dragon.fallDistance = 0.0F;
        dragon.setTarget(null);

        boolean flying = dragon.isFlying();
        float currentYaw = dragon.getYRot();
        float targetYaw = player.getYRot();
        float rawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
        float blend = flying ? 0.35f : 0.28f;
        float newYaw = currentYaw + (rawDiff * blend);

        dragon.setYRot(newYaw);
        dragon.yBodyRot = newYaw;
        dragon.yHeadRot = newYaw;
        if (!flying) {
            dragon.setXRot(0.0F);
        }
        if (flying && !isTakeoffWindowActive()) {
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
            final double baseSpeed = dragon.getAttributeValue(Attributes.FLYING_SPEED);
            final boolean sprinting = dragon.isAccelerating();
            double targetSpeed = (sprinting ? SPRINT_SPEED_MULT : CRUISE_SPEED_MULT) * baseSpeed;

            Vec3 currentVelocity = dragon.getDeltaMovement();

            final boolean keyPitchMode = dragon.isRiderPitchKeyMode();
            float pitchRad = getEffectivePitchRadians(player);
            if (keyPitchMode) {
                pitchRad = 0.0f;
            }
            float pitchDegrees = (float) Math.toDegrees(pitchRad);

            DragonRiderFlightPhysics.DiveResponse diveResponse =
                    DragonRiderFlightPhysics.computeDiveResponse(pitchDegrees, keyPitchMode);
            double diveMultiplier = diveResponse.speedMultiplier();
            double diveAcceleration = diveResponse.acceleration();
            double diveDrag = diveResponse.drag();

            targetSpeed *= diveMultiplier;

            double forwardInput = motion.z;
            double strafeInput = motion.x;
            boolean hasInput = Math.abs(forwardInput) > 0.01 || Math.abs(strafeInput) > 0.01;
            float yawRad = (float) Math.toRadians(dragon.getYRot());
            double forwardXZ = Math.cos(pitchRad);
            double forwardX = -Math.sin(yawRad) * forwardXZ;
            double forwardY = keyPitchMode ? 0.0 : -Math.sin(pitchRad);
            double forwardZ = Math.cos(yawRad) * forwardXZ;
            double rightX = Math.cos(yawRad);
            double rightZ = Math.sin(yawRad);

            double targetDirX = forwardX * forwardInput + rightX * (strafeInput * STRAFE_POWER);
            double targetDirY = forwardY * forwardInput * 1.35;
            double targetDirZ = forwardZ * forwardInput + rightZ * (strafeInput * STRAFE_POWER);
            double dirLength = Math.sqrt(targetDirX * targetDirX + targetDirY * targetDirY + targetDirZ * targetDirZ);

            Vec3 newVelocity;

            if (hasInput && dirLength > 0.01) {
                targetDirX /= dirLength;
                targetDirY /= dirLength;
                targetDirZ /= dirLength;

                Vec3 targetVelocity = new Vec3(
                    targetDirX * targetSpeed,
                    targetDirY * targetSpeed,
                    targetDirZ * targetSpeed
                );
                newVelocity = new Vec3(
                    Mth.lerp(diveAcceleration, currentVelocity.x, targetVelocity.x),
                    Mth.lerp(diveAcceleration, currentVelocity.y, targetVelocity.y),
                    Mth.lerp(diveAcceleration, currentVelocity.z, targetVelocity.z)
                );
                newVelocity = newVelocity.scale(1.0 - diveDrag);
            } else {
                newVelocity = currentVelocity.scale(1.0 - DRAG_NO_INPUT);
                if (newVelocity.length() < 0.01) {
                    newVelocity = Vec3.ZERO;
                }
            }

            double verticalVel = newVelocity.y;
            boolean isDiving = !keyPitchMode && pitchDegrees >= 45.0f && hasInput;

            if (!isDiving) {
                if (isTakeoffWindowActive() && dragon.isGoingUp()) {
                    // Apply modest boost during takeoff if Space is held
                    double boost = ASCEND_THRUST * 0.65;
                    verticalVel = Math.max(verticalVel + boost, 0.20);
                } else if (dragon.isGoingUp()) {
                    verticalVel += ASCEND_THRUST;
                } else if (dragon.isGoingDown()) {
                    verticalVel -= DESCEND_THRUST;
                }
                verticalVel = Mth.clamp(verticalVel, -TERMINAL_VELOCITY, TERMINAL_VELOCITY);
            }
            Vec3 finalVelocity = new Vec3(newVelocity.x, verticalVel, newVelocity.z);
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
        if (dragon.isRiderPitchKeyMode()) {
            return getKeyPitchRadians();
        }
        return (float) Math.toRadians(player.getXRot());
    }

    private float getKeyPitchRadians() {
        if (dragon.isGoingUp()) {
            return (float) -Math.toRadians(Ignivorus.RIDER_KEY_PITCH_DEG);
        }
        if (dragon.isGoingDown()) {
            return (float) Math.toRadians(Ignivorus.RIDER_KEY_PITCH_DEG);
        }
        return 0.0f;
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
        Entity entity = dragon.getFirstPassenger();
        if (entity instanceof Player player && dragon.isTame() && dragon.isOwnedBy(player)) {
            return player;
        }
        return null;
    }

    public void requestRiderTakeoff() {
        dragon.tryRiderTakeoff(getControllingPassenger() instanceof Player player ? player : null);
    }

    private boolean isTakeoffWindowActive() {
        return dragon.isTakeoff() || dragon.timeFlying < Ignivorus.TAKEOFF_ANIMATION_TICKS;
    }
}
