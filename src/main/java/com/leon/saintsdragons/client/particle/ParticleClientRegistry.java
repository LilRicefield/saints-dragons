package com.leon.saintsdragons.client.particle;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.client.particle.lightningdragon.LightningParticle;
import com.leon.saintsdragons.client.particle.lightningdragon.LightningArcParticle;
import com.leon.saintsdragons.client.particle.lightningdragon.LightningChainParticle;
import com.leon.saintsdragons.common.registry.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SaintsDragons.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ParticleClientRegistry {
    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.LIGHTNING_STORM.get(), LightningParticle.Factory::new);
        event.registerSpriteSet(ModParticles.LIGHTNING_ARC.get(), LightningArcParticle.Factory::new);
        event.registerSpriteSet(ModParticles.LIGHTNING_CHAIN.get(), LightningChainParticle.Factory::new);
    }
}