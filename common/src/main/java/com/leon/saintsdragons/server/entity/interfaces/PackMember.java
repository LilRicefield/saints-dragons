package com.leon.saintsdragons.server.entity.interfaces;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface PackMember<T> {
    @Nullable
    UUID getPackLeaderUuid();
    void setPackLeaderUuid(@Nullable UUID leaderUuid);
    default boolean canParticipateInPack() {
        DragonEntity self = (DragonEntity) this;
        if (self.isTame()) {
            return false;
        }
        if (self.isBaby() || self.isDying()) {
            return false;
        }
        if (!self.isAlive() || self.isRemoved()) {
            return false;
        }
        return !self.isOrderedToSit() && self.getCommand() != 1;
    }
    default boolean canLeadPack() {
        DragonEntity self = (DragonEntity) this;
        return canParticipateInPack() && !self.isFemale();
    }
    default int getPackLeadershipPriority() {
        DragonEntity self = (DragonEntity) this;
        return Math.round((self.getHealth() / Math.max(1.0F, self.getMaxHealth())) * 100.0F);
    }
    int getMaxPackSize();
    double getPackSearchRadius();
    default int getPackLeaderRefreshIntervalTicks() {
        return 60;
    }
    default boolean handleDirectAirPackFollow(Vec3 target, double speed) {
        return false;
    }
}
