package com.leon.saintsdragons.server.ai.dragonbrain;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class DragonOwnerTeleport {
    private DragonOwnerTeleport() {
    }

    public static boolean attempt(RideableDragonBase dragon, LivingEntity owner) {
        if (owner == null || dragon.level() != owner.level()) {
            return false;
        }
        Vec3 destination = DragonOwnerFollowTarget.safeTeleportTarget(dragon, owner);
        if (destination == null) {
            return false;
        }
        dragon.teleportTo(destination.x, destination.y, destination.z);
        dragon.getAIMovement().stop();
        return true;
    }
}
