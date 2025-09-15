package com.leon.saintsdragons.client;

import com.leon.saintsdragons.client.ui.DragonStatusUI;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Event handler for Dragon UI system
 */
@Mod.EventBusSubscriber(modid = "saintsdragons", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class DragonUIEventHandler {
    
    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        
        DragonStatusUIManager manager = DragonStatusUIManager.getInstance();
        DragonStatusUI ui = manager.getDragonStatusUI();
        
        if (ui.isVisible()) {
            ui.render(event.getGuiGraphics(), 
                     (int) minecraft.mouseHandler.xpos(), 
                     (int) minecraft.mouseHandler.ypos(), 
                     event.getPartialTick());
        }
    }
    
    @SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            DragonStatusUIManager.getInstance().update();
            DragonUIKeybinds.handleKeybinds();
        }
    }
    
    /**
     * Block rendering of entities that are in the blockedEntityRenders list
     * This is used to prevent double rendering of players on dragons
     */
    @SubscribeEvent
    public static void preRenderLiving(RenderLivingEvent.Pre event) {
        if (ClientProxy.blockedEntityRenders.contains(event.getEntity().getUUID())) {
            // Skip rendering the player in first-person view (let the camera handle it)
            if (!(event.getEntity() == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson())) {
                event.setCanceled(true);
            }
            // Remove from blocked list after processing (one-time block)
            ClientProxy.blockedEntityRenders.remove(event.getEntity().getUUID());
        }
    }
    
}
