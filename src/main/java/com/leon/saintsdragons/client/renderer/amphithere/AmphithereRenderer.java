package com.leon.saintsdragons.client.renderer.amphithere;

import com.leon.saintsdragons.client.model.amphithere.AmphithereModel;
import com.leon.saintsdragons.server.entity.dragons.amphithere.AmphithereEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class AmphithereRenderer extends GeoEntityRenderer<AmphithereEntity> {
    public AmphithereRenderer(EntityRendererProvider.Context context) {
        super(context, new AmphithereModel());
        this.shadowRadius = 0.9F;
    }
}
