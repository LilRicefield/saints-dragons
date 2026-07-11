package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncSwimController;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.interfaces.SemiAquaticDragon;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.ToDoubleBiFunction;

public class AsyncWaterChaseTargetBehaviour<T extends RideableDragonBase> extends DragonBehaviour<T> {
    private final ToDoubleBiFunction<T, LivingEntity> speedModifier;
    private final BiPredicate<T, LivingEntity> movementLocked;
    private final float turnSpeed;

    public AsyncWaterChaseTargetBehaviour(double speedModifier, float turnSpeed) {
        this((dragon, target) -> speedModifier, turnSpeed, (dragon, target) -> false);
    }

    public AsyncWaterChaseTargetBehaviour(ToDoubleBiFunction<T, LivingEntity> speedModifier,
                                          float turnSpeed) {
        this(speedModifier, turnSpeed, (dragon, target) -> false);
    }

    public AsyncWaterChaseTargetBehaviour(ToDoubleBiFunction<T, LivingEntity> speedModifier,
                                          float turnSpeed,
                                          BiPredicate<T, LivingEntity> movementLocked) {
        super(Map.of(DragonMemories.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
        this.speedModifier = speedModifier;
        this.turnSpeed = turnSpeed;
        this.movementLocked = movementLocked;
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        return isWaterCombatContext(context);
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        return isWaterCombatContext(context);
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        context.dragon().getNavigation().stop();
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (target == null) {
            return;
        }

        AsyncSwimController controller = dragon.getAiSwimController();
        if (context.memories().has(DragonMemories.MOVEMENT_INTENT)) {
            return;
        }
        if (movementLocked.test(dragon, target)) {
            controller.stop();
            return;
        }
        Vec3 targetPosition = target.position().add(0.0D, target.getEyeHeight() * 0.5D, 0.0D);
        double speed = speedModifier.applyAsDouble(dragon, target);
        if (dragon instanceof SemiAquaticDragon swimmer) {
            speed *= swimmer.getSwimSpeed();
        }
        if (dragon.distanceToSqr(target) > 225.0D) {
            speed *= 1.5D;
        }
        if (controller.trackTarget(targetPosition, speed, turnSpeed)) {
            controller.serverTick();
        }
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        context.dragon().getAiSwimController().stop();
    }

    private boolean isWaterCombatContext(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        return dragon.isInWaterOrBubble()
                && !dragon.isVehicle()
                && target != null
                && dragon.isTargetValid(target);
    }
}
