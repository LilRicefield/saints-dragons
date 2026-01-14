package com.leon.saintsdragons.forge.client.event;

import com.leon.saintsdragons.client.DragonStatusUIManager;
import com.leon.saintsdragons.client.ui.DragonStatusUI;
import com.leon.saintsdragons.client.ui.FireballChargeIndicator;
import com.leon.saintsdragons.client.ui.RaevyxBeamMeterIndicator;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Event handler for Dragon UI system
 */
@Mod.EventBusSubscriber(modid = "saintsdragons", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class DragonUIEventHandler {

    /**
     * Hide vanilla HUD elements when riding a dragon (unless player stats mode is active)
     */
    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        DragonStatusUIManager manager = DragonStatusUIManager.getInstance();
        DragonStatusUI ui = manager.getDragonStatusUI();

        // Only hide vanilla HUD when riding and NOT in player stats mode
        if (ui.isRidingDragon() && !ui.shouldShowPlayerStats()) {
            // Hide player health
            if (event.getOverlay() == VanillaGuiOverlay.PLAYER_HEALTH.type()) {
                event.setCanceled(true);
            }
            // Hide armor bar
            else if (event.getOverlay() == VanillaGuiOverlay.ARMOR_LEVEL.type()) {
                event.setCanceled(true);
            }
            // Hide experience bar
            else if (event.getOverlay() == VanillaGuiOverlay.EXPERIENCE_BAR.type()) {
                event.setCanceled(true);
            }
            // Hide mount health bar
            else if (event.getOverlay() == VanillaGuiOverlay.MOUNT_HEALTH.type()) {
                event.setCanceled(true);
            }
            // Hide food level
            else if (event.getOverlay() == VanillaGuiOverlay.FOOD_LEVEL.type()) {
                event.setCanceled(true);
            }
            // Hide air level (breath bubbles)
            else if (event.getOverlay() == VanillaGuiOverlay.AIR_LEVEL.type()) {
                event.setCanceled(true);
            }
        }
    }

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

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        // Always render melee mode notification (independent of UI visibility)
        ui.getMeleeModeNotification().render(event.getGuiGraphics(), screenWidth, screenHeight);

        // Render fireball charge indicator when riding Ignivorus
        if (ui.getCurrentDragon() instanceof Ignivorus ignivorus) {
            FireballChargeIndicator chargeIndicator = ui.getFireballChargeIndicator();
            chargeIndicator.setChargeLevel(ignivorus.getFireballChargeLevel());
            chargeIndicator.render(event.getGuiGraphics(), screenWidth, screenHeight, event.getPartialTick());

            if (ui.isRidingDragon() && !ui.shouldShowPlayerStats()) {
                var fireBreathMeter = ui.getIgnivorusFireBreathMeterIndicator();
                fireBreathMeter.setBreathEnergy(ignivorus.getFireBreathEnergy());
                fireBreathMeter.setBreathing(ignivorus.isBreathingFire());
                fireBreathMeter.render(event.getGuiGraphics(), screenWidth, screenHeight, event.getPartialTick());
            }
        }

        // Render beam meter when riding Raevyx and dragon UI is active
        if (ui.isRidingDragon() && !ui.shouldShowPlayerStats() && ui.getCurrentDragon() instanceof Raevyx raevyx) {
            RaevyxBeamMeterIndicator beamMeter = ui.getRaevyxBeamMeterIndicator();
            beamMeter.setBeamEnergy(raevyx.getBeamEnergy());
            beamMeter.setBeaming(raevyx.isBeaming());
            beamMeter.render(event.getGuiGraphics(), screenWidth, screenHeight, event.getPartialTick());
        }

        // Render dragon ride health bar when riding and NOT in player stats mode
        if (ui.isRidingDragon() && !ui.shouldShowPlayerStats()) {
            ui.getRideHealthBar().render(event.getGuiGraphics(), screenWidth, screenHeight, event.getPartialTick());
        }
    }
    
    @SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            DragonStatusUIManager manager = DragonStatusUIManager.getInstance();
            manager.update();
            DragonUIKeybinds.handleKeybinds();

            DragonStatusUI ui = manager.getDragonStatusUI();

            // Always tick melee mode notification
            ui.getMeleeModeNotification().tick();

            // Tick fireball charge indicator for smooth animation
            ui.getFireballChargeIndicator().tick();

            // Tick beam meter for smooth animation
            ui.getRaevyxBeamMeterIndicator().tick();
            ui.getIgnivorusFireBreathMeterIndicator().tick();
        }
    }
}
