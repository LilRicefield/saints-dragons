package com.leon.saintsdragons.server.entity.dragons.raevyx.handlers;

import com.leon.saintsdragons.server.entity.component.DragonTamingStunComponent;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;

public class RaevyxTamingHandler extends DragonTamingStunComponent<Raevyx> {

    public RaevyxTamingHandler(Raevyx wyvern) {
        super(wyvern);
    }

    @Override
    protected boolean isTamingStunned() {
        return dragon.getEntityData().get(Raevyx.DATA_TAMING_STUNNED);
    }

    @Override
    protected void setTamingStunned(boolean stunned) {
        dragon.getEntityData().set(Raevyx.DATA_TAMING_STUNNED, stunned);
    }

    @Override
    protected boolean isBelowTamingThreshold() {
        return dragon.isBelowTamingThreshold();
    }

    @Override
    protected float getTamingThreshold() {
        return dragon.getTamingThreshold();
    }

    @Override
    protected String getTamingTimeoutTranslationKey() {
        return "entity.saintsdragons.raevyx.taming_timeout";
    }

    @Override
    protected boolean isInAerialStateForStun() {
        return dragon.isFlying() || dragon.isHovering() || dragon.isTakeoff() || dragon.isLanding();
    }

    @Override
    protected void clearAerialStateForStun() {
        dragon.clearAerialStateForInterrupt();
    }

    @Override
    protected void stopActiveAbilitiesForStun() {
        dragon.forceEndActiveAbility();
        dragon.setBeaming(false);
    }
}
