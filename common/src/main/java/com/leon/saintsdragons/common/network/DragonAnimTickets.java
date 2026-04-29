package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import software.bernie.geckolib.network.SerializableDataTicket;
import software.bernie.geckolib.util.GeckoLibUtil;


public final class DragonAnimTickets {
    public static final SerializableDataTicket<Integer> GROUND_MODE = registerInt("dragon_ground_state");
    public static final SerializableDataTicket<Integer> FLIGHT_MODE = registerInt("dragon_flight_mode");

    private DragonAnimTickets() {
    }
    public static void bootstrap() {
    }
    private static SerializableDataTicket<Integer> registerInt(String path) {
        SerializableDataTicket<Integer> ticket = SerializableDataTicket.ofInt(
                SaintsDragonsCommon.rl(path)
        );
        return GeckoLibUtil.addDataTicket(ticket);
    }
}