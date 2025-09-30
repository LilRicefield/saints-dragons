package com.leon.saintsdragons.client.model.amphithere;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.dragons.amphithere.AmphithereEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class AmphithereModel extends DefaultedEntityGeoModel<AmphithereEntity> {
    public AmphithereModel() {
        super(SaintsDragons.rl("amphithere"), "headController");
    }
}

