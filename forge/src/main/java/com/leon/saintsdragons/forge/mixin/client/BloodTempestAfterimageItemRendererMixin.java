package com.leon.saintsdragons.forge.mixin.client;

import com.leon.saintsdragons.client.renderer.vfx.BloodTempestAfterimageRenderContext;
import com.leon.saintsdragons.client.renderer.vfx.BloodTempestAfterimageVertexConsumer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(ItemRenderer.class)
public abstract class BloodTempestAfterimageItemRendererMixin {
    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/model/BakedModel;getRenderTypes(Lnet/minecraft/world/item/ItemStack;Z)Ljava/util/List;"
            ),
            require = 0
    )
    private List<RenderType> saintsdragons$useAfterimageRenderType(BakedModel model, ItemStack stack,
                                                                   boolean fabulous) {
        return BloodTempestAfterimageRenderContext.isActive()
                ? List.of(Sheets.translucentItemSheet())
                : model.getRenderTypes(stack, fabulous);
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;hasFoil()Z"
            ),
            require = 0
    )
    private boolean saintsdragons$hideAfterimageFoil(ItemStack stack) {
        return !BloodTempestAfterimageRenderContext.isActive() && stack.hasFoil();
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;getFoilBuffer(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            ),
            require = 0
    )
    private VertexConsumer saintsdragons$fadeAfterimageItem(MultiBufferSource buffers, RenderType renderType,
                                                            boolean direct, boolean foil) {
        return BloodTempestAfterimageVertexConsumer.wrap(
                ItemRenderer.getFoilBuffer(buffers, renderType, direct, foil));
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;getFoilBufferDirect(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            ),
            require = 0
    )
    private VertexConsumer saintsdragons$fadeAfterimageItemDirect(MultiBufferSource buffers, RenderType renderType,
                                                                  boolean direct, boolean foil) {
        return BloodTempestAfterimageVertexConsumer.wrap(
                ItemRenderer.getFoilBufferDirect(buffers, renderType, direct, foil));
    }
}
