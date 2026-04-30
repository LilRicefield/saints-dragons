package com.leon.saintsdragons.client.model.npc;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.npc.IvyTheDragonMerchant;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class IvyTheDragonMerchantModel extends DefaultedEntityGeoModel<IvyTheDragonMerchant> {
    public IvyTheDragonMerchantModel() {
        super(SaintsDragonsCommon.rl("ivy_oleander"));
    }

    @Override
    public void setCustomAnimations(IvyTheDragonMerchant entity, long instanceId, AnimationState<IvyTheDragonMerchant> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (modelData == null) {
            return;
        }

        float headYawRad = Mth.clamp(modelData.netHeadYaw(), -45.0f, 45.0f) * Mth.DEG_TO_RAD;
        float headPitchRad = Mth.clamp(modelData.headPitch(), -25.0f, 25.0f) * Mth.DEG_TO_RAD;
        float deviationRad = (float) (entity.bodyRotDeviation.get(animationState.getPartialTick()) * Mth.DEG_TO_RAD);

        GeoBone head = getBoneOrNull("head");
        if (head != null && entity.shouldApplyHeadTracking()) {
            head.setRotY(head.getRotY() + headYawRad);
            head.setRotX(head.getRotX() + headPitchRad);
        }

        GeoBone body = getBoneOrNull("waist");
        if (body == null) {
            body = getBoneOrNull("body");
        }
        if (body != null) {
            body.setRotY(body.getRotY() - deviationRad);
        }
    }

    private GeoBone getBoneOrNull(String name) {
        var bone = getBone(name);
        return bone.isPresent() ? bone.get() : null;
    }
}