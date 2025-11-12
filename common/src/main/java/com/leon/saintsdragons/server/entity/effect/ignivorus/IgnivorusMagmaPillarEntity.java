package com.leon.saintsdragons.server.entity.effect.ignivorus;

import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;

/**
 * GeckoLib-driven magma pillar spawned by the Ignivorus roar.
 * Handles timing, damage, and knockback locally so the ability can simply instantiate it.
 */
public class IgnivorusMagmaPillarEntity extends Entity implements GeoEntity {
    private static final RawAnimation EMERGE_ANIMATION =
            RawAnimation.begin().thenPlay("animation.ignivorus_magma_pillar.emerge");
    private static final RawAnimation SUBSIDE_ANIMATION =
            RawAnimation.begin().thenPlay("animation.ignivorus_magma_pillar.subside");
    private static final int SUBSIDE_DURATION_TICKS = 9; // Matches 0.416s subside animation
    private static final EntityDimensions BASE_DIMENSIONS = EntityDimensions.scalable(5.5F, 5.5F);

    private static final EntityDataAccessor<Integer> DATA_STAGE =
            SynchedEntityData.defineId(IgnivorusMagmaPillarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(IgnivorusMagmaPillarEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_SUBSIDING =
            SynchedEntityData.defineId(IgnivorusMagmaPillarEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    private Ignivorus owner;
    private UUID ownerUUID;
    private float impactDamage = 16.0f;
    private double knockbackStrength = 1.0D;
    private int warmupTicks = 6;
    private int lifetimeTicks = 36;
    private int livedTicks;
    private int subsideTicks;
    private final java.util.Set<UUID> hitEntities = new java.util.HashSet<>();
    private boolean rotationLocked = false; // Lock rotation after initial setup
    private float lockedHeadYaw = 0.0f; // Store head rotation since setter doesn't work

    public IgnivorusMagmaPillarEntity(EntityType<? extends IgnivorusMagmaPillarEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public IgnivorusMagmaPillarEntity(Level level, Vec3 pos, Ignivorus owner, int stageIndex,
                                      float impactDamage, double knockbackStrength,
                                      int warmupTicks, int lifetimeTicks) {
        this(ModEntities.IGNIVORUS_MAGMA_PILLAR.get(), level);
        setPos(pos);
        this.owner = owner;
        this.ownerUUID = owner != null ? owner.getUUID() : null;
        this.impactDamage = impactDamage;
        this.knockbackStrength = knockbackStrength;
        this.warmupTicks = warmupTicks;
        this.lifetimeTicks = lifetimeTicks;
        setStage(stageIndex);
        setVisualScale(1.0f + stageIndex * 0.2f);

        // Set rotation AFTER all other initialization, then lock it
        // Snap to nearest cardinal direction (0°, 90°, 180°, 270°) for consistent appearance
        float ownerYaw = owner != null ? owner.getYRot() : 0f;
        float yaw = Math.round(ownerYaw / 90.0f) * 90.0f;

        initializeRotation(yaw);
        this.rotationLocked = true; // Lock rotation from now on
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_STAGE, 0);
        this.entityData.define(DATA_SCALE, 1.0f);
        this.entityData.define(DATA_SUBSIDING, false);
    }

    public void setStage(int stage) {
        this.entityData.set(DATA_STAGE, Math.max(0, stage));
        refreshDimensions();
    }

    public int getStage() {
        return this.entityData.get(DATA_STAGE);
    }

    public void setVisualScale(float scale) {
        this.entityData.set(DATA_SCALE, Math.max(0.5f, scale));
        refreshDimensions();
    }

    public float getVisualScale() {
        return this.entityData.get(DATA_SCALE);
    }

    public void setImpactDamage(float impactDamage) {
        this.impactDamage = impactDamage;
    }

    public void setKnockbackStrength(double knockbackStrength) {
        this.knockbackStrength = knockbackStrength;
    }

    public void setWarmupTicks(int warmupTicks) {
        this.warmupTicks = warmupTicks;
    }

    public void setLifetimeTicks(int lifetimeTicks) {
        this.lifetimeTicks = lifetimeTicks;
    }

    public boolean isSubsiding() {
        return this.entityData.get(DATA_SUBSIDING);
    }

    private void beginSubside() {
        if (isSubsiding()) {
            return;
        }
        this.subsideTicks = 0;
        this.entityData.set(DATA_SUBSIDING, true);
    }

    @Override
    public void tick() {
        super.tick();
        livedTicks++;
        setDeltaMovement(Vec3.ZERO);

        if (level().isClientSide) {
            spawnClientEffects();
        }

        if (isSubsiding()) {
            subsideTicks++;
            if (!level().isClientSide && subsideTicks >= SUBSIDE_DURATION_TICKS) {
                discard();
            }
            return;
        }

        if (!level().isClientSide) {
            resolveOwner();
            // Apply damage every tick after warmup until pillar begins subsiding
            if (livedTicks >= warmupTicks) {
                applyImpact();
            }
            if (livedTicks >= lifetimeTicks) {
                beginSubside();
            }
        }
    }

    @Override
    public void setYRot(float yaw) {
        // Only allow rotation changes before lock
        if (!rotationLocked) {
            super.setYRot(yaw);
        }
    }

    @Override
    public void setXRot(float pitch) {
        // Always lock pitch to 0 (vertical pillars)
        if (!rotationLocked) {
            super.setXRot(0.0f);
        }
    }

    @Override
    public void setYBodyRot(float yaw) {
        // Only allow before lock
        if (!rotationLocked) {
            super.setYBodyRot(yaw);
        }
    }

    @Override
    public void setYHeadRot(float yaw) {
        // Only allow before lock
        if (!rotationLocked) {
            super.setYHeadRot(yaw);
        }
    }

    @Override
    public float getYHeadRot() {
        // Return our locked head rotation instead of the entity's default
        return rotationLocked ? lockedHeadYaw : super.getYHeadRot();
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        // Prevent client interpolation from overriding rotation
        if (rotationLocked) {
            super.lerpTo(x, y, z, lockedHeadYaw, 0.0f, steps, teleport);
        } else {
            super.lerpTo(x, y, z, yRot, xRot, steps, teleport);
        }
    }

    private void resolveOwner() {
        if (owner == null && ownerUUID != null && level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(ownerUUID);
            if (entity instanceof Ignivorus ignivorus) {
                owner = ignivorus;
            }
        }
    }

    private void applyImpact() {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }

        double radius = 1.6D * getVisualScale();
        double height = 4.0D + getVisualScale() * 2.0D;
        AABB area = new AABB(getX() - radius, getY(), getZ() - radius,
                getX() + radius, getY() + height, getZ() + radius);

        List<LivingEntity> hits = server.getEntitiesOfClass(LivingEntity.class, area, target -> {
            if (!target.isAlive() || !target.attackable()) {
                return false;
            }
            if (target == owner) {
                return false;
            }
            // Skip if already hit
            if (hitEntities.contains(target.getUUID())) {
                return false;
            }
            return owner == null || !owner.isAlly(target);
        });

        DamageSource source = owner != null
                ? damageSources().mobAttack(owner)
                : damageSources().hotFloor();

        boolean firstHit = hitEntities.isEmpty() && !hits.isEmpty();

        for (LivingEntity target : hits) {
            // Mark as hit to prevent multiple damage ticks
            hitEntities.add(target.getUUID());

            target.hurt(source, impactDamage);
            target.setSecondsOnFire(4);

            Vec3 knockDir = target.position().subtract(position());
            knockDir = new Vec3(knockDir.x, 0.0D, knockDir.z);
            if (knockDir.lengthSqr() < 1.0E-4D) {
                knockDir = new Vec3(0, 0, 1);
            }
            knockDir = knockDir.normalize().scale(knockbackStrength);
            double verticalBoost = 0.35D + (getStage() * 0.05D);
            target.push(knockDir.x, verticalBoost, knockDir.z);
            target.hasImpulse = true;
            target.hurtMarked = true;
            if (target instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(target));
            }
        }

        // Only play effects/sounds on the first hit to avoid spam
        if (firstHit) {
            server.sendParticles(ParticleTypes.LAVA, getX(), getY() + 2.0D, getZ(), 20,
                    0.6D, 1.2D, 0.6D, 0.05D);
            server.playSound(null, blockPosition(), ModSounds.IGNIVORUS_MAGMA_PILLAR.get(), SoundSource.HOSTILE,
                    1.4F, 0.8F + server.random.nextFloat() * 0.2F);
        }
    }

    private void spawnClientEffects() {
        if (level().random.nextFloat() < 0.6f) {
            level().addParticle(ParticleTypes.SMALL_FLAME,
                    getX(), getY() + level().random.nextDouble() * 3.0D,
                    getZ(),
                    0.0D, 0.01D, 0.0D);
        }
        if (level().random.nextFloat() < 0.4f) {
            level().addParticle(ParticleTypes.FALLING_LAVA,
                    getX() + (level().random.nextDouble() - 0.5D) * 0.6D,
                    getY() + 0.1D,
                    getZ() + (level().random.nextDouble() - 0.5D) * 0.6D,
                    0.0D, -0.04D, 0.0D);
        }
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        livedTicks = tag.getInt("Lived");
        lifetimeTicks = tag.getInt("Lifetime");
        warmupTicks = tag.getInt("Warmup");
        impactDamage = tag.getFloat("ImpactDamage");
        knockbackStrength = tag.getDouble("Knockback");
        setStage(tag.getInt("Stage"));
        setVisualScale(tag.getFloat("Scale"));
        if (tag.hasUUID("Owner")) {
            ownerUUID = tag.getUUID("Owner");
        }

        // Load hit entities
        hitEntities.clear();
        if (tag.contains("HitEntities")) {
            long[] uuidArray = tag.getLongArray("HitEntities");
            for (int i = 0; i < uuidArray.length; i += 2) {
                hitEntities.add(new UUID(uuidArray[i], uuidArray[i + 1]));
            }
        }

        if (tag.contains("LockedYaw")) {
            initializeRotation(tag.getFloat("LockedYaw"));
        }
        this.rotationLocked = tag.getBoolean("RotationLocked");
        this.subsideTicks = Math.max(0, tag.getInt("SubsideTicks"));
        if (tag.contains("Subsiding")) {
            this.entityData.set(DATA_SUBSIDING, tag.getBoolean("Subsiding"));
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putInt("Lived", livedTicks);
        tag.putInt("Lifetime", lifetimeTicks);
        tag.putInt("Warmup", warmupTicks);
        tag.putFloat("ImpactDamage", impactDamage);
        tag.putDouble("Knockback", knockbackStrength);
        tag.putInt("Stage", getStage());
        tag.putFloat("Scale", getVisualScale());
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
        tag.putBoolean("RotationLocked", rotationLocked);
        tag.putFloat("LockedYaw", lockedHeadYaw);
        tag.putBoolean("Subsiding", isSubsiding());
        tag.putInt("SubsideTicks", subsideTicks);

        // Save hit entities
        long[] uuidArray = new long[hitEntities.size() * 2];
        int i = 0;
        for (UUID uuid : hitEntities) {
            uuidArray[i++] = uuid.getMostSignificantBits();
            uuidArray[i++] = uuid.getLeastSignificantBits();
        }
        tag.putLongArray("HitEntities", uuidArray);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        float yaw = packet.getYRot();
        initializeRotation(yaw);
        this.rotationLocked = true;
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        return BASE_DIMENSIONS.scale(getVisualScale());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::animationPredicate));
    }

    private <E extends GeoEntity> PlayState animationPredicate(AnimationState<E> state) {
        if (isSubsiding()) {
            state.getController().setAnimation(SUBSIDE_ANIMATION);
        } else {
            state.getController().setAnimation(EMERGE_ANIMATION);
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    private void initializeRotation(float yaw) {
        super.setYRot(yaw);
        super.setYBodyRot(yaw);
        super.setYHeadRot(yaw);
        this.lockedHeadYaw = yaw;
        this.yRotO = yaw;
        this.xRotO = 0.0f;
    }
}
