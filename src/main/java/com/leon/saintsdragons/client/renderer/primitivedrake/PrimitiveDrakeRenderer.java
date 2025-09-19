package com.leon.saintsdragons.client.renderer.primitivedrake;

import com.leon.saintsdragons.client.model.primitivedrake.PrimitiveDrakeModel;
import com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;

@OnlyIn(Dist.CLIENT)
public class PrimitiveDrakeRenderer extends GeoEntityRenderer<PrimitiveDrakeEntity> {
    
    public PrimitiveDrakeRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new PrimitiveDrakeModel());
    }

    @Override
    public RenderType getRenderType(PrimitiveDrakeEntity animatable, ResourceLocation texture, 
                                  @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public void render(@NotNull PrimitiveDrakeEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        // Scale down the drake to make it smaller and cuter
        poseStack.scale(0.8f, 0.8f, 0.8f);
        
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull PrimitiveDrakeEntity entity) {
        return  ResourceLocation.fromNamespaceAndPath("saintsdragons", "textures/entity/primitivedrake/primitivedrake.png");
    }
}
