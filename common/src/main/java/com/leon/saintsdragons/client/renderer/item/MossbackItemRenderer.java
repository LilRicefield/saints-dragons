package com.leon.saintsdragons.client.renderer.item;

import com.leon.saintsdragons.client.model.item.MossbackItemModel;
import com.leon.saintsdragons.common.item.MossbackItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class MossbackItemRenderer extends GeoItemRenderer<MossbackItem> {
    float s = 1.15f;

    public MossbackItemRenderer() {
        super(new MossbackItemModel());
        useAlternateGuiLighting();
    }
    @Override
    public void preRender(PoseStack poseStack, MossbackItem animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        if (!isReRender && this.renderPerspective == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(135.0F));
            poseStack.scale(s, s, s);
            poseStack.translate(0, -0.15, 0);
        }
    }
}
