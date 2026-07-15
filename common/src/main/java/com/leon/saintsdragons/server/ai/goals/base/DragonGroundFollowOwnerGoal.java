package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.interfaces.SemiAquaticDragon;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.EnumSet;

public class DragonGroundFollowOwnerGoal<T extends RideableDragonBase> extends DragonBaseGoal<T> {
    private final FollowConfig config;
    private int pathRecalcCooldown;
    private double lastOwnerX = Double.NaN;
    private double lastOwnerY = Double.NaN;
    private double lastOwnerZ = Double.NaN;

    public DragonGroundFollowOwnerGoal(T dragon, FollowConfig config) {
        super(dragon);
        this.config = config;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    protected boolean canUseAdditional() {
        if (!canFollowOwner()) {
            return false;
        }
        LivingEntity owner = dragon.getOwner();
        return owner != null && dragon.distanceToSqr(owner) > config.startFollowDist * config.startFollowDist;
    }

    @Override
    protected boolean canContinueAdditional() {
        if (!canFollowOwner()) {
            return false;
        }
        LivingEntity owner = dragon.getOwner();
        return owner != null && dragon.distanceToSqr(owner) > config.stopFollowDist * config.stopFollowDist;
    }

    private boolean canFollowOwner() {
        if (dragon.isTame() && dragon.getCommand() != 0) {
            return false;
        }
        if (!dragon.isTame() || dragon.isOrderedToSit() || dragon.isInLove()) {
            return false;
        }
        if (config.blockWhileVehicle && dragon.isVehicle()) {
            return false;
        }
        LivingEntity target = dragon.getTarget();
        if (target != null && (!config.onlyBlockLivingTarget || target.isAlive())) {
            return false;
        }
        LivingEntity owner = dragon.getOwner();
        return owner != null && owner.isAlive() && owner.level() == dragon.level();
    }

    @Override
    public void start() {
        resetPathTracking();
    }

    @Override
    public void stop() {
        dragon.getAIMovement().stop();
        resetPathTracking();
    }

    @Override
    public void tick() {
        LivingEntity owner = dragon.getOwner();
        if (owner == null) {
            return;
        }

        double distance = dragon.distanceTo(owner);
        if (distance > config.teleportDist && canTeleportToOwner()) {
            teleportToOwner(owner);
            return;
        }

        dragon.getLookControl().setLookAt(owner, 10.0F, dragon.getMaxHeadXRot());
        if (distance <= config.stopFollowDist) {
            dragon.getAIMovement().stop();
            pathRecalcCooldown = 0;
            return;
        }

        boolean fast = distance > config.runDist;
        if (shouldUseWaterFollowing(owner)) {
            dragon.getAIMovement().setGroundMoveState(fast);
            updateWaterFollow(owner, fast, distance);
            return;
        }

        double speed = fast ? config.runSpeed : config.walkSpeed;
        updateGroundPath(owner, speed, distance, fast);
        if (dragon.getAIMovement().hasFailed()) {
            dragon.getJumpControl().jump();
            dragon.getAIMovement().stop();
            pathRecalcCooldown = 0;
        }
    }

    protected boolean canTeleportToOwner() {
        return dragon.isGroundedForTeleport() || shouldUseWaterFollowing(dragon.getOwner());
    }

    protected void teleportToOwner(LivingEntity owner) {
        if (!DragonFollowOwnerGoal.attemptOwnerTeleport(dragon, owner)) {
            dragon.teleportTo(owner.getX(), owner.getY() + config.fallbackTeleportYOffset, owner.getZ());
        }
        dragon.getAIMovement().stop();
        resetPathTracking();
    }

    protected void setFastFollowing(boolean fast) {
        dragon.getAIMovement().setGroundMoveState(fast);
    }

    private boolean shouldUseWaterFollowing(LivingEntity owner) {
        return owner != null
                && dragon.canSwim()
                && dragon instanceof SemiAquaticDragon
                && (dragon.isInWaterOrBubble() || owner.isInWaterOrBubble());
    }

    private void updateGroundPath(LivingEntity owner, double speed, double distance, boolean running) {
        if (pathRecalcCooldown > 0) {
            pathRecalcCooldown--;
        }

        boolean ownerMoved = ownerMovedSignificantly(owner);
        boolean navIdle = dragon.getAIMovement().hasArrived() || !dragon.getAIMovement().isPathing();
        if (navIdle || ownerMoved || pathRecalcCooldown <= 0) {
            double effectiveSpeed = dragon.isInWater() ? speed * config.groundSpeedInWaterMultiplier : speed;
            if (!dragon.getAIMovement().moveToGroundTarget(owner, effectiveSpeed, running)) {
                dragon.getAIMovement().moveToGroundPosition(owner.position(), effectiveSpeed, running);
            }
            rememberOwnerPosition(owner);
            pathRecalcCooldown = computeRepathCooldown(distance, running);
        }
    }

    private void updateWaterFollow(LivingEntity owner, boolean running, double distance) {
        dragon.getAIMovement().stop();
        if (distance <= config.stopFollowDist) {
            dragon.setDeltaMovement(dragon.getDeltaMovement().scale(0.85D));
            return;
        }

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

        double swimSpeed = config.swimSpeed;
        if (dragon instanceof SemiAquaticDragon semiAquaticDragon) {
            swimSpeed = semiAquaticDragon.getSwimSpeed() * (running ? config.runSwimSpeedMultiplier : config.walkSwimSpeedMultiplier);
            if (horizontalDist > 15.0D) {
                swimSpeed *= 1.2D;
            }
        }

        double yawRad = dragon.getYRot() * Mth.DEG_TO_RAD;
        double pitchRad = dragon.getXRot() * Mth.DEG_TO_RAD;
        double dirX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double dirY = -Math.sin(pitchRad);
        double dirZ = Math.cos(yawRad) * Math.cos(pitchRad);
        dragon.setDeltaMovement(dirX * swimSpeed, dirY * swimSpeed, dirZ * swimSpeed);
        dragon.hasImpulse = true;
    }

    private int computeRepathCooldown(double distance, boolean running) {
        int base = (int) Math.ceil(distance * (running ? config.runRepathScale : config.walkRepathScale));
        return Mth.clamp(base, running ? config.minRunRepathTicks : config.minWalkRepathTicks,
                running ? config.maxRunRepathTicks : config.maxWalkRepathTicks);
    }

    private boolean ownerMovedSignificantly(LivingEntity owner) {
        if (Double.isNaN(lastOwnerX)) {
            return true;
        }
        double dx = owner.getX() - this.lastOwnerX;
        double dy = owner.getY() - this.lastOwnerY;
        double dz = owner.getZ() - this.lastOwnerZ;
        return dx * dx + dy * dy + dz * dz > config.ownerMoveThresholdSqr;
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
    }

    public record FollowConfig(
            double startFollowDist,
            double stopFollowDist,
            double teleportDist,
            double runDist,
            double walkSpeed,
            double runSpeed,
            double swimSpeed,
            double fallbackTeleportYOffset,
            double groundSpeedInWaterMultiplier,
            double walkSwimSpeedMultiplier,
            double runSwimSpeedMultiplier,
            double walkRepathScale,
            double runRepathScale,
            int minWalkRepathTicks,
            int maxWalkRepathTicks,
            int minRunRepathTicks,
            int maxRunRepathTicks,
            double ownerMoveThresholdSqr,
            boolean blockWhileVehicle,
            boolean onlyBlockLivingTarget
    ) {
        public static FollowConfig forStegonaut() {
            return new FollowConfig(
                    12.0D, 8.0D, 32.0D, Double.POSITIVE_INFINITY,
                    0.8D, 0.8D, 0.0D, 1.0D,
                    1.0D, 0.25D, 0.35D,
                    0.45D, 0.45D,
                    6, 24, 6, 24,
                    1.0D, false, true
            );
        }

        public static FollowConfig forVarasuchus() {
            return new FollowConfig(
                    20.0D, 16.0D, 32.0D, 25.0D,
                    0.85D, 1.35D, 0.0D, 0.0D,
                    1.3D, 0.25D, 0.35D,
                    0.55D, 0.4D,
                    6, 24, 4, 16,
                    1.0D, true, false
            );
        }
    }
}
