package com.leon.saintsdragons.fabric.resource;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.block.crucible.DraconicCrucibleThermalReloadListener;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class FabricDraconicCrucibleThermalReloadListener implements IdentifiableResourceReloadListener {
    private static final ResourceLocation ID = SaintsDragonsCommon.rl("draconic_crucible_thermal");
    private final DraconicCrucibleThermalReloadListener delegate =
            DraconicCrucibleThermalReloadListener.getInstance();

    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }

    @Override
    public @NotNull CompletableFuture<Void> reload(PreparableReloadListener.@NotNull PreparationBarrier barrier,
                                                   @NotNull ResourceManager manager,
                                                   @NotNull ProfilerFiller prepareProfiler,
                                                   @NotNull ProfilerFiller applyProfiler,
                                                   @NotNull Executor prepareExecutor,
                                                   @NotNull Executor applyExecutor) {
        return this.delegate.reload(
                barrier, manager, prepareProfiler, applyProfiler, prepareExecutor, applyExecutor);
    }
}
