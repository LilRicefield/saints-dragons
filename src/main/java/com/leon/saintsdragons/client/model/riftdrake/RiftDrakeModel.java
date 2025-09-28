package com.leon.saintsdragons.client.model.riftdrake;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.dragons.riftdrake.RiftDrakeEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class RiftDrakeModel extends DefaultedEntityGeoModel<RiftDrakeEntity> {

    public RiftDrakeModel() {
        super(SaintsDragons.rl("rift_drake"), "head");
    }
}
