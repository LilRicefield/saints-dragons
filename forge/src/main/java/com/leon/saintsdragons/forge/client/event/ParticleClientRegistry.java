package com.leon.saintsdragons.forge.client.event;

import com.leon.saintsdragons.client.init.CommonParticleFactories;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SaintsDragonsCommon.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ParticleClientRegistry {
    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        CommonParticleFactories.register(new CommonParticleFactories.Registrar() {
            @Override
            public <T extends ParticleOptions> void register(ParticleType<T> type,
                                                            CommonParticleFactories.SpriteFactory<T> factory) {
                event.registerSpriteSet(type, factory::create);
            }
        });
    }
}
