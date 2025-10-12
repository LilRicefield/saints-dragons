package com.leon.saintsdragons.client.renderer.raevyx;

import com.leon.saintsdragons.server.entity.effect.raevyx.RaevyxLightningChainEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

/**
 * Renderer for LightningChainEntity.
 * Since this entity is purely visual and manages its own particles,
 * we don't need to render anything here - just prevent the null renderer crash.
 */
@OnlyIn(Dist.CLIENT)
public class RaevyxLightningChainRenderer extends EntityRenderer<RaevyxLightningChainEntity> {

    public RaevyxLightningChainRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull RaevyxLightningChainEntity entity) {
        // Return a dummy texture since we don't actually render the entity
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/lightning_bolt.png");
    }
}
