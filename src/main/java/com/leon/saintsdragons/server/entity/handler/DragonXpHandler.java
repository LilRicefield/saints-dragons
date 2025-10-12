package com.leon.saintsdragons.server.entity.handler;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Awards XP orbs for kills made by dragons in multiplayer.
 *
 * By default, Minecraft drops XP when a player (or player-attributed source) kills a mob.
 * When the wyvern kills directly (bite/gore), the victim may not drop XP.
 * This handler spawns a reasonable amount of XP orbs at the victim's position
 * when the killer is our wyvern. Roar lightning uses a vanilla LightningBolt with
 * the owner set to the wyvern's owner, so XP for Roar remains vanilla and is not doubled.
 */
@Mod.EventBusSubscriber(modid = SaintsDragons.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DragonXpHandler {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event == null || event.getEntity() == null || event.getSource() == null) return;

        // Only handle on server
        if (!(event.getEntity().level() instanceof ServerLevel server)) return;

        // We only care when the DRAGON dies, and is wild (untamed)
        if (!(event.getEntity() instanceof DragonEntity dragonVictim)) return;
        if (dragonVictim.isTame()) return; // tamed dragons: do not drop extra XP

        // Like vanilla mobs: only drop XP when killed by a player (or player-owned source)
        // Use kill credit, which accounts for direct and indirect player damage
        var credit = dragonVictim.getKillCredit();
        if (!(credit instanceof net.minecraft.world.entity.player.Player)) return;

        // Compute XP payout for the wyvern
        // Heuristic based on max health; clamp to reasonable bounds
        int xp = Mth.clamp((int)Math.ceil(dragonVictim.getMaxHealth() / 4.0f), 10, 75);

        if (xp > 0) {
            ExperienceOrb.award(server, dragonVictim.position(), xp);
        }
    }
}
