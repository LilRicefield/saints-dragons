package com.leon.saintsdragons.client.model.atroxiia;

import com.leon.saintsdragons.client.model.DragonGeoModel;
import com.leon.saintsdragons.client.model.DragonModelPoseHelper;
import com.leon.saintsdragons.client.ui.DraconicCodexScreen;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.data.EntityModelData;

public class AtroxiiaModel extends DragonGeoModel<Atroxiia> {

    private static final DragonModelPoseHelper.WeightedBoneChain NECK = DragonModelPoseHelper.WeightedBoneChain.of(
            new String[] {"neck1Controller", "neck2Controller", "headController"},
            0.15f, 0.30f, 0.45f
    );
    private static final DragonModelPoseHelper.WeightedBoneChain TAIL = DragonModelPoseHelper.WeightedBoneChain.of(
            new String[] {"tail1", "tail2", "tail3", "tail4", "tail5", "tail6"},
             0.30f, 0.35f, 0.40f, 0.75f, 0.80f, 1f
    );

    public AtroxiiaModel() {
        super("atroxiia");
    }

    @Override
    public void setCustomAnimations(Atroxiia entity, long instanceId, AnimationState<Atroxiia> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        if (DraconicCodexScreen.RENDERING_IN_GUI.get()) {
            return;
        }
        if (entity.isScentAssessing()) {
            return;
        }
        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (modelData == null) {
            return;
        }
        float partialTick = animationState.getPartialTick();

        if (entity.isAlive()) {
            if (entity.isDeadOrDying()) {
                return;
            }
            if (!entity.isVehicle() && !entity.isInWaterOrBubble()) {
                applyNeckFollow(entity, modelData, animationState.getPartialTick());
            }
            applyBodyRotationDeviation(entity, partialTick);
            applyGroundNeckTurn(entity, partialTick);
            applyTailDrag(entity, partialTick);
            applySwimPitch(entity, partialTick);
        }
    }

    private void applyGroundNeckTurn(Atroxiia entity, float partialTick) {
        if (entity.isFlying()) {
            return;
        }
        DragonModelPoseHelper.applyGroundNeckTurn(this, entity, partialTick, NECK, 25.0);
    }

    private void applyTailDrag(Atroxiia entity, float partialTick) {
        DragonModelPoseHelper.applyTailDrag(this, entity, partialTick, TAIL, 30.0);
    }

    private void applySwimPitch(Atroxiia entity, float partialTick) {
        if (!entity.isInWaterOrBubble()) {
            return;
        }
        GeoBone body = getBone("heightController").orElse(null);
        if (body != null) {
            body.setRotX(body.getRotX() + entity.getSwimPitchRadians(partialTick));
        }
    }

    private void applyNeckFollow(Atroxiia entity, EntityModelData modelData, float partialTick) {

        float lookPitchRad = modelData.headPitch() * Mth.DEG_TO_RAD;
        if (entity.isFlying()) {
            lookPitchRad *= 0.5f;
        }

        float totalYawRad = DragonModelPoseHelper.lookYawWithBodyDeviation(entity, modelData, partialTick, 2.0);
        DragonModelPoseHelper.applyWeightedNeckFollow(this, entity, NECK, lookPitchRad, totalYawRad);
    }
    private void applyBodyRotationDeviation(Atroxiia entity, float partialTick) {
        DragonModelPoseHelper.applyBodyYawDeviation(this, entity, "root", partialTick, -1.0f, true);
    }
}
