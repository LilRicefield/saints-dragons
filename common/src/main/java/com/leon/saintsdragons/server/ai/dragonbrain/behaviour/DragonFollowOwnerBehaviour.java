package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonOwnerTeleport;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class DragonFollowOwnerBehaviour<T extends RideableFlyingDragon> extends DragonBehaviour<T> {
    private static final double AIR_TARGET_EPSILON_SQR = 9.0D;
    private static final double AIR_SPEED_EPSILON = 0.15D;
    private static final double AIR_CATCH_UP_DISTANCE = 18.0D;
    private static final double AIR_CATCH_UP_MULTIPLIER = 1.35D;
    private static final int FAILED_GROUND_PATH_RETRY_TICKS = 10;
    private static final Config BABY_CONFIG = new Config(
            DragonBabyOwnerFollowTuning.START_DISTANCE,
            DragonBabyOwnerFollowTuning.STOP_DISTANCE,
            DragonBabyOwnerFollowTuning.TELEPORT_DISTANCE,
            DragonBabyOwnerFollowTuning.RUN_DISTANCE,
            24.0D,
            10.0D,
            2.5D,
            DragonBabyOwnerFollowTuning.WALK_SPEED,
            DragonBabyOwnerFollowTuning.RUN_SPEED,
            DragonBabyOwnerFollowTuning.MAX_WALK_SPEED,
            DragonBabyOwnerFollowTuning.MAX_RUN_SPEED,
            4.0D
    );

    private final Config adultConfig;
    private final Consumer<T> takeoffStarter;
    private final DragonOwnerFollowWaterHandoff waterHandoff = new DragonOwnerFollowWaterHandoff();
    private int groundRepathCooldown;
    private int airRefreshCooldown;
    private double lastOwnerX = Double.NaN;
    private double lastOwnerY = Double.NaN;
    private double lastOwnerZ = Double.NaN;
    @Nullable
    private Vec3 lastAirTarget;
    private double lastAirSpeed = Double.NaN;
    private String mode = "idle";

    public DragonFollowOwnerBehaviour(Config config, Consumer<T> takeoffStarter) {
        super(Map.of(DragonMemories.MOVEMENT_INTENT, MemoryStatus.REGISTERED));
        this.adultConfig = Objects.requireNonNull(config);
        this.takeoffStarter = Objects.requireNonNull(takeoffStarter);
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        LivingEntity owner = dragon.getOwner();
        Config config = configFor(dragon);
        return canFollow(dragon, owner)
                && dragon.distanceToSqr(owner) > config.startDistance * config.startDistance;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        LivingEntity owner = dragon.getOwner();
        Config config = configFor(dragon);
        if (!canFollow(dragon, owner)) {
            return false;
        }
        if (dragon.isLanding()) {
            return !dragon.onGround();
        }
        if (dragon.isAerial() && ownerGrounded(owner)) {
            return true;
        }
        return dragon.distanceToSqr(owner) > config.stopDistance * config.stopDistance;
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        resetTracking();
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        LivingEntity owner = dragon.getOwner();
        if (owner == null) {
            return;
        }
        if (airRefreshCooldown > 0) {
            airRefreshCooldown--;
        }

        Config config = configFor(dragon);
        double distance = dragon.distanceTo(owner);
        if (distance > config.teleportDistance && dragon.isGroundedForTeleport()) {
            if (!DragonOwnerTeleport.attempt(dragon, owner)) {
                dragon.teleportTo(owner.getX(), owner.getY() + 3.0D, owner.getZ());
            }
            if (!dragon.isBaby() && dragon.canTakeoff()) {
                dragon.beginAiFlight();
            } else {
                dragon.clearAerialState();
            }
            resetTracking();
            return;
        }

        dragon.getLookControl().setLookAt(owner, 10.0F, 10.0F);
        boolean ownerAirborne = isOwnerAirborne(dragon, owner);
        boolean shouldFly = shouldFly(dragon, owner, distance, ownerAirborne, config);
        if (updateFlightState(context, dragon, owner, ownerAirborne, shouldFly, config)) {
            mode = "landing";
            return;
        }

        if (dragon.isLanding()) {
            waterHandoff.release();
            mode = "landing";
            if (!dragon.getAIMovement().isPathing()) {
                context.memories().set(
                        DragonMemories.MOVEMENT_INTENT,
                        DragonMovementIntent.transitionToGround(owner, config.flightSpeed)
                );
            }
        } else if (dragon.isFlying() || dragon.isTakeoff() || dragon.isHovering()) {
            waterHandoff.release();
            followInAir(context, dragon, owner, ownerAirborne, config);
        } else {
            followOnGround(dragon, owner, distance, config);
        }
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        context.memories().erase(DragonMemories.MOVEMENT_INTENT);
        context.dragon().setAccelerating(false);
        context.dragon().getAIMovement().stop();
        waterHandoff.release();
        mode = "idle";
        resetTracking();
    }

    private boolean canFollow(T dragon, @Nullable LivingEntity owner) {
        return hasOwnerFollowPriority(dragon)
                && !dragon.isOrderedToSit()
                && !dragon.isInLove()
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && !dragon.isSittingDownAnimation()
                && !dragon.isInWaterOrBubble()
                && (dragon.getTarget() == null || !dragon.getTarget().isAlive())
                && owner != null
                && owner.isAlive()
                && owner.level() == dragon.level();
    }

    static boolean hasOwnerFollowPriority(RideableDragonBase dragon) {
        LivingEntity owner = dragon.getOwner();
        LivingEntity target = dragon.getTarget();
        return dragon.isTame()
                && dragon.getCommand() == 0
                && owner != null
                && owner.isAlive()
                && owner.level() == dragon.level()
                && (target == null || !target.isAlive());
    }

    private boolean updateFlightState(DragonBrainContext<T> context,
                                      T dragon,
                                      LivingEntity owner,
                                      boolean ownerAirborne,
                                      boolean shouldFly,
                                      Config config) {
        if (dragon.isBaby() && dragon.isAerial()) {
            if (dragon.onGround()) {
                dragon.clearAerialState();
            } else {
                context.memories().set(
                        DragonMemories.MOVEMENT_INTENT,
                        DragonMovementIntent.transitionToGround(owner, config.flightSpeed)
                );
            }
            resetTracking();
            return true;
        }
        if (shouldFly && !dragon.isFlying() && !dragon.isTakeoff()) {
            takeoffStarter.accept(dragon);
            resetTracking();
            return false;
        }
        if (!(dragon.isFlying() || dragon.isHovering())) {
            return false;
        }

        double dx = owner.getX() - dragon.getX();
        double dz = owner.getZ() - dragon.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        if (!shouldFly && !ownerAirborne
                && horizontalDistance < config.landingDistance
                && !dragon.isLanding()) {
            context.memories().set(
                    DragonMemories.MOVEMENT_INTENT,
                    DragonMovementIntent.transitionToGround(owner, config.flightSpeed)
            );
            groundRepathCooldown = 0;
            return true;
        }
        return false;
    }

    private void followInAir(DragonBrainContext<T> context,
                             T dragon,
                             LivingEntity owner,
                             boolean ownerAirborne,
                             Config config) {
        mode = "air";
        Vec3 target = flightTarget(dragon, owner, ownerAirborne, config);
        boolean catchUp = dragon.distanceToSqr(target) > AIR_CATCH_UP_DISTANCE * AIR_CATCH_UP_DISTANCE;
        double speed = catchUp ? config.flightSpeed * AIR_CATCH_UP_MULTIPLIER : config.flightSpeed;
        dragon.setAccelerating(catchUp);
        if (dragon.distanceToSqr(target) <= 1.0D) {
            dragon.setAccelerating(false);
            dragon.getAIMovement().stop();
            return;
        }
        if (shouldRefreshAirTarget(target, speed)) {
            context.memories().set(DragonMemories.MOVEMENT_INTENT, DragonMovementIntent.auto(target, speed));
            lastAirTarget = target;
            lastAirSpeed = speed;
            airRefreshCooldown = speed >= 1.4D ? 3 : speed >= 1.0D ? 5 : 7;
        }
    }

    private void followOnGround(T dragon, LivingEntity owner, double distance, Config config) {
        mode = "ground";
        waterHandoff.activate(dragon);
        if (distance <= config.stopDistance) {
            dragon.setAccelerating(false);
            dragon.getAIMovement().stop();
            groundRepathCooldown = 0;
            return;
        }
        if (dragon.getAIMovement().hasFailed()) {
            dragon.getAIMovement().stop();
            rememberOwner(owner);
            groundRepathCooldown = FAILED_GROUND_PATH_RETRY_TICKS;
            return;
        }
        boolean running = distance > config.runDistance;
        dragon.setAccelerating(running);
        double baseSpeed = running ? config.runSpeed : config.walkSpeed;
        double speed = Math.min(
                baseSpeed * (1.0D + distance / 50.0D),
                running ? config.maxRunSpeed : config.maxWalkSpeed
        );
        if (groundRepathCooldown > 0) {
            groundRepathCooldown--;
        }
        if (dragon.getAIMovement().hasArrived()) {
            groundRepathCooldown = 0;
        }
        if (ownerMoved(owner) || groundRepathCooldown <= 0) {
            boolean accepted = dragon.getAIMovement().moveToProgressiveGroundTarget(owner, speed, running);
            rememberOwner(owner);
            int baseCooldown = (int)Math.ceil(distance * (running ? 0.3D : 0.45D));
            groundRepathCooldown = accepted
                    ? Mth.clamp(baseCooldown, running ? 4 : 6, running ? 18 : 24)
                    : FAILED_GROUND_PATH_RETRY_TICKS;
        }
    }

    private boolean shouldFly(T dragon,
                              LivingEntity owner,
                              double distance,
                              boolean ownerAirborne,
                              Config config) {
        if (dragon.isBaby()) {
            return false;
        }
        if (dragon.isFlying() || dragon.isTakeoff() || dragon.isHovering()) {
            if (ownerAirborne) {
                return true;
            }
            return !(distance < config.landingDistance && ownerGrounded(owner));
        }
        if (!canTriggerFlight(dragon) || distance < config.stopDistance * 1.5D) {
            return false;
        }
        return ownerAirborne || distance > config.flightTriggerDistance;
    }

    private boolean canTriggerFlight(T dragon) {
        return !dragon.isOrderedToSit()
                && dragon.canFly()
                && !dragon.isBaby()
                && dragon.onGround()
                && dragon.getPassengers().isEmpty()
                && dragon.getControllingPassenger() == null
                && !dragon.isPassenger()
                && dragon.getActiveAbility() == null;
    }

    private boolean isOwnerAirborne(T dragon, LivingEntity owner) {
        Entity vehicle = owner.getVehicle();
        if (vehicle != null) {
            if (vehicle instanceof DragonFlightCapable flightCapable) {
                return flightCapable.isFlying()
                        || flightCapable.isTakeoff()
                        || flightCapable.isHovering()
                        || flightCapable.isLanding() && !vehicle.onGround();
            }
            return !vehicle.onGround();
        }
        if (owner.onGround()) {
            return false;
        }
        BlockPos position = owner.blockPosition();
        int groundY = dragon.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, position).getY();
        return owner.getY() - groundY > 4.0D;
    }

    private boolean ownerGrounded(LivingEntity owner) {
        Entity vehicle = owner.getVehicle();
        if (vehicle instanceof DragonFlightCapable flightCapable) {
            return vehicle.onGround()
                    && !flightCapable.isFlying()
                    && !flightCapable.isTakeoff()
                    && !flightCapable.isHovering()
                    && !flightCapable.isLanding();
        }
        return vehicle != null ? vehicle.onGround() : owner.onGround();
    }

    private Vec3 flightTarget(T dragon, LivingEntity owner, boolean ownerAirborne, Config config) {
        double targetY = ownerAirborne
                ? owner.getY() + owner.getBbHeight() + config.hoverHeight
                : owner.getY() + owner.getBbHeight() * 0.5D;
        Vec3 look = owner.getLookAngle();
        double followOffset = ownerAirborne ? 3.0D : 1.5D;
        double verticalOffset = ownerAirborne ? Math.sin(dragon.tickCount * 0.2D) * 0.3D : 0.0D;
        return new Vec3(
                owner.getX() - look.x * followOffset,
                targetY + verticalOffset,
                owner.getZ() - look.z * followOffset
        );
    }

    private boolean shouldRefreshAirTarget(Vec3 target, double speed) {
        return lastAirTarget == null
                || airRefreshCooldown <= 0
                || target.distanceToSqr(lastAirTarget) > AIR_TARGET_EPSILON_SQR
                || Math.abs(speed - lastAirSpeed) > AIR_SPEED_EPSILON;
    }

    private Config configFor(T dragon) {
        return dragon.isBaby() ? BABY_CONFIG : adultConfig;
    }

    private boolean ownerMoved(LivingEntity owner) {
        if (Double.isNaN(lastOwnerX)) {
            return true;
        }
        double dx = owner.getX() - lastOwnerX;
        double dy = owner.getY() - lastOwnerY;
        double dz = owner.getZ() - lastOwnerZ;
        return dx * dx + dy * dy + dz * dz > 1.2D;
    }

    private void rememberOwner(LivingEntity owner) {
        lastOwnerX = owner.getX();
        lastOwnerY = owner.getY();
        lastOwnerZ = owner.getZ();
    }

    private void resetTracking() {
        groundRepathCooldown = 0;
        airRefreshCooldown = 0;
        lastOwnerX = Double.NaN;
        lastOwnerY = Double.NaN;
        lastOwnerZ = Double.NaN;
        lastAirTarget = null;
        lastAirSpeed = Double.NaN;
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        return Map.of(
                "mode", mode,
                "ground_repath", Integer.toString(groundRepathCooldown),
                "air_refresh", Integer.toString(airRefreshCooldown),
                "air_target", lastAirTarget == null ? "none" : lastAirTarget.toString(),
                "water_handoff", Boolean.toString(waterHandoff.isActive())
        );
    }

    public record Config(double startDistance,
                         double stopDistance,
                         double teleportDistance,
                         double runDistance,
                         double flightTriggerDistance,
                         double landingDistance,
                         double hoverHeight,
                         double walkSpeed,
                         double runSpeed,
                         double maxWalkSpeed,
                         double maxRunSpeed,
                         double flightSpeed) {
        public static Config raevyx() {
            return withStandardGround(30.0D, 10.0D, 2.5D, 4.0D);
        }

        public static Config cindervane() {
            return withStandardGround(30.0D, 10.0D, 2.5D, 4.0D);
        }

        public static Config ignivorus() {
            return withStandardGround(20.0D, 10.0D, 2.5D, 4.0D);
        }

        public static Config volitans() {
            return withStandardGround(24.0D, 10.0D, 2.5D, 4.0D);
        }

        private static Config withStandardGround(double flightTriggerDistance,
                                                 double landingDistance,
                                                 double hoverHeight,
                                                 double flightSpeed) {
            return new Config(
                    DragonAdultOwnerFollowTuning.START_DISTANCE,
                    DragonAdultOwnerFollowTuning.STOP_DISTANCE,
                    DragonAdultOwnerFollowTuning.TELEPORT_DISTANCE,
                    DragonAdultOwnerFollowTuning.RUN_DISTANCE,
                    flightTriggerDistance,
                    landingDistance,
                    hoverHeight,
                    DragonAdultOwnerFollowTuning.WALK_SPEED,
                    DragonAdultOwnerFollowTuning.RUN_SPEED,
                    DragonAdultOwnerFollowTuning.MAX_WALK_SPEED,
                    DragonAdultOwnerFollowTuning.MAX_RUN_SPEED,
                    flightSpeed
            );
        }
    }
}
