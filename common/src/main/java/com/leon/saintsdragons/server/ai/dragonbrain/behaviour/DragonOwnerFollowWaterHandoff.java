package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.navigation.PathNavigateGround;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.entity.interfaces.SemiAquaticDragon;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import org.jetbrains.annotations.Nullable;

final class DragonOwnerFollowWaterHandoff {
    @Nullable
    private RideableDragonBase dragon;
    @Nullable
    private PathNavigateGround navigation;
    private float originalWaterMalus;
    private float originalWaterBorderMalus;

    void activate(RideableDragonBase candidate) {
        if (dragon == candidate && navigation != null) {
            return;
        }
        release();
        if (!(candidate instanceof SemiAquaticDragon) || !candidate.canSwim()) {
            return;
        }
        if (candidate instanceof RideableFlyingDragon flyingDragon) {
            flyingDragon.switchToGroundNavigation();
        }
        if (!(candidate.getNavigation() instanceof PathNavigateGround groundNavigation)) {
            return;
        }

        dragon = candidate;
        navigation = groundNavigation;
        originalWaterMalus = candidate.getPathfindingMalus(BlockPathTypes.WATER);
        originalWaterBorderMalus = candidate.getPathfindingMalus(BlockPathTypes.WATER_BORDER);
        groundNavigation.setWaterEntryAllowed(true);
        candidate.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        candidate.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0F);
    }

    void release() {
        if (dragon == null || navigation == null) {
            return;
        }
        navigation.setWaterEntryAllowed(false);
        dragon.setPathfindingMalus(BlockPathTypes.WATER, originalWaterMalus);
        dragon.setPathfindingMalus(BlockPathTypes.WATER_BORDER, originalWaterBorderMalus);
        dragon = null;
        navigation = null;
    }

    boolean isActive() {
        return dragon != null;
    }
}
