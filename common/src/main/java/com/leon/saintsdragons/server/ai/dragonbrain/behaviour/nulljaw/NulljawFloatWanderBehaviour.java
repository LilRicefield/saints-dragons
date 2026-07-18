package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.nulljaw;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public final class NulljawFloatWanderBehaviour extends DragonBehaviour<Nulljaw> {
    private static final int HORIZONTAL_RANGE = 16;
    private static final int VERTICAL_RANGE = 8;
    private static final int TARGET_ATTEMPTS = 8;
    private static final int MIN_REST_TICKS = 20;
    private static final int RANDOM_REST_TICKS = 41;
    private static final double SPEED = 1.0D;

    @Nullable
    private Vec3 target;

    @Override
    protected boolean canStart(DragonBrainContext<Nulljaw> context) {
        Nulljaw dragon = context.dragon();
        if (!dragon.canFloatWander()
                || !dragon.isAiFlightDone()) {
            return false;
        }
        target = findTarget(dragon);
        return target != null;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<Nulljaw> context) {
        return target != null
                && context.dragon().canFloatWander()
                && !higherPriorityMovementNeeded(context)
                && (!context.dragon().isAiFlightDone()
                    || context.memories().has(DragonMemories.MOVEMENT_INTENT));
    }

    @Override
    protected void start(DragonBrainContext<Nulljaw> context) {
        if (target != null) {
            context.dragon().beginAiFlight();
            context.memories().set(
                    DragonMemories.MOVEMENT_INTENT,
                    DragonMovementIntent.strictAir(target, SPEED)
            );
        }
    }

    @Override
    protected void stop(DragonBrainContext<Nulljaw> context) {
        context.memories().erase(DragonMemories.MOVEMENT_INTENT);
        if (!context.dragon().isAiFlightDone()) {
            context.dragon().getAIMovement().stop();
        }
        target = null;
    }

    @Override
    protected int cooldownForTicks(DragonBrainContext<Nulljaw> context) {
        return MIN_REST_TICKS + context.dragon().getRandom().nextInt(RANDOM_REST_TICKS);
    }

    private boolean higherPriorityMovementNeeded(DragonBrainContext<Nulljaw> context) {
        Nulljaw dragon = context.dragon();
        if (dragon.isInLove()) {
            return true;
        }
        if ((dragon.tickCount & 3) == 0 && NulljawTemptBehaviour.hasTemptingPlayer(dragon)) {
            return true;
        }
        if (dragon.isTame() && dragon.getCommand() == 0 && dragon.getOwner() != null
                && dragon.distanceToSqr(dragon.getOwner()) > 64.0D) {
            return true;
        }
        UUID leaderId = dragon.getPackLeaderUuid();
        if (leaderId == null || leaderId.equals(dragon.getUUID())) {
            return false;
        }
        Entity leader = context.level().getEntity(leaderId);
        return leader instanceof Nulljaw && dragon.distanceToSqr(leader) > 81.0D;
    }

    @Nullable
    private Vec3 findTarget(Nulljaw dragon) {
        for (int attempt = 0; attempt < TARGET_ATTEMPTS; attempt++) {
            Vec3 candidate = findCandidate(dragon);
            if (isUsableTarget(dragon, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    @Nullable
    private Vec3 findCandidate(Nulljaw dragon) {
        Vec3 view = dragon.getViewVector(0.0F);
        Vec3 candidate = HoverRandomPos.getPos(
                dragon,
                HORIZONTAL_RANGE,
                VERTICAL_RANGE,
                view.x,
                view.z,
                (float)Math.PI / 2.0F,
                6,
                4
        );
        if (candidate == null) {
            Vec3 forward = dragon.position().add(view);
            candidate = AirAndWaterRandomPos.getPos(
                    dragon,
                    HORIZONTAL_RANGE,
                    VERTICAL_RANGE - 2,
                    -4,
                    forward.x,
                    forward.z,
                    (float)Math.PI / 2.0F
            );
        }
        if (candidate == null) {
            return null;
        }
        return candidate;
    }

    private boolean isUsableTarget(Nulljaw dragon, @Nullable Vec3 candidate) {
        if (candidate == null) {
            return false;
        }
        BlockPos position = BlockPos.containing(candidate);
        if (!dragon.level().hasChunkAt(position) || !dragon.level().getFluidState(position).isEmpty()) {
            return false;
        }
        Vec3 offset = candidate.subtract(dragon.position());
        return dragon.level().noCollision(dragon, dragon.getBoundingBox().move(offset));
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        return Map.of("target", target == null ? "none" : target.toString());
    }
}
