package com.leon.saintsdragons.forge.server.event;

import com.leon.saintsdragons.server.entity.ability.abilities.stegonaut.StegonautBinderAbility;
import com.leon.saintsdragons.server.world.StegonautLushCaveSpawner;
import com.leon.saintsdragons.server.world.VolitansCoastalSpawner;
import com.leon.saintsdragons.server.world.VillageIvySpawner;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "saintsdragons")
public class ServerEventHandler {

    /**
     * Handle server tick events for portable Drake Binder buffs and village Ivy spawning
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (event.getServer().getTickCount() % 20 == 0) {
                for (ServerLevel level : event.getServer().getAllLevels()) {
                    StegonautBinderAbility.updateAllPortableBuffs(level);
                }
            }

            // Tick village Ivy spawner for all levels
            for (ServerLevel level : event.getServer().getAllLevels()) {
                VillageIvySpawner.tick(level);
                StegonautLushCaveSpawner.tick(level);
                VolitansCoastalSpawner.tick(level);
            }
        }
    }

    /**
     * Clean up village tracking when server stops
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        VillageIvySpawner.clearTracking();
        StegonautLushCaveSpawner.clearTracking();
        VolitansCoastalSpawner.clearTracking();
    }
}
