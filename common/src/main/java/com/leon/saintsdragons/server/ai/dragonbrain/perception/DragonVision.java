package com.leon.saintsdragons.server.ai.dragonbrain.perception;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public final class DragonVision {
    private static final int MAX_TRACKS = 32;
    private static final int MEMORY_TICKS = 80;
    private final DragonEntity dragon;
    private final Map<UUID, Contact> contacts = new LinkedHashMap<>();
    private final Map<UUID, Long> projectiles = new LinkedHashMap<>();
    private final List<Ray> rays = new ArrayList<>();
    private long lastTick = Long.MIN_VALUE;
    private long debugUntil = Long.MIN_VALUE;
    private int scanOffset;
    private Vec3 eye = Vec3.ZERO;
    private Vec3 forward = new Vec3(0, 0, 1);
    private DragonVisionProfile profile = DragonVisionProfile.standard(32, 2);
    private @Nullable ProjectileEvidence projectileEvidence;

    public DragonVision(DragonEntity dragon) { this.dragon = dragon; }

    public void tick() {
        if (!(dragon.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        if (lastTick == now) return;
        lastTick = now;
        eye = dragon.getEyePosition();
        forward = Vec3.directionFromRotation(dragon.getXRot(), dragon.getYHeadRot()).normalize();
        profile = dragon.getVisionProfile();
        rays.removeIf(ray -> now - ray.tick() > 8 || now > debugUntil);
        projectiles.entrySet().removeIf(entry -> now - entry.getValue() > MEMORY_TICKS);
        if (projectileEvidence != null && now - projectileEvidence.tick() > MEMORY_TICKS) projectileEvidence = null;
        for (Contact contact : contacts.values()) {
            if (now - contact.checkedAt > DragonVisionProfile.SCAN_INTERVAL) contact.clear = false;
            if (!contact.clear) contact.awareness = Math.max(0, contact.awareness - DragonVisionProfile.DECAY_PER_TICK);
            if (contact.awareness <= DragonVisionProfile.FORGET_RECOGNITION) contact.recognized = false;
        }
        contacts.values().removeIf(contact -> (contact.seenAt == Long.MIN_VALUE || now - contact.seenAt > MEMORY_TICKS)
                && now - contact.checkedAt > MEMORY_TICKS);
        if (!dragon.isAlive() || dragon.isDying() || dragon.isSleepLocked()) {
            contacts.clear();
            rays.clear();
            projectileEvidence = null;
            return;
        }

        LivingEntity target = dragon.getBrain().getMemory(DragonMemories.ATTACK_TARGET).orElse(null);
        if (eligible(target)) sample(target, now);
        if ((now + dragon.getId()) % DragonVisionProfile.SCAN_INTERVAL == 0) {
            List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class,
                    dragon.getBoundingBox().inflate(profile.range()),
                    entity -> eligible(entity) && eye.distanceToSqr(entity.getBoundingBox().getCenter()) <= profile.range() * profile.range());
            // Reserve two slots for nearby players; rotate the rest to avoid starvation.
            candidates.sort(Comparator.<LivingEntity>comparingInt(entity -> entity instanceof Player ? 0 : 1)
                    .thenComparingDouble(dragon::distanceToSqr));
            int priority = Math.min(2, candidates.size());
            for (int i = 0; i < priority; i++) sample(candidates.get(i), now);
            int rest = candidates.size() - priority;
            int count = Math.min(DragonVisionProfile.LIVING_BUDGET - priority, rest);
            for (int i = 0; i < count; i++) sample(candidates.get(priority + (scanOffset + i) % rest), now);
            if (rest > 0) scanOffset = (scanOffset + count) % rest;
        }
        sampleProjectiles(level, now);
    }

    private boolean eligible(@Nullable LivingEntity entity) {
        return entity != null && entity != dragon && entity.isAlive() && !entity.isSpectator()
                && entity.level() == dragon.level() && !entity.isInvisible()
                && !entity.isPassengerOfSameVehicle(dragon)
                && !(entity instanceof Player player && player.isCreative());
    }

    private Contact contact(LivingEntity entity) {
        Contact contact = contacts.get(entity.getUUID());
        if (contact != null) {
            contact.entityId = entity.getId();
            return contact;
        }
        if (contacts.size() >= MAX_TRACKS) {
            UUID oldest = contacts.entrySet().stream().min(Comparator.comparingLong(e -> e.getValue().checkedAt))
                    .orElseThrow().getKey();
            contacts.remove(oldest);
        }
        contact = new Contact(entity.getId());
        contacts.put(entity.getUUID(), contact);
        return contact;
    }

    private void sample(LivingEntity entity, long now) {
        Contact contact = contact(entity);
        if (contact.checkedAt == now) return;
        long elapsed = contact.clear ? Math.min(DragonVisionProfile.SCAN_INTERVAL, now - contact.checkedAt) : 1;
        contact.checkedAt = now;
        boolean retained = contact.recognized || dragon.getBrain().getMemory(DragonMemories.ATTACK_TARGET).orElse(null) == entity;
        Vec3 center = entity.getBoundingBox().getCenter();
        Vec3[] points = {entity.getEyePosition(), center,
                new Vec3(center.x, entity.getY() + entity.getBbHeight() * 0.2, center.z)};
        Vec3 seen = null;
        boolean inView = false;
        for (Vec3 point : points) {
            if (!profile.contains(eye, forward, point, retained)) continue;
            inView = true;
            if (clearRay(point, now)) { seen = point; break; }
        }
        contact.clear = seen != null;
        contact.reason = !inView ? "outside-view" : seen == null ? "occluded" : "visible";
        if (seen == null) return;
        contact.position = seen;
        contact.seenAt = now;
        double distance = eye.distanceTo(seen);
        double centrality = Math.max(0.15, forward.dot(seen.subtract(eye).normalize()));
        double proximity = Math.max(0, 1 - distance / profile.range());
        DragonTargetTrack track = DragonTargetMemory.get(dragon.getBrain());
        boolean familiarThreat = track != null && track.sourceUuid().equals(entity.getUUID()) && track.isActive(now);
        double gain = familiarThreat || retained ? 0.35 : (0.025 + 0.10 * proximity) * centrality;
        contact.awareness = Math.min(1, contact.awareness + (float)(gain * elapsed));
        if (contact.awareness >= DragonVisionProfile.RECOGNITION) contact.recognized = true;
    }

    private boolean clearRay(Vec3 point, long now) {
        if (!dragon.level().hasChunkAt(BlockPos.containing(point))) return false;
        HitResult hit = dragon.level().clip(new ClipContext(eye, point, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, dragon));
        boolean clear = hit.getType() == HitResult.Type.MISS;
        if (now <= debugUntil) {
            if (rays.size() >= 64) rays.remove(0);
            rays.add(new Ray(eye, clear ? point : hit.getLocation(), clear, now));
        }
        return clear;
    }

    public boolean recognizes(LivingEntity entity) {
        Contact contact = contacts.get(entity.getUUID());
        return eligible(entity) && contact != null && contact.recognized && canSee(entity);
    }

    public boolean canSee(LivingEntity entity) {
        Contact contact = contacts.get(entity.getUUID());
        return eligible(entity) && contact != null && contact.clear
                && dragon.level().getGameTime() - contact.checkedAt <= DragonVisionProfile.SCAN_INTERVAL
                && contact.position != null && profile.contains(eye, forward, contact.position, contact.recognized
                || dragon.getBrain().getMemory(DragonMemories.ATTACK_TARGET).orElse(null) == entity);
    }

    public <E extends LivingEntity> @Nullable E nearest(Class<E> type, Predicate<E> predicate) {
        return nearest(type, predicate, true);
    }

    public <E extends LivingEntity> @Nullable E nearestVisible(Class<E> type, Predicate<E> predicate) {
        return nearest(type, predicate, false);
    }

    private <E extends LivingEntity> @Nullable E nearest(Class<E> type, Predicate<E> predicate, boolean combat) {
        if (!(dragon.level() instanceof ServerLevel level)) return null;
        TargetingConditions conditions = (combat ? TargetingConditions.forCombat() : TargetingConditions.forNonCombat())
                .range(profile.range()).ignoreLineOfSight();
        E nearest = null;
        double distance = Double.MAX_VALUE;
        for (Contact contact : contacts.values()) {
            Entity entity = level.getEntity(contact.entityId);
            if (!type.isInstance(entity)) continue;
            E candidate = type.cast(entity);
            if (!recognizes(candidate) || !predicate.test(candidate) || (combat && !dragon.canTarget(candidate))
                    || !conditions.test(dragon, candidate)) continue;
            double next = dragon.distanceToSqr(candidate);
            if (next < distance) { nearest = candidate; distance = next; }
        }
        return nearest;
    }

    public @Nullable Vec3 attentionPosition() {
        return contacts.values().stream().filter(c -> c.position != null && c.clear
                        && c.awareness >= DragonVisionProfile.SUSPICION && !c.recognized
                        && profile.contains(eye, forward, c.position, false))
                .max(Comparator.comparingDouble(c -> c.awareness)).map(c -> c.position).orElse(null);
    }

    private void sampleProjectiles(ServerLevel level, long now) {
        List<Projectile> nearby = level.getEntitiesOfClass(Projectile.class,
                dragon.getBoundingBox().inflate(DragonVisionProfile.PROJECTILE_RANGE),
                p -> p.isAlive() && canInvestigateProjectile(p) && !projectiles.containsKey(p.getUUID())
                        && p.getDeltaMovement().lengthSqr() > 0.04);
        nearby.sort(Comparator.comparingDouble(dragon::distanceToSqr));
        int checked = 0;
        for (Projectile projectile : nearby) {
            Vec3 position = projectile.position();
            Vec3 velocity = projectile.getDeltaMovement();
            Vec3 relativeVelocity = velocity.subtract(dragon.getDeltaMovement());
            Vec3 toDragon = dragon.getBoundingBox().getCenter().subtract(position);
            AABB danger = dragon.getBoundingBox().inflate(2);
            Vec3 predicted = position.add(relativeVelocity.scale(DragonVisionProfile.PROJECTILE_HORIZON));
            Vec3 previous = previousPosition(projectile, position, velocity);
            boolean passing = danger.contains(position) || danger.clip(previous, position).isPresent();
            boolean approaching = toDragon.dot(relativeVelocity) > 0 && danger.clip(position, predicted).isPresent();
            if (!passing && !approaching) continue;
            if (checked++ >= DragonVisionProfile.PROJECTILE_BUDGET) break;
            Vec3 observed = null;
            // Only the already-travelled segment counts as visual evidence. The future segment is prediction.
            for (Vec3 point : new Vec3[]{position, previous.lerp(position, 0.5), previous}) {
                if (profile.contains(eye, forward, point, false) && clearRay(point, now)) { observed = point; break; }
            }
            if (observed == null) continue;
            rememberProjectile(projectile, previous, observed, velocity, now);
        }
    }

    private static Vec3 previousPosition(Projectile projectile, Vec3 position, Vec3 velocity) {
        Vec3 previous = new Vec3(projectile.xo, projectile.yo, projectile.zo);
        // A new or teleported projectile has no trustworthy swept segment yet.
        return projectile.tickCount <= 1 || previous.distanceToSqr(position) > Math.max(4, velocity.lengthSqr() * 4)
                ? position : previous;
    }

    private void rememberProjectile(Projectile projectile, Vec3 previous, Vec3 observed, Vec3 velocity, long now) {
        projectiles.put(projectile.getUUID(), now);
        if (projectiles.size() > MAX_TRACKS) projectiles.remove(projectiles.keySet().iterator().next());
        Vec3 direction = velocity.scale(-1).normalize();
        float confidence = 0.65F;
        if (projectileEvidence != null && now - projectileEvidence.tick() < 60
                && projectileEvidence.direction().dot(direction) > 0.85) {
            direction = projectileEvidence.direction().scale(0.4).add(direction.scale(0.6)).normalize();
            confidence = Math.min(0.85F, projectileEvidence.confidence() + 0.1F);
        }
        // Back-project a bounded bearing. Gravity makes this a search region, never an exact muzzle position.
        Vec3 origin = observed.add(direction.scale(Math.min(24, profile.range() * 0.5)));
        double uncertainty = 4 + (1 - confidence) * 10;
        Vec3 relativeVelocity = velocity.subtract(dragon.getDeltaMovement());
        double approach = Math.max(0, dragon.getBoundingBox().getCenter().subtract(observed).dot(relativeVelocity)
                / Math.max(0.01, relativeVelocity.lengthSqr()));
        projectileEvidence = new ProjectileEvidence(previous, observed, observed.add(velocity.scale(4)),
                origin, direction, uncertainty, confidence, approach, now);
        Entity owner = projectile.getOwner();
        UUID source = owner instanceof LivingEntity living && recognizes(living) ? owner.getUUID() : null;
        DragonSensoryObservation observation = new DragonSensoryObservation(origin, source,
                DragonSensoryObservation.Kind.PROJECTILE, confidence, now);
        DragonInvestigation.remember(dragon, observation);
        DragonAwarenessMemory awareness = DragonAwarenessMemory.get(dragon);
        awareness.rememberSound(observation, true, now);
        if (source != null) awareness.rememberProjectileImpact(source, projectile.getUUID(), now);
    }

    private boolean canInvestigateProjectile(Projectile projectile) {
        Entity owner = projectile.getOwner();
        return owner == null || owner != dragon && !owner.isPassengerOfSameVehicle(dragon) && dragon.canTarget(owner);
    }

    public boolean observeProjectileImpact(Projectile projectile, boolean hitDragon) {
        if (!(dragon.level() instanceof ServerLevel) || !canInvestigateProjectile(projectile)) return false;
        if (projectiles.containsKey(projectile.getUUID())) return true;
        long now = dragon.level().getGameTime();
        Vec3 position = projectile.position();
        if (!hitDragon && (dragon.isSleepLocked()
                || !profile.contains(eye, forward, position, false) || !clearRay(position, now))) return false;
        Vec3 velocity = projectile.getDeltaMovement();
        Vec3 previous = previousPosition(projectile, position, velocity);
        if (velocity.lengthSqr() < 1.0E-4) velocity = position.subtract(previous);
        if (velocity.lengthSqr() < 1.0E-4) return false;
        rememberProjectile(projectile, previous, position, velocity, now);
        return true;
    }

    public void enableDebug(long now) { debugUntil = now + 12; }
    public Vec3 eye() { return eye; }
    public Vec3 forward() { return forward; }
    public DragonVisionProfile profile() { return profile; }
    public List<Ray> rays() { return List.copyOf(rays); }
    public @Nullable ProjectileEvidence projectileEvidence() { return projectileEvidence; }
    public List<Marker> markers() {
        return contacts.values().stream().filter(c -> c.position != null)
                .map(c -> new Marker(c.position, c.awareness, c.recognized, c.reason,
                        dragon.level().getGameTime() - c.seenAt)).toList();
    }
    public String debugSummary() {
        return "contacts=" + contacts.size() + ",recognized=" + contacts.values().stream().filter(c -> c.recognized).count()
                + ",target={" + targetDebugSummary() + "}"
                + ",projectile=" + (projectileEvidence == null ? "none" : projectileEvidence.origin());
    }

    private String targetDebugSummary() {
        LivingEntity target = dragon.getBrain().getMemory(DragonMemories.ATTACK_TARGET).orElse(null);
        if (target == null) return "none";
        Contact contact = contacts.get(target.getUUID());
        if (!eligible(target)) return "id=" + target.getId() + ",reason=ineligible";
        if (contact == null) return "id=" + target.getId() + ",reason=not-sampled";
        long now = dragon.level().getGameTime();
        long checkedAge = now - contact.checkedAt;
        String reason = checkedAge > DragonVisionProfile.SCAN_INTERVAL ? "stale-sample" : contact.reason;
        return "id=" + target.getId() + ",reason=" + reason
                + ",awareness=" + String.format(Locale.ROOT, "%.2f", contact.awareness)
                + ",recognized=" + contact.recognized + ",checkedAge=" + checkedAge
                + ",seenAge=" + (contact.seenAt == Long.MIN_VALUE ? "never" : now - contact.seenAt);
    }

    private static final class Contact {
        int entityId;
        long checkedAt = Long.MIN_VALUE, seenAt = Long.MIN_VALUE;
        float awareness;
        boolean clear, recognized;
        @Nullable Vec3 position;
        String reason = "unseen";
        Contact(int entityId) { this.entityId = entityId; }
    }
    public record Ray(Vec3 from, Vec3 to, boolean clear, long tick) {}
    public record Marker(Vec3 position, float awareness, boolean recognized, String reason, long age) {}
    public record ProjectileEvidence(Vec3 previous, Vec3 observed, Vec3 predicted, Vec3 origin, Vec3 direction,
                                     double uncertainty, float confidence, double impactTicks, long tick) {}
}
