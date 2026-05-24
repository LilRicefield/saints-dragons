package com.leon.saintsdragons.server.entity.dragons;

import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModParticles;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.controller.BodyControl;
import com.leon.saintsdragons.server.entity.controller.GenericLookControl;
import com.leon.saintsdragons.util.animation.MovementAnimationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Mossback extends Animal implements GeoEntity {
    private static final int TOXIN_EFFECT_TICKS = 20 * 10;
    private static final double TOXIN_RADIUS = 3.25D;
    private static final int TOXIN_PARTICLES = 36;

    private static final EntityDataAccessor<Boolean> DATA_THROWN =
            SynchedEntityData.defineId(Mossback.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_THROWN_AIRBORNE =
            SynchedEntityData.defineId(Mossback.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_LANDED_TICKS =
            SynchedEntityData.defineId(Mossback.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.mossback.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.mossback.walk");
    private static final RawAnimation THROWN = RawAnimation.begin().thenLoop("animation.mossback.thrown");
    private static final RawAnimation LANDED = RawAnimation.begin().thenLoop("animation.mossback.landed");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public Mossback(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
        this.lookControl = new GenericLookControl(this);
        this.moveControl = new MossbackMoveControl(this);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_THROWN, false);
        this.entityData.define(DATA_THROWN_AIRBORNE, false);
        this.entityData.define(DATA_LANDED_TICKS, 0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.15D);
    }

    public static boolean canSpawnHere(EntityType<Mossback> type,
                                       LevelAccessor level,
                                       MobSpawnType spawnType,
                                       BlockPos pos,
                                       RandomSource random) {
        return Animal.checkAnimalSpawnRules(type, level, spawnType, pos, random);
    }

    public void markThrown() {
        setThrown(true);
        setThrownAirborne(true);
        setLandedTicks(0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.3D));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    protected @NotNull BodyRotationControl createBodyControl() {
        return new BodyControl(this, 0.7F);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            return;
        }

        int landedTicks = getLandedTicks();
        if (landedTicks > 0) {
            setLandedTicks(landedTicks - 1);
        }

        if (isThrown() && !onGround()) {
            setThrownAirborne(true);
        }

        if (isThrown() && isThrownAirborne() && onGround()) {
            setThrown(false);
            setThrownAirborne(false);
            setLandedTicks(12);
            spawnLandingEffects();
            releaseToxin();
        }
    }

    private boolean isThrown() {
        return this.entityData.get(DATA_THROWN);
    }

    private void setThrown(boolean thrown) {
        this.entityData.set(DATA_THROWN, thrown);
    }

    private boolean isThrownAirborne() {
        return this.entityData.get(DATA_THROWN_AIRBORNE);
    }

    private void setThrownAirborne(boolean airborne) {
        this.entityData.set(DATA_THROWN_AIRBORNE, airborne);
    }

    private int getLandedTicks() {
        return this.entityData.get(DATA_LANDED_TICKS);
    }

    private void setLandedTicks(int ticks) {
        this.entityData.set(DATA_LANDED_TICKS, Math.max(0, ticks));
    }

    private void spawnLandingEffects() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.15D, this.getZ(),
                8, 0.18D, 0.08D, 0.18D, 0.02D);
        this.playSound(ModSounds.MOSSBACK_LANDED.get(), 0.7F, 0.95F + this.getRandom().nextFloat() * 0.12F);
    }

    private void releaseToxin() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        RandomSource random = this.getRandom();
        for (int i = 0; i < TOXIN_PARTICLES; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double speed = 0.035D + random.nextDouble() * 0.085D;
            double xSpeed = Math.cos(angle) * speed;
            double zSpeed = Math.sin(angle) * speed;
            double ySpeed = 0.025D + random.nextDouble() * 0.055D;
            double x = this.getX() + (random.nextDouble() - 0.5D) * 0.9D;
            double y = this.getY() + 0.25D + random.nextDouble() * 0.35D;
            double z = this.getZ() + (random.nextDouble() - 0.5D) * 0.9D;
            serverLevel.sendParticles(ModParticles.MOSSBACK_POISON_FUME.get(), x, y, z, 0,
                    xSpeed, ySpeed, zSpeed, 1.0D);
        }

        AABB toxinArea = this.getBoundingBox().inflate(TOXIN_RADIUS, 1.4D, TOXIN_RADIUS);
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, toxinArea, target -> target != this)) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, TOXIN_EFFECT_TICKS, 0));
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, TOXIN_EFFECT_TICKS, 0));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, TOXIN_EFFECT_TICKS, 0));
        }

        this.playSound(ModSounds.MOSSBACK_TOXIN.get(), 0.9F, 0.9F + random.nextFloat() * 0.18F);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && amount > 0.0F && !this.level().isClientSide) {
            releaseToxin();
        }
        return hurt;
    }

    @Override
    public boolean canBeAffected(@NotNull MobEffectInstance effectInstance) {
        if (effectInstance.getEffect() == MobEffects.POISON) {
            return false;
        }
        return super.canBeAffected(effectInstance);
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return ModSounds.MOSSBACK_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.MOSSBACK_DIE.get();
    }

    @Override
    public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty()) {
            return super.mobInteract(player, hand);
        }

        if (!this.level().isClientSide) {
            ItemStack mossback = new ItemStack(ModItems.MOSSBACK.get());
            if (!player.getInventory().add(mossback)) {
                player.drop(mossback, false);
            }
            this.discard();
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, @NotNull DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, @NotNull BlockState state, @NotNull BlockPos pos) {
    }

    @Override
    public boolean isFood(@NotNull ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel level, @NotNull AgeableMob otherParent) {
        return null;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Thrown", isThrown());
        tag.putBoolean("ThrownAirborne", isThrownAirborne());
        tag.putInt("LandedTicks", getLandedTicks());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setThrown(tag.getBoolean("Thrown"));
        setThrownAirborne(tag.getBoolean("ThrownAirborne"));
        setLandedTicks(tag.getInt("LandedTicks"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "thrown", 1, state -> {
            if (isThrown() || isThrownAirborne()) {
                state.setAndContinue(THROWN);
                return PlayState.CONTINUE;
            }
            return PlayState.STOP;
        }));

        controllers.add(new AnimationController<>(this, "landed", 1, state -> {
            if (getLandedTicks() > 0) {
                state.setAndContinue(LANDED);
                return PlayState.CONTINUE;
            }
            return PlayState.STOP;
        }));

        controllers.add(new AnimationController<>(this, "movement", 4, state -> {
            if (isThrown() || isThrownAirborne()) {
                return PlayState.STOP;
            }
            if (getLandedTicks() > 0) {
                return PlayState.STOP;
            }
            if (state.isMoving()) {
                MovementAnimationHelper.setAndContinue(state, WALK);
            } else {
                MovementAnimationHelper.setAndContinue(state, IDLE);
            }
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    private static final class MossbackMoveControl extends MoveControl {
        private final Mossback mossback;

        private MossbackMoveControl(Mossback mossback) {
            super(mossback);
            this.mossback = mossback;
        }

        @Override
        public void tick() {
            super.tick();
            if (this.mossback.onGround() && this.mossback.getDeltaMovement().horizontalDistanceSqr() > 0.0001D) {
                this.mossback.setYRot(this.mossback.yBodyRot);
            }
        }
    }

}
