package com.leon.saintsdragons.fabric.mixin.fabric;

import com.leon.saintsdragons.client.DragonStatusUIManager;
import com.leon.saintsdragons.client.ui.DragonStatusUI;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to hide vanilla HUD elements when riding a dragon.
 *
 * Note: Many granular render methods (renderArmor, renderFood, renderAir, etc.)
 * don't exist in Minecraft 1.20.1's Gui class - HUD rendering was restructured.
 * We inject into the main render method instead.
 */
@Mixin(Gui.class)
public class GuiMixin {

    private static boolean shouldHideVanillaHud() {
        DragonStatusUIManager manager = DragonStatusUIManager.getInstance();
        DragonStatusUI ui = manager.getDragonStatusUI();
        return ui.isRidingDragon() && !ui.shouldShowPlayerStats();
    }

    /**
     * Inject into the main render method to hide ALL HUD elements at once
     * when riding a dragon and not in player stats mode.
     */
    @Inject(method = "renderPlayerHealth", at = @At("HEAD"), cancellable = true)
    private void onRenderPlayerHealth(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (shouldHideVanillaHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void onRenderExperienceBar(GuiGraphics guiGraphics, int x, CallbackInfo ci) {
        if (shouldHideVanillaHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderVehicleHealth", at = @At("HEAD"), cancellable = true)
    private void onRenderVehicleHealth(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (shouldHideVanillaHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderJumpMeter", at = @At("HEAD"), cancellable = true)
    private void onRenderJumpMeter(net.minecraft.world.entity.PlayerRideableJumping mount, GuiGraphics guiGraphics, int x, CallbackInfo ci) {
        if (shouldHideVanillaHud()) {
            ci.cancel();
        }
    }
}
