package com.leon.saintsdragons.client.renderer.primitivedrake;

import com.leon.saintsdragons.client.model.primitivedrake.PrimitiveDrakeModel;
import com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;

@OnlyIn(Dist.CLIENT)
public class PrimitiveDrakeRenderer extends GeoEntityRenderer<PrimitiveDrakeEntity> {
    private BakedGeoModel lastBakedModel;
    
    // Mouth locator offset from head bone (adjust these values based on your model)
    private static final float MOUTH_X = 0.0f;
    private static final float MOUTH_Y = -0.2f; // Slightly below head center
    private static final float MOUTH_Z = 0.8f;  // Forward from head center
    
    public PrimitiveDrakeRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new PrimitiveDrakeModel());
    }
    @Override
    public void preRender(PoseStack poseStack,
                          PrimitiveDrakeEntity entity,
                          BakedGeoModel model,
                          MultiBufferSource bufferSource,
                          VertexConsumer buffer,
                          boolean isReRender,
                          float partialTick,
                          int packedLight,
                          int packedOverlay,
                          float red, float green, float blue, float alpha) {

        float scale = 1.5f;
        poseStack.scale(scale, scale, scale);
        this.shadowRadius = 0.8f * scale;

        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
    
    @Override
    public void render(PrimitiveDrakeEntity entity, float entityYaw, float partialTick, 
                      PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // Store the current model for locator sampling
        this.lastBakedModel = this.getGeoModel().getBakedModel(this.getGeoModel().getModelResource(entity));
        
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        
        // After bones have been processed, sample accurate world positions for mouth locator
        sampleAndStashLocatorsAccurate(entity);
    }
    
    private void sampleAndStashLocatorsAccurate(PrimitiveDrakeEntity entity) {
        if (this.lastBakedModel == null || entity == null) return;
        
        // Sample mouth origin from head bone
        this.lastBakedModel.getBone("head").ifPresent(b -> {
            net.minecraft.world.phys.Vec3 world = transformLocator(b, MOUTH_X, MOUTH_Y, MOUTH_Z);
            if (world != null) entity.setClientLocatorPosition("mouth_origin", world);
        });
    }
    
    private static net.minecraft.world.phys.Vec3 transformLocator(software.bernie.geckolib.cache.object.GeoBone bone,
                                                                  float px, float py, float pz) {
        if (bone == null) return null;
        
        // Convert pixels to model units (blocks)
        float lx = px / 16f;
        float ly = py / 16f;
        float lz = pz / 16f;
        
        // Transform using bone's world matrix
        org.joml.Matrix4f worldMat = new org.joml.Matrix4f(bone.getWorldSpaceMatrix());
        org.joml.Vector4f in = new org.joml.Vector4f(lx, ly, lz, 1f);
        org.joml.Vector4f out = worldMat.transform(in);
        
        return new net.minecraft.world.phys.Vec3(out.x(), out.y(), out.z());
    }
}
