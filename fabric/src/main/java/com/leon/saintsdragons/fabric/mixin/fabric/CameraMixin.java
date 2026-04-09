package com.leon.saintsdragons.fabric.mixin.fabric;

import com.leon.saintsdragons.client.renderer.RiderBullcrap;
import com.leon.saintsdragons.fabric.client.accessor.CameraAccessor;
import com.leon.saintsdragons.fabric.client.camera.CameraLeanData;
import com.leon.saintsdragons.fabric.client.camera.DragonCameraState;
import com.leon.saintsdragons.fabric.client.event.FabricClientEventHandler;
import com.leon.saintsdragons.fabric.config.FabricClientConfigAccess;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
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

    @Inject(method = "setup", at = @At("HEAD"))
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
        if (!(vehicle instanceof RideableDragonBase dragon) || !saintsdragons$usesSeatAnchoredCameraPath(dragon)) {
            DragonCameraState.clearRoll();
            CameraLeanData.reset();
            return;
        }

        if (dragon instanceof Raevyx raevyx && raevyx.isBeaming()) {
            DragonCameraState.clearRoll();
            CameraLeanData.reset();
            return;
        }

        float rollDegrees = saintsdragons$getBodyRollDegrees(dragon, partialTick);
        float pitchDegrees = Mth.lerp(partialTick, dragon.xRotO, dragon.getXRot());
        float yawSpeed = Mth.wrapDegrees(dragon.yBodyRot - dragon.yBodyRotO);
        CameraLeanData.updateTarget(rollDegrees, pitchDegrees, yawSpeed, 1.0f);
        CameraLeanData.update();

        float cameraTilt = (float) CameraLeanData.getCameraTilt();
        DragonCameraState.setCurrentRoll(-rollDegrees + cameraTilt);
    }

    @Inject(method = "setup", at = @At("RETURN"))
    private void saintsdragons$onCameraSetup(
            BlockGetter area,
            Entity focusedEntity,
            boolean thirdPerson,
            boolean inverseView,
            float partialTick,
            CallbackInfo ci) {
        FabricClientEventHandler.onComputeCamera((Camera) (Object) this, partialTick);

        if (focusedEntity == null || thirdPerson) {
            return;
        }

        Entity vehicle = focusedEntity.getVehicle();
        if (!(vehicle instanceof RideableDragonBase dragon) || !saintsdragons$usesSeatAnchoredCameraPath(dragon)) {
            return;
        }

        if (dragon instanceof Raevyx raevyx && raevyx.isBeaming()) {
            return;
        }

        Vec3 saddleOffset = RiderBullcrap.getCameraOffset(dragon.getId(), saintsdragons$getSeatIndex(dragon, focusedEntity));
        if (saddleOffset == null) {
            return;
        }
        if (Math.abs(saddleOffset.x) >= 20.0 || Math.abs(saddleOffset.y) >= 20.0 || Math.abs(saddleOffset.z) >= 20.0) {
            return;
        }

        double interpX = Mth.lerp(partialTick, dragon.xo, dragon.getX());
        double interpY = Mth.lerp(partialTick, dragon.yo, dragon.getY());
        double interpZ = Mth.lerp(partialTick, dragon.zo, dragon.getZ());

        float eyeHeight = focusedEntity.getEyeHeight();
        Vec3 pivot = new Vec3(
                interpX + saddleOffset.x + this.up.x() * eyeHeight,
                interpY + saddleOffset.y + this.up.y() * eyeHeight,
                interpZ + saddleOffset.z + this.up.z() * eyeHeight
        );

        double leanX = CameraLeanData.getLeanX();
        double leanY = CameraLeanData.getLeanY();
        double leanZ = CameraLeanData.getLeanZ();
        if (Math.abs(leanX) > 0.001 || Math.abs(leanY) > 0.001 || Math.abs(leanZ) > 0.001) {
            pivot = pivot.add(
                    this.forwards.x() * leanZ + this.up.x() * leanY + this.left.x() * leanX,
                    this.forwards.y() * leanZ + this.up.y() * leanY + this.left.y() * leanX,
                    this.forwards.z() * leanZ + this.up.z() * leanY + this.left.z() * leanX
            );
        }

        this.setPosition(pivot);
    }

    private static int saintsdragons$getSeatIndex(RideableDragonBase dragon, Entity rider) {
        if (dragon instanceof Cindervane) {
            return Math.max(dragon.getPassengers().indexOf(rider), 0);
        }
        return 0;
    }

    private static boolean saintsdragons$usesSeatAnchoredCameraPath(RideableDragonBase dragon) {
        return dragon instanceof Raevyx
                || dragon instanceof Cindervane
                || dragon instanceof Ignivorus
                || dragon instanceof Volitans;
    }

    private static float saintsdragons$getBodyRollDegrees(RideableDragonBase dragon, float partialTick) {
        if (dragon instanceof Raevyx raevyx) {
            return raevyx.getBankAngleDegrees(partialTick) + raevyx.getSmoothedRoll(partialTick) * Mth.RAD_TO_DEG;
        }
        if (dragon instanceof Cindervane cindervane) {
            return cindervane.getBankAngleDegrees(partialTick) + cindervane.getSmoothedRoll(partialTick) * Mth.RAD_TO_DEG;
        }
        if (dragon instanceof Ignivorus ignivorus) {
            return ignivorus.getBankAngleDegrees(partialTick) + ignivorus.getSmoothedRoll(partialTick) * Mth.RAD_TO_DEG;
        }
        if (dragon instanceof Volitans volitans) {
            return volitans.getBankAngleDegrees(partialTick) + volitans.getSmoothedRoll(partialTick) * Mth.RAD_TO_DEG;
        }
        return 0.0f;
    }
}
