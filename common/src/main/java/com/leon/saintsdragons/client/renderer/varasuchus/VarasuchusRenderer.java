package com.leon.saintsdragons.client.renderer.varasuchus;

import com.leon.saintsdragons.client.model.varasuchus.VarasuchusModel;
import com.leon.saintsdragons.client.renderer.DragonGeoEntityRenderer;
import com.leon.saintsdragons.client.renderer.layer.varasuchus.VarasuchusNightEmissiveLayer;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class VarasuchusRenderer extends DragonGeoEntityRenderer<Varasuchus> {
    private static final String PASSENGER_BONE = "passengerBone";
    private static final float PASSENGER_X = 0.0f;
    private static final float PASSENGER_Y = -3.0f;
    private static final float PASSENGER_Z = 0.0f;

    public VarasuchusRenderer(EntityRendererProvider.Context context) {
        super(context, new VarasuchusModel());
        this.addRenderLayer(new VarasuchusNightEmissiveLayer(this));
    }

    @Override
    protected float getBabyShadowRadius(Varasuchus entity) {
        return 1.5F;
    }

    @Override
    protected float getAdultShadowRadius(Varasuchus entity) {
        return 2.5f;
    }

    @Override
    protected String[] trackedBoneNames() {
        return new String[] {PASSENGER_BONE};
    }

    @Override
    protected LocatorSpec[] locatorSpecs(Varasuchus entity) {
        return new LocatorSpec[] {
                new LocatorSpec(PASSENGER_BONE, PASSENGER_X, PASSENGER_Y, PASSENGER_Z, "passengerLocator")
        };
    }
}
