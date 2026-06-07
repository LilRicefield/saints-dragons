package com.leon.saintsdragons.client.model.stegonaut;

import com.leon.saintsdragons.client.model.DragonGeoModel;
import com.leon.saintsdragons.client.model.DragonModelPoseHelper;
import com.leon.saintsdragons.client.model.DragonModelPoseHelper.WeightedBoneChain;
import com.leon.saintsdragons.client.ui.DraconicCodexScreen;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.data.EntityModelData;

public class StegonautModel extends DragonGeoModel<Stegonaut> {
    private static final WeightedBoneChain NECK_FOLLOW = WeightedBoneChain.of(
            new String[] {"neck1Controller", "neck2Controller", "headController"},
            0.35f, 0.40f, 0.45f
    );
    private static final WeightedBoneChain NECK_TURN = WeightedBoneChain.of(
            new String[] {"neck1Controller", "neck2Controller", "headController"},
            0.30f, 0.50f, 0.35f
    );
    private static final WeightedBoneChain TAIL = WeightedBoneChain.of(
            new String[] {"tail1", "tail2", "tail3"},
            0.5f, 1.0f, 1.5f
    );

    public StegonautModel() {
        super("stegonaut");
    }

    @Override
    public void setCustomAnimations(Stegonaut entity, long instanceId, AnimationState<Stegonaut> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        if (DraconicCodexScreen.RENDERING_IN_GUI.get()) {
            return;
        }

        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (modelData == null) return;

        float partialTick = animationState.getPartialTick();

        if(entity.isAlive()) {
            if (entity.isDeadOrDying()){
                return;
            }
            if (!entity.isVehicle() && !entity.isInWaterOrBubble()) {
                applyNeckFollow(entity, modelData, animationState.getPartialTick());
            }
            applyBodyRotationDeviation(entity, partialTick);
            applyTailDrag(entity, partialTick);
            applyGroundNeckTurn(entity, partialTick);
        }
    }

    private void applyGroundNeckTurn(Stegonaut entity, float partialTick) {
        DragonModelPoseHelper.applyGroundNeckTurn(this, entity, partialTick, NECK_TURN, 25.0);
    }

    private void applyBodyRotationDeviation(Stegonaut entity, float partialTick) {
        DragonModelPoseHelper.applyBodyYawDeviation(this, entity, "root", partialTick, -1.0f, true);
    }


    private void applyTailDrag(Stegonaut entity, float partialTick) {
        DragonModelPoseHelper.applyTailDrag(this, entity, partialTick, TAIL, 30.0);
    }

    private void applyNeckFollow(Stegonaut entity, EntityModelData modelData, float partialTick) {
        float lookPitchRad = modelData.headPitch() * Mth.DEG_TO_RAD;
        float totalYawRad = DragonModelPoseHelper.lookYawWithBodyDeviation(entity, modelData, partialTick, 2.0);
        DragonModelPoseHelper.applyWeightedNeckFollow(this, NECK_FOLLOW, lookPitchRad, totalYawRad);
    }
}
