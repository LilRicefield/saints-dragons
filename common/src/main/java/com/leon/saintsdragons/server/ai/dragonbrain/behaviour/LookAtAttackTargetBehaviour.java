package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonTargetLifecycle;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Map;

public class LookAtAttackTargetBehaviour<T extends DragonEntity> extends DragonBehaviour<T> {
    private final float yawSpeed;
    private final float pitchSpeed;

    public LookAtAttackTargetBehaviour(float yawSpeed, float pitchSpeed) {
        super(Map.of(DragonMemories.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT), false);
        this.yawSpeed = yawSpeed;
        this.pitchSpeed = pitchSpeed;
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        return hasVisibleTarget(context);
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        return hasVisibleTarget(context);
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        if (!context.memories().get(DragonMemories.TARGET_VISIBLE).orElse(false)) {
            return;
        }
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (target != null) {
            context.dragon().getLookControl().setLookAt(target, yawSpeed, pitchSpeed);
        }
    }

    private boolean hasVisibleTarget(DragonBrainContext<T> context) {
        return context.memories().get(DragonMemories.TARGET_VISIBLE).orElse(false)
                && context.memories().get(DragonMemories.ATTACK_TARGET)
                .filter(target -> DragonTargetLifecycle.isValidTarget(context.dragon(), target))
                .isPresent();
    }
}
