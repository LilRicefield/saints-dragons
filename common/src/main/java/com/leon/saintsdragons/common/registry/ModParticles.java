package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.particle.BloodTempestKatanaRingData;
import com.leon.saintsdragons.common.particle.GroundDecalParticleData;
import com.leon.saintsdragons.common.particle.FireBreathParticleData;
import com.leon.saintsdragons.common.particle.FireBreathBurstData;
import com.leon.saintsdragons.common.particle.VolitansBreathParticleData;
import com.leon.saintsdragons.common.particle.raevyx.RaevyxLightningChainData;
import com.leon.saintsdragons.common.particle.raevyx.RaevyxLightningStormData;
import com.leon.saintsdragons.common.particle.SonicRingData;
import com.leon.saintsdragons.platform.RegistryHelper;
import com.leon.saintsdragons.platform.Services;
import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

import java.util.function.Supplier;

public final class ModParticles {
    private static final RegistryHelper.RegistryWrapper<ParticleType<?>> REGISTER =
            Services.PLATFORM.getRegistryHelper()
                    .create(Registries.PARTICLE_TYPE, () -> BuiltInRegistries.PARTICLE_TYPE, SaintsDragonsCommon.MOD_ID);

    public static final Supplier<SimpleParticleType> VOLITANS_POISON_ORB_EMITTER =
            REGISTER.register("volitans_poison_orb_emitter", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> VOLITANS_POISON_ORB_TRAIL =
            REGISTER.register("volitans_poison_orb_trail", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> ATROXIIA_QUAKE_SMOKE =
            REGISTER.register("atroxiia_quake_smoke", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> ATROXIIA_ICE_BURST =
            REGISTER.register("atroxiia_ice_burst", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<ParticleType<VolitansBreathParticleData>> VOLITANS_BREATH_STREAM =
            REGISTER.register("volitans_breath_stream", () -> new ParticleType<>(false, VolitansBreathParticleData.DESERIALIZER) {
                @Override
                public Codec<VolitansBreathParticleData> codec() {
                    return VolitansBreathParticleData.CODEC;
                }
            });

    public static final Supplier<SimpleParticleType> VOLITANS_WATER_BREATH =
            REGISTER.register("volitans_water_breath", () -> Services.PLATFORM.createSimpleParticle(false));
    public static final Supplier<SimpleParticleType> VOLITANS_POISON_BREATH =
            REGISTER.register("volitans_poison_breath", () -> Services.PLATFORM.createSimpleParticle(false));
    public static final Supplier<SimpleParticleType> VOLITANS_POISON_SKULL =
            REGISTER.register("volitans_poison_skull", () -> Services.PLATFORM.createSimpleParticle(false));
    public static final Supplier<SimpleParticleType> VOLITANS_POISON_FLAME =
            REGISTER.register("volitans_poison_flame", () -> Services.PLATFORM.createSimpleParticle(false));
    public static final Supplier<SimpleParticleType> VOLITANS_POISON_EXPLOSION =
            REGISTER.register("volitans_poison_explosion", () -> Services.PLATFORM.createSimpleParticle(false));
    public static final Supplier<SimpleParticleType> VOLITANS_POISON_GROUND_BURST =
            REGISTER.register("volitans_poison_ground_burst", () -> Services.PLATFORM.createSimpleParticle(false));
    public static final Supplier<SimpleParticleType> VOLITANS_POISON_CLOUD =
            REGISTER.register("volitans_poison_cloud", () -> Services.PLATFORM.createSimpleParticle(false));
    public static final Supplier<SimpleParticleType> VOLITANS_BREATH_BUBBLES =
            REGISTER.register("volitans_breath_bubbles", () -> Services.PLATFORM.createSimpleParticle(false));
    public static final Supplier<SimpleParticleType> VOLITANS_BREATH_EMITTER =
            REGISTER.register("volitans_breath_emitter", () -> Services.PLATFORM.createSimpleParticle(false));
    public static final Supplier<SimpleParticleType> VOLITANS_BREATH_STAR =
            REGISTER.register("volitans_breath_star", () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<ParticleType<RaevyxLightningStormData>> LIGHTNING_STORM =
            REGISTER.register("lightning_storm",
                    () -> new ParticleType<>(false, RaevyxLightningStormData.DESERIALIZER) {
                        @Override
                        public Codec<RaevyxLightningStormData> codec() {
                            return RaevyxLightningStormData.CODEC(this);
                        }
                    });

    public static final Supplier<ParticleType<RaevyxLightningStormData>> LIGHTNING_STORM_NIGHT_GOLD =
            REGISTER.register("lightning_storm_night_gold",
                    () -> new ParticleType<>(false, RaevyxLightningStormData.DESERIALIZER) {
                        @Override
                        public Codec<RaevyxLightningStormData> codec() {
                            return RaevyxLightningStormData.CODEC(this);
                        }
                    });

    public static final Supplier<ParticleType<RaevyxLightningChainData>> LIGHTNING_CHAIN =
            REGISTER.register("lightning_chain",
                    () -> new ParticleType<>(false, RaevyxLightningChainData.DESERIALIZER) {
                        @Override
                        public Codec<RaevyxLightningChainData> codec() {
                            return RaevyxLightningChainData.CODEC(this);
                        }
                    });

    public static final Supplier<ParticleType<SonicRingData>> RAEVYX_SONIC_RING =
            REGISTER.register("raevyx_sonic_ring",
                    () -> new ParticleType<>(false, SonicRingData.DESERIALIZER) {
                        @Override
                        public Codec<SonicRingData> codec() {
                            return SonicRingData.CODEC(this);
                        }
                    });

    public static final Supplier<ParticleType<BloodTempestKatanaRingData>> BLOOD_TEMPEST_SWORD_RING =
            REGISTER.register("blood_tempest_sword_ring",
                    () -> new ParticleType<>(false, BloodTempestKatanaRingData.DESERIALIZER) {
                        @Override
                        public Codec<BloodTempestKatanaRingData> codec() {
                            return BloodTempestKatanaRingData.codec(this);
                        }
                    });

    public static final Supplier<ParticleType<FireBreathParticleData>> FIRE_BREATH_FLAME =
            REGISTER.register("fire_breath_flame", () -> new ParticleType<>(false, FireBreathParticleData.DESERIALIZER) {
                @Override
                public Codec<FireBreathParticleData> codec() {
                    return FireBreathParticleData.CODEC;
                }
            });

    public static final Supplier<SimpleParticleType> CINDERVANE_CRASH_SPLATTER =
            REGISTER.register("cindervane_crash_splatter", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> CINDERVANE_CRASH_FIRE =
            REGISTER.register("cindervane_crash_fire", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> CINDERVANE_CRASH_GROUND =
            REGISTER.register("cindervane_crash_ground", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> CINDERVANE_CRASH_CIRCLE =
            REGISTER.register("cindervane_crash_circle", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_FIREBALL_BRIGHT_FIRE =
            REGISTER.register("ignivorus_fireball_bright_fire", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_FIREBALL_ORANGE_SPEC =
            REGISTER.register("ignivorus_fireball_orange_spec", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_FIREBALL_EXPLOSION =
            REGISTER.register("ignivorus_fireball_explosion", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_FIREBALL_SMALL_EXPLOSION =
            REGISTER.register("ignivorus_fireball_small_explosion", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_FIREBALL_GROUND_IMPACT =
            REGISTER.register("ignivorus_fireball_ground_impact", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_LEVEL_TWO_IMPACT_EXPLOSION =
            REGISTER.register("ignivorus_level_two_impact_explosion", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_LEVEL_TWO_IMPACT_SMALL_EXPLOSION =
            REGISTER.register("ignivorus_level_two_impact_small_explosion", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_LEVEL_TWO_IMPACT_GROUND =
            REGISTER.register("ignivorus_level_two_impact_ground", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_LEVEL_TWO_IMPACT_TOON =
            REGISTER.register("ignivorus_level_two_impact_toon", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_LEVEL_TWO_IMPACT_CIRCLE =
            REGISTER.register("ignivorus_level_two_impact_circle", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> CINDERVANE_FIRE_BODY_SMOKE =
            REGISTER.register("cindervane_fire_body_smoke", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> RAEVYX_STORM_AURA =
            REGISTER.register("raevyx_storm_aura", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> RAEVYX_STORM_ZAP =
            REGISTER.register("raevyx_storm_zap", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> RAEVYX_STORM_EMITTER =
            REGISTER.register("raevyx_storm_emitter", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> RAEVYX_STORM_FIRE_SPEC =
            REGISTER.register("raevyx_storm_fire_spec", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> RAEVYX_STORM_STAR =
            REGISTER.register("raevyx_storm_star", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> CINDERVANE_FIRE_BODY_STAR =
            REGISTER.register("cindervane_fire_body_star", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> CINDERVANE_MOUTH_EMITTER =
            REGISTER.register("cindervane_mouth_emitter", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> CINDERVANE_IMPACT_EMITTER =
            REGISTER.register("cindervane_impact_emitter", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> CINDERVANE_FIRE_EXPLOSION =
            REGISTER.register("cindervane_fire_explosion", () -> Services.PLATFORM.createSimpleParticle(true));
    public static final Supplier<SimpleParticleType> CINDERVANE_SMALL_EXPLOSION =
            REGISTER.register("cindervane_small_explosion", () -> Services.PLATFORM.createSimpleParticle(true));
    public static final Supplier<SimpleParticleType> CINDERVANE_GROUND_IMPACT =
            REGISTER.register("cindervane_ground_impact", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_CHARGED_FIRE_TRAIL =
            REGISTER.register("ignivorus_charged_fire_trail", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_CHARGED_SPEC_TRAIL =
            REGISTER.register("ignivorus_charged_spec_trail", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_CHARGED_MORE_SPEC_TRAIL =
            REGISTER.register("ignivorus_charged_more_spec_trail", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_CHARGED_EMBER_TRAIL =
            REGISTER.register("ignivorus_charged_ember_trail", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_CHARGED_EMITTER_TRAIL =
            REGISTER.register("ignivorus_charged_emitter_trail", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_CHARGED_STAR_TRAIL =
            REGISTER.register("ignivorus_charged_star_trail", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_LEVEL_THREE_GROUND_SMOKE =
            REGISTER.register("ignivorus_level_three_ground_smoke", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_LEVEL_THREE_GROUND_RING =
            REGISTER.register("ignivorus_level_three_ground_ring", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_LEVEL_THREE_GROUND_GLITTER =
            REGISTER.register("ignivorus_level_three_ground_glitter", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_LEVEL_THREE_SPLATTER_LARGE =
            REGISTER.register("ignivorus_level_three_splatter_large", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_LEVEL_THREE_SPLATTER_SMALL =
            REGISTER.register("ignivorus_level_three_splatter_small", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_LEVEL_THREE_TOON_EXPLOSION =
            REGISTER.register("ignivorus_level_three_toon_explosion", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> CINDERVANE_FIREBALL_FIRE_TRAIL =
            REGISTER.register("cindervane_fireball_fire_trail", () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> CINDERVANE_BETTER_FIRE_TRAIL =
            REGISTER.register("cindervane_better_fire_trail", () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> CINDERVANE_FIRE_TRAIL =
            REGISTER.register("cindervane_fire_trail", () -> Services.PLATFORM.createSimpleParticle(false));
    public static final Supplier<SimpleParticleType> CINDERVANE_DARK_FIRE_TRAIL =
            REGISTER.register("cindervane_dark_fire_trail", () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> CINDERVANE_MORE_SPEC_TRAIL =
            REGISTER.register("cindervane_more_spec_trail", () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> CINDERVANE_SPEC_TRAIL =
            REGISTER.register("cindervane_spec_trail", () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> FIRE_BREATH_SMOKE =
            REGISTER.register("fire_breath_smoke", () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> FIRE_BREATH_EMBER =
            REGISTER.register("fire_breath_ember", () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> FIRE_BREATH_FLICKER =
            REGISTER.register("fire_breath_flicker", () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<ParticleType<FireBreathBurstData>> FIRE_BREATH_BURST =
            REGISTER.register("fire_breath_burst", () -> new ParticleType<>(false, FireBreathBurstData.DESERIALIZER) {
                @Override
                public Codec<FireBreathBurstData> codec() {
                    return FireBreathBurstData.CODEC;
                }
            });

    public static final Supplier<SimpleParticleType> FIRE_BREATH_OUTER_FLAME =
            REGISTER.register("fire_breath_outer_flame", () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> FIRE_BREATH_BACKBLAST =
            REGISTER.register("fire_breath_backblast", () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> FIRE_BREATH_STAR =
            REGISTER.register("fire_breath_star", () -> Services.PLATFORM.createSimpleParticle(false));

    public static final Supplier<SimpleParticleType> IGNIVORUS_EXPLOSION_SPEC =
            REGISTER.register("ignivorus_explosion_spec", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_EXPLOSION_FIRE =
            REGISTER.register("ignivorus_explosion_fire", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_EXPLOSION_STAR =
            REGISTER.register("ignivorus_explosion_star", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_NOVA_SPARKLE =
            REGISTER.register("ignivorus_nova_sparkle", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_CHARGE_SPARKLE =
            REGISTER.register("ignivorus_charge_sparkle", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_AFTERMATH =
            REGISTER.register("ignivorus_aftermath", () -> Services.PLATFORM.createSimpleParticle(true));
    public static final Supplier<SimpleParticleType> IGNIVORUS_AIR_AFTERMATH =
            REGISTER.register("ignivorus_air_aftermath", () -> Services.PLATFORM.createSimpleParticle(true));
    public static final Supplier<SimpleParticleType> IGNIVORUS_LINGERING_FIRE =
            REGISTER.register("ignivorus_lingering_fire", () -> Services.PLATFORM.createSimpleParticle(true));
    public static final Supplier<SimpleParticleType> IGNIVORUS_LINGERING_SPEC =
            REGISTER.register("ignivorus_lingering_spec", () -> Services.PLATFORM.createSimpleParticle(true));
    public static final Supplier<SimpleParticleType> IGNIVORUS_LINGERING_BETTER_FIRE =
            REGISTER.register("ignivorus_lingering_better_fire", () -> Services.PLATFORM.createSimpleParticle(true));
    public static final Supplier<SimpleParticleType> IGNIVORUS_NOVA_SMOKE =
            REGISTER.register("ignivorus_nova_smoke", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_EXPLOSION_LAYER =
            REGISTER.register("ignivorus_explosion_layer", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_GROUND_IMPACT =
            REGISTER.register("ignivorus_ground_impact", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_MAGMA_PILLARS_IMPACT =
            REGISTER.register("ignivorus_magma_pillars_impact", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_MAGMA_PILLAR_TOON_EXPLOSION =
            REGISTER.register("ignivorus_magma_pillar_toon_explosion", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_FIRE_SPEC =
            REGISTER.register("ignivorus_fire_spec", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_SKYFALL_CHARGE =
            REGISTER.register("ignivorus_skyfall_charge", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_SKYFALL_AURA =
            REGISTER.register("ignivorus_skyfall_aura", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_SKYFALL_SHARP =
            REGISTER.register("ignivorus_skyfall_sharp", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_SKYFALL_SWIRL =
            REGISTER.register("ignivorus_skyfall_swirl", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_SKYFALL_ABSORB =
            REGISTER.register("ignivorus_skyfall_absorb", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> IGNIVORUS_SKYFALL_CIRCLE =
            REGISTER.register("ignivorus_skyfall_circle", () -> Services.PLATFORM.createSimpleParticle(true));
    public static final Supplier<SimpleParticleType> IGNIVORUS_TOON_EXPLOSION =
            REGISTER.register("ignivorus_toon_explosion", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<SimpleParticleType> DRAGON_DUST =
            REGISTER.register("dragon_dust", () -> Services.PLATFORM.createSimpleParticle(true));

    public static final Supplier<ParticleType<GroundDecalParticleData>> GROUND_CRACK =
            REGISTER.register("ground_crack",
                    () -> new ParticleType<>(false, GroundDecalParticleData.DESERIALIZER) {
                        @Override
                        public Codec<GroundDecalParticleData> codec() {
                            return GroundDecalParticleData.codec(this);
                        }
                    });

    public static final Supplier<ParticleType<GroundDecalParticleData>> GROUND_CRACK_FISSURE =
            REGISTER.register("ground_crack_fissure",
                    () -> new ParticleType<>(false, GroundDecalParticleData.DESERIALIZER) {
                        @Override
                        public Codec<GroundDecalParticleData> codec() {
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

    private ModParticles() {
    }

    public static void register() {
        REGISTER.register();
    }
}
