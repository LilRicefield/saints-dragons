package com.leon.saintsdragons.forge.server.event;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SaintsDragonsCommon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BabyDragonHitAdvancementHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event == null || event.getEntity() == null) {
            return;
        }
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (!(event.getEntity() instanceof DragonEntity dragon) || !dragon.isBaby()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var advancement = player.server.getAdvancements()
            .getAdvancement(SaintsDragonsCommon.rl("why"));
        if (advancement != null) {
            player.getAdvancements().award(advancement, "hit_baby");
        }
    }
}
