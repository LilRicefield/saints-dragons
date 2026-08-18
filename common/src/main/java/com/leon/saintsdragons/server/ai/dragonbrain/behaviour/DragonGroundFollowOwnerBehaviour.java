package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonOwnerFollowTarget;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonOwnerTeleport;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

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
    @Nullable
    private Vec3 lastFollowTarget;
    private boolean mountedOwner;

    public DragonGroundFollowOwnerBehaviour(Config config) {
        this.adultConfig = config;
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        LivingEntity owner = dragon.getOwner();
        Config config = configFor(dragon);
        if (!canFollow(dragon, owner)) {
            return false;
        }
        Vec3 followTarget = DragonOwnerFollowTarget.groundTarget(dragon, owner);
        return DragonOwnerFollowTarget.groundFollowDistanceToSqr(dragon, owner, followTarget)
                > DragonOwnerFollowTarget.groundStartDistanceSqr(
                        dragon,
                        owner,
                        config.startDistance,
                        config.stopDistance
                );
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        LivingEntity owner = dragon.getOwner();
        Config config = configFor(dragon);
        if (!canFollow(dragon, owner)) {
            return false;
        }
        Vec3 followTarget = DragonOwnerFollowTarget.groundTarget(dragon, owner);
        double stopDistance = DragonOwnerFollowTarget.groundStopDistance(
                dragon,
                owner,
                config.stopDistance
        );
        return DragonOwnerFollowTarget.groundFollowDistanceToSqr(dragon, owner, followTarget)
                > stopDistance * stopDistance;
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
        Vec3 followTarget = DragonOwnerFollowTarget.groundTarget(dragon, owner);
        double distance = Math.sqrt(
                DragonOwnerFollowTarget.groundFollowDistanceToSqr(dragon, owner, followTarget)
        );
        double stopDistance = DragonOwnerFollowTarget.groundStopDistance(
                dragon,
                owner,
                config.stopDistance
        );
        lastFollowTarget = followTarget;
        mountedOwner = DragonOwnerFollowTarget.isMounted(owner);
        boolean fast = distance > config.fastDistance;
        dragon.setAccelerating(fast);
        if (distance > config.teleportDistance
                && dragon.isGroundedForTeleport()
                && DragonOwnerTeleport.attempt(dragon, owner)) {
            dragon.getAIMovement().stop();
            resetTracking();
            return;
        }

        Vec3 lookTarget = DragonOwnerFollowTarget.groundLookTarget(dragon, owner, followTarget);
        dragon.getLookControl().setLookAt(
                lookTarget.x,
                lookTarget.y,
                lookTarget.z,
                10.0F,
                dragon.getMaxHeadXRot()
        );
        if (distance <= stopDistance) {
            dragon.getAIMovement().stop();
            repathCooldown = 0;
            return;
        }
        if (dragon.getAIMovement().hasFailed()) {
            dragon.getAIMovement().stop();
            repathCooldown = FAILED_PATH_RETRY_TICKS;
            return;
        }
        if (repathCooldown > 0) repathCooldown--;
        if (dragon.getAIMovement().hasArrived()) {
            repathCooldown = 0;
        }
        if (repathCooldown <= 0) {
            double speed = fast ? config.fastSpeed : config.speed;
            boolean accepted = mountedOwner
                    ? dragon.getAIMovement().moveToProgressiveGroundPosition(
                            followTarget,
                            speed,
                            fast,
                            1.0D
                    )
                    : dragon.getAIMovement().moveToProgressiveGroundPosition(
                            followTarget,
                            speed,
                            fast
                    );
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

    private Config configFor(T dragon) {
        return dragon.isBaby() ? BABY_CONFIG : adultConfig;
    }

    private void resetTracking() {
        repathCooldown = 0;
        lastFollowTarget = null;
        mountedOwner = false;
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        return Map.of(
                "repath_cooldown", Integer.toString(repathCooldown),
                "target", lastFollowTarget == null ? "none" : lastFollowTarget.toString(),
                "mounted_owner", Boolean.toString(mountedOwner),
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
