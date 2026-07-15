package com.leon.saintsdragons.server.entity.effect;

import com.leon.saintsdragons.common.item.tools.SwordAbilityTargeting;
import com.leon.saintsdragons.common.registry.ModEntities;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GroundCrackEntity extends Entity {
    public static final float RENDER_PLANE_Y = 0.04F;
    private static final int STEGONAUT_DURATION = 34;
    private static final int STEGONAUT_STYLE = 0;
    private static final int DRAGONLORD_FISSURE_STYLE = 1;
    private static final int FISSURE_DAMAGE_INTERVAL = 20;
    private static final double FISSURE_CONTACT_HEIGHT = 1.25D;
    private static final int FISSURE_PARTICLE_SPOKES = 12;
    private static final EntityDataAccessor<Integer> DATA_STYLE =
            SynchedEntityData.defineId(GroundCrackEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DURATION =
            SynchedEntityData.defineId(GroundCrackEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_VISUAL_RADIUS =
            SynchedEntityData.defineId(GroundCrackEntity.class, EntityDataSerializers.FLOAT);

    private int age;
    private UUID ownerUuid;
    private float damageRadius;
    private float damage;
    private final Map<UUID, Integer> nextDamageTicks = new HashMap<>();

    public GroundCrackEntity(EntityType<? extends GroundCrackEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    public GroundCrackEntity(Level level, Vec3 position, float yaw) {
        this(ModEntities.STEGONAUT_GROUND_CRACK.get(), level);
        setPos(position);
        setYRot(yaw);
        this.yRotO = yaw;
        GroundEffectSurfaceSnap.snap(this, RENDER_PLANE_Y);
    }

    public GroundCrackEntity(Level level, Vec3 position, float yaw, ServerPlayer owner,
                             float visualRadius, float damageRadius, float damage, int duration) {
        this(level, position, yaw);
        entityData.set(DATA_STYLE, DRAGONLORD_FISSURE_STYLE);
        entityData.set(DATA_DURATION, Math.max(1, duration));
        entityData.set(DATA_VISUAL_RADIUS, Math.max(0.5F, visualRadius));
        this.ownerUuid = owner.getUUID();
        this.damageRadius = Math.max(0.0F, damageRadius);
        this.damage = Math.max(0.0F, damage);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_STYLE, STEGONAUT_STYLE);
        entityData.define(DATA_DURATION, STEGONAUT_DURATION);
        entityData.define(DATA_VISUAL_RADIUS, 7.0F);
    }

    public boolean isDragonlordFissure() {
        return entityData.get(DATA_STYLE) == DRAGONLORD_FISSURE_STYLE;
    }

    public float getScale(float partialTicks) {
        float progress = Math.min((age + partialTicks) / 5.0F, 1.0F);
        if (isDragonlordFissure()) {
            float radius = entityData.get(DATA_VISUAL_RADIUS);
            return radius * (0.72F + progress * 0.28F);
        }
        return 4.0F + progress * 3.0F;
    }

    public float getOpacity(float partialTicks) {
        float ageFrac = (age + partialTicks) / (float) entityData.get(DATA_DURATION);
        if (isDragonlordFissure()) {
            return ageFrac < 0.82F
                    ? 1.0F
                    : Math.max(1.0F - (ageFrac - 0.82F) / 0.18F, 0.0F);
        }
        return Math.max(1.0F - ageFrac * ageFrac, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        age++;
        GroundEffectSurfaceSnap.snap(this, RENDER_PLANE_Y);
        if (level() instanceof ServerLevel server) {
            if (isDragonlordFissure()) {
                damageEntitiesInsideFissure();
                spawnFissureParticles(server);
            }
            if (age >= entityData.get(DATA_DURATION)) {
                discard();
            }
        }
    }

    private void spawnFissureParticles(ServerLevel server) {
        if (damageRadius <= 0.0F) {
            return;
        }

        if (age % 2 == 0) {
            for (int i = 0; i < 3; i++) {
                Vec3 point = randomFissurePoint();
                server.sendParticles(
                        ParticleTypes.FLAME,
                        point.x,
                        point.y,
                        point.z,
                        0,
                        (random.nextDouble() - 0.5D) * 0.035D,
                        0.18D + random.nextDouble() * 0.3D,
                        (random.nextDouble() - 0.5D) * 0.035D,
                        1.0D
                );
            }
        }

        if (age % 5 == 0) {
            Vec3 lavaPoint = randomFissurePoint();
            server.sendParticles(
                    ParticleTypes.LAVA,
                    lavaPoint.x,
                    lavaPoint.y,
                    lavaPoint.z,
                    1,
                    0.08D,
                    0.03D,
                    0.08D,
                    0.02D
            );

            Vec3 magmaPoint = randomFissurePoint();
            server.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MAGMA_BLOCK.defaultBlockState()),
                    magmaPoint.x,
                    magmaPoint.y,
                    magmaPoint.z,
                    2,
                    0.08D,
                    0.12D,
                    0.08D,
                    0.08D
            );
        }

        if (age % 10 == 0) {
            for (int i = 0; i < 2; i++) {
                Vec3 smokePoint = randomFissurePoint();
                server.sendParticles(
                        ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        smokePoint.x,
                        smokePoint.y + 0.08D,
                        smokePoint.z,
                        0,
                        (random.nextDouble() - 0.5D) * 0.015D,
                        0.045D + random.nextDouble() * 0.035D,
                        (random.nextDouble() - 0.5D) * 0.015D,
                        1.0D
                );
            }
        }
    }

    private Vec3 randomFissurePoint() {
        double spoke = random.nextInt(FISSURE_PARTICLE_SPOKES) * (Math.PI * 2.0D / FISSURE_PARTICLE_SPOKES);
        double angle = spoke + (random.nextDouble() - 0.5D) * 0.24D;
        double radius = 0.45D + random.nextDouble() * Math.max(0.1D, damageRadius - 0.45D);
        return new Vec3(
                getX() + Math.cos(angle) * radius,
                getY() + RENDER_PLANE_Y + 0.025D,
                getZ() + Math.sin(angle) * radius
        );
    }

    private void damageEntitiesInsideFissure() {
        if (!(level() instanceof ServerLevel server)
                || ownerUuid == null
                || damage <= 0.0F
                || damageRadius <= 0.0F
                || !(server.getEntity(ownerUuid) instanceof ServerPlayer owner)) {
            return;
        }

        AABB hitbox = getBoundingBox().inflate(damageRadius, 2.5D, damageRadius);
        for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, hitbox,
                entity -> SwordAbilityTargeting.canDamage(owner, entity))) {
            UUID targetId = target.getUUID();
            if (age < nextDamageTicks.getOrDefault(targetId, 0)) {
                continue;
            }

            AABB targetBounds = target.getBoundingBox();
            if (targetBounds.minY > getY() + FISSURE_CONTACT_HEIGHT || targetBounds.maxY < getY() - 0.25D) {
                continue;
            }
            double nearestX = Math.max(targetBounds.minX, Math.min(getX(), targetBounds.maxX));
            double nearestZ = Math.max(targetBounds.minZ, Math.min(getZ(), targetBounds.maxZ));
            double dx = nearestX - getX();
            double dz = nearestZ - getZ();
            if (dx * dx + dz * dz > damageRadius * damageRadius) {
                continue;
            }

            if (target.hurt(server.damageSources().playerAttack(owner), damage)) {
                nextDamageTicks.put(targetId, age + FISSURE_DAMAGE_INTERVAL);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        age = tag.getInt("Age");
        setYRot(tag.getFloat("Yaw"));
        this.yRotO = getYRot();
        entityData.set(DATA_STYLE, tag.getInt("Style"));
        entityData.set(DATA_DURATION, tag.contains("Duration") ? Math.max(1, tag.getInt("Duration")) : STEGONAUT_DURATION);
        entityData.set(DATA_VISUAL_RADIUS, tag.contains("VisualRadius") ? tag.getFloat("VisualRadius") : 7.0F);
        if (tag.hasUUID("Owner")) {
            ownerUuid = tag.getUUID("Owner");
        }
        damageRadius = tag.getFloat("DamageRadius");
        damage = tag.getFloat("Damage");
        nextDamageTicks.clear();
        int cooldownCount = tag.getInt("CooldownCount");
        for (int i = 0; i < cooldownCount; i++) {
            String key = "CooldownTarget" + i;
            if (tag.hasUUID(key)) {
                nextDamageTicks.put(tag.getUUID(key), tag.getInt("CooldownTick" + i));
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putInt("Age", age);
        tag.putFloat("Yaw", getYRot());
        tag.putInt("Style", entityData.get(DATA_STYLE));
        tag.putInt("Duration", entityData.get(DATA_DURATION));
        tag.putFloat("VisualRadius", entityData.get(DATA_VISUAL_RADIUS));
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }
        tag.putFloat("DamageRadius", damageRadius);
        tag.putFloat("Damage", damage);
        tag.putInt("CooldownCount", nextDamageTicks.size());
        int index = 0;
        for (Map.Entry<UUID, Integer> cooldown : nextDamageTicks.entrySet()) {
            tag.putUUID("CooldownTarget" + index, cooldown.getKey());
            tag.putInt("CooldownTick" + index, cooldown.getValue());
            index++;
        }
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 16384.0D;
    }
}
