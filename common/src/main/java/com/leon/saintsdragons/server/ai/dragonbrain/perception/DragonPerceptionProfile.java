package com.leon.saintsdragons.server.ai.dragonbrain.perception;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

public record DragonPerceptionProfile(
        int hearingRange,
        int targetMemoryTicks,
        int soundMemoryTicks,
        int searchTicks,
        double investigationSpeed,
        double arrivalDistance
) {
    private static final int MIN_HEARING_RANGE = 24;
    private static final int MAX_HEARING_RANGE = 48;
    private static final int MIN_INVESTIGATION_TICKS = 20 * 8;
    private static final int MAX_INVESTIGATION_TICKS = 20 * 20;
    private static final int ROUTE_MARGIN_TICKS = 20 * 2;
    private static final double ESTIMATED_TICKS_PER_BLOCK = 8.0D;

    public static DragonPerceptionProfile forDragon(DragonEntity dragon) {
        double followRange = dragon.getAttributeValue(Attributes.FOLLOW_RANGE);
        int hearingRange = Mth.clamp(
                (int)Math.round(Math.max(MIN_HEARING_RANGE, followRange * 0.9D)),
                MIN_HEARING_RANGE,
                MAX_HEARING_RANGE
        );
        return new DragonPerceptionProfile(
                hearingRange,
                20 * 6,
                20 * 4,
                20 * 3,
                0.85D,
                Math.max(2.0D, dragon.getBbWidth() * 0.75D)
        );
    }

    public int investigationMemoryTicks(DragonEntity dragon, Vec3 destination) {
        double travelDistance = Math.max(0.0D,
                dragon.position().distanceTo(destination) - arrivalDistance);
        int estimatedTravelTicks = Mth.ceil(travelDistance * ESTIMATED_TICKS_PER_BLOCK);
        return Mth.clamp(
                estimatedTravelTicks + searchTicks + ROUTE_MARGIN_TICKS,
                MIN_INVESTIGATION_TICKS,
                MAX_INVESTIGATION_TICKS
        );
    }
}
