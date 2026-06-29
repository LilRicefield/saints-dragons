package com.leon.saintsdragons.client.renderer.layer.stegonaut;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class StegonautChestLayer extends GeoRenderLayer<Stegonaut> {
    private static final ResourceLocation CHEST_TEXTURE =
            SaintsDragonsCommon.rl("textures/entity/stegonaut/stegonaut_chest_layer.png");

    public StegonautChestLayer(GeoRenderer<Stegonaut> renderer) {
        super(renderer);
    }

    @Override
    public void render(@NotNull PoseStack poseStack,
                       Stegonaut animatable,
                       BakedGeoModel bakedModel,
                       @NotNull RenderType renderType,
                       @NotNull MultiBufferSource bufferSource,
                       @NotNull VertexConsumer buffer,
                       float partialTick,
                       int packedLight,
                       int packedOverlay) {
        if (!animatable.hasStegonautChest()) {
            return;
        }

        RenderType chestRenderType = RenderType.entityCutoutNoCull(CHEST_TEXTURE);
        VertexConsumer chestBuffer = bufferSource.getBuffer(chestRenderType);
        getRenderer().reRender(
                bakedModel,
                poseStack,
                bufferSource,
                animatable,
                chestRenderType,
                chestBuffer,
                partialTick,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                1.0F,
                1.0F,
                1.0F,
                1.0F
        );
    }
}
