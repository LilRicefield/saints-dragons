package com.leon.saintsdragons.client.model.mossback;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.Mossback;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class MossbackModel extends DefaultedEntityGeoModel<Mossback> {
    private static final ResourceLocation ADULT_MODEL = SaintsDragonsCommon.rl("geo/entity/mossback.geo.json");
    private static final ResourceLocation BABY_MODEL = SaintsDragonsCommon.rl("geo/entity/baby_mossback.geo.json");
    private static final ResourceLocation ADULT_TEXTURE = SaintsDragonsCommon.rl("textures/entity/mossback/mossback.png");
    private static final ResourceLocation BABY_TEXTURE = SaintsDragonsCommon.rl("textures/entity/mossback/baby_mossback.png");
    private static final ResourceLocation ADULT_ANIMATION = SaintsDragonsCommon.rl("animations/entity/mossback.animation.json");
    private static final ResourceLocation BABY_ANIMATION = SaintsDragonsCommon.rl("animations/entity/baby_mossback.animation.json");

    public MossbackModel() {
        super(SaintsDragonsCommon.rl("mossback"));
    }

    @Override
    public ResourceLocation getModelResource(Mossback entity) {
        return entity != null && entity.isBaby() ? BABY_MODEL : ADULT_MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(Mossback entity) {
        return entity != null && entity.isBaby() ? BABY_TEXTURE : ADULT_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(Mossback entity) {
        return entity != null && entity.isBaby() ? BABY_ANIMATION : ADULT_ANIMATION;
    }
}
