package com.leon.saintsdragons.fabric;

import net.fabricmc.api.ClientModInitializer;

/**
 * Temporary Fabric client hook. Client-side setup will be wired in after the
 * shared client code is moved into the common module.
 */
public final class SaintsDragonsFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        System.out.println("[Saint's Dragons] Fabric client hook loaded – functionality pending.");
    }
}
