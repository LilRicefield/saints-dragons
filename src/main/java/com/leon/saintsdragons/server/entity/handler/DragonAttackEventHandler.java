package com.leon.saintsdragons.server.entity.handler;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Routes player left-clicks to the dragon's bite ability while riding.
 * This ensures clicks on any entity (including the dragon itself) trigger the bite.
 */
@Mod.EventBusSubscriber(modid = SaintsDragons.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DragonAttackEventHandler {

    @SubscribeEvent
    public static void onPlayerAttackEntity(AttackEntityEvent event) {
        if (event == null || event.getEntity() == null) return;

        // Only handle on server; client won't have authoritative combat
        if (event.getEntity().level().isClientSide) return;

        var player = event.getEntity();
        if (!(player.getVehicle() instanceof LightningDragonEntity dragon)) return;
        // Block rider-initiated attack while controls are locked (e.g., Summon Storm windup)
        if (dragon.areRiderControlsLocked()) return;
        if (!dragon.isTame() || !dragon.isOwnedBy(player)) return;

        // While ridden, randomly choose between bite and horn gore (prefer bite in flight)
        boolean useBite = dragon.isFlying() || dragon.getRandom().nextBoolean();
        if (useBite) {
            dragon.combatManager.tryUseAbility(ModAbilities.BITE);
        } else {
            dragon.combatManager.tryUseAbility(ModAbilities.HORN_GORE);
        }

        // Cancel vanilla attack so player cooldown/damage doesn't interfere
        event.setCanceled(true);
    }
}
