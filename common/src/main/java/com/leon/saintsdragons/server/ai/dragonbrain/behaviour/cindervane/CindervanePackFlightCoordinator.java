package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.cindervane;

import com.leon.saintsdragons.common.config.dragon.profile.CindervaneStatProfile.PackFlightCoordinator;
import com.leon.saintsdragons.server.ai.navigation.async.DragonFlightRequest;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Leader-owned, transient formation state. No global cache or retained entity references. */
public final class CindervanePackFlightCoordinator {
    private final Map<UUID, Slot> slots = new HashMap<>();
    private long nextCleanup;
    private long lastLeaderUpdate = Long.MIN_VALUE;
    private float headingYaw;
    private Vec3 leaderVelocity = Vec3.ZERO;
    private boolean leaderMoving;

    public FollowPlan plan(ServerLevel level, Cindervane leader, Cindervane member) {
        long now = level.getGameTime();
        updateLeader(leader, now);
        if (now >= nextCleanup) {
            // Keep surviving members in their slots when another member leaves or joins.
            slots.keySet().removeIf(id -> !(level.getEntity(id) instanceof Cindervane other)
                    || !other.canParticipateInPack()
                    || !leader.getUUID().equals(other.getPackLeaderUuid())
                    || leader.distanceToSqr(other) > Math.pow(leader.getPackSearchRadius() * 2.0D, 2));
            nextCleanup = now + PackFlightCoordinator.SLOT_CLEANUP_INTERVAL_TICKS;
        }
        Slot slot = slots.computeIfAbsent(member.getUUID(), id -> new Slot(firstFreeSlot()));
        Vec3 position = slotPosition(leader, slot.index);
        Vec3 error = position.subtract(member.position());
        double distance = error.length();
        slot.catchUp = distance > (slot.catchUp
                ? PackFlightCoordinator.CATCH_UP_END_DISTANCE : PackFlightCoordinator.CATCH_UP_START_DISTANCE);

        double flightSpeed = Math.max(0.01D, member.getFlightSpeed());
        Vec3 correction = distance > PackFlightCoordinator.SLOT_DEADBAND
                ? error.scale(Math.min(
                        (distance - PackFlightCoordinator.SLOT_DEADBAND) * PackFlightCoordinator.SLOT_CORRECTION_GAIN,
                        flightSpeed * PackFlightCoordinator.MAX_SLOT_CORRECTION_SPEED) / distance)
                : Vec3.ZERO;
        Vec3 separation = separation(level, member, flightSpeed);
        Vec3 formationVelocity = leaderVelocity.add(correction);
        if (leaderMoving && leaderVelocity.horizontalDistanceSqr() > 0.0025D) {
            // An overtaking follower slows down instead of turning back across the pack.
            Vec3 forward = leaderVelocity.multiply(1.0D, 0.0D, 1.0D).normalize();
            double minForward = leaderVelocity.horizontalDistance()
                    * PackFlightCoordinator.MIN_FORWARD_SPEED_FRACTION;
            double forwardSpeed = formationVelocity.dot(forward);
            if (forwardSpeed < minForward) {
                formationVelocity = formationVelocity.add(forward.scale(minForward - forwardSpeed));
            }
        }
        Vec3 desiredVelocity = limit(formationVelocity.add(separation),
                flightSpeed * PackFlightCoordinator.MAX_SPEED_MODIFIER);

        boolean settled = !leaderMoving && distance <= PackFlightCoordinator.SLOT_DEADBAND
                && separation.lengthSqr() < 1.0E-4D;
        Vec3 target;
        DragonFlightRequest request;
        String phase;
        if (!leaderMoving) {
            // A hovering leader has a real arrival point; a cruising leader never does.
            target = position.add(separation.scale(PackFlightCoordinator.LOOK_AHEAD_TICKS));
            double speed = Mth.clamp(desiredVelocity.length() / flightSpeed, 0.25D,
                    PackFlightCoordinator.MAX_SPEED_MODIFIER);
            request = DragonFlightRequest.track(target, speed, PackFlightCoordinator.SLOT_DEADBAND);
            phase = settled ? "holding" : "joining-hover";
        } else {
            double speed = desiredVelocity.length();
            Vec3 direction = speed > 1.0E-4D ? desiredVelocity.scale(1.0D / speed)
                    : Vec3.directionFromRotation(0.0F, headingYaw);
            double lookAhead = Mth.clamp(speed * PackFlightCoordinator.LOOK_AHEAD_TICKS, 8.0D, 28.0D);
            target = member.position().add(direction.scale(lookAhead));
            request = new DragonFlightRequest(target, speed / flightSpeed,
                    slot.catchUp ? DragonFlightRequest.Purpose.TRACK : DragonFlightRequest.Purpose.CRUISE,
                    1.0D, DragonFlightRequest.Arrival.PASS_THROUGH);
            phase = slot.catchUp ? "catching-up" : "formation";
        }
        return new FollowPlan(leader.getUUID(), slot.index, position, distance, separation.length(),
                slot.catchUp, settled, phase, request);
    }

    private int firstFreeSlot() {
        int index = 0;
        while (slotOccupied(index)) index++;
        return index;
    }

    private boolean slotOccupied(int index) {
        for (Slot slot : slots.values()) {
            if (slot.index == index) return true;
        }
        return false;
    }

    private void updateLeader(Cindervane leader, long now) {
        if (lastLeaderUpdate == now) return;
        Vec3 velocity = leader.getDeltaMovement();
        boolean reset = lastLeaderUpdate == Long.MIN_VALUE || now - lastLeaderUpdate > 20L;
        int elapsed = reset ? 1 : (int) Math.max(1L, now - lastLeaderUpdate);
        float desiredYaw = velocity.horizontalDistanceSqr() > 0.01D
                ? (float) Math.toDegrees(Math.atan2(-velocity.x, velocity.z))
                : reset ? leader.getYRot() : headingYaw;
        if (reset) {
            headingYaw = desiredYaw;
            leaderVelocity = velocity;
        } else {
            float turn = PackFlightCoordinator.HEADING_TURN_DEGREES_PER_TICK * elapsed;
            headingYaw += Mth.clamp(Mth.wrapDegrees(desiredYaw - headingYaw), -turn, turn);
            double blend = 1.0D - Math.pow(1.0D - PackFlightCoordinator.LEADER_VELOCITY_BLEND, elapsed);
            leaderVelocity = leaderVelocity.lerp(velocity, blend);
        }
        leaderMoving = leaderVelocity.lengthSqr() > (leaderMoving ? 0.0025D : 0.01D);
        lastLeaderUpdate = now;
    }

    private Vec3 slotPosition(Cindervane leader, int index) {
        // Two wing positions and a rear position; additional rows also remain unique.
        int row = index / 3;
        int side = index % 3;
        double lateral = side == 2 ? 0.0D : (side == 0 ? -1.0D : 1.0D)
                * PackFlightCoordinator.SLOT_LATERAL_SPACING * (row + 1);
        double trailing = PackFlightCoordinator.SLOT_TRAILING_DISTANCE
                + (row * 2 + (side == 2 ? 1 : 0)) * PackFlightCoordinator.SLOT_ROW_SPACING;
        Vec3 forward = Vec3.directionFromRotation(0.0F, headingYaw);
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        return leader.position().subtract(forward.scale(trailing)).add(right.scale(lateral))
                .add(0.0D, leader.getBbHeight() + PackFlightCoordinator.SLOT_HEIGHT + row, 0.0D);
    }

    private Vec3 separation(ServerLevel level, Cindervane member, double flightSpeed) {
        double range = PackFlightCoordinator.SEPARATION_RANGE;
        Vec3 separation = Vec3.ZERO;
        // Nearby packs may need collision clearance, but never contribute cohesion or alignment.
        for (Cindervane other : level.getEntitiesOfClass(Cindervane.class,
                member.getBoundingBox().inflate(range), other -> other != member
                        && other.isAlive() && !other.isRemoved() && other.isAerial())) {
            Vec3 away = member.position().subtract(other.position());
            double distance = away.length();
            double clearance = Math.min(range, (member.getBbWidth() + other.getBbWidth()) * 0.5D
                    + PackFlightCoordinator.SEPARATION_PADDING);
            if (distance >= clearance) continue;
            Vec3 direction = distance > 1.0E-4D ? away.scale(1.0D / distance)
                    : new Vec3(member.getUUID().compareTo(other.getUUID()) < 0 ? -1.0D : 1.0D, 0.0D, 0.0D);
            double strength = 1.0D - distance / clearance;
            separation = separation.add(direction.scale(strength * strength));
        }
        // Preserve the falloff instead of normalizing every small concern to full strength.
        return limit(separation, 1.0D).scale(flightSpeed * PackFlightCoordinator.MAX_SEPARATION_SPEED);
    }

    private static Vec3 limit(Vec3 vector, double maxLength) {
        double length = vector.length();
        return length > maxLength ? vector.scale(maxLength / length) : vector;
    }

    private static final class Slot {
        private final int index;
        private boolean catchUp;

        private Slot(int index) {
            this.index = index;
        }
    }

    public record FollowPlan(UUID leader, int slot, Vec3 slotPosition, double slotError,
                             double separation, boolean catchUp, boolean settled,
                             String phase, DragonFlightRequest request) {
        public String debugSummary() {
            return String.format(Locale.ROOT,
                    "phase=%s,leader=%s,slot=%d,slotPos=(%.1f,%.1f,%.1f),error=%.2f,separation=%.3f,catchUp=%s,speed=%.2f,target=(%.1f,%.1f,%.1f)",
                    phase, leader, slot, slotPosition.x, slotPosition.y, slotPosition.z, slotError, separation,
                    catchUp, request.speedModifier(), request.target().x, request.target().y, request.target().z);
        }
    }
}
