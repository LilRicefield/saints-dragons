package com.leon.saintsdragons.server.ai.dragonbrain.perception;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class DragonHearingListener implements GameEventListener {
    private static final float MIN_CONFIDENCE = 0.08F;
    private static final double MIN_PROJECTILE_DANGER_RADIUS = 6.0D;
    private static final double PROJECTILE_DANGER_VERTICAL_RADIUS = 4.0D;

    private final DragonEntity dragon;
    private final PositionSource positionSource;

    public DragonHearingListener(DragonEntity dragon) {
        this.dragon = dragon;
        this.positionSource = new EntityPositionSource(dragon, dragon.getEyeHeight() * 0.5F);
    }

    @Override
    public PositionSource getListenerSource() {
        return positionSource;
    }

    @Override
    public int getListenerRadius() {
        return DragonPerceptionProfile.forDragon(dragon).hearingRange();
    }

    @Override
    public DeliveryMode getDeliveryMode() {
        return DeliveryMode.BY_DISTANCE;
    }

    @Override
    public boolean handleGameEvent(ServerLevel level,
                                   GameEvent event,
                                   GameEvent.Context eventContext,
                                   Vec3 eventPosition) {
        if (!dragon.isAlive() || dragon.isDying() || dragon.isRemoved()) {
            return false;
        }

        Entity eventSource = eventContext.sourceEntity();
        Entity source = perceivedSource(eventSource);
        if (eventSource == dragon
                || source == dragon
                || source != null && (source.getVehicle() == dragon || dragon.getVehicle() == source)
                || source instanceof Player player && player.isSpectator()
                || source != null && source.isSilent()) {
            return false;
        }

        Stimulus stimulus = stimulus(event);
        if (stimulus == null) {
            return false;
        }

        DragonPerceptionProfile profile = DragonPerceptionProfile.forDragon(dragon);
        double distance = dragon.getEyePosition().distanceTo(eventPosition);
        double effectiveRange = profile.hearingRange() * (0.35D + stimulus.loudness() * 0.65D);
        if (distance > effectiveRange) {
            return false;
        }

        float confidence = (float)(stimulus.loudness() * (1.0D - distance / effectiveRange));
        if (source != null
                && stimulus.kind() == DragonSensoryObservation.Kind.STEP
                && source.isSteppingCarefully()) {
            confidence *= 0.35F;
        }
        if (isObstructed(level, eventPosition)) {
            confidence *= 0.45F;
        }
        if (confidence < MIN_CONFIDENCE) {
            return false;
        }

        Vec3 approximatePosition = addUncertainty(eventPosition, confidence);
        boolean unresolvedProjectile = stimulus.kind() == DragonSensoryObservation.Kind.PROJECTILE
                && !(source instanceof LivingEntity);
        UUID sourceUuid = unresolvedProjectile || source == null ? null : source.getUUID();
        DragonSensoryObservation observation = new DragonSensoryObservation(
                approximatePosition,
                sourceUuid,
                stimulus.kind(),
                confidence,
                level.getGameTime()
        );

        int ttl = Math.max(20, Math.round(profile.soundMemoryTicks() * stimulus.memoryMultiplier()));
        DragonAwarenessMemory awareness = DragonAwarenessMemory.get(dragon);
        if (event == GameEvent.PROJECTILE_LAND
                && source instanceof LivingEntity
                && sourceUuid != null
                && isDangerousProjectileImpact(eventPosition)) {
            UUID projectileUuid = eventSource != null && eventSource != source
                    ? eventSource.getUUID()
                    : null;
            awareness.rememberProjectileImpact(sourceUuid, projectileUuid, level.getGameTime());
        }
        boolean storedAmbient = storeObservation(DragonMemories.HEARD_STIMULUS, observation, ttl, level.getGameTime());
        LivingEntity target = dragon.getBrain().getMemory(DragonMemories.ATTACK_TARGET).orElse(null);
        boolean threatening = source instanceof LivingEntity living
                && (living == target
                || living == dragon.getLastHurtByMob()
                || awareness.isProjectileThreat(sourceUuid, level.getGameTime()));
        if (storedAmbient) {
            awareness.rememberSound(observation, threatening, level.getGameTime());
        }
        boolean storedTarget = target != null
                && sourceUuid != null
                && sourceUuid.equals(target.getUUID())
                && storeObservation(DragonMemories.HEARD_TARGET, observation, ttl, level.getGameTime());
        boolean storedInvestigation = target == null
                && stimulus.investigate()
                && canInvestigate(stimulus, source)
                && DragonInvestigation.isMeaningfulSound(observation)
                && DragonInvestigation.remember(dragon, observation);
        return storedAmbient || storedTarget || storedInvestigation;
    }

    private static boolean canInvestigate(Stimulus stimulus, Entity source) {
        if (stimulus.kind() == DragonSensoryObservation.Kind.PROJECTILE) {
            return source instanceof LivingEntity;
        }
        return stimulus.kind() != DragonSensoryObservation.Kind.BLOCK || source != null;
    }

    private boolean isDangerousProjectileImpact(Vec3 eventPosition) {
        double horizontalRadius = Math.max(MIN_PROJECTILE_DANGER_RADIUS, dragon.getBbWidth());
        return dragon.getBoundingBox()
                .inflate(horizontalRadius, PROJECTILE_DANGER_VERTICAL_RADIUS, horizontalRadius)
                .contains(eventPosition);
    }

    private Entity perceivedSource(Entity source) {
        Entity current = source;
        for (int depth = 0; current != null && depth < 3; depth++) {
            if (current instanceof LivingEntity) {
                return current;
            }
            Entity owner = null;
            if (current instanceof TraceableEntity traceable) {
                owner = traceable.getOwner();
            }
            if (owner == null && current instanceof OwnableEntity ownable) {
                owner = ownable.getOwner();
            }
            if (owner == null || owner == current) {
                break;
            }
            current = owner;
        }
        return current;
    }

    private boolean storeObservation(MemoryModuleType<DragonSensoryObservation> memory,
                                     DragonSensoryObservation observation,
                                     int ttl,
                                     long gameTime) {
        DragonSensoryObservation existing = dragon.getBrain().getMemory(memory).orElse(null);
        if (existing != null
                && gameTime - existing.observedAt() < 10L
                && existing.confidence() > observation.confidence()) {
            return false;
        }
        dragon.getBrain().setMemoryWithExpiry(memory, observation, ttl);
        return true;
    }

    private boolean isObstructed(ServerLevel level, Vec3 eventPosition) {
        Vec3 origin = dragon.getEyePosition();
        HitResult result = level.clip(new ClipContext(
                origin,
                eventPosition,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                dragon
        ));
        if (result.getType() == HitResult.Type.MISS) {
            return false;
        }
        double totalDistance = origin.distanceTo(eventPosition);
        return origin.distanceTo(result.getLocation()) + 0.75D < totalDistance;
    }

    private Vec3 addUncertainty(Vec3 position, float confidence) {
        double radius = (1.0D - confidence) * 5.0D;
        if (radius < 0.15D) {
            return position;
        }
        double angle = dragon.getRandom().nextDouble() * Math.PI * 2.0D;
        double distance = Math.sqrt(dragon.getRandom().nextDouble()) * radius;
        double vertical = (dragon.getRandom().nextDouble() - 0.5D) * radius * 0.25D;
        return position.add(Math.cos(angle) * distance, vertical, Math.sin(angle) * distance);
    }

    private static Stimulus stimulus(GameEvent event) {
        if (event == GameEvent.EXPLODE || event == GameEvent.LIGHTNING_STRIKE) {
            return new Stimulus(DragonSensoryObservation.Kind.EXPLOSION, 1.0F, 1.5F, true);
        }
        if (event == GameEvent.ENTITY_ROAR || event == GameEvent.SHRIEK) {
            return new Stimulus(DragonSensoryObservation.Kind.ROAR, 0.95F, 1.35F, true);
        }
        if (event == GameEvent.ENTITY_DAMAGE || event == GameEvent.ENTITY_DIE) {
            return new Stimulus(DragonSensoryObservation.Kind.COMBAT, 0.85F, 1.25F, true);
        }
        if (event == GameEvent.HIT_GROUND) {
            return new Stimulus(DragonSensoryObservation.Kind.IMPACT, 0.70F, 0.75F, false);
        }
        if (event == GameEvent.PROJECTILE_LAND || event == GameEvent.PROJECTILE_SHOOT) {
            return new Stimulus(DragonSensoryObservation.Kind.PROJECTILE, 0.75F, 1.0F, true);
        }
        if (event == GameEvent.BLOCK_DESTROY || event == GameEvent.BLOCK_PLACE) {
            return new Stimulus(DragonSensoryObservation.Kind.BLOCK, 0.65F, 1.0F, true);
        }
        if (event == GameEvent.BLOCK_OPEN || event == GameEvent.BLOCK_CLOSE) {
            return new Stimulus(DragonSensoryObservation.Kind.BLOCK, 0.65F, 1.0F, false);
        }
        if (event == GameEvent.TELEPORT) {
            return new Stimulus(DragonSensoryObservation.Kind.TELEPORT, 0.70F, 1.0F, true);
        }
        if (event == GameEvent.SPLASH || event == GameEvent.SWIM) {
            return new Stimulus(DragonSensoryObservation.Kind.SPLASH, 0.50F, 0.75F, false);
        }
        if (event == GameEvent.STEP || event == GameEvent.FLAP || event == GameEvent.ELYTRA_GLIDE) {
            return new Stimulus(DragonSensoryObservation.Kind.STEP, 0.30F, 0.65F, false);
        }
        return null;
    }

    private record Stimulus(DragonSensoryObservation.Kind kind,
                            float loudness,
                            float memoryMultiplier,
                            boolean investigate) {
    }
}
