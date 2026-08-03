package com.leon.saintsdragons.fabric;

import com.leon.saintsdragons.client.init.CommonClientModEvents;
import com.leon.saintsdragons.common.registry.ModBlocks;
import com.leon.saintsdragons.common.registry.ModBlockEntities;
import com.leon.saintsdragons.client.model.block.DraconianNucleusModel;
import com.leon.saintsdragons.client.renderer.block.DraconianNucleusRenderer;
import com.leon.saintsdragons.client.model.block.DraconicCrucibleEntity;
import com.leon.saintsdragons.client.renderer.block.DraconicCrucibleRenderer;
import com.leon.saintsdragons.fabric.client.FabricDragonRideKeybinds;
import com.leon.saintsdragons.fabric.client.FabricDragonUI;
import com.leon.saintsdragons.fabric.client.event.FabricClientEventHandler;
import com.leon.saintsdragons.fabric.client.particle.FabricParticleRegistry;
import com.leon.saintsdragons.fabric.client.renderer.FabricDraconianArmorRenderer;
import com.leon.saintsdragons.fabric.client.renderer.FabricDragonPartRenderer;
import com.leon.saintsdragons.fabric.entity.part.FabricPartEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.renderer.RenderType;

public final class SaintsDragonsFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CommonClientModEvents.registerEntityRenderers(EntityRendererRegistry::register);
        CommonClientModEvents.registerMenuScreens();
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DRACONIAN_NUCLEUS.get(), RenderType.translucent());
        EntityModelLayerRegistry.registerModelLayer(
                DraconianNucleusModel.LAYER_LOCATION,
                DraconianNucleusModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(
                DraconicCrucibleEntity.LAYER_LOCATION,
                DraconicCrucibleEntity::createBodyLayer);
        BlockEntityRendererRegistry.register(
                ModBlockEntities.DRACONIAN_NUCLEUS.get(),
                DraconianNucleusRenderer::new);
        BlockEntityRendererRegistry.register(
                ModBlockEntities.DRACONIC_CRUCIBLE.get(),
                DraconicCrucibleRenderer::new);
        EntityRendererRegistry.register(FabricPartEntities.DRAGON_PART, FabricDragonPartRenderer::new);
        FabricDraconianArmorRenderer.register();
        FabricParticleRegistry.registerParticleFactories();
        FabricDragonRideKeybinds.init();
        FabricDragonUI.init();
        FabricClientEventHandler.init();
    }
}
