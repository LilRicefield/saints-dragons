package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.particle.BloodTempestKatanaRingData;
import com.leon.saintsdragons.common.particle.GroundDecalParticleData;
import com.leon.saintsdragons.common.particle.raevyx.RaevyxLightningChainData;
import com.leon.saintsdragons.common.particle.raevyx.RaevyxLightningStormData;
import com.leon.saintsdragons.common.particle.SonicRingData;
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

    public static final Supplier<ParticleType<BloodTempestKatanaRingData>> BLOOD_TEMPEST_SWORD_RING =
            REGISTER.register("blood_tempest_sword_ring",
                    () -> new ParticleType<>(false, BloodTempestKatanaRingData.DESERIALIZER) {
                        @Override
                        public com.mojang.serialization.Codec<BloodTempestKatanaRingData> codec() {
                            return BloodTempestKatanaRingData.codec(this);
                        }
                    });

    public static final Supplier<SimpleParticleType> FIRE_BREATH_FLAME =
            REGISTER.register("fire_breath_flame", () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> FIRE_BREATH_SMOKE =
            REGISTER.register("fire_breath_smoke", () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> DRAGON_DUST =
            REGISTER.register("dragon_dust", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<ParticleType<GroundDecalParticleData>> GROUND_CRACK =
            REGISTER.register("ground_crack",
                    () -> new ParticleType<>(false, GroundDecalParticleData.DESERIALIZER) {
                        @Override
                        public com.mojang.serialization.Codec<GroundDecalParticleData> codec() {
                            return GroundDecalParticleData.codec(this);
                        }
                    });

    public static final Supplier<ParticleType<GroundDecalParticleData>> GROUND_CRACK_FISSURE =
            REGISTER.register("ground_crack_fissure",
                    () -> new ParticleType<>(false, GroundDecalParticleData.DESERIALIZER) {
                        @Override
                        public com.mojang.serialization.Codec<GroundDecalParticleData> codec() {
                            return GroundDecalParticleData.codec(this);
                        }
                    });

    public static final Supplier<SimpleParticleType> BLOOD_TEMPEST_KATANA_X_MARK =
            REGISTER.register("blood_tempest_katana_x_mark", () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> BLOOD_TEMPEST_KATANA_GLINT =
            REGISTER.register("blood_tempest_katana_glint", () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> BLOOD_TEMPEST_KATANA_LIGHTNING_STRIKE =
            REGISTER.register("blood_tempest_katana_lightning_strike",
                    () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> SECOND_LIGHTNING_STRIKE =
            REGISTER.register("second_lightning_strike",
                    () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> GLOWING_EMITTER =
            REGISTER.register("glowing_emitter", () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> RED_GLOW =
            REGISTER.register("red_glow", () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> RAINBOW_FLARE =
            REGISTER.register("rainbow_flare", () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> BLOOD_TEMPEST_KATANA_FIRST_IMPACT =
            REGISTER.register("blood_tempest_katana_first_impact",
                    () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> BLOOD_TEMPEST_KATANA_SECOND_IMPACT =
            REGISTER.register("blood_tempest_katana_second_impact",
                    () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> SECOND_IMPACT_RING =
            REGISTER.register("second_impact_ring", () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> MOSSBACK_POISON_FUME =
            REGISTER.register("mossback_poison_fume", () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> DRACONIAN_NUCLEUS_PARTICLE =
            REGISTER.register("draconian_nucleus_particle", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> ATROXIIA_SNOW =
            REGISTER.register("atroxiia_snow", () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> ATROXIIA_SNOW_SHARD =
            REGISTER.register("atroxiia_snow_shard", () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> ATROXIIA_SNOW_SPARK =
            REGISTER.register("atroxiia_snow_spark", () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> ATROXIIA_SNOW_DUST =
            REGISTER.register("atroxiia_snow_dust", () -> Services.PLATFORM.createSimpleParticle(false));

    private ModParticles() {
    }

    public static void register() {
        REGISTER.register();
    }
}
