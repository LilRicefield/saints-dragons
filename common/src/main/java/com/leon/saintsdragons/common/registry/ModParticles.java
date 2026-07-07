package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.particle.raevyx.RaevyxLightningChainData;
import com.leon.saintsdragons.common.particle.raevyx.RaevyxLightningStormData;
import com.leon.saintsdragons.common.particle.raevyx.SonicRingData;
import com.leon.saintsdragons.platform.RegistryHelper;
import com.leon.saintsdragons.platform.Services;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

import java.util.function.Supplier;

public final class ModParticles {
    private static final RegistryHelper.RegistryWrapper<ParticleType<?>> REGISTER =
            Services.PLATFORM.getRegistryHelper()
                    .create(Registries.PARTICLE_TYPE, () -> BuiltInRegistries.PARTICLE_TYPE, SaintsDragonsCommon.MOD_ID);

    public static final Supplier<ParticleType<RaevyxLightningStormData>> LIGHTNING_STORM =
            REGISTER.register("lightning_storm",
                    () -> new ParticleType<>(false, RaevyxLightningStormData.DESERIALIZER) {
                        @Override
                        public com.mojang.serialization.Codec<RaevyxLightningStormData> codec() {
                            return RaevyxLightningStormData.CODEC(this);
                        }
                    });

    public static final Supplier<ParticleType<RaevyxLightningStormData>> LIGHTNING_STORM_NIGHT_GOLD =
            REGISTER.register("lightning_storm_night_gold",
                    () -> new ParticleType<>(false, RaevyxLightningStormData.DESERIALIZER) {
                        @Override
                        public com.mojang.serialization.Codec<RaevyxLightningStormData> codec() {
                            return RaevyxLightningStormData.CODEC(this);
                        }
                    });

    public static final Supplier<ParticleType<RaevyxLightningChainData>> LIGHTNING_CHAIN =
            REGISTER.register("lightning_chain",
                    () -> new ParticleType<>(false, RaevyxLightningChainData.DESERIALIZER) {
                        @Override
                        public com.mojang.serialization.Codec<RaevyxLightningChainData> codec() {
                            return RaevyxLightningChainData.CODEC(this);
                        }
                    });

    public static final Supplier<ParticleType<SonicRingData>> RAEVYX_SONIC_RING =
            REGISTER.register("raevyx_sonic_ring",
                    () -> new ParticleType<>(false, SonicRingData.DESERIALIZER) {
                        @Override
                        public com.mojang.serialization.Codec<SonicRingData> codec() {
                            return SonicRingData.CODEC(this);
                        }
                    });

    public static final Supplier<SimpleParticleType> FIRE_BREATH_FLAME =
            REGISTER.register("fire_breath_flame", () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> FIRE_BREATH_SMOKE =
            REGISTER.register("fire_breath_smoke", () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> DRAGON_DUST =
            REGISTER.register("dragon_dust", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> MOSSBACK_POISON_FUME =
            REGISTER.register("mossback_poison_fume", () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> DRACONIAN_NUCLEUS_PARTICLE =
            REGISTER.register("draconian_nucleus_particle", () -> Services.PLATFORM.createSimpleParticle(true));

    private ModParticles() {
    }

    public static void register() {
        REGISTER.register();
    }
}
