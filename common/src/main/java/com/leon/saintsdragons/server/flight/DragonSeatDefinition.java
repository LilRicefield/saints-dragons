package com.leon.saintsdragons.server.flight;

import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public record DragonSeatDefinition(Vec3 offset, String locatorName) {
    public DragonSeatDefinition {
        Objects.requireNonNull(offset, "offset");
        Objects.requireNonNull(locatorName, "locatorName");
        if (!Double.isFinite(offset.x) || !Double.isFinite(offset.y)
                || !Double.isFinite(offset.z) || locatorName.isBlank()) {
            throw new IllegalArgumentException("A seat needs a finite local offset and a locator name");
        }
    }
}
