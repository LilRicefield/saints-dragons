package com.leon.saintsdragons.server.ai.dragonbrain.debug;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.schedule.Activity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Associates a built brain with the actual behaviour instances installed in it.
 * Weak keys ensure diagnostics never keep an entity brain alive.
 */
public final class DragonBrainDiagnostics {
    private static final Map<Brain<?>, List<RegisteredBehaviour>> LAYOUTS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<LivingEntity, List<RegisteredBehaviour>> ENTITY_LAYOUTS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private DragonBrainDiagnostics() {
    }

    public static void attach(Brain<?> brain, List<RegisteredBehaviour> behaviours) {
        LAYOUTS.put(brain, List.copyOf(behaviours));
    }

    public static void attach(LivingEntity entity, List<RegisteredBehaviour> behaviours) {
        ENTITY_LAYOUTS.put(entity, List.copyOf(behaviours));
    }

    public static List<RegisteredBehaviour> getBehaviours(LivingEntity entity, Brain<?> brain) {
        List<RegisteredBehaviour> behaviours = ENTITY_LAYOUTS.get(entity);
        if (behaviours != null && !behaviours.isEmpty()) {
            return behaviours;
        }
        behaviours = LAYOUTS.get(brain);
        return behaviours == null ? List.of() : behaviours;
    }

    public record RegisteredBehaviour(Activity activity,
                                      int priority,
                                      BehaviorControl<?> behaviour) {
    }
}
