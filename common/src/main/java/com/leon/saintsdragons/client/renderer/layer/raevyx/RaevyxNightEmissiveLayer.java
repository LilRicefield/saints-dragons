package com.leon.saintsdragons.client.renderer.layer.raevyx;

import com.leon.saintsdragons.client.renderer.layer.NightEmissiveLayer;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoRenderer;

public class RaevyxNightEmissiveLayer extends NightEmissiveLayer<Raevyx> {
    private static final ResourceLocation EMISSIVE_TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/raevyx/raevyx_emissive.png");
    private static final ResourceLocation NIGHT_GOLD_EMISSIVE_TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/raevyx/raevyx_night_gold_emissive.png");

    public RaevyxNightEmissiveLayer(GeoRenderer<Raevyx> renderer) {
        super(renderer);
    }

    @Override
    @Nullable
    protected ResourceLocation getEmissiveTexture(Raevyx animatable) {
        if (animatable.getTextureVariant() == Raevyx.VARIANT_NIGHT_GOLD) {
            return NIGHT_GOLD_EMISSIVE_TEXTURE;
        }
        return EMISSIVE_TEXTURE;
    }
}
