package com.leon.saintsdragons.client.model.draconianswarm;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.draconianswarm.Winged;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class WingedModel extends GeoModel<Winged> {
    private static final ResourceLocation MODEL = SaintsDragonsCommon.rl("geo/entity/winged.geo.json");
    private static final ResourceLocation TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/draconian_swarm/winged/winged.png");
    private static final ResourceLocation ANIMATIONS =
            SaintsDragonsCommon.rl("animations/entity/winged.animation.json");

    @Override
    public ResourceLocation getModelResource(Winged animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(Winged animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(Winged animatable) {
        return ANIMATIONS;
    }

    @Override
    public void setCustomAnimations(Winged entity, long instanceId, AnimationState<Winged> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);
        if (!entity.isAlive()) {
            return;
        }

        float pitch = Mth.clamp(entity.getFlightPitchRadians(animationState.getPartialTick()), -0.95F, 0.95F);
        getBone("root").ifPresent(bone -> bone.setRotX(bone.getInitialSnapshot().getRotX() + pitch));

        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (modelData != null) {
            float lookPitch = Mth.clamp(modelData.headPitch() * Mth.DEG_TO_RAD, -0.55F, 0.55F);
            float lookYaw = Mth.clamp(modelData.netHeadYaw() * Mth.DEG_TO_RAD, -0.75F, 0.75F);
            getBone("head").ifPresent(bone -> {
                bone.setRotX(bone.getRotX() + lookPitch);
                bone.setRotY(bone.getRotY() + lookYaw);
            });
        }
    }
}
