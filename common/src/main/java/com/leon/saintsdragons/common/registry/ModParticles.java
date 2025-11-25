package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.particle.raevyx.RaevyxLightningArcData;
import com.leon.saintsdragons.common.particle.raevyx.RaevyxLightningChainData;
import com.leon.saintsdragons.common.particle.raevyx.RaevyxLightningStormData;
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
                    () -> new ParticleType<>(false) {
                        @Override
                        public com.mojang.serialization.MapCodec<RaevyxLightningStormData> codec() {
                            return RaevyxLightningStormData.MAP_CODEC;
                        }

                        @Override
                        public net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, RaevyxLightningStormData> streamCodec() {
                            return RaevyxLightningStormData.STREAM_CODEC;
                        }
                    });

    public static final Supplier<ParticleType<RaevyxLightningStormData>> LIGHTNING_STORM_FEMALE =
            REGISTER.register("lightning_storm_female",
                    () -> new ParticleType<>(false) {
                        @Override
                        public com.mojang.serialization.MapCodec<RaevyxLightningStormData> codec() {
                            return RaevyxLightningStormData.MAP_CODEC;
                        }

                        @Override
                        public net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, RaevyxLightningStormData> streamCodec() {
                            return RaevyxLightningStormData.STREAM_CODEC;
                        }
                    });

    public static final Supplier<ParticleType<RaevyxLightningArcData>> LIGHTNING_ARC =
            REGISTER.register("lightning_arc",
                    () -> new ParticleType<>(false) {
                        @Override
                        public com.mojang.serialization.MapCodec<RaevyxLightningArcData> codec() {
                            return RaevyxLightningArcData.MAP_CODEC;
                        }

                        @Override
                        public net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, RaevyxLightningArcData> streamCodec() {
                            return RaevyxLightningArcData.STREAM_CODEC;
                        }
                    });

    public static final Supplier<ParticleType<RaevyxLightningChainData>> LIGHTNING_CHAIN =
            REGISTER.register("lightning_chain",
                    () -> new ParticleType<>(false) {
                        @Override
                        public com.mojang.serialization.MapCodec<RaevyxLightningChainData> codec() {
                            return RaevyxLightningChainData.MAP_CODEC;
                        }

                        @Override
                        public net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, RaevyxLightningChainData> streamCodec() {
                            return RaevyxLightningChainData.STREAM_CODEC;
                        }
                    });

    public static final Supplier<SimpleParticleType> FIRE_BREATH_FLAME =
            REGISTER.register("fire_breath_flame", () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> FIRE_BREATH_SMOKE =
            REGISTER.register("fire_breath_smoke", () -> Services.PLATFORM.createSimpleParticle(false));

    private ModParticles() {
    }

    public static void register() {
        REGISTER.register();
    }
}
