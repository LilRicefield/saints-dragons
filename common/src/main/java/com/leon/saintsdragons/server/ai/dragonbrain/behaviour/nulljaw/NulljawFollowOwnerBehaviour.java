package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.nulljaw;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonOwnerFollowTarget;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonBabyOwnerFollowTuning;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class NulljawFollowOwnerBehaviour extends DragonBehaviour<Nulljaw> {
    private static final double ADULT_START_DISTANCE_SQR = 20.0D * 20.0D;
    private static final double ADULT_STOP_DISTANCE_SQR = 8.0D * 8.0D;
    private static final double ADULT_CATCH_UP_DISTANCE_SQR = 18.0D * 18.0D;
    private static final double BABY_START_DISTANCE_SQR = DragonBabyOwnerFollowTuning.START_DISTANCE
            * DragonBabyOwnerFollowTuning.START_DISTANCE;
    private static final double BABY_STOP_DISTANCE_SQR = DragonBabyOwnerFollowTuning.STOP_DISTANCE
            * DragonBabyOwnerFollowTuning.STOP_DISTANCE;
    private static final double BABY_CATCH_UP_DISTANCE_SQR = DragonBabyOwnerFollowTuning.RUN_DISTANCE
            * DragonBabyOwnerFollowTuning.RUN_DISTANCE;
    private static final double TARGET_EPSILON_SQR = 9.0D;
    private static final double FLIGHT_SPEED = 1.0D;

    @Nullable
    private Vec3 lastTarget;
    private int refreshCooldown;

    @Override
    protected boolean canStart(DragonBrainContext<Nulljaw> context) {
        Nulljaw dragon = context.dragon();
        LivingEntity owner = dragon.getOwner();
        return canFollow(dragon, owner)
                && DragonOwnerFollowTarget.anchorDistanceToSqr(dragon, owner)
                > DragonOwnerFollowTarget.startDistanceSqr(owner, startDistanceSqr(dragon));
    }

    @Override
    protected boolean canContinue(DragonBrainContext<Nulljaw> context) {
        Nulljaw dragon = context.dragon();
        LivingEntity owner = dragon.getOwner();
        return canFollow(dragon, owner)
                && DragonOwnerFollowTarget.anchorDistanceToSqr(dragon, owner) > stopDistanceSqr(dragon);
    }

    @Override
    protected void start(DragonBrainContext<Nulljaw> context) {
        lastTarget = null;
        refreshCooldown = 0;
        context.dragon().beginAiFlight();
    }

    @Override
    protected void tick(DragonBrainContext<Nulljaw> context) {
        Nulljaw dragon = context.dragon();
        LivingEntity owner = dragon.getOwner();
        if (owner == null) {
            return;
        }
        Vec3 lookTarget = DragonOwnerFollowTarget.visualTarget(owner);
        dragon.getLookControl().setLookAt(
                lookTarget.x,
                lookTarget.y,
                lookTarget.z,
                10.0F,
                10.0F
        );
        if (refreshCooldown > 0) {
            refreshCooldown--;
        }

        Vec3 target = DragonOwnerFollowTarget.airFormationTarget(
                owner,
                2.5D,
                3.0D,
                Math.sin(dragon.tickCount * 0.2D) * 0.3D
        );
        boolean catchUp = dragon.distanceToSqr(target) > catchUpDistanceSqr(dragon);
        double speed = catchUp ? FLIGHT_SPEED * 1.35D : FLIGHT_SPEED;
        dragon.setAccelerating(catchUp);
        if (dragon.distanceToSqr(target) <= 1.0D) {
            dragon.setAccelerating(false);
            dragon.getAIMovement().stop();
            return;
        }
        if (lastTarget == null
                || refreshCooldown <= 0
                || lastTarget.distanceToSqr(target) > TARGET_EPSILON_SQR) {
            context.memories().set(
                    DragonMemories.MOVEMENT_INTENT,
                    DragonMovementIntent.strictAir(target, speed)
            );
            lastTarget = target;
            refreshCooldown = speed >= 1.3D ? 3 : 5;
        }
    }

    @Override
    protected void stop(DragonBrainContext<Nulljaw> context) {
        context.memories().erase(DragonMemories.MOVEMENT_INTENT);
        context.dragon().setAccelerating(false);
        context.dragon().getAIMovement().stop();
        lastTarget = null;
        refreshCooldown = 0;
    }

    private boolean canFollow(Nulljaw dragon, @Nullable LivingEntity owner) {
        return dragon.isTame()
                && dragon.getCommand() == 0
                && !dragon.isOrderedToSit()
                && !dragon.isInLove()
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && !dragon.isSittingDownAnimation()
                && (dragon.getTarget() == null || !dragon.getTarget().isAlive())
                && owner != null
                && owner.isAlive()
                && owner.level() == dragon.level();
    }

    private double startDistanceSqr(Nulljaw dragon) {
        return dragon.isBaby() ? BABY_START_DISTANCE_SQR : ADULT_START_DISTANCE_SQR;
    }

    private double stopDistanceSqr(Nulljaw dragon) {
        return dragon.isBaby() ? BABY_STOP_DISTANCE_SQR : ADULT_STOP_DISTANCE_SQR;
    }

    private double catchUpDistanceSqr(Nulljaw dragon) {
        return dragon.isBaby() ? BABY_CATCH_UP_DISTANCE_SQR : ADULT_CATCH_UP_DISTANCE_SQR;
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        return Map.of(
                "target", lastTarget == null ? "none" : lastTarget.toString(),
                "refresh", Integer.toString(refreshCooldown)
        );
    }
}
