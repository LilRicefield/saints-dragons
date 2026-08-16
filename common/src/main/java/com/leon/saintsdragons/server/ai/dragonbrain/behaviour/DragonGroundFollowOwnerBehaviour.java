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
    private static final Config BABY_CONFIG = new Config(
            DragonBabyOwnerFollowTuning.START_DISTANCE,
            DragonBabyOwnerFollowTuning.STOP_DISTANCE,
            DragonBabyOwnerFollowTuning.TELEPORT_DISTANCE,
            DragonBabyOwnerFollowTuning.WALK_SPEED,
            1.0D,
            DragonBabyOwnerFollowTuning.RUN_DISTANCE,
            DragonBabyOwnerFollowTuning.RUN_SPEED
    );

    private final Config adultConfig;
    private final DragonOwnerFollowWaterHandoff waterHandoff = new DragonOwnerFollowWaterHandoff();
    private int repathCooldown;
    private double lastOwnerX = Double.NaN;
    private double lastOwnerY = Double.NaN;
    private double lastOwnerZ = Double.NaN;

    public DragonGroundFollowOwnerBehaviour(Config config) {
        this.adultConfig = config;
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
        return canFollow(dragon, owner)
                && dragon.distanceToSqr(owner) > config.stopDistance * config.stopDistance;
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        resetTracking();
        waterHandoff.activate(context.dragon());
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        LivingEntity owner = dragon.getOwner();
        if (owner == null) return;
        waterHandoff.activate(dragon);

        Config config = configFor(dragon);
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
        waterHandoff.release();
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

    private Config configFor(T dragon) {
        return dragon.isBaby() ? BABY_CONFIG : adultConfig;
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
        return Map.of(
                "repath_cooldown", Integer.toString(repathCooldown),
                "water_handoff", Boolean.toString(waterHandoff.isActive())
        );
    }

    public record Config(double startDistance,
                         double stopDistance,
                         double teleportDistance,
                         double speed,
                         double fallbackTeleportYOffset,
                         double fastDistance,
                         double fastSpeed) {
        public static Config standardAdult() {
            return new Config(
                    DragonAdultOwnerFollowTuning.START_DISTANCE,
                    DragonAdultOwnerFollowTuning.STOP_DISTANCE,
                    DragonAdultOwnerFollowTuning.TELEPORT_DISTANCE,
                    DragonAdultOwnerFollowTuning.WALK_SPEED,
                    DragonAdultOwnerFollowTuning.FALLBACK_TELEPORT_Y_OFFSET,
                    DragonAdultOwnerFollowTuning.RUN_DISTANCE,
                    DragonAdultOwnerFollowTuning.RUN_SPEED
            );
        }
    }
}
