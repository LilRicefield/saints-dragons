package com.leon.saintsdragons.common.particle;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Server collision geometry. Each square section travels independently of its emitter. */
public final class ExpandingBreathSection {
    public static final double DEFAULT_RANGE = 96;
    public static final double DEFAULT_SPEED = 4;
    public static final int MAX_TICKS = 40;
    public static final int GRID_SIZE = 5;
    public static final int LANE_COUNT = GRID_SIZE * GRID_SIZE;
    private static final double START_HALF_WIDTH = 0.6;
    private static final double MAX_HALF_WIDTH = 8;
    private static final double GROWTH_PER_BLOCK = 0.3;
    private static final double THICKNESS = 0.08;

    private final Vec3 origin;
    private final Vec3 forward;
    private final Vec3 right;
    private final Vec3 up;
    private double speed;
    private final double range;
    private final Profile profile;
    private int age;
    private final boolean[] open = new boolean[LANE_COUNT];
    private double distance;
    private int activeLanes = LANE_COUNT;

    public ExpandingBreathSection(Vec3 origin, Vec3 velocity, double range) {
        this(origin, velocity.lengthSqr() < 1.0E-8 ? new Vec3(0, 0, 0.5)
                        : velocity.normalize().scale(Math.max(0.5, Math.min(12, velocity.length()))),
                Math.max(1, Math.min(96, Math.min(range, Math.max(0.5, Math.min(12, velocity.length())) * MAX_TICKS))),
                Profile.FIRE);
    }

    public ExpandingBreathSection(Vec3 origin, Vec3 velocity, double range, Profile profile) {
        this.origin = origin;
        this.speed = velocity.length();
        this.profile = profile;
        this.forward = velocity.lengthSqr() < 1.0E-8 ? new Vec3(0, 0, 1) : velocity.normalize();
        Vec3 reference = Math.abs(forward.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        this.right = forward.cross(reference).normalize();
        this.up = right.cross(forward).normalize();
        this.range = Math.max(0, range);
        Arrays.fill(open, true);
    }

    public double range() {
        return range;
    }

    public double distance() {
        return distance;
    }

    public boolean finished() {
        return (speed > 0 && distance >= range) || age >= profile.maxTicks() || activeLanes == 0;
    }

    public static double halfWidth(double distance) {
        return Math.min(MAX_HALF_WIDTH, START_HALF_WIDTH + Math.max(0, distance) * GROWTH_PER_BLOCK);
    }

    /** Collision shape and travel rules; particle appearance is deliberately separate. */
    public record Profile(double startHalfWidth, double maxHalfWidth, double growthPerBlock,
                          int maxTicks, double drag, TagKey<Fluid> blockingFluid) {
        public static final Profile FIRE = new Profile(START_HALF_WIDTH, MAX_HALF_WIDTH,
                GROWTH_PER_BLOCK, MAX_TICKS, 1.0, null);

        public Profile {
            if (!Double.isFinite(startHalfWidth) || startHalfWidth <= 0
                    || !Double.isFinite(maxHalfWidth) || maxHalfWidth < startHalfWidth
                    || !Double.isFinite(growthPerBlock) || growthPerBlock < 0
                    || maxTicks < 1 || !Double.isFinite(drag) || drag <= 0 || drag > 1) {
                throw new IllegalArgumentException("Invalid breath profile");
            }
        }

        public double halfWidthAt(double distance) {
            return Math.min(maxHalfWidth, startHalfWidth + Math.max(0, distance) * growthPerBlock);
        }
    }

    public Vec3 center() {
        return origin.add(forward.scale(distance));
    }

    public Vec3 lanePosition(int lane, double atDistance) {
        double u = (2.0 * (lane % GRID_SIZE) + 1) / GRID_SIZE - 1;
        double v = (2.0 * (lane / GRID_SIZE) + 1) / GRID_SIZE - 1;
        return origin.add(forward.scale(atDistance))
                .add(right.scale(u * profile.halfWidthAt(atDistance))).add(up.scale(v * profile.halfWidthAt(atDistance)));
    }

    public AABB nextBounds() {
        double next = Math.min(range, distance + speed);
        double half = profile.halfWidthAt(next);
        Vec3 extent = extent(half);
        return new AABB(center(), origin.add(forward.scale(next))).inflate(extent.x, extent.y, extent.z);
    }

    /** Gather terrain once per section, rather than performing a world lookup for every lane. */
    public List<Sweep> advance(Level level) {
        if (finished()) return List.of();
        AABB bounds = nextBounds();
        if (!level.hasChunksAt(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ),
                BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ))) {
            activeLanes = 0;
            return List.of();
        }
        List<AABB> obstacles = new ArrayList<>();
        List<BlockPos> positions = new ArrayList<>();
        BlockCollisions<TerrainShape> collisions = new BlockCollisions<>(level, null, bounds, false,
                (pos, shape) -> new TerrainShape(pos.immutable(), shape));
        while (collisions.hasNext()) {
            TerrainShape terrain = collisions.next();
            for (AABB box : terrain.shape().toAabbs()) {
                obstacles.add(box);
                positions.add(terrain.pos());
            }
        }
        if (profile.blockingFluid() != null) {
            for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ),
                    BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ))) {
                var fluid = level.getFluidState(pos);
                if (!fluid.is(profile.blockingFluid())) continue;
                for (AABB box : fluid.getShape(level, pos).toAabbs()) {
                    obstacles.add(box.move(pos));
                    positions.add(pos.immutable());
                }
            }
        }
        return advance(obstacles, positions);
    }

    public List<Sweep> advance(List<AABB> obstacles) {
        return advance(obstacles, null);
    }

    private record TerrainShape(BlockPos pos, VoxelShape shape) {}

    private List<Sweep> advance(List<AABB> obstacles, List<BlockPos> positions) {
        if (finished()) {
            return List.of();
        }
        double next = Math.min(range, distance + speed);
        double fromHalf = profile.halfWidthAt(distance);
        double toHalf = profile.halfWidthAt(next);
        Vec3 fromExtent = extent(fromHalf / GRID_SIZE);
        Vec3 toExtent = extent(toHalf / GRID_SIZE);
        List<Sweep> sweeps = new ArrayList<>(activeLanes);
        for (int lane = 0; lane < LANE_COUNT; lane++) {
            if (!open[lane]) continue;
            double u = (2.0 * (lane % GRID_SIZE) + 1) / GRID_SIZE - 1;
            double v = (2.0 * (lane / GRID_SIZE) + 1) / GRID_SIZE - 1;
            Vec3 offset = right.scale(u).add(up.scale(v));
            Vec3 start = center().add(offset.scale(fromHalf));
            Vec3 end = origin.add(forward.scale(next)).add(offset.scale(toHalf));
            double stop = Double.POSITIVE_INFINITY;
            AABB blocker = null;
            BlockPos blockPos = null;
            for (int i = 0; i < obstacles.size(); i++) {
                AABB obstacle = obstacles.get(i);
                double contact = contactFraction(start, end, fromExtent, toExtent, obstacle);
                if (contact < stop) {
                    stop = contact;
                    blocker = obstacle;
                    blockPos = positions == null ? null : positions.get(i);
                }
            }
            Vec3 impact = null;
            if (blocker != null) {
                open[lane] = false;
                activeLanes--;
                Vec3 point = start.lerp(end, stop);
                impact = new Vec3(clamp(point.x, blocker.minX, blocker.maxX),
                        clamp(point.y, blocker.minY, blocker.maxY), clamp(point.z, blocker.minZ, blocker.maxZ));
            }
            sweeps.add(new Sweep(lane, start, end, fromExtent, toExtent, stop, impact, blockPos));
        }
        distance = next;
        speed *= profile.drag();
        age++;
        return sweeps;
    }

    // World-axis bounds of a square tile perpendicular to the beam. This is conservative
    // for diagonally oriented tiles; terrain uses exactly the same footprint as entity hits.
    private Vec3 extent(double half) {
        return new Vec3((Math.abs(right.x) + Math.abs(up.x)) * half + Math.abs(forward.x) * THICKNESS,
                (Math.abs(right.y) + Math.abs(up.y)) * half + Math.abs(forward.y) * THICKNESS,
                (Math.abs(right.z) + Math.abs(up.z)) * half + Math.abs(forward.z) * THICKNESS);
    }

    /** Continuous intersection, including growth during a tick and targets containing the start. */
    public static double contactFraction(Vec3 start, Vec3 end, Vec3 fromHalf, Vec3 toHalf, AABB box) {
        double[] interval = {0, 1};
        if (!clipAxis(start.x, end.x - start.x, fromHalf.x, toHalf.x - fromHalf.x, box.minX, box.maxX, interval)
                || !clipAxis(start.y, end.y - start.y, fromHalf.y, toHalf.y - fromHalf.y, box.minY, box.maxY, interval)
                || !clipAxis(start.z, end.z - start.z, fromHalf.z, toHalf.z - fromHalf.z, box.minZ, box.maxZ, interval)) {
            return Double.POSITIVE_INFINITY;
        }
        return interval[0];
    }

    private static boolean clipAxis(double start, double delta, double half, double growth,
                                    double min, double max, double[] interval) {
        return clipPositive(start + half - min, delta + growth, interval)
                && clipPositive(max - start + half, growth - delta, interval);
    }

    // Intersect the time interval with the linear inequality value + slope * t >= 0.
    private static boolean clipPositive(double value, double slope, double[] interval) {
        if (Math.abs(slope) < 1.0E-10) return value >= 0;
        double time = -value / slope;
        if (slope > 0) interval[0] = Math.max(interval[0], time);
        else interval[1] = Math.min(interval[1], time);
        return interval[0] <= interval[1];
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record Sweep(int lane, Vec3 start, Vec3 end, Vec3 fromHalf, Vec3 toHalf,
                        double blockFraction, Vec3 blockImpact, BlockPos blockPos) {
        public Sweep(int lane, Vec3 start, Vec3 end, Vec3 fromHalf, Vec3 toHalf,
                     double blockFraction, Vec3 blockImpact) {
            this(lane, start, end, fromHalf, toHalf, blockFraction, blockImpact, null);
        }
        public boolean hits(AABB target) {
            double contact = contactFraction(start, end, fromHalf, toHalf, target);
            // Terrain wins ties. In particular, a tile starting inside a wall cannot damage through it.
            return contact <= 1 && contact < blockFraction;
        }

        public Vec3 visibleEnd() {
            return start.lerp(end, Math.min(1, blockFraction));
        }
    }
}
