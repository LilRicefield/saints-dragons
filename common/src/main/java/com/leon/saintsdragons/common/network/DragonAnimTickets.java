package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import software.bernie.geckolib.network.SerializableDataTicket;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Shared GeckoLib data tickets for syncing wyvern animation state.
 *
 * FLIGHT_MODE values:
 * -1 = grounded
 *  0 = glide
 *  1 = flap
 *  2 = hover
 *  3 = takeoff
 *  4 = sprint_flap
 *  5 = fly_idle (stationary rider hover)
 */
public final class DragonAnimTickets {
    public static final SerializableDataTicket<Integer> GROUND_STATE = registerInt("dragon_ground_state");
    public static final SerializableDataTicket<Integer> FLIGHT_MODE = registerInt("dragon_flight_mode");

    private DragonAnimTickets() {
    }

    public static void bootstrap() {
        // Intentionally empty – simply referencing this class ensures static initialisers run.
    }

    private static SerializableDataTicket<Integer> registerInt(String path) {
        SerializableDataTicket<Integer> ticket = SerializableDataTicket.ofInt(
                SaintsDragonsCommon.rl(path)
        );
        return GeckoLibUtil.addDataTicket(ticket);
    }

    private static SerializableDataTicket<Boolean> registerBool(String path) {
        SerializableDataTicket<Boolean> ticket = SerializableDataTicket.ofBoolean(
                SaintsDragonsCommon.rl(path)
        );
        return GeckoLibUtil.addDataTicket(ticket);
    }
}
