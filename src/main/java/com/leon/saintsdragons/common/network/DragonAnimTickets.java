package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.SaintsDragons;
import software.bernie.geckolib.network.SerializableDataTicket;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Shared GeckoLib data tickets for syncing dragon animation state.
 */
public final class DragonAnimTickets {
    public static final SerializableDataTicket<Integer> GROUND_STATE = registerInt("dragon_ground_state");
    public static final SerializableDataTicket<Integer> FLIGHT_MODE = registerInt("dragon_flight_mode");

    private DragonAnimTickets() {
    }

    public static void bootstrap() {
        // Force class loading to ensure tickets are registered on both logical sides
    }

    private static SerializableDataTicket<Integer> registerInt(String path) {
        SerializableDataTicket<Integer> ticket = SerializableDataTicket.ofInt(SaintsDragons.rl(path));
        return GeckoLibUtil.addDataTicket(ticket);
    }
}
