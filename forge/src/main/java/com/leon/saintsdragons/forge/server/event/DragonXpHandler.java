package com.leon.saintsdragons.forge.server.event;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = SaintsDragonsCommon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DragonXpHandler {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event == null || event.getEntity() == null || event.getSource() == null) return;
        if (!(event.getEntity().level() instanceof ServerLevel server)) return;
        if (!(event.getEntity() instanceof DragonEntity dragonVictim)) return;
        if (dragonVictim.isTame()) return; // tamed dragons: do not drop extra XP
        var credit = dragonVictim.getKillCredit();
        if (!(credit instanceof net.minecraft.world.entity.player.Player)) return;
        int xp = Mth.clamp((int)Math.ceil(dragonVictim.getMaxHealth() / 4.0f), 10, 75);
        if (xp > 0) {
            ExperienceOrb.award(server, dragonVictim.position(), xp);
        }
    }
}
