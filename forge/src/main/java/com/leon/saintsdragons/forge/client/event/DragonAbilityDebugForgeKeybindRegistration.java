package com.leon.saintsdragons.forge.client.event;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SaintsDragonsCommon.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DragonAbilityDebugForgeKeybindRegistration {
    private DragonAbilityDebugForgeKeybindRegistration() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(DragonAbilityDebugForgeHandler.TOGGLE_ABILITY_DEBUG);
    }
}
