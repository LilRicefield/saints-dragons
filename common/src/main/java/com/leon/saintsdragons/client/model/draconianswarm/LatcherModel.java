package com.leon.saintsdragons.client.model.draconianswarm;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.draconianswarm.Latcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class LatcherModel extends GeoModel<Latcher> {
    private static final ResourceLocation MODEL =
            SaintsDragonsCommon.rl("geo/entity/latcher.geo.json");
    private static final ResourceLocation TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/draconian_swarm/latcher/latcher.png");
    private static final ResourceLocation ANIMATIONS =
            SaintsDragonsCommon.rl("animations/entity/latcher.animation.json");

    @Override
    public ResourceLocation getModelResource(Latcher animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(Latcher animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(Latcher animatable) {
        return ANIMATIONS;
    }

    @Override
    public void setCustomAnimations(Latcher entity, long instanceId, AnimationState<Latcher> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        if (!entity.isAlive()) {
            return;
        }

        float partialTick = animationState.getPartialTick();
        float pitchRad = Mth.clamp(entity.getFlightPitchRadians(partialTick), -0.95F, 0.95F);
        float speed = Mth.clamp((float) entity.getDeltaMovement().length(), 0.0F, 0.7F);
        float dragYaw = Mth.clamp(entity.getTailDragYawRadians(partialTick), -0.95F, 0.95F);
        float swayYaw = Mth.sin((entity.tickCount + partialTick) * 0.18F) * speed * 0.12F;
        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

        applyFlightPitch(pitchRad);
        applyLook(modelData);
        applyTailDrag("secondrot", dragYaw * 0.85F + swayYaw);
        applyTailDrag("thirdrot", dragYaw * 1.25F + swayYaw * 1.25F);
        applyTailDrag("forthrot", dragYaw * 1.35F + swayYaw * 1.35F);
    }

    private void applyFlightPitch(float pitchRad) {
        getBone("root").ifPresent(bone -> {
            var snapshot = bone.getInitialSnapshot();
            bone.setRotX(snapshot.getRotX() + pitchRad);
        });
    }

    private void applyLook(EntityModelData modelData) {
        if (modelData == null) {
            return;
        }

        float lookPitchRad = Mth.clamp(modelData.headPitch() * Mth.DEG_TO_RAD, -0.65F, 0.65F);
        float lookYawRad = Mth.clamp(modelData.netHeadYaw() * Mth.DEG_TO_RAD, -0.85F, 0.85F);
        applyLookRotation("neck", lookPitchRad * 0.35F, lookYawRad * 0.40F);
        applyLookRotation("head", lookPitchRad * 0.65F, lookYawRad * 0.70F);
    }

    private void applyLookRotation(String boneName, float pitchRad, float yawRad) {
        getBone(boneName).ifPresent(bone -> {
            bone.setRotX(bone.getRotX() + pitchRad);
            bone.setRotY(bone.getRotY() + yawRad);
        });
    }

    private void applyTailDrag(String boneName, float yawRad) {
        getBone(boneName).ifPresent(bone -> {
            var snapshot = bone.getInitialSnapshot();
            bone.setRotY(snapshot.getRotY() + yawRad);
        });
    }
}
