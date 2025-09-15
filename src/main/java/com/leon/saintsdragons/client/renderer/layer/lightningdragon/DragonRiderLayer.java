package com.leon.saintsdragons.client.renderer.layer.lightningdragon;

import com.leon.saintsdragons.client.model.tools.ModelPartMatrix;
import com.leon.saintsdragons.client.render.SaintRenderUtils;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Renders the player on the dragon's neck, following the neck4Controller bone animations.
 * Based on Mowzie's Mobs GeckoParrotOnShoulderLayer approach.
 */
public class DragonRiderLayer extends GeoRenderLayer<LightningDragonEntity> {
    private final EntityRenderDispatcher entityRenderDispatcher;

    public DragonRiderLayer(EntityRenderDispatcher entityRenderDispatcher) {
        super(null);
        this.entityRenderDispatcher = entityRenderDispatcher;
    }

    @Override
    public void render(@NotNull PoseStack poseStack, LightningDragonEntity dragon, BakedGeoModel bakedModel,
                       @NotNull net.minecraft.client.renderer.RenderType renderType, @NotNull MultiBufferSource bufferSource, @NotNull com.mojang.blaze3d.vertex.VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {

        if (!dragon.isVehicle()) {
            return;
        }

        // Get the neck4Controller bone
        var neckBone = bakedModel.getBone("neck4Controller");
        if (neckBone.isEmpty()) {
            return;
        }

        // Create a ModelPartMatrix to track the bone's world position
        ModelPartMatrix neckMatrix = new ModelPartMatrix();
        neckMatrix.setName("neck4Controller");
        
        // Calculate world matrix for the neck bone
        calculateBoneWorldMatrix(neckBone.get(), dragon, partialTick, neckMatrix);

        // Render each passenger
        for (Entity passenger : dragon.getPassengers()) {
            if (passenger instanceof Player player) {
                // Skip first-person view for the main player
                if (passenger == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
                    continue;
                }

                poseStack.pushPose();
                
                // Scale down the player FIRST (before dragon transformations) - try bigger for testing
                poseStack.scale(100.0f, 100.0f, 100.0f);
                
                // Transform pose stack to follow the neck bone
                SaintRenderUtils.transformStackToModelPart(poseStack, neckMatrix);
                
                // Apply player positioning adjustments
                poseStack.translate(0, 0.5F, 0.35F); // Position on neck
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(10F)); // Slight forward lean
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180F)); // Face forward
                
                // Render the player
                renderPassenger(player, poseStack, bufferSource, packedLight, partialTick);
                
                // Pop the pose that transformStackToModelPart added
                poseStack.popPose();
                // Pop our original pose
                poseStack.popPose();
            }
        }
    }

    private void calculateBoneWorldMatrix(software.bernie.geckolib.cache.object.GeoBone bone, LightningDragonEntity dragon, float partialTick, ModelPartMatrix matrixPart) {
        // Create pose stack to calculate world matrix
        PoseStack matrixStack = new PoseStack();
        
        // Apply entity transformations
        matrixStack.translate(dragon.getX(), dragon.getY(), dragon.getZ());
        matrixStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-dragon.yBodyRot + 180));
        matrixStack.scale(-1, -1, 1);
        matrixStack.translate(0, -1.5f, 0);
        
        // Apply dragon scale
        matrixStack.scale(4.5f, 4.5f, 4.5f);
        
        // Apply bone hierarchy transformations
        SaintRenderUtils.matrixStackFromModel(matrixStack, bone);
        
        // Extract world matrix
        PoseStack.Pose matrixEntry = matrixStack.last();
        matrixPart.setWorldXform(new org.joml.Matrix4f(matrixEntry.pose()));
        matrixPart.setWorldNormal(new org.joml.Matrix3f(matrixEntry.normal()));
    }

    private void renderPassenger(Player player, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float partialTick) {
        // Get the player's renderer
        var playerRenderer = entityRenderDispatcher.getRenderer(player);
        if (playerRenderer == null) {
            return;
        }

        // Render the player
        playerRenderer.render(player, player.getYRot(), partialTick, poseStack, bufferSource, packedLight);
    }
}
