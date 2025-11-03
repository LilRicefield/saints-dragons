package com.leon.saintsdragons.forge.client;

import com.leon.saintsdragons.client.DragonStatusUIManager;
import com.leon.saintsdragons.client.ui.DragonStatusUI;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
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

        // Render UI (health and speed only, no control guide)
        if (ui.isVisible()) {
            ui.render(event.getGuiGraphics(), -1, -1, event.getPartialTick());
        }

        // Always render melee mode notification (independent of UI visibility)
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        ui.getMeleeModeNotification().render(event.getGuiGraphics(), screenWidth, screenHeight);
    }
    
    @SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            DragonStatusUIManager manager = DragonStatusUIManager.getInstance();
            manager.update();
            DragonUIKeybinds.handleKeybinds();

            // Always tick melee mode notification
            manager.getDragonStatusUI().getMeleeModeNotification().tick();
        }
    }
}