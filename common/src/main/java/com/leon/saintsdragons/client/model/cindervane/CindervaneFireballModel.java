package com.leon.saintsdragons.client.model.cindervane;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.client.renderer.vfx.BeamRenderTypes;
import com.leon.saintsdragons.server.entity.effect.cindervane.CindervaneFireballEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.model.GeoModel;

public class CindervaneFireballModel extends GeoModel<CindervaneFireballEntity> {
    private static final ResourceLocation MODEL = SaintsDragonsCommon.rl("geo/objects/shared/fireball/fireball_stage_1.geo.json");
    private static final ResourceLocation TEXTURE = SaintsDragonsCommon.rl("textures/objects/shared/fireball/fireball_stage_1.png");
    private static final ResourceLocation ANIMATION = SaintsDragonsCommon.rl("animations/objects/shared/fireball/fireball_stage_1.animation.json");

    @Override
    public ResourceLocation getModelResource(CindervaneFireballEntity entity) { return MODEL; }

    @Override
    public ResourceLocation getTextureResource(CindervaneFireballEntity entity) { return TEXTURE; }

    @Override
    public ResourceLocation getAnimationResource(CindervaneFireballEntity entity) { return ANIMATION; }

    @Override
    public RenderType getRenderType(CindervaneFireballEntity entity, ResourceLocation texture) {
        return BeamRenderTypes.translucent(texture);
    }
}
