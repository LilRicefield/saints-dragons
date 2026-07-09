package com.leon.saintsdragons.client.init;

import com.leon.saintsdragons.client.particle.DustParticle;
import com.leon.saintsdragons.client.particle.DraconianNucleusParticle;
import com.leon.saintsdragons.client.particle.MossbackPoisonFumeParticle;
import com.leon.saintsdragons.client.particle.RaevyxLightningChainParticle;
import com.leon.saintsdragons.client.particle.RaevyxLightningParticle;
import com.leon.saintsdragons.client.particle.SonicRingParticle;
import com.leon.saintsdragons.common.registry.ModParticles;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

public final class CommonParticleFactories {
    private CommonParticleFactories() {
    }

    public static void register(Registrar registrar) {
        register(registrar, ModParticles.LIGHTNING_STORM.get(), RaevyxLightningParticle.Factory::new);
        register(registrar, ModParticles.LIGHTNING_STORM_NIGHT_GOLD.get(), RaevyxLightningParticle.Factory::new);
        register(registrar, ModParticles.LIGHTNING_CHAIN.get(), RaevyxLightningChainParticle.Factory::new);
        register(registrar, ModParticles.RAEVYX_SONIC_RING.get(), SonicRingParticle.Factory::new);
        register(registrar, ModParticles.DRAGON_DUST.get(), DustParticle.Factory::new);
        register(registrar, ModParticles.MOSSBACK_POISON_FUME.get(), MossbackPoisonFumeParticle.Factory::new);
        register(registrar, ModParticles.DRACONIAN_NUCLEUS_PARTICLE.get(), DraconianNucleusParticle.Factory::new);
    }

    private static <T extends ParticleOptions> void register(Registrar registrar,
                                                            ParticleType<T> type,
                                                            SpriteFactory<T> factory) {
        registrar.register(type, factory);
    }

    @FunctionalInterface
    public interface Registrar {
        <T extends ParticleOptions> void register(ParticleType<T> type, SpriteFactory<T> factory);
    }

    @FunctionalInterface
    public interface SpriteFactory<T extends ParticleOptions> {
        ParticleProvider<T> create(SpriteSet sprites);
    }
}
