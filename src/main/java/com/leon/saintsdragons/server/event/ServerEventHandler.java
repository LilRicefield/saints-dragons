package com.leon.saintsdragons.server.event;

import com.leon.saintsdragons.server.entity.ability.abilities.primitivedrake.PrimitiveDrakeBinderAbility;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "saintsdragons")
public class ServerEventHandler {
    
    /**
     * Handle server tick events for portable Drake Binder buffs
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (event.getServer().getTickCount() % 20 == 0) {
                for (ServerLevel level : event.getServer().getAllLevels()) {
                    PrimitiveDrakeBinderAbility.updateAllPortableBuffs(level);
                }
            }
        }
    }
}
