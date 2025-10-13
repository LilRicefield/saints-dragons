package com.leon.saintsdragons.client.model.nulljaw;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class NulljawModel extends DefaultedEntityGeoModel<Nulljaw> {

    public NulljawModel() {
        super(SaintsDragons.rl("nulljaw"), "head");
    }

    @Override
    public void setCustomAnimations(Nulljaw animatable, long instanceId, AnimationState<Nulljaw> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
    }
}
