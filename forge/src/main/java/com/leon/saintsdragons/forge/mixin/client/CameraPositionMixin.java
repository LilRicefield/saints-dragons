package com.leon.saintsdragons.forge.mixin.client;

import com.leon.saintsdragons.client.renderer.DragonSeatAnchoredCamera;
import com.leon.saintsdragons.client.renderer.RiderBullcrap;
import com.leon.saintsdragons.forge.client.camera.CameraLeanData;
import com.leon.saintsdragons.forge.client.camera.DragonCameraState;
import com.leon.saintsdragons.forge.platform.ForgeClientConfig;
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

    @Inject(method = "setup", at = @At("HEAD"))
    private void saintsdragons$preSetupSyncRoll(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        if (entity == null || detached || !isFirstPersonBankingCameraEnabled()) {
            DragonCameraState.clearRoll();
            return;
        }

        Entity vehicle = entity.getVehicle();
        if (!(vehicle instanceof RideableDragonBase dragon) || !usesSeatAnchoredCameraPath(dragon)) {
            DragonCameraState.clearRoll();
            return;
        }

        float rollDegrees = getBodyRollDegrees(dragon, partialTick);
        DragonCameraState.setCurrentRoll(-rollDegrees);
    }

    @Inject(method = "setup", at = @At("TAIL"))
    private void saintsdragons$postSetupSaddlePosition(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        if (entity == null || detached) {
            return;
        }

        Entity vehicle = entity.getVehicle();
        if (!(vehicle instanceof RideableDragonBase dragon) || !usesSeatAnchoredCameraPath(dragon)) {
            return;
        }

        Vec3 saddleOffset = RiderBullcrap.getCameraOffset(dragon.getId(), getSeatIndex(dragon, entity));
        if (!DragonSeatAnchoredCamera.isValidSeatOffset(saddleOffset)) {
            return;
        }

        double leanX = CameraLeanData.getLeanX();
        double leanY = CameraLeanData.getLeanY();
        double leanZ = CameraLeanData.getLeanZ();
        this.setPosition(DragonSeatAnchoredCamera.computePivot(
                dragon,
                entity,
                saddleOffset,
                this.up,
                this.forwards,
                this.left,
                partialTick,
                leanX,
                leanY,
                leanZ
        ));
    }

    private static int getSeatIndex(RideableDragonBase dragon, Entity rider) {
        if (dragon instanceof Cindervane) {
            return Math.max(dragon.getPassengers().indexOf(rider), 0);
        }
        return 0;
    }

    private static boolean usesSeatAnchoredCameraPath(RideableDragonBase dragon) {
        return dragon instanceof Raevyx
                || dragon instanceof Cindervane
                || dragon instanceof Ignivorus
                || dragon instanceof Volitans;
    }

    private static boolean isFirstPersonBankingCameraEnabled() {
        return ForgeClientConfig.FIRST_PERSON_BANKING_CAMERA_ENABLED == null
                || ForgeClientConfig.FIRST_PERSON_BANKING_CAMERA_ENABLED.get();
    }

    private static float getBodyRollDegrees(RideableDragonBase dragon, float partialTick) {
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
