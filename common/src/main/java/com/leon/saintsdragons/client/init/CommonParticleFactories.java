package com.leon.saintsdragons.client.init;

import com.leon.saintsdragons.client.particle.AtroxiiaSnowParticle;
import com.leon.saintsdragons.client.particle.AtroxiiaSnowDustParticle;
import com.leon.saintsdragons.client.particle.AtroxiiaSnowShardParticle;
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
import com.leon.saintsdragons.client.particle.ImpactGlowParticle;
import com.leon.saintsdragons.client.particle.MossbackPoisonFumeParticle;
import com.leon.saintsdragons.client.particle.RaevyxLightningChainParticle;
import com.leon.saintsdragons.client.particle.RaevyxLightningParticle;
import com.leon.saintsdragons.client.particle.SecondImpactRingParticle;
import com.leon.saintsdragons.client.particle.SonicRingParticle;
import com.leon.saintsdragons.client.particle.VolitansBreathParticle;
import com.leon.saintsdragons.common.registry.ModParticles;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

public final class CommonParticleFactories {
    private CommonParticleFactories() {
    }

    public static void register(Registrar registrar) {
        register(registrar, ModParticles.VOLITANS_WATER_BREATH.get(), sprites -> new VolitansBreathParticle.Factory(sprites, false));
        register(registrar, ModParticles.VOLITANS_POISON_BREATH.get(), sprites -> new VolitansBreathParticle.Factory(sprites, true));
        register(registrar, ModParticles.FIRE_BREATH_FLAME.get(), FireBreathParticle.Factory::new);
        register(registrar, ModParticles.FIRE_BREATH_EMBER.get(), FireBreathEmberParticle.Factory::new);
        register(registrar, ModParticles.FIRE_BREATH_FLICKER.get(), FireBreathFlickerParticle.Factory::new);
        register(registrar, ModParticles.FIRE_BREATH_BURST.get(), FireBreathBurstParticle.Factory::new);
        register(registrar, ModParticles.FIRE_BREATH_OUTER_FLAME.get(), FireBreathOuterFlameParticle.Factory::new);
        register(registrar, ModParticles.FIRE_BREATH_BACKBLAST.get(), FireBreathBackblastParticle.Factory::new);
        register(registrar, ModParticles.FIRE_BREATH_STAR.get(), FireBreathStarParticle.Factory::new);
        register(registrar, ModParticles.FIRE_BREATH_SMOKE.get(), FireBreathSmokeParticle.Factory::new);
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
        register(registrar, ModParticles.ATROXIIA_SNOW.get(), AtroxiiaSnowParticle.FlakeFactory::new);
        register(registrar, ModParticles.ATROXIIA_SNOW_SHARD.get(), AtroxiiaSnowShardParticle.Factory::new);
        register(registrar, ModParticles.ATROXIIA_SNOW_SPARK.get(), AtroxiiaSnowParticle.SparkFactory::new);
        register(registrar, ModParticles.ATROXIIA_SNOW_DUST.get(), AtroxiiaSnowDustParticle.Factory::new);
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
