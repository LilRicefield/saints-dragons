package com.leon.saintsdragons.client.renderer.volitans;

import com.leon.saintsdragons.client.model.volitans.VolitansSpineModel;
import com.leon.saintsdragons.server.entity.effect.volitans.VolitansSpineEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class VolitansSpineRenderer extends GeoEntityRenderer<VolitansSpineEntity> {
    public VolitansSpineRenderer(EntityRendererProvider.Context context) {
        super(context, new VolitansSpineModel());
        this.shadowRadius = 0.15F;
    }

    @Override
    public void render(@NotNull VolitansSpineEntity entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entity.getYRot(), partialTick, poseStack, bufferSource, packedLight);
    }
}
