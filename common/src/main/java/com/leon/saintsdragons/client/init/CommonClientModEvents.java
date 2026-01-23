package com.leon.saintsdragons.client.init;

import com.leon.saintsdragons.client.renderer.cindervane.CindervaneMagmaBlockRenderer;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.client.renderer.cindervane.CindervaneRenderer;
import com.leon.saintsdragons.client.renderer.ignivorus.IgnivorusFlameRenderer;
import com.leon.saintsdragons.client.renderer.ignivorus.IgnivorusRenderer;
import com.leon.saintsdragons.client.renderer.ignivorus.IgnivorusMagmaBlockRenderer;
import com.leon.saintsdragons.client.renderer.ignivorus.IgnivorusMagmaPillarRenderer;
import com.leon.saintsdragons.client.renderer.ignivorus.IgnivorusNovaRenderer;
import com.leon.saintsdragons.client.renderer.ignivorus.IgnivorusNovaRingRenderer;
import com.leon.saintsdragons.client.renderer.nulljaw.NulljawRenderer;
import com.leon.saintsdragons.client.renderer.raevyx.RaevyxLightningChainRenderer;
import com.leon.saintsdragons.client.renderer.raevyx.RaevyxRenderer;
import com.leon.saintsdragons.client.renderer.stegonaut.StegonautRenderer;
import com.leon.saintsdragons.client.renderer.VisualFallingBlockRenderer;
import com.leon.saintsdragons.client.renderer.npc.IvyTheDragonMerchantRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

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
        registrar.register(ModEntities.IGNIVORUS_MAGMA_BLOCK.get(), IgnivorusMagmaBlockRenderer::new);
        registrar.register(ModEntities.IGNIVORUS_MAGMA_PILLAR.get(), IgnivorusMagmaPillarRenderer::new);
        registrar.register(ModEntities.IGNIVORUS_FLAME.get(), IgnivorusFlameRenderer::new);
        registrar.register(ModEntities.IGNIVORUS_NOVA.get(), IgnivorusNovaRenderer::new);
        registrar.register(ModEntities.IGNIVORUS_NOVA_RING.get(), IgnivorusNovaRingRenderer::new);
        registrar.register(ModEntities.VISUAL_FALLING_BLOCK.get(), VisualFallingBlockRenderer::new);
        registrar.register(ModEntities.IVY_THE_DRAGON_MERCHANT.get(), IvyTheDragonMerchantRenderer::new);
    }

    @FunctionalInterface
    public interface RendererRegistrar {
        <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<? super T> provider);
    }
}
