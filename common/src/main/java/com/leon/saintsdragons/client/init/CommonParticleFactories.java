package com.leon.saintsdragons.client.init;

import com.leon.saintsdragons.client.particle.VolitansPoisonOrbTrailParticle;
import com.leon.saintsdragons.client.particle.DragonlordJumpParticle;
import com.leon.saintsdragons.client.particle.AtroxiiaIceBurstParticle;
import com.leon.saintsdragons.client.particle.AtroxiiaQuakeSmokeParticle;
import com.leon.saintsdragons.client.particle.CindervaneFireTrailParticle;
import com.leon.saintsdragons.client.particle.IgnivorusChargedFireballTrailParticle;
import com.leon.saintsdragons.client.particle.CindervaneFireImpactParticle;
import com.leon.saintsdragons.client.particle.CindervaneImpactEmitterParticle;
import com.leon.saintsdragons.client.particle.CindervaneFireBodyStarParticle;
import com.leon.saintsdragons.client.particle.RaevyxStormAuraParticle;
import com.leon.saintsdragons.client.particle.CindervaneFireBodySmokeParticle;
import com.leon.saintsdragons.client.particle.BloodTempestKatanaRingParticle;
import com.leon.saintsdragons.client.particle.BloodTempestKatanaGlintParticle;
import com.leon.saintsdragons.client.particle.BloodTempestKatanaImpactParticle;
import com.leon.saintsdragons.client.particle.BloodTempestKatanaLightningStrikeParticle;
import com.leon.saintsdragons.client.particle.BloodTempestKatanaXMarkParticle;
import com.leon.saintsdragons.client.particle.DustParticle;
import com.leon.saintsdragons.client.particle.DraconianNucleusParticle;
import com.leon.saintsdragons.client.particle.GlowingEmitterParticle;
import com.leon.saintsdragons.client.particle.FireBreathParticle;
import com.leon.saintsdragons.client.particle.FireBreathEmberParticle;
import com.leon.saintsdragons.client.particle.FireBreathFlickerParticle;
import com.leon.saintsdragons.client.particle.FireBreathBurstParticle;
import com.leon.saintsdragons.client.particle.FireBreathOuterFlameParticle;
import com.leon.saintsdragons.client.particle.FireBreathBackblastParticle;
import com.leon.saintsdragons.client.particle.FireBreathStarParticle;
import com.leon.saintsdragons.client.particle.FireBreathSmokeParticle;
import com.leon.saintsdragons.client.particle.GroundDecalParticle;
import com.leon.saintsdragons.client.particle.IgnivorusExplosionFireParticle;
import com.leon.saintsdragons.client.particle.IgnivorusExplosionStarParticle;
import com.leon.saintsdragons.client.particle.IgnivorusNovaSparkleParticle;
import com.leon.saintsdragons.client.particle.IgnivorusAftermathParticle;
import com.leon.saintsdragons.client.particle.IgnivorusAftermathFlameParticle;
import com.leon.saintsdragons.client.particle.IgnivorusExplosionLayerParticle;
import com.leon.saintsdragons.client.particle.IgnivorusMagmaPillarsImpactParticles;
import com.leon.saintsdragons.client.particle.ImpactGlowParticle;
import com.leon.saintsdragons.client.particle.MossbackPoisonFumeParticle;
import com.leon.saintsdragons.client.particle.RaevyxLightningChainParticle;
import com.leon.saintsdragons.client.particle.RaevyxLightningParticle;
import com.leon.saintsdragons.client.particle.SecondImpactRingParticle;
import com.leon.saintsdragons.client.particle.SonicRingParticle;
import com.leon.saintsdragons.client.particle.VolitansBreathParticle;
import com.leon.saintsdragons.client.particle.VolitansBreathEmitterParticle;
import com.leon.saintsdragons.client.particle.VolitansPoisonImpactParticle;
import com.leon.saintsdragons.common.registry.ModParticles;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

public final class CommonParticleFactories {
    private CommonParticleFactories() {
    }

    public static void register(Registrar registrar) {
        register(registrar, ModParticles.DRAGONLORD_JUMP_FIRE.get(), sprites -> new DragonlordJumpParticle.Factory(sprites, DragonlordJumpParticle.Kind.FIRE));
        register(registrar, ModParticles.DRAGONLORD_JUMP_SPEC.get(), sprites -> new DragonlordJumpParticle.Factory(sprites, DragonlordJumpParticle.Kind.SPEC));
        register(registrar, ModParticles.DRAGONLORD_JUMP_SMOKE.get(), sprites -> new DragonlordJumpParticle.Factory(sprites, DragonlordJumpParticle.Kind.SMOKE));
        register(registrar, ModParticles.DRAGONLORD_JUMP_EMITTER.get(), sprites -> new DragonlordJumpParticle.Factory(sprites, DragonlordJumpParticle.Kind.EMITTER));
        register(registrar, ModParticles.VOLITANS_POISON_ORB_EMITTER.get(), sprites -> new VolitansPoisonOrbTrailParticle.Factory(sprites, true));
        register(registrar, ModParticles.VOLITANS_POISON_ORB_TRAIL.get(), VolitansPoisonOrbTrailParticle.Factory::new);
        register(registrar, ModParticles.ATROXIIA_QUAKE_SMOKE.get(), AtroxiiaQuakeSmokeParticle.Factory::new);
        register(registrar, ModParticles.ATROXIIA_ICE_BURST.get(), AtroxiiaIceBurstParticle.Factory::new);
        register(registrar, ModParticles.IGNIVORUS_LEVEL_THREE_SPLATTER_LARGE.get(), sprites -> new CindervaneFireImpactParticle.Factory(sprites, CindervaneFireImpactParticle.Kind.VIOLET_SPLATTER_LARGE));
        register(registrar, ModParticles.IGNIVORUS_LEVEL_THREE_SPLATTER_SMALL.get(), sprites -> new CindervaneFireImpactParticle.Factory(sprites, CindervaneFireImpactParticle.Kind.VIOLET_SPLATTER_SMALL));
        register(registrar, ModParticles.IGNIVORUS_LEVEL_THREE_TOON_EXPLOSION.get(), sprites -> new CindervaneFireImpactParticle.Factory(sprites, CindervaneFireImpactParticle.Kind.VIOLET_TOON_EXPLOSION));
        register(registrar, ModParticles.IGNIVORUS_LEVEL_THREE_GROUND_SMOKE.get(), sprites -> new CindervaneFireImpactParticle.Factory(sprites, CindervaneFireImpactParticle.Kind.VIOLET_SMOKE));
        register(registrar, ModParticles.IGNIVORUS_LEVEL_THREE_GROUND_RING.get(), sprites -> new CindervaneFireImpactParticle.Factory(sprites, CindervaneFireImpactParticle.Kind.VIOLET_RING));
        register(registrar, ModParticles.IGNIVORUS_LEVEL_THREE_GROUND_GLITTER.get(), sprites -> new CindervaneFireImpactParticle.Factory(sprites, CindervaneFireImpactParticle.Kind.VIOLET_GLITTER));
        register(registrar, ModParticles.IGNIVORUS_CHARGED_FIRE_TRAIL.get(), sprites -> new IgnivorusChargedFireballTrailParticle.Factory(sprites, IgnivorusChargedFireballTrailParticle.Kind.FIRE));
        register(registrar, ModParticles.IGNIVORUS_CHARGED_SPEC_TRAIL.get(), sprites -> new IgnivorusChargedFireballTrailParticle.Factory(sprites, IgnivorusChargedFireballTrailParticle.Kind.SPEC));
        register(registrar, ModParticles.IGNIVORUS_CHARGED_MORE_SPEC_TRAIL.get(), sprites -> new IgnivorusChargedFireballTrailParticle.Factory(sprites, IgnivorusChargedFireballTrailParticle.Kind.MORE_SPEC));
        register(registrar, ModParticles.IGNIVORUS_CHARGED_EMBER_TRAIL.get(), sprites -> new IgnivorusChargedFireballTrailParticle.Factory(sprites, IgnivorusChargedFireballTrailParticle.Kind.EMBER));
        register(registrar, ModParticles.IGNIVORUS_CHARGED_EMITTER_TRAIL.get(), sprites -> new IgnivorusChargedFireballTrailParticle.Factory(sprites, IgnivorusChargedFireballTrailParticle.Kind.EMITTER));
        register(registrar, ModParticles.IGNIVORUS_CHARGED_STAR_TRAIL.get(), sprites -> new IgnivorusChargedFireballTrailParticle.Factory(sprites, IgnivorusChargedFireballTrailParticle.Kind.STAR));
        register(registrar, ModParticles.IGNIVORUS_LEVEL_TWO_IMPACT_CIRCLE.get(), sprites -> new CindervaneFireImpactParticle.Factory(sprites, CindervaneFireImpactParticle.Kind.CRASH_CIRCLE, 1.2F));
        register(registrar, ModParticles.IGNIVORUS_LEVEL_TWO_IMPACT_TOON.get(), sprites -> new CindervaneFireImpactParticle.Factory(sprites, CindervaneFireImpactParticle.Kind.TOON, 2.0F));
        register(registrar, ModParticles.IGNIVORUS_LEVEL_TWO_IMPACT_GROUND.get(), sprites -> new CindervaneFireImpactParticle.Factory(sprites, CindervaneFireImpactParticle.Kind.GROUND, 2.25F));
        register(registrar, ModParticles.IGNIVORUS_LEVEL_TWO_IMPACT_SMALL_EXPLOSION.get(), sprites -> new CindervaneFireImpactParticle.Factory(sprites, CindervaneFireImpactParticle.Kind.SMALL, 2.25F));
        register(registrar, ModParticles.IGNIVORUS_LEVEL_TWO_IMPACT_EXPLOSION.get(), sprites -> new CindervaneFireImpactParticle.Factory(sprites, CindervaneFireImpactParticle.Kind.FIRE, 2.25F));
        register(registrar, ModParticles.IGNIVORUS_FIREBALL_GROUND_IMPACT.get(), sprites -> new CindervaneFireImpactParticle.Factory(sprites, CindervaneFireImpactParticle.Kind.GROUND, 1.5F));
        register(registrar, ModParticles.IGNIVORUS_FIREBALL_SMALL_EXPLOSION.get(), sprites -> new CindervaneFireImpactParticle.Factory(sprites, CindervaneFireImpactParticle.Kind.SMALL, 1.5F));
        register(registrar, ModParticles.IGNIVORUS_FIREBALL_EXPLOSION.get(), sprites -> new CindervaneFireImpactParticle.Factory(sprites, CindervaneFireImpactParticle.Kind.FIRE, 1.5F));
        register(registrar, ModParticles.IGNIVORUS_FIREBALL_ORANGE_SPEC.get(), sprites -> new CindervaneFireTrailParticle.Factory(sprites, CindervaneFireTrailParticle.Kind.ORANGE_SPEC));
        register(registrar, ModParticles.IGNIVORUS_FIREBALL_BRIGHT_FIRE.get(), sprites -> new CindervaneFireTrailParticle.Factory(sprites, CindervaneFireTrailParticle.Kind.BRIGHT_FIRE));
        register(registrar, ModParticles.CINDERVANE_CRASH_SPLATTER.get(), sprites -> new CindervaneFireImpactParticle.Factory(sprites, CindervaneFireImpactParticle.Kind.CRASH_SPLATTER));
        register(registrar, ModParticles.CINDERVANE_CRASH_CIRCLE.get(), sprites -> new CindervaneFireImpactParticle.Factory(sprites, CindervaneFireImpactParticle.Kind.CRASH_CIRCLE));
        register(registrar, ModParticles.CINDERVANE_CRASH_GROUND.get(), sprites -> new CindervaneFireImpactParticle.Factory(sprites, CindervaneFireImpactParticle.Kind.CRASH_GROUND));
        register(registrar, ModParticles.CINDERVANE_CRASH_FIRE.get(), sprites -> new CindervaneFireImpactParticle.Factory(sprites, CindervaneFireImpactParticle.Kind.CRASH_FIRE));
        register(registrar, ModParticles.CINDERVANE_DARK_FIRE_TRAIL.get(), sprites -> new CindervaneFireTrailParticle.Factory(sprites, CindervaneFireTrailParticle.Kind.DARK_FIRE));
        register(registrar, ModParticles.CINDERVANE_FIRE_BODY_SMOKE.get(), CindervaneFireBodySmokeParticle.Factory::new);
        register(registrar, ModParticles.RAEVYX_STORM_AURA.get(), sprites -> new RaevyxStormAuraParticle.Factory(sprites, 0));
        register(registrar, ModParticles.RAEVYX_STORM_ZAP.get(), sprites -> new RaevyxStormAuraParticle.Factory(sprites, 1));
        register(registrar, ModParticles.RAEVYX_STORM_EMITTER.get(), sprites -> new RaevyxStormAuraParticle.Factory(sprites, 2));
        register(registrar, ModParticles.RAEVYX_STORM_FIRE_SPEC.get(), sprites -> new RaevyxStormAuraParticle.Factory(sprites, 4));
        register(registrar, ModParticles.RAEVYX_STORM_STAR.get(), sprites -> new RaevyxStormAuraParticle.Factory(sprites, 3));
        register(registrar, ModParticles.CINDERVANE_FIRE_BODY_STAR.get(), CindervaneFireBodyStarParticle.Factory::new);
        register(registrar, ModParticles.CINDERVANE_MORE_SPEC_TRAIL.get(), sprites -> new CindervaneFireTrailParticle.Factory(sprites, CindervaneFireTrailParticle.Kind.MORE_SPEC));
        register(registrar, ModParticles.CINDERVANE_MOUTH_EMITTER.get(), CindervaneImpactEmitterParticle.MouthFactory::new);
        register(registrar, ModParticles.CINDERVANE_IMPACT_EMITTER.get(), CindervaneImpactEmitterParticle.Factory::new);
        register(registrar, ModParticles.CINDERVANE_FIRE_EXPLOSION.get(), sprites -> new CindervaneFireImpactParticle.Factory(sprites, CindervaneFireImpactParticle.Kind.FIRE));
        register(registrar, ModParticles.CINDERVANE_SMALL_EXPLOSION.get(), sprites -> new CindervaneFireImpactParticle.Factory(sprites, CindervaneFireImpactParticle.Kind.SMALL));
        register(registrar, ModParticles.CINDERVANE_GROUND_IMPACT.get(), sprites -> new CindervaneFireImpactParticle.Factory(sprites, CindervaneFireImpactParticle.Kind.GROUND));
        register(registrar, ModParticles.CINDERVANE_FIREBALL_FIRE_TRAIL.get(), sprites -> new CindervaneFireTrailParticle.Factory(sprites, CindervaneFireTrailParticle.Kind.FIREBALL_FIRE));
        register(registrar, ModParticles.CINDERVANE_BETTER_FIRE_TRAIL.get(), sprites -> new CindervaneFireTrailParticle.Factory(sprites, CindervaneFireTrailParticle.Kind.BETTER_FIRE));
        register(registrar, ModParticles.CINDERVANE_FIRE_TRAIL.get(), sprites -> new CindervaneFireTrailParticle.Factory(sprites, CindervaneFireTrailParticle.Kind.FIRE));
        register(registrar, ModParticles.CINDERVANE_SPEC_TRAIL.get(), sprites -> new CindervaneFireTrailParticle.Factory(sprites, CindervaneFireTrailParticle.Kind.SPEC));
        register(registrar, ModParticles.VOLITANS_BREATH_STREAM.get(), sprites -> new VolitansBreathEmitterParticle.Factory());
        register(registrar, ModParticles.VOLITANS_WATER_BREATH.get(), sprites -> new VolitansBreathParticle.Factory(sprites, VolitansBreathParticle.Kind.WATER));
        register(registrar, ModParticles.VOLITANS_POISON_BREATH.get(), sprites -> new VolitansBreathParticle.Factory(sprites, VolitansBreathParticle.Kind.POISON));
        register(registrar, ModParticles.VOLITANS_POISON_SKULL.get(), sprites -> new VolitansBreathParticle.Factory(sprites, VolitansBreathParticle.Kind.POISON_SKULL));
        register(registrar, ModParticles.VOLITANS_POISON_FLAME.get(), sprites -> new VolitansBreathParticle.Factory(sprites, VolitansBreathParticle.Kind.POISON_FLAME));
        register(registrar, ModParticles.VOLITANS_POISON_EXPLOSION.get(), sprites -> new VolitansPoisonImpactParticle.Factory(sprites, false));
        register(registrar, ModParticles.VOLITANS_POISON_GROUND_BURST.get(), com.leon.saintsdragons.client.particle.VolitansPoisonGroundBurstParticle.Factory::new);
        register(registrar, ModParticles.VOLITANS_POISON_CLOUD.get(), sprites -> new VolitansPoisonImpactParticle.Factory(sprites, true));
        register(registrar, ModParticles.VOLITANS_BREATH_BUBBLES.get(), sprites -> new VolitansBreathParticle.Factory(sprites, VolitansBreathParticle.Kind.BUBBLES));
        register(registrar, ModParticles.VOLITANS_BREATH_EMITTER.get(), sprites -> new VolitansBreathParticle.Factory(sprites, VolitansBreathParticle.Kind.EMITTER));
        register(registrar, ModParticles.VOLITANS_BREATH_STAR.get(), sprites -> new VolitansBreathParticle.Factory(sprites, VolitansBreathParticle.Kind.STAR));
        register(registrar, ModParticles.FIRE_BREATH_FLAME.get(), FireBreathParticle.Factory::new);
        register(registrar, ModParticles.FIRE_BREATH_EMBER.get(), FireBreathEmberParticle.Factory::new);
        register(registrar, ModParticles.FIRE_BREATH_FLICKER.get(), FireBreathFlickerParticle.Factory::new);
        register(registrar, ModParticles.FIRE_BREATH_BURST.get(), FireBreathBurstParticle.Factory::new);
        register(registrar, ModParticles.FIRE_BREATH_OUTER_FLAME.get(), FireBreathOuterFlameParticle.Factory::new);
        register(registrar, ModParticles.FIRE_BREATH_BACKBLAST.get(), FireBreathBackblastParticle.Factory::new);
        register(registrar, ModParticles.FIRE_BREATH_STAR.get(), FireBreathStarParticle.Factory::new);
        register(registrar, ModParticles.FIRE_BREATH_SMOKE.get(), FireBreathSmokeParticle.Factory::new);
        register(registrar, ModParticles.IGNIVORUS_EXPLOSION_SPEC.get(), sprites -> new IgnivorusExplosionFireParticle.Factory(sprites, true));
        register(registrar, ModParticles.IGNIVORUS_EXPLOSION_FIRE.get(), IgnivorusExplosionFireParticle.Factory::new);
        register(registrar, ModParticles.IGNIVORUS_EXPLOSION_STAR.get(), IgnivorusExplosionStarParticle.Factory::new);
        register(registrar, ModParticles.IGNIVORUS_NOVA_SPARKLE.get(), IgnivorusNovaSparkleParticle.Factory::new);
        register(registrar, ModParticles.IGNIVORUS_CHARGE_SPARKLE.get(), IgnivorusNovaSparkleParticle.Factory::new);
        register(registrar, ModParticles.IGNIVORUS_AFTERMATH.get(), sprites -> new IgnivorusAftermathParticle.Factory());
        register(registrar, ModParticles.IGNIVORUS_AIR_AFTERMATH.get(), sprites -> new IgnivorusAftermathParticle.Factory(true));
        register(registrar, ModParticles.IGNIVORUS_LINGERING_FIRE.get(), sprites -> new IgnivorusAftermathFlameParticle.Factory(sprites, IgnivorusAftermathFlameParticle.Style.FIRE));
        register(registrar, ModParticles.IGNIVORUS_LINGERING_SPEC.get(), sprites -> new IgnivorusAftermathFlameParticle.Factory(sprites, IgnivorusAftermathFlameParticle.Style.SPEC));
        register(registrar, ModParticles.IGNIVORUS_LINGERING_BETTER_FIRE.get(), sprites -> new IgnivorusAftermathFlameParticle.Factory(sprites, IgnivorusAftermathFlameParticle.Style.BETTER_FIRE));
        register(registrar, ModParticles.IGNIVORUS_NOVA_SMOKE.get(), sprites -> new IgnivorusAftermathFlameParticle.Factory(sprites, IgnivorusAftermathFlameParticle.Style.SMOKE));
        register(registrar, ModParticles.IGNIVORUS_EXPLOSION_LAYER.get(), sprites -> new IgnivorusExplosionLayerParticle.Factory(sprites, IgnivorusExplosionLayerParticle.Layer.EXPLOSION));
        register(registrar, ModParticles.IGNIVORUS_GROUND_IMPACT.get(), sprites -> new IgnivorusExplosionLayerParticle.Factory(sprites, IgnivorusExplosionLayerParticle.Layer.GROUND));
        register(registrar, ModParticles.IGNIVORUS_MAGMA_PILLARS_IMPACT.get(), IgnivorusMagmaPillarsImpactParticles.Factory::new);
        register(registrar, ModParticles.IGNIVORUS_MAGMA_PILLAR_TOON_EXPLOSION.get(), sprites -> new IgnivorusExplosionLayerParticle.Factory(sprites, IgnivorusExplosionLayerParticle.Layer.MAGMA_PILLAR_TOON));
        register(registrar, ModParticles.IGNIVORUS_FIRE_SPEC.get(), sprites -> new IgnivorusExplosionLayerParticle.Factory(sprites, IgnivorusExplosionLayerParticle.Layer.SPEC));
        register(registrar, ModParticles.IGNIVORUS_SKYFALL_CHARGE.get(), sprites -> new IgnivorusExplosionLayerParticle.Factory(sprites, IgnivorusExplosionLayerParticle.Layer.CHARGE));
        register(registrar, ModParticles.IGNIVORUS_SKYFALL_AURA.get(), sprites -> new IgnivorusExplosionLayerParticle.Factory(sprites, IgnivorusExplosionLayerParticle.Layer.AURA));
        register(registrar, ModParticles.IGNIVORUS_SKYFALL_SHARP.get(), sprites -> new IgnivorusExplosionLayerParticle.Factory(sprites, IgnivorusExplosionLayerParticle.Layer.SHARP));
        register(registrar, ModParticles.IGNIVORUS_SKYFALL_SWIRL.get(), sprites -> new IgnivorusExplosionLayerParticle.Factory(sprites, IgnivorusExplosionLayerParticle.Layer.SWIRL));
        register(registrar, ModParticles.IGNIVORUS_SKYFALL_ABSORB.get(), sprites -> new IgnivorusExplosionLayerParticle.Factory(sprites, IgnivorusExplosionLayerParticle.Layer.ABSORB));
        register(registrar, ModParticles.IGNIVORUS_SKYFALL_CIRCLE.get(), sprites -> new IgnivorusExplosionLayerParticle.Factory(sprites, IgnivorusExplosionLayerParticle.Layer.CIRCLE));
        register(registrar, ModParticles.IGNIVORUS_TOON_EXPLOSION.get(), sprites -> new IgnivorusExplosionLayerParticle.Factory(sprites, IgnivorusExplosionLayerParticle.Layer.TOON));
        register(registrar, ModParticles.LIGHTNING_STORM.get(), RaevyxLightningParticle.Factory::new);
        register(registrar, ModParticles.LIGHTNING_STORM_NIGHT_GOLD.get(), RaevyxLightningParticle.Factory::new);
        register(registrar, ModParticles.LIGHTNING_CHAIN.get(), RaevyxLightningChainParticle.Factory::new);
        register(registrar, ModParticles.RAEVYX_SONIC_RING.get(), SonicRingParticle.Factory::new);
        register(registrar, ModParticles.BLOOD_TEMPEST_SWORD_RING.get(), BloodTempestKatanaRingParticle.Factory::new);
        register(registrar, ModParticles.DRAGON_DUST.get(), DustParticle.Factory::new);
        register(registrar, ModParticles.GROUND_CRACK.get(), GroundDecalParticle.Factory::new);
        register(registrar, ModParticles.GROUND_CRACK_FISSURE.get(), GroundDecalParticle.Factory::new);
        register(registrar, ModParticles.BLOOD_TEMPEST_KATANA_X_MARK.get(), BloodTempestKatanaXMarkParticle.Factory::new);
        register(registrar, ModParticles.BLOOD_TEMPEST_KATANA_GLINT.get(), BloodTempestKatanaGlintParticle.Factory::new);
        register(registrar, ModParticles.BLOOD_TEMPEST_KATANA_LIGHTNING_STRIKE.get(),
                BloodTempestKatanaLightningStrikeParticle.Factory::new);
        register(registrar, ModParticles.SECOND_LIGHTNING_STRIKE.get(),
                BloodTempestKatanaLightningStrikeParticle.Factory::new);
        register(registrar, ModParticles.GLOWING_EMITTER.get(), GlowingEmitterParticle.Factory::new);
        register(registrar, ModParticles.RED_GLOW.get(), ImpactGlowParticle.RedGlowFactory::new);
        register(registrar, ModParticles.RAINBOW_FLARE.get(), ImpactGlowParticle.RainbowFlareFactory::new);
        register(registrar, ModParticles.BLOOD_TEMPEST_KATANA_FIRST_IMPACT.get(),
                BloodTempestKatanaImpactParticle.FirstImpactFactory::new);
        register(registrar, ModParticles.BLOOD_TEMPEST_KATANA_SECOND_IMPACT.get(),
                BloodTempestKatanaImpactParticle.SecondImpactFactory::new);
        register(registrar, ModParticles.SECOND_IMPACT_RING.get(), SecondImpactRingParticle.Factory::new);
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
