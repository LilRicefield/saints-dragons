package com.leon.saintsdragons.client.renderer.nulljaw;

import com.leon.saintsdragons.client.model.nulljaw.NulljawModel;
import com.leon.saintsdragons.client.renderer.DragonGeoEntityRenderer;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

@Environment(EnvType.CLIENT)
public final class NulljawRenderer extends DragonGeoEntityRenderer<Nulljaw> {
    private static final String PASSENGER_BONE = "passengerBone";
    private static final float PASSENGER_X = -0.5f;
    private static final float PASSENGER_Y = 1.0f;
    private static final float PASSENGER_Z = 0.0f;

    public NulljawRenderer(EntityRendererProvider.Context context) {
        super(context, new NulljawModel());
        this.shadowRadius = 1.3F;
    }

    @Override
    protected float getBabyShadowRadius(Nulljaw entity) {
        return 0.75F;
    }

    @Override
    protected float getAdultShadowRadius(Nulljaw entity) {
        return 1.3F;
    }

    @Override
    protected String[] trackedBoneNames() {
        return new String[] {PASSENGER_BONE};
    }

    @Override
    protected LocatorSpec[] locatorSpecs(Nulljaw entity) {
        return new LocatorSpec[] {
                new LocatorSpec(PASSENGER_BONE, PASSENGER_X, PASSENGER_Y, PASSENGER_Z,
                        "passengerLocator", "passengerSeat0")
        };
    }
}
