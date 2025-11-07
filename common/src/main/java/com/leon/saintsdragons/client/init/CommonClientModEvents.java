package com.leon.saintsdragons.client.init;

import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.client.renderer.cindervane.CindervaneMagmaBlockRenderer;
import com.leon.saintsdragons.client.renderer.cindervane.CindervaneRenderer;
import com.leon.saintsdragons.client.renderer.ignivorus.IgnivorusRenderer;
import com.leon.saintsdragons.client.renderer.nulljaw.NulljawRenderer;
import com.leon.saintsdragons.client.renderer.raevyx.RaevyxLightningChainRenderer;
import com.leon.saintsdragons.client.renderer.raevyx.RaevyxRenderer;
import com.leon.saintsdragons.client.renderer.stegonaut.StegonautRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * Client-only renderer wiring shared across loaders.
 */
public final class CommonClientModEvents {
    private CommonClientModEvents() {
    }

    public static void registerEntityRenderers(RendererRegistrar registrar) {
        registrar.register(ModEntities.RAEVYX.get(), RaevyxRenderer::new);
        registrar.register(ModEntities.RAEVYX_LIGHTNING_CHAIN.get(), RaevyxLightningChainRenderer::new);
        registrar.register(ModEntities.STEGONAUT.get(), StegonautRenderer::new);
        registrar.register(ModEntities.CINDERVANE.get(), CindervaneRenderer::new);
        registrar.register(ModEntities.NULLJAW.get(), NulljawRenderer::new);
        registrar.register(ModEntities.IGNIVORUS.get(), IgnivorusRenderer::new);
        registrar.register(ModEntities.CINDERVANE_MAGMA_BLOCK.get(), CindervaneMagmaBlockRenderer::new);
    }

    @FunctionalInterface
    public interface RendererRegistrar {
        <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<? super T> provider);
    }
}
