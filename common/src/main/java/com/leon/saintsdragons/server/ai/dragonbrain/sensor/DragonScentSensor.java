package com.leon.saintsdragons.server.ai.dragonbrain.sensor;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonSensor;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonAwarenessMemory;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonScentProfile;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonScentEligibility;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonSensoryObservation;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class DragonScentSensor<T extends DragonEntity> extends DragonSensor<T> {
    public DragonScentSensor(int scanRateTicks) {
        super(scanRateTicks);
    }

    @Override
    protected boolean canScan(DragonBrainContext<T> context) {
        return DragonScentEligibility.isAvailable(context.dragon(), context.memories())
                && !context.memories().has(DragonMemories.SCENT_CANDIDATE)
                && !context.memories().has(DragonMemories.SCENT_COOLDOWN);
    }

    @Override
    protected void scan(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        DragonScentProfile profile = DragonScentProfile.forDragon(dragon);
        AABB searchArea = dragon.getBoundingBox().inflate(
                profile.horizontalRange(),
                profile.verticalRange(),
                profile.horizontalRange()
        );
        List<LivingEntity> candidates = context.level().getEntitiesOfClass(
                LivingEntity.class,
                searchArea,
                candidate -> canSmell(dragon, candidate)
        );
        candidates.sort(Comparator.comparingDouble(dragon::distanceToSqr));

        DragonAwarenessMemory awareness = DragonAwarenessMemory.get(dragon);
        for (LivingEntity candidate : candidates) {
            if (dragon.getSensing().hasLineOfSight(candidate)) {
                continue;
            }
            double distance = dragon.distanceTo(candidate);
            double uncertainty = profile.uncertainty(distance);
            Vec3 estimatedPosition = candidate.position().add(
                    (dragon.getRandom().nextDouble() * 2.0D - 1.0D) * uncertainty,
                    (dragon.getRandom().nextDouble() - 0.5D) * Math.min(1.5D, uncertainty * 0.35D),
                    (dragon.getRandom().nextDouble() * 2.0D - 1.0D) * uncertainty
            );
            DragonSensoryObservation observation = new DragonSensoryObservation(
                    estimatedPosition,
                    candidate.getUUID(),
                    DragonSensoryObservation.Kind.SCENT,
                    profile.confidence(distance),
                    context.gameTime()
            );
            if (!awareness.rememberScent(observation, context.gameTime())) {
                continue;
            }
            context.memories().set(DragonMemories.SCENT_CANDIDATE, observation, profile.maxAssessmentTicks() + 20);
            return;
        }
    }

    private boolean canSmell(T dragon, LivingEntity candidate) {
        if (candidate == dragon
                || !candidate.isAlive()
                || candidate.isSpectator()
                || candidate instanceof DragonEntity
                || !(candidate instanceof Mob || candidate instanceof Player)
                || dragon.isAlly(candidate)) {
            return false;
        }
        return !(candidate instanceof Player player) || !player.isCreative();
    }

    @Override
    protected Set<MemoryModuleType<?>> memoriesUsed() {
        return Set.of(
                DragonMemories.ATTACK_TARGET,
                DragonMemories.BREED_TARGET,
                DragonMemories.INVESTIGATION_TARGET,
                DragonMemories.SCENT_CANDIDATE,
                DragonMemories.SCENT_COOLDOWN,
                DragonMemories.RESCUE_TARGET,
                DragonMemories.INTERCEPT_PROJECTILE,
                DragonMemories.AWARENESS
        );
    }
}
