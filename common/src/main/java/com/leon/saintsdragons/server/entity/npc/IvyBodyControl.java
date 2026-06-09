package com.leon.saintsdragons.server.entity.npc;

import com.leon.saintsdragons.server.entity.controller.HumanoidBodyControl;

class IvyBodyControl extends HumanoidBodyControl {
    private static final float BOXING_VISUAL_TURN_SPEED = 0.8F;

    private final IvyTheDragonMerchant ivy;

    IvyBodyControl(IvyTheDragonMerchant ivy) {
        super(ivy, 0.45F, 0.12F, 30.0F, 50.0F);
        this.ivy = ivy;
    }

    @Override
    public void clientTick() {
        if (ivy.isBoxingCombatActive()) {
            lockBodyToYaw(ivy.getYRot(), BOXING_VISUAL_TURN_SPEED);
            return;
        }
        super.clientTick();
    }

    @Override
    public void serverTick() {
        if (ivy.isBoxingCombatActive()) {
            lockBodyToYaw(ivy.getYRot(), BOXING_VISUAL_TURN_SPEED);
            return;
        }
        super.serverTick();
    }
}
