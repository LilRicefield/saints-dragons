package com.leon.saintsdragons.client.model.volitans;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.effect.volitans.VolitansPoisonOrbEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class VolitansPoisonOrbModel extends GeoModel<VolitansPoisonOrbEntity> {
    @Override
    public ResourceLocation getModelResource(VolitansPoisonOrbEntity entity) {
        return SaintsDragonsCommon.rl("geo/objects/volitans/volitans_poison_orb.geo.json");
    }
    @Override
    public ResourceLocation getTextureResource(VolitansPoisonOrbEntity entity) {
        return SaintsDragonsCommon.rl("textures/objects/volitans/volitans_poison_orb.png");
    }
    @Override
    public ResourceLocation getAnimationResource(VolitansPoisonOrbEntity entity) {
        return SaintsDragonsCommon.rl("animations/objects/volitans/volitans_poison_orb.animation.json");
    }
}
