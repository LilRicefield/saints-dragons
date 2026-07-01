package com.leon.saintsdragons.client.model.raevyx;

import com.leon.saintsdragons.client.model.DragonGeoModel;
import com.leon.saintsdragons.client.model.DragonModelPoseHelper;
import com.leon.saintsdragons.client.model.DragonModelPoseHelper.WeightedBoneChain;
import com.leon.saintsdragons.client.ui.DraconicCodexScreen;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;
public class RaevyxModel extends DragonGeoModel<Raevyx> {
    private static final WeightedBoneChain NECK_FOLLOW = WeightedBoneChain.of(
            new String[] {"neck1Controller", "neck2Controller", "neck3Controller", "headController"},
            0.20f, 0.25f, 0.30f, 0.35f
    );
    private static final WeightedBoneChain NECK_TURN = WeightedBoneChain.of(
            new String[] {"neck1Controller", "neck2Controller", "neck3Controller", "headController"},
            0.35f, 0.45f, 0.55f, 0.60f
    );
    private static final WeightedBoneChain TAIL = WeightedBoneChain.of(
            new String[] {"tail1", "tail2", "tail3", "tail4", "tail5"},
            0.5f, 0.75f, 1.0f, 1.25f, 1.75f
    );

    public  RaevyxModel() {
        super("raevyx");
    }

    private static final ResourceLocation NIGHT_GOLD_TEXTURE = SaintsDragonsCommon.rl("textures/entity/raevyx/raevyx_night_gold.png");
    private static final ResourceLocation NIGHT_GOLD_FEMALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/raevyx/raevyx_night_gold_female.png");

    @Override
    protected ResourceLocation getAdultTexture(Raevyx entity) {
        if (entity.hasCustomTextureVariant()) {
            return super.getAdultTexture(entity);
        }
        boolean nightGold = entity.getTextureVariant() == Raevyx.VARIANT_NIGHT_GOLD;
        if (nightGold) {
            return entity.isFemale() ? NIGHT_GOLD_FEMALE_TEXTURE : NIGHT_GOLD_TEXTURE;
        }
        return super.getAdultTexture(entity);
    }

    @Override
    public void setCustomAnimations(Raevyx entity, long instanceId, AnimationState<Raevyx> animationState) {

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
            if (!entity.isVehicle() && !entity.isInWaterOrBubble()) {
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

    private void applyBodyRotationDeviation(Raevyx entity, float partialTick) {
        DragonModelPoseHelper.applyBodyYawDeviation(this, entity, "root", partialTick, -1.0f, true);
    }

    private void applyBankingRoll(Raevyx entity, AnimationState<Raevyx> state) {
        var bodyOpt = getBone("body");
        if (bodyOpt.isEmpty()) return;

        GeoBone body = bodyOpt.get();
        var snap = body.getInitialSnapshot();
        float partialTick = state.getPartialTick();
        float bankAngleDeg = entity.getBankAngleDegrees(partialTick);
        float bankAngleRad = Mth.clamp(-bankAngleDeg * Mth.DEG_TO_RAD, -Mth.HALF_PI, Mth.HALF_PI);
        float barrelRollRad = entity.getSmoothedRoll(partialTick);
        body.setRotZ(snap.getRotZ() + bankAngleRad + barrelRollRad);
    }

    private void applyFlightPitch(Raevyx entity, AnimationState<Raevyx> state) {
        var rootOpt = getBone("root");
        if (rootOpt.isEmpty()) return;

        GeoBone root = rootOpt.get();
        var snap = root.getInitialSnapshot();

        float partialTick = state.getPartialTick();
        float pitchRad = entity.getFlightPitchRadians(partialTick);
        pitchRad = Mth.clamp(pitchRad, -Mth.HALF_PI, Mth.HALF_PI);

        root.setRotX(snap.getRotX() + pitchRad);
    }

    private void applyNeckBankingLean(Raevyx entity, float partialTick) {
        if (!entity.isVehicle() || !entity.isFlying()) {
            return;
        }
        float bankAngleDeg = entity.getBankAngleDegrees(partialTick);
        float neckLeanRad = -(bankAngleDeg / 45.0f) * 35.0f * Mth.DEG_TO_RAD;
        DragonModelPoseHelper.applyWeightedRotationY(this, NECK_FOLLOW, neckLeanRad);
    }

    private void applyGroundNeckTurn(Raevyx entity, float partialTick) {
        if (entity.isFlying()) {
            return;
        }

        DragonModelPoseHelper.applyGroundNeckTurn(this, entity, partialTick, NECK_FOLLOW, 25.0);
    }

    private void applyNeckFollow(Raevyx entity, EntityModelData modelData, float partialTick) {

        float totalYawRad = DragonModelPoseHelper.lookYawWithBodyDeviation(entity, modelData, partialTick, 2.0);
        float lookPitchRad = modelData.headPitch() * Mth.DEG_TO_RAD;
        if (entity.isFlying()) {
            lookPitchRad *= 0.5f;
        }

        DragonModelPoseHelper.applyWeightedNeckFollow(this, NECK_TURN, lookPitchRad, totalYawRad);
    }

    private void applyTailDrag(Raevyx entity, float partialTick) {
        DragonModelPoseHelper.applyTailDrag(this, entity, partialTick, TAIL, 30.0);
    }
}
