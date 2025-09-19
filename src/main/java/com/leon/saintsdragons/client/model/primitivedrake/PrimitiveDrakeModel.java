package com.leon.saintsdragons.client.model.primitivedrake;

import com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class PrimitiveDrakeModel extends DefaultedEntityGeoModel<PrimitiveDrakeEntity> {
    
    public PrimitiveDrakeModel() {
        super(ResourceLocation.fromNamespaceAndPath("saintsdragons", "primitivedrake"), "head");
    }
}
