package com.leon.saintsdragons.client.event;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = SaintsDragons.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEventHandler {
    
    // Entity hiding mechanism for custom rider positioning
    public static List<UUID> blockedEntityRenders = new ArrayList<>();

    public static void blockRenderingEntity(UUID id) {
        blockedEntityRenders.add(id);
    }

    public static void releaseRenderingEntity(UUID id) {
        blockedEntityRenders.remove(id);
    }

    @SubscribeEvent
    public static void onComputeCamera(ViewportEvent.ComputeCameraAngles event) {
        Entity player = Minecraft.getInstance().getCameraEntity();
        if (player != null && player.isPassenger() && player.getVehicle() instanceof LightningDragonEntity && event.getCamera().isDetached()) {
            // Base zoom for dragon riding - adjusted distance for better dragon visibility
            event.getCamera().move(-event.getCamera().getMaxZoom(10F), 0, 0);
        }
    }

    @SubscribeEvent
    public static void preRenderLiving(RenderLivingEvent.Pre event) {
        if (blockedEntityRenders.contains(event.getEntity().getUUID())) {
            if (!isFirstPersonPlayer(event.getEntity())) {
                MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Post<>(event.getEntity(), event.getRenderer(), event.getPartialTick(), 
                    event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight()));
                event.setCanceled(true);
            }
            blockedEntityRenders.remove(event.getEntity().getUUID());
        }
    }

    public static boolean isFirstPersonPlayer(Entity entity) {
        return entity.equals(Minecraft.getInstance().cameraEntity) && Minecraft.getInstance().options.getCameraType().isFirstPerson();
    }
}