package com.leon.saintsdragons.client.model.stegonaut;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class StegonautModel extends DefaultedEntityGeoModel<Stegonaut> {
    public StegonautModel() {
        super(SaintsDragons.rl( "stegonaut"), "head");
    }
}
