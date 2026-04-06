package com.leon.saintsdragons.forge.client.event;

import com.leon.saintsdragons.client.ui.DragonRideHealthBar;
import com.leon.saintsdragons.client.ui.DragonUIRegistry;
import com.leon.saintsdragons.client.ui.FireballChargeIndicator;
import com.leon.saintsdragons.client.ui.IgnivorusFireBreathMeterIndicator;
import com.leon.saintsdragons.client.ui.MeleeModeNotification;
import com.leon.saintsdragons.client.ui.RaevyxBeamMeterIndicator;
import com.leon.saintsdragons.client.ui.VolitansBreathMeterIndicator;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Event handler for Dragon UI rendering
 */
@Mod.EventBusSubscriber(modid = "saintsdragons", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class DragonUIEventHandler {
    private static final MeleeModeNotification meleeModeNotification = new MeleeModeNotification();
    private static final FireballChargeIndicator fireballChargeIndicator = new FireballChargeIndicator();
    private static final RaevyxBeamMeterIndicator raevyxBeamMeterIndicator = new RaevyxBeamMeterIndicator();
    private static final IgnivorusFireBreathMeterIndicator ignivorusFireBreathMeterIndicator = new IgnivorusFireBreathMeterIndicator();
    private static final VolitansBreathMeterIndicator volitansBreathMeterIndicator = new VolitansBreathMeterIndicator();
    private static final DragonRideHealthBar rideHealthBar = new DragonRideHealthBar();

    static {
        // Initialize the UI registry so other classes can access the melee mode notification
        DragonUIRegistry.init(meleeModeNotification);
    }

    /**
     * Hide vanilla HUD elements when riding a dragon (only if dragon UI is visible)
     */
    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(minecraft.player.getVehicle() instanceof DragonEntity)) {
            return;
        }

        // Only hide vanilla HUD if dragon UI is visible (F4 toggle)
        // When dragon UI is hidden, show vanilla HUD instead
        if (!DragonUIRegistry.isUIVisible()) {
            return;
        }

        // Hide vanilla HUD when riding a dragon and dragon UI is active
        if (event.getOverlay() == VanillaGuiOverlay.PLAYER_HEALTH.type()) {
            event.setCanceled(true);
        } else if (event.getOverlay() == VanillaGuiOverlay.ARMOR_LEVEL.type()) {
            event.setCanceled(true);
        } else if (event.getOverlay() == VanillaGuiOverlay.EXPERIENCE_BAR.type()) {
            event.setCanceled(true);
        } else if (event.getOverlay() == VanillaGuiOverlay.MOUNT_HEALTH.type()) {
            event.setCanceled(true);
        } else if (event.getOverlay() == VanillaGuiOverlay.FOOD_LEVEL.type()) {
            event.setCanceled(true);
        } else if (event.getOverlay() == VanillaGuiOverlay.AIR_LEVEL.type()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        // Get current dragon if riding
        DragonEntity currentDragon = null;
        if (minecraft.player.getVehicle() instanceof DragonEntity dragon) {
            currentDragon = dragon;
            rideHealthBar.setDragon(dragon);
        }

        // Always render melee mode notification (independent of UI visibility toggle)
        meleeModeNotification.render(event.getGuiGraphics(), screenWidth, screenHeight);

        // Only render dragon UI elements if UI is visible
        if (!DragonUIRegistry.isUIVisible()) {
            return;
        }

        // Render dragon-specific UI when riding
        if (currentDragon instanceof Ignivorus ignivorus) {
            // Fireball charge indicator
            fireballChargeIndicator.setChargeLevel(ignivorus.getFireballChargeLevel());
            fireballChargeIndicator.render(event.getGuiGraphics(), screenWidth, screenHeight, event.getPartialTick());

            // Fire breath meter
            ignivorusFireBreathMeterIndicator.setBreathEnergy(ignivorus.getFireBreathEnergy());
            ignivorusFireBreathMeterIndicator.setBreathing(ignivorus.isBreathingFire());
            ignivorusFireBreathMeterIndicator.render(event.getGuiGraphics(), screenWidth, screenHeight, event.getPartialTick());
        } else if (currentDragon instanceof Raevyx raevyx) {
            // Beam meter for Raevyx
            raevyxBeamMeterIndicator.setBeamEnergy(raevyx.getBeamEnergy());
            raevyxBeamMeterIndicator.setBeaming(raevyx.isBeaming());
            raevyxBeamMeterIndicator.render(event.getGuiGraphics(), screenWidth, screenHeight, event.getPartialTick());
        } else if (currentDragon instanceof Volitans volitans) {
            volitansBreathMeterIndicator.setWaterEnergy(volitans.getWaterBreathEnergy());
            volitansBreathMeterIndicator.setPoisonEnergy(volitans.getPoisonBreathEnergy());
            volitansBreathMeterIndicator.setBreathMode(volitans.getBreathMode());
            volitansBreathMeterIndicator.setBreathing(volitans.isBreathing());
            volitansBreathMeterIndicator.render(event.getGuiGraphics(), screenWidth, screenHeight, event.getPartialTick());
        }

        // Render dragon ride health bar when riding any dragon
        if (currentDragon != null) {
            rideHealthBar.render(event.getGuiGraphics(), screenWidth, screenHeight, event.getPartialTick());
        }
    }

    @SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            // Handle keybinds
            DragonUIKeybinds.handleKeybinds();

            // Tick all UI elements for smooth animations
            meleeModeNotification.tick();
            fireballChargeIndicator.tick();
            raevyxBeamMeterIndicator.tick();
            ignivorusFireBreathMeterIndicator.tick();
            volitansBreathMeterIndicator.tick();
        }
    }
}
