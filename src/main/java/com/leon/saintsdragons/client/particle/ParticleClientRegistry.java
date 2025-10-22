package com.leon.saintsdragons.client.particle;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.client.particle.raevyx.RaevyxLightningParticle;
import com.leon.saintsdragons.client.particle.raevyx.RaevyxLightningArcParticle;
import com.leon.saintsdragons.client.particle.raevyx.RaevyxLightningChainParticle;
import com.leon.saintsdragons.client.particle.cindervane.FireBreathFlameParticle;
import com.leon.saintsdragons.client.particle.cindervane.FireBreathSmokeParticle;
import com.leon.saintsdragons.common.registry.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SaintsDragons.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ParticleClientRegistry {
    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.LIGHTNING_STORM.get(), RaevyxLightningParticle.Factory::new);
        event.registerSpriteSet(ModParticles.LIGHTNING_ARC.get(), RaevyxLightningArcParticle.Factory::new);
        event.registerSpriteSet(ModParticles.LIGHTNING_CHAIN.get(), RaevyxLightningChainParticle.Factory::new);
        
        // Fire breath particles
        event.registerSpriteSet(ModParticles.FIRE_BREATH_FLAME.get(), FireBreathFlameParticle.Factory::new);
        event.registerSpriteSet(ModParticles.FIRE_BREATH_SMOKE.get(), FireBreathSmokeParticle.Factory::new);
    }
}