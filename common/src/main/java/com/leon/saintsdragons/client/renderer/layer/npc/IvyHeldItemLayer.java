package com.leon.saintsdragons.client.renderer.layer.npc;

import com.leon.saintsdragons.server.entity.npc.IvyTheDragonMerchant;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class IvyHeldItemLayer extends BlockAndItemGeoLayer<IvyTheDragonMerchant> {
    private static final String RIGHT_HAND_LOCATOR = "rightArmItemLocator";

    public IvyHeldItemLayer(GeoRenderer<IvyTheDragonMerchant> renderer) {
        super(renderer);
    }

    @Nullable
    @Override
    protected ItemStack getStackForBone(GeoBone bone, IvyTheDragonMerchant animatable) {
        if (!RIGHT_HAND_LOCATOR.equals(bone.getName())) {
            return null;
        }
        ItemStack stack = animatable.getRecoveryItemForRender();
        if (!stack.isEmpty()) {
            return stack;
        }
        ItemStack sword = animatable.getSwordForRender();
        return sword.isEmpty() ? null : sword;
    }

    @Override
    protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, IvyTheDragonMerchant animatable) {
        return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    @Override
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, IvyTheDragonMerchant animatable,
                                      MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
        poseStack.translate(0.0D, -0.0625D, -0.1D);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
    }
}
