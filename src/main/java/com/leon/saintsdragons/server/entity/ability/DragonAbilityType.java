package com.leon.saintsdragons.server.entity.ability;

import net.minecraft.world.entity.LivingEntity;

public record DragonAbilityType<M extends LivingEntity, T extends DragonAbility<M>>(
        String name,
        IFactory<M, T> factory
) implements Comparable<DragonAbilityType<M, T>> {

    public DragonAbilityType {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Ability name must not be null/empty");
        }
        if (factory == null) {
            throw new IllegalArgumentException("Factory must not be null for ability: " + name);
        }
    }

    /**
     * Type-safe construction that avoids unchecked cast warnings.
     */
    public T makeInstance(M user) {
        return factory.create(this, user);
    }

    public interface IFactory<M extends LivingEntity, T extends DragonAbility<M>> {
        T create(DragonAbilityType<M, T> abilityType, M user);
    }

    // Backwards-friendly accessor name used elsewhere in the codebase
    public String getName() {
        return name;
    }

    @Override
    public int compareTo(DragonAbilityType<M, T> o) {
        return this.getName().compareTo(o.getName());
    }
}
