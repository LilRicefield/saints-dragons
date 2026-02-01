package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Simple registry to map ability names to types and back.
 */
public final class AbilityRegistry {
    private AbilityRegistry() {}

    private static final Map<ResourceLocation, DragonAbilityType<?, ?>> BY_NAME = new HashMap<>();
    private static final Map<DragonAbilityType<?, ?>, ResourceLocation> BY_TYPE = new IdentityHashMap<>();

    public static synchronized <M extends LivingEntity, T extends DragonAbility<M>> DragonAbilityType<M, T> register(DragonAbilityType<M, T> type) {
        return register(type.getName(), type);
    }

    public static synchronized <M extends LivingEntity, T extends DragonAbility<M>> DragonAbilityType<M, T> register(String name, DragonAbilityType<M, T> type) {
        ResourceLocation key = resolveKey(name);
        if (key == null) {
            throw new IllegalArgumentException("Ability name must not be null/empty");
        }
        DragonAbilityType<?, ?> existing = BY_NAME.putIfAbsent(key, type);
        if (existing != null && existing != type) {
            throw new IllegalStateException("Duplicate ability name: " + key);
        }
        BY_TYPE.putIfAbsent(type, key);
        return type;
    }

    public static DragonAbilityType<?, ?> get(String name) {
        ResourceLocation key = resolveKey(name);
        if (key == null) {
            return null;
        }
        return BY_NAME.get(key);
    }

    public static String getName(DragonAbilityType<?, ?> type) {
        ResourceLocation key = BY_TYPE.get(type);
        return key != null ? key.toString() : null;
    }

    private static ResourceLocation resolveKey(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        if (name.indexOf(':') >= 0) {
            return ResourceLocation.tryParse(name);
        }
        return new ResourceLocation(SaintsDragonsCommon.MOD_ID, name);
    }
}
