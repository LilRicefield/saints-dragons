package com.leon.saintsdragons.forge.mixin.client;

import com.leon.saintsdragons.client.renderer.DragonSeatAnchoredCamera;
import com.leon.saintsdragons.forge.client.camera.CameraLeanData;
import com.leon.saintsdragons.forge.client.camera.DragonCameraState;
import com.leon.saintsdragons.forge.platform.ForgeClientConfig;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraPositionMixin {
    @Shadow
    private Vector3f up;

    @Shadow
    private Vector3f forwards;

    @Shadow
    private Vector3f left;

    @Shadow
    public abstract void setPosition(double x, double y, double z);

    @Shadow
    public abstract void setPosition(Vec3 pos);

    @Inject(method = "setup", at = @At("HEAD"), require = 0)
    private void saintsdragons$preSetupSyncRoll(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        if (entity == null || detached || !isFirstPersonBankingCameraEnabled()) {
            DragonCameraState.clearRoll();
            CameraLeanData.reset();
            return;
        }

        Entity vehicle = entity.getVehicle();
        if (!(vehicle instanceof RideableDragonBase dragon) || !DragonSeatAnchoredCamera.supports(dragon)) {
            DragonCameraState.clearRoll();
            CameraLeanData.reset();
            return;
        }
        if (!usesAerialBankingCamera(dragon)) {
            DragonCameraState.clearRoll();
            CameraLeanData.reset();
            return;
        }
        if (dragon instanceof Raevyx raevyx && raevyx.isBeaming()
                && ForgeClientConfig.isRaevyxBeamFirstPersonEnabled()) {
            DragonCameraState.clearRoll();
            CameraLeanData.reset();
            return;
        }

        var seat = DragonSeatAnchoredCamera.resolve(dragon, entity, partialTick);
        if (seat == null) {
            DragonCameraState.clearRoll();
            CameraLeanData.reset();
            return;
        }
        float rollDegrees = seat.cameraRoll(entity.getViewYRot(partialTick), entity.getViewXRot(partialTick));
        float pitchDegrees = Mth.lerp(partialTick, dragon.xRotO, dragon.getXRot());
        float yawSpeed = Mth.wrapDegrees(dragon.yBodyRot - dragon.yBodyRotO);
        CameraLeanData.updateTarget(-rollDegrees, pitchDegrees, yawSpeed, 1.0f);
        CameraLeanData.update();

        float cameraTilt = (float) CameraLeanData.getCameraTilt();
        DragonCameraState.setCurrentRoll(rollDegrees + cameraTilt);
    }

    @Inject(method = "setup", at = @At("TAIL"), require = 0)
    private void saintsdragons$postSetupSaddlePosition(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        if (entity == null || detached) {
            return;
        }

        Entity vehicle = entity.getVehicle();
        if (!(vehicle instanceof RideableDragonBase dragon) || !DragonSeatAnchoredCamera.supports(dragon)) {
            return;
        }
        var seat = DragonSeatAnchoredCamera.resolve(dragon, entity, partialTick);
        if (seat == null) {
            return;
        }

        double leanX = CameraLeanData.getLeanX();
        double leanY = CameraLeanData.getLeanY();
        double leanZ = CameraLeanData.getLeanZ();
        this.setPosition(DragonSeatAnchoredCamera.computePivot(
                dragon,
                entity,
                seat,
                this.up,
                this.forwards,
                this.left,
                partialTick,
                leanX,
                leanY,
                leanZ
        ));
    }

    // Set the orbit origin before vanilla performs its third-person obstruction checks.
    @Inject(method = "setup", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;getMaxZoom(D)D"), require = 0)
    private void saintsdragons$thirdPersonSeatPivot(BlockGetter level, Entity rider, boolean detached,
                                                    boolean inverseView, float partialTick, CallbackInfo ci) {
        if (detached && rider != null && rider.getVehicle() instanceof RideableDragonBase dragon) {
            var seat = DragonSeatAnchoredCamera.resolve(dragon, rider, partialTick);
            if (seat != null) {
                this.setPosition(seat.eyePosition(dragon, rider, partialTick));
            }
        }
    }

    private static boolean isFirstPersonBankingCameraEnabled() {
        return ForgeClientConfig.FIRST_PERSON_BANKING_CAMERA_ENABLED == null
                || ForgeClientConfig.FIRST_PERSON_BANKING_CAMERA_ENABLED.get();
    }

    private static boolean usesAerialBankingCamera(RideableDragonBase dragon) {
        return dragon.isFlying()
                || dragon.isTakeoff()
                || dragon.isLanding()
                || dragon.isHovering();
    }

}
