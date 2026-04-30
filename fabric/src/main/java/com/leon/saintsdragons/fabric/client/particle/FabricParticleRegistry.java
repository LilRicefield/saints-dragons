package com.leon.saintsdragons.fabric.client.particle;


import com.leon.saintsdragons.client.particle.raevyx.RaevyxLightningChainParticle;
import com.leon.saintsdragons.client.particle.raevyx.RaevyxLightningParticle;
import com.leon.saintsdragons.common.registry.ModParticles;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;

public class FabricParticleRegistry {
    public static void registerParticleFactories() {
        ParticleFactoryRegistry.getInstance().register(ModParticles.LIGHTNING_STORM.get(), RaevyxLightningParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.LIGHTNING_STORM_NIGHT_GOLD.get(), RaevyxLightningParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.LIGHTNING_CHAIN.get(), RaevyxLightningChainParticle.Factory::new);

    }
}
