package com.leon.saintsdragons.fabric.mixin.fabric;

import com.leon.saintsdragons.client.camera.DragonFovHelper;
import com.leon.saintsdragons.client.renderer.RiderBullcrap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

@Mixin(value = GameRenderer.class, priority = 500)
public class EntityRendererMixin {
    
    // Smooth FOV transition state
    @Unique
    private static double saint_sDragons$currentFOVMultiplier = 1.0;
    @Unique
    private static final double FOV_TRANSITION_SPEED = 0.05; // How fast FOV changes (0.01 = very slow, 0.1 = fast)
    @Unique
    private static boolean saintsdragons$zoomifyLookupResolved = false;
    @Unique
    private static Method saintsdragons$zoomifyDivisorMethod = null;

    @Inject(method = "getFov(Lnet/minecraft/client/Camera;FZ)D", at = @At("RETURN"), cancellable = true, require = 0)
    private void modifyFOV(Camera camera, float partialTicks, boolean useFOVSetting, CallbackInfoReturnable<Double> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getVehicle() != null) {
            Entity vehicle = mc.player.getVehicle();
            if (!DragonFovHelper.shouldApply(vehicle)) {
                saint_sDragons$currentFOVMultiplier = 1.0;
                return;
            }

            double targetFOVMultiplier = DragonFovHelper.getTargetMultiplier(vehicle);
            
            // Smooth interpolation between current and target FOV multiplier
            double diff = targetFOVMultiplier - saint_sDragons$currentFOVMultiplier;
            if (Math.abs(diff) > 0.001) { // Only interpolate if there's a meaningful difference
                saint_sDragons$currentFOVMultiplier += diff * FOV_TRANSITION_SPEED;
            } else {
                saint_sDragons$currentFOVMultiplier = targetFOVMultiplier; // Snap to target if very close
            }
            
            // Apply the smoothly interpolated FOV multiplier
            double baseFOV = cir.getReturnValue();
            if (!camera.isDetached()) {
                baseFOV /= saintsdragons$getZoomifyDivisor(partialTicks);
            }
            double newFOV = baseFOV * saint_sDragons$currentFOVMultiplier;
            
            cir.setReturnValue(newFOV);
        } else {
            saint_sDragons$currentFOVMultiplier = 1.0;
        }
    }

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void saintsdragons$beginRiderRenderFrame(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        RiderBullcrap.beginRenderFrame();
    }

    @Unique
    private static double saintsdragons$getZoomifyDivisor(float partialTicks) {
        if (!saintsdragons$zoomifyLookupResolved) {
            saintsdragons$zoomifyLookupResolved = true;
            try {
                Class<?> zoomifyClass = Class.forName("dev.isxander.zoomify.Zoomify");
                saintsdragons$zoomifyDivisorMethod = zoomifyClass.getMethod("getZoomDivisor", float.class);
            } catch (ReflectiveOperationException ignored) {
                saintsdragons$zoomifyDivisorMethod = null;
            }
        }

        if (saintsdragons$zoomifyDivisorMethod == null) {
            return 1.0D;
        }

        try {
            Object value = saintsdragons$zoomifyDivisorMethod.invoke(null, partialTicks);
            if (value instanceof Number number) {
                return Math.max(1.0D, number.doubleValue());
            }
        } catch (ReflectiveOperationException ignored) {
            saintsdragons$zoomifyDivisorMethod = null;
        }
        return 1.0D;
    }
}
