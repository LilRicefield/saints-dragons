package com.leon.saintsdragons.client.init;

import com.leon.saintsdragons.client.renderer.cindervane.CindervaneMagmaBlockRenderer;
import com.leon.saintsdragons.client.renderer.vfx.DragonWaterSplashRenderer;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.client.renderer.cindervane.CindervaneRenderer;
import com.leon.saintsdragons.client.renderer.ignivorus.IgnivorusFlameRenderer;
import com.leon.saintsdragons.client.renderer.ignivorus.IgnivorusRenderer;
import com.leon.saintsdragons.client.renderer.ignivorus.IgnivorusMagmaBlockRenderer;
import com.leon.saintsdragons.client.renderer.ignivorus.IgnivorusMagmaPillarRenderer;
import com.leon.saintsdragons.client.renderer.vfx.IgnivorusNovaRenderer;
import com.leon.saintsdragons.client.renderer.vfx.IgnivorusNovaRingRenderer;
import com.leon.saintsdragons.client.renderer.nulljaw.NulljawRenderer;
import com.leon.saintsdragons.client.renderer.varasuchus.VarasuchusRenderer;
import com.leon.saintsdragons.client.renderer.raevyx.RaevyxGroundRendTrailRenderer;
import com.leon.saintsdragons.client.renderer.raevyx.RaevyxLightningChainRenderer;
import com.leon.saintsdragons.client.renderer.raevyx.RaevyxRenderer;
import com.leon.saintsdragons.client.renderer.stegonaut.StegonautAmethystPillarRenderer;
import com.leon.saintsdragons.client.renderer.stegonaut.StegonautGroundCrackRenderer;
import com.leon.saintsdragons.client.renderer.stegonaut.StegonautGroundChunkRenderer;
import com.leon.saintsdragons.client.renderer.vfx.ImpactRingRenderer;
import com.leon.saintsdragons.client.renderer.stegonaut.StegonautRenderer;
import com.leon.saintsdragons.client.renderer.volitans.VolitansRenderer;
import com.leon.saintsdragons.client.renderer.volitans.ArrowOfVenomRenderer;
import com.leon.saintsdragons.client.renderer.volitans.VolitansBurrowMoundRenderer;
import com.leon.saintsdragons.client.renderer.volitans.VolitansGroundChunkRenderer;
import com.leon.saintsdragons.client.renderer.volitans.VolitansPoisonBallRenderer;
import com.leon.saintsdragons.client.renderer.volitans.VolitansSpineRenderer;
import com.leon.saintsdragons.client.renderer.volitans.VolitansWaterBreathRenderer;
import com.leon.saintsdragons.client.renderer.vfx.VisualFallingBlockRenderer;
import com.leon.saintsdragons.client.renderer.npc.IvyTheDragonMerchantRenderer;
import com.leon.saintsdragons.client.renderer.mossback.MossbackRenderer;
import com.leon.saintsdragons.client.renderer.otheranimals.MoopRenderer;
import com.leon.saintsdragons.client.ui.IvyInventoryScreen;
import com.leon.saintsdragons.client.ui.StegonautInventoryScreen;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import com.leon.saintsdragons.common.registry.ModMenus;

public final class CommonClientModEvents {
    private CommonClientModEvents() {
    }

    public static void registerEntityRenderers(RendererRegistrar registrar) {
        registrar.register(ModEntities.RAEVYX.get(), RaevyxRenderer::new);
        registrar.register(ModEntities.RAEVYX_LIGHTNING_CHAIN.get(), RaevyxLightningChainRenderer::new);
        registrar.register(ModEntities.RAEVYX_GROUND_REND_TRAIL.get(), RaevyxGroundRendTrailRenderer::new);
        registrar.register(ModEntities.STEGONAUT.get(), StegonautRenderer::new);
        registrar.register(ModEntities.CINDERVANE.get(), CindervaneRenderer::new);
        registrar.register(ModEntities.VARASUCHUS.get(), VarasuchusRenderer::new);
        registrar.register(ModEntities.IGNIVORUS.get(), IgnivorusRenderer::new);
        registrar.register(ModEntities.VOLITANS.get(), VolitansRenderer::new);
        registrar.register(ModEntities.NULLJAW.get(), NulljawRenderer::new);
        registrar.register(ModEntities.MOOP.get(), MoopRenderer::new);
        registrar.register(ModEntities.MOSSBACK.get(), MossbackRenderer::new);
        registrar.register(ModEntities.CINDERVANE_MAGMA_BLOCK.get(), CindervaneMagmaBlockRenderer::new);
        registrar.register(ModEntities.IGNIVORUS_MAGMA_BLOCK.get(), IgnivorusMagmaBlockRenderer::new);
        registrar.register(ModEntities.IGNIVORUS_MAGMA_PILLAR.get(), IgnivorusMagmaPillarRenderer::new);
        registrar.register(ModEntities.IGNIVORUS_FLAME.get(), IgnivorusFlameRenderer::new);
        registrar.register(ModEntities.IGNIVORUS_NOVA.get(), IgnivorusNovaRenderer::new);
        registrar.register(ModEntities.IGNIVORUS_NOVA_RING.get(), IgnivorusNovaRingRenderer::new);
        registrar.register(ModEntities.STEGONAUT_GROUND_CHUNK.get(), StegonautGroundChunkRenderer::new);
        registrar.register(ModEntities.STEGONAUT_AMETHYST_PILLAR.get(), StegonautAmethystPillarRenderer::new);
        registrar.register(ModEntities.STEGONAUT_IMPACT_RING.get(), ImpactRingRenderer::new);
        registrar.register(ModEntities.STEGONAUT_GROUND_CRACK.get(), StegonautGroundCrackRenderer::new);
        registrar.register(ModEntities.DRAGON_WATER_SPLASH.get(), DragonWaterSplashRenderer::new);
        registrar.register(ModEntities.VOLITANS_GROUND_CHUNK.get(), VolitansGroundChunkRenderer::new);
        registrar.register(ModEntities.VOLITANS_BURROW_MOUND.get(), VolitansBurrowMoundRenderer::new);
        registrar.register(ModEntities.VISUAL_FALLING_BLOCK.get(), VisualFallingBlockRenderer::new);
        registrar.register(ModEntities.VOLITANS_SPINE.get(), VolitansSpineRenderer::new);
        registrar.register(ModEntities.ARROW_OF_VENOM.get(), ArrowOfVenomRenderer::new);
        registrar.register(ModEntities.VOLITANS_WATER_BREATH.get(), VolitansWaterBreathRenderer::new);
        registrar.register(ModEntities.VOLITANS_POISON_BALL.get(), VolitansPoisonBallRenderer::new);
        registrar.register(ModEntities.IVY_THE_DRAGON_MERCHANT.get(), IvyTheDragonMerchantRenderer::new);
    }

    public static void registerMenuScreens() {
        MenuScreens.register(ModMenus.STEGONAUT_INVENTORY.get(), StegonautInventoryScreen::new);
        MenuScreens.register(ModMenus.IVY_INVENTORY.get(), IvyInventoryScreen::new);
    }

    @FunctionalInterface
    public interface RendererRegistrar {
        <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<? super T> provider);
    }

}
