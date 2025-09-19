package com.leon.saintsdragons.server.event;

import com.leon.saintsdragons.server.entity.spawner.PrimitiveDrakeSpawner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Event handler for Primitive Drake spawning
 */
@Mod.EventBusSubscriber(modid = "saintsdragons")
public class PrimitiveDrakeSpawnEventHandler {
    
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        // Only run on server side
        if (event.getServer().overworld() == null) return;
        
        ServerLevel level = event.getServer().overworld();
        
        // Check each player
        for (Player player : level.players()) {
            if (player.isAlive() && !player.isSpectator()) {
                PrimitiveDrakeSpawner.attemptSpawnAroundPlayer(level, player.blockPosition());
            }
        }
    }
}
