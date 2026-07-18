package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.nulljaw;

import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonFollowParentBehaviour;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.world.phys.Vec3;

public final class NulljawFollowParentBehaviour extends DragonFollowParentBehaviour<Nulljaw> {
    private static final double FOLLOW_SPEED = 0.9D;

    public NulljawFollowParentBehaviour() {
        super(Nulljaw.class, FOLLOW_SPEED);
    }

    @Override
    protected void moveTo(Nulljaw baby, Nulljaw adult) {
        baby.beginAiFlight();
        Vec3 target = adult.position().add(0.0D, adult.getBbHeight() * 0.5D, 0.0D);
        baby.getAIMovement().setAsyncAirWaypoint(target, FOLLOW_SPEED);
    }
}
