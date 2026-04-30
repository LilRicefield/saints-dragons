package com.leon.saintsdragons.fabric;

import com.leon.saintsdragons.client.init.CommonClientModEvents;
import com.leon.saintsdragons.fabric.client.FabricDragonRideKeybinds;
import com.leon.saintsdragons.fabric.client.FabricDragonUI;
import com.leon.saintsdragons.fabric.client.event.FabricClientEventHandler;
import com.leon.saintsdragons.fabric.client.particle.FabricParticleRegistry;
import com.leon.saintsdragons.fabric.client.renderer.FabricDragonPartRenderer;
import com.leon.saintsdragons.fabric.entity.part.FabricPartEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class SaintsDragonsFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CommonClientModEvents.registerEntityRenderers(EntityRendererRegistry::register);
        CommonClientModEvents.registerMenuScreens();
        EntityRendererRegistry.register(FabricPartEntities.DRAGON_PART, FabricDragonPartRenderer::new);
        FabricParticleRegistry.registerParticleFactories();
        FabricDragonRideKeybinds.init();
        FabricDragonUI.init();
        FabricClientEventHandler.init();
    }
}
