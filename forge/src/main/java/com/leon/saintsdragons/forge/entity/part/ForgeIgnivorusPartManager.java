package com.leon.saintsdragons.forge.entity.part;

import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.part.DragonPartManager;

public final class ForgeIgnivorusPartManager {
    private final Ignivorus dragon;
    private final DragonPartManager<ForgeDragonPart> manager;
    private final ForgeDragonPart[] parts;

    public ForgeIgnivorusPartManager(Ignivorus dragon) {
        this.dragon = dragon;
        manager = new DragonPartManager<>(dragon, index -> new ForgeDragonPart(dragon, index));
        manager.update();
        parts = manager.parts().toArray(ForgeDragonPart[]::new);
    }

    public void updatePartPositions() {
        manager.update();
    }

    public ForgeDragonPart[] getParts() { return parts; }

    public void removeAllParts() {
        manager.remove();
    }
}
