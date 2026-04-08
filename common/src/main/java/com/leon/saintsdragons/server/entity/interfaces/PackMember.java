package com.leon.saintsdragons.server.entity.interfaces;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Contract for entities that can participate in pack behaviors.
 * Implementations provide species-specific participation and leader rules.
 */
public interface PackMember<T> {
    @Nullable
    UUID getPackLeaderUuid();

    void setPackLeaderUuid(@Nullable UUID leaderUuid);

    boolean canParticipateInPack();

    boolean canLeadPack();

    int getPackLeadershipPriority();

    int getMaxPackSize();

    double getPackSearchRadius();

    default int getPackLeaderRefreshIntervalTicks() {
        return 60;
    }

    default boolean handleDirectAirPackFollow(Vec3 target, double speed) {
        return false;
    }
}
