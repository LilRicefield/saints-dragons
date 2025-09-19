package com.leon.saintsdragons.client.model.primitivedrake;

import com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class PrimitiveDrakeModel extends GeoModel<PrimitiveDrakeEntity> {
    
    @Override
    public ResourceLocation getModelResource(PrimitiveDrakeEntity object) {
        return ResourceLocation.fromNamespaceAndPath("saintsdragons", "geo/primitivedrake/primitivedrake.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(PrimitiveDrakeEntity object) {
        return  ResourceLocation.fromNamespaceAndPath("saintsdragons", "textures/entity/primitivedrake/primitivedrake.png");
    }

    @Override
    public ResourceLocation getAnimationResource(PrimitiveDrakeEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("saintsdragons", "animations/primitivedrake/primitivedrake.animation.json");
    }
}
