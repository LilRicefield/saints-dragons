package com.leon.saintsdragons.server.entity.effect.amphithere;

import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.dragons.amphithere.AmphithereEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class AmphithereMagmaBlockEntity extends Entity {
    private static final EntityDataAccessor<BlockState> DATA_BLOCK_STATE =
            SynchedEntityData.defineId(AmphithereMagmaBlockEntity.class, EntityDataSerializers.BLOCK_STATE);

    private AmphithereEntity owner;
    private double impactRadius;
    private float impactDamage;
    private int lifetimeTicks;
    private int livedTicks;

    public AmphithereMagmaBlockEntity(EntityType<? extends AmphithereMagmaBlockEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        this.refreshDimensions();
    }

    public AmphithereMagmaBlockEntity(Level level, Vec3 pos, AmphithereEntity owner,
                                      double impactRadius, float impactDamage, int lifetimeTicks) {
        this(ModEntities.AMPHITHERE_MAGMA_BLOCK.get(), level);
        this.setPos(pos);
        this.owner = owner;
        this.impactRadius = impactRadius;
        this.impactDamage = impactDamage;
        this.lifetimeTicks = lifetimeTicks;
        this.setDeltaMovement(Vec3.ZERO);
        this.setBlockState(Blocks.MAGMA_BLOCK.defaultBlockState());
        this.refreshDimensions();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_BLOCK_STATE, Blocks.MAGMA_BLOCK.defaultBlockState());
    }

    public void setFiringProperties(AmphithereEntity owner, double impactRadius, float impactDamage, int lifetimeTicks) {
        this.owner = owner;
        this.impactRadius = impactRadius;
        this.impactDamage = impactDamage;
        this.lifetimeTicks = lifetimeTicks;
    }

    public void setBlockState(BlockState state) {
        this.entityData.set(DATA_BLOCK_STATE, state);
    }

    public BlockState getBlockState() {
        return this.entityData.get(DATA_BLOCK_STATE);
    }

    @Override
    public void tick() {
        if (this.getBlockState().isAir()) {
            discard();
            return;
        }

        if (livedTicks++ > lifetimeTicks) {
            explode();
            return;
        }

        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.02D, 0.0D));
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.98D));

        if (this.onGround()) {
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(motion.x * 0.7D, -motion.y * 0.5D, motion.z * 0.7D);
            explode();
            return;
        }

        if (level().isClientSide) {
            spawnTrailParticles();
        }
    }

    private void spawnTrailParticles() {
        level().addParticle(ParticleTypes.FLAME, getX(), getY() + 0.2D, getZ(), 0.0D, 0.011D, 0.0D);
        level().addParticle(ParticleTypes.SMALL_FLAME, getX(), getY() + 0.2D, getZ(), 0.0D, 0.003D, 0.0D);
        level().addParticle(ParticleTypes.FALLING_LAVA, getX(), getY(), getZ(), 0.0D, -0.035D, 0.0D);
    }

    private void explode() {
        if (!(level() instanceof ServerLevel server)) {
            discard();
            return;
        }

        Vec3 impact = position();
        server.sendParticles(ParticleTypes.LAVA, impact.x, impact.y + 0.5D, impact.z, 18,
                0.5D, 0.3D, 0.5D, 0.04D);
        server.sendParticles(ParticleTypes.FLAME, impact.x, impact.y + 0.5D, impact.z, 30,
                0.6D, 0.4D, 0.6D, 0.08D);
        server.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE, getSoundSource(), 0.7F, 1.1F);

        AABB area = new AABB(impact.x - impactRadius, impact.y - impactRadius, impact.z - impactRadius,
                impact.x + impactRadius, impact.y + impactRadius, impact.z + impactRadius);
        List<net.minecraft.world.entity.LivingEntity> hits = server.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, area,
                target -> target.isAlive() && target != owner && (owner == null || !owner.isAlly(target)));

        for (net.minecraft.world.entity.LivingEntity target : hits) {
            target.hurt(server.damageSources().explosion(this, owner != null ? owner : this), impactDamage);
            target.setSecondsOnFire(4);
        }

        igniteArea(server, BlockPos.containing(impact));
        discard();
    }

    private void igniteArea(ServerLevel server, BlockPos base) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = base.offset(dx, 0, dz);
                BlockState state = server.getBlockState(pos);
                if (!state.isAir()) continue;

                BlockPos below = pos.below();
                BlockState belowState = server.getBlockState(below);
                if (!belowState.isAir() && Blocks.FIRE.defaultBlockState().canSurvive(server, pos)) {
                    server.setBlock(pos, Blocks.FIRE.defaultBlockState(), 11);
                }
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        this.livedTicks = tag.getInt("Lived");
        this.lifetimeTicks = tag.getInt("Lifetime");
        this.impactRadius = tag.getDouble("ImpactRadius");
        this.impactDamage = tag.getFloat("ImpactDamage");
        if (tag.contains("BlockState", CompoundTag.TAG_COMPOUND) && level() instanceof ServerLevel server) {
            BlockState state = NbtUtils.readBlockState(server.holderLookup(net.minecraft.core.registries.Registries.BLOCK), tag.getCompound("BlockState"));
            setBlockState(state.isAir() ? Blocks.MAGMA_BLOCK.defaultBlockState() : state);
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putInt("Lived", livedTicks);
        tag.putInt("Lifetime", lifetimeTicks);
        tag.putDouble("ImpactRadius", impactRadius);
        tag.putFloat("ImpactDamage", impactDamage);
        tag.put("BlockState", NbtUtils.writeBlockState(getBlockState()));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        return false;
    }

    @Override
    public double getPassengersRidingOffset() {
        return -0.2D;
    }

    @Override
    public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
        return !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public float getEyeHeight(@NotNull Pose pose) {
        return 0.5F;
    }

    @Override
    public EntityDimensions getDimensions(@NotNull Pose pose) {
        return EntityDimensions.fixed(0.98F, 0.98F);
    }

    @Nullable
    public AmphithereEntity getOwner() {
        return owner;
    }
}

