package com.leon.saintsdragons.client.model.cindervane;

import com.leon.saintsdragons.client.model.DragonGeoModel;
import com.leon.saintsdragons.client.model.DragonModelPoseHelper;
import com.leon.saintsdragons.client.model.DragonModelPoseHelper.WeightedBoneChain;
import com.leon.saintsdragons.client.ui.DraconicCodexScreen;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.data.EntityModelData;

public class CindervaneModel extends DragonGeoModel<Cindervane> {
    private static final WeightedBoneChain NECK = WeightedBoneChain.of(
            new String[] {"neck1Controller", "neck2Controller", "neck3Controller", "neck4Controller", "headController"},
            0.15f, 0.30f, 0.45f, 0.60f, 0.75f
    );
    private static final WeightedBoneChain TAIL = WeightedBoneChain.of(
            new String[] {"bone", "tail1", "tail2", "tail3", "tail4"},
            0.25f, 0.50f, 0.75f, 0.80f, 0.95f
    );

public CindervaneModel() {
    super("cindervane");
}

    private static final ResourceLocation ALBINO_MALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/cindervane/cindervane_albino.png");
    private static final ResourceLocation ALBINO_FEMALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/cindervane/cindervane_albino_female.png");
    @Override
    public void setCustomAnimations(Cindervane entity, long instanceId, AnimationState<Cindervane> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        if (DraconicCodexScreen.RENDERING_IN_GUI.get()) {
            return;
        }
        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (modelData == null) return;
        float partialTick = animationState.getPartialTick();

        if (entity.isAlive()) {
            if (entity.isDeadOrDying()){
                return;
            }
            if (!entity.isVehicle()) {
                applyNeckFollow(entity, modelData, animationState.getPartialTick());
            }
            applyBodyRotationDeviation(entity, partialTick);
            applyBankingRoll(entity, animationState);
            applyFlightPitch(entity, animationState);
            applyNeckBankingLean(entity, partialTick);
            applyGroundNeckTurn(entity, partialTick);
            applyTailDrag(entity, partialTick);
        }
    }

    @Override
    protected ResourceLocation getAdultTexture(Cindervane entity) {
        if (entity.hasCustomTextureVariant()) {
            return super.getAdultTexture(entity);
        }
        if (entity.getTextureVariant() == 1) {
            return entity.isFemale() ? ALBINO_FEMALE_TEXTURE : ALBINO_MALE_TEXTURE;
        }
        return super.getAdultTexture(entity);
    }

    private void applyBodyRotationDeviation(Cindervane entity, float partialTick) {
        DragonModelPoseHelper.applyBodyYawDeviation(this, entity, "root", partialTick, -1.0f, true);
    }

    private void applyBankingRoll(Cindervane entity, AnimationState<Cindervane> state) {
        var bodyOpt = getBone("body");
        if (bodyOpt.isEmpty()) {
            return;
        }

        GeoBone body = bodyOpt.get();
        var snap = body.getInitialSnapshot();

        float partialTick = state.getPartialTick();
        float bankAngleDeg = entity.getBankAngleDegrees(partialTick);
        float bankAngleRad = Mth.clamp(-bankAngleDeg * Mth.DEG_TO_RAD, -Mth.HALF_PI, Mth.HALF_PI);
        float barrelRollRad = entity.getSmoothedRoll(partialTick);
        body.setRotZ(snap.getRotZ() + bankAngleRad + barrelRollRad);
    }

    private void applyFlightPitch(Cindervane entity, AnimationState<Cindervane> state) {
        var rootOpt = getBone("root");
        if (rootOpt.isEmpty()) {
            return;
        }

        GeoBone root = rootOpt.get();
        var snap = root.getInitialSnapshot();

        float partialTick = state.getPartialTick();
        float pitchRad = entity.getFlightPitchRadians(partialTick);
        pitchRad = Mth.clamp(pitchRad, -Mth.HALF_PI, Mth.HALF_PI);

        root.setRotX(snap.getRotX() + pitchRad);
    }

    private void applyNeckBankingLean(Cindervane entity, float partialTick) {
        if (!entity.isVehicle() || !entity.isFlying()) {
            return;
        }
        float bankAngleDeg = entity.getBankAngleDegrees(partialTick);
        float neckLeanRad = -(bankAngleDeg / 45.0f) * 30.0f * Mth.DEG_TO_RAD;

        DragonModelPoseHelper.applyWeightedRotationY(this, NECK, neckLeanRad);
    }

    private void applyGroundNeckTurn(Cindervane entity, float partialTick) {
        if (entity.isFlying()) {
            return;
        }

        DragonModelPoseHelper.applyGroundNeckTurn(this, entity, partialTick, NECK, 25.0);
    }

    private void applyNeckFollow(Cindervane entity, EntityModelData modelData, float partialTick) {

        float lookPitchRad = modelData.headPitch() * Mth.DEG_TO_RAD;
        if (entity.isFlying()) {
            lookPitchRad *= 0.5f;
        }

        float totalYawRad = DragonModelPoseHelper.lookYawWithBodyDeviation(entity, modelData, partialTick, 2.0);
        DragonModelPoseHelper.applyWeightedNeckFollow(this, NECK, lookPitchRad, totalYawRad);
    }

    private void applyTailDrag(Cindervane entity, float partialTick) {
        DragonModelPoseHelper.applyTailDrag(this, entity, partialTick, TAIL, 30.0);
    }
}
