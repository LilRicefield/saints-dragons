package com.leon.saintsdragons.fabric.mixin.fabric;

import com.leon.saintsdragons.fabric.entity.part.FabricDragonPart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes FabricDragonPart entities render as green hitboxes in debug mode (F3+B),
 * matching the behavior of Forge's PartEntity.
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Inject(
            method = "renderHitbox",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void renderPartEntityAsGreen(
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            Entity entity,
            float partialTicks,
            CallbackInfo ci
    ) {
        if (entity instanceof FabricDragonPart) {
            // Render as green hitbox like Forge's PartEntity
            AABB aabb = entity.getBoundingBox().move(-entity.getX(), -entity.getY(), -entity.getZ());
            LevelRenderer.renderLineBox(poseStack, vertexConsumer, aabb, 0.0F, 1.0F, 0.0F, 1.0F);
            ci.cancel();
        }
    }
}
