package com.leon.saintsdragons.client.model.draconianswarm;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.draconianswarm.Whettled;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class WhettledModel extends GeoModel<Whettled> {
    private static final ResourceLocation MODEL = SaintsDragonsCommon.rl("geo/entity/whettled.geo.json");
    private static final ResourceLocation TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/draconianswarm/whettled/whettled.png");
    private static final ResourceLocation ANIMATIONS =
            SaintsDragonsCommon.rl("animations/entity/whettled.animation.json");

    @Override
    public ResourceLocation getModelResource(Whettled animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(Whettled animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(Whettled animatable) {
        return ANIMATIONS;
    }

    @Override
    public void setCustomAnimations(Whettled entity, long instanceId, AnimationState<Whettled> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);
        if (!entity.isAlive()) {
            return;
        }
        float partialTick = animationState.getPartialTick();
        float pitch = Mth.clamp(entity.getFlightPitchRadians(partialTick), -0.95F, 0.95F);
        float drag = Mth.clamp(entity.getTailDragYawRadians(partialTick), -0.95F, 0.95F);
        getBone("root").ifPresent(bone -> bone.setRotX(bone.getInitialSnapshot().getRotX() + pitch));
        applyTailDrag("tail1rot", drag * 0.70F);
        applyTailDrag("tail2rot", drag * 1.00F);
        applyTailDrag("tail3rot", drag * 1.30F);
    }

    private void applyTailDrag(String boneName, float yaw) {
        getBone(boneName).ifPresent(bone -> bone.setRotY(bone.getInitialSnapshot().getRotY() + yaw));
    }
}
