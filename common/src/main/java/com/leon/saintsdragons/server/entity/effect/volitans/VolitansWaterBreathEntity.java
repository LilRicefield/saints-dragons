package com.leon.saintsdragons.server.entity.effect.volitans;

import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import com.leon.saintsdragons.server.entity.dragons.util.DragonElementalImmunity;
import com.leon.saintsdragons.server.entity.dragons.util.DragonUtilities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class VolitansWaterBreathEntity extends Entity {
    private static final EntityDataAccessor<Boolean> DATA_POISON_MODE =
            SynchedEntityData.defineId(VolitansWaterBreathEntity.class, EntityDataSerializers.BOOLEAN);
    private UUID ownerUUID;
    private LivingEntity owner;
    private float damage;
    private float pushStrength;
    private int age;
    private int maxAge;
    private int poisonDurationTicks;
    private int poisonAmplifier;

    public VolitansWaterBreathEntity(EntityType<? extends VolitansWaterBreathEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.maxAge = 12;
        this.poisonDurationTicks = 80;
        this.poisonAmplifier = 0;
    }

    public VolitansWaterBreathEntity(Level level, Vec3 position, Vec3 velocity, Entity owner,
                                     float damage, float pushStrength, int lifetime, boolean poisonMode,
                                     int poisonDurationTicks, int poisonAmplifier) {
        this(ModEntities.VOLITANS_WATER_BREATH.get(), level);
        this.setPos(position);
        this.setDeltaMovement(velocity);
        this.ownerUUID = owner != null ? owner.getUUID() : null;
        this.owner = owner instanceof LivingEntity living ? living : null;
        this.damage = damage;
        this.pushStrength = pushStrength;
        this.maxAge = Math.max(1, lifetime);
        this.poisonDurationTicks = Math.max(0, poisonDurationTicks);
        this.poisonAmplifier = Math.max(-1, poisonAmplifier);
        this.entityData.set(DATA_POISON_MODE, poisonMode);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_POISON_MODE, false);
    }

    @Override
    public void tick() {
        super.tick();
        age++;

        if (!level().isClientSide) {
            if (age >= maxAge) {
                discard();
                return;
            }
            if (!isPoisonMode() && level() instanceof ServerLevel serverLevel) {
                Vec3 start = position();
                Vec3 end = start.add(getDeltaMovement());
                if (convertLavaToCobblestone(serverLevel, start, end)) {
                    discard();
                    return;
                }
                if (DragonUtilities.extinguishFire(serverLevel, start, end, 1.0D)
                        && getOwner() instanceof DragonEntity dragon) {
                    var player = DragonUtilities.resolveResponsiblePlayer(dragon);
                    if (player != null) {
                        DragonUtilities.awardAdvancement(player, "fire_hydrant", "fire_hydrant");
                    }
                }
            }
            if (hitEntity()) {
                discard();
                return;
            }
        }

        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        // Keep momentum strong so the stream reaches farther.
        setDeltaMovement(motion.scale(0.997D));
    }

    private boolean convertLavaToCobblestone(ServerLevel level, Vec3 start, Vec3 end) {
        BlockPos startPos = BlockPos.containing(start);
        if (level.getFluidState(startPos).is(FluidTags.LAVA)) {
            return convertLavaBlock(level, startPos);
        }

        BlockHitResult hit = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                this));
        if (hit.getType() == HitResult.Type.MISS
                || !level.getFluidState(hit.getBlockPos()).is(FluidTags.LAVA)) {
            return false;
        }
        return convertLavaBlock(level, hit.getBlockPos());
    }

    private boolean convertLavaBlock(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos) || !level.getFluidState(pos).is(FluidTags.LAVA)) {
            return false;
        }
        level.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 3);
        level.levelEvent(LevelEvent.LAVA_FIZZ, pos, 0);
        return true;
    }

    private boolean hitEntity() {
        Vec3 start = position();
        Vec3 end = start.add(getDeltaMovement());
        AABB box = new AABB(start, end).inflate(0.8D);
        List<LivingEntity> targets = level().getEntitiesOfClass(LivingEntity.class, box);
        LivingEntity attacker = getOwner();
        boolean poisonActive = isPoisonMode() && !(attacker instanceof Volitans volitans && volitans.isVenomNeutralized());

        for (LivingEntity target : targets) {
            if (!target.isAlive() || target.isRemoved()) {
                continue;
            }
            if (ownerUUID != null && ownerUUID.equals(target.getUUID())) {
                continue;
            }
            if (isAlliedTarget(attacker, target)) {
                continue;
            }
            if (poisonActive && DragonElementalImmunity.isPoisonImmune(target)) {
                continue;
            }

            DamageSource source = attacker != null
                    ? level().damageSources().mobAttack(attacker)
                    : level().damageSources().generic();
            target.hurt(source, damage);
            if (poisonActive && poisonDurationTicks > 0 && poisonAmplifier >= 0) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, poisonDurationTicks, poisonAmplifier));
            }

            Vec3 pushDir = target.position().subtract(position());
            if (pushDir.lengthSqr() < 1.0E-6) {
                pushDir = getDeltaMovement();
            }
            if (pushStrength > 0.001F && pushDir.lengthSqr() > 1.0E-6) {
                pushDir = pushDir.normalize().scale(pushStrength);
                target.push(pushDir.x, 0.04D, pushDir.z);
                target.hasImpulse = true;
            }
            return true;
        }
        return false;
    }

    private boolean isAlliedTarget(LivingEntity attacker, LivingEntity target) {
        if (attacker == null || target == null) {
            return false;
        }
        if (attacker.isAlliedTo(target)) {
            return true;
        }
        if (attacker instanceof DragonEntity dragon) {
            return dragon.isAlly(target);
        }
        return false;
    }

    private LivingEntity getOwner() {
        if (owner == null && ownerUUID != null && level() instanceof ServerLevel server) {
            Entity entity = server.getEntity(ownerUUID);
            if (entity instanceof LivingEntity living) {
                owner = living;
            }
        }
        return owner;
    }

    public int getAge() {
        return age;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public boolean isPoisonMode() {
        return this.entityData.get(DATA_POISON_MODE);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        age = tag.getInt("Age");
        maxAge = tag.getInt("MaxAge");
        damage = tag.getFloat("Damage");
        pushStrength = tag.getFloat("PushStrength");
        poisonDurationTicks = tag.getInt("PoisonDuration");
        poisonAmplifier = tag.getInt("PoisonAmplifier");
        this.entityData.set(DATA_POISON_MODE, tag.getBoolean("PoisonMode"));
        if (tag.hasUUID("Owner")) {
            ownerUUID = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putInt("Age", age);
        tag.putInt("MaxAge", maxAge);
        tag.putFloat("Damage", damage);
        tag.putFloat("PushStrength", pushStrength);
        tag.putInt("PoisonDuration", poisonDurationTicks);
        tag.putInt("PoisonAmplifier", poisonAmplifier);
        tag.putBoolean("PoisonMode", this.entityData.get(DATA_POISON_MODE));
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
