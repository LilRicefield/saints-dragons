package com.leon.saintsdragons.client.model.ignivorus;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.client.renderer.vfx.BeamRenderTypes;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusFireballEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.model.GeoModel;

public class IgnivorusFireballModel extends GeoModel<IgnivorusFireballEntity> {
    private static final ResourceLocation MODEL = SaintsDragonsCommon.rl("geo/blocks/fireball_stage_1.geo.json");
    private static final ResourceLocation TEXTURE = SaintsDragonsCommon.rl("textures/blocks/fireball_stage_1.png");
    private static final ResourceLocation ANIMATION = SaintsDragonsCommon.rl("animations/blocks/fireball_stage_1.animation.json");

    @Override
    public ResourceLocation getModelResource(IgnivorusFireballEntity entity) { return MODEL; }

    @Override
    public ResourceLocation getTextureResource(IgnivorusFireballEntity entity) { return TEXTURE; }

    @Override
    public ResourceLocation getAnimationResource(IgnivorusFireballEntity entity) { return ANIMATION; }

    @Override
    public RenderType getRenderType(IgnivorusFireballEntity entity, ResourceLocation texture) {
        return BeamRenderTypes.translucent(texture);
    }
}
