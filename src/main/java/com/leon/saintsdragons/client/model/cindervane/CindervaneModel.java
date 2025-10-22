package com.leon.saintsdragons.client.model.cindervane;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class CindervaneModel extends DefaultedEntityGeoModel<Cindervane> {
    public CindervaneModel() {
        super(SaintsDragons.rl("cindervane"), "headController");
    }
}

