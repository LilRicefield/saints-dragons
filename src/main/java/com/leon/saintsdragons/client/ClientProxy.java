package com.leon.saintsdragons.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClientProxy {
    
    // List of entity UUIDs that should not be rendered (for custom rider positioning)
    public static List<UUID> blockedEntityRenders = new ArrayList<>();
    
    public void clientInit() {
    }
    
    /**
     * Block rendering of an entity by adding its UUID to the blocked list
     */
    public static void blockRenderingEntity(UUID id) {
        blockedEntityRenders.add(id);
    }
    
    /**
     * Release rendering of an entity by removing its UUID from the blocked list
     */
    public static void releaseRenderingEntity(UUID id) {
        blockedEntityRenders.remove(id);
    }
}