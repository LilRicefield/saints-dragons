package com.leon.saintsdragons.server.ai.navigation.async;

import java.util.Objects;
import net.minecraft.world.phys.Vec3;

public record DragonLandingPlan(Vec3 approach,
                                Vec3 glide,
                                Vec3 flare,
                                Vec3 touchdown) {
    public DragonLandingPlan {
        Objects.requireNonNull(approach, "approach");
        Objects.requireNonNull(glide, "glide");
        Objects.requireNonNull(flare, "flare");
        Objects.requireNonNull(touchdown, "touchdown");
    }
}
