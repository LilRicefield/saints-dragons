package com.leon.saintsdragons.forge.client.event;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.client.particle.raevyx.RaevyxLightningParticle;
import com.leon.saintsdragons.client.particle.raevyx.RaevyxLightningChainParticle;
import com.leon.saintsdragons.common.registry.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SaintsDragonsCommon.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ParticleClientRegistry {
    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.LIGHTNING_STORM.get(), RaevyxLightningParticle.Factory::new);
        event.registerSpriteSet(ModParticles.LIGHTNING_CHAIN.get(), RaevyxLightningChainParticle.Factory::new);
    }
}
