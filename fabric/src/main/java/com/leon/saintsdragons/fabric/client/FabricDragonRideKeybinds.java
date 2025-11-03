package com.leon.saintsdragons.fabric.client;

import com.leon.saintsdragons.client.input.DragonRideInputHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

/**
 * Fabric glue that registers the shared dragon riding keybinds and ticking behaviour.
 */
public final class FabricDragonRideKeybinds {
    private FabricDragonRideKeybinds() {
    }

    public static void init() {
        DragonRideInputHandler.registerKeys(KeyBindingHelper::registerKeyBinding);
        ClientTickEvents.END_CLIENT_TICK.register(client -> DragonRideInputHandler.clientTick());
    }
}

