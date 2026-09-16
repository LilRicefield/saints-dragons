package com.leon.saintsdragons.forge.mixin.client;

import com.leon.saintsdragons.client.camera.IgnivorusSkyfallScreenEffects;
import com.leon.saintsdragons.client.input.DragonPartInteractionTargeting;
import com.leon.saintsdragons.client.renderer.RiderBullcrap;
import com.leon.saintsdragons.client.ui.SpeedLineOverlay;
import com.leon.saintsdragons.forge.platform.ForgeClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @ModifyArg(method = "pick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"),
            index = 4)
    private Predicate<Entity> saintsdragons$filterInteractionParts(Predicate<Entity> predicate) {
        return DragonPartInteractionTargeting.filter(predicate);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;flush()V"))
    private void saintsdragons$skyfallFlash(float partialTick, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        if (renderLevel) {
            IgnivorusSkyfallScreenEffects.renderFlash(partialTick);
        }
    }

    @Shadow
    private Minecraft minecraft;

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void saintsdragons$beginRiderRenderFrame(float partialTick, long nanoTime, boolean renderLevel,
                                                      CallbackInfo ci) {
        RiderBullcrap.beginRenderFrame(this.minecraft.level);
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;flush()V"
            ),
            require = 0
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
