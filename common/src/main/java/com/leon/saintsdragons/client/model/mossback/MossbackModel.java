package com.leon.saintsdragons.client.model.mossback;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.Mossback;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class MossbackModel extends DefaultedEntityGeoModel<Mossback> {
    public MossbackModel() {
        super(SaintsDragonsCommon.rl("mossback"));
    }
}
