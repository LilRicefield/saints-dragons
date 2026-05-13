package com.leon.saintsdragons.client.renderer.stegonaut;

import com.leon.saintsdragons.client.renderer.DragonGeoEntityRenderer;
import com.leon.saintsdragons.client.model.stegonaut.StegonautModel;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

@Environment(EnvType.CLIENT)
public class StegonautRenderer extends DragonGeoEntityRenderer<Stegonaut> {
    private static final String PASSENGER_BONE = "passengerBone";
    private static final float PASSENGER_X = 0.0f;
    private static final float PASSENGER_Y = -3.0f;
    private static final float PASSENGER_Z = 0.0f;
    
    public StegonautRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new StegonautModel());
    }

    @Override
    protected float getBabyShadowRadius(Stegonaut entity) {
        return 1.0F;
    }

    @Override
    protected float getAdultShadowRadius(Stegonaut entity) {
        return 2.25f;
    }

    @Override
    protected String[] trackedBoneNames() {
        return new String[] {PASSENGER_BONE};
    }

    @Override
    protected LocatorSpec[] locatorSpecs(Stegonaut entity) {
        return new LocatorSpec[] {
                new LocatorSpec(PASSENGER_BONE, PASSENGER_X, PASSENGER_Y, PASSENGER_Z, "passengerLocator")
        };
    }
}
