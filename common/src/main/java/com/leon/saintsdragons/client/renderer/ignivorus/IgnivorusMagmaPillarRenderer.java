package com.leon.saintsdragons.client.renderer.ignivorus;

import com.leon.saintsdragons.client.model.ignivorus.IgnivorusMagmaPillarModel;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusMagmaPillarEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class IgnivorusMagmaPillarRenderer extends GeoEntityRenderer<IgnivorusMagmaPillarEntity> {

    public IgnivorusMagmaPillarRenderer(EntityRendererProvider.Context context) {
        super(context, new IgnivorusMagmaPillarModel());
        this.shadowRadius = 1.0F;
    }

    @Override
    protected float getDeathMaxRotation(@NotNull IgnivorusMagmaPillarEntity entity) {
        return 0.0F;
    }
}
