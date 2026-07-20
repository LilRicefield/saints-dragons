package com.leon.saintsdragons.server.ai.dragonbrain.sensor;

import com.leon.saintsdragons.server.ai.DragonAirCombatSettingsProvider;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonSensor;
import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonSensoryObservation;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;

import java.util.Set;

public class DragonTargetSensor<T extends DragonEntity> extends DragonSensor<T> {
    private final double airborneHeightThreshold;

    public DragonTargetSensor(int scanRateTicks) {
        this(scanRateTicks, 8.0D);
    }

    public DragonTargetSensor(int scanRateTicks, double airborneHeightThreshold) {
        super(scanRateTicks);
        this.airborneHeightThreshold = airborneHeightThreshold;
    }

    @Override
    protected void scan(DragonBrainContext<T> context) {
        LivingEntity target = context.dragon().getTarget();
        if (!isValidTarget(context.dragon(), target)) {
            LivingEntity rememberedTarget = context.memories()
                    .get(DragonMemories.ATTACK_TARGET)
                    .orElse(null);
            if (target != null) {
                context.dragon().setTarget(null);
            }
            DragonSensoryObservation investigation = context.memories()
                    .get(DragonMemories.INVESTIGATION_TARGET)
                    .orElse(null);
            boolean matchesEntityTarget = investigation != null
                    && target != null
                    && target.getUUID().equals(investigation.sourceUuid());
            boolean matchesRememberedTarget = investigation != null
                    && rememberedTarget != null
                    && rememberedTarget.getUUID().equals(investigation.sourceUuid());
            if (investigation != null
                    && investigation.sourceUuid() != null
                    && (matchesEntityTarget || matchesRememberedTarget)) {
                context.memories().erase(DragonMemories.INVESTIGATION_TARGET);
            }
            context.memories().erase(DragonMemories.ATTACK_TARGET);
            context.memories().erase(DragonMemories.TARGET_AIRBORNE);
            context.memories().erase(DragonMemories.TARGET_VISIBLE);
            context.memories().erase(DragonMemories.LAST_SEEN_TARGET);
            context.memories().erase(DragonMemories.HEARD_TARGET);
            return;
        }

        double targetAirborneHeight = context.dragon() instanceof DragonAirCombatSettingsProvider provider
                ? provider.getAiTargetAirborneHeight(target)
                : airborneHeightThreshold;
        context.memories().set(DragonMemories.ATTACK_TARGET, target, scanMemoryTtl());
        context.memories().set(DragonMemories.TARGET_AIRBORNE,
                DragonTargetingHelper.isTargetAirborne(target, targetAirborneHeight) && !target.isInWaterOrBubble(),
                scanMemoryTtl());
    }

    protected int scanMemoryTtl() {
        return 5;
    }

    protected boolean isValidTarget(T dragon, LivingEntity target) {
        if (target == null || !dragon.isTargetValid(target)) {
            return false;
        }
        return !(target instanceof Player player) || (!player.isCreative() && !player.isSpectator());
    }

    @Override
    protected Set<MemoryModuleType<?>> memoriesUsed() {
        return Set.of(
                DragonMemories.ATTACK_TARGET,
                DragonMemories.TARGET_AIRBORNE,
                DragonMemories.TARGET_VISIBLE,
                DragonMemories.LAST_SEEN_TARGET,
                DragonMemories.INVESTIGATION_TARGET,
                DragonMemories.HEARD_TARGET
        );
    }
}
