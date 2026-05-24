package com.leon.saintsdragons.fabric.client.particle;


import com.leon.saintsdragons.client.init.CommonParticleFactories;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

public class FabricParticleRegistry {
    public static void registerParticleFactories() {
        CommonParticleFactories.register(new CommonParticleFactories.Registrar() {
            @Override
            public <T extends ParticleOptions> void register(ParticleType<T> type,
                                                            CommonParticleFactories.SpriteFactory<T> factory) {
                ParticleFactoryRegistry.getInstance().register(type, factory::create);
            }
        });
    }
}
