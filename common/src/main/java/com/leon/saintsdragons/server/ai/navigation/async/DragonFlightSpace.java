package com.leon.saintsdragons.server.ai.navigation.async;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DragonFlightSpace {
    private static final int VERTICAL_RANGE = 32;
    private static final int CACHE_TICKS = 10;
    private static final int CACHE_COLUMNS = 32;
    private static final int MAX_CORRIDOR_LENGTH = 256;
    private static final double[] DESTINATION_OFFSETS = {0, -2, 2, -4, 4, -8, 8};
    private final Mob dragon;
    private final Map<BlockPos, Column> columns = new LinkedHashMap<>();
    private long cacheTick = Long.MIN_VALUE;
    private double cachedWidth;
    private double cachedHeight;
    private String decision = "unobserved";
    private FlightClearance lastClearance;
    private BlockPos lastColumn;

    public DragonFlightSpace(Mob dragon) {
        this.dragon = dragon;
    }

    public @Nullable FlightClearance observe(Vec3 feet) {
        if (!finite(feet)) return null;
        long tick = dragon.level().getGameTime();
        if (tick - cacheTick >= CACHE_TICKS || tick < cacheTick
                || cachedWidth != dragon.getBbWidth() || cachedHeight != dragon.getBbHeight()) {
            columns.clear();
            cacheTick = tick;
            cachedWidth = dragon.getBbWidth();
            cachedHeight = dragon.getBbHeight();
        }
        BlockPos key = BlockPos.containing(feet);
        Column column = columns.get(key);
        if (column == null) {
            double radius = dragon.getBbWidth() * 0.5D + 1.0D;
            AABB scan = new AABB(key.getX() - radius, Math.max(dragon.level().getMinBuildHeight(), key.getY() - VERTICAL_RANGE),
                    key.getZ() - radius, key.getX() + 1 + radius,
                    Math.min(dragon.level().getMaxBuildHeight(), key.getY() + 1 + dragon.getBbHeight() + VERTICAL_RANGE),
                    key.getZ() + 1 + radius);
            if (!loaded(dragon, scan)) {
                decision = "unloaded-column";
                lastClearance = null;
                lastColumn = key;
                return null;
            }
            List<AABB> obstacles = new ArrayList<>();
            for (var shape : dragon.level().getBlockCollisions(dragon, scan)) obstacles.addAll(shape.toAabbs());
            column = new Column(scan.minY, scan.maxY, obstacles);
            if (columns.size() >= CACHE_COLUMNS) columns.remove(columns.keySet().iterator().next());
            columns.put(key, column);
        }
        lastClearance = FlightClearance.measure(bodyAt(feet), column.bottom, column.top, column.obstacles);
        lastColumn = key;
        return lastClearance;
    }

    public @Nullable Vec3 fitDestination(Vec3 desired) {
        if (!finite(desired)) return null;
        // Prefer the requested altitude, then bounded nearby adjustments. Routing still requires a clear path.
        for (double offset : DESTINATION_OFFSETS) {
            Vec3 seed = desired.add(0, offset, 0);
            FlightClearance space = observe(seed);
            if (space == null || !space.clear()) continue;
            double y = space.fitHeight(desired.y, dragon.getBbHeight(), 0.25D, 0.5D);
            if (!Double.isFinite(y) || Math.abs(y - desired.y) > 8.0D) continue;
            Vec3 fitted = new Vec3(desired.x, y, desired.z);
            if (fits(fitted)) {
                decision = fitted.distanceToSqr(desired) < 0.01D ? "destination-clear" : "destination-adjusted";
                return fitted;
            }
        }
        decision = "no-local-destination";
        return null;
    }

    public boolean fits(Vec3 feet) {
        if (!finite(feet)) return false;
        AABB body = bodyAt(feet).deflate(1.0E-4D);
        return loaded(dragon, body) && dragon.level().noCollision(dragon, body)
                && !dragon.level().containsAnyLiquid(body);
    }

    public boolean corridorClear(Vec3 from, Vec3 to) {
        return corridorClear(from, to, false);
    }

    /** Water-breach takeoffs need solid headroom checks even while the body is still submerged. */
    public boolean takeoffHeadroomClear(double lift) {
        return corridorClear(dragon.position(), dragon.position().add(0, lift, 0), true);
    }

    private boolean corridorClear(Vec3 from, Vec3 to, boolean allowFluids) {
        if (!finite(from) || !finite(to)) return false;
        double distance = from.distanceTo(to);
        if (distance > MAX_CORRIDOR_LENGTH) return false;
        int steps = Math.max(1, Mth.ceil(distance / 4.0D));
        Vec3 step = to.subtract(from).scale(1.0D / steps);
        for (int i = 0; i < steps; i++) {
            AABB body = bodyAt(from.add(step.scale(i))).deflate(1.0E-4D);
            AABB swept = body.expandTowards(step);
            if (!loaded(dragon, swept) || (!allowFluids && dragon.level().containsAnyLiquid(swept))
                    || !VoxelAabbSweeper.isClear(dragon.level(), dragon, body, step)) return false;
        }
        return true;
    }

    public boolean canTakeoff(double lift) {
        Vec3 from = dragon.position();
        Vec3 to = from.add(0, Math.max(2.0D, lift), 0);
        boolean clear = corridorClear(from, to) && fits(to);
        decision = clear ? "takeoff-clear" : "takeoff-blocked";
        return clear;
    }

    public @Nullable Vec3 findCruiseTarget(double maxTurnDegrees, double minRange, double extraRange,
                                          double maxHeightAboveFloor, boolean widerSearch) {
        Vec3 origin = dragon.position();
        FlightClearance local = observe(origin);
        if (local == null || !local.clear()) return null;
        boolean enclosed = local.ceilingKnown();
        double wantedY = local.floorKnown()
                ? local.floor() + Math.min(maxHeightAboveFloor, enclosed ? 5.0D : 15.0D + dragon.getRandom().nextDouble() * 20.0D)
                : origin.y;
        wantedY = Mth.clamp(wantedY, origin.y - 6.0D, origin.y + 6.0D);
        wantedY = local.fitHeight(wantedY, dragon.getBbHeight(), 1.5D, 1.5D);
        if (!Double.isFinite(wantedY)) {
            decision = "insufficient-flight-height";
            return null;
        }
        double yaw = Math.toRadians(dragon.getYRot());
        for (int attempt = 0; attempt < 16; attempt++) {
            // Nearby alternatives are also tried outdoors when long routes are obstructed.
            boolean nearby = enclosed || attempt >= 8;
            double range = nearby ? 6.0D + dragon.getRandom().nextDouble() * 18.0D
                    : Math.min(192.0D, minRange + dragon.getRandom().nextDouble() * extraRange);
            double turn = widerSearch || nearby ? 360.0D : maxTurnDegrees;
            double angle = yaw + Math.toRadians((dragon.getRandom().nextDouble() - 0.5D) * turn);
            Vec3 candidate = new Vec3(origin.x - Math.sin(angle) * range,
                    wantedY + (dragon.getRandom().nextDouble() - 0.5D) * 3.0D, origin.z + Math.cos(angle) * range);
            FlightClearance there = observe(candidate);
            if (there == null || !there.clear()) continue;
            double y = there.fitHeight(candidate.y, dragon.getBbHeight(), 1.5D, 1.5D);
            if (!Double.isFinite(y) || Math.abs(y - origin.y) > 8.0D) continue;
            candidate = new Vec3(candidate.x, y, candidate.z);
            if (fits(candidate) && corridorClear(origin, candidate)) {
                decision = enclosed ? "cruise-enclosed" : nearby ? "cruise-local" : "cruise-open";
                return candidate;
            }
        }
        decision = "no-clear-cruise-corridor";
        return null;
    }

    public static double heightAboveLocalFloor(Entity entity, double range) {
        Vec3 from = entity.position().add(0, 0.01D, 0);
        Vec3 to = from.add(0, -Math.min(96.0D, Math.max(1.0D, range)), 0);
        if (!loaded(entity, new AABB(from, to).inflate(0.01D))) return 0.0D;
        var hit = entity.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, entity));
        return hit.getType() == HitResult.Type.MISS ? from.y - to.y : Math.max(0, entity.getY() - hit.getLocation().y);
    }

    public static @Nullable BlockPos findLandingGround(Mob dragon, BlockPos column, int referenceY) {
        if (!dragon.level().hasChunkAt(column)) return null;
        int top = Math.min(referenceY, dragon.level().getMaxBuildHeight() - 1);
        // Skip empty sky using the already loaded column's heightmap. Inside caves the
        // surface is above us, so retain the bounded local downward scan.
        top = Math.min(top, dragon.level().getHeight(
                Heightmap.Types.MOTION_BLOCKING,
                column.getX(), column.getZ()) - 1);
        int bottom = Math.max(dragon.level().getMinBuildHeight(), top - 96);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(column.getX(), top, column.getZ());
        if (!dragon.level().hasChunkAt(cursor)) return null;
        for (int y = top; y >= bottom; y--) {
            cursor.setY(y);
            var state = dragon.level().getBlockState(cursor);
            if (!state.getFluidState().isEmpty()) return null;
            if (state.getCollisionShape(dragon.level(), cursor).isEmpty()) continue;
            return y < referenceY && state.isFaceSturdy(dragon.level(), cursor, Direction.UP) ? cursor.immutable() : null;
        }
        return null;
    }

    public String debugSummary() {
        return decision + ",sample=" + lastColumn + (lastClearance == null ? "" : ",floor=" + lastClearance.floor()
                + ",ceiling=" + lastClearance.ceiling() + ",floorKnown=" + lastClearance.floorKnown()
                + ",ceilingKnown=" + lastClearance.ceilingKnown() + ",clear=" + lastClearance.clear())
                + ",columns=" + columns.size();
    }

    private AABB bodyAt(Vec3 feet) {
        return dragon.getBoundingBox().move(feet.subtract(dragon.position()));
    }

    private static boolean finite(Vec3 v) {
        return v != null && Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }

    private static boolean loaded(Entity entity, AABB box) {
        if (box.minY < entity.level().getMinBuildHeight() || box.maxY > entity.level().getMaxBuildHeight()) return false;
        if (!entity.level().getWorldBorder().isWithinBounds(box)) return false;
        for (int x = Mth.floor(box.minX) >> 4; x <= Mth.floor(box.maxX) >> 4; x++) {
            for (int z = Mth.floor(box.minZ) >> 4; z <= Mth.floor(box.maxZ) >> 4; z++) {
                if (!entity.level().hasChunk(x, z)) return false;
            }
        }
        return true;
    }

    private record Column(double bottom, double top, List<AABB> obstacles) {}
}
