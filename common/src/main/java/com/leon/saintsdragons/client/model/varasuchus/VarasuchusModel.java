package com.leon.saintsdragons.client.model.varasuchus;

import com.leon.saintsdragons.client.model.DragonGeoModel;
import com.leon.saintsdragons.client.model.DragonModelPoseHelper;
import com.leon.saintsdragons.client.model.DragonModelPoseHelper.WeightedBoneChain;
import com.leon.saintsdragons.client.ui.DraconicCodexScreen;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.data.EntityModelData;

public class VarasuchusModel extends DragonGeoModel<Varasuchus> {
    private static final ResourceLocation VOID_KISSED_TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/varasuchus/varasuchus_void_kissed.png");
    private static final ResourceLocation VOID_KISSED_FEMALE_TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/varasuchus/varasuchus_void_kissed_female.png");
    private static final WeightedBoneChain NECK_FOLLOW = WeightedBoneChain.of(
            new String[] {"neck1Controller", "neck2Controller", "neck3Controller", "headController"},
            0.35f, 0.40f, 0.45f, 0.46f
    );
    private static final WeightedBoneChain NECK_TURN = WeightedBoneChain.of(
            new String[] {"neck1Controller", "neck2Controller", "neck3Controller", "headController"},
            0.40f, 0.42f, 0.44f, 0.46f
    );
    private static final WeightedBoneChain TAIL = WeightedBoneChain.of(
            new String[] {"tail1", "tail2", "tail3", "tail4"},
            0.25f, 0.5f, 0.75f, 1.0f
    );

    public VarasuchusModel() {
        super("varasuchus");
    }

    @Override
    protected ResourceLocation getAdultTexture(Varasuchus entity) {
        if (Varasuchus.VOID_KISSED_VARIANT_ID.equals(entity.getTextureVariantId())) {
            return entity.isFemale() ? VOID_KISSED_FEMALE_TEXTURE : VOID_KISSED_TEXTURE;
        }
        return super.getAdultTexture(entity);
    }

    @Override
    public void setCustomAnimations(Varasuchus entity, long instanceId, AnimationState<Varasuchus> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);
        if (DraconicCodexScreen.RENDERING_IN_GUI.get()) {
            return;
        }
        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (modelData == null) return;
        float partialTick = animationState.getPartialTick();
        if (entity.isAlive()){
            if (entity.isDeadOrDying()){
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

    private void applyBodyRotationDeviation(Varasuchus entity, float partialTick) {
        DragonModelPoseHelper.applyBodyYawDeviation(this, entity, "root", partialTick, -1.0f, true);
    }

    private void applyGroundNeckTurn(Varasuchus entity, float partialTick) {
        DragonModelPoseHelper.applyGroundNeckTurn(this, entity, partialTick, NECK_TURN, 25.0);
    }

    private void applyNeckFollow(Varasuchus entity, EntityModelData modelData, float partialTick) {
        float lookPitchRad = modelData.headPitch() * Mth.DEG_TO_RAD;
        float totalYawRad = DragonModelPoseHelper.lookYawWithBodyDeviation(entity, modelData, partialTick, 2.0);
        DragonModelPoseHelper.applyWeightedNeckFollow(this, entity, NECK_FOLLOW, lookPitchRad, totalYawRad);
    }

    private void applyTailDrag(Varasuchus entity, float partialTick) {
        DragonModelPoseHelper.applyTailDrag(this, entity, partialTick, TAIL, 30.0);
    }

    private void applySwimPitch(Varasuchus entity, float partialTick) {
        if (!entity.isInWater() && !entity.isInWaterOrBubble()) {
            return;
        }
        var bodyOpt = getBone("heightController");
        if (bodyOpt.isEmpty()) {
            return;
        }
        GeoBone body = bodyOpt.get();
        float swimPitchRad = entity.getSwimPitchRadians(partialTick);
        body.setRotX(body.getRotX() + swimPitchRad);
    }
}
