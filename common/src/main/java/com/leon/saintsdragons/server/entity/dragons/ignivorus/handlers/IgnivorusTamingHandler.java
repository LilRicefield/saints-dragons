package com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers;

import com.leon.saintsdragons.server.entity.component.DragonTamingStunComponent;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;

public class IgnivorusTamingHandler extends DragonTamingStunComponent<Ignivorus> {

    public IgnivorusTamingHandler(Ignivorus dragon) {
        super(dragon);
    }

    @Override
    protected boolean isTamingStunned() {
        return dragon.getEntityData().get(Ignivorus.DATA_TAMING_STUNNED);
    }

    @Override
    protected void setTamingStunned(boolean stunned) {
        if (stunned) {
            dragon.clearPhase2ForTamingStun();
        }
        dragon.getEntityData().set(Ignivorus.DATA_TAMING_STUNNED, stunned);
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
        return "entity.saintsdragons.ignivorus.taming_timeout";
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
        dragon.setBreathingFire(false);
    }
}
