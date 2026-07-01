package com.leon.saintsdragons.client.renderer.atroxiia;

import com.leon.saintsdragons.client.model.atroxiia.AtroxiiaModel;
import com.leon.saintsdragons.client.renderer.DragonGeoEntityRenderer;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class AtroxiiaRenderer extends DragonGeoEntityRenderer<Atroxiia> {
    private static final String PASSENGER_BONE = "passengerBone";
    private static final float PASSENGER_X = 0.0f;
    private static final float PASSENGER_Y = -3.0f;
    private static final float PASSENGER_Z = 0.0f;

    public AtroxiiaRenderer(EntityRendererProvider.Context context) {
        super(context, new AtroxiiaModel());
    }

    @Override
    protected float getBabyShadowRadius(Atroxiia entity) {
        return 0.45F;
    }

    @Override
    protected float getAdultShadowRadius(Atroxiia entity) {
        return 2.0F;
    }

    @Override
    protected String[] trackedBoneNames() {
        return new String[] {PASSENGER_BONE};
    }

    @Override
    protected LocatorSpec[] locatorSpecs(Atroxiia entity) {
        return new LocatorSpec[] {
                new LocatorSpec(PASSENGER_BONE, PASSENGER_X, PASSENGER_Y, PASSENGER_Z, "passengerLocator")
        };
    }
}
