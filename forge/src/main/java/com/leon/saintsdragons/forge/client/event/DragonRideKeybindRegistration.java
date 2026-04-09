package com.leon.saintsdragons.forge.client.event;

import com.leon.saintsdragons.client.input.DragonRideInputHandler;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SaintsDragonsCommon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DragonRideKeybindRegistration {
    private DragonRideKeybindRegistration() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        DragonRideInputHandler.registerKeys(event::register, false);
    }
}
