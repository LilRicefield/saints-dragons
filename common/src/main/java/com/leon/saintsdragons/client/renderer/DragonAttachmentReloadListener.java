package com.leon.saintsdragons.client.renderer;

import com.leon.saintsdragons.client.camera.DragonRideCameraController;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public final class DragonAttachmentReloadListener implements ResourceManagerReloadListener {
    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        RiderBullcrap.clear();
        DragonRideCameraController.reset();
        var level = Minecraft.getInstance().level;
        if (level != null) {
            for (var entity : level.entitiesForRendering()) {
                if (entity instanceof RideableDragonBase dragon) {
                    dragon.clearClientLocatorPositions();
                }
            }
        }
    }
}
