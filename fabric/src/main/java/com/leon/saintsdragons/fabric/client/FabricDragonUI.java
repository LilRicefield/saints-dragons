package com.leon.saintsdragons.fabric.client;

import com.leon.saintsdragons.client.ui.DragonRideHealthBar;
import com.leon.saintsdragons.client.ui.DragonDiveSpeedLineOverlay;
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
import com.leon.saintsdragons.fabric.config.FabricClientConfigAccess;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Fabric-specific wiring for dragon UI overlay rendering
 */
public final class FabricDragonUI {
    private static final MeleeModeNotification meleeModeNotification = new MeleeModeNotification();
    private static final FireballChargeIndicator fireballChargeIndicator = new FireballChargeIndicator();
    private static final RaevyxBeamMeterIndicator raevyxBeamMeterIndicator = new RaevyxBeamMeterIndicator();
    private static final IgnivorusFireBreathMeterIndicator ignivorusFireBreathMeterIndicator = new IgnivorusFireBreathMeterIndicator();
    private static final VolitansBreathMeterIndicator volitansBreathMeterIndicator = new VolitansBreathMeterIndicator();
    private static final DragonRideHealthBar rideHealthBar = new DragonRideHealthBar();
    private static final DragonDiveSpeedLineOverlay diveSpeedLineOverlay = new DragonDiveSpeedLineOverlay();

    private static final KeyMapping TOGGLE_DRAGON_UI = new KeyMapping(
            "key.saintsdragons.toggle_dragon_ui",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F4,
            "key.categories.saintsdragons"
    );

    static {
        // Initialize the UI registry so other classes can access the melee mode notification
        DragonUIRegistry.init(meleeModeNotification);
    }

    private FabricDragonUI() {
    }

    public static void init() {
        KeyBindingHelper.registerKeyBinding(TOGGLE_DRAGON_UI);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }

            // Handle F4 toggle keybind
            if (client.screen == null) {
                while (TOGGLE_DRAGON_UI.consumeClick()) {
                    DragonUIRegistry.toggleUIVisibility();
                }
            } else {
                // Clear queued clicks so the key isn't processed when returning to game
                TOGGLE_DRAGON_UI.consumeClick();
            }

            // Tick all UI elements for smooth animations
            meleeModeNotification.tick();
            fireballChargeIndicator.tick();
            raevyxBeamMeterIndicator.tick();
            ignivorusFireBreathMeterIndicator.tick();
            volitansBreathMeterIndicator.tick();
        });

        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) {
                return;
            }

            int width = client.getWindow().getGuiScaledWidth();
            int height = client.getWindow().getGuiScaledHeight();

            if (FabricClientConfigAccess.isDiveSpeedLinesEnabled()) {
                diveSpeedLineOverlay.render(graphics, width, height, tickDelta);
            }

            // Get current dragon if riding
            DragonEntity currentDragon = null;
            if (client.player.getVehicle() instanceof DragonEntity dragon) {
                currentDragon = dragon;
                rideHealthBar.setDragon(dragon);
            }

            // Always render melee mode notification (independent of UI visibility toggle)
            meleeModeNotification.render(graphics, width, height);

            // Only render dragon UI elements if UI is visible
            if (!DragonUIRegistry.isUIVisible()) {
                return;
            }

            // Render dragon-specific UI when riding
            if (currentDragon instanceof Ignivorus ignivorus) {
                // Fireball charge indicator
                fireballChargeIndicator.setChargeLevel(ignivorus.getFireballChargeLevel());
                fireballChargeIndicator.render(graphics, width, height, tickDelta);

                // Fire breath meter
                ignivorusFireBreathMeterIndicator.setBreathEnergy(ignivorus.getFireBreathEnergy());
                ignivorusFireBreathMeterIndicator.setBreathing(ignivorus.isBreathingFire());
                ignivorusFireBreathMeterIndicator.render(graphics, width, height, tickDelta);
            } else if (currentDragon instanceof Raevyx raevyx) {
                // Beam meter for Raevyx
                raevyxBeamMeterIndicator.setBeamEnergy(raevyx.getBeamEnergy());
                raevyxBeamMeterIndicator.setBeaming(raevyx.isBeaming());
                raevyxBeamMeterIndicator.render(graphics, width, height, tickDelta);
            } else if (currentDragon instanceof Volitans volitans) {
                volitansBreathMeterIndicator.setWaterEnergy(volitans.getWaterBreathEnergy());
                volitansBreathMeterIndicator.setPoisonEnergy(volitans.getPoisonBreathEnergy());
                volitansBreathMeterIndicator.setBreathMode(volitans.getBreathMode());
                volitansBreathMeterIndicator.setBreathing(volitans.isBreathing());
                volitansBreathMeterIndicator.render(graphics, width, height, tickDelta);
            }

            // Render dragon ride health bar when riding any dragon
            if (currentDragon != null) {
                rideHealthBar.render(graphics, width, height, tickDelta);
            }
        });
    }
}
