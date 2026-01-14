package com.leon.saintsdragons.fabric.client;

import com.leon.saintsdragons.client.DragonStatusUIManager;
import com.leon.saintsdragons.client.ui.DragonStatusUI;
import com.leon.saintsdragons.client.ui.FireballChargeIndicator;
import com.leon.saintsdragons.client.ui.RaevyxBeamMeterIndicator;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Fabric-specific wiring for the dragon status UI hotkey and overlay rendering.
 */
public final class FabricDragonUI {
    private static final KeyMapping TOGGLE_DRAGON_UI = new KeyMapping(
            "key.saintsdragons.toggle_dragon_ui",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F4,
            "key.categories.saintsdragons"
    );

    private FabricDragonUI() {
    }

    public static void init() {
        KeyBindingHelper.registerKeyBinding(TOGGLE_DRAGON_UI);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }

            DragonStatusUIManager manager = DragonStatusUIManager.getInstance();
            manager.update();

            if (client.screen == null) {
                while (TOGGLE_DRAGON_UI.consumeClick()) {
                    manager.getDragonStatusUI().toggleVisibility();
                }
            } else {
                // Clear queued clicks so the key isn't processed when returning to game.
                TOGGLE_DRAGON_UI.consumeClick();
            }

            DragonStatusUI ui = manager.getDragonStatusUI();
            ui.getMeleeModeNotification().tick();
            ui.getFireballChargeIndicator().tick();
            ui.getRaevyxBeamMeterIndicator().tick();
            ui.getIgnivorusFireBreathMeterIndicator().tick();
        });

        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) {
                return;
            }

            DragonStatusUIManager manager = DragonStatusUIManager.getInstance();
            DragonStatusUI ui = manager.getDragonStatusUI();

            if (ui.isVisible()) {
                ui.render(graphics, -1, -1, tickDelta);
            }

            int width = client.getWindow().getGuiScaledWidth();
            int height = client.getWindow().getGuiScaledHeight();
            ui.getMeleeModeNotification().render(graphics, width, height);

            // Render fireball charge indicator when riding Ignivorus
            if (ui.getCurrentDragon() instanceof Ignivorus ignivorus) {
                FireballChargeIndicator chargeIndicator = ui.getFireballChargeIndicator();
                chargeIndicator.setChargeLevel(ignivorus.getFireballChargeLevel());
                chargeIndicator.render(graphics, width, height, tickDelta);

                if (ui.isRidingDragon() && !ui.shouldShowPlayerStats()) {
                    var fireBreathMeter = ui.getIgnivorusFireBreathMeterIndicator();
                    fireBreathMeter.setBreathEnergy(ignivorus.getFireBreathEnergy());
                    fireBreathMeter.setBreathing(ignivorus.isBreathingFire());
                    fireBreathMeter.render(graphics, width, height, tickDelta);
                }
            }

            // Render beam meter when riding Raevyx
            if (ui.isRidingDragon() && !ui.shouldShowPlayerStats() && ui.getCurrentDragon() instanceof Raevyx raevyx) {
                RaevyxBeamMeterIndicator beamMeter = ui.getRaevyxBeamMeterIndicator();
                beamMeter.setBeamEnergy(raevyx.getBeamEnergy());
                beamMeter.setBeaming(raevyx.isBeaming());
                beamMeter.render(graphics, width, height, tickDelta);
            }

            // Render dragon ride health bar when riding and NOT in player stats mode
            if (ui.isRidingDragon() && !ui.shouldShowPlayerStats()) {
                ui.getRideHealthBar().render(graphics, width, height, tickDelta);
            }
        });
    }
}
