package com.leon.saintsdragons.client.compat;

import com.leon.saintsdragons.client.renderer.DragonSeatAnchoredCamera;
import com.leon.saintsdragons.client.renderer.RiderBullcrap;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.platform.Services;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.BiFunction;

public final class RealCameraCompatibility {
    private static final String REAL_CAMERA_MOD_ID = "realcamera";
    private static final String SKIN_HEAD_TARGET_NAME = "skin_head";
    private static final int PROVIDER_PRIORITY = 1_000;

    private static Constructor<?> resultConstructor;
    private static Method setPosition;
    private static Method setForward;
    private static Method setUpward;
    private static Object riderBindingTarget;
    private static Object emptyResult;
    private static boolean registered;
    private static boolean runtimeFailureLogged;

    private RealCameraCompatibility() {
    }

    public static synchronized void register() {
        if (registered || !Services.PLATFORM.isModLoaded(REAL_CAMERA_MOD_ID)) {
            return;
        }

        try {
            Class<?> apiClass = Class.forName("com.xtracr.realcamera.api.RealCameraAPI");
            Class<?> resultClass = Class.forName("com.xtracr.realcamera.api.BindResult");
            Class<?> targetClass = Class.forName("com.xtracr.realcamera.config.BindTarget");
            Field emptyField = resultClass.getField("EMPTY");
            Field defaultTargetsField = targetClass.getField("DEFAULT_TARGETS");
            Method targetName = targetClass.getMethod("name");

            resultConstructor = resultClass.getConstructor(targetClass);
            setPosition = resultClass.getMethod("setPosition", Vec3.class);
            setForward = resultClass.getMethod("setForward", Vec3.class);
            setUpward = resultClass.getMethod("setUpward", Vec3.class);
            emptyResult = emptyField.get(null);
            riderBindingTarget = findSkinHeadTarget((List<?>) defaultTargetsField.get(null), targetName);
            if (riderBindingTarget == null) {
                throw new IllegalStateException("Real Camera does not expose its skin-head target");
            }

            BiFunction<Minecraft, Float, Object> provider = RealCameraCompatibility::computeBinding;
            apiClass.getMethod("registerFunction", int.class, BiFunction.class)
                    .invoke(null, PROVIDER_PRIORITY, provider);
            registered = true;
            SaintsDragonsCommon.LOGGER.info("Enabled Real Camera compatibility for dragon riders");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            SaintsDragonsCommon.LOGGER.warn("Could not enable Real Camera compatibility", exception);
        }
    }

    private static Object computeBinding(Minecraft minecraft, Float partialTickValue) {
        Entity rider = minecraft.getCameraEntity();
        if (rider == null || !(rider.getVehicle() instanceof RideableDragonBase dragon)
                || !DragonSeatAnchoredCamera.supports(dragon)) {
            return emptyResult;
        }

        int seatIndex = DragonSeatAnchoredCamera.getSeatIndex(dragon, rider);
        Vec3 saddleOffset = RiderBullcrap.getCameraOffset(dragon.getId(), seatIndex);
        if (!DragonSeatAnchoredCamera.isValidSeatOffset(saddleOffset)) {
            return emptyResult;
        }

        try {
            float partialTick = partialTickValue == null ? 0.0F : partialTickValue;
            Camera camera = minecraft.gameRenderer.getMainCamera();
            Vector3f forward = camera.getLookVector();
            Vector3f upward = camera.getUpVector();
            Vector3f left = camera.getLeftVector();
            Vec3 cameraPivot = DragonSeatAnchoredCamera.computePivot(
                    dragon,
                    rider,
                    saddleOffset,
                    upward,
                    forward,
                    left,
                    partialTick,
                    0.0D,
                    0.0D,
                    0.0D
            );
            Vec3 interpolatedRiderPosition = new Vec3(
                    Mth.lerp(partialTick, rider.xo, rider.getX()),
                    Mth.lerp(partialTick, rider.yo, rider.getY()),
                    Mth.lerp(partialTick, rider.zo, rider.getZ())
            );

            Object result = resultConstructor.newInstance(riderBindingTarget);
            setPosition.invoke(result, cameraPivot.subtract(interpolatedRiderPosition));
            setForward.invoke(result, validDirection(forward, new Vec3(0.0D, 0.0D, 1.0D)));
            setUpward.invoke(result, validDirection(upward, new Vec3(0.0D, 1.0D, 0.0D)));
            return result;
        } catch (ReflectiveOperationException | LinkageError exception) {
            if (!runtimeFailureLogged) {
                runtimeFailureLogged = true;
                SaintsDragonsCommon.LOGGER.warn("Real Camera rejected the dragon rider binding", exception);
            }
            return emptyResult;
        }
    }

    private static Vec3 validDirection(Vector3f direction, Vec3 fallback) {
        Vec3 value = new Vec3(direction);
        return value.lengthSqr() > 1.0E-6D ? value : fallback;
    }

    private static Object findSkinHeadTarget(List<?> targets, Method targetName) throws ReflectiveOperationException {
        for (Object target : targets) {
            if (SKIN_HEAD_TARGET_NAME.equals(targetName.invoke(target))) {
                return target;
            }
        }
        return null;
    }

}
