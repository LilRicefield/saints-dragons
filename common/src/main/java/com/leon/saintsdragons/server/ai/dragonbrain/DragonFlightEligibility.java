package com.leon.saintsdragons.server.ai.dragonbrain;

import com.leon.saintsdragons.server.ai.DragonAirCombatHelper;
import com.leon.saintsdragons.server.ai.DragonAirCombatSettingsProvider;
import com.leon.saintsdragons.server.ai.dragonbrain.tactical.DragonCombatFlightState;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public final class DragonFlightEligibility {
    private DragonFlightEligibility() {}

    public static @Nullable String pursuitBlockReason(RideableFlyingDragon dragon,
                                                      LivingEntity target,
                                                      boolean targetAirborne,
                                                      boolean abandonedGroundRoute,
                                                      boolean landingReserved) {
        if (!(dragon instanceof DragonAirCombatSettingsProvider settings)) return "no-flight-combat";
        String blocked = movementBlockReason(dragon);
        if (blocked != null) return blocked;
        if (!DragonAirCombatHelper.canUseAirCombat(dragon, target, settings.getAiAirCombatSettings().fallbackFollowRange())) {
            return "invalid-or-out-of-range-target";
        }
        DragonCombatFlightState combatFlight = settings.getCombatFlightState();
        if (combatFlight != null) {
            if (!combatFlight.wantsFlight()) return "combat-plan-not-airborne";
            return dragon.isAerial() ? null : "awaiting-planned-takeoff";
        }
        if (landingReserved) return "landing-reserved";
        if (abandonedGroundRoute) {
            return dragon.isAerial() || DragonAirCombatHelper.canTriggerAiFlight(dragon)
                    ? null : "takeoff-unavailable";
        }
        if (!targetAirborne) return "grounded-target";
        return DragonAirCombatHelper.canEngageAirborneTarget(dragon, target,
                settings.getAiAirCombatSettings(), settings.getAiTargetAirborneHeight(target))
                ? null : "air-engagement-unavailable";
    }

    public static @Nullable String movementBlockReason(RideableFlyingDragon dragon) {
        if (dragon.isAiLandingRecoveryActive()) return "landing-recovery";
        if (!dragon.canFly() || dragon.isBaby()) return "flight-unavailable";
        if (dragon.isVehicle() || dragon.isPassenger()) return "rider-or-passenger";
        if (dragon.isOrderedToSit() || dragon.isSleepLocked() || dragon.isDying()) return "inactive";
        return dragon instanceof DragonAirCombatSettingsProvider settings
                ? settings.getAiAirCombatBlockReason() : "no-flight-combat";
    }
}
