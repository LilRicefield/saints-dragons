package com.leon.saintsdragons.server.entity.effect.ignivorus;

import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModParticles;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.ai.dragonbrain.learning.DragonCombatLearning;
import com.leon.saintsdragons.server.entity.dragons.util.DragonElementalImmunity;
import com.leon.saintsdragons.server.entity.dragons.util.DragonGriefingRules;
import net.minecraft.core.BlockPos;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;

import javax.annotation.Nullable;
import java.util.List;

public class IgnivorusFireballEntity extends Entity implements software.bernie.geckolib.animatable.GeoEntity {
    private final AnimatableInstanceCache animationCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    public float getVisualAge(float partialTick) { return livedTicks + partialTick; }

    private static final EntityDataAccessor<BlockState> DATA_BLOCK_STATE =
            SynchedEntityData.defineId(IgnivorusFireballEntity.class, EntityDataSerializers.BLOCK_STATE);
    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(IgnivorusFireballEntity.class, EntityDataSerializers.FLOAT);

    private Ignivorus owner;
    private double impactRadius;
    private float impactDamage;
    private int lifetimeTicks;
    private int livedTicks;
    private double distanceTravelled;
    private long learningTrial;
    private double learningExpectedDistance;

    private static final double STAGE_ONE_STRAIGHT_DISTANCE = 25.0D;
    private static final double STAGE_TWO_STRAIGHT_DISTANCE = 50.0D;
    private static final double STAGE_THREE_STRAIGHT_DISTANCE = 85.0D;
    private static final double FIREBALL_GRAVITY = 0.01D;
    private static final SphereOffsets OFFSETS_RADIUS_6 = SphereOffsets.create(6);
    private static final SphereOffsets OFFSETS_RADIUS_12 = SphereOffsets.create(12);

    public IgnivorusFireballEntity(EntityType<? extends IgnivorusFireballEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        this.setNoGravity(true);
        this.refreshDimensions();
    }

    public IgnivorusFireballEntity(Level level, Vec3 pos, Ignivorus owner,
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

    public void trackCombatResult(long token, double expectedDistance) {
        learningTrial = token;
        learningExpectedDistance = expectedDistance;
    }

    public static Vec3 motionForTick(Vec3 motion, double travelled, float scale) {
        double straightDistance = scale >= 8.0F ? STAGE_THREE_STRAIGHT_DISTANCE
                : scale >= 6.0F ? STAGE_TWO_STRAIGHT_DISTANCE : STAGE_ONE_STRAIGHT_DISTANCE;
        return travelled >= straightDistance ? motion.add(0, -FIREBALL_GRAVITY, 0) : motion;
    }

    @Override
    public void tick() {

        if (this.getBlockState().isAir()) {
            discard();
            return;
        }
        livedTicks++;

        setDeltaMovement(motionForTick(getDeltaMovement(), distanceTravelled, getVisualScale()));
        Vec3 currentPos = this.position();
        Vec3 motion = this.getDeltaMovement();
        Vec3 nextPos = currentPos.add(motion);

        BlockHitResult hitResult = level().clip(new ClipContext(
                currentPos,
                nextPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));

        boolean hitBlock = hitResult.getType() == HitResult.Type.BLOCK;
        EntityHitResult entityHit = findEntityHit(currentPos, nextPos);
        boolean hitEntity = entityHit != null;

        if (hitEntity && hitBlock) {
            double blockDist = hitResult.getLocation().distanceToSqr(currentPos);
            double entityDist = entityHit.getLocation().distanceToSqr(currentPos);
            if (entityDist <= blockDist) {
                this.setPos(entityHit.getLocation());
            } else {
                this.setPos(hitResult.getLocation());
            }
        } else if (hitEntity) {
            this.setPos(entityHit.getLocation());
        } else if (hitBlock) {
            this.setPos(hitResult.getLocation());
        } else {
            this.setPos(nextPos);
        }

        distanceTravelled += position().distanceTo(currentPos);
        if (!level().isClientSide) {
            if (livedTicks > lifetimeTicks) {
                explode();
                return;
            }
            if (hitEntity) {
                explode();
                return;
            }

            if (hitBlock) {
                explode();
            }
        } else {
            spawnTrailParticles();
        }
    }

    @Nullable
    private EntityHitResult findEntityHit(Vec3 start, Vec3 end) {
        if (level().isClientSide) {
            return null;
        }
        AABB bounds = getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        return ProjectileUtil.getEntityHitResult(level(), this, start, end, bounds, target -> {
            if (!(target instanceof LivingEntity living)) {
                return false;
            }
            if (!living.isAlive()) {
                return false;
            }
            if (owner != null) {
                if (target == owner || owner.isAlliedTo(target)) {
                    return false;
                }
            }
            return true;
        });
    }

    private void spawnTrailParticles() {
        com.leon.saintsdragons.client.particle.IgnivorusFireballTrail.emit(this);
    }

    private void explode() {
        if (!(level() instanceof ServerLevel server)) {
            discard();
            return;
        }

        Vec3 impact = position();
        float scale = getVisualScale();
        BlockPos impactPos = BlockPos.containing(impact);
        boolean aiFireball = owner != null && owner.getControllingPassenger() == null;
        boolean allowGriefing = DragonGriefingRules.canDestroyBlocks(server);
        if (scale >= 8.0F) spawnStageThreeGroundImpact(server, impact);

        if (scale <= 4.01F) {
            spawnStageOneImpact(server, impact);
        } else if (scale >= 8.0F) {
            // Core explosion particles - scale with fireball size
            server.sendParticles(ParticleTypes.LAVA, impact.x, impact.y + 0.5D * scale, impact.z, capParticles(10, scale, 60),
                    0.6D * scale, 0.4D * scale, 0.6D * scale, 0.05D);
            server.sendParticles(ParticleTypes.FLAME, impact.x, impact.y + 0.5D * scale, impact.z, capParticles(14, scale, 70),
                    0.7D * scale, 0.5D * scale, 0.7D * scale, 0.1D);
        }

        if (scale >= 8.0F) {
            server.sendParticles(ParticleTypes.LARGE_SMOKE, impact.x, impact.y + 0.5D * scale, impact.z, capParticles(14, scale, 70),
                    0.9D * scale, 0.7D * scale, 0.9D * scale, 0.06D);
            server.sendParticles(ParticleTypes.ASH, impact.x, impact.y + 2.0D * scale, impact.z, capParticles(10, scale, 50),
                    1.2D * scale, 1.0D * scale, 1.2D * scale, 0.1D);
        }
        if (scale >= 6.0F && allowGriefing) {
            destroyBlocks(server, impactPos, 6, false);
        }
        if (scale >= 8.0F) {

            server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, impact.x, impact.y + 0.5D, impact.z, 1,
                    0.0D, 0.0D, 0.0D, 0.0D);
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, impact.x, impact.y + 0.5D * scale, impact.z, capParticles(16, scale, 60),
                    2.0D * scale, 1.0D * scale, 2.0D * scale, 0.2D);

            if (allowGriefing) {
                destroyBlocks(server, impactPos, 12, true);
            }
        }

        if (scale > 4.01F && scale < 8.0F) spawnStageTwoImpact(server, impact);

        float volume = 1.0F + (scale * 0.2F);
        float pitch = Math.max(0.4F, 0.9F / scale);
        server.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE, getSoundSource(), volume, pitch);

        AABB area = new AABB(impact.x - impactRadius, impact.y - impactRadius, impact.z - impactRadius,
                impact.x + impactRadius, impact.y + impactRadius, impact.z + impactRadius);
        List<LivingEntity> hits = server.getEntitiesOfClass(LivingEntity.class, area,
                target -> target.isAlive()
                        && target != owner
                        && (owner == null || !owner.isAlly(target)));

        for (LivingEntity target : hits) {
            if (DragonElementalImmunity.isFireImmune(target)) {
                if (owner != null) owner.getCombatLearning().recordContact(learningTrial, target);
                continue;
            }
            boolean damaged = target.hurt(server.damageSources().explosion(this, owner != null ? owner : this), impactDamage);
            if (owner != null) {
                if (damaged) owner.getCombatLearning().recordHit(learningTrial, target);
                else owner.getCombatLearning().recordContact(learningTrial, target);
            }
            target.setSecondsOnFire((int)(4 * scale));

            // Knockback for larger explosions - stronger for max charge
            if (scale >= 6.0F) {
                double knockbackStrength = scale >= 8.0F ? scale * 0.25D : scale * 0.15D;
                double upwardBoost = scale >= 8.0F ? 0.5D : 0.3D;
                Vec3 knockback = target.position().subtract(impact).normalize().scale(knockbackStrength);
                target.push(knockback.x, knockback.y + upwardBoost, knockback.z);
            }
        }

        if (allowGriefing) {
            igniteArea(server, impactPos);
        }
        if (owner != null && learningTrial != 0) {
            // An early collision prevented the intended shot; a hit still counts through either outcome.
            owner.getCombatLearning().finishAttack(learningTrial,
                    distanceTravelled + impactRadius < learningExpectedDistance
                            ? DragonCombatLearning.Outcome.BLOCKED : DragonCombatLearning.Outcome.COMPLETED);
            learningTrial = 0;
        }
        discard();
    }

    private void spawnStageTwoImpact(ServerLevel server, Vec3 impact) {
        HitResult ground = server.clip(new ClipContext(impact.add(0, 0.5, 0), impact.add(0, -12, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        for (var player : server.players()) {
            if (player.distanceToSqr(impact) > 256.0D * 256.0D) continue;
            server.sendParticles(player, ModParticles.CINDERVANE_IMPACT_EMITTER.get(), true,
                    impact.x, impact.y + 0.9D, impact.z, 72, 0.3D, 0.18D, 0.3D, 0);
            server.sendParticles(player, ModParticles.IGNIVORUS_LEVEL_TWO_IMPACT_EXPLOSION.get(), true,
                    impact.x, impact.y + 2.7D, impact.z, 1, 0, 0, 0, 0);
            server.sendParticles(player, ModParticles.IGNIVORUS_LEVEL_TWO_IMPACT_SMALL_EXPLOSION.get(), true,
                    impact.x, impact.y + 1.35D, impact.z, 1, 0, 0, 0, 0);
            server.sendParticles(player, ModParticles.IGNIVORUS_LEVEL_TWO_IMPACT_TOON.get(), true,
                    impact.x, impact.y + 2.0D, impact.z, 1, 0, 0, 0, 0);
            if (ground.getType() == HitResult.Type.BLOCK) {
                Vec3 point = ground.getLocation();
                server.sendParticles(player, ModParticles.IGNIVORUS_LEVEL_TWO_IMPACT_GROUND.get(), true,
                        point.x, point.y + 0.04D, point.z, 1, 0, 0, 0, 0);
                server.sendParticles(player, ModParticles.IGNIVORUS_LEVEL_TWO_IMPACT_CIRCLE.get(), true,
                        point.x, point.y + 0.07D, point.z, 1, 0, 0, 0, 0);
            }
        }
    }

    private void spawnStageThreeGroundImpact(ServerLevel server, Vec3 impact) {
        HitResult ground = server.clip(new ClipContext(impact.add(0, 0.5D, 0), impact.add(0, -12, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (ground.getType() != HitResult.Type.BLOCK) return;
        Vec3 point = ground.getLocation();
        for (var player : server.players()) {
            if (player.distanceToSqr(point) > 256.0D * 256.0D) continue;
            server.sendParticles(player, ModParticles.IGNIVORUS_LEVEL_THREE_GROUND_SMOKE.get(), true,
                    point.x, point.y + 0.04D, point.z, 1, 0, 0, 0, 0);
            server.sendParticles(player, ModParticles.IGNIVORUS_LEVEL_THREE_GROUND_RING.get(), true,
                    point.x, point.y + 0.07D, point.z, 1, 0, 0, 0, 0);
            server.sendParticles(player, ModParticles.IGNIVORUS_LEVEL_THREE_GROUND_GLITTER.get(), true,
                    point.x, point.y + 0.10D, point.z, 1, 0, 0, 0, 0);
            server.sendParticles(player, ModParticles.IGNIVORUS_LEVEL_THREE_SPLATTER_LARGE.get(), true,
                    point.x, point.y + 3.0D, point.z, 1, 0, 0, 0, 0);
            server.sendParticles(player, ModParticles.IGNIVORUS_LEVEL_THREE_SPLATTER_SMALL.get(), true,
                    point.x, point.y + 1.5D, point.z, 1, 0, 0, 0, 0);
            server.sendParticles(player, ModParticles.IGNIVORUS_LEVEL_THREE_TOON_EXPLOSION.get(), true,
                    point.x, point.y + 4.0D, point.z, 1, 0, 0, 0, 0);
        }
    }

    private void spawnStageOneImpact(ServerLevel server, Vec3 impact) {
        HitResult ground = server.clip(new ClipContext(impact.add(0, 0.5, 0), impact.add(0, -6, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        for (var player : server.players()) {
            if (player.distanceToSqr(impact) > 256.0D * 256.0D) continue;
            server.sendParticles(player, ModParticles.CINDERVANE_IMPACT_EMITTER.get(), true,
                    impact.x, impact.y + 0.6D, impact.z, 48, 0.18D, 0.12D, 0.18D, 0);
            server.sendParticles(player, ModParticles.IGNIVORUS_FIREBALL_EXPLOSION.get(), true,
                    impact.x, impact.y + 1.8D, impact.z, 1, 0, 0, 0, 0);
            server.sendParticles(player, ModParticles.IGNIVORUS_FIREBALL_SMALL_EXPLOSION.get(), true,
                    impact.x, impact.y + 0.9D, impact.z, 1, 0, 0, 0, 0);
            if (ground.getType() == HitResult.Type.BLOCK) {
                Vec3 point = ground.getLocation();
                server.sendParticles(player, ModParticles.IGNIVORUS_FIREBALL_GROUND_IMPACT.get(), true,
                        point.x, point.y + 0.04D, point.z, 1, 0, 0, 0, 0);
            }
        }
    }

    private void destroyBlocks(ServerLevel server, BlockPos center, int radius, boolean maxPower) {
        SphereOffsets offsets = radius == 12 ? OFFSETS_RADIUS_12 : OFFSETS_RADIUS_6;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        double innerCore = maxPower ? radius * 0.6 : radius * 0.5;
        double outerRadius = radius - innerCore;

        for (int i = 0; i < offsets.size; i++) {
            int dx = offsets.dx[i];
            int dy = offsets.dy[i];
            int dz = offsets.dz[i];
            float distance = offsets.distance[i];
            pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);

            BlockState state = server.getBlockState(pos);

            // Skip air and unbreakable blocks
            if (state.isAir()) continue;
            float hardness = state.getDestroySpeed(server, pos);
            if (hardness < 0) continue; // Bedrock, etc.

            // Don't destroy obsidian (hardness 50) unless max power
            if (!maxPower && hardness > 50.0F) continue;
            if (hardness > 100.0F) continue; // Never destroy reinforced blocks

            if (distance <= innerCore) {
                server.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
                continue;
            }

            double distanceFactor = 1.0 - ((distance - innerCore) / outerRadius);
            if (maxPower) {
                if (server.random.nextDouble() < distanceFactor * 0.9) {
                    server.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
                }
            } else {
                double hardnessFactor = hardness > 3.0F ? 0.8 : 1.0;
                double breakChance = distanceFactor * hardnessFactor;
                if (server.random.nextDouble() < breakChance) {
                    server.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
                }
            }
        }
    }

    private void igniteArea(ServerLevel server, BlockPos base) {
        if (!DragonGriefingRules.canSetBlocksOnFire(server)) {
            return;
        }
        float scale = getVisualScale();
        int radius = (int) Math.ceil(scale) + 1; // Slightly larger fire spread
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();
        int minY = base.getY() - radius;
        int maxY = base.getY() + radius;
        int radiusSq = (radius + 1) * (radius + 1);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radiusSq) {
                    continue;
                }
                for (int y = maxY; y >= minY; y--) {
                    pos.set(base.getX() + dx, y, base.getZ() + dz);
                    BlockState state = server.getBlockState(pos);
                    if (!state.isAir()) {
                        continue;
                    }

                    below.set(pos.getX(), y - 1, pos.getZ());
                    BlockState belowState = server.getBlockState(below);
                    if (belowState.isAir() || !belowState.isSolid() || !Blocks.FIRE.defaultBlockState().canSurvive(server, pos)) {
                        continue;
                    }

                    double distance = Math.sqrt(dx * dx + dz * dz);
                    double chance = 1.0 - (distance / (radius + 1)) * 0.5;
                    if (server.random.nextDouble() < chance) {
                        server.setBlock(pos, Blocks.FIRE.defaultBlockState(), 11);
                    }
                    break;
                }
            }
        }
    }


    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        this.livedTicks = tag.getInt("Lived");
        this.distanceTravelled = tag.getDouble("DistanceTravelled");
        this.lifetimeTicks = tag.getInt("Lifetime");
        this.impactRadius = tag.getDouble("ImpactRadius");
        this.impactDamage = tag.getFloat("ImpactDamage");
        if (tag.contains("BlockState", CompoundTag.TAG_COMPOUND) && level() instanceof ServerLevel server) {
            BlockState state = NbtUtils.readBlockState(server.holderLookup(Registries.BLOCK), tag.getCompound("BlockState"));
            setBlockState(state.isAir() ? Blocks.MAGMA_BLOCK.defaultBlockState() : state);
        }
        if (tag.contains("Scale")) {
            setVisualScale(tag.getFloat("Scale"));
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putInt("Lived", livedTicks);
        tag.putDouble("DistanceTravelled", distanceTravelled);
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
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 16384.0D;
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
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        return false;
    }

    @Override
    public double getPassengersRidingOffset() {
        return -0.2D * getVisualScale();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
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

    @Nullable
    public Ignivorus getOwner() {
        return owner;
    }

    private static int capParticles(int base, float scale, int max) {
        return Math.min(max, Math.max(1, (int) (base * scale)));
    }

    private static final class SphereOffsets {
        private final int[] dx;
        private final int[] dy;
        private final int[] dz;
        private final float[] distance;
        private final int size;

        private SphereOffsets(int[] dx, int[] dy, int[] dz, float[] distance, int size) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.distance = distance;
            this.size = size;
        }

        private static SphereOffsets create(int radius) {
            int maxCount = (radius * 2 + 1);
            maxCount = maxCount * maxCount * maxCount;
            int[] dx = new int[maxCount];
            int[] dy = new int[maxCount];
            int[] dz = new int[maxCount];
            float[] distance = new float[maxCount];
            int count = 0;
            int radiusSq = radius * radius;
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        int distSq = x * x + y * y + z * z;
                        if (distSq > radiusSq) {
                            continue;
                        }
                        dx[count] = x;
                        dy[count] = y;
                        dz[count] = z;
                        distance[count] = (float) Math.sqrt(distSq);
                        count++;
                    }
                }
            }
            return new SphereOffsets(dx, dy, dz, distance, count);
        }
    }
}
