package com.leon.saintsdragons.forge.client.event;

import com.leon.saintsdragons.client.input.DragonRideInputHandler;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge wiring that exposes the shared dragon ride keybinds to the event system.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DragonRideKeybinds {
    private DragonRideKeybinds() {
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModEvents {
        private ModEvents() {
        }

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            DragonRideInputHandler.registerKeys(event::register);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            DragonRideInputHandler.clientTick();
        }
    }

    public static KeyMapping[] getKeyMappings() {
        return new KeyMapping[]{
                DragonRideInputHandler.DRAGON_ASCEND,
                DragonRideInputHandler.DRAGON_DESCEND,
                DragonRideInputHandler.DRAGON_ACCELERATE,
                DragonRideInputHandler.DRAGON_TERTIARY_ABILITY,
                DragonRideInputHandler.DRAGON_PRIMARY_ABILITY,
                DragonRideInputHandler.DRAGON_SECONDARY_ABILITY,
                DragonRideInputHandler.DRAGON_TOGGLE_MELEE
        };
    }
}

