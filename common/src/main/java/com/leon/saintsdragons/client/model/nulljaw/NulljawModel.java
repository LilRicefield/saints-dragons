package com.leon.saintsdragons.client.model.nulljaw;

import com.leon.saintsdragons.client.model.DragonGeoModel;
import com.leon.saintsdragons.client.model.DragonModelPoseHelper;
import com.leon.saintsdragons.client.model.DragonModelPoseHelper.WeightedBoneChain;
import com.leon.saintsdragons.client.ui.DraconicCodexScreen;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.data.EntityModelData;

public final class NulljawModel extends DragonGeoModel<Nulljaw> {
    private static final WeightedBoneChain NECK_FOLLOW = WeightedBoneChain.of(
            new String[] {"neck1Controller", "neck2Controller", "neck3Controller", "headController"},
            0.25F, 0.45F, 0.65F, 0.85F
    );
    private static final WeightedBoneChain TAIL = WeightedBoneChain.of(
            new String[] {"tail1", "tail2", "tail3", "tail4", "tail5"},
            0.45F, 0.70F, 0.95F, 1.20F, 1.45F
    );

    public NulljawModel() {
        super("nulljaw");
    }

    @Override
    public void setCustomAnimations(Nulljaw entity, long instanceId, AnimationState<Nulljaw> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        if (DraconicCodexScreen.RENDERING_IN_GUI.get()) {
            return;
        }

        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (modelData == null || !entity.isAlive() || entity.isDeadOrDying()) {
            return;
        }

        float partialTick = animationState.getPartialTick();
        applyBodyRotationDeviation(entity, partialTick);
        applyFlightPitch(entity, partialTick);
        if (!entity.isInWaterOrBubble()) {
            applyNeckFollow(entity, modelData, partialTick);
        }
        applyTailDrag(entity, partialTick);
    }

    private void applyBodyRotationDeviation(Nulljaw entity, float partialTick) {
        DragonModelPoseHelper.applyBodyYawDeviation(this, entity, "heightController", partialTick, -1.0F, false);
    }

    private void applyFlightPitch(Nulljaw entity, float partialTick) {
        var bodyOpt = getBone("heightController");
        if (bodyOpt.isEmpty()) {
            return;
        }

        float pitchRad = Mth.clamp(entity.getFlightPitchRadians(partialTick), -Mth.HALF_PI, Mth.HALF_PI);
        GeoBone body = bodyOpt.get();
        body.setRotX(body.getRotX() + pitchRad);
    }

    private void applyNeckFollow(Nulljaw entity, EntityModelData modelData, float partialTick) {
        if (entity.isVehicle()) {
            return;
        }

        float totalYawRad = DragonModelPoseHelper.lookYawWithBodyDeviation(entity, modelData, partialTick, 1.4D);
        float lookPitchRad = modelData.headPitch() * Mth.DEG_TO_RAD * 0.35F;
        DragonModelPoseHelper.applyWeightedNeckFollow(this, NECK_FOLLOW, lookPitchRad, totalYawRad);
    }

    private void applyTailDrag(Nulljaw entity, float partialTick) {
        DragonModelPoseHelper.applyTailDrag(this, entity, partialTick, TAIL, 30.0D);
    }
}
