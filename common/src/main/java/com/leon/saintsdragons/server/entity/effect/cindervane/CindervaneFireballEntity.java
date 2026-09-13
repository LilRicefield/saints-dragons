package com.leon.saintsdragons.server.entity.effect.cindervane;

import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModParticles;
import com.leon.saintsdragons.server.entity.dragons.util.DragonElementalImmunity;
import com.leon.saintsdragons.server.entity.dragons.util.DragonGriefingRules;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.core.animation.AnimatableManager;

import javax.annotation.Nullable;
import java.util.List;

public class CindervaneFireballEntity extends Entity implements GeoEntity {
    private final AnimatableInstanceCache animationCache =
            GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    private static final EntityDataAccessor<BlockState> DATA_BLOCK_STATE =
            SynchedEntityData.defineId(CindervaneFireballEntity.class, EntityDataSerializers.BLOCK_STATE);

    private Cindervane owner;
    private double impactRadius;
    private float impactDamage;
    private int lifetimeTicks;
    private int livedTicks;
    private boolean exploded;

    public CindervaneFireballEntity(EntityType<? extends CindervaneFireballEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        this.refreshDimensions();
    }

    public CindervaneFireballEntity(Level level, Vec3 pos, Cindervane owner,
                                    double impactRadius, float impactDamage, int lifetimeTicks) {
        this(ModEntities.CINDERVANE_MAGMA_BLOCK.get(), level);
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

        livedTicks++;
        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.04D, 0.0D));
        }
        if (!level().isClientSide && checkImpactCollision()) {
            explode();
            return;
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (!level().isClientSide) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.98D));

            if (livedTicks > lifetimeTicks) {
                explode();
                return;
            }
            if (this.onGround()) {
                Vec3 motion = this.getDeltaMovement();
                this.setDeltaMovement(motion.x * 0.7D, -motion.y * 0.5D, motion.z * 0.7D);
                explode();
                return;
            }
        } else {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.98D));
            spawnTrailParticles();
        }
    }

    private void spawnTrailParticles() {
        Vec3 velocity = getDeltaMovement();
        for (int i = 0; i < 4; i++) {
            double along = (i + random.nextDouble()) / 4.0;
            double ox = (random.nextDouble() - 0.5) * 1.2;
            double oy = (random.nextDouble() - 0.5) * 1.2;
            double oz = (random.nextDouble() - 0.5) * 1.2;
            level().addParticle(i == 0
                            ? ModParticles.CINDERVANE_FIRE_TRAIL.get()
                            : i == 1 ? ModParticles.CINDERVANE_SPEC_TRAIL.get()
                            : i == 2 ? ModParticles.CINDERVANE_BETTER_FIRE_TRAIL.get()
                            : ModParticles.CINDERVANE_FIREBALL_FIRE_TRAIL.get(), true,
                    getX() - velocity.x * along + ox, getY() + getBbHeight() * 0.5 - velocity.y * along + oy,
                    getZ() - velocity.z * along + oz,
                    velocity.x * 0.12 + ox * 0.10, velocity.y * 0.12 + oy * 0.10 + 0.025,
                    velocity.z * 0.12 + oz * 0.10);
        }
    }

    private boolean checkImpactCollision() {
        Vec3 start = position();
        Vec3 end = start.add(getDeltaMovement());
        HitResult blockHit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            setPos(blockHit.getLocation());
            return true;
        }

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                level(),
                this,
                start,
                end,
                getBoundingBox().expandTowards(getDeltaMovement()).inflate(0.75D),
                this::canImpactEntity);
        if (entityHit != null) {
            setPos(entityHit.getLocation());
            return true;
        }
        return false;
    }

    private boolean canImpactEntity(Entity entity) {
        if (entity == this) {
            return false;
        }
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        if (!living.isAlive() || living.isRemoved()) {
            return false;
        }
        if (living == owner) {
            return false;
        }
        return owner == null || !owner.isAlly(living);
    }

    private void explode() {
        if (exploded) {
            return;
        }
        exploded = true;
        if (!(level() instanceof ServerLevel server)) {
            discard();
            return;
        }

        Vec3 impact = position();
        spawnImpactEffects(server, impact);
        server.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE, getSoundSource(), 0.7F, 1.1F);

        AABB area = new AABB(impact.x - impactRadius, impact.y - impactRadius, impact.z - impactRadius,
                impact.x + impactRadius, impact.y + impactRadius, impact.z + impactRadius);
        List<net.minecraft.world.entity.LivingEntity> hits = server.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, area,
                target -> target.isAlive()
                        && target != owner
                        && (owner == null || !owner.isAlly(target))
                        && !DragonElementalImmunity.isFireImmune(target));

        for (net.minecraft.world.entity.LivingEntity target : hits) {
            target.hurt(server.damageSources().explosion(this, owner != null ? owner : this), impactDamage);
            target.setSecondsOnFire(4);
        }

        igniteArea(server, BlockPos.containing(impact));
        discard();
    }

    private void spawnImpactEffects(ServerLevel server, Vec3 impact) {
        HitResult ground = server.clip(new ClipContext(impact.add(0, 0.5, 0), impact.add(0, -6, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        for (var player : server.players()) {
            if (player.distanceToSqr(impact) > 256.0D * 256.0D) continue;
            server.sendParticles(player, ModParticles.CINDERVANE_IMPACT_EMITTER.get(), true,
                    impact.x, impact.y + 0.4D, impact.z, 24, 0.12D, 0.08D, 0.12D, 0);
            server.sendParticles(player, ModParticles.CINDERVANE_FIRE_EXPLOSION.get(), true,
                    impact.x, impact.y + 1.2D, impact.z, 1, 0, 0, 0, 0);
            server.sendParticles(player, ModParticles.CINDERVANE_SMALL_EXPLOSION.get(), true,
                    impact.x, impact.y + 0.6D, impact.z, 1, 0, 0, 0, 0);
            if (ground.getType() == HitResult.Type.BLOCK) {
                Vec3 point = ground.getLocation();
                server.sendParticles(player, ModParticles.CINDERVANE_GROUND_IMPACT.get(), true,
                        point.x, point.y + 0.04D, point.z, 1, 0, 0, 0, 0);
            }
        }
    }

    private void igniteArea(ServerLevel server, BlockPos base) {
        if (!DragonGriefingRules.canSetBlocksOnFire(server)) {
            return;
        }
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
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet) {
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
        return distance < 256.0D * 256.0D;
    }

    @Override
    protected boolean canAddPassenger(@NotNull Entity passenger) {
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
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        return EntityDimensions.fixed(0.98F, 0.98F);
    }

    @Nullable
    public Cindervane getOwner() {
        return owner;
    }
}
