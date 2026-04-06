package com.leon.saintsdragons.server.entity.dragons.volitans.handlers;

import com.leon.saintsdragons.server.entity.component.DragonTamingStunComponent;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;


public class VolitansTamingHandler extends DragonTamingStunComponent<Volitans> {

    public VolitansTamingHandler(Volitans dragon) {
        super(dragon);
    }

    @Override
    protected boolean isTamingStunned() {
        return dragon.getEntityData().get(Volitans.DATA_TAMING_STUNNED);
    }

    @Override
    protected void setTamingStunned(boolean stunned) {
        dragon.getEntityData().set(Volitans.DATA_TAMING_STUNNED, stunned);
    }

    @Override
    protected boolean isBelowTamingThreshold() {
        return dragon.isBelowTamingThreshold();
    }

    @Override
    protected String getTamingTimeoutTranslationKey() {
        return "entity.saintsdragons.volitans.taming_timeout";
    }

    @Override
    protected boolean isInAerialStateForStun() {
        return dragon.isFlying() || dragon.isHovering() || dragon.isTakeoff() || dragon.isLanding();
    }

    @Override
    protected void clearAerialStateForStun() {
        dragon.setFlying(false);
        dragon.setHovering(false);
        dragon.setTakeoff(false);
        dragon.setLanding(false);
    }

    @Override
    protected void stopActiveAbilitiesForStun() {
        dragon.forceEndActiveAbility();
        dragon.setBreathing(false);
        dragon.setBurrowing(false);
    }
}
