package com.leon.saintsdragons.fabric.mixin.fabric;

import com.leon.saintsdragons.client.camera.BloodTempestKatanaVisuals;
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
    
    @Unique
    private static final double BASELINE_FRAME_SECONDS = 1.0D / 60.0D;
    @Unique
    private static final double MAX_FRAME_SCALE = 4.0D;
    @Unique
    private static double saint_sDragons$currentFOVMultiplier = 1.0;
    @Unique
    private static final double FOV_TRANSITION_SPEED = 0.05;
    @Unique
    private static long saintsdragons$lastFovUpdateNanos = 0L;
    @Unique
    private static boolean saintsdragons$zoomifyLookupResolved = false;
    @Unique
    private static Method saintsdragons$zoomifyDivisorMethod = null;

    @Inject(method = "getFov(Lnet/minecraft/client/Camera;FZ)D", at = @At("RETURN"), cancellable = true, require = 0)
    private void modifyFOV(Camera camera, float partialTicks, boolean useFOVSetting, CallbackInfoReturnable<Double> cir) {
        Minecraft mc = Minecraft.getInstance();
        double resultFov = cir.getReturnValue();
        if (mc.player != null && mc.player.getVehicle() != null) {
            Entity vehicle = mc.player.getVehicle();
            if (!DragonFovHelper.shouldApply(vehicle)) {
                saintsdragons$resetFovSmoothing();
            } else {
                double targetFOVMultiplier = DragonFovHelper.getTargetMultiplier(vehicle);

                double diff = targetFOVMultiplier - saint_sDragons$currentFOVMultiplier;
                if (Math.abs(diff) > 0.001) {
                    double smoothing = saintsdragons$frameAdjustedSmoothing(
                            FOV_TRANSITION_SPEED, saintsdragons$consumeFovFrameScale());
                    saint_sDragons$currentFOVMultiplier += diff * smoothing;
                } else {
                    saint_sDragons$currentFOVMultiplier = targetFOVMultiplier;
                }

                if (!camera.isDetached()) {
                    resultFov /= saintsdragons$getZoomifyDivisor(partialTicks);
                }
                resultFov *= saint_sDragons$currentFOVMultiplier;
            }
        } else {
            saintsdragons$resetFovSmoothing();
        }

        resultFov *= BloodTempestKatanaVisuals.getFovMultiplier(partialTicks);
        cir.setReturnValue(resultFov);
    }

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void saintsdragons$beginRiderRenderFrame(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        RiderBullcrap.beginRenderFrame();
    }

    @Unique
    private static void saintsdragons$resetFovSmoothing() {
        saint_sDragons$currentFOVMultiplier = 1.0D;
        saintsdragons$lastFovUpdateNanos = 0L;
    }

    @Unique
    private static double saintsdragons$consumeFovFrameScale() {
        long now = System.nanoTime();
        if (saintsdragons$lastFovUpdateNanos == 0L) {
            saintsdragons$lastFovUpdateNanos = now;
            return 1.0D;
        }

        double elapsedSeconds = (now - saintsdragons$lastFovUpdateNanos) / 1_000_000_000.0D;
        saintsdragons$lastFovUpdateNanos = now;
        return Math.min(Math.max(elapsedSeconds / BASELINE_FRAME_SECONDS, 0.0D), MAX_FRAME_SCALE);
    }

    @Unique
    private static double saintsdragons$frameAdjustedSmoothing(double smoothing, double frameScale) {
        if (smoothing <= 0.0D) {
            return 0.0D;
        }
        if (smoothing >= 1.0D) {
            return 1.0D;
        }
        return 1.0D - Math.pow(1.0D - smoothing, frameScale);
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
