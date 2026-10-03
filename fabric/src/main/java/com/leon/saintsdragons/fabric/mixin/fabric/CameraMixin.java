package com.leon.saintsdragons.fabric.mixin.fabric;

import com.leon.saintsdragons.client.renderer.DragonSeatAnchoredCamera;
import com.leon.saintsdragons.fabric.client.accessor.CameraAccessor;
import com.leon.saintsdragons.fabric.client.camera.CameraLeanData;
import com.leon.saintsdragons.fabric.client.camera.DragonCameraState;
import com.leon.saintsdragons.fabric.client.event.FabricClientEventHandler;
import com.leon.saintsdragons.fabric.config.FabricClientConfigAccess;
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
public abstract class CameraMixin implements CameraAccessor {

    @Shadow
    protected abstract void move(double distance, double yaw, double pitch);

    @Shadow
    protected abstract double getMaxZoom(double distance);

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    protected abstract void setPosition(Vec3 pos);

    @Shadow
    protected abstract float getXRot();

    @Shadow
    protected abstract float getYRot();

    @Shadow
    private Vector3f up;

    @Shadow
    private Vector3f forwards;

    @Shadow
    private Vector3f left;

    /**
     * Accessor methods for other parts of the mod to call.
     */
    @Override
    public void saintsdragons$invokeMove(double distance, double yaw, double pitch) {
        this.move(distance, yaw, pitch);
    }

    @Override
    public double saintsdragons$invokeGetMaxZoom(double distance) {
        return this.getMaxZoom(distance);
    }

    @Override
    public void saintsdragons$invokeSetPosition(double x, double y, double z) {
        this.setPosition(x, y, z);
    }

    @Override
    public void saintsdragons$invokeSetRotation(float yaw, float pitch) {
        this.setRotation(yaw, pitch);
    }

    @Override
    public float saintsdragons$invokeGetXRot() {
        return this.getXRot();
    }

    @Override
    public float saintsdragons$invokeGetYRot() {
        return this.getYRot();
    }

    @Inject(method = "setup", at = @At("HEAD"), require = 0)
    private void saintsdragons$preSetupSyncRoll(
            BlockGetter area,
            Entity focusedEntity,
            boolean thirdPerson,
            boolean inverseView,
            float partialTick,
            CallbackInfo ci) {
        if (focusedEntity == null || thirdPerson || !FabricClientConfigAccess.isFirstPersonBankingCameraEnabled()) {
            DragonCameraState.clearRoll();
            CameraLeanData.reset();
            return;
        }

        Entity vehicle = focusedEntity.getVehicle();
        if (!(vehicle instanceof RideableDragonBase dragon) || !DragonSeatAnchoredCamera.supports(dragon)) {
            DragonCameraState.clearRoll();
            CameraLeanData.reset();
            return;
        }
        if (!saintsdragons$usesAerialBankingCamera(dragon)) {
            DragonCameraState.clearRoll();
            CameraLeanData.reset();
            return;
        }

        if (dragon instanceof Raevyx raevyx && raevyx.isBeaming()
                && FabricClientConfigAccess.isRaevyxBeamFirstPersonEnabled()) {
            DragonCameraState.clearRoll();
            CameraLeanData.reset();
            return;
        }

        var seat = DragonSeatAnchoredCamera.resolve(dragon, focusedEntity, partialTick);
        if (seat == null) {
            DragonCameraState.clearRoll();
            CameraLeanData.reset();
            return;
        }
        float rollDegrees = seat.cameraRoll(focusedEntity.getViewYRot(partialTick), focusedEntity.getViewXRot(partialTick));
        float pitchDegrees = Mth.lerp(partialTick, dragon.xRotO, dragon.getXRot());
        float yawSpeed = Mth.wrapDegrees(dragon.yBodyRot - dragon.yBodyRotO);
        CameraLeanData.updateTarget(-rollDegrees, pitchDegrees, yawSpeed, 1.0f);
        CameraLeanData.update();

        float cameraTilt = (float) CameraLeanData.getCameraTilt();
        DragonCameraState.setCurrentRoll(rollDegrees + cameraTilt);
    }

    @Inject(method = "setup", at = @At("RETURN"), require = 0)
    private void saintsdragons$onCameraSetup(
            BlockGetter area,
            Entity focusedEntity,
            boolean thirdPerson,
            boolean inverseView,
            float partialTick,
            CallbackInfo ci) {
        saintsdragons$applySeatAnchor(focusedEntity, thirdPerson, partialTick);
        FabricClientEventHandler.onComputeCamera((Camera) (Object) this, partialTick);
        FabricClientEventHandler.onComputeCameraAfterSeatAnchor((Camera) (Object) this, partialTick);
    }

    private void saintsdragons$applySeatAnchor(Entity focusedEntity, boolean thirdPerson, float partialTick) {
        if (focusedEntity == null || thirdPerson) {
            return;
        }

        Entity vehicle = focusedEntity.getVehicle();
        if (!(vehicle instanceof RideableDragonBase dragon) || !DragonSeatAnchoredCamera.supports(dragon)) {
            return;
        }

        var seat = DragonSeatAnchoredCamera.resolve(dragon, focusedEntity, partialTick);
        if (seat == null) {
            return;
        }

        double leanX = CameraLeanData.getLeanX();
        double leanY = CameraLeanData.getLeanY();
        double leanZ = CameraLeanData.getLeanZ();
        this.setPosition(DragonSeatAnchoredCamera.computePivot(
                dragon,
                focusedEntity,
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

    private static boolean saintsdragons$usesAerialBankingCamera(RideableDragonBase dragon) {
        return dragon.isFlying()
                || dragon.isTakeoff()
                || dragon.isLanding()
                || dragon.isHovering();
    }

}
