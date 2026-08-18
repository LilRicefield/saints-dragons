package com.leon.saintsdragons.fabric.mixin.fabric;

import com.leon.saintsdragons.client.renderer.EntityPreviewRenderContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @Inject(method = "renderEntityInInventory", at = @At("HEAD"))
    private static void saintsdragons$beginEntityPreview(GuiGraphics guiGraphics,
                                                          int x,
                                                          int y,
                                                          int scale,
                                                          Quaternionf pose,
                                                          Quaternionf cameraOrientation,
                                                          LivingEntity entity,
                                                          CallbackInfo ci) {
        EntityPreviewRenderContext.begin();
    }

    @Inject(method = "renderEntityInInventory", at = @At("RETURN"))
    private static void saintsdragons$endEntityPreview(GuiGraphics guiGraphics,
                                                        int x,
                                                        int y,
                                                        int scale,
                                                        Quaternionf pose,
                                                        Quaternionf cameraOrientation,
                                                        LivingEntity entity,
                                                        CallbackInfo ci) {
        EntityPreviewRenderContext.end();
    }
}
