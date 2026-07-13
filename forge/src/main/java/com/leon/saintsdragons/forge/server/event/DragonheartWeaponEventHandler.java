package com.leon.saintsdragons.forge.server.event;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.item.tools.DragonheartSwordItem;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SaintsDragonsCommon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DragonheartWeaponEventHandler {
    private DragonheartWeaponEventHandler() {
    }

    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        if (!event.isVanillaCritical()
                || !(event.getEntity().getMainHandItem().getItem() instanceof DragonheartSwordItem sword)) {
            return;
        }

        event.setDamageModifier(event.getDamageModifier() + sword.getCriticalDamageBonus());
    }
}
