package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.common.particle.raevyx.RaevyxLightningStormData;
import com.leon.saintsdragons.common.particle.raevyx.RaevyxLightningArcData;
import com.leon.saintsdragons.common.particle.raevyx.RaevyxLightningChainData;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> REGISTER =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, SaintsDragons.MOD_ID);

    public static final RegistryObject<ParticleType<RaevyxLightningStormData>> LIGHTNING_STORM =
            REGISTER.register("lightning_storm",
                    () -> new ParticleType<RaevyxLightningStormData>(false, RaevyxLightningStormData.DESERIALIZER) {
                        @Override
                        public com.mojang.serialization.Codec<RaevyxLightningStormData> codec() {
                            return RaevyxLightningStormData.CODEC(this);
                        }
                    });

    public static final RegistryObject<ParticleType<RaevyxLightningArcData>> LIGHTNING_ARC =
            REGISTER.register("lightning_arc",
                    () -> new ParticleType<RaevyxLightningArcData>(false, RaevyxLightningArcData.DESERIALIZER) {
                        @Override
                        public com.mojang.serialization.Codec<RaevyxLightningArcData> codec() {
                            return RaevyxLightningArcData.CODEC(this);
                        }
                    });

    public static final RegistryObject<ParticleType<RaevyxLightningChainData>> LIGHTNING_CHAIN =
            REGISTER.register("lightning_chain",
                    () -> new ParticleType<RaevyxLightningChainData>(false, RaevyxLightningChainData.DESERIALIZER) {
                        @Override
                        public com.mojang.serialization.Codec<RaevyxLightningChainData> codec() {
                            return RaevyxLightningChainData.CODEC(this);
                        }
                    });

    // Fire breath particles for Amphithere
    public static final RegistryObject<SimpleParticleType> FIRE_BREATH_FLAME =
            REGISTER.register("fire_breath_flame", () -> new SimpleParticleType(false));
    
    public static final RegistryObject<SimpleParticleType> FIRE_BREATH_SMOKE =
            REGISTER.register("fire_breath_smoke", () -> new SimpleParticleType(false));
}