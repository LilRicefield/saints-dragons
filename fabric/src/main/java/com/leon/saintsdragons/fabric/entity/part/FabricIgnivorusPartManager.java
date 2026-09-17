package com.leon.saintsdragons.fabric.entity.part;

import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.part.DragonPartManager;

public final class FabricIgnivorusPartManager {
    private final Ignivorus dragon;
    private final DragonPartManager<FabricDragonPart> manager;
    private final FabricDragonPart[] parts;

    public FabricIgnivorusPartManager(Ignivorus dragon) {
        this.dragon = dragon;
        manager = new DragonPartManager<>(dragon, index -> new FabricDragonPart(dragon, index));
        manager.update();
        parts = manager.parts().toArray(FabricDragonPart[]::new);
    }

    public void updatePartPositions() {
        manager.update();
        if (dragon.level().isClientSide) {
            for (FabricDragonPart part : parts) {
                if (!part.isRemoved() && dragon.level().getEntity(part.getId()) == null)
                    FabricPartClientHooks.addClientPart(dragon.level(), part);
            }
        } else if (dragon.level() instanceof FabricDragonPartIndex.Access access) {
            access.saintsdragons$partIndex().update(dragon, parts);
        }
    }

    public FabricDragonPart[] getParts() { return parts; }

    public void removeAllParts() {
        if (dragon.level().isClientSide) {
            for (FabricDragonPart part : parts) FabricPartClientHooks.removeClientPart(dragon.level(), part);
        } else if (dragon.level() instanceof FabricDragonPartIndex.Access access) {
            access.saintsdragons$partIndex().remove(dragon);
        }
        manager.remove();
    }
}
