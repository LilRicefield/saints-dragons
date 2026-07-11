package com.leon.saintsdragons.server.ai.dragonbrain;

import com.mojang.serialization.Dynamic;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;

public final class DragonBrain {
    private DragonBrain() {
    }

    public static <T extends DragonEntity> Brain<T> makeBrain(DragonBrainOwner<T> owner, Dynamic<?> dynamic) {
        return owner.makeBrain(owner.brainProvider().makeBrain(dynamic));
    }

    public static <T extends DragonEntity> void tick(DragonBrainOwner<T> owner, T dragon) {
        if (dragon.level() instanceof ServerLevel serverLevel) {
            owner.tickBrain(serverLevel, dragon);
        }
    }
}
