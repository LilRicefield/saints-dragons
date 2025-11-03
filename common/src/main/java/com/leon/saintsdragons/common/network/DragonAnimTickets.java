package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import software.bernie.geckolib.network.SerializableDataTicket;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Shared GeckoLib data tickets for syncing wyvern animation state.
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
}
