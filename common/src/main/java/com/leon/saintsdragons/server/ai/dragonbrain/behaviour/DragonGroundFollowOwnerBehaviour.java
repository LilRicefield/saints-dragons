package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonOwnerTeleport;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;

public final class DragonGroundFollowOwnerBehaviour<T extends RideableDragonBase> extends DragonBehaviour<T> {
    private static final int FAILED_PATH_RETRY_TICKS = 10;

    private final Config config;
    private int repathCooldown;
    private double lastOwnerX = Double.NaN;
    private double lastOwnerY = Double.NaN;
    private double lastOwnerZ = Double.NaN;

    public DragonGroundFollowOwnerBehaviour(Config config) {
        this.config = config;
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        LivingEntity owner = dragon.getOwner();
        return canFollow(dragon, owner)
                && dragon.distanceToSqr(owner) > config.startDistance * config.startDistance;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        LivingEntity owner = dragon.getOwner();
        return canFollow(dragon, owner)
                && dragon.distanceToSqr(owner) > config.stopDistance * config.stopDistance;
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        resetTracking();
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        LivingEntity owner = dragon.getOwner();
        if (owner == null) return;

        double distance = dragon.distanceTo(owner);
        boolean fast = distance > config.fastDistance;
        dragon.setAccelerating(fast);
        if (distance > config.teleportDistance && dragon.isGroundedForTeleport()) {
            if (!DragonOwnerTeleport.attempt(dragon, owner)) {
                dragon.teleportTo(owner.getX(), owner.getY() + config.fallbackTeleportYOffset, owner.getZ());
            }
            dragon.getAIMovement().stop();
            resetTracking();
            return;
        }

        dragon.getLookControl().setLookAt(owner, 10.0F, dragon.getMaxHeadXRot());
        if (distance <= config.stopDistance) {
            dragon.getAIMovement().stop();
            repathCooldown = 0;
            return;
        }
        if (dragon.getAIMovement().hasFailed()) {
            dragon.getAIMovement().stop();
            remember(owner);
            repathCooldown = FAILED_PATH_RETRY_TICKS;
            return;
        }
        if (repathCooldown > 0) repathCooldown--;
        if (dragon.getAIMovement().hasArrived()) {
            repathCooldown = 0;
        }
        if (ownerMoved(owner) || repathCooldown <= 0) {
            double speed = fast ? config.fastSpeed : config.speed;
            boolean accepted = dragon.getAIMovement().moveToProgressiveGroundTarget(owner, speed, fast);
            remember(owner);
            repathCooldown = accepted
                    ? Mth.clamp((int)Math.ceil(distance * 0.45D), 6, 24)
                    : FAILED_PATH_RETRY_TICKS;
        }
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        context.dragon().getAIMovement().stop();
        context.dragon().setAccelerating(false);
        resetTracking();
    }

    private boolean canFollow(T dragon, LivingEntity owner) {
        if (!dragon.isTame() || dragon.getCommand() != 0 || dragon.isOrderedToSit()
                || dragon.isInLove() || dragon.isPassenger() || dragon.isSittingDownAnimation()
                || dragon.isInWaterOrBubble()) {
            return false;
        }
        if (dragon.getTarget() != null && dragon.getTarget().isAlive()) return false;
        return owner != null && owner.isAlive() && owner.level() == dragon.level();
    }

    private boolean ownerMoved(LivingEntity owner) {
        if (Double.isNaN(lastOwnerX)) return true;
        double dx = owner.getX() - lastOwnerX;
        double dy = owner.getY() - lastOwnerY;
        double dz = owner.getZ() - lastOwnerZ;
        return dx * dx + dy * dy + dz * dz > 1.0D;
    }

    private void remember(LivingEntity owner) {
        lastOwnerX = owner.getX();
        lastOwnerY = owner.getY();
        lastOwnerZ = owner.getZ();
    }

    private void resetTracking() {
        repathCooldown = 0;
        lastOwnerX = lastOwnerY = lastOwnerZ = Double.NaN;
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        return Map.of("repath_cooldown", Integer.toString(repathCooldown));
    }

    public record Config(double startDistance,
                         double stopDistance,
                         double teleportDistance,
                         double speed,
                         double fallbackTeleportYOffset,
                         double fastDistance,
                         double fastSpeed) {
        public static Config stegonaut() {
            return new Config(12.0D, 8.0D, 32.0D, 0.8D, 1.0D,
                    Double.POSITIVE_INFINITY, 0.8D);
        }

        public static Config atroxiia() {
            return new Config(12.0D, 8.0D, 32.0D, 0.95D, 1.0D,
                    16.0D, 1.25D);
        }

        public static Config varasuchus() {
            return new Config(20.0D, 16.0D, 32.0D, 0.85D, 0.0D,
                    25.0D, 1.35D);
        }
    }
}
