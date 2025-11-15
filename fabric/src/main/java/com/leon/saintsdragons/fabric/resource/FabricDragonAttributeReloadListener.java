package com.leon.saintsdragons.fabric.resource;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Fabric wrapper that adapts the common dragon attribute loader to the identifiable listener API.
 */
public final class FabricDragonAttributeReloadListener implements IdentifiableResourceReloadListener {
    private static final ResourceLocation ID = SaintsDragonsCommon.rl("dragon_attribute_loader");
    private final DragonAttributeConfigLoader delegate = DragonAttributeConfigLoader.getInstance();

    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }

    @Override
    public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier barrier,
                                          ResourceManager manager,
                                          ProfilerFiller prepareProfiler,
                                          ProfilerFiller applyProfiler,
                                          Executor prepareExecutor,
                                          Executor applyExecutor) {
        return delegate.reload(barrier, manager, prepareProfiler, applyProfiler, prepareExecutor, applyExecutor);
    }
}
