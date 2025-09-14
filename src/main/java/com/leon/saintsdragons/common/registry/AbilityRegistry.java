package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Simple registry to map ability names to types and back.
 */
public final class AbilityRegistry {
    private AbilityRegistry() {}

    private static final Map<String, DragonAbilityType<?, ?>> BY_NAME = new HashMap<>();
    private static final Map<DragonAbilityType<?, ?>, String> BY_TYPE = new IdentityHashMap<>();

    public static synchronized <M extends LivingEntity, T extends DragonAbility<M>> DragonAbilityType<M, T> register(DragonAbilityType<M, T> type) {
        return register(type.getName(), type);
    }

    public static synchronized <M extends LivingEntity, T extends DragonAbility<M>> DragonAbilityType<M, T> register(String name, DragonAbilityType<M, T> type) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Ability name must not be null/empty");
        }
        DragonAbilityType<?, ?> existing = BY_NAME.putIfAbsent(name, type);
        if (existing != null && existing != type) {
            throw new IllegalStateException("Duplicate ability name: " + name);
        }
        BY_TYPE.putIfAbsent(type, name);
        return type;
    }

    public static DragonAbilityType<?, ?> get(String name) {
        return BY_NAME.get(name);
    }

    public static String getName(DragonAbilityType<?, ?> type) {
        return BY_TYPE.get(type);
    }
}