package com.leon.saintsdragons.forge.server.event;

import com.leon.saintsdragons.server.entity.ability.abilities.stegonaut.StegonautBuffAbility;
import com.leon.saintsdragons.server.world.RaevyxStormSpawner;
import com.leon.saintsdragons.server.world.StegonautLushCaveSpawner;
import com.leon.saintsdragons.server.world.VolitansUnderwaterSpawner;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "saintsdragons")
public class ServerEventHandler {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (event.getServer().getTickCount() % 20 == 0) {
                for (ServerLevel level : event.getServer().getAllLevels()) {
                    StegonautBuffAbility.updateAllPortableBuffs(level);
                }
            }

            for (ServerLevel level : event.getServer().getAllLevels()) {
                RaevyxStormSpawner.tick(level);
                StegonautLushCaveSpawner.tick(level);
                VolitansUnderwaterSpawner.tick(level);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        RaevyxStormSpawner.clearTracking();
        StegonautLushCaveSpawner.clearTracking();
        VolitansUnderwaterSpawner.clearTracking();
    }
}