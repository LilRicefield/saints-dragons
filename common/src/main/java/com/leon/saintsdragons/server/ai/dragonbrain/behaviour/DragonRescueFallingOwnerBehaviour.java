package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DragonRescueFallingOwnerBehaviour<T extends RideableFlyingDragon>
        extends DragonBehaviour<T> {
    private static final int WANDERING_COMMAND = 2;

    private final Config config;
    private int targetRefreshCooldown;
    @Nullable
    private Vec3 interceptTarget;
    private String phase = "idle";

    public DragonRescueFallingOwnerBehaviour(Config config) {
        super(Map.of(
                DragonMemories.RESCUE_TARGET, MemoryStatus.VALUE_PRESENT,
                DragonMemories.MOVEMENT_INTENT, MemoryStatus.REGISTERED
        ));
        this.config = config;
    }

    public static <T extends RideableFlyingDragon> boolean updateRescueTarget(
            Brain<T> brain,
            T dragon,
            Config config
    ) {
        LivingEntity committed = brain.getMemory(DragonMemories.RESCUE_TARGET).orElse(null);
        if (committed != null) {
            if (isCommittedTargetValid(dragon, committed, config)) {
                return true;
            }
            brain.eraseMemory(DragonMemories.RESCUE_TARGET);
        }

        if (!(dragon.getOwner() instanceof ServerPlayer owner)
                || !canRespond(dragon, owner, config)
                || !isDangerousFall(owner, config)
                || !isPreferredRescuer(dragon, owner, config)) {
            return false;
        }

        brain.setMemory(DragonMemories.RESCUE_TARGET, owner);
        return true;
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        LivingEntity target = rescueTarget(context);
        return target instanceof ServerPlayer owner && canRespond(context.dragon(), owner, config);
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        LivingEntity target = rescueTarget(context);
        return target != null && isCommittedTargetValid(context.dragon(), target, config);
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        dragon.wakeUpImmediately();
        dragon.getAIMovement().stopAndClearAllMovement();
        dragon.setOnGround(false);
        dragon.setFlying(true);
        dragon.setTakeoff(false);
        dragon.setLanding(false);
        dragon.setHovering(false);
        dragon.setGoingUp(false);
        dragon.setGoingDown(false);
        dragon.setAccelerating(true);
        targetRefreshCooldown = 0;
        interceptTarget = null;
        phase = "intercepting";
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        LivingEntity target = rescueTarget(context);
        if (!(target instanceof ServerPlayer owner)) {
            return;
        }

        dragon.getLookControl().setLookAt(owner, 30.0F, 30.0F);
        if (tryCatch(dragon, owner)) {
            context.memories().erase(DragonMemories.RESCUE_TARGET);
            context.memories().erase(DragonMemories.MOVEMENT_INTENT);
            phase = "caught";
            return;
        }

        if (targetRefreshCooldown > 0) {
            targetRefreshCooldown--;
            return;
        }

        interceptTarget = predictIntercept(dragon, owner, config);
        context.memories().set(
                DragonMemories.MOVEMENT_INTENT,
                DragonMovementIntent.auto(interceptTarget, config.flightSpeed)
        );
        dragon.setAccelerating(true);
        targetRefreshCooldown = config.targetRefreshTicks;
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        if (!dragon.isVehicle()) {
            dragon.getAIMovement().stop();
            dragon.setAccelerating(false);
        }
        targetRefreshCooldown = 0;
        interceptTarget = null;
        phase = "idle";
    }

    @Override
    public List<MemoryModuleType<?>> clearMemoriesWhenStopped() {
        return List.of(DragonMemories.RESCUE_TARGET, DragonMemories.MOVEMENT_INTENT);
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("phase", phase);
        details.put("refresh", Integer.toString(targetRefreshCooldown));
        details.put("intercept", interceptTarget == null ? "none" : interceptTarget.toString());
        return details;
    }

    public String getRescueDebugSummary(RideableFlyingDragon dragon) {
        LivingEntity committed = dragon.getBrain().getMemory(DragonMemories.RESCUE_TARGET).orElse(null);
        if (committed != null) {
            return "active:target=" + committed.getId() + ",phase=" + phase;
        }
        if (!(dragon.getOwner() instanceof ServerPlayer owner)) {
            return "disabled:no-player-owner";
        }
        if (dragon.getCommand() != WANDERING_COMMAND) {
            return "disabled:command=" + commandName(dragon.getCommand());
        }
        if (!canRespond(dragon, owner, config)) {
            return "blocked:dragon-unavailable";
        }
        String fallRejection = fallRejectionReason(owner, config);
        if (fallRejection != null) {
            return "waiting:" + fallRejection;
        }
        if (!isPreferredRescuer(dragon, owner, config)) {
            return "blocked:closer-rescuer";
        }
        return "ready";
    }

    private static String commandName(int command) {
        return switch (command) {
            case 0 -> "following";
            case 1 -> "sitting";
            case WANDERING_COMMAND -> "wandering";
            default -> "unknown-" + command;
        };
    }

    @Nullable
    private LivingEntity rescueTarget(DragonBrainContext<T> context) {
        return context.memories().get(DragonMemories.RESCUE_TARGET).orElse(null);
    }

    private boolean tryCatch(T dragon, ServerPlayer owner) {
        if (!dragon.getBoundingBox().inflate(
                config.catchHorizontalRange,
                config.catchVerticalRange,
                config.catchHorizontalRange
        ).intersects(owner.getBoundingBox())) {
            return false;
        }
        if (!owner.startRiding(dragon, true)) {
            return false;
        }

        owner.fallDistance = 0.0F;
        owner.setDeltaMovement(Vec3.ZERO);
        dragon.fallDistance = 0.0F;
        dragon.getAIMovement().stopAndClearAllMovement();
        dragon.setAccelerating(false);
        return true;
    }

    private static Vec3 predictIntercept(RideableFlyingDragon dragon, ServerPlayer owner, Config config) {
        double distance = dragon.distanceTo(owner);
        double leadTicks = Mth.clamp(
                distance / config.leadDistancePerTick,
                config.minimumLeadTicks,
                config.maximumLeadTicks
        );
        Vec3 velocity = owner.getDeltaMovement();
        double targetX = owner.getX() + velocity.x * leadTicks;
        double targetY = owner.getY() + velocity.y * leadTicks
                - config.gravityPrediction * leadTicks * leadTicks
                - dragon.getBbHeight() * config.verticalCatchOffset;
        double targetZ = owner.getZ() + velocity.z * leadTicks;
        targetY = Math.max(dragon.level().getMinBuildHeight() + 1.0D, targetY);
        return new Vec3(targetX, targetY, targetZ);
    }

    private static boolean isCommittedTargetValid(
            RideableFlyingDragon dragon,
            LivingEntity target,
            Config config
    ) {
        return target instanceof ServerPlayer owner
                && dragon.isOwnedBy(owner)
                && target.isAlive()
                && !target.isRemoved()
                && target.level() == dragon.level()
                && !target.onGround()
                && !target.isPassenger()
                && !target.isInWaterOrBubble()
                && !target.isFallFlying()
                && !owner.isSpectator()
                && !owner.isCreative()
                && dragon.getCommand() == WANDERING_COMMAND
                && dragon.distanceToSqr(target)
                <= config.maxCommitmentDistance * config.maxCommitmentDistance;
    }

    private static boolean canRespond(
            RideableFlyingDragon dragon,
            ServerPlayer owner,
            Config config
    ) {
        return dragon.isAlive()
                && !dragon.isDying()
                && dragon.isTame()
                && dragon.isOwnedBy(owner)
                && dragon.getCommand() == WANDERING_COMMAND
                && dragon.canFly()
                && !dragon.isBaby()
                && !dragon.isOrderedToSit()
                && !dragon.isSleeping()
                && !dragon.isSleepTransitioning()
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && !dragon.isInWaterOrBubble()
                && dragon.getActiveAbility() == null
                && !dragon.areRiderControlsLocked()
                && (dragon.isAerial() || dragon.canTakeoff())
                && dragon.distanceToSqr(owner)
                <= config.maxResponseDistance * config.maxResponseDistance;
    }

    private static boolean isPreferredRescuer(
            RideableFlyingDragon dragon,
            ServerPlayer owner,
            Config config
    ) {
        double ownDistance = dragon.distanceToSqr(owner);
        for (RideableFlyingDragon candidate : dragon.level().getEntitiesOfClass(
                RideableFlyingDragon.class,
                owner.getBoundingBox().inflate(config.maxResponseDistance),
                candidate -> isRescueCapableSpecies(candidate)
                        && canRespond(candidate, owner, config)
        )) {
            double candidateDistance = candidate.distanceToSqr(owner);
            if (candidateDistance + 1.0E-4D < ownDistance
                    || Math.abs(candidateDistance - ownDistance) <= 1.0E-4D
                    && candidate.getId() < dragon.getId()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isRescueCapableSpecies(RideableFlyingDragon dragon) {
        return dragon instanceof Raevyx
                || dragon instanceof Cindervane
                || dragon instanceof Ignivorus
                || dragon instanceof Volitans;
    }

    private static boolean isDangerousFall(ServerPlayer owner, Config config) {
        return fallRejectionReason(owner, config) == null;
    }

    @Nullable
    private static String fallRejectionReason(ServerPlayer owner, Config config) {
        if (!owner.isAlive()) return "owner-unavailable";
        if (owner.onGround()) return "owner-grounded";
        if (owner.isPassenger()) return "owner-mounted";
        if (owner.isInWaterOrBubble()) return "owner-in-water";
        if (owner.isFallFlying()) return "owner-gliding";
        if (owner.isSpectator() || owner.isCreative() || owner.getAbilities().flying) {
            return "owner-can-fly";
        }
        if (owner.hasEffect(MobEffects.SLOW_FALLING)) return "slow-falling";
        if (owner.getDeltaMovement().y > config.maximumTriggerVelocity) return "not-falling-fast-enough";
        if (owner.fallDistance < config.minimumFallDistance) return "fall-too-short";

        Vec3 start = owner.position();
        Vec3 end = start.add(0.0D, -config.groundScanDepth, 0.0D);
        BlockHitResult landing = owner.level().clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                owner
        ));
        if (landing.getType() == HitResult.Type.MISS) {
            return null;
        }
        if (owner.level().getFluidState(landing.getBlockPos()).is(FluidTags.WATER)) {
            return "water-below";
        }
        return start.y - landing.getLocation().y >= config.minimumRemainingDrop
                ? null
                : "ground-too-close";
    }

    public record Config(
            double maxResponseDistance,
            double maxCommitmentDistance,
            double minimumFallDistance,
            double minimumRemainingDrop,
            double groundScanDepth,
            double maximumTriggerVelocity,
            double flightSpeed,
            double catchHorizontalRange,
            double catchVerticalRange,
            double leadDistancePerTick,
            double minimumLeadTicks,
            double maximumLeadTicks,
            double gravityPrediction,
            double verticalCatchOffset,
            int targetRefreshTicks
    ) {
        public static Config raevyx() {
            return standard(4.5D, 1.5D, 1.75D);
        }

        public static Config cindervane() {
            return standard(5.0D, 1.5D, 1.75D);
        }

        public static Config ignivorus() {
            return standard(3.8D, 2.5D, 3.0D);
        }

        public static Config volitans() {
            return standard(4.2D, 2.0D, 2.5D);
        }

        private static Config standard(
                double flightSpeed,
                double catchHorizontalRange,
                double catchVerticalRange
        ) {
            return new Config(
                    256.0D,
                    256.0D,
                    1.5D,
                    20.0D,
                    96.0D,
                    -0.12D,
                    flightSpeed,
                    catchHorizontalRange,
                    catchVerticalRange,
                    2.0D,
                    1.5D,
                    12.0D,
                    0.04D,
                    0.65D,
                    2
            );
        }
    }
}
