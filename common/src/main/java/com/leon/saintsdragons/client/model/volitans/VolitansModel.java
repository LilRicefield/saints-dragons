package com.leon.saintsdragons.client.model.volitans;

import com.leon.saintsdragons.client.model.DragonGeoModel;
import com.leon.saintsdragons.client.model.DragonModelPoseHelper;
import com.leon.saintsdragons.client.model.DragonModelPoseHelper.WeightedBoneChain;
import com.leon.saintsdragons.client.ui.DraconicCodexScreen;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.data.EntityModelData;

public class VolitansModel extends DragonGeoModel<Volitans> {
    private static final WeightedBoneChain NECK = WeightedBoneChain.of(
            new String[] {"neck1Controller", "neck2Controller", "headController"},
            0.25f, 0.50f, 1.0f
    );
    private static final WeightedBoneChain TAIL = WeightedBoneChain.of(
            new String[] {"tail1", "tail2", "tail3", "tail4"},
            0.5f, 0.75f, 1.0f, 1.25f
    );
    private static final ResourceLocation BLOODSHOT_TEXTURE = SaintsDragonsCommon.rl("textures/entity/volitans/volitans_bloodshot.png");
    private static final ResourceLocation BLOODSHOT_FEMALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/volitans/volitans_bloodshot_female.png");

    public VolitansModel() {
        super("volitans");
    }

    @Override
    protected ResourceLocation getAdultTexture(Volitans entity) {
        if (entity.hasCustomTextureVariant()) {
            return super.getAdultTexture(entity);
        }
        boolean bloodshot = entity.getTextureVariant() == Volitans.VARIANT_BLOODSHOT;
        if (entity.isFemale()) {
            return bloodshot ? BLOODSHOT_FEMALE_TEXTURE : femaleTexture;
        }
        return bloodshot ? BLOODSHOT_TEXTURE : maleTexture;
    }

    @Override
    public void setCustomAnimations(Volitans entity, long instanceId, AnimationState<Volitans> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        if (DraconicCodexScreen.RENDERING_IN_GUI.get()) {
            return;
        }

        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (modelData == null) return;
        if (entity.isAlive()){
            if (entity.isDeadOrDying()){
                return;
            }
            if (!entity.isVehicle() && !entity.isInWaterOrBubble()) {
                applyNeckFollow(entity, modelData, animationState.getPartialTick());
            }
            applyBodyRotationDeviation(entity, animationState.getPartialTick());
            applyBankingRoll(entity, animationState);
            applyFlightPitch(entity, animationState);
            applyNeckBankingLean(entity, animationState.getPartialTick());
            applyGroundNeckTurn(entity, animationState.getPartialTick());
            applySwimPitch(entity, animationState.getPartialTick());
            applySwimRoll(entity, animationState.getPartialTick());
            applyTailDrag(entity, animationState.getPartialTick());

        }
    }

    private void applyBodyRotationDeviation(Volitans entity, float partialTick) {
        DragonModelPoseHelper.applyBodyYawDeviation(this, entity, "root", partialTick, -1.0f, true);
    }

    private void applyBankingRoll(Volitans entity, AnimationState<Volitans> state) {
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

    private void applyFlightPitch(Volitans entity, AnimationState<Volitans> state) {
        var rootOpt = getBone("root");
        if (rootOpt.isEmpty()) {
            return;
        }

        GeoBone root = rootOpt.get();
        var snap = root.getInitialSnapshot();

        float partialTick = state.getPartialTick();
        float pitchRad = entity.getFlightPitchRadians(partialTick);
        pitchRad = Mth.clamp(pitchRad, -Mth.HALF_PI, Mth.HALF_PI);

        root.setRotX(snap.getRotX() - pitchRad);
    }

    private void applyNeckFollow(Volitans entity, EntityModelData modelData, float partialTick) {
        float totalYawRad = DragonModelPoseHelper.lookYawWithBodyDeviation(entity, modelData, partialTick, 2.0);
        float lookPitchRad = modelData.headPitch() * Mth.DEG_TO_RAD;
        if (entity.isFlying()) {
            lookPitchRad *= 0.5f;
        }

        DragonModelPoseHelper.applyWeightedNeckFollow(this, NECK, lookPitchRad, totalYawRad);
    }

    private void applyNeckBankingLean(Volitans entity, float partialTick) {
        if (!entity.isVehicle() || !entity.isFlying()) {
            return;
        }
        float bankAngleDeg = entity.getBankAngleDegrees(partialTick);
        float neckLeanRad = -(bankAngleDeg / 45.0f) * 30.0f * Mth.DEG_TO_RAD;

        DragonModelPoseHelper.applyWeightedRotationY(this, NECK, neckLeanRad);
    }

    private void applyGroundNeckTurn(Volitans entity, float partialTick) {
        if (entity.isFlying()) {
            return;
        }

        DragonModelPoseHelper.applyGroundNeckTurn(this, entity, partialTick, NECK, 25.0);
    }

    private void applySwimPitch(Volitans entity, float partialTick) {
        if (!entity.isInWaterOrBubble() || !entity.isFlying()) {
            return;
        }

        var bodyOpt = getBone("heightController");
        if (bodyOpt.isEmpty()) {
            return;
        }

        GeoBone body = bodyOpt.get();
        float swimPitchRad = Mth.clamp(entity.getFlightPitchRadians(partialTick), -Mth.HALF_PI, Mth.HALF_PI);
        body.setRotX(body.getRotX() - swimPitchRad * 0.75f);
    }

    private void applySwimRoll(Volitans entity, float partialTick) {
        if (!entity.isInWaterOrBubble() || !entity.isFlying()) {
            return;
        }

        var bodyOpt = getBone("heightController");
        if (bodyOpt.isEmpty()) {
            return;
        }

        GeoBone body = bodyOpt.get();
        double velocity = entity.getYawVelocity().get(partialTick);
        velocity = Mth.clamp(velocity, -30.0, 30.0);
        float swimRollRad = (float) velocity * Mth.DEG_TO_RAD * 0.35f;
        body.setRotZ(body.getRotZ() + swimRollRad);
    }

    private void applyTailDrag(Volitans entity, float partialTick) {
        DragonModelPoseHelper.applyTailDrag(this, entity, partialTick, TAIL, 30.0);
    }
}
