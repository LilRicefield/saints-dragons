package com.leon.saintsdragons.server.ai.navigation;

import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;

/**
 * Shared arbiter for dragons that swap between ground and air movement authorities.
 * Only one side owns navigation/movement at a time.
 */
public class DragonNavigationModeController {
    public interface Host {
        void setActiveNavigation(PathNavigation navigation);

        void setActiveMoveControl(MoveControl moveControl);

        default void beforeSwitchToAir() {
        }

        default void afterSwitchToAir() {
        }

        default void beforeSwitchToGround() {
        }

        default void afterSwitchToGround() {
        }
    }

    private final Host host;
    private final PathNavigation groundNavigation;
    private final PathNavigation airNavigation;
    private final MoveControl groundMoveControl;
    private final MoveControl airMoveControl;
    private boolean usingAirNavigation;

    public DragonNavigationModeController(
            Host host,
            PathNavigation groundNavigation,
            PathNavigation airNavigation,
            MoveControl groundMoveControl,
            MoveControl airMoveControl
    ) {
        this.host = host;
        this.groundNavigation = groundNavigation;
        this.airNavigation = airNavigation;
        this.groundMoveControl = groundMoveControl;
        this.airMoveControl = airMoveControl;
    }

    public boolean isUsingAirNavigation() {
        return this.usingAirNavigation;
    }

    public void switchToAir() {
        if (this.usingAirNavigation) {
            return;
        }

        this.host.beforeSwitchToAir();
        this.groundNavigation.stop();
        this.host.setActiveNavigation(this.airNavigation);
        this.host.setActiveMoveControl(this.airMoveControl);
        this.usingAirNavigation = true;
        this.host.afterSwitchToAir();
    }

    public void switchToGround() {
        if (!this.usingAirNavigation) {
            return;
        }

        this.host.beforeSwitchToGround();
        this.airNavigation.stop();
        this.host.setActiveNavigation(this.groundNavigation);
        this.host.setActiveMoveControl(this.groundMoveControl);
        this.usingAirNavigation = false;
        this.host.afterSwitchToGround();
    }
}
