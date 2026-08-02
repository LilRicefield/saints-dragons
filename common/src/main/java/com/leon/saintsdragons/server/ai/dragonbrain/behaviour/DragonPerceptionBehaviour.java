package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonTargetLifecycle;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonInvestigation;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonAwarenessMemory;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonPerceptionProfile;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonSensoryObservation;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DragonPerceptionBehaviour<T extends DragonEntity> extends DragonBehaviour<T> {
    private boolean targetVisible;
    private String lastObservation = "none";
    private int familiarSources;

    public DragonPerceptionBehaviour() {
        super(false);
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        return true;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        return true;
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        DragonAwarenessMemory awareness = DragonAwarenessMemory.get(dragon);
        familiarSources = awareness.familiarSourceCount();
        if (target == null) {
            targetVisible = false;
            lastObservation = "none";
            lookTowardAttention(context);
            return;
        }

        awareness.rememberThreat(target.getUUID(), context.gameTime());

        targetVisible = context.memories().get(DragonMemories.TARGET_VISIBLE).orElse(false);

        if (targetVisible) {
            context.memories().erase(DragonMemories.INVESTIGATION_TARGET);
            lastObservation = "sight";
            return;
        }

        DragonPerceptionProfile profile = DragonPerceptionProfile.forDragon(dragon);
        DragonSensoryObservation remembered = context.memories()
                .get(DragonMemories.LAST_SEEN_TARGET)
                .orElse(null);
        DragonSensoryObservation heard = context.memories()
                .get(DragonMemories.HEARD_TARGET)
                .filter(observation -> observation.sourceUuid() != null
                        && observation.sourceUuid().equals(target.getUUID()))
                .orElse(null);
        if (heard != null && (remembered == null || heard.observedAt() > remembered.observedAt())) {
            remembered = heard;
            context.memories().set(
                    DragonMemories.LAST_SEEN_TARGET,
                    heard,
                    profile.soundMemoryTicks()
            );
            lastObservation = "heard_target_"
                    + heard.kind().name().toLowerCase(java.util.Locale.ROOT);
        }
        boolean hasFreshEvidence = remembered != null
                && target.getUUID().equals(remembered.sourceUuid());
        if (hasFreshEvidence) {
            DragonInvestigation.remember(dragon, remembered);
        }

        DragonSensoryObservation investigation = context.memories()
                .get(DragonMemories.INVESTIGATION_TARGET)
                .filter(observation -> target.getUUID().equals(observation.sourceUuid()))
                .orElse(null);
        if (!hasFreshEvidence && investigation == null) {
            if (dragon.getTarget() == null || dragon.getTarget() == target) {
                DragonTargetLifecycle.clearCombatTarget(context.memories(), dragon, false);
            } else {
                DragonTargetLifecycle.clearTargetMemories(context.memories());
            }
            lastObservation = "forgotten";
            return;
        }

        DragonSensoryObservation focus = hasFreshEvidence ? remembered : investigation;
        if (!hasFreshEvidence) {
            lastObservation = "investigating_"
                    + focus.kind().name().toLowerCase(java.util.Locale.ROOT);
        } else if (!lastObservation.startsWith("heard_target_")) {
            lastObservation = "last_seen";
        }
        Vec3 position = focus.position();
        dragon.getLookControl().setLookAt(
                position.x,
                position.y,
                position.z,
                10.0F,
                dragon.getMaxHeadXRot()
        );
    }

    private void lookTowardAttention(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        if (dragon.isVehicle() || dragon.isOrderedToSit() || dragon.isSleepLocked()) {
            return;
        }
        DragonSensoryObservation heard = DragonAwarenessMemory.get(dragon)
                .attention(context.gameTime());
        if (heard == null) {
            return;
        }
        Vec3 position = heard.position();
        dragon.getLookControl().setLookAt(
                position.x,
                position.y,
                position.z,
                8.0F,
                dragon.getMaxHeadXRot()
        );
        lastObservation = "heard_" + heard.kind().name().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("target_visible", Boolean.toString(targetVisible));
        details.put("observation", lastObservation);
        details.put("familiar_sources", Integer.toString(familiarSources));
        return Map.copyOf(details);
    }
}
