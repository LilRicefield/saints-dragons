// NetworkHandler.java
package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.SaintsDragons;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    private static final int ID_RIDER_INPUT = 0;
    private static final int ID_CONTROL_STATE = 1;
    private static final int ID_ALLY_MANAGEMENT = 2;
    private static final int ID_ALLY_LIST = 3;
    private static final int ID_ALLY_REQUEST = 4;
    private static final int ID_ALLY_DELTA = 5;

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(SaintsDragons.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        DragonAnimTickets.bootstrap();
        // Message: Rider input
        INSTANCE.messageBuilder(MessageDragonRideInput.class, ID_RIDER_INPUT)
                .encoder(MessageDragonRideInput::encode)
                .decoder(MessageDragonRideInput::decode)
                .consumerNetworkThread(MessageDragonRideInput::handle)
                .add();

        // Message: Control state bitfield
        INSTANCE.messageBuilder(MessageDragonControl.class, ID_CONTROL_STATE)
                .encoder(MessageDragonControl::encode)
                .decoder(MessageDragonControl::decode)
                .consumerNetworkThread(MessageDragonControl::handle)
                .add();


        // Message: Client->Server ally management (add/remove)
        INSTANCE.messageBuilder(MessageDragonAllyManagement.class, ID_ALLY_MANAGEMENT)
                .encoder(MessageDragonAllyManagement::encode)
                .decoder(MessageDragonAllyManagement::new)
                .consumerNetworkThread(MessageDragonAllyManagement::handle)
                .add();

        // Message: Server->Client ally list sync
        INSTANCE.messageBuilder(MessageDragonAllyList.class, ID_ALLY_LIST)
                .encoder(MessageDragonAllyList::encode)
                .decoder(MessageDragonAllyList::new)
                .consumerNetworkThread(MessageDragonAllyList::handle)
                .add();

        // Message: Client->Server ally list request
        INSTANCE.messageBuilder(MessageDragonAllyRequest.class, ID_ALLY_REQUEST)
                .encoder(MessageDragonAllyRequest::encode)
                .decoder(MessageDragonAllyRequest::new)
                .consumerNetworkThread(MessageDragonAllyRequest::handle)
                .add();

        // Message: Server->Client ally delta (add/remove single ally, optimized)
        INSTANCE.messageBuilder(MessageDragonAllyDelta.class, ID_ALLY_DELTA)
                .encoder(MessageDragonAllyDelta::encode)
                .decoder(MessageDragonAllyDelta::new)
                .consumerNetworkThread(MessageDragonAllyDelta::handle)
                .add();
    }
}