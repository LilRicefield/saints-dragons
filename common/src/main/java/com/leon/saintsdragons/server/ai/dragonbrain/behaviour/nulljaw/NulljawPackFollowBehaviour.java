package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.nulljaw;

import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonPackFollowBehaviour;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;

public final class NulljawPackFollowBehaviour extends DragonPackFollowBehaviour<Nulljaw> {
    public NulljawPackFollowBehaviour() {
        super(Nulljaw.class, 0.9D, 18.0D, 9.0D);
    }

    @Override
    protected boolean shouldLandWhenFollowEnds(Nulljaw member) {
        return false;
    }
}
