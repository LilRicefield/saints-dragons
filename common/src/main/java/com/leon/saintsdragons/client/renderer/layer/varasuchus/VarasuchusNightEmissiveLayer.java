package com.leon.saintsdragons.client.renderer.layer.varasuchus;

import com.leon.saintsdragons.client.renderer.layer.NightEmissiveLayer;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoRenderer;

public class VarasuchusNightEmissiveLayer extends NightEmissiveLayer<Varasuchus> {
    private static final ResourceLocation EMISSIVE_TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/varasuchus/varasuchus_emissive.png");
    private static final ResourceLocation VOID_KISSED_EMISSIVE_TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/varasuchus/varasuchus_void_kissed_emissive.png");

    public VarasuchusNightEmissiveLayer(GeoRenderer<Varasuchus> renderer) {
        super(renderer);
    }

    @Override
    protected ResourceLocation getEmissiveTexture(Varasuchus animatable) {
        if (Varasuchus.VOID_KISSED_VARIANT_ID.equals(animatable.getTextureVariantId())) {
            return VOID_KISSED_EMISSIVE_TEXTURE;
        }
        return EMISSIVE_TEXTURE;
    }
}
