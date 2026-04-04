package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import com.leon.saintsdragons.server.entity.interfaces.SemiAquaticDragon;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Generic follow owner goal for all rideable flying dragons.
 * Handles both ground and flight following with configurable parameters.
 */
public class DragonFollowOwnerGoal<T extends RideableDragonBase & DragonFlightCapable> extends DragonBaseGoal<T> {
    private static final double AIR_MOVE_TARGET_EPSILON_SQR = 9.0D;
    private static final double AIR_MOVE_SPEED_EPSILON = 0.15D;

    private final FollowConfig config;

    private int pathRecalcCooldown = 0;
    private double lastOwnerX = Double.NaN;
    private double lastOwnerY = Double.NaN;
    private double lastOwnerZ = Double.NaN;
    private int airMoveRefreshCooldown = 0;
    private Vec3 lastAirMoveTarget = null;
    private double lastAirMoveSpeed = Double.NaN;

    public DragonFollowOwnerGoal(T dragon, FollowConfig config) {
        super(dragon);
        this.config = config;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    protected boolean canUseAdditional() {
        // Follow goal only runs in Follow command mode (0).
        if (dragon.isTame() && dragon.getCommand() != 0) {
            return false;
        }

        if (dragon.isInLove()) {
            return false;
        }

        // Don't follow while fighting
        if (isInCombat()) {
            return false;
        }

        LivingEntity owner = dragon.getOwner();
        if (owner == null || !owner.isAlive()) {
            return false;
        }

        // Must be in same dimension
        if (owner.level() != dragon.level()) {
            return false;
        }

        // Check for forced follow (dragon-specific mechanic)
        if (shouldForceFollow()) {
            return true;
        }

        // Only follow if owner is far enough
        double distSq = dragon.distanceToSqr(owner);
        return distSq > config.startFollowDist * config.startFollowDist;
    }

    @Override
    protected boolean canContinueAdditional() {
        // Follow goal only runs in Follow command mode (0).
        if (dragon.isTame() && dragon.getCommand() != 0) {
            return false;
        }

        if (dragon.isInLove()) {
            return false;
        }

        // Stop following while fighting
        if (isInCombat()) {
            return false;
        }

        LivingEntity owner = dragon.getOwner();
        if (owner == null || !owner.isAlive()) {
            return false;
        }

        if (owner.level() != dragon.level()) {
            return false;
        }

        if (shouldForceFollow()) {
            return true;
        }

        if (dragon.isLanding()) {
            return !dragon.onGround();
        }

        // Keep following until close enough
        double distSq = dragon.distanceToSqr(owner);
        return distSq > config.stopFollowDist * config.stopFollowDist;
    }

    @Override
    public void start() {
        resetPathTracking();
    }

    @Override
    public void stop() {
        dragon.setRunning(false);
        dragon.getNavigation().stop();
        dragon.setGroundMoveStateFromAI(0);
        clearForceFollow();
        resetPathTracking();
    }

    @Override
    public void tick() {
        LivingEntity owner = dragon.getOwner();
        if (owner == null) return;
        if (airMoveRefreshCooldown > 0) {
            airMoveRefreshCooldown--;
        }

        double distance = dragon.distanceTo(owner);

        // Emergency teleport if too far
        if (distance > config.teleportDist) {
            handleTeleportToOwner(owner);
            return;
        }

        // Always look at owner while following
        dragon.getLookControl().setLookAt(owner, 10.0f, 10.0f);

        // Determine if dragon should be flying
        boolean ownerAirborne = isOwnerAirborne(owner);
        boolean shouldFly = shouldTriggerFlight(owner, distance, ownerAirborne);

        // Handle flight state transitions
        updateFlightState(owner, shouldFly, ownerAirborne, distance);

        // Execute movement
        if (shouldUseWaterFollowing(owner)) {
            handleWaterFollowing(owner, distance);
        } else if (dragon.isLanding()) {
            if (!dragon.getNavigation().isInProgress()) {
                DragonAggroLandingHelper.beginAggroLanding(dragon, owner, getFlightFollowSpeed());
            }
        } else if (dragon.isFlying() || dragon.isTakeoff() || dragon.isHovering()) {
            handleFlightFollowing(owner, ownerAirborne);
        } else {
            handleGroundFollowing(owner, distance);
        }
    }

    /**
     * Teleport dragon to owner when too far away
     */
    private void handleTeleportToOwner(LivingEntity owner) {
        dragon.teleportTo(owner.getX(), owner.getY() + 3, owner.getZ());
        dragon.setFlying(true);
        dragon.setTakeoff(false);
        dragon.setLanding(false);
        dragon.setHovering(false);
        resetPathTracking();
    }

    /**
     * Update flight state based on conditions
     */
    protected void updateFlightState(LivingEntity owner, boolean shouldFly, boolean ownerAirborne, double distance) {
        if (shouldFly && !dragon.isFlying() && !dragon.isTakeoff()) {
            startFollowTakeoff();
            resetPathTracking();
        } else if (dragon.isFlying() || dragon.isHovering()) {
            // Check if should land
            double dx = owner.getX() - dragon.getX();
            double dz = owner.getZ() - dragon.getZ();
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            boolean shouldLand = !shouldFly && !ownerAirborne && horizontalDistance < config.landingDistance;

            if (shouldLand && !dragon.isLanding()) {
                DragonAggroLandingHelper.beginAggroLanding(dragon, owner, getFlightFollowSpeed());
                pathRecalcCooldown = 0;
            }
        }
    }

    protected void startFollowTakeoff() {
        dragon.setFlying(true);
        dragon.setTakeoff(true);
        dragon.setLanding(false);
        dragon.setHovering(false);
    }

    /**
     * Handle flight movement toward owner
     */
    protected void handleFlightFollowing(LivingEntity owner, boolean ownerAirborne) {
        Vec3 targetPos = getFlightFollowTarget(owner, ownerAirborne);
        double followSpeed = getFlightFollowSpeed();
        double distToTargetSq = dragon.distanceToSqr(targetPos.x, targetPos.y, targetPos.z);
        if (distToTargetSq > 1.0) {
            requestAirMove(targetPos, followSpeed);
        } else {
            dragon.getNavigation().stop();
        }
    }

    protected double getFlightFollowSpeed() {
        return config.flightSpeed;
    }

    protected Vec3 getFlightFollowTarget(LivingEntity owner, boolean ownerAirborne) {
        double targetY = ownerAirborne
                ? owner.getY() + owner.getBbHeight() + config.hoverHeight
                : owner.getY() + owner.getBbHeight() * 0.5D;
        Vec3 ownerLook = owner.getLookAngle();
        double followOffset = ownerAirborne ? 3.0D : 1.5D;
        double offsetX = -ownerLook.x * followOffset;
        double offsetZ = -ownerLook.z * followOffset;
        double verticalOffset = ownerAirborne ? Math.sin(dragon.tickCount * 0.2) * 0.3D : 0.0D;
        double targetX = owner.getX() + offsetX;
        double targetZ = owner.getZ() + offsetZ;
        return new Vec3(targetX, targetY + verticalOffset, targetZ);
    }

    /**
     * Handle ground movement toward owner
     */
    private void handleGroundFollowing(LivingEntity owner, double distance) {
        // Stop if close enough
        if (distance <= config.stopFollowDist) {
            if (dragon.getGroundMoveState() > 0) {
                dragon.getNavigation().stop();
                dragon.setRunning(false);
                dragon.setGroundMoveStateFromAI(0);
            }
            pathRecalcCooldown = 0;
            return;
        }

        // Determine movement style
        boolean shouldRun = distance > config.runDist;
        dragon.setRunning(shouldRun);
        dragon.setGroundMoveStateFromAI(shouldRun ? 2 : 1);

        // Calculate speed with distance scaling
        double baseSpeed = shouldRun ? config.runSpeed : config.walkSpeed;
        double speed = baseSpeed * (1.0 + (distance / 50.0));
        speed = Math.min(speed, shouldRun ? config.maxRunSpeed : config.maxWalkSpeed);

        // Update pathfinding
        updateGroundPath(owner, speed, distance, shouldRun);

        // Handle obstacles
        if (dragon.getNavigation().isStuck()) {
            dragon.getJumpControl().jump();
            dragon.getNavigation().stop();
            pathRecalcCooldown = 0;
        }
    }


    private void updateGroundPath(LivingEntity owner, double speed, double distance, boolean running) {
        if (pathRecalcCooldown > 0) {
            pathRecalcCooldown--;
        }

        boolean ownerMoved = hasOwnerMovedSignificantly(owner);
        boolean navIdle = dragon.getNavigation().isDone() || !dragon.getNavigation().isInProgress();

        if (navIdle || ownerMoved || pathRecalcCooldown <= 0) {
            if (!dragon.getNavigation().moveTo(owner, speed)) {
                dragon.getNavigation().moveTo(owner.getX(), owner.getY(), owner.getZ(), speed);
            }
            rememberOwnerPosition(owner);
            pathRecalcCooldown = computeRepathCooldown(distance, running);
        }
    }


    private boolean shouldTriggerFlight(LivingEntity owner, double distance, boolean ownerAirborne) {
        if (shouldUseWaterFollowing(owner)) {
            return false;
        }

        // If already flying, check if should land
        if (dragon.isFlying() || dragon.isTakeoff() || dragon.isHovering()) {
            if (shouldForceFollow() || ownerAirborne) {
                return true; // Keep flying
            }
            return !(distance < config.landingDistance && owner.onGround());
        }

        // Check if can take off
        if (!canTriggerFlight()) {
            return false;
        }

        // Forced follow or owner is airborne
        if (shouldForceFollow() || ownerAirborne) {
            return true;
        }

        // Don't take off if very close
        if (distance < config.stopFollowDist * 1.5) {
            return false;
        }

        // Take off if far away or owner is significantly higher
        boolean farAway = distance > config.flightTriggerDist;
        boolean ownerAbove = (owner.getY() - dragon.getY()) > config.flightHeightDiff;

        return farAway || ownerAbove;
    }


    private boolean canTriggerFlight() {
        return !dragon.isOrderedToSit()
                && !dragon.isBaby()
                && (dragon.onGround() || dragon.isInWater())
                && dragon.getPassengers().isEmpty()
                && dragon.getControllingPassenger() == null
                && !dragon.isPassenger()
                && dragon.getActiveAbility() == null;
    }

    private boolean shouldUseWaterFollowing(LivingEntity owner) {
        return dragon instanceof SemiAquaticDragon
                && (dragon.isInWaterOrBubble() || owner.isInWaterOrBubble());
    }

    private void handleWaterFollowing(LivingEntity owner, double distance) {
        dragon.getNavigation().stop();

        if (distance <= config.stopFollowDist) {
            dragon.setRunning(false);
            dragon.setGroundMoveStateFromAI(0);
            dragon.setDeltaMovement(dragon.getDeltaMovement().scale(0.85D));
            return;
        }

        boolean shouldRun = distance > config.runDist;
        dragon.setRunning(shouldRun);
        dragon.setGroundMoveStateFromAI(shouldRun ? 2 : 1);

        double dx = owner.getX() - dragon.getX();
        double dy = (owner.getY() + owner.getEyeHeight() * 0.5D) - (dragon.getY() + dragon.getEyeHeight() * 0.5D);
        double dz = owner.getZ() - dragon.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDist < 1.0E-5D && Math.abs(dy) < 1.0E-5D) {
            return;
        }

        float targetYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        dragon.setYRot(Mth.wrapDegrees(targetYaw));
        dragon.yBodyRot = dragon.getYRot();
        dragon.yHeadRot = dragon.getYRot();

        float targetPitch = -((float) (Mth.atan2(dy, horizontalDist) * Mth.RAD_TO_DEG));
        dragon.setXRot(Mth.clamp(Mth.wrapDegrees(targetPitch), -85.0F, 85.0F));

        double swimSpeed = getWaterFollowSpeed(shouldRun, distance);
        double yawRad = dragon.getYRot() * Mth.DEG_TO_RAD;
        double pitchRad = dragon.getXRot() * Mth.DEG_TO_RAD;
        double dirX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double dirY = -Math.sin(pitchRad);
        double dirZ = Math.cos(yawRad) * Math.cos(pitchRad);

        dragon.setDeltaMovement(dirX * swimSpeed, dirY * swimSpeed, dirZ * swimSpeed);
        dragon.hasImpulse = true;
    }

    private double getWaterFollowSpeed(boolean running, double distance) {
        double baseSpeed = running ? config.runSpeed : config.walkSpeed;
        if (dragon instanceof SemiAquaticDragon semiAquaticDragon) {
            baseSpeed = semiAquaticDragon.getSwimSpeed() * (running ? 0.35D : 0.25D);
            if (distance > 15.0D) {
                baseSpeed *= 1.2D;
            }
        }
        return baseSpeed;
    }


    private boolean isOwnerAirborne(LivingEntity owner) {
        if (owner == null || owner.level() != dragon.level()) {
            return false;
        }

        // Check if riding something airborne
        if (owner.isPassenger()) {
            Entity vehicle = owner.getVehicle();
            if (vehicle != null && !vehicle.onGround()) {
                return true;
            }
        }

        // On ground = not airborne
        if (owner.onGround()) {
            return false;
        }

        // Check if significantly elevated above ground (not just jumping)
        BlockPos pos = owner.blockPosition();
        int groundY = owner.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY();
        return owner.getY() - groundY > 4.0;
    }

    private int computeRepathCooldown(double distance, boolean running) {
        int base = (int) Math.ceil(distance * (running ? 0.3 : 0.45));
        return Mth.clamp(base, running ? 4 : 6, running ? 18 : 24);
    }

    private boolean hasOwnerMovedSignificantly(LivingEntity owner) {
        if (Double.isNaN(lastOwnerX)) {
            return true;
        }
        double dx = owner.getX() - lastOwnerX;
        double dy = owner.getY() - lastOwnerY;
        double dz = owner.getZ() - lastOwnerZ;
        return dx * dx + dy * dy + dz * dz > 1.2;
    }

    private void rememberOwnerPosition(LivingEntity owner) {
        this.lastOwnerX = owner.getX();
        this.lastOwnerY = owner.getY();
        this.lastOwnerZ = owner.getZ();
    }

    private void resetPathTracking() {
        this.pathRecalcCooldown = 0;
        this.lastOwnerX = Double.NaN;
        this.lastOwnerY = Double.NaN;
        this.lastOwnerZ = Double.NaN;
        this.airMoveRefreshCooldown = 0;
        this.lastAirMoveTarget = null;
        this.lastAirMoveSpeed = Double.NaN;
    }

    private void requestAirMove(Vec3 target, double speed) {
        if (shouldRefreshAirMoveTarget(target, speed)) {
            dragon.getMoveControl().setWantedPosition(target.x, target.y, target.z, speed);
            lastAirMoveTarget = target;
            lastAirMoveSpeed = speed;
            airMoveRefreshCooldown = airMoveRefreshInterval(speed);
        }
    }

    private boolean shouldRefreshAirMoveTarget(Vec3 target, double speed) {
        if (lastAirMoveTarget == null || airMoveRefreshCooldown <= 0) {
            return true;
        }

        if (target.distanceToSqr(lastAirMoveTarget) > AIR_MOVE_TARGET_EPSILON_SQR) {
            return true;
        }

        return Math.abs(speed - lastAirMoveSpeed) > AIR_MOVE_SPEED_EPSILON;
    }

    private int airMoveRefreshInterval(double speed) {
        if (speed >= 1.4D) {
            return 3;
        }
        if (speed >= 1.0D) {
            return 5;
        }
        return 7;
    }

    protected boolean shouldForceFollow() {
        return false;
    }


    protected void clearForceFollow() {
        // Override if needed
    }

    public static class FollowConfig {
        public final double startFollowDist;
        public final double stopFollowDist;
        public final double teleportDist;
        public final double runDist;
        public final double flightTriggerDist;
        public final double flightHeightDiff;
        public final double landingDistance;
        public final double hoverHeight;
        public final double walkSpeed;
        public final double runSpeed;
        public final double maxWalkSpeed;
        public final double maxRunSpeed;
        public final double flightSpeed;

        public FollowConfig(double startFollowDist, double stopFollowDist, double teleportDist,
                            double runDist, double flightTriggerDist, double flightHeightDiff,
                            double landingDistance, double hoverHeight, double walkSpeed,
                            double runSpeed, double maxWalkSpeed, double maxRunSpeed, double flightSpeed) {
            this.startFollowDist = startFollowDist;
            this.stopFollowDist = stopFollowDist;
            this.teleportDist = teleportDist;
            this.runDist = runDist;
            this.flightTriggerDist = flightTriggerDist;
            this.flightHeightDiff = flightHeightDiff;
            this.landingDistance = landingDistance;
            this.hoverHeight = hoverHeight;
            this.walkSpeed = walkSpeed;
            this.runSpeed = runSpeed;
            this.maxWalkSpeed = maxWalkSpeed;
            this.maxRunSpeed = maxRunSpeed;
            this.flightSpeed = flightSpeed;
        }

        public static FollowConfig forIgnivorus() {
            return new FollowConfig(
                    20.0,   // startFollowDist
                    8.0,    // stopFollowDist (must be lower than landingDistance so flight follow can transition into landing)
                    128.0,  // teleportDist
                    25.0,   // runDist
                    20.0,   // flightTriggerDist
                    8.0,    // flightHeightDiff
                    12.0,   // landingDistance
                    3.0,    // hoverHeight
                    0.8,    // walkSpeed
                    1.5,    // runSpeed
                    1.2,    // maxWalkSpeed
                    2.5,    // maxRunSpeed
                    2.5     // flightSpeed
            );
        }

        public static FollowConfig forRaevyx() {
            return new FollowConfig(
                    20.0,   // startFollowDist
                    5.0,    // stopFollowDist
                    64.0,  // teleportDist
                    20.0,   // runDist
                    30.0,   // flightTriggerDist
                    8.0,    // flightHeightDiff
                    12.0,   // landingDistance
                    3.0,    // hoverHeight
                    0.8,    // walkSpeed
                    1.5,    // runSpeed
                    1.2,    // maxWalkSpeed
                    2.5,    // maxRunSpeed
                    1.2     // flightSpeed
            );
        }

        public static FollowConfig forCindervane() {
            return new FollowConfig(20.,    // startFollowDist
                    8.0,    // stopFollowDist
                    64.0,  // teleportDist
                    10.0,   // runDist
                    24.0,   // flightTriggerDist
                    6.0,    // flightHeightDiff
                    10.0,   // landingDistance
                    2.5,    // hoverHeight
                    0.7,    // walkSpeed
                    1.1,    // runSpeed
                    1.0,    // maxWalkSpeed
                    1.6,    // maxRunSpeed
                    1.0     // flightSpeed (uses dragon.getFlightSpeed())
            );
        }

        public static FollowConfig forVolitans() {
            return new FollowConfig(
                    20.0,   // startFollowDist
                    8.0,    // stopFollowDist
                    64.0,   // teleportDist
                    10.0,   // runDist
                    24.0,   // flightTriggerDist
                    6.0,    // flightHeightDiff
                    10.0,   // landingDistance
                    2.5,    // hoverHeight
                    0.7,    // walkSpeed
                    1.1,    // runSpeed
                    1.0,    // maxWalkSpeed
                    1.6,    // maxRunSpeed
                    4.0     // flightSpeed multiplier for direct air-follow
            );
        }
    }
}
