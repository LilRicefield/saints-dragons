package com.leon.saintsdragons.client.renderer.layer.raevyx;

import com.leon.saintsdragons.client.event.ClientEventHandler;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * GeckoLib render layer that attaches the primary rider to the Raevyx passenger bone.
 */
public class RaevyxRiderLayer extends GeoRenderLayer<Raevyx> {
    private static final String SEAT_BONE = "passengerBone";

    public RaevyxRiderLayer() {
        super(null);
    }

    @Override
    public void render(@NotNull PoseStack poseStack,
                       Raevyx animatable,
                       BakedGeoModel bakedModel,
                       @NotNull RenderType renderType,
                       @NotNull MultiBufferSource bufferSource,
                       @NotNull VertexConsumer buffer,
                       float partialTick,
                       int packedLight,
                       int packedOverlay) {

        if (animatable == null || animatable.isBaby() || !animatable.isVehicle()) {
            return;
        }

        Entity passenger = animatable.getFirstPassenger();
        if (passenger == null) {
            return;
        }

        GeoBone seatBone = bakedModel.getBone(SEAT_BONE).orElse(null);
        if (seatBone == null) {
            return;
        }

        // Get the seat bone's world-space transform
        Matrix4f seatMatrix = new Matrix4f(seatBone.getWorldSpaceMatrix());
        Vector3f seatWorld = seatMatrix.transformPosition(new Vector3f(0.0F, 0.0F, 0.0F));

        // Extract rotation from seat bone matrix
        Vector3f forward = seatMatrix.transformDirection(new Vector3f(0.0F, 0.0F, 1.0F)).normalize();
        Vector3f up = seatMatrix.transformDirection(new Vector3f(0.0F, 1.0F, 0.0F)).normalize();
        Vector3f right = up.cross(forward, new Vector3f()).normalize();
        up = forward.cross(right, new Vector3f()).normalize();

        Matrix3f basis = new Matrix3f(
                right.x(), up.x(), forward.x(),
                right.y(), up.y(), forward.y(),
                right.z(), up.z(), forward.z()
        );
        Quaternionf seatRotation = basis.getNormalizedRotation(new Quaternionf());

        // Get interpolated dragon position for smooth rendering
        double lerpX = Mth.lerp(partialTick, animatable.xo, animatable.getX());
        double lerpY = Mth.lerp(partialTick, animatable.yo, animatable.getY());
        double lerpZ = Mth.lerp(partialTick, animatable.zo, animatable.getZ());

        // Save and restore pose stack state
        poseStack.pushPose();

        // Translate to the seat bone's world position relative to dragon's render position
        poseStack.translate(seatWorld.x(), seatWorld.y(), seatWorld.z());

        // Apply seat bone rotation
        poseStack.mulPose(seatRotation);

        // Rotate rider to face forward (dragons modeled facing +Z, players face -Z)
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        // Adjust for rider's mount offset
        poseStack.translate(0.0D, -passenger.getMyRidingOffset(), 0.0D);

        // Translate back to world origin for entity renderer
        // The entity renderer expects to render at the entity's world position
        poseStack.translate(-lerpX, -lerpY, -lerpZ);

        // Render the passenger with our transformed PoseStack
        renderPassenger(passenger, poseStack, bufferSource, packedLight, partialTick);

        poseStack.popPose();
    }

    private void renderPassenger(Entity passenger,
                                 PoseStack poseStack,
                                 MultiBufferSource bufferSource,
                                 int packedLight,
                                 float partialTicks) {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderer<? super Entity> renderer = dispatcher.getRenderer(passenger);

        // Allow rendering through the suppression check
        ClientEventHandler.allowRaevyxPassengerRender = true;
        try {
            // Render passenger at position (0,0,0) since we've already transformed the PoseStack
            renderer.render(passenger, 0.0F, partialTicks, poseStack, bufferSource, packedLight);
        } finally {
            ClientEventHandler.allowRaevyxPassengerRender = false;
        }
    }
}
