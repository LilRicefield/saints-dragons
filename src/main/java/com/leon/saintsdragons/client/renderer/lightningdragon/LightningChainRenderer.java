package com.leon.saintsdragons.client.renderer.lightningdragon;

import com.leon.saintsdragons.server.entity.effect.lightningdragon.LightningChainEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Renderer for LightningChainEntity.
 * Since this entity is purely visual and manages its own particles,
 * we don't need to render anything here - just prevent the null renderer crash.
 */
@OnlyIn(Dist.CLIENT)
public class LightningChainRenderer extends EntityRenderer<LightningChainEntity> {

    public LightningChainRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(LightningChainEntity entity) {
        // Return a dummy texture since we don't actually render the entity
        return new ResourceLocation("minecraft", "textures/entity/lightning_bolt.png");
    }
}
