package com.leon.saintsdragons.client.renderer.npc;

import com.leon.saintsdragons.client.model.npc.IvyTheDragonMerchantModel;
import com.leon.saintsdragons.client.renderer.layer.npc.IvyHeldItemLayer;
import com.leon.saintsdragons.server.entity.npc.IvyTheDragonMerchant;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class IvyTheDragonMerchantRenderer extends GeoEntityRenderer<com.leon.saintsdragons.server.entity.npc.IvyTheDragonMerchant> {
    @Override
    public float getMotionAnimThreshold(IvyTheDragonMerchant animatable) {
        return 0.000001f;
    }

    public IvyTheDragonMerchantRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new IvyTheDragonMerchantModel());
        this.addRenderLayer(new IvyHeldItemLayer(this));
        this.shadowRadius = 0.6f;
    }

}
