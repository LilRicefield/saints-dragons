package com.leon.saintsdragons.server.entity.base;

public record DragonVariant(int id, String name, int weight) {
    public static DragonVariant of(int id, String name, int weight) {
        return new DragonVariant(id, name, weight);
    }

    public DragonVariant {
        if (id < 0) {
            throw new IllegalArgumentException("Dragon variant id must be >= 0");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Dragon variant name must not be blank");
        }
        if (weight < 0) {
            throw new IllegalArgumentException("Dragon variant weight must be >= 0");
        }
    }
}
