package com.leon.saintsdragons.server.entity.dragons.atroxiia.handlers;

import com.leon.saintsdragons.server.entity.component.DragonTamingStunComponent;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;

public final class AtroxiiaTamingHandler extends DragonTamingStunComponent<Atroxiia> {
    public AtroxiiaTamingHandler(Atroxiia dragon) {
        super(dragon);
    }

    @Override
    protected boolean isTamingStunned() {
        return dragon.getEntityData().get(Atroxiia.DATA_TAMING_STUNNED);
    }

    @Override
    protected void setTamingStunned(boolean stunned) {
        dragon.getEntityData().set(Atroxiia.DATA_TAMING_STUNNED, stunned);
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
        return "entity.saintsdragons.atroxiia.taming_timeout";
    }

    @Override
    protected boolean isInAerialStateForStun() {
        return false;
    }

    @Override
    protected void clearAerialStateForStun() {
    }

    @Override
    protected void stopActiveAbilitiesForStun() {
        dragon.forceEndActiveAbility();
    }
}
