package com.leon.saintsdragons.client.renderer.layer.ignivorus;

import com.leon.saintsdragons.client.renderer.layer.NightEmissiveLayer;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoRenderer;

public class IgnivorusNightEmissiveLayer extends NightEmissiveLayer<Ignivorus> {
    private static final ResourceLocation EMISSIVE_TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/ignivorus/ignivorus_emissive.png");
    private static final ResourceLocation CRIMSON_EMISSIVE_TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/ignivorus/crimson_ignivorus_emissive.png");
    private static final ResourceLocation CRIMSON_FEMALE_EMISSIVE_TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/ignivorus/crimson_ignivorus_female_emissive.png");

    public IgnivorusNightEmissiveLayer(GeoRenderer<Ignivorus> renderer) {
        super(renderer);
    }

    @Override
    protected ResourceLocation getEmissiveTexture(Ignivorus animatable) {
        if (animatable.getTextureVariant() == Ignivorus.VARIANT_CRIMSON) {
            return animatable.isFemale() ? CRIMSON_FEMALE_EMISSIVE_TEXTURE : CRIMSON_EMISSIVE_TEXTURE;
        }
        return EMISSIVE_TEXTURE;
    }
}
