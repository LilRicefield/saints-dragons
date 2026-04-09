package com.leon.saintsdragons.forge.client.event;

import com.leon.saintsdragons.client.input.DragonRideInputHandler;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;

/**
 * Forge wiring that exposes the shared dragon ride keybinds to the event system.
 */
@Mod.EventBusSubscriber(modid = SaintsDragonsCommon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DragonRideKeybinds {
    private DragonRideKeybinds() {
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
                DragonRideInputHandler.DRAGON_TOGGLE_MELEE,
                DragonRideInputHandler.DRAGON_TOGGLE_PITCH_MODE,
                DragonRideInputHandler.DRAGON_TAUNT
        };
    }
}

