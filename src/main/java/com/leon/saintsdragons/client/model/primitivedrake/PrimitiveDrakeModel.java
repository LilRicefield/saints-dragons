package com.leon.saintsdragons.client.model.primitivedrake;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class PrimitiveDrakeModel extends DefaultedEntityGeoModel<PrimitiveDrakeEntity> {
    public PrimitiveDrakeModel() {
        super(SaintsDragons.rl( "primitive_drake"), "head");
    }
}
