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
        this.noPhysics = true; // Disable block collision physics
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
        // Don't call super.tick() - handle movement manually

        if (this.getBlockState().isAir()) {
            discard();
            return;
        }

        livedTicks++;

        // Gradual arc: gravity increases over time for a natural arc trajectory
        // Starts with no gravity, then gradually increases
        if (!this.isNoGravity()) {
            // Gravity ramps up over the first 20 ticks, then stays constant
            float gravityProgress = Math.min(1.0f, livedTicks / 20.0f);
            double gravity = -0.05D * gravityProgress; // Max gravity of -0.05
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, gravity, 0.0D));
        }

        Vec3 currentPos = this.position();
        Vec3 motion = this.getDeltaMovement();
        Vec3 nextPos = currentPos.add(motion);

        // Raycast to check for block collision along the path
        net.minecraft.world.phys.BlockHitResult hitResult = level().clip(new net.minecraft.world.level.ClipContext(
                currentPos,
                nextPos,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                this
        ));

        boolean hitBlock = hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK;

        if (hitBlock) {
            // Move to impact point
            this.setPos(hitResult.getLocation());
        } else {
            // Move freely without physics collision
            this.setPos(nextPos);
        }

        // Server-side logic only
        if (!level().isClientSide) {
            // Apply air resistance
            if (!this.isNoGravity()) {
                this.setDeltaMovement(this.getDeltaMovement().scale(0.99D));
            }

            // Check lifetime
            if (livedTicks > lifetimeTicks) {
                explode();
                return;
            }

            // Explode on block hit
            if (hitBlock) {
                explode();
                return;
            }
        } else {
            // Client-side: apply same air resistance for prediction
            if (!this.isNoGravity()) {
                this.setDeltaMovement(this.getDeltaMovement().scale(0.99D));
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
        BlockPos impactPos = BlockPos.containing(impact);

        // Core explosion particles - scale with fireball size
        server.sendParticles(ParticleTypes.LAVA, impact.x, impact.y + 0.5D * scale, impact.z, (int)(20 * scale),
                0.6D * scale, 0.4D * scale, 0.6D * scale, 0.05D);
        server.sendParticles(ParticleTypes.FLAME, impact.x, impact.y + 0.5D * scale, impact.z, (int)(35 * scale),
                0.7D * scale, 0.5D * scale, 0.7D * scale, 0.1D);

        // Enhanced effects for larger fireballs (charge level 2+)
        if (scale >= 6.0F) {
            // Multiple explosion clouds spread around
            server.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y + 1.0D, impact.z, (int)(5 * (scale / 4.0F)),
                    1.0D * scale, 0.6D * scale, 1.0D * scale, 0.02D);
            // Dense smoke plume
            server.sendParticles(ParticleTypes.LARGE_SMOKE, impact.x, impact.y + 0.5D * scale, impact.z, (int)(40 * scale),
                    0.9D * scale, 0.7D * scale, 0.9D * scale, 0.06D);
            // Campfire sparks rising
            server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, impact.x, impact.y + 0.3D * scale, impact.z, (int)(12 * scale),
                    0.5D * scale, 0.3D * scale, 0.5D * scale, 0.04D);
            // Falling debris particles
            server.sendParticles(ParticleTypes.ASH, impact.x, impact.y + 2.0D * scale, impact.z, (int)(30 * scale),
                    1.2D * scale, 1.0D * scale, 1.2D * scale, 0.1D);

            // Destroy blocks for charge level 2+ (radius 6 = ~13 block diameter crater)
            destroyBlocks(server, impactPos, 6, false);
        }

        // Max charge explosion (charge level 3) - DEVASTATING
        if (scale >= 8.0F) {
            // Multiple explosion emitters spread across the blast zone
            server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, impact.x, impact.y + 0.5D, impact.z, 1,
                    0.0D, 0.0D, 0.0D, 0.0D);
            for (int i = 0; i < 8; i++) {
                double angle = (i / 8.0) * Math.PI * 2;
                double offsetX = Math.cos(angle) * 4.0;
                double offsetZ = Math.sin(angle) * 4.0;
                server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, impact.x + offsetX, impact.y + 0.5D, impact.z + offsetZ, 1,
                        0.0D, 0.0D, 0.0D, 0.0D);
            }

            // Soul fire ring
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, impact.x, impact.y + 0.5D * scale, impact.z, (int)(60 * scale),
                    2.0D * scale, 1.0D * scale, 2.0D * scale, 0.2D);
            // Massive lava burst
            server.sendParticles(ParticleTypes.LAVA, impact.x, impact.y + 1.0D * scale, impact.z, (int)(80 * scale),
                    2.5D * scale, 1.5D * scale, 2.5D * scale, 0.1D);
            // Shockwave dust ring
            server.sendParticles(ParticleTypes.CLOUD, impact.x, impact.y + 0.2D, impact.z, (int)(50 * scale),
                    3.0D * scale, 0.5D, 3.0D * scale, 0.03D);
            // Falling ember rain
            server.sendParticles(ParticleTypes.FALLING_LAVA, impact.x, impact.y + 8.0D, impact.z, (int)(100 * scale),
                    4.0D * scale, 2.0D, 4.0D * scale, 0.0D);

            // Massive block destruction radius for max charge (radius 12 = ~25 block diameter crater)
            destroyBlocks(server, impactPos, 12, true);
        }

        // Sound - louder and lower pitch for bigger explosions
        float volume = 1.0F + (scale * 0.2F);
        float pitch = Math.max(0.4F, 0.9F / scale);
        server.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE, getSoundSource(), volume, pitch);

        // Additional dramatic sound for max charge
        if (scale >= 8.0F) {
            server.playSound(null, blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, getSoundSource(), 0.6F, 0.6F);
        }

        AABB area = new AABB(impact.x - impactRadius, impact.y - impactRadius, impact.z - impactRadius,
                impact.x + impactRadius, impact.y + impactRadius, impact.z + impactRadius);
        List<net.minecraft.world.entity.LivingEntity> hits = server.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, area,
                target -> target.isAlive() && target != owner && (owner == null || !owner.isAlly(target)));

        for (net.minecraft.world.entity.LivingEntity target : hits) {
            target.hurt(server.damageSources().explosion(this, owner != null ? owner : this), impactDamage);
            target.setSecondsOnFire((int)(4 * scale));

            // Knockback for larger explosions - stronger for max charge
            if (scale >= 6.0F) {
                double knockbackStrength = scale >= 8.0F ? scale * 0.25D : scale * 0.15D;
                double upwardBoost = scale >= 8.0F ? 0.5D : 0.3D;
                Vec3 knockback = target.position().subtract(impact).normalize().scale(knockbackStrength);
                target.push(knockback.x, knockback.y + upwardBoost, knockback.z);
            }
        }

        igniteArea(server, impactPos);
        discard();
    }

    private void destroyBlocks(ServerLevel server, BlockPos center, int radius, boolean maxPower) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    // Spherical destruction pattern
                    double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (distance > radius) continue;

                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = server.getBlockState(pos);

                    // Skip air and unbreakable blocks
                    if (state.isAir()) continue;
                    float hardness = state.getDestroySpeed(server, pos);
                    if (hardness < 0) continue; // Bedrock, etc.

                    // Don't destroy obsidian (hardness 50) unless max power
                    if (!maxPower && hardness > 50.0F) continue;
                    if (hardness > 100.0F) continue; // Never destroy reinforced blocks

                    // Max power mode: guaranteed destruction in larger core, destroys stone easily
                    if (maxPower) {
                        // Inner 60%: guaranteed destruction
                        if (distance <= radius * 0.6) {
                            server.destroyBlock(pos, true, owner);
                        }
                        // Outer ring: very high chance, ignores hardness
                        else {
                            double distanceFactor = 1.0 - ((distance - radius * 0.6) / (radius * 0.4));
                            if (server.random.nextDouble() < distanceFactor * 0.9) {
                                server.destroyBlock(pos, true, owner);
                            }
                        }
                    } else {
                        // Normal mode: Inner 50% guaranteed, outer ring has chance
                        if (distance <= radius * 0.5) {
                            server.destroyBlock(pos, true, owner);
                        } else {
                            double distanceFactor = 1.0 - ((distance - radius * 0.5) / (radius * 0.5));
                            double hardnessFactor = hardness > 3.0F ? 0.8 : 1.0;
                            double breakChance = distanceFactor * hardnessFactor;
                            if (server.random.nextDouble() < breakChance) {
                                server.destroyBlock(pos, true, owner);
                            }
                        }
                    }
                }
            }
        }
    }

    private void igniteArea(ServerLevel server, BlockPos base) {
        float scale = getVisualScale();
        int radius = (int) Math.ceil(scale) + 1; // Slightly larger fire spread

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                // Check multiple Y levels to handle terrain
                for (int dy = -radius; dy <= radius; dy++) {
                    BlockPos pos = base.offset(dx, dy, dz);
                    BlockState state = server.getBlockState(pos);

                    // Need air to place fire
                    if (!state.isAir()) continue;

                    BlockPos below = pos.below();
                    BlockState belowState = server.getBlockState(below);

                    // Place fire if there's a solid block below
                    if (!belowState.isAir() && belowState.isSolid() && Blocks.FIRE.defaultBlockState().canSurvive(server, pos)) {
                        // Higher chance closer to center
                        double distance = Math.sqrt(dx * dx + dz * dz);
                        double chance = 1.0 - (distance / (radius + 1)) * 0.5;
                        if (server.random.nextDouble() < chance) {
                            server.setBlock(pos, Blocks.FIRE.defaultBlockState(), 11);
                        }
                        break; // Only place one fire per column
                    }
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
