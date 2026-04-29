package com.leon.saintsdragons.server.entity.effect.stegonaut;

import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class StegonautGroundChunkEntity extends Entity {
    private static final EntityDataAccessor<BlockState> DATA_BLOCK_STATE =
            SynchedEntityData.defineId(StegonautGroundChunkEntity.class, EntityDataSerializers.BLOCK_STATE);
    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(StegonautGroundChunkEntity.class, EntityDataSerializers.FLOAT);

    private Stegonaut owner;
    private double impactRadius;
    private float impactDamage;
    private int lifetimeTicks;
    private int livedTicks;

    public StegonautGroundChunkEntity(EntityType<? extends StegonautGroundChunkEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        this.refreshDimensions();
    }

    public StegonautGroundChunkEntity(Level level, Vec3 pos, Stegonaut owner,
                                      BlockState blockState, double impactRadius, float impactDamage, int lifetimeTicks) {
        this(ModEntities.STEGONAUT_GROUND_CHUNK.get(), level);
        this.setPos(pos);
        this.owner = owner;
        this.impactRadius = impactRadius;
        this.impactDamage = impactDamage;
        this.lifetimeTicks = lifetimeTicks;
        this.setBlockState(blockState);
        this.setVisualScale(1.0F);
        this.noPhysics = true;
        this.refreshDimensions();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_BLOCK_STATE, Blocks.DIRT.defaultBlockState());
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
        if (this.getBlockState().isAir()) {
            discard();
            return;
        }

        livedTicks++;
        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.05D, 0.0D));
        }

        Vec3 currentPos = this.position();
        Vec3 motion = this.getDeltaMovement();
        Vec3 nextPos = currentPos.add(motion);

        BlockHitResult blockHit = level().clip(new ClipContext(
                currentPos, nextPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));
        boolean hitBlock = blockHit.getType() == HitResult.Type.BLOCK;
        EntityHitResult entityHit = findEntityHit(currentPos, nextPos);
        boolean hitEntity = entityHit != null;

        if (hitEntity && hitBlock) {
            double blockDist = blockHit.getLocation().distanceToSqr(currentPos);
            double entityDist = entityHit.getLocation().distanceToSqr(currentPos);
            this.setPos(entityDist <= blockDist ? entityHit.getLocation() : blockHit.getLocation());
        } else if (hitEntity) {
            this.setPos(entityHit.getLocation());
        } else if (hitBlock) {
            this.setPos(blockHit.getLocation());
        } else {
            this.setPos(nextPos);
        }

        if (!level().isClientSide) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.99D));
            if (livedTicks > lifetimeTicks || hitBlock || hitEntity) {
                explode();
                return;
            }
        } else {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.99D));
        }
    }

    @Nullable
    private EntityHitResult findEntityHit(Vec3 start, Vec3 end) {
        if (level().isClientSide) {
            return null;
        }
        AABB bounds = getBoundingBox().expandTowards(end.subtract(start)).inflate(0.8D);
        return ProjectileUtil.getEntityHitResult(level(), this, start, end, bounds, target -> {
            if (!(target instanceof LivingEntity living) || !living.isAlive()) {
                return false;
            }
            if (owner != null && (target == owner || owner.isAlly(target))) {
                return false;
            }
            return true;
        });
    }

    private void explode() {
        if (!(level() instanceof ServerLevel server)) {
            discard();
            return;
        }

        Vec3 impact = position();
        float scale = getVisualScale();
        server.sendParticles(ParticleTypes.POOF,
                impact.x, impact.y + 0.3D, impact.z,
                Math.min(80, Math.max(10, (int) (24 * scale))),
                0.45D * scale, 0.35D * scale, 0.45D * scale, 0.05D);

        server.playSound(null, blockPosition(), SoundEvents.STONE_BREAK, getSoundSource(), 1.2F, 0.9F);

        AABB area = new AABB(impact.x - impactRadius, impact.y - impactRadius, impact.z - impactRadius,
                impact.x + impactRadius, impact.y + impactRadius, impact.z + impactRadius);
        List<LivingEntity> hits = server.getEntitiesOfClass(LivingEntity.class, area,
                target -> target.isAlive() && target != owner && (owner == null || !owner.isAlly(target)));
        for (LivingEntity target : hits) {
            if (owner != null) {
                target.hurt(server.damageSources().mobAttack(owner), impactDamage);
            } else {
                target.hurt(server.damageSources().generic(), impactDamage);
            }
            Vec3 kb = target.position().subtract(impact).normalize().scale(0.55D * scale);
            target.push(kb.x, 0.16D, kb.z);
        }
        discard();
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        this.livedTicks = tag.getInt("Lived");
        this.lifetimeTicks = tag.getInt("Lifetime");
        this.impactRadius = tag.getDouble("ImpactRadius");
        this.impactDamage = tag.getFloat("ImpactDamage");
        if (tag.contains("BlockState", CompoundTag.TAG_COMPOUND) && level() instanceof ServerLevel server) {
            BlockState state = NbtUtils.readBlockState(server.holderLookup(Registries.BLOCK), tag.getCompound("BlockState"));
            setBlockState(state.isAir() ? Blocks.DIRT.defaultBlockState() : state);
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
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.setDeltaMovement(packet.getXa(), packet.getYa(), packet.getZa());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 16384.0D;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public float getEyeHeight(@NotNull Pose pose) {
        return 0.5F * getVisualScale();
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        float scale = getVisualScale();
        return EntityDimensions.fixed(0.98F * scale, 0.98F * scale);
    }
}