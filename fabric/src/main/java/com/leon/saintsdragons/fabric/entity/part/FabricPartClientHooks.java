package com.leon.saintsdragons.fabric.entity.part;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.leon.saintsdragons.fabric.mixin.ClientLevelEntityMapAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

@Environment(EnvType.CLIENT)
public final class FabricPartClientHooks {
    private FabricPartClientHooks() {
    }

    public static void addClientPart(Level level, Entity part) {
        if (!(level instanceof ClientLevel clientLevel)) {
            return;
        }
        if (clientLevel.getEntity(part.getId()) != null) {
            return;
        }
        ((ClientLevelEntityMapAccessor) clientLevel).saintsdragons$addEntity(part.getId(), part);
    }

    public static void removeClientPart(Level level, Entity part) {
        if (!(level instanceof ClientLevel clientLevel)) {
            return;
        }
        if (clientLevel.getEntity(part.getId()) == null) {
            return;
        }
        ((ClientLevelEntityMapAccessor) clientLevel).saintsdragons$removeEntity(part.getId(), Entity.RemovalReason.DISCARDED);
    }
}
