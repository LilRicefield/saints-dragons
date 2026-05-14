package com.leon.saintsdragons.server.entity.base;

public final class DragonSitTransitionController {
    private final DragonEntity dragon;
    private int transitionTicks;
    private boolean sittingDown;
    private boolean standingUp;

    public DragonSitTransitionController(DragonEntity dragon) {
        this.dragon = dragon;
    }

    public void tick(int sitDownTicks, int sitUpTicks, Runnable sitDownAnimation, Runnable sitUpAnimation) {
        if (dragon.level().isClientSide) {
            return;
        }

        tickTransitionTimer();

        float sitProgress = dragon.getSitProgress();
        float maxSitTicks = dragon.maxSitTicks();
        if (dragon.isOrderedToSit()) {
            if ((sitProgress == 0f || standingUp) && !sittingDown) {
                sitDownAnimation.run();
                sittingDown = true;
                standingUp = false;
                transitionTicks = sitDownTicks;
            }

            if (sitProgress < maxSitTicks) {
                dragon.setSitProgress(sitProgress + 1f);
            }
            return;
        }

        if (dragon.isVehicle()) {
            if (sitProgress != 0f) {
                dragon.clearSitProgress();
            }
            clearTransitionOnly();
            return;
        }

        if (sitProgress > 0f) {
            if ((sitProgress >= maxSitTicks || sittingDown) && !standingUp) {
                sitUpAnimation.run();
                standingUp = true;
                sittingDown = false;
                transitionTicks = sitUpTicks;
            }

            float decrementRate = maxSitTicks / Math.max(1f, sitUpTicks);
            sitProgress -= decrementRate;
            if (sitProgress < 0f) {
                sitProgress = 0f;
            }
            dragon.setSitProgress(sitProgress);
        }
    }

    public void clear() {
        dragon.clearSitProgress();
        clearTransitionOnly();
    }

    public void clearTransitionOnly() {
        sittingDown = false;
        standingUp = false;
        transitionTicks = 0;
    }

    public boolean isInTransition() {
        return sittingDown || standingUp;
    }

    public boolean isSittingDown() {
        return sittingDown;
    }

    public boolean isStandingUp() {
        return standingUp;
    }

    private void tickTransitionTimer() {
        if (transitionTicks <= 0) {
            return;
        }
        transitionTicks--;
        if (transitionTicks == 0) {
            clearTransitionOnly();
        }
    }
}
