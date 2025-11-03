package com.leon.saintsdragons.fabric;

import com.leon.saintsdragons.client.ClientProxy;
import com.leon.saintsdragons.client.init.CommonClientModEvents;
import com.leon.saintsdragons.fabric.client.FabricDragonRideKeybinds;
import com.leon.saintsdragons.fabric.client.FabricDragonUI;
import com.leon.saintsdragons.fabric.client.event.FabricClientEventHandler;
import com.leon.saintsdragons.fabric.client.particle.FabricParticleRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class SaintsDragonsFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CommonClientModEvents.registerEntityRenderers(EntityRendererRegistry::register);
        FabricParticleRegistry.registerParticleFactories();
        FabricDragonRideKeybinds.init();
        FabricDragonUI.init();
        FabricClientEventHandler.init();
        new ClientProxy().clientInit();
    }
}
