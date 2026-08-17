package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

public final class DragonRoostComponent {
    private static final int HOME_LOOKUP_INTERVAL_TICKS = 20;
    private static final int MAX_HOME_LOOKUP_ATTEMPTS = 10;
    private static final int SLEEP_ANCHOR_LOOKUP_INTERVAL_TICKS = 20;
    private static final int MAX_SLEEP_ANCHOR_LOOKUP_ATTEMPTS = 10;
    private static final String SLEEP_ANCHOR_TAG = "saintsdragons.sleep_anchor";

    private final DragonEntity dragon;
    private final ResourceKey<Structure> roostStructure;
    private final double sleepRadiusSqr;
    private final double territoryRadius;
    private final int requiredSleepSettleTicks;

    private int sleepSettleTicks;
    private int homeLookupCooldown;
    private int homeLookupAttempts;
    private int sleepAnchorLookupCooldown;
    private int sleepAnchorLookupAttempts;
    private boolean sleepAnchorResolved;

    public DragonRoostComponent(DragonEntity dragon,
                                ResourceKey<Structure> roostStructure,
                                double sleepRadius,
                                double territoryRadius,
                                int requiredSleepSettleTicks) {
        this.dragon = dragon;
        this.roostStructure = roostStructure;
        this.sleepRadiusSqr = sleepRadius * sleepRadius;
        this.territoryRadius = territoryRadius;
        this.requiredSleepSettleTicks = requiredSleepSettleTicks;
    }

    public void initializeHomeFromSpawn(ServerLevelAccessor level, MobSpawnType spawnType) {
        if (spawnType == MobSpawnType.STRUCTURE) {
            setHome(GlobalPos.of(level.getLevel().dimension(), dragon.blockPosition()));
        }
    }

    public void tick() {
        if (dragon.level().isClientSide) {
            return;
        }

        GlobalPos home = getHome();
        if (home == null) {
            tryInitializeHomeFromContainingStructure();
            home = getHome();
        }
        if (home != null) {
            ensureSleepPosition(home);
            tryResolveSleepAnchor(home);
        }
        tickSleepReadiness();
    }

    public void restoreMemories() {
        GlobalPos home = getHome();
        if (home != null) {
            ensureSleepPosition(home);
            GlobalPos sleepPosition = dragon.getBrain().getMemory(DragonMemories.ROOST_SLEEP_POSITION)
                    .orElse(null);
            sleepAnchorResolved = sleepPosition != null
                    && sleepPosition.dimension().equals(home.dimension())
                    && !sleepPosition.pos().equals(home.pos());
            sleepAnchorLookupCooldown = 0;
            sleepAnchorLookupAttempts = sleepAnchorResolved ? MAX_SLEEP_ANCHOR_LOOKUP_ATTEMPTS : 0;
        }
    }

    public boolean canSleepAtRoost() {
        if (dragon.isTame()) {
            return true;
        }
        GlobalPos sleepPosition = getSleepPosition();
        if (sleepPosition == null) {
            return true;
        }
        boolean insideRoost = sleepPosition.dimension().equals(dragon.level().dimension())
                && sleepPosition.pos().distSqr(dragon.blockPosition()) <= sleepRadiusSqr;
        return insideRoost
                && (dragon.isSleeping()
                || dragon.isSleepTransitioning()
                || sleepSettleTicks >= requiredSleepSettleTicks);
    }

    public boolean wantsToReturnToSleepSite() {
        return dragon.wantsToSleep()
                || (!dragon.isTame()
                && hasTerritory()
                && dragon.getSleepPreferences().canSleepDuringConditions(dragon.level()));
    }

    public int getSleepSettleTicks() {
        return sleepSettleTicks;
    }

    public boolean hasTerritory() {
        GlobalPos home = getHome();
        return !dragon.isTame()
                && home != null
                && home.dimension().equals(dragon.level().dimension());
    }

    public boolean isWithinTerritory(Vec3 position) {
        return isWithinTerritory(position, territoryRadius);
    }

    public boolean isWithinTerritory(Vec3 position, double radius) {
        GlobalPos home = getHome();
        if (dragon.isTame() || home == null || !home.dimension().equals(dragon.level().dimension())) {
            return true;
        }
        double dx = position.x - (home.pos().getX() + 0.5D);
        double dz = position.z - (home.pos().getZ() + 0.5D);
        return dx * dx + dz * dz <= radius * radius;
    }

    public boolean isOutsideTerritory() {
        return hasTerritory() && !isWithinTerritory(dragon.position());
    }

    public boolean isInsideStructure(Vec3 position) {
        if (!hasTerritory()
                || !isWithinTerritory(position)
                || !(dragon.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        return isValidStructureAt(serverLevel, BlockPos.containing(position));
    }

    public boolean shouldSuspendWandering() {
        return hasTerritory()
                && (isOutsideTerritory()
                || dragon.getSleepPreferences().canSleepDuringConditions(dragon.level()));
    }

    private void tryInitializeHomeFromContainingStructure() {
        if (dragon.isTame() || homeLookupAttempts >= MAX_HOME_LOOKUP_ATTEMPTS) {
            return;
        }
        if (homeLookupCooldown > 0) {
            homeLookupCooldown--;
            return;
        }
        homeLookupCooldown = HOME_LOOKUP_INTERVAL_TICKS;
        homeLookupAttempts++;

        if (!(dragon.level() instanceof ServerLevel serverLevel)
                || !isValidStructureAt(serverLevel, dragon.blockPosition())) {
            return;
        }
        setHome(GlobalPos.of(serverLevel.dimension(), dragon.blockPosition()));
    }

    private boolean isValidStructureAt(ServerLevel level, BlockPos position) {
        return getRoostAt(level, position) != null;
    }

    private void setHome(GlobalPos home) {
        dragon.setPersistenceRequired();
        dragon.getBrain().setMemory(DragonMemories.HOME, home);
        // Structure templates can retain old brain data, so always seed this roost's local fallback first.
        dragon.getBrain().setMemory(DragonMemories.ROOST_SLEEP_POSITION, home);
        homeLookupAttempts = MAX_HOME_LOOKUP_ATTEMPTS;
        sleepAnchorLookupCooldown = 0;
        sleepAnchorLookupAttempts = 0;
        sleepAnchorResolved = false;
    }

    private void tryResolveSleepAnchor(GlobalPos home) {
        if (sleepAnchorResolved || sleepAnchorLookupAttempts >= MAX_SLEEP_ANCHOR_LOOKUP_ATTEMPTS) {
            return;
        }
        if (sleepAnchorLookupCooldown > 0) {
            sleepAnchorLookupCooldown--;
            return;
        }
        sleepAnchorLookupCooldown = SLEEP_ANCHOR_LOOKUP_INTERVAL_TICKS;
        sleepAnchorLookupAttempts++;

        if (!(dragon.level() instanceof ServerLevel serverLevel)
                || !home.dimension().equals(serverLevel.dimension())) {
            return;
        }
        StructureStart roost = getRoostAt(serverLevel, home.pos());
        if (roost == null) {
            return;
        }

        BoundingBox bounds = roost.getBoundingBox();
        AABB searchBounds = new AABB(
                bounds.minX(),
                bounds.minY(),
                bounds.minZ(),
                bounds.maxX() + 1.0D,
                bounds.maxY() + 1.0D,
                bounds.maxZ() + 1.0D
        );
        Vec3 homeCenter = Vec3.atCenterOf(home.pos());
        Marker sleepAnchor = serverLevel.getEntitiesOfClass(
                        Marker.class,
                        searchBounds,
                        marker -> marker.getTags().contains(SLEEP_ANCHOR_TAG)
                ).stream()
                .min(Comparator
                        .comparingDouble((Marker marker) -> marker.distanceToSqr(homeCenter))
                        .thenComparingInt(Marker::getId))
                .orElse(null);
        if (sleepAnchor == null) {
            return;
        }

        dragon.getBrain().setMemory(
                DragonMemories.ROOST_SLEEP_POSITION,
                GlobalPos.of(home.dimension(), sleepAnchor.blockPosition())
        );
        sleepAnchorResolved = true;
        sleepAnchorLookupAttempts = MAX_SLEEP_ANCHOR_LOOKUP_ATTEMPTS;
    }

    @Nullable
    private StructureStart getRoostAt(ServerLevel level, BlockPos position) {
        StructureStart roost = level.structureManager().getStructureWithPieceAt(position, roostStructure);
        return roost != null && roost.isValid() ? roost : null;
    }

    private void ensureSleepPosition(GlobalPos home) {
        GlobalPos sleepPosition = dragon.getBrain().getMemory(DragonMemories.ROOST_SLEEP_POSITION)
                .filter(position -> position.dimension().equals(home.dimension()))
                .orElse(null);
        if (sleepPosition == null) {
            dragon.getBrain().setMemory(DragonMemories.ROOST_SLEEP_POSITION, home);
        }
    }

    @Nullable
    private GlobalPos getHome() {
        return dragon.getBrain().getMemory(DragonMemories.HOME).orElse(null);
    }

    @Nullable
    private GlobalPos getSleepPosition() {
        return dragon.getBrain().getMemory(DragonMemories.ROOST_SLEEP_POSITION)
                .orElseGet(this::getHome);
    }

    private void tickSleepReadiness() {
        if (dragon.isTame()) {
            sleepSettleTicks = 0;
            return;
        }

        GlobalPos sleepPosition = getSleepPosition();
        boolean insideRoost = sleepPosition != null
                && sleepPosition.dimension().equals(dragon.level().dimension())
                && sleepPosition.pos().distSqr(dragon.blockPosition()) <= sleepRadiusSqr;
        if (insideRoost && (dragon.isSleeping() || dragon.isSleepTransitioning())) {
            sleepSettleTicks = requiredSleepSettleTicks;
            return;
        }

        boolean settled = insideRoost
                && dragon.getSleepPreferences().canSleepDuringConditions(dragon.level())
                && dragon.onGround()
                && !dragon.isInWaterOrBubble()
                && dragon.getTarget() == null
                && dragon.getActiveAbility() == null;
        sleepSettleTicks = settled
                ? Math.min(requiredSleepSettleTicks, sleepSettleTicks + 1)
                : 0;
    }
}
