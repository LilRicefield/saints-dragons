package com.leon.saintsdragons.forge.server.event;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.init.CommonServerLifecycleEvents;
import com.leon.saintsdragons.common.item.BloodTempestArmorSetBonus;
import com.leon.saintsdragons.common.item.DragonlordArmorSetBonus;
import com.leon.saintsdragons.server.debug.DragonPathDebugTracker;
import com.leon.saintsdragons.server.ai.dragonbrain.debug.DragonBrainDiagnostics;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.npc.IvyTheDragonMerchant;
import com.leon.saintsdragons.forge.entity.part.ForgeDragonPart;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingMakeBrainEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SaintsDragonsCommon.MOD_ID)
public class ServerEventHandler {

    @SubscribeEvent
    public static void onLivingMakeBrain(LivingMakeBrainEvent event) {
        if (!(event.getEntity() instanceof DragonEntity dragon)) {
            return;
        }

        java.util.List<DragonBrainDiagnostics.RegisteredBehaviour> behaviours = new java.util.ArrayList<>();
        event.getTypedBrainBuilder(dragon).getAvailableBehaviorsByPriority().forEach((priority, activities) ->
                activities.forEach((activity, controls) -> controls.forEach(control -> {
                    @SuppressWarnings("unchecked")
                    net.minecraft.world.entity.ai.behavior.BehaviorControl<? super net.minecraft.world.entity.LivingEntity>
                            debugControl = (net.minecraft.world.entity.ai.behavior.BehaviorControl<? super net.minecraft.world.entity.LivingEntity>)
                            (net.minecraft.world.entity.ai.behavior.BehaviorControl<?>)control;
                    behaviours.add(new DragonBrainDiagnostics.RegisteredBehaviour(
                            activity, priority, debugControl));
                })));
        DragonBrainDiagnostics.attach(dragon, behaviours);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            CommonServerLifecycleEvents.onEndServerTick(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CommonServerLifecycleEvents.onPlayerJoin(player);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel sourceLevel = player.server.getLevel(event.getFrom());
        if (sourceLevel != null) {
            IvyTheDragonMerchant.followOwnerAcrossDimension(player, sourceLevel);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.isEndConquered()) {
            return;
        }
        ServerLevel endLevel = player.server.getLevel(Level.END);
        if (endLevel != null) {
            player.server.execute(() -> IvyTheDragonMerchant.followOwnerAcrossDimension(player, endLevel));
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (BloodTempestArmorSetBonus.blocksDamage(player, event.getSource())
                || DragonlordArmorSetBonus.blocksDamage(player, event.getSource())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !event.getItemStack().is(Items.DEBUG_STICK)
                || !player.canUseGameMasterBlocks()) {
            return;
        }

        DragonEntity dragon = null;
        if (event.getTarget() instanceof DragonEntity directDragon) {
            dragon = directDragon;
        } else if (event.getTarget() instanceof ForgeDragonPart part
                && part.getParent() instanceof DragonEntity parentDragon) {
            dragon = parentDragon;
        }
        if (dragon == null) {
            return;
        }

        DragonPathDebugTracker.toggle(player, dragon);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CommonServerLifecycleEvents.onPlayerDisconnect(player);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        CommonServerLifecycleEvents.onServerStopping(event.getServer());
    }
}
