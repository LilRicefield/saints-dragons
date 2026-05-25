package com.leon.saintsdragons.server.entity.effect.volitans;

import com.leon.saintsdragons.common.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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

public class VolitansBurrowMoundEntity extends Entity implements GeoEntity {
    private static final RawAnimation SPAWN =
            RawAnimation.begin().thenPlay("animation.burrow_mound.spawn");
    private static final EntityDimensions DIMENSIONS = EntityDimensions.fixed(9.0F, 4.5F);
    private static final int LIFETIME_TICKS = 38;
    private static final int FADE_TICKS = 12;
    private static final EntityDataAccessor<BlockState> DATA_BLOCK_STATE =
            SynchedEntityData.defineId(VolitansBurrowMoundEntity.class, EntityDataSerializers.BLOCK_STATE);
    private static final EntityDataAccessor<Float> DATA_VISUAL_YAW =
            SynchedEntityData.defineId(VolitansBurrowMoundEntity.class, EntityDataSerializers.FLOAT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int age;

    public VolitansBurrowMoundEntity(EntityType<? extends VolitansBurrowMoundEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public VolitansBurrowMoundEntity(Level level, Vec3 pos, float yaw, BlockState blockState) {
        this(ModEntities.VOLITANS_BURROW_MOUND.get(), level);
        setPos(pos);
        setBlockState(blockState);
        initializeRotation(yaw);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_BLOCK_STATE, Blocks.DIRT.defaultBlockState());
        this.entityData.define(DATA_VISUAL_YAW, 0.0F);
    }

    public BlockState getBlockState() {
        return this.entityData.get(DATA_BLOCK_STATE);
    }

    public void setBlockState(BlockState state) {
        this.entityData.set(DATA_BLOCK_STATE, state.isAir() ? Blocks.DIRT.defaultBlockState() : state);
    }

    public float getVisualYaw() {
        return this.entityData.get(DATA_VISUAL_YAW);
    }

    public float getOpacity(float partialTick) {
        return 1.0F - getFadeProgress(partialTick);
    }

    public float getFadeProgress(float partialTick) {
        float fadeAge = age + partialTick - (LIFETIME_TICKS - FADE_TICKS);
        if (fadeAge <= 0.0F) {
            return 0.0F;
        }
        return Math.min(fadeAge / (float) FADE_TICKS, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        age++;
        if (!level().isClientSide && age >= LIFETIME_TICKS) {
            discard();
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
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        age = tag.getInt("Age");
        if (tag.contains("BlockState", CompoundTag.TAG_COMPOUND)) {
            setBlockState(NbtUtils.readBlockState(level().holderLookup(Registries.BLOCK), tag.getCompound("BlockState")));
        }
        if (tag.contains("VisualYaw")) {
            initializeRotation(tag.getFloat("VisualYaw"));
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putInt("Age", age);
        tag.put("BlockState", NbtUtils.writeBlockState(getBlockState()));
        tag.putFloat("VisualYaw", getVisualYaw());
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        initializeRotation(packet.getYRot());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 16384.0D;
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        return DIMENSIONS;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::animationPredicate));
    }

    private <E extends GeoEntity> PlayState animationPredicate(AnimationState<E> state) {
        state.getController().setAnimation(SPAWN);
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private void initializeRotation(float yaw) {
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
        this.entityData.set(DATA_VISUAL_YAW, yaw);
        this.yRotO = yaw;
        this.xRotO = 0.0F;
    }

    public static Vec3 surfacePosition(BlockPos groundPos) {
        return Vec3.atBottomCenterOf(groundPos.above());
    }
}
