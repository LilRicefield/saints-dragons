package com.leon.saintsdragons.server.ai.dragonbrain.debug;

import java.util.Map;

/**
 * Optional, read-only details exposed by a brain behaviour to the live debugger.
 * Implementations must only return cached state; they must not evaluate behaviour
 * predicates or otherwise mutate the owning entity.
 */
public interface DragonBrainDebugDetails {
    default Map<String, String> getDragonBrainDebugDetails() {
        return Map.of();
    }
}
