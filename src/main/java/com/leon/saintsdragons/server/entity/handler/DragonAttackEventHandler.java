package com.leon.saintsdragons.server.entity.handler;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Routes player left-clicks to the wyvern's abilities while riding.
 * This ensures clicks on any entity (including the wyvern itself) trigger wyvern abilities.
 */
@Mod.EventBusSubscriber(modid = SaintsDragons.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DragonAttackEventHandler {

    @SubscribeEvent
    public static void onPlayerAttackEntity(AttackEntityEvent event) {
        if (event == null || event.getEntity() == null) return;

        // Only handle on server; client won't have authoritative combat
        if (event.getEntity().level().isClientSide) return;

        var player = event.getEntity();
        if (!(player.getVehicle() instanceof DragonEntity dragon)) return;
        // Block rider-initiated attack while controls are locked (e.g., Summon Storm windup)
        if (dragon.areRiderControlsLocked()) return;
        if (!dragon.isTame() || !dragon.isOwnedBy(player)) return;

        // While ridden, use wyvern's primary attack ability
        // Each wyvern type can define its own attack abilities
        var abilityType = dragon.getPrimaryAttackAbility();
        if (abilityType != null) {
            // Use the combat manager to handle ability activation
            dragon.combatManager.tryUseAbility(abilityType);
        }

        // Cancel vanilla attack so player cooldown/damage doesn't interfere
        event.setCanceled(true);
    }
}
