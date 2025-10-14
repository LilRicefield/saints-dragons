package com.leon.saintsdragons.client.model.nulljaw;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class NulljawModel extends DefaultedEntityGeoModel<Nulljaw> {

    public NulljawModel() {
        super(SaintsDragons.rl("nulljaw"), "head");
    }
}
