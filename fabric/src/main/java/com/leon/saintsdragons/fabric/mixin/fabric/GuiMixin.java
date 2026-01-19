package com.leon.saintsdragons.fabric.mixin.fabric;

import com.leon.saintsdragons.client.ui.DragonUIRegistry;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to hide vanilla HUD elements when riding a dragon (only if dragon UI is visible).
 */
@Mixin(Gui.class)
public class GuiMixin {

    private static boolean shouldHideVanillaHud() {
        Minecraft mc = Minecraft.getInstance();
        // Only hide vanilla HUD if riding a dragon AND dragon UI is visible (F4 toggle)
        // When dragon UI is hidden, show vanilla HUD instead
        return mc.player != null
            && mc.player.getVehicle() instanceof DragonEntity
            && DragonUIRegistry.isUIVisible();
    }

    /**
     * Hide vanilla player health when riding a dragon
     */
    @Inject(method = "renderPlayerHealth", at = @At("HEAD"), cancellable = true)
    private void onRenderPlayerHealth(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (shouldHideVanillaHud()) {
            ci.cancel();
        }
    }

    /**
     * Hide experience bar when riding a dragon
     */
    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void onRenderExperienceBar(GuiGraphics guiGraphics, int x, CallbackInfo ci) {
        if (shouldHideVanillaHud()) {
            ci.cancel();
        }
    }

    /**
     * Hide vehicle health bar when riding a dragon
     */
    @Inject(method = "renderVehicleHealth", at = @At("HEAD"), cancellable = true)
    private void onRenderVehicleHealth(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (shouldHideVanillaHud()) {
            ci.cancel();
        }
    }

    /**
     * Hide jump meter when riding a dragon
     */
    @Inject(method = "renderJumpMeter", at = @At("HEAD"), cancellable = true)
    private void onRenderJumpMeter(net.minecraft.world.entity.PlayerRideableJumping mount, GuiGraphics guiGraphics, int x, CallbackInfo ci) {
        if (shouldHideVanillaHud()) {
            ci.cancel();
        }
    }
}
