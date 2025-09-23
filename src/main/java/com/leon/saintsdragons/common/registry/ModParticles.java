package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.common.particle.lightningdragon.LightningStormData;
import com.leon.saintsdragons.common.particle.lightningdragon.LightningArcData;
import com.leon.saintsdragons.common.particle.lightningdragon.LightningChainData;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> REGISTER =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, SaintsDragons.MOD_ID);

    public static final RegistryObject<ParticleType<LightningStormData>> LIGHTNING_STORM =
            REGISTER.register("lightning_storm",
                    () -> new ParticleType<LightningStormData>(false, LightningStormData.DESERIALIZER) {
                        @Override
                        public com.mojang.serialization.Codec<LightningStormData> codec() {
                            return LightningStormData.CODEC(this);
                        }
                    });

    public static final RegistryObject<ParticleType<LightningArcData>> LIGHTNING_ARC =
            REGISTER.register("lightning_arc",
                    () -> new ParticleType<LightningArcData>(false, LightningArcData.DESERIALIZER) {
                        @Override
                        public com.mojang.serialization.Codec<LightningArcData> codec() {
                            return LightningArcData.CODEC(this);
                        }
                    });

    public static final RegistryObject<ParticleType<LightningChainData>> LIGHTNING_CHAIN =
            REGISTER.register("lightning_chain",
                    () -> new ParticleType<LightningChainData>(false, LightningChainData.DESERIALIZER) {
                        @Override
                        public com.mojang.serialization.Codec<LightningChainData> codec() {
                            return LightningChainData.CODEC(this);
                        }
                    });

    // Fire breath particles for Amphithere
    public static final RegistryObject<SimpleParticleType> FIRE_BREATH_FLAME =
            REGISTER.register("fire_breath_flame", () -> new SimpleParticleType(false));
    
    public static final RegistryObject<SimpleParticleType> FIRE_BREATH_SMOKE =
            REGISTER.register("fire_breath_smoke", () -> new SimpleParticleType(false));
}