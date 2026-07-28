package com.leon.saintsdragons.forge.mixin.client;

import com.leon.saintsdragons.client.ui.SpeedLineOverlay;
import com.leon.saintsdragons.forge.platform.ForgeClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    private Minecraft minecraft;

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;flush()V"
            )
    )
    private void saintsdragons$renderSpeedLinesWithHiddenGui(float partialTick, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        if (!renderLevel
                || this.minecraft.player == null
                || this.minecraft.screen != null
                || !this.minecraft.options.hideGui
                || !ForgeClientConfig.DIVE_SPEED_LINES_ENABLED.get()) {
            return;
        }

        GuiGraphics graphics = new GuiGraphics(this.minecraft, this.minecraft.renderBuffers().bufferSource());
        SpeedLineOverlay.INSTANCE.render(
                graphics,
                this.minecraft.getWindow().getGuiScaledWidth(),
                this.minecraft.getWindow().getGuiScaledHeight(),
                partialTick
        );
    }
}
