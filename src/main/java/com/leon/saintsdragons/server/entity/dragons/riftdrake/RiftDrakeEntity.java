package com.leon.saintsdragons.server.entity.dragons.riftdrake;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.ai.navigation.DragonPathNavigateGround;
import com.leon.saintsdragons.server.ai.navigation.DragonSwimMoveControl;
import com.leon.saintsdragons.server.ai.navigation.DragonSwimNavigate;
import com.leon.saintsdragons.server.entity.dragons.riftdrake.handlers.RiftDrakeAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.riftdrake.handlers.RiftDrakeFindWaterGoal;
import com.leon.saintsdragons.server.entity.dragons.riftdrake.handlers.RiftDrakeLeaveWaterGoal;
import com.leon.saintsdragons.server.entity.dragons.riftdrake.handlers.RiftDrakeRandomSwimGoal;
import com.leon.saintsdragons.server.entity.handler.DragonKeybindHandler;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.AquaticDragon;
import com.leon.saintsdragons.server.entity.interfaces.RideableDragon;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreathAirGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

public class RiftDrakeEntity extends DragonEntity implements RideableDragon, AquaticDragon {

    private static final EntityDataAccessor<Float> DATA_RIDER_FORWARD = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_RIDER_STRAFE = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_ACCELERATING = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SWIMMING = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    private final DragonSoundHandler soundHandler = new DragonSoundHandler(this);
    private final DragonKeybindHandler keybindHandler = new DragonKeybindHandler(this);
    private final RiftDrakeAnimationHandler animationHandler = new RiftDrakeAnimationHandler(this);
    private final PathNavigation groundNavigation;
    private final DragonSwimNavigate waterNavigation;
    private final MoveControl landMoveControl;
    private final DragonSwimMoveControl swimMoveControl;
    private final LookControl landLookControl;
    private final SmoothSwimmingLookControl swimLookControl;
    private RiftDrakeRandomSwimGoal waterSwimGoal;
    private boolean swimming;
    private int swimTicks;
    private int ticksInWater;
    private int ticksOutOfWater;

    public RiftDrakeEntity(EntityType<? extends RiftDrakeEntity> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0F);
        this.groundNavigation = new DragonPathNavigateGround(this, level);
        this.waterNavigation = new DragonSwimNavigate(this, level);
        this.landMoveControl = new RiftDrakeMoveControl(this);
        this.swimMoveControl = new DragonSwimMoveControl(this, 6.0F, 0.08D, 0.12D);
        this.landLookControl = new LookControl(this);
        this.swimLookControl = new SmoothSwimmingLookControl(this, 10);
        this.navigation = this.groundNavigation;
        this.moveControl = this.landMoveControl;
        this.lookControl = this.landLookControl;
        this.setRideable(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return DragonEntity.createDragonAttributes()
                .add(Attributes.MAX_HEALTH, 140.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_RIDER_FORWARD, 0.0F);
        this.entityData.define(DATA_RIDER_STRAFE, 0.0F);
        this.entityData.define(DATA_ACCELERATING, false);
        this.entityData.define(DATA_SWIMMING, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new BreathAirGoal(this));
        this.goalSelector.addGoal(2, new RiftDrakeLeaveWaterGoal(this));
        this.goalSelector.addGoal(3, new RiftDrakeFindWaterGoal(this));
        this.waterSwimGoal = new RiftDrakeRandomSwimGoal(this, 1.0D, 30);
        this.goalSelector.addGoal(4, waterSwimGoal);
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, animationHandler::movementPredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }

    @Override
    public void initializeAnimationState() {
        // TODO: derive initial animation state for Rift Drake
    }

    @Override
    public void resetAnimationState() {
        // TODO: recalculate animation state when needed
    }

    public DragonSoundHandler getSoundHandler() {
        return soundHandler;
    }

    public DragonKeybindHandler getKeybindHandler() {
        return keybindHandler;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            boolean inWater = this.isInWater();
            if (inWater) {
                this.setAirSupply(this.getMaxAirSupply());
                swimTicks = Math.min(swimTicks + 1, 200);
                ticksInWater = Math.min(ticksInWater + 1, 1200);
                ticksOutOfWater = 0;
            } else {
                swimTicks = Math.max(swimTicks - 1, 0);
                ticksOutOfWater = Math.min(ticksOutOfWater + 1, 1200);
                ticksInWater = 0;
            }

            if (inWater && !swimming) {
                enterSwimState();
            } else if (!inWater && swimming) {
                exitSwimState();
            }
        }
    }

    @Override
    public void travel(@NotNull Vec3 motion) {
        if (this.isInWater()) {
            this.moveRelative(this.getSpeed(), motion);
            this.move(MoverType.SELF, this.getDeltaMovement());
            Vec3 delta = this.getDeltaMovement();
            float drag = this.isControlledByLocalInstance() ? 0.9F : 0.92F;
            this.setDeltaMovement(delta.multiply(drag, 0.9D, drag));
            if (this.getTarget() == null) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.005D, 0.0D));
            }
        } else {
            super.travel(motion);
        }
    }

    @Override
    public PathNavigation getNavigation() {
        return swimming ? waterNavigation : groundNavigation;
    }

    @Override
    public PathNavigation getAquaticNavigation() {
        return waterNavigation;
    }

    @Override
    public void onEnterWater() {
        this.setDeltaMovement(this.getDeltaMovement().add(getBuoyancyVector()));
    }

    @Override
    public void onExitWater() {
        this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.6D, 1.0D));
    }

    @Override
    public InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        return super.mobInteract(player, hand);
    }

    @Override
    public void setLastRiderForward(float forward) {
        this.entityData.set(DATA_RIDER_FORWARD, forward);
    }

    @Override
    public void setLastRiderStrafe(float strafe) {
        this.entityData.set(DATA_RIDER_STRAFE, strafe);
    }

    @Override
    public boolean isAccelerating() {
        return this.entityData.get(DATA_ACCELERATING);
    }

    @Override
    public int getGroundMoveState() {
        return 0;
    }

    @Override
    public int getSyncedFlightMode() {
        return -1;
    }

    @Override
    public int getEffectiveGroundState() {
        return 0;
    }

    @Override
    public void setAccelerating(boolean accelerating) {
        this.entityData.set(DATA_ACCELERATING, accelerating);
    }

    @Override
    public int getMaxAirSupply() {
        return 600;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public double getSwimSpeed() {
        return 0.28D;
    }

    @Override
    public Vec3 getBuoyancyVector() {
        return new Vec3(0.0D, 0.03D, 0.0D);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    @Nullable
    @Override
    public Vec3 getDismountLocationForPassenger(@NotNull net.minecraft.world.entity.LivingEntity passenger) {
        Vec3 dismount = super.getDismountLocationForPassenger(passenger);
        if (dismount != null) {
            return dismount;
        }
        return this.position().add(1.0D, 0.0D, 0.0D);
    }

    @Override
    public @NotNull Vec3 getRiddenInput(@NotNull Player player, @NotNull Vec3 deltaIn) {
        return super.getRiddenInput(player, deltaIn);
    }

    @Override
    public void removePassenger(@NotNull net.minecraft.world.entity.Entity passenger) {
        super.removePassenger(passenger);
    }

    @Override
    public void tickAnimationStates() {
        // TODO: implement animation states
    }

    @Override
    public Vec3 getHeadPosition() {
        return this.getEyePosition();
    }

    @Override
    public Vec3 getMouthPosition() {
        return this.position().add(0, this.getBbHeight() * 0.8, 0);
    }

    @Override
    public net.minecraft.world.entity.AgeableMob getBreedOffspring(@NotNull net.minecraft.server.level.ServerLevel level, @NotNull net.minecraft.world.entity.AgeableMob other) {
        return null;
    }

    @Override
    public com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> getPrimaryAttackAbility() {
        return null;
    }

    public static boolean canSpawn(EntityType<RiftDrakeEntity> type, LevelAccessor level, MobSpawnType reason, BlockPos pos, net.minecraft.util.RandomSource random) {
        return level.getFluidState(pos).isSource() || level.getFluidState(pos.below()).isSource();
    }

    private void enterSwimState() {
        swimming = true;
        this.navigation = waterNavigation;
        this.moveControl = swimMoveControl;
        this.lookControl = swimLookControl;
        this.entityData.set(DATA_SWIMMING, true);
        if (waterSwimGoal != null) {
            waterSwimGoal.forceTrigger();
        }
    }

    private void exitSwimState() {
        swimming = false;
        this.navigation = groundNavigation;
        this.moveControl = landMoveControl;
        this.lookControl = landLookControl;
        this.waterNavigation.stop();
        Vec3 delta = this.getDeltaMovement();
        this.setDeltaMovement(new Vec3(delta.x, 0.0D, delta.z));
        this.entityData.set(DATA_SWIMMING, false);
    }

    public boolean isSwimming() {
        if (level().isClientSide) {
            return this.entityData.get(DATA_SWIMMING);
        }
        return swimming;
    }

    @Nullable
    public Vec3 pickSwimTarget(double radius, double verticalRange) {
        return LandRandomPos.getPos(this, (int) radius, (int) verticalRange);
    }

    private static class RiftDrakeMoveControl extends MoveControl {
        private final RiftDrakeEntity drake;

        public RiftDrakeMoveControl(RiftDrakeEntity drake) {
            super(drake);
            this.drake = drake;
        }

        @Override
        public void tick() {
            super.tick();
        }
    }
}
