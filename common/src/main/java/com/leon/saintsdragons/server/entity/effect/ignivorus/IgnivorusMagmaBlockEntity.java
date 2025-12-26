package com.leon.saintsdragons.server.entity.effect.ignivorus;

import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
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

public class IgnivorusMagmaBlockEntity extends Entity {
    private static final EntityDataAccessor<BlockState> DATA_BLOCK_STATE =
            SynchedEntityData.defineId(IgnivorusMagmaBlockEntity.class, EntityDataSerializers.BLOCK_STATE);
    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(IgnivorusMagmaBlockEntity.class, EntityDataSerializers.FLOAT);

    private Ignivorus owner;
    private double impactRadius;
    private float impactDamage;
    private int lifetimeTicks;
    private int livedTicks;

    public IgnivorusMagmaBlockEntity(EntityType<? extends IgnivorusMagmaBlockEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        this.refreshDimensions();
    }

    public IgnivorusMagmaBlockEntity(Level level, Vec3 pos, Ignivorus owner,
                                     double impactRadius, float impactDamage, int lifetimeTicks) {
        this(ModEntities.IGNIVORUS_MAGMA_BLOCK.get(), level);
        this.setPos(pos);
        this.owner = owner;
        this.impactRadius = impactRadius;
        this.impactDamage = impactDamage;
        this.lifetimeTicks = lifetimeTicks;
        this.setDeltaMovement(Vec3.ZERO);
        this.setBlockState(Blocks.MAGMA_BLOCK.defaultBlockState());
        this.setVisualScale(1.0F);
        this.refreshDimensions();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_BLOCK_STATE, Blocks.MAGMA_BLOCK.defaultBlockState());
        this.entityData.define(DATA_SCALE, 1.0F);
    }

    public void setBlockState(BlockState state) {
        this.entityData.set(DATA_BLOCK_STATE, state);
    }

    public BlockState getBlockState() {
        return this.entityData.get(DATA_BLOCK_STATE);
    }

    public void setVisualScale(float scale) {
        this.entityData.set(DATA_SCALE, scale);
        this.refreshDimensions();
    }

    public float getVisualScale() {
        return this.entityData.get(DATA_SCALE);
    }

    @Override
    public void tick() {
        // Don't call super.tick() - handle movement manually like FallingBlockEntity does

        if (this.getBlockState().isAir()) {
            discard();
            return;
        }

        livedTicks++;

        // Apply gravity (on both sides for smooth client prediction)
        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.04D, 0.0D));
        }

        // Move (on both sides)
        this.move(MoverType.SELF, this.getDeltaMovement());

        // Server-side logic only
        if (!level().isClientSide) {
            // Apply air resistance only for gravity-driven shots
            if (!this.isNoGravity()) {
                this.setDeltaMovement(this.getDeltaMovement().scale(0.98D));
            }

            // Check lifetime
            if (livedTicks > lifetimeTicks) {
                explode();
                return;
            }

            // Check for ground impact
            if (this.onGround()) {
                Vec3 motion = this.getDeltaMovement();
                this.setDeltaMovement(motion.x * 0.7D, -motion.y * 0.5D, motion.z * 0.7D);
                explode();
                return;
            }
        } else {
            // Client-side: apply same air resistance for prediction (gravity-driven only)
            if (!this.isNoGravity()) {
                this.setDeltaMovement(this.getDeltaMovement().scale(0.98D));
            }

            // Spawn trail particles
            spawnTrailParticles();
        }
    }

    private void spawnTrailParticles() {
        float scale = getVisualScale();
        level().addParticle(ParticleTypes.FLAME, getX(), getY() + 0.2D * scale, getZ(), 0.0D, 0.011D, 0.0D);
        level().addParticle(ParticleTypes.SMALL_FLAME, getX(), getY() + 0.2D * scale, getZ(), 0.0D, 0.003D, 0.0D);
        level().addParticle(ParticleTypes.FALLING_LAVA, getX(), getY(), getZ(), 0.0D, -0.035D, 0.0D);
    }

    private void explode() {
        if (!(level() instanceof ServerLevel server)) {
            discard();
            return;
        }

        Vec3 impact = position();
        float scale = getVisualScale();
        server.sendParticles(ParticleTypes.LAVA, impact.x, impact.y + 0.5D * scale, impact.z, (int)(18 * scale),
                0.5D * scale, 0.3D * scale, 0.5D * scale, 0.04D);
        server.sendParticles(ParticleTypes.FLAME, impact.x, impact.y + 0.5D * scale, impact.z, (int)(30 * scale),
                0.6D * scale, 0.4D * scale, 0.6D * scale, 0.08D);
        server.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE, getSoundSource(), 0.7F * scale, 1.1F / scale);

        AABB area = new AABB(impact.x - impactRadius, impact.y - impactRadius, impact.z - impactRadius,
                impact.x + impactRadius, impact.y + impactRadius, impact.z + impactRadius);
        List<net.minecraft.world.entity.LivingEntity> hits = server.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, area,
                target -> target.isAlive() && target != owner && (owner == null || !owner.isAlly(target)));

        for (net.minecraft.world.entity.LivingEntity target : hits) {
            target.hurt(server.damageSources().explosion(this, owner != null ? owner : this), impactDamage);
            target.setSecondsOnFire((int)(4 * scale));
        }

        igniteArea(server, BlockPos.containing(impact));
        discard();
    }

    private void igniteArea(ServerLevel server, BlockPos base) {
        float scale = getVisualScale();
        int radius = (int)Math.ceil(scale);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
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
        if (tag.contains("Scale")) {
            setVisualScale(tag.getFloat("Scale"));
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putInt("Lived", livedTicks);
        tag.putInt("Lifetime", lifetimeTicks);
        tag.putDouble("ImpactRadius", impactRadius);
        tag.putFloat("ImpactDamage", impactDamage);
        tag.put("BlockState", NbtUtils.writeBlockState(getBlockState()));
        tag.putFloat("Scale", getVisualScale());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        // Restore velocity from packet
        this.setDeltaMovement(packet.getXa(), packet.getYa(), packet.getZa());
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        // Render up to 128 blocks away for larger fireballs
        return distance < 16384.0D;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return false;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
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
        return -0.2D * getVisualScale();
    }

    @Override
    public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
        return !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public float getEyeHeight(@NotNull Pose pose) {
        return 0.5F * getVisualScale();
    }

    @Override
    public EntityDimensions getDimensions(@NotNull Pose pose) {
        float scale = getVisualScale();
        return EntityDimensions.fixed(0.98F * scale, 0.98F * scale);
    }

    @Nullable
    public Ignivorus getOwner() {
        return owner;
    }
}
