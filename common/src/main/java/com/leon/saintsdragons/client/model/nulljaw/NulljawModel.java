package com.leon.saintsdragons.client.model.nulljaw;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public final class NulljawModel extends DefaultedEntityGeoModel<Nulljaw> {
    private static final ResourceLocation MODEL = SaintsDragonsCommon.rl("geo/entity/nulljaw.geo.json");
    private static final ResourceLocation ANIM = SaintsDragonsCommon.rl("animations/entity/nulljaw.animation.json");
    private static final ResourceLocation TEXTURE = SaintsDragonsCommon.rl("textures/entity/nulljaw/nulljaw.png");
    private static final ResourceLocation FEMALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/nulljaw/nulljaw_female.png");

    public NulljawModel() {
        super(SaintsDragonsCommon.rl("nulljaw"));
    }

    @Override
    public ResourceLocation getModelResource(Nulljaw animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(Nulljaw animatable) {
        return animatable.isFemale() ? FEMALE_TEXTURE : TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(Nulljaw animatable) {
        return ANIM;
    }
}
