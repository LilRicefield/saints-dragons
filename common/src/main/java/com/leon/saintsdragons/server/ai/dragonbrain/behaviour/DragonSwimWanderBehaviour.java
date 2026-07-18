package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncSwimController;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.interfaces.SemiAquaticDragon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public final class DragonSwimWanderBehaviour<T extends RideableDragonBase & SemiAquaticDragon>
        extends DragonBehaviour<T> {
    private static final int TARGET_ATTEMPTS = 6;

    private final float turnSpeed;
    private final double speedModifier;
    private final int interval;
    private final Predicate<T> eligibility;
    private final BiPredicate<T, Vec3> targetFilter;
    @Nullable
    private Vec3 target;
    private int recalcTimer;
    private int obstructionCooldown;
    private boolean obstructed;

    public DragonSwimWanderBehaviour(float turnSpeed,
                                     double speedModifier,
                                     int interval,
                                     Predicate<T> eligibility,
                                     BiPredicate<T, Vec3> targetFilter) {
        this.turnSpeed = turnSpeed;
        this.speedModifier = speedModifier;
        this.interval = interval;
        this.eligibility = Objects.requireNonNull(eligibility);
        this.targetFilter = Objects.requireNonNull(targetFilter);
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        if (!basicConditions(dragon) || dragon.getRandom().nextInt(interval) != 0) {
            return false;
        }
        target = findTarget(dragon);
        return target != null;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        return basicConditions(dragon)
                && target != null
                && dragon.distanceToSqr(target) > 4.0D;
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        recalcTimer = 0;
        obstructionCooldown = 0;
        obstructed = false;
        context.dragon().getNavigation().stop();
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        if (target == null) {
            return;
        }
        dragon.getNavigation().stop();
        if (++recalcTimer > 100) {
            replaceTarget(dragon);
            recalcTimer = 0;
        }
        if (obstructionCooldown-- <= 0) {
            obstructed = isObstructed(dragon, dragon.position(), target);
            obstructionCooldown = 5;
        }
        if (obstructed) {
            replaceTarget(dragon);
        }

        AsyncSwimController controller = dragon.getAiSwimController();
        double speed = dragon.getSwimSpeed() * speedModifier;
        if (!controller.trackTarget(target, speed, turnSpeed)) {
            replaceTarget(dragon);
            return;
        }
        controller.serverTick();
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        target = null;
        context.dragon().getAiSwimController().stop();
    }

    private boolean basicConditions(T dragon) {
        return dragon.canSwim()
                && dragon.isInWaterOrBubble()
                && !dragon.isVehicle()
                && !dragon.isAerial()
                && !dragon.isInLove()
                && (dragon.getTarget() == null || !dragon.getTarget().isAlive())
                && (!dragon.isTame() || dragon.getCommand() == 2)
                && eligibility.test(dragon);
    }

    private void replaceTarget(T dragon) {
        Vec3 replacement = findTarget(dragon);
        if (replacement != null) {
            target = replacement;
            obstructed = false;
        }
    }

    @Nullable
    private Vec3 findTarget(T dragon) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int attempt = 0; attempt < TARGET_ATTEMPTS; attempt++) {
            double angle = dragon.getRandom().nextDouble() * Math.PI * 2.0D;
            double distance = 32.0D + dragon.getRandom().nextDouble() * 64.0D;
            int x = Mth.floor(dragon.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(dragon.getZ() + Math.sin(angle) * distance);
            int startY = Mth.floor(dragon.getY());
            if (!areaLoaded(dragon, x, z, x, z)) {
                continue;
            }

            cursor.set(x, startY, z);
            int surfaceY = startY;
            while (cursor.getY() < dragon.level().getMaxBuildHeight()
                    && dragon.level().getFluidState(cursor).is(FluidTags.WATER)) {
                surfaceY = cursor.getY();
                cursor.move(0, 1, 0);
            }
            cursor.setY(startY);
            int bottomY = startY;
            while (cursor.getY() > dragon.level().getMinBuildHeight()
                    && dragon.level().getFluidState(cursor).is(FluidTags.WATER)) {
                bottomY = cursor.getY();
                cursor.move(0, -1, 0);
            }
            int minY = bottomY + 3;
            int maxY = surfaceY - 1;
            if (minY >= maxY) {
                continue;
            }
            int y = dragon.getRandom().nextFloat() < 0.25F
                    ? Math.max(minY, maxY - 2)
                    : minY + dragon.getRandom().nextInt(maxY - minY + 1);
            cursor.set(x, y, z);
            if (!dragon.level().getFluidState(cursor).is(FluidTags.WATER)) {
                continue;
            }
            Vec3 candidate = new Vec3(x + 0.5D, y, z + 0.5D);
            if (targetFilter.test(dragon, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isObstructed(T dragon, Vec3 from, Vec3 to) {
        int minX = Mth.floor(Math.min(from.x, to.x)) - 1;
        int maxX = Mth.floor(Math.max(from.x, to.x)) + 1;
        int minZ = Mth.floor(Math.min(from.z, to.z)) - 1;
        int maxZ = Mth.floor(Math.max(from.z, to.z)) + 1;
        if (!areaLoaded(dragon, minX, minZ, maxX, maxZ)) {
            return true;
        }
        HitResult hit = dragon.level().clip(new ClipContext(
                from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, dragon));
        return hit.getType() != HitResult.Type.MISS;
    }

    private boolean areaLoaded(T dragon, int minX, int minZ, int maxX, int maxZ) {
        int minChunkX = SectionPos.blockToSectionCoord(Math.min(minX, maxX));
        int maxChunkX = SectionPos.blockToSectionCoord(Math.max(minX, maxX));
        int minChunkZ = SectionPos.blockToSectionCoord(Math.min(minZ, maxZ));
        int maxChunkZ = SectionPos.blockToSectionCoord(Math.max(minZ, maxZ));
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                BlockPos sample = new BlockPos((chunkX << 4) + 8, Mth.floor(dragon.getY()), (chunkZ << 4) + 8);
                if (!dragon.level().hasChunkAt(sample)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        return Map.of("swim_target", target == null ? "none" : target.toString());
    }
}
